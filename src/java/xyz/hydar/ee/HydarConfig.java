package xyz.hydar.ee;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

/**
 * Uses reflection to turn hydar.properties file into
 * easier to access static fields.
 * See 'default.properties' for all the possible settings.
 * */
class Config{
	/**TODO:dynamic config*/
	/**TODO:replace @categories with Context.[thing] etc*/
	public Map<String,String> config = new HashMap<>();
	public Map<String,String> macros = new HashMap<>();
	public static int PORT = 8080;
	private String CACHE_DIR_PATH = "./HydarCompilerCache";
	public static String IMPORTANT_PATH = "./bots/Amogus.jar";
	public String SERVLET_PATH = "";
	public static boolean SSL_ENABLED = false;
	public static String SSL_TRUST_STORE_PATH="", SSL_TRUST_STORE_PASSPHRASE="", SSL_KEY_STORE_PATH, SSL_KEY_STORE_PASSPHRASE;
	public static String SSL_CONTEXT_NAME = "TLS";
	public List<String> CLASSPATH=new ArrayList<>(List.of(System.getProperty("java.class.path")));
	public static String[] SSL_ENABLED_PROTOCOLS = new String[]{"TLSv1.2","TLSv1.3"};
	public static int SSL_REDIRECT_FROM=-1;
	public boolean CREATE_JAVA_FILES=false;
	public int REFRESH_TIMER=2000;
	public static int MAX_THREADS=256;
	public boolean USE_WATCH_SERVICE=true;
	public boolean FORBIDDEN_SILENT=true;
	public int CACHE_MAX=1024000;
	public Optional<Pattern> HOST=Optional.empty();//empty=anything allowed
	public Optional<Pattern> FORBIDDEN_REGEX=Optional.empty();//empty=anything allowed
	public Optional<Pattern> CACHE_OFF_REGEX=Optional.empty();//empty=anything allowed 
	public Optional<Pattern> CACHE_ON_REGEX=Optional.of(HydarUtil.MATCH_ALL);//empty=anything allowed 
	public boolean CACHE_ENABLED=true;
	public List<String> ZIP_ALGS=List.of("gzip","deflate");
	public Set<String> ZIP_MIMES = Set.of("text/html", "text/css", "text/plain", "text/xml", "text/x-component",
			"text/javascript", "application/x-javascript", "application/javascript", "application/json",
			"application/manifest,json", "application/vnd.api,json", "application/xml", "application/xhtml,xml",
			"application/rss,xml", "application/atom,xml", "application/vnd.ms-fontobject", "application/x-font-ttf",
			"application/x-font-opentype", "application/x-font-truetype", "image/svg,xml", "image/x-icon",
			"image/vnd.microsoft.icon", "font/ttf", "font/eot", "font/otf", "font/opentype");
	public List<String> COMPILER_OPTIONS = new ArrayList<>();
	public boolean PARALLEL_COMPILE=false;
	public boolean COMPILE_IN_MEMORY=true;
	public boolean LOWERCASE_URLS=true;
	public List<String> AUTO_APPEND_URLS= List.of(".jsp",".html");
	public boolean PERSIST_SESSIONS=true;
	
	public static boolean H2_ENABLED=true;
	public static int H2_WINDOW_ATTEMPTS=3;
	public static int H2_LOCAL_WINDOW_TIMER=0;
	public static int H2_REMOTE_WINDOW_TIMER=1000;
	public static int H2_LOCAL_WINDOW_INC=10_240_000;
	public static int H2_HEADER_TABLE_SIZE=4096;
	public static int H2_MAX_CONCURRENT_STREAMS=256;
	public static int H2_MAX_FRAME_SIZE=16384;
	public static int H2_MAX_HEADER_LIST_SIZE=8192;
	
	public static int HTTP_INITIAL_LIFETIME=45000;
	public static int HTTP_KEEPALIVE_LIFETIME=5000;
	
	public boolean SEND_ETAG=true;
	public boolean SEND_DATE=true;
	public boolean RANGE_JSP = false;
	public boolean RANGE_NO_JSP = true;
	public boolean LASTMODIFIED_FROM_FILE = false;
	public boolean RECEIVE_ETAGS=true;
	public String HOMEPAGE="/index.html";
	
	public boolean WS_ENABLED=true;
	public boolean WS_DEFLATE=true;
	public String SERVER_HEADER="Large_Hydar/2.0";
	public static boolean SSL_HSTS=true;
	
	public String CACHE_CONTROL_JSP="no-cache";
	public String CACHE_CONTROL_NO_JSP="public, max-age=604800, must-revalidate";

	public Map<Pattern,String> links = new LinkedHashMap<>();
	public Map<Pattern,List<String>> linkParams = new LinkedHashMap<>();
	
	public static int H2_LIFETIME=30000;
	public static String H2_HPACK_TREE_STRATEGY="ARRAY";
	public static String H2_HPACK_TABLE_STRATEGY="MAP";
	public int WS_LIFETIME=15000;

	public static boolean TC_ENABLED=false;
	public static int TC_FAST_HTTP_REQUEST=50;
	public static int TC_FAST_WS_MESSAGE=100;
	public static int TC_FAST_H2_FRAME=5;
	public static int TC_PERMANENT_THREAD=100;
	public static int TC_PERMANENT_H2_STREAM=10;
	public static int TC_MAX_BUFFER=10 * 1024 * 1024;
	public static int TC_SLOW_JSP_INVOKE=100;
	public static Set<String> alreadySet = new HashSet<>();
	public String configPath = "";
	public static String TC_PERMANENT_STATE, TC_IN, TC_OUT, TC_SLOW_API, TC_FAST_API;
	
	public String WEB_ROOT = ".";
	public boolean LAZY_COMPILE=true;
	public boolean LAZY_FILES=true;
	public boolean TRY_UNSAFE_WATCH_SERVICE=false;
	public Map<String,String> errorPages=new HashMap<>();
	private static final Map<String,Object> jndi = new ConcurrentHashMap<>();
	public final Hydar hydar;
	public Config(Hydar hydar) {
		this.hydar=hydar;
	}
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
	/**
	*	Adds a new link to the config, which will redirect requests.
	*	Path params in the regex, k, will become URL params if enclosed with /{}/.
	*	After that, k is compiled as a regex which is replaced with v.
	*	See default.properties for examples.
	*/
	public void link(String k, String v){
		List<String> params = new ArrayList<>();
		String parsedPattern = Arrays.stream(k.split("\\/")).
			map(element -> {
				if(element.startsWith("{") && element.endsWith("}")) {
					String newRegex = "[^\\/]*";
					if(element.contains("}{")) {
						newRegex = element.substring(element.indexOf("}{")+2, element.length()-1);
					}
					String paramName = element.substring(1,element.indexOf("}"));
					params.add(paramName);
					return newRegex;
				}
				params.add(null); //pad out non-params
				return element;
			})
			.collect(Collectors.joining("/"));
		Pattern linkPattern = p(parsedPattern).orElse(HydarUtil.MATCH_ALL);
		links.put(linkPattern, LOWERCASE_URLS ? v.toLowerCase() : v);
		linkParams.put(linkPattern, params);
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
					jndi.put(name,ds);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		var comp=new InitialContext() {
			@Override
			public Object lookup(String name) throws NamingException{
				return jndi.get(name);
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
		try {
			NamingManager.setInitialContextFactoryBuilder(env1->env2->init);
		}catch(IllegalStateException ise) {
			//already initialized
		}
	}
	public void load(String configPath) {
		try{
			System.out.println("Loading config from "+configPath+"...");
			this.configPath=configPath;
			//cfg.removeIf(s->(s.startsWith("#")||s.trim().length()==0));
			int state=0;
			Map<String,Map<String,String>> rsrc = new HashMap<>();
			for(String s:Files.readAllLines(Path.of(configPath))){
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
					link(k,v);
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
						errorPages.put(k,v);
					}else {
						var dir = Path.of(config.getOrDefault("Hydar.WEB_ROOT","."));
						Path resolved=!v.startsWith("/") ? 
								dir.resolve("./"+v)
								:dir.resolve("."+v);
						errorPages.put(k,Files.readString(resolved));
					}
				}
				else if(state<=1)
					(state==1?macros:config).put(k,v);
			}
			errorPages.putIfAbsent("default","A known error has occurred.");
			
			set(config);
			if(!rsrc.isEmpty())
				try {
					loadResources(rsrc);
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
	public void set(Map<String,String> cfg){
		cfg.forEach((k_,v)->{
			String k=k_.replace(".","_").toUpperCase().split("HYDAR.")[1];
			try {
				Field field = Config.class.getDeclaredField(k);
				try {
					field.get(null);
					if(!alreadySet.add(k)) {
						System.out.println("Repeat global setting "+k+". This setting is global and can only be changed once. Please ensure it only occurs in one properties file.");
						return;
					}
				}catch(NullPointerException e) {
					
				}
				switch(k) {
					case "ZIP_ALGS":
					field.set(this,Arrays.stream(multi(v))
							.filter(x->Arrays.stream(Encoding.values())
									.map(y->y.toString())
									.anyMatch(x::equals))
							.toList());
					return;
					case "ZIP_MIMES":
					field.set(this,Set.of(Arrays.stream(v.split(";"))
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
						field.set(this,List.of(multi(v)));
					return;
					case "HOST":
						try {
							if(InetAddress.getByName(v.split(":")[0].trim()).isLoopbackAddress()) {
								HOST=null;
							}
						} catch (Exception e) {}
						Pattern HOST_p=null;
						if(HOST!=null)
							HOST_p=Pattern.compile(
									"^("+
									Arrays.stream(v.split(","))
									.map(x->"("+x.trim().replace(".","\\.").replace("*",".*")+")")
									.collect(Collectors.joining("|"))
									+")$");
						HOST=Optional.ofNullable(HOST_p).filter(p->!p.toString().trim().equals("^((.*))$"));
					return;
				}
				switch(field.getType().getCanonicalName()) {
					case "java.lang.String":
						field.set(this, v);
						break;
					case "java.lang.String[]":
						field.set(this, multi(v));
						break;
					case "java.util.Optional":
						field.set(this,p(v));
						break;
					case "int":
						field.setInt(this, k.contains("TIME")?m(v):i(v));
						break;
					case "boolean":
						field.setBoolean(this, b(v));
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
		hydar.config=this;
		hydar.dir=Path.of(configPath).resolveSibling(WEB_ROOT);
		hydar.cache=Path.of(configPath).resolveSibling(CACHE_DIR_PATH);
		if(TC_ENABLED) {
			int mbuf=TC_MAX_BUFFER;
			HydarLimiter.maxBuffer=(mbuf<0?Integer.MAX_VALUE:mbuf);
		}
	}
	/**Return an appopriate error page, backed by HydarConfig.*/
	public String getErrorPage(String code) {
		return errorPages.getOrDefault(code,errorPages.get("default"));
	}
}