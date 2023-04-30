package xyz.hydar.ee;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import xyz.hydar.ee.HydarEE.HttpSession;

/**A WebSocket context*/
public class HydarWS extends OutputStream{
	//private final boolean CLIENT_NO_CONTEXT_TAKEOVER;
	//private final boolean SERVER_NO_CONTEXT_TAKEOVER;
	//private final int CLIENT_MAX_WINDOW_BITS;
	//private final int SERVER_MAX_WINDOW_BITS;
	//websocket buffers and stuff
	private byte[] sb;
	private int so=0;
	private int size=1024;
	private int pls=0;
	private final String search;
	private final String path;
	private Endpoint endpoint;
	//stuff for websocket compression
	private boolean deflate;
	public int ping=10;
	public ServerThread thread;
	private static final byte[] EMPTY_BLOCK=new byte[]{0x00,0x00,(byte)0xff,(byte)0xff};
	private static final byte[] WS_CLOSE={(byte)(0x80 | 0x08),0};
	private BAOS inflate_baos;
	private InflaterOutputStream inflate_ios;
	public static ConcurrentHashMap<String, Object> endpoints=new ConcurrentHashMap<>();
	private BAOS deflate_baos;
	private DeflaterOutputStream deflate_dos;
	
	public HydarWS(ServerThread thread, String path,String search,boolean deflate) throws IOException{
		
		this.thread=thread;
		thread.client.setSoTimeout(Config.WS_LIFETIME);
		this.path=path;
		this.search=search;
		this.deflate=deflate;
		if(deflate) {
			deflate_baos=new BAOS(256);
			deflate_dos=new DeflaterOutputStream(deflate_baos,new Deflater(Deflater.DEFAULT_COMPRESSION, true),true);
			inflate_baos = new BAOS(64);
			inflate_ios = new InflaterOutputStream(inflate_baos,new Inflater(true));
		}
		sb=new byte[1024];
		if(!hasEndpoint(path)) {
			HydarEE.jsp_invoke(path.substring(0,path.indexOf(".jsp")),thread.session,search);
		}
		endpoint=constructEndpoint(path,this);
		if(endpoint==null) {
			close();
		}else {
			endpoint.onOpen();
		}
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] {(byte)b},0,1);
	}
	@Override
	public void write(byte[] data, int start, int len) throws IOException{
		//System.out.println("id: "+this+" >> ");
		byte first=(byte)0x81;
		thread.lock.lock();
		try {
			if(deflate){
				first|=(byte)0b01000000;
				//first=(byte)0xc1;
				//DeflaterOutputStream out2 = new DeflaterOutputStream(out1,new Deflater(Deflater.NO_COMPRESSION, true));
				deflate_dos.write(data,start,len);
				deflate_dos.flush();
				data=deflate_baos.buf();
				start=0;
				len=deflate_baos.size();
				//truncate empty deflate block if present
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
	@Override
	public void close() throws IOException{
		try {
			if(endpoint!=null) {
				endpoint.onClose();
			}
			super.close();
			if(thread.alive)
				thread.output.write(WS_CLOSE);
		} finally {
			thread.close();
		}
	}
	private int read_() throws IOException{
		int len = thread.input.read(sb,size-1024,800);
		thread.limiter.force(Token.IN,len);
		size+=len;
		pls+=len;
		sb = Arrays.copyOf(sb,size);
		return len;
	}
	static final LongBuffer empty=LongBuffer.allocate(0);
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
		if(--ping==0||!thread.alive){//nothing sent for a while
			System.out.println("L bozi,");
			close();
			return;
		}
		//TODO: this still only corresponds with tcp packets->use input stream instead
		do{
			len=read_();
		}while(len>=0&&(len==800||length>(pls-4-off))&&thread.limiter.checkBuffer(pls));
		if(len<0) {
			close();return;
		}
		int l = (sb[1])&0b01111111;
		if(l==126){
			length=((sb[2]&0xff)<<8)|(sb[3]&0xff);
			off=4;
		}else if(l==127){
			for (int i=2;i<10;i++) {
				length<<=8;
				length|=(sb[i]&0xff);
			}
			off=10;
		}else{
			length=l;
		}
		
		//read length from header
		
		
		if(size>0&&sb[so]<0){
			so+=size+len;
			sb = Arrays.copyOf(sb,size);
		}else {
			//havent read enough
			return;
		}
		
		
		if(so>0){
			ping=8;
			pls=0;
			if(!thread.limiter.acquire(Token.FAST_API, Config.TC_FAST_WS_MESSAGE)) {
				close();
				return;
			}//empty
			if(so==1){
				sb= new byte[1024];
				return;
			}
			//not masked(close)
			if(((sb[1]&(byte)0x80)>>7)==1){
				System.out.println("E");
				close();
				return;
			}
			if((len+so)<length){
				return;
			}
			size=1024;
			so=0;
			//decode data
			byte[] pl=new byte[(int)length];
			unmask(sb,pl,off);
			if(deflate&&((sb[0]&0b01000000)!=0x00)){
				inflate_ios.write(pl,0,pl.length-1);
				//empty DEFLATE block
				if(((sb[off+4]^sb[off])&0b00000001)==0x00){
					int i=pl.length-1;
					inflate_ios.write((sb[i+off+4])^(sb[off+(i%4)]));
					inflate_ios.write(EMPTY_BLOCK);
				}
				//out.write((byte)0x00);
				inflate_ios.flush();
				pl=inflate_baos.toByteArray();
				length = inflate_baos.size();
				inflate_baos.reset();
			}
			byte op = (byte)(sb[0]&0x0f);
			if(op == 0x08||sb[0]==0x88||(sb[0]==0xff&&sb[1]==0x00)){
				System.out.println("closed socket.");
				//output.write(pl);
				close();
				return;
			}else if(op == 0x09){
				//TODO: make this use buffers as well maybe
				System.out.println("aaa i got pinged");
				sb[0]+=1;
				for(int i=0;i<length;i++){
					sb[i+off+4]=(byte)((sb[i+off+4])^(sb[off+(i%4)]));
				}
				thread.output.write(sb,0,(int)length+off+4);
				thread.output.flush();
				return;
			}else{
				if(thread.alive==false){
					close();
					return;
				}
				line = new String(pl,0,(int)length,StandardCharsets.UTF_8);
				//on session expire, end the connection
				if(thread.session==null || thread.session!=HydarEE.HttpSession.get(thread.client_addr, thread.session.id)) {
					thread.session=null;
					close();
					return;
				}
				final var limiter=thread.limiter;
				long invokeTime=System.currentTimeMillis();
				endpoint.onMessage(line);
				if(!limiter.acquire(Token.SLOW_API, (int)(System.currentTimeMillis()-invokeTime))) {
					close();
					return;
				}
			}
			sb= new byte[1024];
		
		}
		
	}
	
	public static EndpointBuilder endpointBuilder() {
		return new EndpointBuilder();
	}
	public static class EndpointBuilder{
		private BiConsumer<? super HydarEE.HttpSession, ? super PrintStream> onOpen=null;
		private Consumer<? super PrintStream> onClose=null;
		private BiConsumer<? super String,? super PrintStream> onMessage=null;
		public EndpointBuilder onOpen(Runnable r) {
			onOpen=(x,y)->r.run();
			return this;
		}
		public EndpointBuilder onClose(Runnable r) {
			onClose=x->r.run();
			return this;
		}
		public EndpointBuilder onMessage(Consumer<? super String> r) {
			onMessage=(x,y)->r.accept(x);
			return this;
		}
		public EndpointBuilder onOpen(Consumer<? super PrintStream> r) {
			onOpen=(x,y)->r.accept(y);
			return this;
		}
		public EndpointBuilder onOpen(BiConsumer<? super HydarEE.HttpSession,? super PrintStream> r) {
			onOpen=r;
			return this;
		}
		public EndpointBuilder onClose(Consumer<? super PrintStream> r) {
			onClose=r;
			return this;
		}
		public EndpointBuilder onMessage(BiConsumer<? super String,? super PrintStream> r) {
			onMessage=r;
			return this;
		}
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
	public static boolean hasEndpoint(String endpoint) {
		return endpoints.containsKey(endpoint);
	}
	public static void removeEndpoint(String endpoint) {
		endpoints.remove(endpoint);
	}
	private static void recompileEndpoint(String path) {
		HydarEE.addCompileListener((file)->{
			String path2=file.toString().replace("\\","/");
			String n=path2.substring(path2.lastIndexOf("./")+2);
			if(n.equals(path)) {
				removeEndpoint(path);
				return true;
			}
			return false;
		});
	}
	public static void registerEndpoint(String path,EndpointBuilder builder){
		recompileEndpoint(path);
		endpoints.put(path,builder);
		
	}
	public static void registerEndpoint(String path,Class<? extends Endpoint> classObject){
		recompileEndpoint(path);
		endpoints.put(path,classObject);
	}
	public static Endpoint constructEndpoint(String path, HydarWS websocket){
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
					return null;
				}
			}
			return test;
	}
}
