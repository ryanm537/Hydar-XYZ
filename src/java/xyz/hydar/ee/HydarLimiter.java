package xyz.hydar.ee;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.net.InetAddress;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;


/**Provides token bucket rate limiting*/
public class HydarLimiter extends Limiter{ 
	private boolean alive=false;
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	public static volatile int maxBuffer=1024000;
	private static final ScheduledFuture<?> updateTask=executor.scheduleWithFixedDelay(()->{
		if(Hydar.alive)
			updateAll();
	},500,500,TimeUnit.MILLISECONDS);
	private static final Map<InetAddress,HydarLimiter> limiters=new ConcurrentHashMap<>();
	//just make a List of an obj and the obj contains lastReset and stuff?? cant be record
	//might still be worth for tokensLeft only --> avoid map traversal?
	private static final Map<Token,Map<Long,Long>> lastReset=new EnumMap<>(Token.class);
	private final Map<Token,Map<Long,LongAdder>> tokensLeft=new EnumMap<>(Token.class);//only the integer changes or something
	private final LongAdder inBytes = new LongAdder();
	private final LongAdder outBytes = new LongAdder();
	private static final int byteGrain = 1024*1024;
	//this is better, but it doesn't account for fast api
	//IN gets uniquely spammed when doing readInt etc, fast api wouldn't?
	//problem: we don't even have token.in as an object
	//we do, actually
	
	//we also don't have hthread everywhere
	//what do???
	
	//otherwise need grain to be passed to Response
	//h2 and stuff can get it easier but annoying
	static {
		for(var token:Token.values()) {
			var map=new LinkedHashMap<Long,Long>(token.tasks().size());
			for(var task:token.tasks().keySet()) {
				map.put(task,0L);
			}
			lastReset.put(token,map);
		}
		//System.out.println(lastReset);
		Limiter.setProvider(HydarLimiter::from);
	}
	
	private HydarLimiter() {
		for(Token token:Token.values()) {
			tokensLeft.put(token,token.tasks()
				.keySet().stream()
				.collect(toUnmodifiableMap(x->x,x->new LongAdder())
				)
			);
		}
	}
	public static Limiter from(InetAddress address) {
		if(address.isLoopbackAddress())
			return Limiter.UNLIMITER;
		HydarLimiter ret = limiters.get(address);
		if(ret==null) {
			ret=new HydarLimiter();
			limiters.put(address,ret);
		}
		return ret;
		
	}
	//TODO: update gradually(for now, just avoid long reset periods)
	public static void updateAll() {
		Long now=System.currentTimeMillis();
		if(!Hydar.alive) {
			updateTask.cancel(false);
			return;
		}
		for(Token token:Token.values()) {
			var thisMap=lastReset.get(token);
			for(var last:thisMap.entrySet()) {
				Long time=last.getKey();
				long timestamp=last.getValue();
				if(time>=0&&timestamp+time<now) {
					thisMap.put(time,now);
					for(var limiter:limiters.values()) {
						limiter.tokensLeft.get(token).get(time).reset();
					}
				}
			}
		}
		limiters.values().removeIf(HydarLimiter::empty);
	}
	private boolean empty() {
		if(!alive)return false;
		for(Token token:Token.values()) {
			for(var adder:tokensLeft.get(token).values()) {
				if(adder.sum()!=0) {
					return false;
				}
			}
		}
		return true;
	}
	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		}catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	@Override
	public boolean checkBuffer(int bytesToRead){
		if(maxBuffer<bytesToRead) {
			return false;
		}return true;
	}
	@Override
	public boolean acquireNow(AbstractToken t, int amount){
		return acquireImpl(t,amount,false);
	}
	@Override
	public boolean acquire(AbstractToken t,int amount) {
		return acquireImpl(t,amount,true);
	}
	public boolean acquireImpl(AbstractToken t, int amount, boolean blocking){
		//in and out have separate grain since they frequently get called with low amounts
		//System.out.println(t+", "+amount);
		if(t == Token.IN || t == Token.OUT) {
			var shouldLimit = t==Token.IN ? inBytes:outBytes;
			long oldVal = shouldLimit.sum();
			shouldLimit.add(amount);
			if((oldVal + amount) / byteGrain > oldVal / byteGrain) {
				amount = Math.max(byteGrain, amount);
			}else return true;
		}
		final int granularity = 1000;
		final int maxTries = 15;
		int totalWait=0;
		var leftMap = tokensLeft.get(t);
		var resetMap = lastReset.get(t);
		long now=System.currentTimeMillis();
		nextTask:
		for(var time:t.tasks().keySet()) {
			var counter=leftMap.get(time);
			long used=0;
			long total=t.getCount(time);
			try {
				int tries=0;
				while((used=counter.sum()) >= total && ++tries < maxTries) {
					if(time<0) {
						if(!blocking)
							return false;
						sleep(granularity);
						continue;
					}
					long delta=(resetMap.get(time)+time-(now));
					//time until the next update
					//but if <0, we should make it even longer
					//tokens should be mostly prob based???
					//problem is the amount depends
					//so: if it is a small portion of the wait time, do a random or interval wait
					//--> we already do no throttling at all below 1/2
					//but this reduces small waits above 1/2
					
					//whether or not something happens at all should depend on the % of tokens you use
					//at 10% do something guaranteed?
					//the something that happens is adding to total wait
					
					//not random, instead
					//only sleep if granularity threshold is crossed -> thing divided by 1s(w/o remainder) is different than before 
					
					//find out using totalWait only, and only sleep totalWait time?
					//delta should be based on how many tokens you are over, not how far from the next reset
					//actually, both
					//System.out.print("delta: "+delta);
					delta += delta * 2 * amount/total;//(-1*delta)/(time)
					//System.out.println(", "+delta);
					//System.out.println("next update in "+(delta));
					//System.out.println("l1: adding "+delta+" ms "+t);
					if(delta<0)continue nextTask;
					totalWait+=(int)delta+250;
					if(used>total*2) {
						//System.out.println("rejected - 2x tokens used");
						return false;
					}
					break;
				}
				if(used<total && used>total/4) {
					long left=total-used;//must not be 0
					int smoothFactor=(int)(1024*left/total);
					int sleepTime=(int)Math.min(granularity,(1024*(smoothFactor+amount)/(left+smoothFactor)));
					
					//int sleepTime=(int)Math.min(1000,((1024*amount)/(left)));
					//System.out.println("l2: adding "+(sleepTime)+" ms(max 1k) "+t);
					totalWait+=sleepTime;
				}
			}finally {
				counter.add(amount);
			}
		}
		if(!blocking&&totalWait>250) {
			//System.out.println("rejected - non blocking");
			return false;
		}if(totalWait > maxTries*granularity) {
			totalWait = maxTries * granularity;
		}

		//long oldTotalTime = oldTotalTime.sum();
		//oldTotalTime.add(totalWait);
		if((totalWait + now) / granularity > now / granularity) {
			//System.out.println("total wait "+totalWait+" ms "+t);
			sleep(Math.max(granularity, totalWait));
		}
		alive=true;
		return true;
	}
	@Override
	public void release(AbstractToken t, int amount){
		var leftMap=tokensLeft.get(t);
		for(var task:leftMap.values()) {
			task.add(-amount);
		}
	}
	@Override
	public String toString() {
		return tokensLeft.toString();
	}
}

