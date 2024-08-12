package xyz.hydar.ee;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

/**Stores context for a single HTTP/2 connection. 
 * Requires an associated ServerThread(for now)
 * */
public class HydarH2{
	/**TODO: unimplemented: local window(maybe configurable)/priorities*/
	/**TODO: server push - load deps based on rates, store in cookie(2 b64/etag?)*/
	public static final byte[] MAGIC="PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(ISO_8859_1);
	public final ServerThread thread;
	public final Config config;
	public final Map<Integer,HStream> streams= new HashMap<>();
	public final HydarHP compressor;
	public final HydarHP decompressor;
	//reuse since parsing is single threaded
	final Frame incoming=Frame.of(Frame.SETTINGS);
	public int minStream;
	public int maxStream;
	public int expects = -1;
	public int[] remoteSettings = {
		0,//unused
		4096,//SETTINGS_HEADER_TABLE_SIZE 
		0,//SETTINGS_ENABLE_PUSH
		4096,//SETTINGS_MAX_CONCURRENT_STREAMS 
		65535,//SETTINGS_INITIAL_WINDOW_SIZE
		16384,//SETTINGS_MAX_FRAME_SIZE
		8192//SETTINGS_MAX_HEADER_LIST_SIZE
	};
	public int[] localSettings;
	public volatile int localWindow;
	//longadder might be better but this saves memory
	public final AtomicInteger remoteWindow = new AtomicInteger(remoteSettings[Setting.SETTINGS_INITIAL_WINDOW_SIZE]);
	public final AtomicInteger senders=new AtomicInteger();//used for upload buffer limiting
	volatile ByteBuffer input=ByteBuffer.allocate(0);
	volatile ByteBuffer output=ByteBuffer.allocate(32);
	public static final Frame SETTINGS_ACK=Frame.of(Frame.SETTINGS).ackFlag();
	public HydarH2(ServerThread thread) throws IOException {
		this.thread=thread;
		this.config=thread.config;
		localSettings=new int[]{
			0,//unused
			Config.H2_HEADER_TABLE_SIZE,//SETTINGS_HEADER_TABLE_SIZE 
			0,//SETTINGS_ENABLE_PUSH
			Config.H2_MAX_CONCURRENT_STREAMS,//SETTINGS_MAX_CONCURRENT_STREAMS 
			65535,//SETTINGS_INITIAL_WINDOW_SIZE
			Config.H2_MAX_FRAME_SIZE,//SETTINGS_MAX_FRAME_SIZE
			Config.H2_MAX_HEADER_LIST_SIZE//SETTINGS_MAX_HEADER_LIST_SIZE
		};
		localWindow= localSettings[Setting.SETTINGS_INITIAL_WINDOW_SIZE];
		compressor = new HydarHP.Compressor(localSettings[Setting.SETTINGS_HEADER_TABLE_SIZE]);
		decompressor = new HydarHP.Decompressor(remoteSettings[Setting.SETTINGS_HEADER_TABLE_SIZE]);
		thread.client.setSoTimeout(Config.H2_LIFETIME);
		incoming.limiter(thread.limiter);
		sendSettings();
	}
	public void ackSettings() throws IOException{
		//note: no limitr
		SETTINGS_ACK.writeTo(thread.output, false);
	};
	ByteBuffer output(int length) {
		return (length>output.capacity())?(output=ByteBuffer.allocate(length)):output;
	}
	ByteBuffer input(int length) {
		return (length>input.capacity())?(input=ByteBuffer.allocate(length)):input;
	}
	public void goaway(int error, String info){
		for(HStream stream:streams.values()) {
			if(stream!=null)
				stream.state=HStream.State.closed;
		}
		streams.clear();
		System.out.println("go away "+error+" "+info+" ");
		//new RuntimeException().fillInStackTrace().printStackTrace();
		try{
			var dos = ByteBuffer.allocate(info.length()+8)
				.putInt(maxStream&0x7fffffff)
				.putInt(error)
				.put(info.getBytes(ISO_8859_1));
			Frame.of(Frame.GOAWAY)
				.withData(dos.array(),0,dos.limit())
				.writeToH2(thread.h2, true);
		}catch(IOException e){
			
		}finally {
			//System.out.println("GO AWAY "+error+" "+info);
			//new RuntimeException().fillInStackTrace().printStackTrace();
			thread.close();
		}
	}
	
	public void sendSettings() throws IOException{
		
		var baos =ByteBuffer.allocate(6*localSettings.length);
		for(short i=1;i<localSettings.length;i++){
			baos.putShort(i).putInt(localSettings[i]);
		}
		Frame.of(Frame.SETTINGS)
			.limiter(thread.limiter)
			.withData(baos.array(),0,baos.limit())
			.writeToH2(this, false);
		Frame.of(Frame.WINDOW_UPDATE).withData(HStream.WINDOW_INC).writeToH2(this,false);
	}
	public void read() throws IOException{
		try {
			Frame.parse(thread.input,thread.h2);
		}catch(HttpTimeoutException e) {
			goaway(0xb,"");
		}catch(SocketTimeoutException e) {
			goaway(0,"hydar");
		}
	}
}
class HStream{
	//ServerThread has STREAMS(hash linked) - STREAM has state
	//can store the status of a block
	//some sort of parent stream/priority thing -> a tree?????
	//state probably an enum too honestly
	//methods: write a frame(also constantly reading-> has to extend thread lmao)
	/**
	possible stream errors:
	extraneous frames, prohibited header fields, the absence of mandatory
    header fields, or the inclusion of uppercase header field names.
	A request or response is also malformed
   if the value of a content-length header field does not equal the sum
   of the DATA frame payload lengths that form the body.
   goaway sends maxstream, immediately stops processing(before that probably)
   -->ensure nothing is sent by synchronizing, probably
   half-closed remote:
   If an endpoint receives additional frames, other than
      WINDOW_UPDATE, PRIORITY, or RST_STREAM, for a stream that is in
      this state, it MUST respond with a stream error (Section 5.4.2) of
      type STREAM_CLOSED.
	  
	  after RST->stream error
	  after es flag->connection error
	  
	  too long headers
	  
	  dynamic settings
	*/
	public State state;
	public final HydarH2 h2;
	public final int number;
	private Map<String,String> heads=null;
	public int blockType;
	public int padLength;
	//
	public int priority;
	public int dependent;
	//
	public volatile int localWindow;
	public final AtomicInteger remoteWindow;
	
	//
	private static final BAOS EMPTY_BAOS=new BAOS(0) {
		@Override public final void write(int b) {}
		@Override public final void write(byte[] b, int off, int len) {}
	};
	private BAOS block=EMPTY_BAOS;
	private BAOS dataBlock=EMPTY_BAOS;
	private int dataBlockCount=0;
	public static final byte[] WINDOW_INC = ByteBuffer.allocate(4).putInt(Config.H2_LOCAL_WINDOW_INC).array();
	private static final byte[][] CLOSE_REASONS=IntStream.range(0,20)
		.mapToObj(x->ByteBuffer.allocate(4).putInt(x).array())
		.toArray(byte[][]::new);
	static enum State{
		idle,reserved_local,reserved_remote,open,half_closed_remote,half_closed_local,closed
	}
	public HStream(State state,HydarH2 h2,int number){
		
		this.blockType=-1;
		this.padLength=0;
		this.state=state;
		this.h2=h2;
		if(state==State.open){
			h2.streams.keySet().removeIf(x->x<number);
		}
		localWindow=h2.localSettings[Setting.SETTINGS_INITIAL_WINDOW_SIZE];
		remoteWindow= new AtomicInteger(h2.remoteSettings[Setting.SETTINGS_INITIAL_WINDOW_SIZE]);
		
		this.number=number;
	}
	public boolean cleanup() {
		boolean first=h2.streams.remove(this.number)!=null;
		state=State.closed;
		if(first&&dataBlock.size()>0) {
			h2.senders.decrementAndGet();
		}
		return first;
	}
	public void close(int reason) throws IOException{
		if(cleanup() && reason>=0)
			Frame.of(Frame.RST_STREAM, this)
				.withData(CLOSE_REASONS[reason])
				.writeToH2(h2, true);
	}
	//check if request on this stream is HEAD, default to prev
	public boolean isHead(boolean prev) {
		return heads!=null?"HEAD".equals(heads.get(":method")):prev;
	}
	//wait for window to allow sending
	public int controlFlow(){
		long time=0,timer=Config.H2_REMOTE_WINDOW_TIMER,max=Config.H2_REMOTE_WINDOW_TIMER;
		while(canSend()&&time<max){
			
			//System.out.println(""+time+":"+remoteWindow+":"+h2.remoteWindow);
			int local=remoteWindow.get();
			int global=h2.remoteWindow.get();
			//System.out.println("l"+local+" g"+global);
			if(local>0&&global>0)
				return Math.min(local,global);
			try {
				Thread.sleep(time==0?50:timer);
				time+=time==0?50:timer;
			} catch (InterruptedException ee) {
				Thread.currentThread().interrupt();
			}
		}
		return -1;
	}
	private BAOS block() {
		return block==EMPTY_BAOS?(block=new BAOS(256)):block;
	}
	private BAOS dataBlock() {
		if(dataBlock==EMPTY_BAOS) {
			h2.senders.incrementAndGet();
			dataBlock=new BAOS(256);
		}
		return dataBlock;
	}
	public int dataBlockSize() {
		return dataBlock.size();
	}
	/**
	 * An endpoint MUST NOT send frames other than PRIORITY on a closed stream. An
	 * endpoint that receives any frame other than PRIORITY after receiving a
	 * RST_STREAM MUST treat that as a stream error (Section 5.4.2) of type
	 * STREAM_CLOSED. Similarly, an endpoint that receives any frames after
	 * receiving a frame with the END_STREAM flag set MUST treat that as a
	 * connection error (Section 5.4.1) of type STREAM_CLOSED, unless the frame is
	 * permitted as described below.
	 */
	public boolean canReceive(){
		return switch(this.state) {
			case idle, reserved_remote, open, half_closed_local -> true;
			default -> false;
		};
	}
	public boolean canSend(){
		return switch(this.state) {
			case idle, reserved_local, open, half_closed_remote -> true;
			default -> false;
		};
	}
	private void parseHeaders() {
		if(heads!=null)throw new IllegalStateException("Already parsed headers");
		heads = new HashMap<>(16);
		//long t1 = new Date().getTime();
		try(var byte_dis = block().toInputStream()){
			h2.decompressor.readFields(byte_dis,heads);
		}catch(IOException e){
			h2.goaway(9,"Decompressing failed");
			return;
		}
	}
	public void recv(Frame frame, ByteBuffer dis, InputStream more) throws IOException{
		
		switch(frame.type){
			case Frame.HEADERS:
				if(blockType!=-1&&blockType!=Frame.HEADERS){
					h2.goaway(1,"Unexpected HEADERS block");
					break;
				}
				blockType=Frame.HEADERS;
				
				if(canReceive()){
					h2.minStream=this.number;
					this.state=State.open;
					if(frame.padded){
						padLength=dis.get()&0xff;
						frame.length-=1;
					}if(frame.priority){
						dependent=dis.getInt()&0x7fff;
						priority=dis.get();
						frame.length-=5;
					}
					//datablock, only process at end_stream-> change state
					block().write(dis.array(), dis.position(), frame.length);
					if(frame.endHeaders){
						blockType=Frame.DATA;
						more.skip(padLength);
						padLength=0;
						parseHeaders();
					}else{
						h2.expects=Frame.CONTINUATION;
					}
				}else{
					//dis.skip(frame.length);
					this.close(5);
				}
				break;
			case Frame.DATA:
				if(blockType!=Frame.DATA){
					//error
					h2.goaway(1,"Unexpected DATA block");
					break;
				}
				if(canReceive()){
					this.state=State.open;
					if(frame.padded){
						padLength=dis.get()&0xff;
						frame.length-=1;
					}
					localWindow-=frame.length;
					h2.localWindow-=frame.length;
					if(localWindow<=0) {
						if(Config.H2_LOCAL_WINDOW_TIMER>0) {
							try {
								Thread.sleep(Config.H2_LOCAL_WINDOW_TIMER*Hydar.threadCount.get());
							} catch (InterruptedException e) {}
						}
						localWindow += Config.H2_LOCAL_WINDOW_INC;
						Frame.of(Frame.WINDOW_UPDATE,this).withData(WINDOW_INC).writeToH2(h2,false);
					}
					if(h2.localWindow<=0) {
						if(Config.H2_LOCAL_WINDOW_TIMER>0) {
							try {
								Thread.sleep(Config.H2_LOCAL_WINDOW_TIMER*Hydar.threadCount.get());
							} catch (InterruptedException e) {}
						}
						h2.localWindow += Config.H2_LOCAL_WINDOW_INC;
						Frame.of(Frame.WINDOW_UPDATE).withData(WINDOW_INC).writeToH2(h2,false);
					}
					//skip length calculation if limiter disabled, and only every 100 frames
					if((++dataBlockCount%100==0)&&h2.thread.limiter!=null && !(h2.thread.limiter.checkBuffer(Integer.MAX_VALUE))) {
						if(!h2.thread.limiter.checkBuffer(dataBlock.size() * h2.senders.get())) {
							close(0xb);//ENHANCE_YOUR_CALM
							return;
						}
					}
					dataBlock().write(dis.array(), dis.position(), frame.length);
					if(frame.endStream){
						more.skip(padLength);
						padLength=0;
					}
				}else{
					//skip
					this.close(5);
				}
				//added to request
				break;
			case Frame.PRIORITY:
				//dis.readInt();
				//dis.read();
				break;
			case Frame.RST_STREAM:
				//dis.readInt();
				this.close(0);
				break;
			case Frame.WINDOW_UPDATE:
				int inc=(dis.getInt()&0x7fffffff);
				//System.out.println("Stream "+number+" +"+inc);
				if(inc==0){
					h2.goaway(1,"0 window increment");
				}else{
					remoteWindow.addAndGet(inc);
				}
				break;
			case Frame.CONTINUATION:
				if(blockType==Frame.HEADERS){
					block().write(dis.array(), dis.position(), frame.length);
					if(frame.endHeaders){
						h2.expects=-1;
						blockType=Frame.DATA;
						more.skip(padLength);
						padLength=0;
						parseHeaders();
					}
				}else{
					//Protocol error
					h2.goaway(1,"Unexpected continuation");
				}
				break;
			default:
				//dis.skip(frame.length);
				return;
		}

		if(frame.endStream){
			if(!canReceive())return;
			if(state==State.half_closed_local) {
				this.close(0);
				return;
			}
			state=State.half_closed_remote;
			blockType=-1;
			h2.expects=-1; 
			
			//System.out.println("read headers took "+(new Date().getTime()-t1)+" ms");
				//System.out.println(heads);
				//System.out.println("\""+heads.get(":path")+"\" "+heads.get(":path").length());
			more.skip(padLength);
			//if(heads.get(":path").endsWith(".jsp")){
			
			Limiter limiter = h2.thread.limiter;
			boolean concurrent = h2.streams.size()<=1+h2.localSettings[Setting.SETTINGS_MAX_CONCURRENT_STREAMS];
			Runnable streamTask = ()->{
				try{
					if(!limiter.acquire(Token.FAST_API,Config.TC_FAST_HTTP_REQUEST)) {
						close(0xb);//enhance_your_calm
						return;
					}
					h2.thread.hparse(heads,Optional.of(this),dataBlock.buf(),dataBlock.size());
				}catch(IOException e){
					
				}finally {
					//h2.streams.remove(number);
					block = dataBlock = EMPTY_BAOS;
					limiter.release(Token.PERMANENT_STATE, Config.TC_PERMANENT_H2_STREAM);
					h2.maxStream=Math.max(this.number,h2.maxStream);
					this.padLength=0;
					try {
						//System.out.println(this.state);
						if(this.state!=State.open){
							this.close(0);
						}else this.state=State.half_closed_local;
					}catch(IOException e) {
						this.cleanup();
					}
				}
			};
			if(limiter.acquire(Token.PERMANENT_STATE, Config.TC_PERMANENT_H2_STREAM) && concurrent) {
				//System.out.println("concurrent "+h2.streams.size()+concurrent);
				HydarUtil.TFAC.newThread(streamTask).start();
			}else {
				//System.out.println("nonconcurrent "+h2.streams.size()+concurrent);
				streamTask.run();
			}
		}
	}
	//
}

//ServerThread has SETTINGS(record probably)

class Setting{
	public static final int SETTINGS_HEADER_TABLE_SIZE=1;
	public static final int SETTINGS_ENABLE_PUSH=2;
	public static final int SETTINGS_MAX_CONCURRENT_STREAMS=3;
	public static final int SETTINGS_INITIAL_WINDOW_SIZE=4;
	public static final int SETTINGS_MAX_FRAME_SIZE=5;
	public static final int SETTINGS_MAX_HEADER_LIST_SIZE=6;
}

//ServerThread has SETTINGS(record probably)

class Frame{
	/**constants for frame types*/
	public static final byte DATA=0x00;
	public static final byte HEADERS=0x01;
	public static final byte PRIORITY=0x02;
	public static final byte RST_STREAM=0x03;
	public static final byte SETTINGS=0x04;//stream 0 required
	public static final byte PUSH_PROMISE=0x05;
	public static final byte PING=0x06;//stream 0 required
	public static final byte GOAWAY=0x07;//stream 0 required
	public static final byte WINDOW_UPDATE=0x08;
	public static final byte CONTINUATION=0x09;
	//flags(pingAck,settingsAck equivalent to endStream)
	public boolean endStream=false;
	public boolean endHeaders=false;
	public boolean padded=false;
	public boolean priority=false;
	public int length;
	public int flags;
	public byte type=0;
	public HStream stream;
	private int offset=0;
	private byte[] data=null;
	private InputStream dataStream=null;
	private Optional<Limiter> limiter=Optional.empty();
	private Optional<ByteBuffer> streamBuffer=Optional.empty();
	private volatile Optional<Lock> lock=Optional.empty();
	private final int MAX_DATA_FRAME_SPLITS=8;
	//error codes
	public Frame(byte type){
		this.type=type;
	}
	public Frame(byte type, HStream stream){
		this.type=type;
		this.stream=stream;
	}
	public static Frame of(byte type) {
		return new Frame(type);
	}
	public static Frame of(byte type, HStream stream) {
		return new Frame(type,stream);
	}
	public Frame stream(HStream stream) {
		this.stream=stream;
		return this;
	}
	public Frame type(byte type) {
		this.type=type;
		return this;
	}
	public Frame withFlags(byte flags) {
		this.flags=flags;
		endStream = (flags&0x01)!=0;
		endHeaders = (flags&0x04)!=0;
		padded = (flags&0x08)!=0;
		priority = (flags&0x20)!=0;
		return this;
	}
	private void addFlag(boolean flag, byte mask) {
		if(flag)
			flags|=mask;
		else if((flags&mask)==mask)
			flags^=mask;
	}
	public Frame endStream() {
		return endStream(true);
	}
	public Frame endStream(boolean flag) {
		addFlag(flag,(byte)0x01);
		this.endStream=flag;
		return this;
	}
	public Frame ackFlag() {
		return endStream(true);
	}
	public Frame ackFlag(boolean flag) {
		return endStream(flag);
	}
	public Frame endHeaders() {
		return endHeaders(true);
	}
	public Frame endHeaders(boolean flag) {
		addFlag(flag,(byte)0x04);
		this.endHeaders=flag;
		return this;
	}
	public Frame withData(InputStream data,int length, ByteBuffer transferBuffer) {
		this.dataStream=data;
		this.length=length;
		this.streamBuffer=Optional.of(transferBuffer);
		return this;
	}
	public Frame withData(byte[] data) {
		this.data=data;
		this.length=data.length;
		return this;
	}
	public Frame withBuffer(ByteBuffer transferBuffer) {
		this.streamBuffer=Optional.of(transferBuffer);
		return this;
	}
	public Frame withData(byte[] data, ByteBuffer transferBuffer) {
		this.data=data;
		this.length=data.length;
		this.streamBuffer=Optional.of(transferBuffer);
		return this;
	}
	public Frame withData(BAOS data) {
		this.data=data.buf();
		this.length=data.size();
		return this;
	}
	public Frame withData(byte[] data,int offset, int length) {
		this.data=data;
		this.offset=offset;
		this.length=length;
		return this;
	}
	public int streamNum() {
		return stream==null?0:stream.number;
	}
	public byte[] header() {
		int s = streamNum();
		return new byte[]{
				(byte)((length>>16)&0xff),
				(byte)((length>>8)&0xff),
				(byte)((length)&0xff),
				(type),
				(byte)(flags),
				(byte)((s>>24)&0x7f),
				(byte)((s>>16)&0xff),
				(byte)((s>>8)&0xff),
				(byte)(s&0xff)
			};
	}
	public Frame lock(Lock lock) {
		this.lock=Optional.of(lock);
		return this;
	}
	public void writeToH2(HydarH2 h2, boolean flush) throws IOException{
		if(length>0)
			lock(h2.thread.lock);
		limiter(h2.thread.limiter);
		lock.ifPresent(Lock::lock);
		try {
			withBuffer(h2.output(length+9));
		}finally {
			lock.ifPresent(Lock::unlock);
		}
		writeTo(h2.thread.output,flush);
	}
	public Frame limiter(Limiter limiter) {
		this.limiter=Optional.of(limiter);
		return this;
	}
	public void putHeader(ByteBuffer to) throws IOException{
		to.putInt(length<<8 | type).put((byte)flags)
				.putInt(streamNum()&0x7ffffff);
	}
	public void writeTo(OutputStream o, boolean flush) throws IOException{
		writeTo(o,flush,0);
	}
	private void writeTo(OutputStream o, boolean flush, int splits) throws IOException{
		Frame part2=null;
		int neededWindow=splits<MAX_DATA_FRAME_SPLITS?1:length;
		if(stream!=null && type==Frame.DATA) {
			int attempts=Config.H2_WINDOW_ATTEMPTS, windowLeft=stream.controlFlow();
			while(windowLeft<neededWindow&&stream.canSend()){
				attempts--;
				//System.out.println("ATTEMPT "+attempts);
				if(attempts<0) {
					//kill h2 if no global window, kill stream if no stream window
					if(stream.remoteWindow.get()>=neededWindow && stream.canSend())
						stream.h2.goaway(0,"Flow control timeout");
					else stream.close(5);
					return;
				}
				windowLeft=stream.controlFlow();
				//System.out.println("%%%%%%LEFT%%%%%%%%"+windowLeft);
			}
			//we have some window available but not the full length - split the frame
			//this way client will notice 0 window and request more
			if(splits<MAX_DATA_FRAME_SPLITS && stream.canSend() && windowLeft>0 && windowLeft<this.length) {
				int oldLength = length;
				length = windowLeft;
				endStream=false;
				if(dataStream!=null) {
					part2=Frame.of(Frame.DATA,stream)
							.endStream(endStream)
							.withData(dataStream,oldLength-length,streamBuffer.orElse(null));
					
				}else {
					part2 = Frame.of(Frame.DATA,stream)
						.endStream(endStream)
						.withData(data,offset+length,oldLength-length);
				}
			}
		}
		if(stream!=null && !stream.canSend())
			return;
		int length=this.length;//protect lock
		lock.ifPresent(Lock::lock);
		try {
			//TODO: the copying didn't seem to lower the amount of tls fragmentation so try removing it again?
			limiter.ifPresent(x->x.acquire(Token.OUT, 9+length));
			var buf = streamBuffer.orElseGet(()->ByteBuffer.allocate(9+length)).position(0);
			putHeader(buf);
			if(length==0 && !flush) {
				return;
			}else if(dataStream!=null) {
				int l=0;
				int cap=length;
				while(cap>0 && (l=dataStream.read(buf.array(),buf.position(),cap))>0) {
					buf.position(buf.position()+l);
					cap-=l;
				}
			}else if(data!=null){
				buf.put(data,offset,length);
			}
			if(stream!=null && type==Frame.DATA) {
				stream.remoteWindow.addAndGet(-length);
				stream.h2.remoteWindow.addAndGet(-length);
			}
			o.write(buf.array(),0,9+length);
			if(flush)
				o.flush();
			if(part2!=null) {
				part2.writeTo(o,flush,splits+1);
			}
		}finally {
			lock.ifPresent(Lock::unlock);
		}
	}
	public byte[] toByteArray() {
		BAOS baos = new BAOS(9+length);
		try {
			writeTo(baos, false);
		} catch (IOException e) {}
		return baos.buf();
	}
	public static void parse(InputStream is, HydarH2 h2) throws IOException{
		//int length = new BigInteger(dis.readNBytes(3)).intValue();
		ServerThread t = h2.thread;
		t.limiter.force(Token.FAST_API, Config.TC_FAST_H2_FRAME);
		//use a byte buffer(avoid eofexception)
		ByteBuffer h = h2.input(9).position(0);
		if(is.readNBytes(h.array(), 0, 9)<9) {
			h2.goaway(0,"hydar(eof)");
			return;
		}
		int length=h.getInt();
		byte type = (byte)length;
		length>>>=8;
		//System.out.println("Length:" +length);
		byte flags = h.get();
		int stream = h.getInt() & 0x7fff;
		Frame frame= h2.incoming
				.type(type)
				.withFlags(flags);//must be odd or even or something like that idk
		frame.length=length;
		//all the possible length errors
		boolean lengthCheck=frame.length<=h2.localSettings[Setting.SETTINGS_MAX_FRAME_SIZE];
		lengthCheck = lengthCheck && 
			switch(frame.type) {
				case PRIORITY->frame.length==5;
				case PING->frame.length==8;
				case RST_STREAM, WINDOW_UPDATE->frame.length==4;
				//endStream is 'ack' in this case
				case SETTINGS->(!frame.endStream||frame.length==0) && frame.length%6==0;
				default->true;
			};
		if(!lengthCheck){
			//Stream error(too long)
			//protocol error instead maybe?
			h2.goaway(6,"Frame size error (frame type "+frame.type+")");
			//dis.skip(frame.length);
			return;
		}
		//set flags
		if(h2.expects==Frame.CONTINUATION&&frame.type!=Frame.CONTINUATION){
			//protocol error(unexpected)
			is.skip(frame.length);
			h2.goaway(1,"Expected CONTINUATION");
			return;
		}
		//push promise contains request headers for the response being pushed
		//System.out.println(frame);
		//implicitly done in buffereddis methods
		//t.limiter.force(Token.IN, frame.length+9);
		h=h2.input(frame.length).position(0);
		if(is.readNBytes(h.array(),0,frame.length)<frame.length) {
			h2.goaway(0,"hydar(eof)");
			return;
		}
		switch(frame.type){
			case SETTINGS:
				if(frame.endStream)
					break;
				if(stream!=0){
					h2.goaway(1,"Expected stream 0");
					return;
				}
				for(int i=0;i<frame.length;i+=6){
					int id=h.getShort();
					int value=h.getInt();
					if(id<1||id>=h2.remoteSettings.length){
						//ignore
					}else{
						if((id==Setting.SETTINGS_ENABLE_PUSH&&(value!=0&&value!=1))||
							(id==Setting.SETTINGS_INITIAL_WINDOW_SIZE&&(value<0))||
							(id==Setting.SETTINGS_MAX_FRAME_SIZE&&(value>16777215))	
							){
							//Connection PROTOCOL_ERROR
							h2.goaway(1,"invalid setting value");
						}
						if(id==Setting.SETTINGS_INITIAL_WINDOW_SIZE){
							h2.remoteWindow.addAndGet(value-h2.remoteSettings[id]);
							h2.streams.values().forEach(s->s.remoteWindow.addAndGet(value-h2.remoteSettings[id]));
						}else if(id==Setting.SETTINGS_HEADER_TABLE_SIZE) {
							h2.decompressor.setMaxTableSize(value);
						}
						h2.remoteSettings[id]=value;
					}
				}
				h2.ackSettings();
			break;
			case PING:
				if(stream!=0){
					h2.goaway(1,"Expected stream 0");
					return;
				}
				Frame.of(Frame.PING)
					.ackFlag()
					.withData(h.array(),h.position(),8)
					.writeToH2(h2, true);
				h.position(h.position()+8);
				break;
			case GOAWAY:
				if(stream!=0){
					h2.goaway(1,"Expected stream 0");
					return;
				}
				System.out.println("GO AWAY(client)");
				h2.goaway(0,"ack");
				//send too maybe
				break;
			case PUSH_PROMISE:
				h2.goaway(1,"Client cannot push");
				return;
			case HEADERS:
				//new stream
				if(h2.streams.get(stream)==null){
					if(stream%2==0){
						//PROTOCOL ERROR
						h2.goaway(1,"Invalid stream "+stream);
						return;
					}else if(stream<h2.minStream){
						h2.streams.put(stream, new HStream(HStream.State.closed,h2,stream));
					}else h2.streams.put(stream, new HStream(HStream.State.idle,h2,stream));
				}
			case WINDOW_UPDATE://can have strm 0
			case PRIORITY://can be received when closed? --> make new temporary closed stream and handle it
			case RST_STREAM:
			case DATA:
			case CONTINUATION:
				if(stream==0&&frame.type==WINDOW_UPDATE){
					int inc=(h.getInt()&0x7fffffff);
					if(inc==0){
						h2.goaway(1,"0 window increment");
					}else{
						//System.out.println("g "+inc);
						h2.remoteWindow.addAndGet(inc);
						//h2.streams.values().forEach(s->s.remoteWindow+=inc);
					}
					return;
				}else{
					var hstream=h2.streams.get(stream);
					if(hstream!=null){
						hstream.recv(frame,h,is);
					}else{
						//ignore
						//is.skip(frame.length);
						//t.goaway(1,"Invalid stream");
					}
				}
				break;
			//ignore unknown
			default:
				//is.skip(frame.length);
				break;
		}
		//read length bytes, unless less than allowed
		//instantiate?
		//record or class?
		//additional operations based on type
		//types have FLAGS
		//return Frame.of(type,flags);
	}
	@Override
	public String toString(){
		return "Type: "+type+"\t Stream: "+stream;
	}
}