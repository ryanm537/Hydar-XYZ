package xyz.hydar.ee;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import xyz.hydar.ee.HydarEE.HttpSession;

/**A WebSocket context. Dependent on a ServerThread*/
public class HydarWS extends OutputStream{
	//TODO:
	//private final boolean CLIENT_NO_CONTEXT_TAKEOVER;
	//private final boolean SERVER_NO_CONTEXT_TAKEOVER;
	//private final int CLIENT_MAX_WINDOW_BITS;
	//private final int SERVER_MAX_WINDOW_BITS;
	
	//websocket buffers and stuff
	private byte[] input;
	private int offset=0;
	private int size=1024;
	private int payloadSize=0;
	public int ping=10;
	
	//endpoint params
	public final ServerThread thread;
	public final Config config;
	private final String search;
	private final String path;
	private final Endpoint endpoint;

	//empty DEFLATE block(for reuse)
	private static final byte[] EMPTY_BLOCK=new byte[]{0x00,0x00,(byte)0xff,(byte)0xff};
	//close packet(for reuse)
	private static final byte[] WS_CLOSE={(byte)(0x80 | 0x08),0};
	
	//Compression params
	private boolean deflate;
	private BAOS inflate_baos;
	private InflaterOutputStream inflate_ios;
	public static ConcurrentHashMap<String, Object> endpoints=new ConcurrentHashMap<>();
	private BAOS deflate_baos;
	private DeflaterOutputStream deflate_dos;
	static final LongBuffer empty=LongBuffer.allocate(0);
	private volatile boolean alive=true;
	public final Hydar hydar;
	
	/**Initialize this context and its endpoint, if one is available.*/
	public HydarWS(ServerThread thread, String path,String search,boolean deflate) throws IOException{
		
		this.thread=thread;
		this.hydar=thread.hydar;
		this.config=thread.config;
		thread.client.setSoTimeout(config.WS_LIFETIME);
		this.path=path;
		this.search=search;
		this.deflate=deflate;
		if(deflate) {
			deflate_baos=new BAOS(256);
			deflate_dos=new DeflaterOutputStream(deflate_baos,new Deflater(Deflater.DEFAULT_COMPRESSION, true),true);
			inflate_baos = new BAOS(64) {
				@Override
				protected void ensureCapacity(int capacity) {
					if(!thread.limiter.checkBuffer(capacity))
						throw new RuntimeException("Buffer overflow");
					super.ensureCapacity(capacity);
				}
			};
			inflate_ios = new InflaterOutputStream(inflate_baos,new Inflater(true));
		}
		input=new byte[1024];
		if(!hasEndpoint(path)) {
			hydar.ee.jsp_invoke(path.substring(0,path.indexOf(".jsp")),thread.session,search);
		}
		endpoint=constructEndpoint(path,this);
		if(endpoint==null) {
			close();
		}else {
			endpoint.onOpen();
		}
	}

	/**
	 * Extending outputstream allows for endpoint overrides to use PrintStream.
	 * Single writes should normally never be used.
	 * */
	@Override
	public void write(int b) throws IOException {
		write(new byte[] {(byte)b},0,1);
	}
	/**
	 * Wrap the given bytes in a WS packet and send it.
	 * */
	@Override
	public void write(byte[] data, int start, int len) throws IOException{
		//System.out.println("id: "+this+" >> ");
		byte first=(byte)0x81;
		thread.lock.lock();
		try {
			if(deflate){
				first|=(byte)0b01000000;
				deflate_dos.write(data,start,len);
				deflate_dos.flush();
				data=deflate_baos.buf();
				start=0;
				len=deflate_baos.size();
				//Truncate empty deflate block if present.
				if(data[len-1]==(byte)0xff&&data[len-2]==(byte)0xff&&data[len-3]==0x00&&data[len-4]==0x00){
					len-=4;
				}
			}
			byte[] header;
			if(len>125){
				header = new byte[] {
					first,
					(byte)0b01111110,
					(byte)((len>>8)&0xff),
					(byte)(len&0xff)
				};
			}else
				header=new byte[] {
					first,
					(byte)(len&0x7F)
				};
			//System.arraycopy(ub,0,w,off2,l2);
			thread.output.write(header);
			thread.output.write(data,start,len);
			thread.output.flush();
			if(deflate)
				deflate_baos.reset();
		}finally {
			thread.lock.unlock();
		}
	
	}
	/**
	 * Closing a WebSocket calls onClose on the endpoint,
	 * writes a close packet, then closes the socket.
	 * */
	@Override
	public void close() throws IOException{
		if(alive) {
			alive=false;
			try {
				if(endpoint!=null) {
					endpoint.onClose();
				}
				super.close();
				if(thread.alive())
					thread.output.write(WS_CLOSE);
			} finally {
				thread.close();
			}
		}
	}
	/**Partial read. TODO: optimize*/
	private int read_() throws IOException{
		int len = thread.input.read(input,size-1024,800);
		thread.limiter.force(Token.IN,len);
		size+=len;
		payloadSize+=len;
		input = Arrays.copyOf(input,size);
		return len;
	}
	/**
	 * Unmask using bytebuffers. 
	 * Simple loops usually get vectorized so this might not be necessary
	 * */
	static byte[] unmask(byte[] sb, byte[] pl, int off) {
		var plb=ByteBuffer.wrap(pl);//output
		var buf=ByteBuffer.wrap(sb);//input
		var lbuf=plb.remaining()<8?empty:buf.slice(off+4,buf.limit()-(off+4)).asLongBuffer();
		long mask=buf.getInt(off);
		mask = (mask<<32) | (mask&0xffffffffl);
		while(plb.remaining()>=8) {
			plb.putLong(lbuf.get()^mask);
		}
		buf.position(off+4+lbuf.position()*8);
		while(plb.hasRemaining()){
			mask=Long.rotateLeft(mask,8);
			plb.put((byte)(buf.get()^mask));
		}
		return pl;
	}
	public void read() throws IOException{
		int len=0;
		int off=2;
		long length=0;
		String line="";
		//on error just die
		if(--ping==0||!thread.alive()){//nothing sent for a while
			System.out.println("L bozi,");
			close();
			return;
		}
		do{//TODO: does this account for multiple packets being queued?
			len=read_();
		}while(len>=0&&(len==800||length>(payloadSize-4-off))&&thread.limiter.checkBuffer(payloadSize));
		if(len<0) {
			close();return;
		}
		//Calculate length as specified by the rfc
		int l = (input[1])&0b01111111;
		if(l==126){
			length=((input[2]&0xff)<<8)|(input[3]&0xff);
			off=4;
		}else if(l==127){
			for (int i=2;i<10;i++) {
				length<<=8;
				length|=(input[i]&0xff);
			}
			off=10;
		}else{
			length=l;
		}
		
		
		if(size>0&&input[offset]<0){
			offset+=size+len;
			input = Arrays.copyOf(input,size);
		}else {
			//havent read enough
			return;
		}
		
		//A packet was read
		if(offset>0){
			ping=8;
			payloadSize=0;
			if(!thread.limiter.acquire(Token.FAST_API, Config.TC_FAST_WS_MESSAGE)) {
				close();
				return;
			}//empty
			if(offset==1){
				input= new byte[1024];
				return;
			}
			//not masked(close)
			if(((input[1]&(byte)0x80)>>7)==1){
				System.out.println("E");
				close();
				return;
			}
			if((len+offset)<length){
				return;
			}
			size=1024;
			offset=0;
			//decode data
			byte[] pl=new byte[(int)length];
			unmask(input,pl,off);
			if(deflate&&((input[0]&0b01000000)!=0x00)){
				inflate_ios.write(pl,0,pl.length-1);
				//empty DEFLATE block
				if(((input[off+4]^input[off])&0b00000001)==0x00){
					int i=pl.length-1;
					inflate_ios.write((input[i+off+4])^(input[off+(i%4)]));
					inflate_ios.write(EMPTY_BLOCK);
				}
				//out.write((byte)0x00);
				inflate_ios.flush();
				pl=inflate_baos.toByteArray();
				length = inflate_baos.size();
				inflate_baos.reset();
			}
			byte op = (byte)(input[0]&0x0f);
			if(op == 0x08||input[0]==0x88||(input[0]==0xff&&input[1]==0x00)){
				System.out.println("closed socket.");
				//output.write(pl);
				close();
				return;
			}else if(op == 0x09){
				//TODO: make this use buffers as well maybe
				System.out.println("aaa i got pinged");
				input[0]+=1;
				for(int i=0;i<length;i++){
					input[i+off+4]=(byte)((input[i+off+4])^(input[off+(i%4)]));
				}
				thread.output.write(input,0,(int)length+off+4);
				thread.output.flush();
				return;
			}else{
				if(thread.alive()==false){
					close();
					return;
				}
				line = new String(pl,0,(int)length,StandardCharsets.UTF_8);
				//on session expire, end the connection
				if(thread.session==null || thread.session!=hydar.ee.get(thread.client_addr, thread.session.id)) {
					thread.session=null;
					close();
					return;
				}
				final var limiter=thread.limiter;
				long invokeTime=System.currentTimeMillis();
				//Notify the endpoint.
				endpoint.onMessage(line);
				if(!limiter.acquire(Token.SLOW_API, (int)(System.currentTimeMillis()-invokeTime))) {
					close();
					return;
				}
			}
			input= new byte[1024];
		
		}
		
	}
	/**Endpoint builders can be used to convert JSPs into websocket endpoints. See the example*/
	public static EndpointBuilder endpointBuilder() {
		return new EndpointBuilder();
	}
	/**Used locally for managing endpoints*/
	public static boolean hasEndpoint(String endpoint) {
		return endpoints.containsKey(endpoint);
	}
	/**Used locally for managing endpoints*/
	public static void removeEndpoint(String endpoint) {
		endpoints.remove(endpoint);
	}
	/**Used locally for managing endpoints*/
	private static void recompileEndpoint(Hydar hydar, String path) {
		hydar.ee.addCompileListener((file)->{
			String path2=file.toString().replace("\\","/");
			String n=path2.substring(path2.lastIndexOf("./")+2);
			if(n.equals(path)) {
				removeEndpoint(path);
				return true;
			}
			return false;
		});
	}
	/**Endpoint builders can be used to convert JSPs into websocket endpoints. See the example*/
	public static void registerEndpoint(String path, EndpointBuilder builder){
		registerAnyEndpoint(path, builder);
	}
	/**Provide an endpoint class, if state that a builder can't handle is needed*/
	public static void registerEndpoint(String path,Class<? extends Endpoint> classObject){
		registerAnyEndpoint(path, classObject);
	}
	/**Endpoint argument must be Class<? extends Endpoint> or EndpointBuilder.*/
	private static void registerAnyEndpoint(String path, Object endpoint){
		//Include possibility of Hydar in its constructor.
		List<Hydar> allPossibleHydars = new ArrayList<>(Hydar.hydars);
		if(HydarEE.lastToCompile != null)
			allPossibleHydars.add(HydarEE.lastToCompile.hydar);
		for(Hydar hydar : allPossibleHydars) {
			if(hydar.ee == HydarEE.lastToCompile) {
				recompileEndpoint(hydar,path);
				if(hydar.config.LOWERCASE_URLS)
					path=path.toLowerCase();
				endpoints.put(path,endpoint);
				return;
			}
		}
		throw new IllegalStateException("This must be called at compile time of a JSP.");
	}
	/**Called when a websocket is opened.*/
	public Endpoint constructEndpoint(String path, HydarWS websocket){
			Object endpoint = endpoints.get(path);
			if(endpoint==null)
				return null;
			Endpoint test;
			if(endpoint instanceof EndpointBuilder epb) {
				test=epb.build(websocket);
			}else {
				Class<?> endpointClass=(Class<?>)endpoint;
				try {
					test = (Endpoint)endpointClass.getConstructors()[0].newInstance(websocket);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | SecurityException e) {
					e.printStackTrace();
					return null;
				}
			}
			return test;
	}
	/**Endpoint builders can be used to convert JSPs into websocket endpoints. See the example*/
	public static class EndpointBuilder{
		private BiConsumer<? super HydarEE.HttpSession, ? super PrintStream> onOpen=null;
		private Consumer<? super PrintStream> onClose=null;
		private BiConsumer<? super String,? super PrintStream> onMessage=null;
		/**One of the possible functional signatures is used*/
		public EndpointBuilder onOpen(Runnable r) {
			onOpen=(x,y)->r.run();
			return this;
		}
		/**One of the possible functional signatures is used*/
		public EndpointBuilder onClose(Runnable r) {
			onClose=x->r.run();
			return this;
		}
		/**One of the possible functional signatures is used*/
		public EndpointBuilder onMessage(Consumer<? super String> r) {
			onMessage=(x,y)->r.accept(x);
			return this;
		}
		/**One of the possible functional signatures is used*/
		public EndpointBuilder onOpen(Consumer<? super PrintStream> r) {
			onOpen=(x,y)->r.accept(y);
			return this;
		}
		/**One of the possible functional signatures is used*/
		public EndpointBuilder onOpen(BiConsumer<? super HydarEE.HttpSession,? super PrintStream> r) {
			onOpen=r;
			return this;
		}
		/**One of the possible functional signatures is used*/
		public EndpointBuilder onClose(Consumer<? super PrintStream> r) {
			onClose=r;
			return this;
		}
		/**One of the possible functional signatures is used*/
		public EndpointBuilder onMessage(BiConsumer<? super String,? super PrintStream> r) {
			onMessage=r;
			return this;
		}
		/**Endpoint builder methods access the underlying connection as a PrintStream.*/
		public Endpoint build(HydarWS ws) {
			PrintStream ps = new PrintStream(ws);
			return new Endpoint(ws) {
				@Override
				public void onOpen() throws IOException {
					onOpen.accept(session,ps);
				}
				@Override
				public void onClose() throws IOException {
					onClose.accept(ps);
				}
				@Override
				public void onMessage(String message) throws IOException {
					onMessage.accept(message,ps);
				}
				
			};
		}
		
	}
	/**
	 * An endpoint represents the backend code a websocket connection
	 * is linked to. Linked to URLs with registerEndpoint().
	 * TODO: move to HydarEE?
	 * */
	public abstract static class Endpoint{
		public HydarWS websocket;
		public String path;
		public String search;
		public HydarEE.HttpSession session;
		public Endpoint(HydarWS websocket) {
			this.websocket=websocket;
			if(websocket!=null) {
				this.session=websocket.thread.session;
				this.path=websocket.path;
				this.search=websocket.search;
			}
		} 
		public HttpSession getSession() {
			return websocket.thread.session;
		}
		public InetAddress getRemoteAddress() {
			return websocket.thread.client_addr;
		}
		public abstract void onOpen() throws IOException;
		public abstract void onClose() throws IOException;
		public abstract void onMessage(String message) throws IOException;
		public final void close() throws IOException {
			websocket.close();
		}
		public final void print(String message) throws IOException{
			websocket.write(message.getBytes());
		}
	}
}
