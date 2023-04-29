package xyz.hydar;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.groupingBy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.naming.NamingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


enum Encoding{
	gzip,deflate,identity;
	public String ext(){
		return this==gzip?".hydar.gz":this==deflate?".hydar.zz":null;
	}
	public DeflaterOutputStream defOS(OutputStream o) throws IOException{
		return this==gzip?new GZIPOutputStream(o):this==deflate?new DeflaterOutputStream(o):null;
	}
	static Encoding compute(String accept) {
		if(accept==null)return identity;
		List<String> encs = Arrays.stream(accept.split(","))
			.map(x->x.split(";",2)[0])
			.map(String::trim)
			.filter(Config.ZIP_ALGS::contains)
			.toList();
		return encs.contains("gzip")||encs.contains("x-gzip")?gzip:
			encs.contains("deflate")?deflate:identity;
	}
}
	/**TODO:retry after(503/429)*/
	
	
class Response{
	static final byte[] CRLF="\r\n".getBytes(ISO_8859_1);
	static final byte[] COLONSPACE=": ".getBytes(ISO_8859_1);
	//TODO: consider array instead of maps since getting headers from responses is rare
	private String responseStatus="200";
	private Map<String,String> headers= new HashMap<>();
	private byte[] data;
	private Resource resource;
	private boolean sendLength=true;
	private boolean sendData=true;
	private boolean chunked;
	private boolean lastChunk;
	private boolean firstChunk;
	Encoding enc=Encoding.identity;
	private int offset;
	private Limiter limiter;
	private OutputStream output;
	private String version="HTTP/1.1";
	private Optional<HStream> hs=Optional.empty();
	private ByteBuffer streamBuffer;
	public long length;
	public Response(int status){
		this(Integer.toString(status));
	}
	public Response(String status){
		status(status);
	}
	public Response limiter(Limiter limiter) {
		this.limiter=limiter;
		return this;
	}
	public Response disableLength() {
		headers.remove("Content-Length");
		this.sendLength=false;
		return this;
	}
	public Response disableData() {
		this.sendData=false;
		return this;
	}
	public Response disableData(boolean should) {
		if(should==false)
			return this;
		else return disableData();
	}
	public Response enableData() {
		this.sendData=true;
		return this;
	}
	public Response data(byte[] data){
		return data(data,0,data.length);
	}
	public Response data(byte[] data, int offset, int length){
		
		this.data=data;
		this.offset=offset;
		this.length=length;
		zip();
		return this;
	}
	public Response data(Resource r){
		this.resource=r;
		if(!r.lengths.containsKey(enc))
			enc=Encoding.identity;
		this.data=r.asBytes(enc);
		this.length=r.lengths.get(enc);
		if(enc!=Encoding.identity)
			headers.put("Content-Encoding",enc.toString());
		if(r.etag!=null && Config.SEND_ETAG)
			headers.put("ETag",r.etag);
		
		return this;
	}
	public Response enc(Encoding enc){
		if(enc!=this.enc && resource!=null||data!=null) {
			throw new UnsupportedOperationException("Cannot zip after adding data");
		}
		this.enc=enc;
		return this;
	}
	public Response header(Map<String,String> headers) {
		headers.forEach(this::header);
		return this;
	}
	public String getHeader(String k){
		return headers.get(k);
	}
	public Response header(String k, int v){
		return header(k,Integer.toString(v));
	}
	public Response header(String k, String v) {
		if(k.equals(":status"))
			responseStatus=k;
		var target = headers;
		String cval;
		if(k.equals("Set-Cookie")&&(cval=headers.get("Set-Cookie"))!=null){
			target.put("Set-Cookie",cval+","+v);
		}else{
			target.put(k,v);
		}
		return this;
	}
	private void zip(){
		if(enc==Encoding.identity)
			return;
		try(BAOS out1= new BAOS(data.length);
			var out = enc.defOS(out1)){
			out.write(data,offset,(int)length);
			out.finish();
			data = out1.buf();
			offset=0;
			if(!chunked || firstChunk)
				headers.put("Content-Encoding",enc.toString());
			this.length=out1.size();
		}catch(IOException e) {
			e.printStackTrace();
			return;
		}
	}public boolean parseRange(String range) {
		//TODO: maybe for(split ;) and collect(;) beforehand
		/**
		 * parse range:
		 * bytes=x-y INCLUSIVE
		 * split by comma and only take first
		 * (endpoints are allowed to only send ranges they want to so only 1 is sent)
		 * */
		if(range!=null) {
			String[] rangeArgs=range.split("=",2);
			if(rangeArgs.length>1&&rangeArgs[0].equals("bytes")) {
				String[] theRange=rangeArgs[1].split("-",2);
				if(theRange.length>1) {
					long start=theRange[0].isBlank()?-1:Long.parseLong(theRange[0]);
					long end=theRange[1].isBlank()?-1:Long.parseLong(theRange[1]);
					if(!applyRange(start,end)) {
						return false;
					}return true;
				}
			}
		}
		//just 200 normally probably
		return true;
		
	}
	public boolean applyRange(long start, long end){
		if(start>=length||end>length)
			return false;
		long realStart,realEnd,realLength=length;
		if(start>=0) {
			offset=(int)start;
			realStart=start;
			if(end==length-1&&start==0) {
				//just 200 normally(full range)
				return true;
			}else if(end>=0) {
				realEnd=end;
				length=end-start+1;
			}else {
				realEnd=length-1;
				length=length-start;
			}
		}else if(end>=0 && end<length-1){
			realStart=length-end;
			realEnd=length;
			offset=(int)realStart;
			length=end;
		}else {
			//just 200 normally("full" range)
			return true;
		}
		status(206);
		header("Content-Range",""+realStart+"-"+realEnd+"/"+realLength);
		return true;
		
	}public Response status(int sc) {
		responseStatus=""+sc;
		return this;
	}
	public Response status(String sc) {
		responseStatus=sc;
		return this;
	}
	public Response firstChunk() {
		lastChunk=false;
		firstChunk=true;
		return this;
	}
	public Response lastChunk() {
		lastChunk=true;
		firstChunk=false;
		return this;
	}
	public Response chunked() {
		chunked=true;
		lastChunk=false;
		firstChunk=false;
		return this;
	}
	public Response version(String version) {
		this.version=version;
		return this;
	}
	public Response output(OutputStream o) {
		this.output=o;
		return this;
	}
	public Response hstream(Optional<HStream> hs) {
		this.hs=hs;
		return this;
	}
	//(http/1.1 write) otherwise use stream.write or something
	void writeHeaders(OutputStream o, Optional<HStream> hs,Limiter limiter) throws IOException{
		//System.out.println(hs.map(x->x.number).orElse(0)+" "+chunked+" "+firstChunk+" "+lastChunk);
		if(chunked && !firstChunk)
			return;
		
		if(hs.isEmpty()) {
			String status=getStatus();
			String version=getVersion();
			System.out.write(("............< "+toString()).getBytes());
			String fl = version+" "+status+" "+HydarUtil.httpInfo(status);
			BAOS baos = new BAOS(256);
			baos.write(fl.getBytes(ISO_8859_1));
			baos.write(CRLF);
			for(var e:headers.entrySet()){
				String k=e.getKey();
				if(k.isEmpty()||k.startsWith(":"))
					continue;
				String v=e.getValue();
				baos.write((k).getBytes(ISO_8859_1));
				baos.write(COLONSPACE);
				if(k.equals("Set-Cookie"))
					for(String s:v.split(","))
						baos.write((s).getBytes(ISO_8859_1));
				else baos.write((v).getBytes(ISO_8859_1));
				baos.write(CRLF);
			}
			baos.write(CRLF);
			limiter.force(Token.OUT,baos.size());
			baos.writeTo(o);
		}else{
			//WRITE TO H
			HStream h=hs.orElseThrow();
			if(!h.canSend()){
				return;
			}
			BAOS j = new BAOS(256);
			final var thread = h.h2.thread;
			final var lock = thread.lock;
			boolean huffman=Hydar.threadCount.get()>Config.MAX_THREADS/2;
			boolean noData=length==0||!this.sendData;
			Frame hf=Frame.of(Frame.HEADERS,h)
					.limiter(limiter)
					.endHeaders()
					.endStream(noData);
			var compressor=h.h2.compressor;
			
			lock.lock();
			try {
				compressor.writeField(j, new Entry(":status",responseStatus), huffman);
				compressor.writeFields(j, headers, huffman);
				hf.withBuffer(h.h2.output(j.size()+9));
				//System.out.println(this+"---->"+HexFormat.of().formatHex(j.buf(),0,j.size()));
				hf.withData(j).writeTo(o,noData);
			}finally { 
				lock.unlock();
			}
		}
	}
	private String getStatus() {
		return responseStatus;
	}
	private String getVersion() {
		return version;
		
	}
	public String getInfo() {
		return HydarUtil.httpInfo(getStatus());
	}
	@Override
	public String toString() {
		return getVersion()+" "+getStatus()+"("+getInfo()+")"+((length!=0&&sendData)?(": "+length+(sendLength?"":"*")+" bytes\n"):"\n");
	}
	//zip here maybe.
	
	
	public void defaults(){
		if(this.sendLength && !this.chunked)
			headers.putIfAbsent("Content-Length",""+length);
		if(Config.SERVER_HEADER.length()>0)
			headers.putIfAbsent("Server",Config.SERVER_HEADER);
		headers.putIfAbsent("Expires","Thu, 01 Dec 1999 16:00:00 GMT");
		headers.putIfAbsent("Referrer-Policy","origin");
		if(Config.SSL_ENABLED&&Config.SSL_HSTS){
			headers.putIfAbsent("Strict-Transport-Security","max-age=63072000; includeSubDomains; preload");
		}
		if(chunked && getVersion().equals("HTTP/1.1"))
			headers.putIfAbsent("Transfer-Encoding","chunked");
		if(Config.SEND_DATE)
			headers.putIfAbsent("Date",HydarUtil.SDF.format(ZonedDateTime.now(ZoneId.of("GMT"))));
		if(getVersion().equals("HTTP/1.1")&&Config.H2_ENABLED&&!Config.SSL_ENABLED){
			headers.putIfAbsent("Alt-Svc","h2c=\":"+Config.PORT+"\"; ma=2592000; persist=1");
		}
	}
	public void write() throws IOException{
		//System.out.println("gz "+enc+" chunked "+chunked+"last "+lastChunk+"first "+firstChunk+" sd"+sendData+" l"+length);
		var output=this.output;
		defaults();
		writeHeaders(output,this.hs,limiter);
		final InputStream stream;  
		var limiter=hs.map(h->h.h2.thread.limiter).orElse(Limiter.UNLIMITER);
		if(sendData&&data==null&&resource!=null) {
			stream = resource.asStream(enc);
			stream.skip(offset);
		}else stream=null;
		if(!sendData || length==0)
			return;
		try(stream){
			if(output==null)
				return;
			if(hs.isEmpty()){
				if(this.length>0) {
					if(data==null) {
						byte[] buffer=new byte[(int) Math.min(length,16384)];
						for(long off=0;off<length;off+=writeStream(output,limiter,stream,buffer, 16384,chunked));
					}else {
						for(int off=0;off<(int)length;off+=writeArr(output,limiter,data,offset+off, 16384,(int)length, chunked));
					}
				}
				//chunk terminator
				if(chunked && lastChunk)
					writeArr(output,limiter,new byte[0],0,0,0,true);
				output.flush();
			}
			else{
				//WRITE TO H
				var h = hs.orElseThrow();
				var thread=h.h2.thread;
				int maxSize=h.h2.remoteSettings[Setting.SETTINGS_MAX_FRAME_SIZE];
				long offset=this.offset;
				long originalSize=length+this.offset;
	
				thread.lock.lock();
				try {
					streamBuffer=h.h2.output(Math.min((int)length,maxSize)+9);
				}finally {
					thread.lock.unlock();
				}
				Frame tmp=Frame.of(Frame.DATA,h)
					.limiter(limiter)
					.lock(thread.lock);
				do{
					int flength=(int)Math.min(originalSize-offset,maxSize);
					//if(resource!=null)
					//System.out.println(length+" --> "+flength);
					boolean endStream=(offset+flength==originalSize)&&(!chunked || lastChunk);
					tmp.endStream(endStream);
					if(data==null) {
						//System.out.println("streamed write");
						tmp.withData(stream,flength,streamBuffer);
					}else{
						//System.out.println("byte[] write");
						tmp.withData(data,(int)offset,flength)
							.withBuffer(streamBuffer);
					}
					tmp.writeTo(output,endStream);
					offset+=flength;
				}while(thread.alive && offset<originalSize && h.canSend());
			}
		}
				//h.thread.alive=false;
	}
	static int writeArr(OutputStream os, Limiter limiter, byte[] buffer, int offset, int length,int max, boolean chunk) throws IOException {
		int len = Math.min(max-offset,length);
		limiter.force(Token.OUT, 9+len);
		if(chunk) {
			os.write(Integer.toHexString(len).getBytes(ISO_8859_1));
			os.write(CRLF);
		}os.write(buffer,offset,len);
		if(chunk)
			os.write(CRLF);
		return len;
	}
	static int writeStream(OutputStream os, Limiter limiter, InputStream stream, byte[] buffer, int length, boolean chunk) throws IOException {
		int len = Math.min(buffer.length,length);
		limiter.force(Token.OUT, 9+len);
		int l=stream.read(buffer,0,len);
		if(l>0){
			if(chunk) {
				os.write(Integer.toHexString(l).getBytes(ISO_8859_1));
				os.write(CRLF);
			}
			os.write(buffer,0,l);
			if(chunk)
				os.write(CRLF);
		}
		return l;
	}
	//TODO:429 is literally never sent
	public static String getErrorPage(String code) {
		return Config.errorPages.getOrDefault(code,Config.errorPages.get("default"));
	}

}
/**a single client(HTTP, WS, or HTTP/2)*/
class ServerThread implements Runnable {
	
	
	public final ReentrantLock lock=new ReentrantLock();
	
	public HydarH2 h2=null;
	public HydarWS ws=null;
	
	public final Socket client;
	public final InetAddress client_addr;
	public final OutputStream output;
	public final BufferedDIS input;
	public volatile HydarEE.HttpSession session=null;
	public volatile boolean alive;
	private boolean h1use=false;
	public final Limiter limiter;
	
	private boolean isHead=false;//INCOMING hstream FIXME:still not threadsafe
	// constructor initializes socket
	//public static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

	public ServerThread(Socket socket) throws IOException{
		this.client = socket;
		this.alive=true;
		var output_ = this.client.getOutputStream();
		var input_ =this.client.getInputStream();
		//this.output_ = output_;
		this.output = output_;
				//new BufferedOutputStream(output_,32768);
		this.client_addr = this.client.getInetAddress();
		this.client.setSoTimeout(Config.HTTP_LIFETIME);
		limiter=Limiter.from(client_addr);
		this.input = new BufferedDIS(input_,limiter,16420);
	}
	
	@Override
	public void run() {
		try(client) {
			this.alive=true;
			while(this.alive){
				//update last used time for session
				try {
					if(h2!=null)
						h2.read();
					else if(ws!=null)
						ws.read();
					else 
						h1Tick();
				}catch(HttpTimeoutException e) {
					if(ws==null)
						sendError("429",Optional.empty());
					return;
				}
			}
		} catch (IOException e) {
			
		}finally {
			this.alive=false;
			limiter.release(Token.PERMANENT_STATE,Config.TC_PERMANENT_THREAD);
			Hydar.threadCount.decrementAndGet();
		}
	}
	public void h1Tick() throws IOException{
		Map<String,String> headers=new HashMap<>();
		//read blocks for 5 seconds
		if(!limiter.acquire(Token.FAST_API,Config.TC_FAST_HTTP_REQUEST)) {
			sendError("429",Optional.empty());
			alive=false;
			return;
		}
		int hb=0;
		String fl="";
		String[] firstLine;
		try {
			//char[] buffer=new char[1024];
			fl=input.readLineCRLFLatin1();
			if(fl==null){
				this.alive=false;	
				return;
			}firstLine = fl.split(" ");
			hb+=fl.length();
			// malformed input
			if (firstLine.length < 3) {
				System.out.println("400 by invalid starter: "+Arrays.asList(firstLine));
				sendError("400",Optional.empty());
				this.alive=false;
				return;
			}
			//tests http version
			if (!(firstLine[2].equals("HTTP/1.0")) && !(firstLine[2].equals("HTTP/1.1")) && !(firstLine[2].equals("HTTP/2.0"))) {

				sendError("505",Optional.empty());
				return;
			}//default
			
			headers.put(":method",firstLine[0]);
			headers.put(":path",firstLine[1]);
			headers.put("::version",firstLine[2]);
			
		}catch (SocketTimeoutException ste) {
			//timed out(close)
			if(!h1use) {
				sendError("408",Optional.empty());
			}
			this.alive=false;
			return;
		}
		if (hb>0) {
			byte[] body=new byte[0];
			int bodyLength=0;
			h1use=true;
			//reads rest of the request(headers)
			String header = fl;
			boolean overflow=false;
			while (header.length()>0&&!(overflow=!limiter.checkBuffer(hb))){
				header=input.readLineCRLFLatin1();
				if(header==null||header.length()==0){
					String cls=headers.get("content-length");
					int cl=(cls==null)?0:Integer.parseInt(cls);
					if(fl.equals("PRI * HTTP/2.0")){
						if (!input.readLineCRLFLatin1().equals("SM")){
							this.alive=false;
							return;
						}input.skip(2);
						break;
					}else if(firstLine[2].equals("HTTP/2.0")) {
						this.alive=false;
						return;
					}
					limiter.forceBuffer(cl);
					body=input.readNBytes(cl);
					bodyLength=body.length;
					if(bodyLength<cl) {
						alive=false;
						return;
					}
					if(bodyLength==0&&headers.containsKey("transfer-encoding")) {
						String[] encodings=headers.get("transfer-encoding").split(",");
						for(String encoding:encodings) {
							if(encoding.equals("chunked")) {
								var chunkStream = new BAOS(256);
								int i=0,length=0;
								do {
									String line=input.readLineCRLFLatin1();
									length=HexFormat.fromHexDigits(line);
									byte[] chunk=input.readNBytes(length);
									if(chunk.length<length) {
										alive=false;
										return;
									}
									chunkStream.write(chunk);
									input.skip(2);
								}while(length>0&&(++i<4096)&&(limiter.checkBuffer(chunkStream.size())));
								body=chunkStream.buf();
								bodyLength=chunkStream.size();
							}else {
								sendError("501",Optional.empty());
								return;
							}
						}
					}
					break;
				}
				int colonIndex = header.indexOf(":");
				if(colonIndex<0||colonIndex>=header.length()-2){
					System.out.println("non-header");
					continue;
				}String name = header.substring(0,colonIndex).toLowerCase();
				String value = header.substring(colonIndex+2);
				headers.put(name, value);
				hb+=header.length()+2;
			}
			if(overflow) {
				sendError("431",Optional.empty());
			}else hparse(headers,Optional.empty(),body,bodyLength);
		}
	}
	private static boolean modifiedAfter(Instant modifInstant, String date) {
		return modifInstant.isAfter(Instant.from(HydarUtil.SDF3.parse(date)));
	}
	//TODO: request/response exchange in its own object => stop passing stream everywhere
	public void hparse(Map<String,String> headers, Optional<HStream> hstream, byte[] body) throws IOException{
		hparse(headers,hstream,body,body.length);
	}
	public void hparse(Map<String,String> headers, Optional<HStream> hstream, byte[] body,int bodyLength) throws IOException{
		isHead=false;
		String method = headers.get(":method");
		String path = headers.get(":path");
		String host = hstream.isPresent() ? headers.get(":authority") : headers.get("host");
		
		if (path.equals("/")) {
			path = Config.HOMEPAGE;
		}
		
		//Config.links.forEach((k,v)->path.replaceAll(k,v));
		for(var s:Config.links.entrySet()){
			path=path.replaceAll(s.getKey(),s.getValue());
		}
		this.isHead=(method.equals("HEAD"));
		String version = headers.get("::version");
		String search = "";
		String[] splitUrl=path.split("\\?",2);
		if(splitUrl.length==2){
			path =splitUrl[0];
			search = splitUrl[1];
		}
		System.out.write((""+client_addr+"> " + method + " " + path + " " + version+"\n").getBytes(StandardCharsets.UTF_8));
		
		if (method.equals("PRI")&&h2==null){
			if(version.equals("HTTP/2.0")&&path.equals("*")){
				h2=new HydarH2(this);
				return;
			}
		}
		//verify authority
		if((host==null ||(!Config.HOST.map(x->x.matcher(host).matches()).orElse(true)))) {
			sendError("400",hstream);
			return;
		}
		if(method.equals("POST")) {
			String ct=headers.get("content-type");
			//TODO: multipart is not supported
			if(ct!=null && ct.startsWith("multipart/")){
				sendError("415",hstream);
				return;
			}
		}
		//check last modified for the file(from hash table)
		String modifiedPath=path.startsWith("/")?path.substring(1):path;
		Resource r = Hydar.resources.get(modifiedPath);
		if(r==null){
			if(!Config.FORBIDDEN_SILENT && Config.FORBIDDEN_REGEX
					.filter(x->x.matcher(modifiedPath).find())
					.isPresent()){
				sendError("403",hstream);
			}else sendError("404",hstream);
			return;//hash table miss
		}
		
		
		boolean upgrade=false;
		String connection = headers.get("connection");
		/**
		set protocol upgrade
		*/
		String protocol=null;
		if(connection!=null){
			connection=connection.toLowerCase();
			if(connection.contains("close"))
				this.alive=false;
			if(connection.contains("upgrade")) {
				upgrade=true;
				protocol = headers.get("upgrade");
			}
		}
		
		/**
		set cookies
		*/
		String sessionID=null;
		Map<String,String> cookies=new HashMap<>();
		String cookieStr=headers.get("cookie");
		if(cookieStr!=null){
			for(String inc:cookieStr.split(";")){
				String[] x = inc.split("=",2);
				if(x.length==2){
					String name = x[0].trim();
					String value = x[1].trim();
					if(name.equals("HYDAR_sessionID")){
						sessionID=value;
					}
					cookies.put(name,value);
				}
			}
		}
			/**
		set encoding
		*/
		path = (path.startsWith("/"))?(path.substring(1)):(path);
		
		if(upgrade) {
			if(h2==null&&!Config.SSL_ENABLED&&protocol.equals("h2c")&&Config.H2_ENABLED) {
				hstream=Optional.of(h2cInit(headers));
				//continue responding to the request on the new stream
				//(it has ID 1)
			}else if(protocol.equals("websocket")&&h2==null && Config.WS_ENABLED){
				/**
				set websocket params
				*/
				String wsKey=headers.get("sec-websocket-key");
				String ext = headers.get("sec-websocket-extensions");
				boolean wsDeflate=false;
				if(ext!=null){
					for(String ex:ext.split(",")){
						String[] params=ex.split(";");
						if(params.length>=1&&params[0].trim().equals("permessage-deflate"))
							wsDeflate=true;
						//maybe: add server max bits etc
					}
				}
				this.wsInit(wsKey,wsDeflate,sessionID,path,search);
				return;
			}else {
				System.out.println("400 by websocket");
				sendError("400",hstream);
				return;//upgrade not allowed(various reasons)
			}
		}
		String encodingStr = headers.get("accept-encoding");

		Encoding enc=Encoding.compute(encodingStr);
		/**
		 * 412 conditions: if-match is present and etag doesn't match, or if-unmodif and it was modified
		 * 304 conditions: if-none-match and match, if-modif and unmodified
		 * if-range: if the precondition it contains fails, ignore ranges and 200
		 * if-match present => if-unmodif ignored
		 * if-nonematch present => if-modif ignored
		 */

		String mime = r.mime;
		Instant resourceInstant = r.modifiedInstant;
		String timestamp = r.formattedTime;
		boolean isJsp=path.endsWith(".jsp");
		if(method.equals("GET")||method.equals("HEAD")) {
			boolean check304 = false;
			String unmodif = headers.get("if-unmodified-since");
			String modif = headers.get("if-modified-since");
			String ifnone = Config.RECEIVE_ETAGS?headers.get("if-none-match"):null;
			String ifmatch = Config.RECEIVE_ETAGS?headers.get("if-match"):null;
			String ifrange = headers.containsKey("range")?headers.get("if-range"):null;
			try {
				//etags
				if(ifmatch!=null&&!HydarUtil.etagMatch(ifmatch,r.etag,true)) {
					sendError("412",hstream);
					return;
				}
				if(ifnone!=null&&HydarUtil.etagMatch(ifnone, r.etag, false)) 
					check304=true;
				//modified
				Instant modifInstant=resourceInstant.minusMillis(1000);
				if (ifmatch==null&&unmodif!=null && (isJsp || modifiedAfter(modifInstant,unmodif))) {
					sendError("412",hstream);
					return;
				}
				if (ifnone==null&&modif!=null && !isJsp && !modifiedAfter(modifInstant,modif))
					check304=true;
				//range
				if(ifrange!=null&&method.equals("GET")) {
					boolean ifrType=ifrange.indexOf("\"")>=0&&ifrange.indexOf("\"")<3;
					if((ifrType && !HydarUtil.etagMatch(ifrange, r.etag, true))||
						(!ifrType && (isJsp||modifiedAfter(modifInstant,ifrange)))) {
						headers.remove("range");
					}
				}
			}catch(DateTimeException e) {
				//fail the precondition if present, otherwise ignore
				if(unmodif!=null||ifmatch!=null) {
					sendError("412",hstream);
					return;
				}
				if(ifrange!=null) {
					headers.remove("range");
				}
			}
			if(check304){
				var resp = newResponse("304",hstream)
					.version(version)
					.disableLength();
				String cc=isJsp?Config.CACHE_CONTROL_JSP:Config.CACHE_CONTROL_NO_JSP;
				if(cc.length()>0){
					resp.header("Cache-Control",cc);
				}
				resp.write();
				return;
			}
		}
		//load the file from hash table
		//hydar hydar hydar hydar
		if (method.equals("GET")||method.equals("POST")||method.equals("PUT")||method.equals("HEAD")) {
			if(!isJsp){
				//not jsp: just send the data
				Response resp = newResponse("200",hstream)
						.enc(enc)
						.data(r);
				long length=r.lengths.get(resp.enc);
				if(length==0) 
					resp.disableLength().status("204");
				if(method.equals("HEAD"))
					resp.disableData();
				
				String range=headers.get("range");
				if(range!=null&&Config.RANGE_NO_JSP&&!resp.parseRange(range)) {
					getError("416",hstream)
						.header("Content-Range","*"+"/"+length)
						.write();
					return;
				}
				
				resp.version(version)
					.header("Last-Modified",timestamp)
					.header("Content-Type",mime)
					.header("Accept-Ranges","bytes");
				
				String cc=Config.CACHE_CONTROL_NO_JSP;
				if(cc.length()>0){
					resp.header("Cache-Control",cc);
				}
				resp.write();
			}else{
				//jsp: execute
				HydarEE.HttpServletRequest request = 
						new HydarEE.HttpServletRequest(headers,body,bodyLength,search,null);
				
				var rs= newResponse(200,hstream).enc(enc);
				var ret = new HydarEE.HttpServletResponse(rs);
				if(!limiter.acquire(Token.SLOW_API, Config.TC_SLOW_JSP_INVOKE)) {
					sendError("429",hstream);
					this.alive=false;
					return;
				}
				boolean fromCookie=true;
				String servletName=path.substring(0,path.indexOf(".jsp"));
				if(HydarEE.jsp_needsSession(servletName)&&(sessionID==null||(session=HydarEE.HttpSession.get(client_addr, sessionID))==null)) {
					fromCookie=false;
					//FIND IT FROM THE URL
					String id=request.getParameter("HYDAR_sessionID");
					if(id==null || (session=HydarEE.HttpSession.get(client_addr, id))==null)
						session=HydarEE.HttpSession.create(client_addr);
				}
				final Optional<HStream> fhs=hstream;//copy
				ret.onReset(()->newResponse(200,fhs));
				request.withSession(session,fromCookie);
				request.withAddr((InetSocketAddress)client.getRemoteSocketAddress());
				ret.withRequest(request);
				//run the stored method
				long invokeTime=System.currentTimeMillis();
				HydarEE.jsp_dispatch(servletName,request, ret);
				if(!limiter.acquire(Token.SLOW_API, (int)(System.currentTimeMillis()-invokeTime))) {
					sendError("429",hstream);
					this.alive=false;
					return;
				}
				Response resp = ret.toHTTP();
				if(resp.length==0 && ret.getStatus()>=400){
					sendError(""+ret.getStatus(),hstream);
					return;
				}
				resp.write();
				/**TODO: range for jsp is useless because preconditions always fail
				*String range=headers.get("range");
				*if(range!=null&&Config.RANGE_JSP&&!rs.parseRange(range)) {
				*	ERR.error("416").add("Content-Range","*"+"/"+r.length).write(output,hs);
				*	return;
				*}
				*rs.add("Accept-Ranges",Config.RANGE_JSP?"bytes":"none");
				*/
			}
							
		}//at some point the HEAD request was here
		else if (method.equals("LINK")|| method.equals("UNLINK") || method.equals("TRACE")||method.equals("CONNECT") ) {
			getError("501",hstream)
			.header("Allow","GET, POST, HEAD, PUT, DELETE, OPTIONS")
			.write();
		} else {
			
			System.out.println("400 by invalid method");
			sendError("400",hstream);
			this.alive=false;
			return;
		}
	}
	protected Response newResponse(int code, Optional<HStream> hs) {
		var build= new Response(""+code).version(h2==null?"HTTP/1.1":"HTTP/2.0").hstream(hs).output(output).limiter(limiter);
		if(isHead)build.disableLength().disableData();
		return build;
	}
	protected Response newResponse(String code, Optional<HStream> hs) {
		var build=new Response(code).version(h2==null?"HTTP/1.1":"HTTP/2.0").hstream(hs).output(output).limiter(limiter);
		if(isHead)build.disableLength().disableData();
		return build;
	}
	private Response getError(String code, Optional<HStream> hs) {
		String error=Response.getErrorPage(code);
		var builder = !isHead?newResponse(code,hs).data(error.getBytes()):newResponse(code,hs);
		return builder;
	}
	private void sendError(String code, Optional<HStream> hs) throws IOException{
		getError(code,hs).write();
	}
	private final Response UPGRADE(String protocol) {
		//TODO: replace all the empty with override
		return newResponse("101",Optional.empty()).version("HTTP/1.1").header("Upgrade",protocol).output(output).header("Connection","Upgrade");
	}
	public HStream h2cInit(Map<String,String> headers) throws IOException{
		UPGRADE("h2c")
			.disableLength()
			.write();
		

		h2=new HydarH2(this);
		byte[] settings = Base64.getUrlDecoder().decode(headers.get("http2-settings"));
		
		BAOS settingsStream=new BAOS(32);
		Frame.of(Frame.SETTINGS).withData(settings).writeTo(settingsStream,false);
		try(var dis=settingsStream.toInputStream()){
			Frame.parse(dis,h2);
		}
		HStream hs = new HStream(StreamState.half_closed_remote,h2,1);
		byte[] magic = input.readNBytes(24);
		if(!Arrays.equals(magic,HydarH2.MAGIC)) {
			this.alive=false;
			sendError("400",Optional.of(hs));
			return null;
		} 
		Frame.parse(input,h2);
		return hs;
	}
	public void wsInit(String wsKey,boolean wsDeflate,String sessionID,String url, String search) throws IOException{
		wsKey+="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		MessageDigest md=null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		}catch(NoSuchAlgorithmException e) {throw new RuntimeException(e);}
		if(sessionID==null ||(session=HydarEE.HttpSession.get(client_addr, sessionID))==null) {
			//FIND IT FROM THE URL
			String id=new HydarEE.HttpServletRequest("",search).getParameter("HYDAR_sessionID");
			if(id==null || (session=HydarEE.HttpSession.get(client_addr, id))==null)
				session=HydarEE.HttpSession.create(client_addr);
		}
		md.update(wsKey.getBytes(ISO_8859_1));
		byte[] digest = md.digest();
		wsKey= Base64.getEncoder().encodeToString(digest);
		wsDeflate = wsDeflate && Config.WS_DEFLATE;
		String ext=null;
		if(wsDeflate){
			ext="permessage-deflate";
		}
		Response resp = UPGRADE("websocket")
			.header("Sec-WebSocket-Accept",wsKey)
			.disableLength();
		if(ext!=null)
			resp.header("Sec-WebSocket-Extensions",ext);
		resp.write();
		ws=new HydarWS(this,url,search,wsDeflate);
		
		
	}
	
}
class Resource{
	public final String etag;
	private final boolean path;
	public final String mime;
	public final Instant fmodif;
	public final Instant modifiedInstant;
	public final String formattedTime;
	public final EnumMap<Encoding,byte[]> encodings=new EnumMap<>(Encoding.class);
	public final EnumMap<Encoding,Path> streamPaths=new EnumMap<>(Encoding.class);
	public final EnumMap<Encoding,Long> lengths=new EnumMap<>(Encoding.class);
	private static final Map<Path,Path> gz_paths = new HashMap<>();
	public Resource(Path p, long fmodif, long modif) throws IOException{
		byte[] fstr;
		/**TODO: logging*/
		this.fmodif = Instant.ofEpochMilli(fmodif);
		modifiedInstant=Instant.ofEpochMilli(modif);
		//long inst=modifiedInstant.toEpochMilli();
		formattedTime=modifiedInstant.atZone(ZoneId.of("GMT")).format(HydarUtil.SDF);
		if(p.toString().endsWith(".swf")) {
			mime="application/x-shockwave-flash";
		}else mime=Objects.requireNonNullElse(Files.probeContentType(p), "application/octet-stream");
		Path old = gz_paths.remove(p);
		if(old!=null)
			Files.deleteIfExists(old);
		if(p.toString().endsWith(".jsp")) {
			path=false;
			etag=null;
			return;
		}long size = Files.size(p);
		long p1 = (""+p.toString()+":"+size).hashCode();
		long p2 = modif;
		byte[] ebytes = ByteBuffer.allocate(8).putLong(31*p1+p2).array();
		
		String et1 = Base64.getUrlEncoder().encodeToString(ebytes);
		this.etag = et1.substring(0,et1.length()-1);
		//System.out.println(etag);
		if(size > Config.CACHE_MAX || !Config.CACHE_ENABLED ||
				!(Config.CACHE_REGEX
						.filter(x->x.matcher(p.toString()).find())
						.isPresent())){
			path=true;
			streamPaths.put(Encoding.identity,p);
			lengths.put(Encoding.identity,size);
			if(Config.ZIP_MIMES.contains(mime)){
				for(String enc:Config.ZIP_ALGS) {
					var enc1=Encoding.valueOf(enc);
					streamPaths.put(enc1,p);
					lengths.put(enc1,zipPath(p,enc1));
				}
			}
		}else{
			path=false;
			fstr=HydarUtil.readAllBytes(p,16384,(int)size);
			if(Config.ZIP_MIMES.contains(mime)){
				for(String enc:Config.ZIP_ALGS) {
					var enc1=Encoding.valueOf(enc);
					byte[] z=HydarUtil.compress(fstr,enc1);
					if(z!=null){
						encodings.put(enc1,z);
						lengths.put(enc1,(long)z.length);
					}
				}
			}
			encodings.put(Encoding.identity,fstr);
			lengths.put(Encoding.identity,(long)fstr.length);
		}
	}
	public byte[] asBytes(Encoding enc) {
		if(!this.path)
			return encodings.get(enc);
		return null;
	}
	public InputStream asStream(Encoding enc) throws IOException{
		if(!this.path)
			return new BAIS(asBytes(enc));
		return new BufferedInputStream(Files.newInputStream(streamPaths.get(enc)),16384);
	}
	public long zipPath(Path p, Encoding enc) throws IOException{
		HydarUtil.mkOptDirs(Hydar.cache);
		Path newPath = Hydar.cache.resolve(etag+enc.ext());
		gz_paths.put(p,newPath);
		try(InputStream in_ = Files.newInputStream(p);
			InputStream in=new BufferedInputStream(in_,32768);
			OutputStream out_ = Files.newOutputStream(newPath);
			OutputStream out = new BufferedOutputStream(out_,32768);
			DeflaterOutputStream out1 = enc.defOS(out)){
			in.transferTo(out1);
		}
		streamPaths.put(enc,newPath);
		return Files.size(newPath);
	}
	public static Resource update(Path p, Path parent, Path root, WatchEvent.Kind<?> kind, long now){
		//check times to decide whether to replace
		if(kind!=null) {
			p=parent.resolve(p).normalize();
		}
		Path q =p.normalize();
		String e=root.relativize(q).toString().replace("\\","/");
		Resource r=null;
		long fmodif=0;
		try {
			if(kind==null) {
				//using polling
			}
			else if(kind == StandardWatchEventKinds.OVERFLOW) {
				System.err.println("overflow :( events lost");
				return null;
			}else if(Config.FORBIDDEN_REGEX.map(x->x.matcher(e+"/").find()).orElse(false)){
				return null;
			}else if(Files.isDirectory(p) && kind != StandardWatchEventKinds.ENTRY_DELETE){
				if(!Hydar.KEYS.containsKey(p)&& HydarUtil.addKey(p,root)) {
					System.out.println("Created folder listener on "+e);
				}
				return null;
			}else if(kind == StandardWatchEventKinds.ENTRY_DELETE) {
				if(Hydar.KEYS.remove(p)==null) {
					Hydar.resources.remove(e);
					HydarEE.servlets.remove(e+".jsp");
					
				}else {
					System.out.println("Removed folder listener on "+e);
					if(Hydar.KEYS.keySet().removeIf(t->t.startsWith(e+"/"))) {
						System.out.println("Subdirectory listeners removed");
					}
				}
				Hydar.resources.keySet().removeIf(x->Path.of(x).startsWith(e+"/"));
				HydarEE.servlets.keySet().removeIf(x->Path.of(x).startsWith(e+"/"));
				System.out.println("File "+e+" was removed from the server directory.");
				return null;
			}
			if((!Config.USE_WATCH_SERVICE||Config.LASTMODIFIED_FROM_FILE)) {
				fmodif = Files.getLastModifiedTime(p).toMillis();
				if((r=Hydar.resources.get(e))!=null) {
					long delta=fmodif-r.fmodif.toEpochMilli();
					if(delta==0)
						return r;
					fmodif = delta<0?now:fmodif;
				}
			}
		}catch(IOException e_) {
			Hydar.resources.remove(e);
			HydarEE.servlets.remove(e+".jsp");
			System.out.println("Failed to verify "+e+" - removing");
			return null;
		}
		System.out.println("Replacing file "+q+"...");
		if(e.endsWith(".jsp")){
			int diag2=0;
			diag2 = HydarEE.compile(q);
			if(diag2>=0)
				System.out.println("Successfully replaced: "+e+", warnings: "+diag2);
		}
		try {
			Resource res = new Resource(q, fmodif,Config.LASTMODIFIED_FROM_FILE?fmodif:now);
			Hydar.resources.put(e,res);
			return res;
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("Failed to replace: "+e);
		return r;
	}
}

//class for main method
public class Hydar { 
	public static final Map<Path,WatchKey> KEYS = new HashMap<>();
	public static Path cache=Path.of("./HydarCompilerCache");
	public static boolean alive=true;
	public static Path dir = Path.of(".");
	public static AtomicInteger threadCount=new AtomicInteger();
	public static Map<String,Resource> resources = new ConcurrentHashMap<>();//file name => resource
	public static WatchService watcher;
	
	//session ID => (attribute name=>value)
	
	public static final ReentrantLock lock = new ReentrantLock();
	
	//TODO: hashing or something at least
	public static String authenticate(String user){
		return HydarEE.HttpSession.tcAuth(user);
	}
	static boolean verifySocket(Socket client) throws IOException{
		Limiter limiter=Limiter.from(client);
		if(threadCount.get()>Config.MAX_THREADS){
			sendErrorNow(client,limiter,"503");
			return false;
		}else if(!limiter.acquire(Token.PERMANENT_STATE, Config.TC_PERMANENT_THREAD)) {
			limiter.release(Token.PERMANENT_STATE, Config.TC_PERMANENT_THREAD); 
			sendErrorNow(client,limiter,"429");
			return false;
		}
		return true;
	}
	static void start301() {
		//TODO: request dispatcher objects should make this less verbose
		HydarUtil.TFAC.newThread(()->{
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY+1);
			try(ServerSocket server301=new ServerSocket(Config.SSL_REDIRECT_FROM)){
				System.out.println("Upgrading HTTP requests from port "+server301.getLocalPort());
				while(alive) {
					try {
					Socket client = server301.accept();
					if(!verifySocket(client))continue;
					ServerThread connection = new ServerThread(client) {
						@Override
						public void hparse(Map<String,String> headers, Optional<HStream> hstream, byte[] body, int bodyLength) throws IOException {
							String path = headers.get(":path");
							String host = hstream.isPresent() ? headers.get(":authority") : headers.get("host");
							String location="https://"+host.split(":",2)[0];
							if(Config.PORT!=443)
								location+=":"+Config.PORT;
							location+=path;
							
							this.newResponse(301,hstream)
								.header("Location",location)
								.data(location.getBytes())
								.write();
							this.alive=false;
						}
					};
					threadCount.incrementAndGet();
					HydarUtil.TFAC.newThread(connection).start();
					}catch(Exception e) {}
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("SSL upgrading server not started");
				return;
			}
		}).start();
	}
	private static void sendErrorNow(Socket client,Limiter limiter,String code) throws IOException{
		if(limiter.acquireNow(Token.FAST_API,Config.TC_FAST_HTTP_REQUEST))
			HydarUtil.TFAC.newThread(()->{
				try(client;OutputStream output = client.getOutputStream()){
					new Response(code).output(output).write();
				}catch(IOException e) {
					return;
				}
			}).start();
	}
	static ServerSocket makeSocket() throws IOException {
		ServerSocket server=null;
		try {//ssl initialization
			if(Config.SSL_ENABLED){
				KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				TrustManager[] tms=null;
				if(!Config.SSL_TRUST_STORE_PATH.isBlank()){
					InputStream tstore = Files.newInputStream(Path.of(Config.SSL_TRUST_STORE_PATH));
					trustStore.load(tstore, Config.SSL_TRUST_STORE_PASSPHRASE.toCharArray());
					tstore.close();
					TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					tmf.init(trustStore);
					tms= tmf.getTrustManagers();
				}
				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				InputStream kstore = Files.newInputStream(Path.of(Config.SSL_KEY_STORE_PATH));
				keyStore.load(kstore, Config.SSL_KEY_STORE_PASSPHRASE.toCharArray());
				kstore.close();
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(keyStore, Config.SSL_KEY_STORE_PASSPHRASE.toCharArray());
				SSLContext ctx = SSLContext.getInstance(Config.SSL_CONTEXT_NAME);
				try{
					ctx.init(kmf.getKeyManagers(), tms, SecureRandom.getInstance("NativePRNGNonBlocking"));
				}catch(NoSuchAlgorithmException e){
					ctx.init(kmf.getKeyManagers(), tms, SecureRandom.getInstanceStrong());
				}
				SSLServerSocketFactory factory = ctx.getServerSocketFactory();
				server = (Config.HOST==null)
					? factory.createServerSocket(Config.PORT,256,InetAddress.getLoopbackAddress())
					: factory.createServerSocket(Config.PORT,256);
				//server.setNeedClientAuth(true);
				((SSLServerSocket)server).setEnabledProtocols(Config.SSL_ENABLED_PROTOCOLS);
				if(Config.H2_ENABLED){
					SSLParameters j=((SSLServerSocket)server).getSSLParameters();
					j.setApplicationProtocols(new String[]{"h2","http/1.1"});
					System.out.println("TLS ALPN Enabled Protocols: "+Arrays.asList(j.getApplicationProtocols()));
					((SSLServerSocket)server).setSSLParameters(j);
				}
			}else{
				server = (Config.HOST==null)
					? new ServerSocket(Config.PORT,256,InetAddress.getLoopbackAddress())
					: new ServerSocket(Config.PORT,256);
			}
			server.setSoTimeout(1000);
		} catch (Exception f) {
			f.printStackTrace();
			System.out.println("Cannot open port " + Config.PORT);
			if(server!=null)server.close();
		}
		return server;
	}
	public static void main(String[] args) throws IOException, NamingException{
		//System.setProperty("java.class.path")
		String configPath=args.length>0?String.join(" ",args):"./hydar.properties";
		Config.load(configPath);
		final ExecutorService ee;
		AtomicBoolean loaded=new AtomicBoolean(false);
		ee = Config.PARALLEL_COMPILE ? newCachedThreadPool() : newSingleThreadExecutor();
		watcher = dir.getFileSystem().newWatchService();
		try{//read files(compile if jsp) to memory

			Files.createDirectories(cache);
			if(Files.isDirectory(cache)){
				Files.walk(cache).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
			List<Path> allFiles = HydarUtil.getFiles(dir);
			long startTime=System.currentTimeMillis();
			
			AtomicInteger errors_=new AtomicInteger();
			AtomicInteger diag_=new AtomicInteger();
			for(Path path:allFiles){
				ee.submit(()->{
					try {
					Path rel=dir.relativize(path).normalize();
					String pathStr = rel.toString().replace("\\","/");
					if(pathStr.endsWith(".jsp")) {
						int status;
						if(!Config.LAZY_COMPILE)
							status = HydarEE.compile(path);
						else status = HydarEE.lazyCompile(path);
						if(status>=0){
							diag_.addAndGet(status);
						}else errors_.incrementAndGet();
						
					}
					long fmodif=0;
					resources.put(pathStr, new Resource(path,
							!Config.USE_WATCH_SERVICE?
							(fmodif=Files.getLastModifiedTime(path).toMillis()):
							startTime,
							Config.LASTMODIFIED_FROM_FILE?fmodif:startTime
							));
					}catch(Exception e) {
						e.printStackTrace();
					}
					//return null;
				});
			}
			long millis=System.currentTimeMillis();
			Runnable join=()->{
				ee.shutdown();
				try {
					ee.awaitTermination(Long.MAX_VALUE,TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {throw new RuntimeException(e);}
				System.out.println("All files loaded after "+(System.currentTimeMillis()-millis)+" ms");
				loaded.set(true);
				
			};
			if(Config.LAZY_FILES)
				HydarUtil.TFAC.newThread(join).start();
			else {
				join.run();
			}
			int errors=errors_.get();
			int diag=diag_.get();
			Path hydr = Path.of(Config.IMPORTANT_PATH);
			
			if(errors==0){
				Files.copy(hydr,System.out);
				if(diag>0){
					System.out.println("\nCompilation successful with "+diag+" warning(s)! Starting server.");
				}else System.out.println("\nCompilation successful! Starting server.");
			}else{
				Files.readString(hydr);
				System.out.println("Compilation unsuccessful with "+errors+" error(s)! Starting server anyways lol");
			}
			System.out.println("Compilation time: "+(System.currentTimeMillis()-startTime)+" ms");
		}catch(IOException ioe){
			ioe.printStackTrace();
			return;	
		}
		
		ServerSocket server = makeSocket();
		
		if(Config.TURN_ENABLED){
			try {
				Class.forName("xyz.hydar.HydarTURN");
			}catch(ClassNotFoundException e) {
				System.out.println("TURN module not found.");
			}
		}
		if(Config.TC_ENABLED){
			try {
				Class.forName("xyz.hydar.HydarLimiter");
			}catch(ClassNotFoundException e) {
				System.out.println("HydarLimiter module not found.");
			}
		}
		//server loop(only ends on ctrl-c)
		long lastUpdate = System.currentTimeMillis();
		long lastSClean = lastUpdate;
		final long scInterval=600_000;
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY+1);
		System.gc();
		if(Config.SSL_ENABLED && Config.SSL_REDIRECT_FROM>=0) {
			start301();
		}
		while (alive) {
			long newTime=System.currentTimeMillis();
			if(newTime-lastSClean>scInterval) {
				HydarEE.HttpSession.clean();
				lastSClean=newTime;
			}
			if(newTime-lastUpdate>Config.REFRESH_TIMER && loaded.get()){
				//check files(recompile as needed)
				lastUpdate = newTime;
				//List<Path> allFiles = HydarUtil.getFiles(dir)
				
				if(Config.USE_WATCH_SERVICE) {
					KEYS.entrySet().stream()
						.flatMap(x->x.getValue().pollEvents().stream().map(y->Map.entry(x,y)))
						.collect(groupingBy(x->x.getValue().context()))
						.forEach((path, entries)->{
							var entry = entries.get(entries.size()-1);
							var evt = entry.getValue();
							Resource.update((Path)path,entry.getKey().getKey(),dir,evt.kind(),newTime);
						});

					for(var k:KEYS.entrySet()) {
						if(!k.getValue().isValid()) {
							HydarUtil.addKey(k.getKey(),Hydar.dir);
							System.out.println("Invalid key for "+k.getKey()+" recreated");
						}
					}
				}
				else {
					Set<Resource> found = new HashSet<>();
					for(Path p:HydarUtil.getFiles(dir)) {
						found.add(Resource.update(p,null,dir,null,newTime));
					}
					if(resources.values().retainAll(found)) {
						HydarEE.servlets.keySet().removeIf(x->!resources.containsKey(x+".jsp"));
						System.out.println("A file was removed from the server directory.");
					}
				}
			}
			try{
				Socket client = server.accept();
				if(!verifySocket(client))continue;
				ServerThread connection = new ServerThread(client);
				threadCount.incrementAndGet();
				HydarUtil.TFAC.newThread(connection).start();
			} catch(SocketTimeoutException ste) {}
			catch(Exception e) {
				e.printStackTrace();
				if(server.isClosed()) {
					try {server.close();}catch(Exception e_) {}
					server = makeSocket();
				}
			}
			

		}
	}

}
