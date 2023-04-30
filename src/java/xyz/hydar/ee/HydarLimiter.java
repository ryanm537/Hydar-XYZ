package xyz.hydar.ee;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.net.InetAddress;
import java.util.EnumMap;
import java.util.HashMap;
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
	private static final Map<Token,Map<Long,Long>> lastReset=new EnumMap<>(Token.class);
	private final Map<Token,Map<Long,LongAdder>> tokensLeft=new EnumMap<>(Token.class);//only the integer changes or something
	static {
		for(var token:Token.values()) {
			var map=new HashMap<Long,Long>();
			for(var task:token.tasks().keySet()) {
				map.put(task,0L);
			}
			lastReset.put(token,map);
		}
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
	public static void updateAll() {
		Long now=System.currentTimeMillis();
		if(!Hydar.alive) {
			updateTask.cancel(false);
			return;
		}
		for(Token token:Token.values()) {
			var thisMap=lastReset.get(token);
			for(var last:lastReset.get(token).entrySet()) {
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
		int totalWait=0;
		var leftMap = tokensLeft.get(t);
		var resetMap = lastReset.get(t);
		for(var task:t.tasks().entrySet()) {
			Long time=task.getKey();
			
			long now=System.currentTimeMillis();
			var counter=leftMap.get(time);
			long used=0;
			long total=t.getCount(time);
			try {
				if((used=counter.sum())>total) {
					if(time<0)return false;
					long delta=(resetMap.get(time)+time-(now));
					//time until the next update
					//but if <0, we should make it even longer
					//System.out.print("delta: "+delta);
					delta += delta * (-1*delta)/(time);
					//System.out.println(", "+delta);
					//System.out.println("next update in "+(delta));
					if(!blocking&&delta>250)return false;
					if(delta+totalWait>15000) {
						return false;
					}else {
						//System.out.println("throttling for "+delta+" ms"+t);
						if(delta<0)continue;
						totalWait+=(int)delta+250;
						sleep((int)delta+250);
					}
				}else if(used>total/2) {
					long left=total-used;
					int smoothFactor=(int)(1024*left/total);
					int sleepTime=(int)Math.min(1000,(1024*(smoothFactor+amount)/(left+smoothFactor)));
					//int sleepTime=(int)Math.min(1000,((1024*amount)/(left)));
					//System.out.println("l2: throttling for "+(sleepTime)+" ms(max 1k) "+t);
					if(sleepTime<100)continue;
					sleep(sleepTime);
					totalWait+=sleepTime;
				}
			}finally {
				counter.add(amount);
			}
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

