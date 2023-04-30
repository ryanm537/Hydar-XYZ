package xyz.hydar.ee;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;


class JavaSourceFromString extends SimpleJavaFileObject {
	private final CharSequence code;
	public JavaSourceFromString(String name, CharSequence code) {
		super(URI.create("hydar:///" + name + Kind.SOURCE.extension),Kind.SOURCE);
		this.code = code;
	}
	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return this.code;
	}
}
class HydarFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>{
	private static Map<String,BAOS> classes=new ConcurrentHashMap<>();
	private final StandardJavaFileManager standard;
	
	public HydarFileManager(StandardJavaFileManager s){
		super(s);
		this.standard=s;
		
	}
	
	@Override
	public void flush(){
		
	}
	
	@Override
	public URLClassLoader getClassLoader(JavaFileManager.Location location){
		//System.out.println(location);
		return new URLClassLoader(new URL[0],HydarEE.class.getClassLoader()){
			@Override
			public Class<?> loadClass(String className) throws ClassNotFoundException{
				try {
					return findClass(className);
					//return super.loadClass(className);
				}catch(ClassNotFoundException e) {
					return super.loadClass(className);
					
				}
			}
			@Override
			protected Class<?> findClass(String className) throws ClassNotFoundException{
				var ret=classes.get(className);
				if(ret==null) {
					return super.findClass(className);
				}else{
					classes.remove(className);
					//classes.entrySet().removeIf(x->x.getKey().startsWith(className+"$"));
					return defineClass(className,ret.buf(),0,ret.size());
				}
				//System.out.println("Class not found: "+className);
				//throw new ClassNotFoundException("HYDAR class loader: could not find "+className);
			} 
		};
	}
	@Override
	public JavaFileObject getJavaFileForOutput(
	JavaFileManager.Location location, String className, Kind kind, FileObject sibling) throws IOException{
		//if(location==StandardLocation.CLASS_OUTPUT || location==StandardLocation.CLASS_PATH) {
		////	return Files.Hydar.cache.resolve(className).normalize()
		//}
		if(kind!=Kind.CLASS||!Config.COMPILE_IN_MEMORY){
			return standard.getJavaFileForOutput(location,className,kind,sibling);
		}
		return new HydarClassObject(className);
	}
	static class HydarClassObject extends SimpleJavaFileObject {
		private final String name;
		private BAOS baos;
		public HydarClassObject(String name) {
			super(URI.create("hydar:///" + name + Kind.CLASS.extension),Kind.CLASS);
			this.name = name;	
		}
		@Override
		public OutputStream openOutputStream(){
			baos=new BAOS(2048);
			classes.put(name,baos);
			return baos;
		}
	}
	
}
public class HydarEE{
	private static Set<Predicate<Path>> compileListeners=new HashSet<>();
	public static Map<String,HttpServlet> servlets = new ConcurrentHashMap<>();//class name => Servlet(jsp)
	
	private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private static final StandardJavaFileManager standard = compiler.getStandardFileManager(null, null, null);
	private static final HydarFileManager manager = new HydarFileManager(standard);
	static {
		ServiceLoader<HttpServlet> loader = ServiceLoader.load(HttpServlet.class);
		for(HttpServlet l:loader) {
			String url=l.RESOURCE_LOCATION();
			if(url==null)url="";
			servlets.put(url,l);
			System.out.println("Loaded servlet "+l.getClass().getCanonicalName()+" with target url "+url);
		}
		System.out.println("Service loader finished.");
	}
	public static void addCompileListener(Predicate<Path> action) {
		compileListeners.add(action);
	}
	public static int lazyCompile(Path p) {
		String e=Hydar.dir.relativize(p).normalize().toString().replace("\\","/");
		String n=e.substring(0,e.length()-4);
		servlets.put(n,new EmptyServlet(p));
		return 0;
	}
	static StringBuilder escapeTrailingQuotes(StringBuilder inner) {
		int qi=inner.length();
		while(qi>0&&inner.charAt(--qi)=='\"');
		int quotes=inner.length()-1-qi;
		inner.setLength(qi+1);
		return inner.append("\\\"".repeat(quotes));
	}
	static URLClassLoader ucl;
	static {
		try {
			ucl=new URLClassLoader(new URL[] {Hydar.cache.toUri().toURL()},HydarEE.class.getClassLoader());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	static String replaceOneLit(String s, int start, int end, String replacement) {
		return s.replaceFirst(Pattern.quote(s.substring(start,end)),Matcher.quoteReplacement(replacement));
	}
	public static int compile(Path p){
		try{
			compileListeners.removeIf(x->x.test(p));
			String path=p.toString().replace("\\","/");
			System.out.write(("Compiling "+path+"\n").getBytes());
			String s = HydarUtil.readString(p,16384);
			List<String> javas = new ArrayList<String>();
			StringBuilder extraImports=new StringBuilder();
			StringBuilder outer=new StringBuilder();
			StringBuilder inner=new StringBuilder();
			var xfac=XMLInputFactory.newInstance();
			int start;
			boolean doesSessions=true;
			while((start=s.indexOf("<%@"))>-1){
				int end=s.indexOf("%>",start);
				if(end<0) throw new IOException("Compilation error: missing %>");
			
				String metadata = new StringBuilder(end-start).append("<").append(s.substring(start+3,end).trim()).append("/>").toString().trim();
				String type=null;
				StringBuilder replacement=new StringBuilder();
				var eventReader=xfac.createXMLEventReader(new StringReader(metadata));
				while(eventReader.hasNext()) {
					XMLEvent evt=eventReader.nextEvent();
					switch(evt.getEventType()) {
					case XMLEvent.START_ELEMENT:
						StartElement sevt=evt.asStartElement();
						type=sevt.getName().toString();
						for(var attr:(Iterable<Attribute>)(()->sevt.getAttributes())) {
							String key=attr.getName().toString();
							String value=attr.getValue();
							switch(type) {
							case "page":
								switch(key) {
								case "import":
									for(String s0:value.split(",")) {
										if(s0.trim().startsWith("javax.servlet")||
												s0.trim().startsWith("jakarta.servlet"))
											continue;
										extraImports.append("import ").append(s0).append(";");
									}
									break;
								case "contentType":
									inner.append("response.setContentType(\"%s\");".formatted(value));
									break;
								case "session":
									doesSessions=Boolean.parseBoolean(value);
									break;
								}
								break;
							case "include":
								if(!"file".equals(key))break;
								Path includePath;
								if(value.startsWith("/")||value.startsWith("\\"))
									includePath=Hydar.dir.resolve(Path.of("."+value));
								else if(Path.of(value).getParent()==null)
									includePath=Hydar.dir.resolve(Path.of("./"+value));
								else
									includePath=Hydar.dir.resolve(p.getParent()).resolve("./"+value).normalize();
								String included=Files.readString(includePath);
								replacement.append(included);
								break;
							case "taglib":
								break;
							}
						}
						break;
					}
				}
				
				s=replaceOneLit(s, start, end+2, replacement.toString());
				//String metadata=s.substring(start+3,end).trim();
				//System.out.println(type+" "+attrs);
				
			}
			String x_="";
			String e=Hydar.dir.relativize(p).normalize().toString().replace("\\","/");
			String n=e.substring(0,e.length()-4);
			String o="";
			String v="";
			String i="";
			String a="";
			String q=e.replace("/","__").replace(".","_");  
			String u="";
			String a_="";
			String r_="";
			String t="";
			String a__="";
			List<String> html = new ArrayList<String>();
			int end=0;
			while((start=s.indexOf("<%",end))>=0){
				html.add(s.substring(end,start));
				end=s.indexOf("%>",start)+2;
				if(end<2||end<start) throw new IOException("Compilation error: "+(end<0?"missing %>":"extra %>"));
				javas.add(s.substring(start+2,end-2));
			}
			html.add(s.substring(end));	
			//System.out.println(javas);
			int index=0;
			//size estimate for buffers
			int size=Stream.concat(javas.stream(),html.stream())
					.mapToInt(String::length)
					.sum() + 512;
			StringBuilder x__=new StringBuilder(size);
			//System.out.println(new Date(j.lastModified()));
			//System.out.println(n);
			Consumer<String> appendHTML = (h)->{
				//text block if multiline, otherwise single line
				if(h.contains("\n")||h.contains("\r")) {
					inner.append("\nout.write(\"\"\"\n").append(h.replace("\\","\\\\"));
					escapeTrailingQuotes(inner).append("\"\"\");");
				}else inner.append("\nout.write(\"")
						.append(h.replace("\\","\\\\").replace("\"","\\\""))
						.append("\");");
			};
			for(String x:javas){
				//System.out.println(path);
				appendHTML.accept(html.get(index++));
				//x__+="\nthis.jsp_OP(\""+html.get(index).replace("\\","\\\\").replace("\"","\\\"").replace("\r","").replace("\n","\\n\"+\n\"")+"\");\n";
				if(x.startsWith("!"))
					outer.append(x,1,x.length());
				else if(x.startsWith("=")) {
					inner.append("out.write(\"\"+(");
					inner.append(x,1,x.length());
					inner.append("));");
				}
				else if(x.startsWith("--")&&x.endsWith("--"))
					continue;else inner.append(x);
			}
			appendHTML.accept(html.get(index));
				//x__+="\nthis.jsp_OP(\""+html.get(index).replace("\\","\\\\").replace("\"","\\\"").replace("\r","").replace("\n","\\n\"+\n\"")+"\");\n";

			x__.append("import java.io.PrintWriter;")
				.append(extraImports)
				.append("public class "+o+v+i+a)
				.append(q)
				.append(" extends xyz.hydar.ee.HydarEE.JspServlet{"+u+a_+r_+t+a__)
				.append(outer);
			x__.append("public void _jspService(xyz.hydar.ee.HydarEE.HttpServletRequest request, xyz.hydar.ee.HydarEE.HttpServletResponse response) {")
				.append("xyz.hydar.ee.HydarEE.HttpSession session = request.getSession();")
				.append("PrintWriter out = response.getWriter();")
				.append("try{\n")
				.append(x_)
				.append(inner);
			x__.append("}catch(Exception jsp_e){\nif(!response.isCommitted())response.sendError(500);jsp_e.printStackTrace();}finally{if(out!=null)out.close();}\n\n}\n}");
			
			
			JavaFileObject file = new JavaSourceFromString(q, x__);
			var compilationUnits = Arrays.asList(file);
			var options = new ArrayList<String>();
			
			options.add("-cp");
			List<String> cp = new ArrayList<>();
			cp.add(System.getProperty("java.class.path"));
			if(!Config.COMPILE_IN_MEMORY)
				cp.add(Hydar.cache.toString());
			
			options.add(String.join(File.pathSeparator,cp));
			//options.add("\""+String.join(File.pathSeparator,Config.CLASSPATH)+"\"");
			//System.out.println(options);
			options.addAll(Config.COMPILER_OPTIONS.stream().filter(x->!x.isBlank()).toList());
			URLClassLoader ucl;
			if(Config.COMPILE_IN_MEMORY) {
				synchronized(compiler) {
					ucl=manager.getClassLoader(null);
				}
			}else {
				
				Path targetPath = Hydar.cache.resolve(q+".class");
				Files.deleteIfExists(targetPath);
				Path parent=targetPath.getParent();
				HydarUtil.mkOptDirs(parent);
				synchronized(compiler) {
					standard.setLocation(StandardLocation.CLASS_OUTPUT, List.of(Hydar.cache.toFile()));
					ucl= new URLClassLoader(new URL[] {Hydar.cache.toUri().toURL()},HydarEE.class.getClassLoader());
							//new URLClassLoader(new URL[] {Hydar.cache.toUri().toURL()},String.class.getClassLoader()) ;
				}
			}
			
			if(Config.CREATE_JAVA_FILES){
				Path targetPath = Hydar.cache.resolve(q+".java");
				HydarUtil.mkOptDirs(targetPath.getParent());
				try{
					Files.writeString(targetPath,x__);//creates the java file(not needed, but useful)
				}catch(IOException eeeeeeeeee){
					System.out.println("Failed to write source: "+q+".java");
					eeeeeeeeee.printStackTrace();
				}
			}
			boolean success=false;
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
			List<Diagnostic<? extends JavaFileObject>> diagList=null;
			synchronized(compiler) {
				var task = compiler.getTask(null, manager, diagnostics, options, null, compilationUnits);
				success = task.call();
				diagList=new ArrayList<>(diagnostics.getDiagnostics());
			}
			//if an error is present, don't print warnings
			String ignoredWarnings="";
			if(diagList.stream().anyMatch(x->x.getKind()==Diagnostic.Kind.ERROR)&& 
				diagList.removeIf(x->(x.getKind()!=Diagnostic.Kind.ERROR)))
					ignoredWarnings=" - "+(diagnostics.getDiagnostics().size()-diagList.size())+" warning(s) skipped due to errors";
			
				
			for (Diagnostic<? extends JavaFileObject> diagnostic : diagList) {//warnings and errors
				  int checkedStart=Math.max(0,(int)diagnostic.getStartPosition());
				  int startPos=Math.max(0,x__.lastIndexOf("\n",checkedStart));
				  int endPos=Math.max(0,x__.indexOf("\n",checkedStart));
				  String line0="^^^^^^^ fix this garbage idiot ('line' "+n+".jsp:"+diagnostic.getLineNumber()+")\n";
				  String line1=diagnostic.getKind()+": "+diagnostic.getCode();
				  String sLine=x__.substring(startPos,endPos).trim();
				  String src=diagnostic.getSource()==null?"":
						  
						  "hydar:/"+((JavaFileObject)(diagnostic.getSource())).getName()+ignoredWarnings;
				 // int maxLen=Math.max(Math.max(Math.max(sLine.length(),line1.length()),line0.length()),src.length());
				  int maxLen=Stream.of(sLine,line1,line0,src)
						  .mapToInt(String::length)
						  .max().orElse(0);
				  char[] border = new char[maxLen];
				  Arrays.fill(border,'=');
				  String borderStr=new String(border);
				  System.err.println(borderStr+"\n"+sLine);
				  System.err.println(line0);
				  System.err.println(line1);
				  System.err.println(diagnostic.getMessage(null)+"\n");
				  System.err.println(src+"\n"+borderStr);

			}
			try{
				synchronized(compiler) {
					if(success){
						//load class and update hash table
							//Hydar.ucl = new URLClassLoader(new URL[]{target.getParent().toFile().toURI().toURL()});
						Class<?> c= ucl.loadClass(q);
						JspServlet servlet=(JspServlet)c.getConstructor().newInstance();
						servlet.doesSessions=doesSessions;
						servlets.put(n,servlet);
						return diagList.size();
					}else{
						double r = ThreadLocalRandom.current().nextDouble();
						if(r<0.25)
							System.err.println(e+": Compilation failed. You are fat");
						else if(r<0.50)
							System.err.println(e+": Compilation failed. laugh at this person");
						else if(r<0.75)
							System.err.println(e+": Compilation failed + ratio");
						else
							System.err.println(e+": Compilation failed. more like cringe compilation LMAO");
						return -1;
					}
				}
			}finally{
				if(Config.COMPILE_IN_MEMORY)
					ucl.close();
			}
		}catch(Exception e){
			System.out.println("Failed to replace "+p.toString());
			e.printStackTrace();
		}
		return -1;
		
	}
	private static final Map<String,Average> estLength=new ConcurrentHashMap<>();
	//TODO request dispatcher
	public static HydarEE.HttpServletResponse jsp_invoke(String name, String query) {
		return jsp_invoke(new HttpServletRequest(name,query));
	}
	public static HydarEE.HttpServletResponse jsp_invoke(String name, HttpSession session, String query) {
		var request=new HttpServletRequest(name,query);
		request.withSession(session, true);
		return jsp_invoke(request);
	}
	public static HydarEE.HttpServletResponse jsp_invoke(HttpServletRequest request){
		String name=request.path.endsWith(".jsp")?
			request.path.substring(0,request.path.lastIndexOf(".")):
			request.path;
		Average avg=estLength.computeIfAbsent(name,x->new Average());
		var resp = new HttpServletResponse(new Response(200), avg.avg());
		resp.withRequest(request);
		jsp_dispatch(name, request, resp);
		return resp;
	}
	//TODO: hide this(possible with request handler obj probably)
	public static boolean jsp_needsSession(String servletName) {
		return ((JspServlet)servlets.get(servletName)).doesSessions;
	}
	public static void jsp_dispatch(String servletName, HttpServletRequest request, HttpServletResponse response){
		var name=servletName;
		var resp=response;
		resp.request=request;
		JspServlet meth = (JspServlet)servlets.get(name);
		if(meth instanceof EmptyServlet lazy) {
			synchronized(meth) {
				compile(lazy.getPath());
				meth=(JspServlet)HydarEE.servlets.get(name);
			}
		}
		meth._jspService(request, resp); 
		estLength.computeIfAbsent(name,x->new Average()).update(resp.baos.size());
	}
	public static class EmptyServlet extends JspServlet{
		private final Path sourcePath;
		public EmptyServlet(Path sourcePath) {
			this.sourcePath=sourcePath;
		}
		public Path getPath() {
			return sourcePath;
		}
		@Override
		public void _jspService(HydarEE.HttpServletRequest request, HydarEE.HttpServletResponse response) {
			
		}
		
	}
	public static interface HttpServlet{
		public String RESOURCE_LOCATION();
		public void service(HttpServletRequest request, HttpServletResponse response) throws IOException;
		
	}
	public static abstract class JspServlet implements HttpServlet{
		boolean doesSessions=true;
		@Override
		public void service(HttpServletRequest request, HttpServletResponse response) {
			_jspService(request, response);
		}
		@Override
		public String RESOURCE_LOCATION() {
			return null;
		}
		public abstract void _jspService(HttpServletRequest request, HttpServletResponse response);
	}
	/**public static class Cookie{
		private final String name;
		private final String value;
		public Cookie(String name,String value) {
			this.name=name;
			this.value=value;
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
	}*/
	public static class HttpServletRequest{
		private final String method;
		private final String path;
		private final Map<String,String> headers;
		private final Map<String,String> params;
		private Map<String,Object> attr;
		private final byte[] body;
		private final String query;
		private HttpSession session;
		private boolean idFromCookie=false;
		private int bodyLength;
		private static final InetSocketAddress LOOPBACK=new InetSocketAddress(InetAddress.getLoopbackAddress(),0);
		private InetSocketAddress addr=LOOPBACK;
		//private Map<String,Cookie> cookies=new HashMap<String,Cookie>();
		static final Context CONTEXT = new Context();
		static final byte[] EMPTY=new byte[0];
		public HttpServletRequest(String servlet, String query){
			this(new HashMap<String,String>(Map.of(":path",servlet)),EMPTY,query);
			
		}
		public HttpServletRequest(String servlet, Map<String,String> queryMap){
			this(new HashMap<String,String>(Map.of(":path",servlet)),EMPTY,0,null,queryMap);
			
		}
		private static Map<String,String> queryParams(String query){
			return Arrays.stream(query.split("&"))
					.map(x->x.split("=",2))
					.collect(
						toUnmodifiableMap(
							x->URLDecoder.decode(x[0],UTF_8),
							x->x.length==1?"":URLDecoder.decode(x[1],UTF_8),
							(x,y)->y
						)
					);
		}
		HttpServletRequest(Map<String,String> headers, byte[] body, int bodyLength, String query, Map<String,String> qmap) {
			this.headers=headers;
			this.method=headers.get(":method");
			String mapPath=headers.get(":path");
			path= mapPath==null ? null : mapPath.split("\\?",2)[0];
			String ct = headers.get("content-type");
			if(ct==null||!method.equals("POST")) {}
			else if(ct.equals("application/x-www-form-urlencoded")) {
				query+="&"+new String(body,StandardCharsets.ISO_8859_1);
			}else if(ct.equals("text/plain")) {
				query+="&"+new String(body,StandardCharsets.ISO_8859_1);
			}
			this.query=query;
			this.body=body;
			this.bodyLength=bodyLength;
			this.params=qmap==null?queryParams(query):qmap;
		}
		public HttpServletRequest(Map<String,String> headers, BAOS body, String query){
			this(headers,body.buf(),body.size(),query,null);
		}
		public HttpServletRequest(Map<String,String> headers, byte[] body, String query){
			this(headers,body,body.length,query,null);
			//
			
			
			/**String cookieStr=headers.get("cookie");
			if(cookieStr!=null){
				for(String inc:cookieStr.split(";")){
					int x = inc.indexOf('=');
					if(x!=-1){
						String name = inc.substring(0,x);//maybe trim
						String value = inc.substring(x+1);
						if(name.trim().equals("HYDAR_sessionID")){
							this.sessionID=value.trim();	
						}
						cookies.put(name.trim(),new Cookie(name.trim(),value.trim()));
					}
				}
			}*/
		}
		//not threadsafe(requests are single thread)
		public Object getAttribute(String str) {
			return attr==null?null:attr.get(str);
		}
		public void removeAttribute(String str) {
			if(attr!=null)attr.remove(str);
		}
		public void setAttribute(String str, Object val) {
			if(attr==null)
				attr=new HashMap<>();
			attr.put(str,val);
		}
		public boolean isRequestedSessionIdFromURL() {
			return session!=null && !idFromCookie;
		}
		public boolean isRequestedSessionIdFromCookie() {
			return session!=null && idFromCookie;
		}
		public boolean isRequestedSessionIdValid() {
			return session!=null && session.valid;
		}
		public Context getServletContext(){
			return CONTEXT;
		}
		public HttpServletRequest withAddr(InetSocketAddress addr) {
			this.addr=addr;
			return this;
		}
		public HttpServletRequest withSession(HttpSession session, boolean fromCookie) {
			this.session=session;
			this.idFromCookie=fromCookie;
			return this;
		}
		public HttpSession getSession() {
			return this.session;
		}
		public String getHeader(String header){
			return headers.get(header);
		}
		public String getMethod(){
			return method;
		}
		public String getQueryString(){
			return query;
		}
		public String getRequestURI(){
			return path.split("\\?",2)[0];
		}
		public String getRemoteAddr() {
			return addr.getAddress().getHostAddress();
		}
		public int getRemotePort() {
			return addr.getPort();
		}
		public String getParameter(String param){
			return params.get(param);
		}
		public InputStream getInputStream(){
			return new BAIS(body,0,bodyLength);
		}
		public Reader getReader(){
			return new InputStreamReader(getInputStream(),UTF_8);
		}
	}
	public static class HttpServletResponse{
		public final PrintWriter out;
		public BAOS baos;
		public Response builder;
		private String contentType="text/html";
		private String characterEncoding="UTF-8";
		private int sc=200;
		public boolean committed=false;
		public boolean scc=false;
		Supplier<Response> onReset;
		private HttpServletRequest request;
		public HttpServletResponse(Response builder) {
			this(builder, 1024);
		}
		public HttpServletResponse(Response builder, int estLength){
			baos = new BAOS(estLength);
			out=new PrintWriter(baos,false,Charset.forName(characterEncoding));
			this.builder=builder;
			setDefaults();
		}
		public void onReset(Supplier<Response> s) {
			onReset=s;
		}
		private void setDefaults() {
			sc=builder.getHeader(":status")==null?200:Integer.parseInt(builder.getHeader(":status"));
			setHeader("Content-Type",contentType);
			setHeader("Expires","Thu, 01 Dec 1999 16:00:00 GMT");
			String cc=Config.CACHE_CONTROL_JSP;
			if(cc.length()>0){
				setHeader("Cache-Control",cc);
			}
			
		}
		public HttpServletResponse withRequest(HttpServletRequest request) {
			this.request=request;
			if(request.method!=null && request.method.equals("HEAD"))
				builder.disableData().disableLength();
			HttpSession session;
			if((session=request.getSession())!=null) {
				String cookieAge=session.cookieTtl>=0?";Max-Age="+(session.cookieTtl/1000):"";
				builder.header("Set-Cookie","HYDAR_sessionID="+session.id+";Path=/;SameSite=Strict;"+(Config.SSL_ENABLED?"Secure":"")+cookieAge);
				if(Config.TURN_ENABLED){
					builder.header("Set-Cookie","HYDAR_turnUser="+session.id.substring(15,24)+";Path=/;SameSite=Strict;"+(Config.SSL_ENABLED?"Secure":"")+cookieAge);
					builder.header("Set-Cookie","HYDAR_turnCred="+session.tc+";Path=/;SameSite=Strict;"+(Config.SSL_ENABLED?"Secure":"")+cookieAge);
				}
			}
			return this;
		}
		public String getContentType(){
			return contentType;
		}
		public String getCharacterEncoding(){
			return characterEncoding;
		}
		public void setContentType(String c){
			this.contentType=c;
			setHeader("Content-Type",c);
		}
		public void setCharacterEncoding(String e){
			this.characterEncoding=e;
			contentType=contentType.split(";")[0]+";charset="+e;
		}
		public void sendRedirect(String location){
			resetBuffer();
			setStatus(302);
			setHeader("Location",location);
			out.print(location);
		}
		public void setHeader(String name, String value){
			builder.header(name,value);
		}
		public void setIntHeader(String name, int value){
			builder.header(name,""+value);
		}
		public void setDateHeader(String name, long value){
			builder.header(name,HydarUtil.SDF.format(Instant.ofEpochMilli(value)));
		}
		public int getIntHeader(String name){
			return Integer.parseInt(getHeader(name));
		}
		public long getDateHeader(String name){
			return Instant.from(HydarUtil.SDF3.parse(getHeader(name))).toEpochMilli();
		}
		public int getBufferSize() {
			return baos.buf().length;
		}
		public void setBufferSize(int size) {
			if(size>baos.buf().length) {
				baos=new BAOS(size);
				baos.write(baos.buf(),0,baos.size());
			}
		}
		private Response commit() {
			if(!committed) {
				builder.status(sc);
				if(!Config.ZIP_MIMES.contains(getContentType().split(";")[0].trim()))
					builder.enc(Encoding.identity);
			}
			committed=true;
			out.flush();
			builder.data(baos.buf(),0,baos.size());
			return builder;
		}
		//called to start stream
		//==>if not committed yet, set start flag
		public void flushBuffer() throws IOException {
			builder.chunked();
			if(!committed)
				builder.firstChunk();
			commit().write();
			baos.reset();
		}
		//called to receive the actual response
		//[or last chunk] => flush can never return a last chunk
		public Response toHTTP() {
			if(committed)
				builder.lastChunk();
			return commit();
		}
		public boolean isCommitted() {
			return committed;
		}
		public void reset() {
			resetBuffer();
			if(onReset!=null)builder=onReset.get();
			else builder=new Response(200);
			sc=200;
			setDefaults();
		}
		public void resetBuffer(){
			if(isCommitted())throw new IllegalStateException("Already committed response");
			out.flush();
			baos.reset();
		}
		public String getHeader(String header) {
			String value=builder.getHeader(header);
			return value==null ? null: 
				header.equals("Set-Cookie")?value.split(",")[0]:value;
		}
		public String encodeRedirectURL(String url) {
			return encodeURL(url.isEmpty()?request.getRequestURI():url);
		}
		public String encodeURL(String url) {
			if(request.idFromCookie || request.getSession()==null)
				return url;
			String id=request.getSession().id;
			
			if(!url.startsWith("/")) {
				url=request.getRequestURI().substring(0,request.getRequestURI().lastIndexOf("/")+1)+url;
			}
			if(url.indexOf("?")<0) {
				return url+"?HYDAR_sessionID="+id;
			}else return (url.endsWith("&")?url:url+"&")+"HYDAR_sessionID="+id;
		}
		public void sendError(int sc){
			resetBuffer();
			setStatus(sc);
		}
		public int getStatus(){
			return sc;
		}
		public void setStatus(int sc){
			this.sc=sc;
			scc=true;
		}
		public OutputStream getOutputStream(){
			return baos;
		}
		public PrintWriter getWriter(){
			return out;
		}
		public ByteArrayOutputStream getBuffer() {
			return baos;
		}
	}
	public static class HttpSession{
		public static final Map<String,HttpSession> map= new ConcurrentHashMap<>();
		private final Map<String,Object> attr= new ConcurrentHashMap<>();
		public final String id=id();
		public volatile boolean isNew=true;
		public volatile long ttl=2_592_000_000l;//server sided lifetime
		public volatile long cookieTtl=-1;//cookie lifetime, -1=session
		public volatile long lastUsed=System.currentTimeMillis();
		public final String tc = Config.TURN_ENABLED?tc():null;
		public volatile boolean valid=true;
		private final InetAddress addr;

		static String tc(){
			return HydarUtil.noise(16);
		}
		static String id(){
			String id;
			do{
				id=HydarUtil.noise(32);
			}while(map.containsKey(id));
			return id;
		}
		public static void clean() {
			long now=System.currentTimeMillis();
			for(HttpSession v:map.values()) {
				if(v.ttl>0 && now-v.lastUsed>v.ttl) {
					v.invalidate();
				}
			}
			map.values().stream().collect(Collectors.groupingBy(x->x.addr))
				.values().stream().filter(x->x.size()>64).forEach(x->
					x.stream().sorted(Comparator.comparingLong(s->s.lastUsed))
						.limit(x.size()-64)
						.forEach(HttpSession::invalidate)
				);
		}
		public String getId() {
			lastUsed=System.currentTimeMillis();
			return id;
		}
		public static String tcAuth(String user) {
			for(String key:map.keySet()){
				if(key.substring(15,24).equals(user))
					return map.get(key).tc;
			}
			return null;
		}
		public Context getServletContext() {
			return HttpServletRequest.CONTEXT;
		}
		public Object getAttribute(String k) {
			lastUsed=System.currentTimeMillis();
			return attr.get(k);
		}
		public void removeAttribute(String k) {
			lastUsed=System.currentTimeMillis();
			attr.remove(k);
		}
		public void setAttribute(String k, Object v) {
			lastUsed=System.currentTimeMillis();
			attr.compute(k,(ki,vi)->v);
		}
		public void invalidate() {
			//System.out.println("Invalidating "+id);
			
			valid=false;
			map.remove(id);
		}
		public int getMaxInactiveInterval() {
			lastUsed=System.currentTimeMillis();
			return (int)(ttl/1000);
		}
		public void setMaxInactiveInterval(int interval) {
			lastUsed=System.currentTimeMillis();
			cookieTtl=interval*1000l;
			ttl=interval<0?2592000000l:interval*1000l;
		}
		public static HttpSession get(String id) {
			
			return get(InetAddress.getLoopbackAddress(),id);
		}
		public static HttpSession create(InetAddress addr) {
			return new HttpSession(addr);
		}
		public static HttpSession get(InetAddress addr, String id) {
			HttpSession ret=map.get(id);
			if(ret!=null && (!ret.addr.isLoopbackAddress()||addr.isLoopbackAddress())) {
				ret.isNew=false;
				ret.lastUsed=System.currentTimeMillis();
				return ret;
			}else return null;
		}
		private HttpSession(InetAddress addr) {
			//count.computeIfAbsent()
			this.addr=addr;
			map.put(id,this);
		}
	}
	public static class Context{
		private Map<String,Object> attr;
		Map<String,String> init;
		Context(){
			attr=new ConcurrentHashMap<String,Object>();
		}
		public void setAttribute(String k, Object v){
			if(v!=null)
				attr.put(k,v);
			else attr.remove(k);
		}
		public Object getAttribute(String k){
			return attr.get(k);
		}
		public String getRealPath(String path) {
			return !path.startsWith("/") ? null :
				Hydar.dir.resolve("."+path).normalize().toString();
		}
		public String getInitParameter(String name) {
			return init.get(name);
		}
		public InputStream getResourceAsStream(String path){
			try {
				return !path.startsWith("/") ? null :
					Files.newInputStream(Hydar.dir.resolve("."+path));
			} catch (IOException e) {
				return null;
			}
		}
		public URL getResource(String path) throws MalformedURLException {
			if(!path.startsWith("/"))
				throw new MalformedURLException("must start with '/'");
			return Hydar.dir.resolve("."+path).normalize().toFile().toURI().toURL();
		}
		public Set<String> getResourcePaths(String path) {
			if(!path.startsWith("/"))return null;
			try(var files=Files.walk(Hydar.dir.resolve("."+path),1)){
				return files.map(Path::normalize)
						.map(x->x.toFile().isDirectory()?x+"/":x)
						.map(x->"/"+x.toString().replace("\\","/"))
						.collect(Collectors.toSet());
			} catch (IOException e) {
				return null;
			}
		}
	}
}