import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.Base64;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.URI;
import java.net.InetAddress;
import java.net.URL;
import java.net.Socket;
import java.net.URLClassLoader;
import java.net.SocketTimeoutException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.File;
import java.lang.Character;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.util.TimeZone;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.Base64.Encoder;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.MessageDigest;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
class ServerThread extends Thread {
	public Socket client = null;
	boolean isWebSocket;
	String wsKey=null;
	String session=null;
	String prevBoard;
	OutputStream output;
	public volatile boolean alive;
	// constructor initializes socket
	public ServerThread(Socket socket) throws IOException{
		this.client = socket;
		this.isWebSocket = false;
		this.alive=true;
		this.output = this.client.getOutputStream();
	}
	@Override
	public void run() {
		try {
			this.alive=true;
			this.client.setSoTimeout(5000);
			SimpleDateFormat s = null;
			InputStream input = this.client.getInputStream();
			InputStreamReader ir = new InputStreamReader(input, Charset.forName("UTF-8"));
			int so=0;
			int size=1024;
			int pls=0;
			String headers = "";
			byte[] sb = new byte[1024];
			byte[] pl = new byte[1024];
			int ping=12;
			String line="";
			prevBoard="";
			String prevUser="";
			while(this.alive){
				boolean gzip=false;
				//update last used time for session
				if(this.session!=null&&Hydar.attr.get(this.session)!=null){
					ConcurrentHashMap<String,String> newAttr = Hydar.attr.get(this.session);
					newAttr.put("lastUsed",""+new Date().getTime());
					Hydar.attr.put(this.session,newAttr);
				}
				if(!this.isWebSocket){
				//read blocks for 5 seconds
					try {
						char[] buffer=new char[1024];
						int l4=ir.read(buffer,0,1024);
						line = new String(buffer);
						headers = new String(line);
					} catch (java.net.SocketTimeoutException ste) {
						//timed out(close)
						output.write(("HTTP/1.1 408 Request Timeout\r\nServer: Large_Hydar/1.1\r\n\r\n" + "408 Request Timeout" + "").getBytes());
						this.alive=false;
						output.close();
						input.close();
						ir.close();
						this.client.close();
						return;
					}
					if (line.length()>0) {
						
						//reads rest of the request(headers)
						this.client.setSoTimeout(1);
						try {
							for (char[] buffer=new char[1024]; (ir.read(buffer,0,1024))>0&&headers.length()<8193; headers += new String(buffer));
						} catch (java.net.SocketTimeoutException seee) {
							//socket times out at end of input(set to 1ms to make it faster, only once per request)
						}
						this.client.setSoTimeout(5000);
						//too long
						if(headers.length()>=8192){
							headers="";
							ir.skip(1000000000);
							output.write("HTTP/1.1 431 Request Header Fields Too Large\r\nServer: Large_Hydar/1.1\r\n\r\n431 Request Header Fields Too Large".getBytes());
							continue;
						}
						String[] heads =headers.split("\r\n");
						String[] firstLine = heads[0].split(" ");
						// malformed input
						if (firstLine.length < 3) {
							output.write(("HTTP/1.1 400 Bad Request\r\nServer: Large_Hydar/1.1\r\n\r\n" + "400 Bad Request" + "").getBytes());
							output.flush();
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
							this.alive=false;
							output.close();
							input.close();
							ir.close();
							this.client.close();
							return;
						}
						//search = request parameters, firstLine[1] = request path
						String search = "";
						if(firstLine[1].indexOf("?")>=0){
							search = firstLine[1].substring(firstLine[1].indexOf("?")+1);
							firstLine[1] = firstLine[1].substring(0,firstLine[1].indexOf("?"));
						}
						System.out.println(""+client.getInetAddress()+"> " + firstLine[0] + " " + firstLine[1] + " " + firstLine[2]);
						//tests http version
						if (!(firstLine[2].equals("HTTP/1.0")) && !(firstLine[2].equals("HTTP/1.1")) && !(firstLine[2].equals("HTTP/2.0"))) {
							output.write(("HTTP/1.1 505 HTTP Version Not Supported\r\nServer: Large_Hydar/1.1\r\nContent-Length: "+Hydar.err_l+"\r\n\r\n"
									+ Hydar.err).getBytes());
							output.flush();
							continue;
						}//default
						if (firstLine[1].equals("/")) {
							firstLine[1] = "/Login.jsp";
						}
						String data="";
						//check last modified for the file(from hash table)
						Date st=null;
						String timestamp = "";
						s = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
						s.setTimeZone(TimeZone.getTimeZone("GMT"));
						st = Hydar.timestamps.get(firstLine[1].substring(firstLine[1].indexOf("/")+1));
						StringBuffer timestampBuffer=new StringBuffer("");
						s.format(st,timestampBuffer,new FieldPosition(0));
						timestamp = timestampBuffer.toString();
						//mime type
						String mime = Files.probeContentType(Paths.get(firstLine[1]));
						if (mime == null)
							mime = "application/octet-stream";
						//load cookies
						this.session=null;
						String modif = null;
						boolean upgrade=false;
						String protocol="";
						ArrayList<String> cookies = new ArrayList<String>();
						ArrayList<String> cookieK = new ArrayList<String>();
						ArrayList<String> cookieV = new ArrayList<String>();
						for (String str : heads) {
							if (str.startsWith("If-Modified-Since: ")) 
								modif = str.substring(str.indexOf(":") + 2);
							if (str.startsWith("Connection: ")) {
								if((str.substring(str.indexOf(":") + 2).toLowerCase().contains("close")))this.alive=false;
								if((str.substring(str.indexOf(":") + 2).toLowerCase().contains("upgrade")))upgrade=true;
							}if (str.startsWith("Upgrade: ")) {
								protocol = str.substring(str.indexOf(":") + 2);
							}if (str.startsWith("Sec-WebSocket-Key: ")) {
								this.wsKey=str.substring(str.indexOf(":") + 2);
							}if (str.startsWith("Cookie: ")) {
										Collections.addAll(cookies, str.substring(str.indexOf(":") + 2).split(";"));
							}if (str.startsWith("Accept-Encoding: ")) 
								if((str.substring(str.indexOf(":") + 2).toLowerCase().contains("gzip")))gzip=true;
						}int cc=0;
						for(String inc: cookies){
							int x = inc.indexOf('=');
							if(x!=-1){
								if(inc.substring(0,x).equals("HYDAR_sessionID")){
									this.session=inc.substring(x+1);
								}
								cookieK.add(inc.substring(0,x));
								cookieV.add(inc.substring(x+1));
								cc++;
							}
						}
						String path = (firstLine[1].startsWith("/"))?(firstLine[1].substring(1)):(firstLine[1]);
						if(data==null){
							zipWrite("HTTP/1.1 404 Not Found\r\nServer: Large_Hydar/1.1\r\nContent-Length: ",Hydar.er(gzip),"\r\n\r\n",Hydar.err,gzip);
							continue;//hash table miss
						}
						if(upgrade&&(!protocol.equals("websocket")||this.session==null||!(firstLine[1].endsWith("HydaRTCSignal.jsp")))){
							zipWrite("HTTP/1.1 400 Bad Request\r\nServer: Large_Hydar/1.1\r\nContent-Length: ",Hydar.er(gzip),"\r\n\r\n",Hydar.err,gzip);
							continue;//upgrade not allowed(various reasons)
						}else if(upgrade){
							try{
								//initialize websocket
								this.isWebSocket=true;
								this.wsKey+="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
								MessageDigest md = MessageDigest.getInstance("SHA-1");
								md.update(this.wsKey.getBytes("UTF-8"), 0, this.wsKey.length());
								byte[] digest = md.digest();
								this.wsKey= Base64.getEncoder().encodeToString(digest);
								output.write(("HTTP/1.1 101 Switching Protocols\r\nServer: Large_Hydar/1.1\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "+this.wsKey+"\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT\r\n\r\n")
											.getBytes());
								Hydar.ws.put(Integer.parseInt(Hydar.attr.get(this.session).get("userid")),this.client.getOutputStream());
								output.flush();
							}catch(Exception e){}
							continue;
						}if(this.session==null||firstLine[1].contains("Verify.jsp")||(Hydar.attr.get(this.session)!=null&&Hydar.attr.get(this.session).get("ip")!=null&&!this.client.getInetAddress().toString().equals(Hydar.attr.get(this.session).get("ip")))){
							//new session: old session doesn't exist, wrong ip, or you relogged
							if(this.session!=null)
								Hydar.attr.remove(this.session);
							ConcurrentHashMap<String,String> tmpAttr = new ConcurrentHashMap<String,String>();
							char[] id = new char[36];
							id[0]='h';id[1]='y';id[2]='d';id[35]='r';
							SecureRandom rng = new SecureRandom();
							for(int i=3;i<id.length-1;i++)
								id[i]=(char)('a'+rng.nextInt(26));
							this.session=new String(id);
							while(Hydar.attr.containsKey(this.session)){
								for(int i=3;i<id.length-1;i++)
									id[i]=(char)('a'+rng.nextInt(26));
								this.session=new String(id);
							}
							tmpAttr.put("lastUsed",""+new Date().getTime());
							tmpAttr.put("ip",this.client.getInetAddress().toString());
							Hydar.attr.put(this.session,tmpAttr);
						}
						if (modif != null) {
							Date ct = null;
							boolean b = false;
							try {
								ct = s.parse(modif);
							} catch (ParseException eeeee) {
								b = true;
							}
							//compares times of last modified and if-modified-since to test for 304(only on GET)
							if (!firstLine[1].endsWith(".jsp")&&!b && (ct.getTime()-st.getTime()>=-1000) && firstLine[0].equals("GET")) {
								output.write(("HTTP/1.1 304 Not Modified\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT\r\nServer: Large_Hydar/1.1\r\n\r\n")
										.getBytes());
								output.flush();
								continue;
							}
						}
						//load the file from hash table
						if(gzip)
							data = Hydar.staticZ.get(path);
						if(!gzip||data==null){
							if(!firstLine[1].endsWith(".jsp"))
								gzip=false;
							data = Hydar.statics.get(path);
						}
						String encoding = (gzip)?"Content-Encoding: gzip\r\n":"Content-Encoding: identity\r\n";
						//get and post return identically, but different implementations are used for later
						if (firstLine[0].equals("GET")||firstLine[0].equals("POST")) {
							if(!firstLine[1].endsWith(".jsp")){
								//not jsp: just send the data
								boolean booled=false;
								for(String x:Hydar.banned){
									if(firstLine[1].contains(x)){
										booled=true;
										zipWrite(
											"HTTP/1.1 403 Forbidden\r\nAllow: GET, POST, HEAD\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT\r\nServer: Large_Hydar/1.1\r\nContent-Length: ",Hydar.er(gzip),"\r\nContent-Type: " + mime
											+ "\r\nLast-Modified: " + timestamp
											+ "\r\n\r\n",Hydar.err,gzip);
											break;
									}
								}if(!booled){
									output.write(("HTTP/1.1 "+((data.length()==0)?"204 No Content":"200 OK")+"\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT\r\nContent-Length: "+data.length()+"\r\n"+encoding+"Content-Type: " + mime
											+ "\r\nLast-Modified: " + timestamp
											+ "\r\n\r\n"+data).getBytes(StandardCharsets.ISO_8859_1));
								}
							}else{
								//jsp: execute
								int i=0;
								String newData="";
								while(data.indexOf("<%@")>-1){
									data=data.substring(data.indexOf("%>")+2);
								}
								if(firstLine[1].endsWith("HydaRTCSignal.jsp")){
									//System.out.println(headers);
								}
								boolean ise=false;
								String re=null;
								if(data.indexOf("<%")>-1){
									try{
										String name = firstLine[1].substring(firstLine[1].indexOf("./")+2);
										name = name.substring(0,name.lastIndexOf("."));
										if(Hydar.attr.get(session)==null)
											Hydar.attr.put(session,new ConcurrentHashMap<String,String>());
										ConcurrentHashMap<String,String> tmpAttr = new ConcurrentHashMap<String,String>(Hydar.attr.get(this.session));
										//run the stored method
										Object[] ret = Hydar.jsp_invoke(name,this.session,search);
										if(ret.length==0){//error
											ise=true;
											newData=Hydar.err;
											data="";
										}else{//parse the object array{whether this attr was changed, value of this attr, html output, redirect}
											@SuppressWarnings("unchecked")ConcurrentHashMap<String,Boolean> ak = (ConcurrentHashMap<String,Boolean>)ret[0];
											@SuppressWarnings("unchecked")ConcurrentHashMap<String,String> av = (ConcurrentHashMap<String,String>)ret[1];
											String h = (String)ret[2];
											String redirect = (String)ret[3];
											ConcurrentHashMap<String,String> tmp = new ConcurrentHashMap<String,String>();
											for(String k:ak.keySet()){
												if(ak.get(k)){
													if(av.get(k)!=null)
														tmp.put(k,av.get(k));
													else tmp.remove(k);
												}else{
													if(Hydar.attr.get(this.session).get(k)!=null)
														tmp.put(k,Hydar.attr.get(this.session).get(k));
													else tmp.remove(k);
												}
											}Hydar.attr.put(this.session,tmp);
											if(redirect==null){
												newData+=h;
											}else{
												re=new String(redirect);
											}
										}
									}catch(Exception e){
										e.printStackTrace();
										try {
											zipWrite("HTTP/1.1 500 Internal Server Error\r\nServer: Large_Hydar/1.1\r\nContent-Length: ",Hydar.er(gzip),"\r\n\r\n",Hydar.err,gzip);
											output.close();
											this.client.close();
											try {
												Thread.sleep(1);
											} catch (InterruptedException ee) {
												Thread.currentThread().interrupt();
											}
											return;
										} catch (IOException eee) {
											//failed to send the error code
											System.out.println("error");
										}
									}
									
									//i++;
								}else{
									newData+=data;
								}
								if(ise){
									zipWrite("HTTP/1.1 500 Internal Server Error\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Length: ",Hydar.er(gzip),"\r\nContent-Type: text/html;charset=ISO-8859-1"
											+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT"+"\r\nSet-Cookie: HYDAR_sessionID="+this.session+"; SameSite=Strict;Secure;HttpOnly\r\n\r\n",Hydar.err,gzip);
								}else if(re==null){
									zipWrite("HTTP/1.1 "+((newData.length()==0)?"204 No Content":"200 OK")+"\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Length: ",-1,"\r\nContent-Type: text/html;charset=ISO-8859-1"
											+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT"+"\r\nSet-Cookie: HYDAR_sessionID="+this.session+"; SameSite=Strict;Secure;HttpOnly\r\n\r\n",newData,gzip);
								}else{output.write(
									("HTTP/1.1 302 Found\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Length: "
											+ 0 + "\r\nLocation: "+re+"\r\nContent-Type: text/html;charset=ISO-8859-1"
											+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT" + "\r\nSet-Cookie: HYDAR_sessionID="+this.session+"; SameSite=Strict;Secure;HttpOnly\r\n\r\n" + "").getBytes(StandardCharsets.ISO_8859_1));
									output.flush();
								}
							}
											
						} else if (firstLine[0].equals("HEAD")) {
							output.write(
									("HTTP/1.1 200 OK\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Length: "
											+ data.length() + "\r\nContent-Type: " + mime
											+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT\r\nLast-Modified: " + timestamp)
													.getBytes(StandardCharsets.ISO_8859_1));
						} else if (firstLine[0].equals("PUT") || firstLine[0].equals("DELETE") || firstLine[0].equals("LINK")
								|| firstLine[0].equals("UNLINK")) {
							output.write(("HTTP/1.1 501 Not Implemented\r\nServer: Large_Hydar/1.1\r\n\r\n501 Not Implemented").getBytes());
							output.flush();
						} else {
							output.write(("HTTP/1.1 400 Bad Request\r\nServer: Large_Hydar/1.1\r\n\r\n400 Bad Request").getBytes());
							output.flush();
						}
						try {
							Thread.sleep(1);
						} catch (InterruptedException ee) {
							Thread.currentThread().interrupt();
						}
					}else this.alive=false;
					
				}else{
					//WEBSOCKET LOOP
					int len=0;
					this.client.setSoTimeout(1000);
					int off=2;
					long length=0;
					ping-=1;
					if(ping<0){//nothing sent for a while
						if(Hydar.attr.get(this.session)!=null&&Hydar.attr.get(this.session).get("userid")!=null&&Hydar.ws.containsKey(Integer.parseInt(Hydar.attr.get(this.session).get("userid"))))
									Hydar.ws.remove(Integer.parseInt(Hydar.attr.get(this.session).get("userid")));
						this.alive=false;
					}else{
						try {
							len = input.read(sb,size-1024,800);
						}catch (java.net.SocketTimeoutException ste) {
							continue;
						}
						size+=len;
						pls+=len;
						sb = Arrays.copyOf(sb,size);
						this.client.setSoTimeout(1);
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
						int tries=3;
						while((len==800||length>(pls-4-off))&&pls<65536){
							try{
								len = input.read(sb,size-1024,800);
								size+=len;
								pls+=len;
								sb = Arrays.copyOf(sb,size);
							}catch(SocketTimeoutException ste2){tries--;if(tries>0)continue;else break;}
						}
						if(pls>=65536){//overflow
							input.skip(1000000000);
							continue;
						}
						this.client.setSoTimeout(1000);
						if(size>0&&sb[so]<0){
							so+=size+len;
							sb = Arrays.copyOf(sb,size);
						}else {
							//input =client.getInputStream();
							System.out.println("else");
							continue;
						}
					}
					
					if(so>0||!this.alive){
						ping=8;
						pls=0;
						if(alive&&so<2){
							//empty
							pl=new byte[1024];
							sb= new byte[1024];
							continue;
						}
						if(((sb[1]&(byte)0x80)>>7)==1){
							//not masked(close)
							System.out.println("E");
							if(Hydar.attr.get(this.session)!=null&&Hydar.attr.get(this.session).get("userid")!=null&&Hydar.ws.containsKey(Integer.parseInt(Hydar.attr.get(this.session).get("userid"))))
								Hydar.ws.remove(Integer.parseInt(Hydar.attr.get(this.session).get("userid")));
							this.alive=false;
						}
						if(alive&&(len+so)<length){
							continue;
						}
						pl =new byte[size];
						size=1024;
						so=0;
						//decode data
						for(int i=0;i<length;i++){
							pl[i]=(byte)((sb[i+off+4])^(sb[off+(i%4)]));
						}
						line = new String(pl,Charset.forName("UTF-8")).trim()+"\n";
						byte op = (byte)(sb[0]&0x0f);
						//System.out.println("OP: "+op);
						if(op == 0x08||sb[0]==0x88||(sb[0]==0xff&&sb[1]==0x00)){
							System.out.println("closed socket.");
							//output.write(pl);
							output.close();
							this.alive=false;
							if(Hydar.attr.get(this.session)!=null&&Hydar.attr.get(this.session).get("userid")!=null&&Hydar.ws.containsKey(Integer.parseInt(Hydar.attr.get(this.session).get("userid")))){
								System.out.println("successfully removed socket");
								Hydar.ws.remove(Integer.parseInt(Hydar.attr.get(this.session).get("userid")));
								this.client.close();
							}
						}
						if(op == 0x09){
							System.out.println("aaa i got pinged");
							sb[0]+=1;
							for(int i=0;i<length;i++){
								sb[i+off+4]=(byte)((sb[i+off+4])^(sb[off+(i%4)]));
							}output.write(sb,0,(int)length+off+4);
							output.flush();
							continue;
						}else{
							String type="";String user=prevUser;String board=prevBoard;
							try{
								type = line.substring(0,line.indexOf("\n"));
								line=line.substring(line.indexOf("\n")+1);
								user = line.substring(0,line.indexOf("\n"));
								line=line.substring(line.indexOf("\n")+1);
								board = line.substring(0,line.indexOf("\n"));
								prevBoard=board;
								prevUser=user;
							}catch(Exception e){
								//e.printStackTrace();
							}
							int b = Integer.parseInt(board);
							if(this.alive==false){
								Hydar.dropUser(Integer.parseInt(user),b,this.session);
								break;
							}
							if(Hydar.attr.get(this.session)==null){
								sb=new byte[1024];
								pl=new byte[1024];
								continue;
							}
							if(!user.equals(Hydar.attr.get(this.session).get("userid"))){
								user=Hydar.attr.get(this.session).get("userid");
								if(user==null){
									sb=new byte[1024];
									pl=new byte[1024];
									continue;
								}
							}
							boolean fail=false;
							Object[] obj;
							try{
								obj = Hydar.jsp_invoke("PermCheck",this.session,"board="+board);
								if(obj.length<4){
									fail=true;
									throw new Exception();
								}else{
									String redir = (String)obj[3];
									if(redir!=null)
										fail=true;
								}
							}catch(Exception e){
								e.printStackTrace();
								if(Hydar.attr.get(this.session)!=null&&Hydar.attr.get(this.session).get("userid")!=null&&Hydar.ws.containsKey(Integer.parseInt(Hydar.attr.get(this.session).get("userid"))))
									Hydar.ws.remove(Integer.parseInt(Hydar.attr.get(this.session).get("userid")));
								this.alive=false;
							}
								
							if(fail){
								sb= new byte[1024];
								pl=new byte[1024];
								continue;
							}
							String toWrite = null;
							int target=Integer.parseInt(user);//self
							if(!type.equals("hydar")){
								System.out.println("not hydar :( type: "+type+" user: "+user);
							}if(Hydar.vc.get(b)==null){
									Hydar.vc.put(b,new ArrayList<Integer>());
									Hydar.vcList.put(b,new ArrayList<String>());
							}
							if(this.alive&&type.equals("hydar")){
								if(Hydar.vc.get(b)==null){
									Hydar.vc.put(b,new ArrayList<Integer>());
									Hydar.vcList.put(b,new ArrayList<String>());
								}
								ArrayList<String> friendlyList=new ArrayList<String>();
								for(String f:Hydar.vcList.get(b))
									friendlyList.add("\""+f+"\"");
								toWrite = "user-list\n"+user+"\n"+b+"\n"+Hydar.vc.get(b).toString()+"\n"+friendlyList.toString();
							}
							else if(type.equals("user-list")){
								ArrayList<String> friendlyList=new ArrayList<String>();
								for(String f:Hydar.vcList.get(b))
									friendlyList.add("\""+f+"\"");
								toWrite = "user-list\n"+user+"\n"+b+"\n"+Hydar.vc.get(b).toString()+"\n"+friendlyList.toString();
							}else if(type.equals("user-join")){
									Hydar.addUser(Integer.parseInt(user),b,this.session);
									target=-1;
									toWrite="";
							}else if(type.equals("user-leave")){
									Hydar.dropUser(Integer.parseInt(user),b,this.session);
									target=-1;
									toWrite="";
							}else{
								//rtc message - just relay it to the target
								line=line.substring(line.indexOf("\n")+1);
								target = Integer.parseInt(line.substring(0,line.indexOf("\n")));
								if(Hydar.vc.get(b).contains(target)){
									toWrite = new String(pl,Charset.forName("UTF-8")).trim();
								}else{
									sb= new byte[1024];
									pl=new byte[1024];
									continue;
								}
							}
							
							if(toWrite!=null){
								/**
								System.out.println("K"+Hydar.ws.keySet());
								System.out.println("V"+Hydar.ws.values());
								*/
								if(target==-1){
									ArrayList<String> friendlyList=new ArrayList<String>();
									for(String f:Hydar.vcList.get(b))
										friendlyList.add("\""+f+"\"");
									for(Integer t2:Hydar.vc.get(b)){
										System.out.println("i wrote something to "+t2+" lol");
										toWrite = "user-list\n"+t2+"\n"+b+"\n"+Hydar.vc.get(b).toString()+"\n"+friendlyList.toString();
										if(Hydar.ws.get(t2)!=null)
										Hydar.wsWrite(toWrite,Hydar.ws.get(t2));
										else{
											this.alive=false;
											Hydar.dropUser(t2,b,this.session);
										}
									}//target=Integer.parseInt(user);
								}else{
									if(Hydar.ws.get(target)!=null)
										Hydar.wsWrite(toWrite,Hydar.ws.get(target));
										else{
											this.alive=false;
											Hydar.dropUser(target,b,this.session);
										}
								}
								//System.out.println("i wrote something to "+target+" lol");
							}
						}
						sb= new byte[1024];
						pl=new byte[1024];
					
					}
					
				}if(!this.alive)break;
			}
				
				if(this.isWebSocket&&Hydar.attr.get(this.session)!=null&&Hydar.attr.get(this.session).get("userid")!=null&&Hydar.ws.containsKey(Integer.parseInt(Hydar.attr.get(this.session).get("userid")))){
						Hydar.ws.remove(Integer.parseInt(Hydar.attr.get(this.session).get("userid")));
						Hydar.dropUser(Integer.parseInt(Hydar.attr.get(this.session).get("userid")),Integer.parseInt(this.prevBoard),this.session);
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException eeee) {
					Thread.currentThread().interrupt();
				}
				output.close();
				input.close();
				ir.close();
				this.client.close();
				return;
					
		} catch (Exception e) {
			//500 is interpreted as "any other error"
			e.printStackTrace();
			try {
				if(!this.isWebSocket){
					OutputStream output = this.client.getOutputStream();
					output.write(
							("HTTP/1.1 500 Internal Server Error\r\nServer: Large_Hydar/1.1\r\n\r\n" + "500 Internal Server Error" + "").getBytes());
					
					output.close();
				}else if(Hydar.attr.get(this.session)!=null&&Hydar.attr.get(this.session).get("userid")!=null&&Hydar.ws.containsKey(Integer.parseInt(Hydar.attr.get(this.session).get("userid")))){
						Hydar.ws.remove(Integer.parseInt(Hydar.attr.get(this.session).get("userid")));
						Hydar.dropUser(Integer.parseInt(Hydar.attr.get(this.session).get("userid")),Integer.parseInt(this.prevBoard),this.session);
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException ee) {
					Thread.currentThread().interrupt();
				}
				this.client.close();
				return;
			} catch (IOException eee) {
				return;
                //eee.printStackTrace();
			}
		}
	}
	public void zipWrite(String headers1, long q, String headers2, String data, boolean doGzip) throws IOException{
		long l1;
		if(q>-1){
			l1=q;
		}else{
			l1=(doGzip)?(Hydar.zipLength(data)):data.length();
		}
		output.write(headers1.getBytes(StandardCharsets.ISO_8859_1));
		output.write((""+l1).getBytes(StandardCharsets.ISO_8859_1));
		output.write(("\r\nContent-Encoding: "+(doGzip?"gzip":"identity")).getBytes(StandardCharsets.ISO_8859_1));
		output.write(headers2.getBytes(StandardCharsets.ISO_8859_1));
		if(doGzip){
			//output.flush();
			GZIPOutputStream out2 = new GZIPOutputStream(output);
			out2.write(data.getBytes(StandardCharsets.ISO_8859_1));
			out2.finish();
		}else{
			output.write(data.getBytes(StandardCharsets.ISO_8859_1));
			output.flush();
		}
	}
}

class HydarStunInstance extends Thread{
	public DatagramSocket server;
	public HydarStunInstance(int port){
		try{
			this.server = new DatagramSocket(port);
			this.server.setReceiveBufferSize(2000);
			this.server.setSoTimeout(5000);
		}catch(Exception e){
				e.printStackTrace();
				return;
		}
	}
	@Override
	public void run() {
		System.out.println("Starting STUN server...");
			try{
				while(true){
					try{
						byte[] d = new byte[200];
						byte[] r = new byte[48];
						DatagramPacket receive = new DatagramPacket(d, 200);
						this.server.receive(receive);
						
						if(!((int)d[0]==0&&(int)d[1]==1))
							continue;
						int length = ((d[2] & 0xff) << 8) | (d[3] & 0xff);
						if(d[4]==0x21&&d[5]==0x12&&d[6]==-92&&d[7]==0x42){
							boolean ipv4=(receive.getAddress().getAddress().length==4);
							r[0]=0x01;
							r[1]=0x01;
							r[2]=0x00;
							if(ipv4)
								r[3]=0x0C;
							else r[3]=0x18;
							for(int i=4;i<20;i++){
								r[i]=d[i];
							}
							r[20]=0x00;
							r[21]=0x20;
							r[24]=0x00;
							if(ipv4)
								r[25]=0x01;
							else r[25]=0x02;
							System.out.println((int)r[25]);
							r[27]=(byte)(((receive.getPort()) & 0xFF)^ 0x12 );
							r[26]=(byte)((((receive.getPort()) & 0xFF00)>>8)^ 0x21);
							int i=0;
							for(i=0;i<receive.getAddress().getAddress().length;i++)
								r[28+i]=(byte)((receive.getAddress().getAddress()[i]) ^ d[4+i]);
							r[22]=0x00;
							r[23]=(byte)(i+4);
							this.server.send(new DatagramPacket(r,28+i,receive.getAddress(),receive.getPort()));
							
						}else{
							//failed
							continue;
						}
					}catch(Exception e){
					}
				}
			}catch(Exception e){
				e.printStackTrace();
				return;
			}
	}
}

class JavaSourceFromString extends SimpleJavaFileObject {
  final String code;

  JavaSourceFromString(String name, String code) {
    super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
    this.code = code;
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return code;
  }
}
//class for main method
public class Hydar {
	public static URLClassLoader ucl;
	public static String err;
	public static int err_l;
	public static long err_zl;
	public static String[] banned;
	private static ArrayList<String> compilerOptions;
	public static volatile ConcurrentHashMap<InetAddress,Integer> ipThreads;//ip => number of threads
	public static volatile ConcurrentHashMap<Integer,OutputStream> ws;//user id => stream(only for websockets)
	public static volatile ConcurrentHashMap<Integer,ArrayList<Integer>> vc;//board id => user id
	public static volatile ConcurrentHashMap<Integer,ArrayList<String>> vcList;//board id => username
	public static volatile ConcurrentHashMap<String,Class> classes;//class name => class
	public static volatile ConcurrentHashMap<String,Date> timestamps;//file name => time
	public static volatile ConcurrentHashMap<String,String> statics;//file name => data
	public static volatile ConcurrentHashMap<String,String> staticZ;//file name => zip data
	public static volatile ConcurrentHashMap<String,ArrayList<String>> htmls;//class name => html strings
	//session ID => (attribute name=>value)
	public static volatile ConcurrentHashMap<String,ConcurrentHashMap<String,String>> attr = new ConcurrentHashMap<String,ConcurrentHashMap<String,String>>();
	public static long zipLength(String data){
		try{
			ByteArrayOutputStream out1= new ByteArrayOutputStream(0);
			GZIPOutputStream out = new GZIPOutputStream(out1);
			out.write(data.getBytes(StandardCharsets.ISO_8859_1));
			out.finish();
			long x = out1.size();
			out1.close();
			out.close();
			return x;
		}catch(IOException e){return -1;}//not possible
	}public static String zipString(String data){
		try{
			ByteArrayOutputStream out1= new ByteArrayOutputStream(0);
			GZIPOutputStream out = new GZIPOutputStream(out1);
			out.write(data.getBytes(StandardCharsets.ISO_8859_1));
			out.finish();
			String x = out1.toString(StandardCharsets.ISO_8859_1);
			float ratio = (float)x.length()/(float)data.length();
			out1.close();
			out.close();
			if(ratio>0.9)return null;
			return x;
		}catch(IOException e){return null;}//not possible
	}
	public static long er(boolean gzip){
		return gzip?err_zl:err_l;
	}
	public static Object[] jsp_invoke(String name, String session, String params){
		try{
			Class c = Hydar.classes.get(name);
			//System.out.println(name.substring(0,name.lastIndexOf("."))+i);
			Method[] m = c.getDeclaredMethods();
			Constructor co = c.getConstructors()[0];
			Object o=co.newInstance();
			if(Hydar.attr.get(session)==null){
				Hydar.attr.put(session,new ConcurrentHashMap<String,String>());
			}
				for(Method meth:m)
					if(meth.getName().equals("jsp_Main"))
						return (Object [])meth.invoke(o,new Object[]{params,Hydar.attr.get(session)});
		}catch(Exception e){
			e.printStackTrace();
		}
		return new Object[]{};
	}
	public static void addUser(Integer user, int b, String session){
		if(!Hydar.vc.get(b).contains(user)){
			ArrayList<Integer> t1 = Hydar.vc.get(b);
			ArrayList<String> t2 = Hydar.vcList.get(b);
			t1.add((Integer)user);
			t2.add(Hydar.attr.get(session).get("username"));
			Hydar.vc.put(b,t1);
			Hydar.vcList.put(b,t2);
		}
	}
	public static void dropUser(Integer user, int b, String session){
		if(Hydar.vc.get(b).contains(user)){
			ArrayList<Integer> t1 = Hydar.vc.get(b);
			ArrayList<String> t2 = Hydar.vcList.get(b);
			t1.remove((Integer)user);
			t2.remove(Hydar.attr.get(session).get("username"));
			Hydar.vc.put(b,t1);
			Hydar.vcList.put(b,t2);
		}
		for(Integer usr:Hydar.vc.get(b)){
			Hydar.wsWrite("user-leave\n"+user+"\n"+b,Hydar.ws.get(user));
		}
	}
	public static void wsWrite(String toWrite, OutputStream o){
		try{
			byte[] ub = toWrite.getBytes(Charset.forName("UTF-8"));
			byte[] w = new byte[ub.length+16];
			w[0]=(byte)0x81;
			int l2 = ub.length;
			int off2=2;
			if(l2>125){
				w[1]=(byte)0b01111110;
				w[2]=(byte)((l2>>8)&0xff);
				w[3]=(byte)(l2&0xff);
				off2=4;
			}else w[1]=(byte)((l2&0xff)&(0x7F));
			//System.out.println(w[1]);
			/**for(int i=off2;i<off2+4;i++){
				w[i]=sb[off+(i-off2)];
			}*/									
			for(int i=0;i<l2;i++){
				w[i+off2]=(byte)((ub[i]));
			}if(o==null)System.out.println("null outputstream!!!");
			o.write(w,0,off2+ub.length);
			o.flush();
		}catch(Exception e){
			e.printStackTrace();
			return;}
	}
	public static int compile(File j){
		try{
			int diag=0;
			String path=j.toPath().toString().replace("\\","/");
			String s = Files.readString(j.toPath(), StandardCharsets.ISO_8859_1);
			ArrayList<String> javas = new ArrayList<String>();
			while(s.indexOf("<%@")>-1){
				s=s.substring(s.indexOf("%>")+2);
			}
			String x_="this.jsp_attr_values=new ConcurrentHashMap<String,String>(jsp_attr);\nthis.jsp_urlParams=jsp_param;\nthis.jsp_attr_set=new ConcurrentHashMap<String,Boolean>();\nfor(String jsp_local_s:jsp_attr.keySet()){\nthis.jsp_attr_set.put(jsp_local_s,false);}\nthis.jsp_urlParams=new String(jsp_param);\nthis.jsp_redirect=null;\nthis.jsp_html=\"\";";
			String e=path.substring(path.lastIndexOf("./")+2,path.lastIndexOf('.'))+".jsp";
			String n=path.substring(path.lastIndexOf("./")+2,path.lastIndexOf('.'));
			String o="private String jsp_urlParams;\nprivate String jsp_redirect;\nprivate String jsp_html;\n";
			String v="private ConcurrentHashMap<String,Boolean> jsp_attr_set;\n";
			String i="private ConcurrentHashMap<String,String> jsp_attr_values;\n";
			String a="private void jsp_SA(Object jsp_arg0, Object jsp_arg1){\nif(jsp_arg1==null){\nthis.jsp_attr_values.remove(jsp_arg0.toString());\nthis.jsp_attr_set.put(jsp_arg0.toString(),true);\nreturn;\n}\nthis.jsp_attr_values.put(jsp_arg0.toString(),jsp_arg1.toString());\nthis.jsp_attr_set.put(jsp_arg0.toString(),true);\n}\n";
			String q="private String jsp_GA(Object jsp_arg0){\nreturn this.jsp_attr_values.get(jsp_arg0.toString());\n}\n";
			String u="private void jsp_OP(Object jsp_arg0){\nthis.jsp_html+=jsp_arg0.toString();\n}\n";
			String a_="private String jsp_GP(Object jsp_arg0){\nif(this.jsp_urlParams.indexOf(jsp_arg0.toString()+\"=\")>=0){\ntry{return java.net.URLDecoder.decode(this.jsp_urlParams.substring(this.jsp_urlParams.indexOf(jsp_arg0.toString()+\"=\")+jsp_arg0.toString().length()+1).split(\"&\")[0], StandardCharsets.UTF_8.name());}catch(Exception e){}}\nreturn null;\n}\n";
			String r_="private void jsp_SR(Object jsp_arg0){\nthis.jsp_redirect=jsp_arg0.toString();\n}\n";
			String t="private void jsp__P(Object jsp_arg0){\nSystem.out.print(jsp_arg0.toString());\n}\n";
			String a__="private void jsp__Pln(Object jsp_arg0){\nSystem.out.println(jsp_arg0.toString());\n}\n";
			ArrayList<String> html = new ArrayList<String>();
			if(e.startsWith("/"))e=e.substring(1);
			if(n.startsWith("/"))n=n.substring(1);
			while(s.indexOf("<%")>-1){
				html.add(s.substring(0,s.indexOf("<%")));
				javas.add(s.substring(s.indexOf("<%")+2,s.indexOf("%>")));
				s=s.substring(s.indexOf("%>")+2);
			}html.add(s);
			htmls.put(n,html);
			//System.out.println(javas);
			int index=0;
			String x__="import java.util.HashMap;\nimport java.nio.charset.StandardCharsets;\nimport java.io.InputStreamReader;\nimport java.net.URLDecoder;\nimport java.util.concurrent.ConcurrentHashMap;\nimport java.sql.Connection;\nimport java.sql.DriverManager;\nimport java.sql.SQLException;\nimport java.sql.ResultSet;\nimport java.sql.Statement;\nimport java.io.BufferedReader;\nimport java.io.InputStream;\npublic class "+n.substring(n.lastIndexOf("/")+1)+"{\npublic "+n.substring(n.lastIndexOf("\\")+1).substring(n.lastIndexOf("/")+1)+"(){\n}\n"+o+v+i+a+q+u+a_+r_+t+a__+"public Object[] jsp_Main(String jsp_param, ConcurrentHashMap<String, String> jsp_attr) {\ntry{\n"+x_;
			//System.out.println(new Date(j.lastModified()));
			//System.out.println(n);
			timestamps.put(e,new Date(j.lastModified()));
			for(String x:javas){
				//System.out.println(path);
				if(htmls.get(n)!=null){
					x__+="\nthis.jsp_OP(\""+htmls.get(n).get(index).replace("\"","\\\"").replace("\r","").replace("\\r","\\\\r").replace("\\n","\\\\n").replace("\n","\\n\"+\n\"")+"\");\n";
				}
				x=x.replace("session.getAttribute","this.jsp_GA");
				x=x.replace("session.setAttribute","this.jsp_SA");
				x=x.replace("System.out.print","this.jsp__P");
				x=x.replace("out.print","this.jsp_OP");
				x=x.replace("request.getParameter","this.jsp_GP");
				x=x.replace("response.sendRedirect","this.jsp_SR");
				x__+=x;
				index++;
			}if(htmls.get(n)!=null){
				x__+="\nthis.jsp_OP(\""+htmls.get(n).get(index).replace("\"","\\\"").replace("\r","").replace("\\r","\\\\r").replace("\\n","\\\\n").replace("\n","\\n\"+\n\"")+"\");\n";
			}
			x__+="}catch(Exception jsp_e){\njsp_e.printStackTrace();return new Object[]{};}\nreturn new Object[]{this.jsp_attr_set,this.jsp_attr_values,this.jsp_html,this.jsp_redirect};\n\n}\n}";
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
			
			StringWriter writer = new StringWriter();
			PrintWriter out = new PrintWriter(writer);
			out.println(x__);
			File cache = new File("./HydarCompilerCache");
			Path target = Paths.get("./HydarCompilerCache/"+n+".class");
			try{//replace class file
			target.toFile().delete();
			}catch(Exception eeeeeeeeee){}
			Files.createDirectories(target.getParent());
			StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
			fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(target.getParent().toFile()));
			try{
			Writer fileWriter = new FileWriter("./HydarCompilerCache/"+n+".java", false);
			fileWriter.write(x__);//creates the java file(not needed, but useful)
			fileWriter.close();
			}catch(Exception eeeeeeeeee){}
			out.close();
			JavaFileObject file = new JavaSourceFromString(n.replace("\\","/"), writer.toString());
			Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
			CompilationTask task = compiler.getTask(null, fileManager, diagnostics, compilerOptions, null, compilationUnits);
			boolean success = task.call();
			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {//warnings and errors
				  diag++;
				  System.out.println(diagnostic.getCode());
				  System.out.println(diagnostic.getKind());
				  System.out.println("line: "+diagnostic.getLineNumber());
				  System.out.println(diagnostic.getSource());
				  System.out.println(diagnostic.getMessage(null));

				}
			if(success){
				File f = new File("./HydarCompilerCache/"+n+".class");
				//load class and update hash table
				try{
					ucl = new URLClassLoader(new URL[]{target.getParent().toFile().toURI().toURL()});
					Class c = ucl.loadClass(n.substring(n.lastIndexOf("\\")+1).substring(n.lastIndexOf("/")+1));
					classes.put(n,c);
				}catch(Exception what){
					what.printStackTrace();
					System.exit(0);
				}
				return diag;
			}else{
				double r = Math.random();
				if(r<0.25)
					System.out.println(e+": Compilation failed. You are fat");
				else if(r<0.50)
					System.out.println(e+": Compilation failed. laugh at this person");
				else if(r<0.75)
					System.out.println(e+": Compilation failed + ratio");
				else
					System.out.println(e+": Compilation failed. more like cringe compilation LMAO");
				return -1;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return -1;
	}		
	public static void afkCheck(){
		for(Integer ai:vc.keySet())
			for(Integer i5:vc.get(ai))
				if(!ws.keySet().contains(i5))
					for(String se:attr.keySet())
						if(attr.get(se).get("userid").equals(""+i5)){
							dropUser(i5,ai,se);
							return;
	}}
	public static void main(String[] args) {
		err="<!DOCTYPE html>";
		err+="<html>";
		err+="<head>";
		err+="<meta charset=\"ISO-8859-1\">";
		err+="<title>Submitting Post... - Hydar</title>";
		err+="<link rel=\"shorcut icon\" href=\"favicon.ico\"/>";
		err+="</head>";
		err+="<body>";
		err+="<script type=\"text/javascript\" src =\"https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js\"></script>";
		err+="<style>";
		err+="	body{";
		err+="		background-image:url('hydarface.png');";
		err+="		background-repeat:no-repeat;";
		err+="		background-attachment:fixed;";
		err+="		background-size:100% 150%;";
		err+="		background-color:rgb(51, 57, 63);";
		err+="		background-position: 0% 50%;";
		err+="	}";
		err+="</style>";
		err+="<style> body{color:rgb(255,255,255); font-family:calibri; text-align:center; font-size:20px;}</style><center>A known error has occurred.";
		err+="<br><br><form method=\"get\" action=\"Logout.jsp\"><td><input type=\"submit\" value=\"Back to login\"></td></form>";
		err+="</body>";
		err+="</html>";
		err_l=err.length();
		err_zl=zipLength(err);
		banned = new String[]{".class",".java",".jar",".bat"};
		compilerOptions = new ArrayList<String>();
		compilerOptions.add("-cp");
		compilerOptions.add("\".;./lib/mysql-connector-java-5.1.49-bin.jar\"");
		compilerOptions.add("-Xlint:deprecation");
		File dir = new File(".");
		timestamps = new ConcurrentHashMap<String,Date>();
		statics = new ConcurrentHashMap<String,String>();
		staticZ = new ConcurrentHashMap<String,String>();
		vc = new ConcurrentHashMap<Integer,ArrayList<Integer>>();
		vcList = new ConcurrentHashMap<Integer,ArrayList<String>>();
		ws = new ConcurrentHashMap<Integer,OutputStream>();
		ipThreads = new ConcurrentHashMap<InetAddress,Integer>();
		int errors=0;
		try{
			Class.forName("com.mysql.jdbc.Driver");
		}catch(Exception exc){
			exc.printStackTrace();
		}
		try{//read files(compile if jsp) to memory
			ArrayList<File> jsp = new ArrayList<File>();
			ArrayList<File> allFiles = new ArrayList<File>();
				try {
					Path root = dir.toPath();
					Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir,
								BasicFileAttributes attrs) {
							return FileVisitResult.CONTINUE;
						}
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
							allFiles.add(file.toFile());
							return FileVisitResult.CONTINUE;
						}
						@Override
						public FileVisitResult visitFileFailed(Path file, IOException e) {
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			for(File f:allFiles){
				String path = f.toPath().toString().replace("\\","/");
				if(path.startsWith("/")||path.startsWith("\\"))
					path=path.substring(1);
				else path=path.substring(path.lastIndexOf("./")+2);
				timestamps.put(path,new Date(f.lastModified()));
				String fstr="";
				try{fstr=Files.readString(f.toPath(), StandardCharsets.ISO_8859_1);}catch(IOException e1){}
				if(path.endsWith(".jsp"))
					jsp.add(f);
				else{
					String z=zipString(fstr);
					if(z!=null)staticZ.put(path,z);
				}
				statics.put(path,fstr);
			}
			File cache = new File("./HydarCompilerCache");
			if(cache.isDirectory()){
				Files.walk(cache.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
			classes = new ConcurrentHashMap<String,Class>();
			htmls = new ConcurrentHashMap<String,ArrayList<String>>();
			new File("./HydarCompilerCache").mkdirs();
			ucl = new URLClassLoader(new URL[]{cache.toURI().toURL()});
			int diag=0;
			for(File j:jsp){
				int status = compile(j);
				if(status>=0){
					diag+=status;
				}else errors++;
			}
			
			if(errors==0){
				File hydr = new File("./lib/Amogus.jar");
				System.out.println(Files.readString(hydr.toPath()));
				if(diag>0){
					System.out.println("Compilation successful with "+diag+" warning(s)! Starting server.");
				}else System.out.println("Compilation successful! Starting server.");
			}else{
				System.out.println("Compilation unsuccessful with "+errors+" error(s)! Starting server anyways lol");
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			return;	
		}
		//checks if a port is specified
		if (args.length == 0) {
			System.out.println("No port specified");
			System.exit(0);
		}
		int port = Integer.parseInt(args[0]);
		//checks if port is valid
		if (port < 1 || port > 65535) {
			System.out.println("Invalid port");
			System.exit(0);
		}
		SSLServerSocket server = null;
		try {//ssl initialization
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			InputStream tstore = Hydar.class.getResourceAsStream("/" + "trust.jks");
			trustStore.load(tstore, "hydarhydar".toCharArray());
			tstore.close();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);
			
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			InputStream kstore = Hydar.class.getResourceAsStream("/" + "identity.jks");
			keyStore.load(kstore, "hydarhydar".toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, "hydarhydar".toCharArray());
			SSLContext ctx = SSLContext.getInstance("TLS");
			try{
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecureRandom.getInstance("NativePRNGNonBlocking"));
			}catch(Exception e){
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecureRandom.getInstanceStrong());
			}
			SSLServerSocketFactory factory = ctx.getServerSocketFactory();
			server = (SSLServerSocket)factory.createServerSocket(port,256);
			//server.setNeedClientAuth(true);
			server.setEnabledProtocols(new String[] {"TLSv1.3"});
		} catch (Exception f) {
			f.printStackTrace();
			System.out.println("Cannot open port " + port);
			return;
		}
		
		HydarStunInstance stun = new HydarStunInstance(3478);
		new Thread(stun).start();
		//server loop(only ends on ctrl-c)
		ArrayList<ServerThread> threads = new ArrayList<ServerThread>(256);
		Date lastUpdate = new Date();
		try{
			server.setSoTimeout(1000);
		}catch(Exception eeeeeee){
			System.out.println("???");
		}while (true) {
			long newTime=new Date().getTime();
			if(newTime-lastUpdate.getTime()>2000){
				//xd
				afkCheck();
				//check sessions
				for(String key:Hydar.attr.keySet()){
					if(newTime-Long.parseLong(Hydar.attr.get(key).get("lastUsed"))>3600000){
						Hydar.attr.remove(key);
					}
				}
				//check files(recompile as needed)
				lastUpdate = new Date();
				ArrayList<String> replaced = new ArrayList<String>();
				ArrayList<File> allFiles = new ArrayList<File>();
				try {
					Path root = dir.toPath();
					Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir,
								BasicFileAttributes attrs) {
							return FileVisitResult.CONTINUE;
						}
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
							if(!file.toString().contains("HydarCompilerCache")&&!file.toString().contains(".java"))
								allFiles.add(file.toFile());
							return FileVisitResult.CONTINUE;
						}
						@Override
						public FileVisitResult visitFileFailed(Path file, IOException e) {
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
				for(File j:allFiles){
					//check times to decide whether to replace
					String path=j.toPath().toString().replace("\\","/");
					String e=path.substring(path.lastIndexOf("./")+2);
					if(e.startsWith("/"))e=e.substring(1);
					if(!j.isDirectory()&&(timestamps.get(e)==null||(timestamps.get(e).getTime()!=j.lastModified()))){
						System.out.println("Replacing file "+e+"...");
						if(timestamps.get(e)!=null&&(timestamps.get(e).getTime()>j.lastModified()))
							j.setLastModified(new Date().getTime());
						timestamps.put(e,new Date(j.lastModified()));
						String fstr="";
						try{fstr=Files.readString(j.toPath(), StandardCharsets.ISO_8859_1);}catch(IOException e1){}
						if(e.endsWith(".jsp")){
							Class temp = classes.get(e);
							ArrayList<String> tempH = htmls.get(e);
							classes.remove(e);
							htmls.remove(e);
							int diag2 = compile(j);
							if(diag2<0){
								if(temp!=null)
									classes.put(e,temp);
								if(tempH!=null)
									htmls.put(e,tempH);
								System.out.println("Failed to replace: "+e);
							}else{
								System.out.println("Successfully replaced: "+e+", warnings: "+diag2);
							}
						}else{String z=zipString(fstr);
						if(z!=null)staticZ.put(e,z);
						}
						statics.put(e,fstr);
						
					}
				}
			}
			Socket client = null;
			
			try {
				client = server.accept();
				//client.setTcpNoDelay(true);
				ipThreads = new ConcurrentHashMap<InetAddress,Integer>();
				//for (ServerThread l:threads){System.out.println(l.isWebSocket);}
				for (int i = 0; i < threads.size(); i++) {
					if (threads.get(i)!=null&&(threads.get(i).alive||threads.get(i).isAlive())&&threads.get(i).client!=null) {
				ipThreads.put(threads.get(i).client.getInetAddress(),(ipThreads.get(threads.get(i).client.getInetAddress())==null)?1:ipThreads.get(threads.get(i).client.getInetAddress())+1);}
				if(threads.get(i)!=null&&!threads.get(i).alive&&!threads.get(i).isAlive()){
						threads.set(i,null);
					}
				}//fix all this garbage
				if(client!=null&&client.getInetAddress()!=null&&ipThreads.get(client.getInetAddress())!=null&&ipThreads.get(client.getInetAddress())>100){
					OutputStream output = client.getOutputStream();
					output.write(("HTTP/1.1 429 Too Many Requests\r\nServer: Large_Hydar/1.1\r\nContent-Length: "+Hydar.err_l+"\r\n\r\n" + Hydar.err).getBytes(StandardCharsets.ISO_8859_1));
					output.flush();
					output.close();
					client.close();
					continue;
				}
				ServerThread connection = new ServerThread(client);
				int alives = 0;
				int index = -1;
				boolean run = false;
				//find dead threads and replace them
				for (int i = 0; i < threads.size(); i++) {
					if (threads.get(i)==null||(!threads.get(i).alive&&!threads.get(i).isAlive())) {
						if(index<0){
							index = i;
							threads.set(i, connection);
						}
					} else
						alives++;
				}
				//all threads are dead -> reset threadpool
				//System.out.println("ALIVE: "+alives+", EXIST: ");
				if (alives == 0&&index==-1) {
					threads = new ArrayList<ServerThread>();
					threads.add(connection);
					index = 0;
					run = true;
					new Thread(threads.get(index)).start();
					continue;
				}
				
				else if(index>-1){
					//at least 1 thread is dead, so just replace it
					run = true;
					threads.set(index,connection);
					new Thread(threads.get(index)).start();
					continue;
				}
				//expand threadpool, or give 505 if already 256+
				if (!run) {
					if (!run && alives < 256) {
						threads.add(connection);
						index = threads.size() - 1;
						run = true;
						new Thread(threads.get(index)).start();
						continue;
					} else {
						try {
							OutputStream output = client.getOutputStream();
							output.write(("HTTP/1.1 505 Service Unavailable\r\nServer: Large_Hydar/1.1\r\nContent-Length: "+Hydar.err_l+"\r\n\r\n" + Hydar.err).getBytes(StandardCharsets.ISO_8859_1));
							output.flush();
							try {
								Thread.sleep(1);
							} catch (InterruptedException ee) {
								Thread.currentThread().interrupt();
							}
							output.close();
							client.close();

						} catch (IOException e) {
							//failed to send error
						}

					}

				}
			} catch (Exception e) {
				//e.printStackTrace();
			}
			

		}
	}

}
