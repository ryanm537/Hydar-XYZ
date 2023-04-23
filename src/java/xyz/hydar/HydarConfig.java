package xyz.hydar;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

/**TODO:dynamic config*/
/**TODO:replace @categories with Context.[thing] etc*/
class Config{
	public static Map<String,String> config = new HashMap<>();
	public static Map<String,String> macros = new HashMap<>();
	public static int PORT = 8080;
	private static String CACHE_DIR_PATH = "./HydarCompilerCache";
	public static String IMPORTANT_PATH = "./bots/Amogus.jar";
	public static boolean SSL_ENABLED = false;
	public static String SSL_TRUST_STORE_PATH, SSL_TRUST_STORE_PASSPHRASE, SSL_KEY_STORE_PATH, SSL_KEY_STORE_PASSPHRASE;
	public static String SSL_CONTEXT_NAME = "TLS";
	public static List<String> CLASSPATH=new ArrayList<>(List.of(System.getProperty("java.class.path")));
	public static String[] SSL_ENABLED_PROTOCOLS = new String[]{"TLSv1.2","TLSv1.3"};
	public static boolean TURN_ENABLED=false;
	public static int TURN_PORT=3478;
	public static int SSL_REDIRECT_FROM=-1;
	public static boolean CREATE_JAVA_FILES=false;
	public static int REFRESH_TIMER=2000;
	public static int MAX_THREADS=256;
	public static boolean USE_WATCH_SERVICE=true;
	public static boolean FORBIDDEN_SILENT=true;
	public static int CACHE_MAX=1024000;
	public static Optional<Pattern> HOST=Optional.empty();//empty=anything allowed
	public static Optional<Pattern> FORBIDDEN_REGEX=Optional.empty();//empty=anything allowed
	public static Optional<Pattern> CACHE_REGEX=Optional.of(Pattern.compile(".*"));//empty=anything allowed 
	public static boolean CACHE_ENABLED=true;
	public static List<String> ZIP_ALGS=List.of("gzip","deflate");
	public static Set<String> ZIP_MIMES = Set.of("text/html", "text/css", "text/plain", "text/xml", "text/x-component",
			"text/javascript", "application/x-javascript", "application/javascript", "application/json",
			"application/manifest,json", "application/vnd.api,json", "application/xml", "application/xhtml,xml",
			"application/rss,xml", "application/atom,xml", "application/vnd.ms-fontobject", "application/x-font-ttf",
			"application/x-font-opentype", "application/x-font-truetype", "image/svg,xml", "image/x-icon",
			"image/vnd.microsoft.icon", "font/ttf", "font/eot", "font/otf", "font/opentype");
	public static List<String> COMPILER_OPTIONS = new ArrayList<>();
	public static boolean PARALLEL_COMPILE=false;
	public static boolean COMPILE_IN_MEMORY=true;
	
	public static boolean H2_ENABLED=true;
	public static int H2_WINDOW_ATTEMPTS=8;
	public static int H2_WINDOW_TIMER=1000;
	public static int H2_HEADER_TABLE_SIZE=4096;
	public static int H2_MAX_CONCURRENT_STREAMS=256;
	public static int H2_MAX_FRAME_SIZE=16384;
	public static int H2_MAX_HEADER_LIST_SIZE=8192;
	
	public static int HTTP_LIFETIME=5000;
	
	public static boolean SEND_ETAG=true;
	public static boolean SEND_DATE=true;
	public static boolean RANGE_JSP = false;
	public static boolean RANGE_NO_JSP = true;
	public static boolean LASTMODIFIED_FROM_FILE = false;
	public static boolean RECEIVE_ETAGS=true;
	public static String HOMEPAGE="/index.html";
	
	public static boolean WS_ENABLED=true;
	public static boolean WS_DEFLATE=true;
	public static String SERVER_HEADER="Large_Hydar/2.0";
	public static boolean SSL_HSTS=true;
	
	public static String CACHE_CONTROL_JSP="no-cache";
	public static String CACHE_CONTROL_NO_JSP="public, max-age=604800, must-revalidate";

	public static boolean TC_ENABLED=false;
	public static Map<String,String> links = new HashMap<>();
	
	public static int H2_LIFETIME=30000;
	public static String H2_HPACK_TREE_STRATEGY="MAP";
	public static String H2_HPACK_TABLE_STRATEGY="MAP";
	public static int WS_LIFETIME=15000;
	
	public static int TC_FAST_HTTP_REQUEST=50;
	public static int TC_FAST_WS_MESSAGE=100;
	public static int TC_FAST_H2_FRAME=5;
	public static int TC_PERMANENT_THREAD=100;
	public static int TC_PERMANENT_H2_STREAM=10;
	public static int TC_MAX_BUFFER=1024 * 1024;
	public static int TC_SLOW_JSP_INVOKE=100;
	
	public static String TC_PERMANENT_STATE, TC_IN, TC_OUT, TC_SLOW_API, TC_FAST_API;
	
	public static String WEB_ROOT = ".";
	public static boolean LAZY_COMPILE=false;
	public static boolean LAZY_FILES=false;
	public static boolean TRY_UNSAFE_WATCH_SERVICE=true;
	public static Map<String,Response> errorPages=new HashMap<>();
	private static String[] multi(String input){
		List<String> ret = new ArrayList<>();
		for(String e:input.split(",")){
			int start = e.indexOf("\"");
			int end = e.lastIndexOf("\"");
			String inner="";
			if(start<0||end<0||start==end)
				inner=e;
			else inner = e.substring(start,end);
			inner=inner.trim();
			if(!inner.isEmpty())
				ret.add(inner.trim());
		}
		return ret.toArray(new String[0]);
	}
	public static void link(String k, String v){
		links.put(k,v);
	}
	private static Map<Long,Long> tasks(String s){
		if(s==null)return Map.of();
		String[] tasks=s.split(";");
		Map<Long,Long> values=new HashMap<>();
		for(String task:tasks) {
			String[] c=task.split("/");
			
			values.put(l(c[1]),l(c[0]));
		}
		return values;
	}
	public static void loadResources(Map<String,Map<String,String>> origin) throws NamingException {
		Map<String,Object> finals=new HashMap<>();
		origin.forEach((k,v)->{
			String name=k;
			if(v.get("name")!=null)
				name=v.remove("name");
			String factory = v.get("factory");
			String type = v.get("type");
			
			
			try {
				Object ds = HydarDataSource.builder().properties(v).build();
				if(ds==null) {
					if(factory!=null) {
						v.remove("factory");
						v.remove("type");
						ds = HydarUtil.getObject(factory,type,v);
					}else {
						System.out.println("Not loading config resource "+name+": no ObjectFactory class found");
						return;
					}
				}
				//Object ds = getObject(factory,type,v);
				if(ds!=null) {
					System.out.println("Resource loader: Loaded "+name+" of type "+ds.getClass());
					System.out.println("to load:\n[type] hydar = ([type])new InitialContext().lookup(\"java:comp/env/"+name+"\");");
					finals.put(name,ds);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		var comp=new InitialContext() {
			final Map<String,Object> lookup=Map.copyOf(finals);
			@Override
			public Object lookup(String name) throws NamingException{
				return lookup.get(name);
			}
		};
	
		var init=new InitialContext() {
			@Override
			public Object lookup(String name) throws NamingException{
				if(name.equals("java:comp/env")) {
					return comp;
				}else if(name.startsWith("java:comp/env/")) {
					return comp.lookup(name.split("java:comp/env/")[1]);
				}
				return null;
			}
		};
		NamingManager.setInitialContextFactoryBuilder(env1->env2->init);
	}
	public static void load(String configPath) {
		try{
			System.out.println("Loading config from "+configPath+"...");
			//cfg.removeIf(s->(s.startsWith("#")||s.trim().length()==0));
			int state=0;
			Map<String,Map<String,String>> rsrc = new HashMap<>();
			for(String s:HydarUtil.lines(Path.of(configPath))){
				if(s.startsWith("#")||s.trim().length()==0)
					continue;
				switch (s.trim()) {
					case "@ContextParams":
						state=1;
						continue;
					case "@Links":
						state=2;
						continue;
					case "@Resources":
						state=3;
						continue;
					case "@ErrorPages":
						state=4;
						continue;
				}
				String[] split=s.split("=",2);
				if(split.length<2)continue;
				String k=split[0].trim();
				String v=split[1].trim();
				if(state==2&&(split=s.split("=>",2)).length==2){
					k=split[0].trim();
					v=split[1].trim();
					Config.link(k,v);
				}else if(state==3) {
					String[] val=k.split("\\.",2);
					if(val.length==2) {
						k=val[0].trim();
						rsrc.computeIfAbsent(k,x->new HashMap<>())
							.put(val[1].trim(),v.trim());
					}
				}
				else if(state==4) {
					if(v.startsWith("\"")&&v.length()>1) {
						v=v.toLowerCase().substring(1,v.lastIndexOf("\""));
						Config.errorPages.put(k,Response.builder()
								.header("Content-Type","text/html")
								.data(v.getBytes())
								.buildDeepCopy());
					}else {
						var dir = Path.of(config.getOrDefault("Hydar.WEB_ROOT","."));
						Path resolved=!v.startsWith("/") ? 
								dir.resolve("./"+v)
								:dir.resolve("."+v);
						Config.errorPages.put(k,Response.builder()
								.data(new Resource(resolved.normalize(),currentTimeMillis(),currentTimeMillis()))
								.buildDeepCopy());
					}
				}
				else if(state<=1)
					(state==1?macros:config).put(k,v);
			}
			Config.errorPages.putIfAbsent("default",Response.builder().data("A known error has occurred.".getBytes()).build());
			
			Config.set(config);
			if(!rsrc.isEmpty())
				try {
					Config.loadResources(rsrc);
				} catch (NamingException e) {
					e.printStackTrace();
				}
		}catch(IOException e){
			e.printStackTrace();
			System.out.println("Config file failed to load. Try specifying a path in command line arguments, or in ./hydar.properties");
		}
	}
	private static long multiplier(char f) {
		return switch(f) {
		case 'K'->1024;
		case 'M'->1024*1024;
		case 'G','B'->1024*1024*1024;
		case 'T'->1024l*1024*1024*1024;
		case 'm'->60*1000;
		case 'h'->60*60*1000;
		case 'd'->60*60*24*1000;
		case 's'->1000;
		default->1;
		};
	}
	private static long l(String s){
		if(s.isBlank())
			return -1;
		long mul=multiplier(s.charAt(s.length()-1));
		return (s.endsWith("ms"))?
			Long.parseLong(s.substring(0,s.length()-2)):
				mul*Long.parseLong(s,0,s.length()-(mul==1?0:1),10);
	}
	private static int m(String s){
		return Math.toIntExact(l(s));
	}
	private static int i(String s){
		if(s.isBlank())
			return -1;
		char last =s.charAt(s.length()-1);
		return switch(last) {
			case 'h','d','m','s'->m(s)/1000;
			default->m(s);
		};
	}
	//private static float f(String s){
	//	return Float.parseFloat(s);
	//}
	private static boolean b(String s){
		return Boolean.parseBoolean(s);
	}
	private static Optional<Pattern> p(String s){
		return (s==null || s.length()==0)? Optional.empty() : Optional.of(Pattern.compile(s));
	}
	public static void set(Map<String,String> cfg){
		cfg.forEach((k_,v)->{
			String k=k_.replace(".","_").toUpperCase().split("HYDAR.")[1];
			try {
				Field field = Config.class.getDeclaredField(k);
				switch(k) {
					case "ZIP_ALGS":
					field.set(null,Arrays.stream(multi(v))
							.filter(x->Arrays.stream(Encoding.values())
									.map(y->y.toString())
									.anyMatch(x::equals))
							.toList());
					return;
					case "ZIP_MIMES":
					field.set(null,Set.of(Arrays.stream(v.split(";"))
							.map(String::trim)
							.map(x->x.replace("\"",""))
							.toArray(String[]::new))
						);
					return;
					/**
					case "CLASSPATH":
						List<String> cp = new ArrayList<>();
						cp.add(System.getProperty("java.class.path"));
						if(!Config.COMPILE_IN_MEMORY)
							cp.add(Hydar.cache.toString());
						for(String entry:multi(v)) {
							int depth = entry.endsWith("/*")?1:
								entry.endsWith("/**")?Integer.MAX_VALUE:0;
							if(depth>0) {
								cp.addAll(
									Files.walk(Path.of(entry.substring(0,entry.indexOf("/*"))),depth)
									.map(Path::toString)
									.toList()
								);
							}else cp.add(entry);
						}
						field.set(null,cp);
					return;*/
					case "COMPILER_OPTIONS":
						field.set(null,List.of(multi(v)));
					return;
					case "HOST":
						try {
							if(InetAddress.getByName(v.split(":")[0].trim()).isLoopbackAddress()) {
								HOST=null;
								System.out.println("Binding to loopback. Set Hydar.HOST to change this.");
							}
						} catch (Exception e) {}
						Pattern HOST_p=null;
						if(HOST!=null)
							HOST_p=Pattern.compile(Arrays.stream(v.split(","))
									.map(x->"("+x.trim().replace(".","\\.").replace("*",".*")+")")
									.collect(Collectors.joining("||")));
						HOST=Optional.ofNullable(HOST_p);
					return;
				}
				switch(field.getType().getCanonicalName()) {
					case "java.lang.String":
						field.set(null, v);
						break;
					case "java.lang.String[]":
						field.set(null, multi(v));
						break;
					case "java.util.Optional":
						field.set(null,p(v));
						break;
					case "int":
						field.setInt(null, k.contains("TIME")?m(v):i(v));
						break;
					case "boolean":
						field.setBoolean(null, b(v));
						break;
					default:
						System.out.println("Unknown type: "+k);	
						break;
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				System.out.println("Unknown setting: "+k);
			}
		});

		Token.PERMANENT_STATE.setTasks(tasks(TC_PERMANENT_STATE));
		Token.SLOW_API.setTasks(tasks(TC_SLOW_API));
		Token.FAST_API.setTasks(tasks(TC_FAST_API));
		Token.IN.setTasks(tasks(TC_IN));
		Token.OUT.setTasks(tasks(TC_OUT));
		Hydar.dir=Path.of(WEB_ROOT);
		Hydar.cache=Path.of(CACHE_DIR_PATH);
		if(TC_ENABLED) {
			int mbuf=TC_MAX_BUFFER;
			HydarLimiter.maxBuffer=(mbuf<0?Integer.MAX_VALUE:mbuf);
		}
		HydarEE.HttpServletRequest.CONTEXT.init = Map.copyOf(macros);
	}
}