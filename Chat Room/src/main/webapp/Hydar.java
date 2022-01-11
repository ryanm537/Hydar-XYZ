import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.Character;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.nio.charset.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.TimeZone;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;
class HydarRuntimeThread extends Thread{
	Method meth=null;
	private final BlockingQueue<String> queue;
	public HydarRuntimeThread(Method m, BlockingQueue<String> q){
		this.meth=m;
		this.queue=q;
	}public void run(){
		try{
			meth.invoke(meth,new Object[]{new String[]{}});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
class ServerThread extends Thread {
	Socket client = null;

	// constructor initializes socket
	public ServerThread(Socket socket) {
		this.client = socket;
	}

	public void run() {
		try {
			this.client.setSoTimeout(5000);
			SimpleDateFormat s;
			InputStream input = this.client.getInputStream();
			InputStreamReader ir = new InputStreamReader(input, Charset.forName("UTF-8"));
			BufferedReader buffer = new BufferedReader(ir);
			OutputStream output = this.client.getOutputStream();
			String line;
			
			//readLine blocks until 5s
			try {
				line = buffer.readLine();
			} catch (java.net.SocketTimeoutException ste) {
				output.write(("HTTP/1.1 408 Request Timeout\r\nServer: Large_Hydar/1.1\r\n\r\n" + "408 Request Timeout" + "").getBytes());
                //flush output and wait .25s(done for every output)
				output.flush();
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				output.close();
				input.close();
				ir.close();
				buffer.close();
				this.client.close();
				return;
			}
			
			if (line != null) {
				String[] firstLine = line.split(" ");
                // malformed input
				if (firstLine.length != 3) {
					output.write(("HTTP/1.1 400 Bad Request\r\nServer: Large_Hydar/1.1\r\n\r\n" + "400 Bad Request" + "").getBytes());
					output.flush();
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					output.close();
					input.close();
					ir.close();
					buffer.close();
					this.client.close();
					return;
				}
				String search = "";
				if(firstLine[1].indexOf("?")>=0){
					search = firstLine[1].substring(firstLine[1].indexOf("?")+1);
					firstLine[1] = firstLine[1].substring(0,firstLine[1].indexOf("?"));
				}System.out.println(""+client.getInetAddress()+"> " + firstLine[0] + " " + firstLine[1] + " " + firstLine[2]);
				
				//tests http version
				if (!(firstLine[2].equals("HTTP/1.0")) && !(firstLine[2].equals("HTTP/1.1")) && !(firstLine[2].equals("HTTP/2.0"))) {
					output.write(("HTTP/1.1 505 HTTP Version Not Supported\r\nServer: Large_Hydar/1.1\r\n\r\n"
							+ "505 HTTP Version Not Supported\nSupported: HTTP/1.1, HTTP/1.0" + "").getBytes());
					output.flush();
					try {
						Thread.sleep(250);
					} catch (InterruptedException ee) {
						Thread.currentThread().interrupt();
					}
					output.close();
					input.close();
					ir.close();
					buffer.close();
					this.client.close();
					return;
				}//default
				if (firstLine[1].equals("/")) {
					firstLine[1] = "/Login.jsp";
				}
				String data;
				String timestamp;
				
				try {
                    //don't allow requests outside of current folder
					Path p = Paths.get("." + firstLine[1]).normalize();
					if (p.toString().contains("..")) {
						output.write(("HTTP/1.1 403 Forbidden\r\nServer: Large_Hydar/1.1\r\n\r\n" + "403 Forbidden" + "").getBytes());
						output.flush();
						try {
							Thread.sleep(250);
						} catch (InterruptedException eee) {
							Thread.currentThread().interrupt();
						}
						output.close();
						input.close();
						ir.close();
						buffer.close();
						this.client.close();
						return;
					}
					data = Files.readString(Paths.get("." + firstLine[1]), StandardCharsets.ISO_8859_1);
					s = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
					s.setTimeZone(TimeZone.getTimeZone("GMT"));
					timestamp = s.format(new File("." + firstLine[1]).lastModified());
				} catch (IOException e) {
                    //create file to test what caused error(could also use e)
					File f = new File("." + firstLine[1]);
					if (!f.exists()) {
						output.write(("HTTP/1.1 404 Not Found\r\nServer: Large_Hydar/1.1\r\n\r\n" + "404 Not Found" + "").getBytes());
					} else if (!Files.isReadable(Paths.get("." + firstLine[1]))) {
						output.write(("HTTP/1.1 403 Forbidden\r\nServer: Large_Hydar/1.1\r\n\r\n" + "403 Forbidden" + "").getBytes());
					} else {
						output.write(("HTTP/1.1 500 Internal Server Error\r\nServer: Large_Hydar/1.1\r\n\r\n" + "500 Internal Server Error" + "")
								.getBytes());
					}
					output.flush();
					try {
						Thread.sleep(250);
					} catch (InterruptedException eee) {
						Thread.currentThread().interrupt();
					}
					output.close();
					input.close();
					ir.close();
					buffer.close();
					this.client.close();

					return;
				}
				
				//mime type
				String mime = Files.probeContentType(Paths.get(firstLine[1]));
				if (mime == null)
					mime = "application/octet-stream";
				
				//reads rest of the request(headers)
				String headers = new String();
				this.client.setSoTimeout(1);
				try {
					for (String head; (head = buffer.readLine()) != null; headers += head+"\n")
						;
				} catch (java.net.SocketTimeoutException seee) {
                    //socket times out at end of input(set to 1ms to make it faster, only once per request)
				}
				this.client.setSoTimeout(5000);
				String[] heads = headers.split("\n");
				String modif = null;
				for (String str : heads) {
					if (str.startsWith("If-Modified-Since: ")) {
						//System.out.println(str+"\n");
						modif = str.substring(str.indexOf(":") + 2);
					}
				}
				if (modif != null) {
					Date ct = null, st = null;
					boolean b = false;
					try {
						ct = s.parse(modif);
						st = s.parse(timestamp);
					} catch (ParseException eeeee) {
						b = true;
					}
					
					//compares times of last modified and if-modified-since to test for 304(only on GET)
					//System.out.println("c"+ct.getTime()+"s"+st.getTime());
					if (!firstLine[1].endsWith(".jsp")&&!b && ct.getTime() >= st.getTime() && firstLine[0].equals("GET")) {
						output.write(("HTTP/1.1 304 Not Modified\r\nServer: Large_Hydar/1.1\r\nExpires: Thu, 01 Dec 2024 16:00:00 GMT\r\n\r\n")
								.getBytes());
						output.flush();
						try {
							Thread.sleep(250);
						} catch (InterruptedException eee) {
							Thread.currentThread().interrupt();
						}
						output.close();
						input.close();
						ir.close();
						buffer.close();
						this.client.close();
						return;
					}
				}
				
				//get and post return identically, but different implementations are used for later
				if (firstLine[0].equals("GET")) {
					if(!firstLine[1].endsWith(".jsp")){
						boolean booled=false;
						for(String x:Hydar.banned){
							if(firstLine[1].contains(x)){
								booled=true;
								output.write(
									("HTTP/1.1 403 Forbidden\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Encoding: identity\r\nContent-Length: "
									+ "403 Forbidden".length() + "\r\nContent-Type: " + mime
									+ "\r\nExpires: Thu, 01 Dec 2024 16:00:00 GMT\r\nLast-Modified: " + timestamp
									+ "\r\n\r\n" + "403 Forbidden").getBytes(StandardCharsets.ISO_8859_1));
									break;
							}
						}if(!booled)
							output.write(
							("HTTP/1.1 200 OK\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Encoding: identity\r\nContent-Length: "
									+ data.length() + "\r\nContent-Type: " + mime
									+ "\r\nExpires: Thu, 01 Dec 2024 16:00:00 GMT\r\nLast-Modified: " + timestamp
									+ "\r\n\r\n" + data + "").getBytes(StandardCharsets.ISO_8859_1));
					}else{
						ArrayList<String> cookies = new ArrayList<String>();
						ArrayList<String> cookieK = new ArrayList<String>();
						ArrayList<String> cookieV = new ArrayList<String>();
						String session = null;
						int cc=0;
						for (String str: heads) {//receive cookies
							if (str.startsWith("Cookie: ")) {
								Collections.addAll(cookies, str.substring(str.indexOf(":") + 2).split(";"));
							}
						}for(String inc: cookies){
							int x = inc.indexOf('=');
							if(x!=-1){
								if(inc.substring(0,x).equals("HYDAR_sessionID")){
									session=inc.substring(x+1);
								}
								cookieK.add(inc.substring(0,x));
								cookieV.add(inc.substring(x+1));
								cc++;
							}
						}if(session==null){
							session="HYDAR-"+UUID.randomUUID().toString();
						}
						int i=0;
						String newData="";
						while(data.indexOf("<%@")>-1){
							data=data.substring(data.indexOf("%>")+2);
						}
						String err="<!DOCTYPE html>";
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
						err+="<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style><center>A known error has occurred.";
						err+="<br><br><form method=\"get\" action=\"Logout.jsp\"><td><input type=\"submit\" value=\"Back to login\"></td></form>";
						err+="</body>";
						err+="</html>";
						boolean ise=false;
						String re=null;
						if(data.indexOf("<%")>-1){
							//newData+=data.substring(0,data.indexOf("<%"));
							//data=data.substring(data.indexOf("%>")+2);
							try{
								String name = firstLine[1].substring(firstLine[1].lastIndexOf("/")+1);
								Class c = Hydar.classes.get(name.substring(0,name.lastIndexOf(".")));
								//System.out.println(name.substring(0,name.lastIndexOf("."))+i);
								Method[] m = c.getDeclaredMethods();
								Constructor co = c.getConstructors()[0];
								Object o=co.newInstance();
								if(Hydar.attr.get(session)==null){
									Hydar.attr.put(session,new ConcurrentHashMap<String,String>());
								}
								for(Method meth:m)
									if(meth.getName().equals("jsp_Main")){
										ConcurrentHashMap<String,String> tmpAttr = new ConcurrentHashMap<String,String>(Hydar.attr.get(session));
										Object[] ret = (Object [])meth.invoke(o,new Object[]{search,Hydar.attr.get(session)});
										if(ret.length==0){
											ise=true;
											newData=err;
											data="";
											break;
										}else{
											@SuppressWarnings("unchecked")ConcurrentHashMap<String,Boolean> ak = (ConcurrentHashMap<String,Boolean>)ret[0];
											@SuppressWarnings("unchecked")ConcurrentHashMap<String,String> av = (ConcurrentHashMap<String,String>)ret[1];
											String h = (String)ret[2];
											String redirect = (String)ret[3];
											ConcurrentHashMap<String,String> tmp = new ConcurrentHashMap<String,String>();
											for(String k:ak.keySet()){
												if(ak.get(k)){
													tmp.put(k,av.get(k));
												}else{
													tmp.put(k,Hydar.attr.get(session).get(k));
												}
											}Hydar.attr.put(session,tmp);
											if(redirect==null){
												newData+=h;
											}else{
												re=new String(redirect);
											}
										}
									}
								
							}catch(Exception e){
								e.printStackTrace();
								try {
									output.write(
											("HTTP/1.1 500 Internal Server Error\r\nServer: Large_Hydar/1.1\r\n\r\n" + "500 Internal Server Error" + "").getBytes());
									output.flush();
									try {
										Thread.sleep(250);
									} catch (InterruptedException ee) {
										Thread.currentThread().interrupt();
									}
									output.close();
									this.client.close();
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
							output.write(("HTTP/1.1 500 Internal Server Error\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Encoding: identity\r\nContent-Length: "
									+ newData.length() + "\r\nContent-Type: text/html;charset=ISO-8859-1"
									+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT"+"\r\nSet-Cookie: HYDAR_sessionID="+session+"; SameSite=Strict\r\n\r\n" + newData + "").getBytes(StandardCharsets.ISO_8859_1));
						}else if(re==null){
							//System.out.println(newData);
							output.write(
							("HTTP/1.1 200 OK\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Encoding: identity\r\nContent-Length: "
									+ newData.length() + "\r\nContent-Type: text/html;charset=ISO-8859-1"
									+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT"+"\r\nSet-Cookie: HYDAR_sessionID="+session+"; SameSite=Strict\r\n\r\n" + newData + "").getBytes(StandardCharsets.ISO_8859_1));
						}else output.write(
							("HTTP/1.1 302 Found\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Encoding: identity\r\nContent-Length: "
									+ 0 + "\r\nLocation: "+re+"\r\nContent-Type: text/html;charset=ISO-8859-1"
									+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT" + "\r\nSet-Cookie: HYDAR_sessionID="+session+"; SameSite=Strict\r\n\r\n" + "").getBytes(StandardCharsets.ISO_8859_1));
						
					}
									
				} else if (firstLine[0].equals("POST")) {
					output.write(
							("HTTP/1.1 200 OK\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Encoding: identity\r\nContent-Length: "
									+ data.length() + "\r\nContent-Type: " + mime
									+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT\r\nLast-Modified: " + timestamp
									+ "\r\n\r\n" + data + "").getBytes(StandardCharsets.ISO_8859_1));
				} else if (firstLine[0].equals("HEAD")) {
					output.write(
							("HTTP/1.1 200 OK\r\nAllow: GET, POST, HEAD\r\nServer: Large_Hydar/1.1\r\nContent-Encoding: identity\r\nContent-Length: "
									+ data.length() + "\r\nContent-Type: " + mime
									+ "\r\nExpires: Thu, 01 Dec 2020 16:00:00 GMT\r\nLast-Modified: " + timestamp)
											.getBytes(StandardCharsets.ISO_8859_1));
				} else if (firstLine[0].equals("PUT") || firstLine[0].equals("DELETE") || firstLine[0].equals("LINK")
						|| firstLine[0].equals("UNLINK")) {
					output.write(("HTTP/1.1 501 Not Implemented\r\nServer: Large_Hydar/1.1\r\n\r\n501 Not Implemented").getBytes());
				} else {
					output.write(("HTTP/1.1 400 Bad Request\r\nServer: Large_Hydar/1.1\r\n\r\n400 Bad Request").getBytes());
				}

			}
			output.flush();
			try {
				Thread.sleep(250);
			} catch (InterruptedException eeee) {
				Thread.currentThread().interrupt();
			}
			output.close();
			input.close();
			ir.close();
			buffer.close();
			this.client.close();
			return;
		} catch (IOException e) {
			
			//500 is interpreted as "any other error"
			try {
				OutputStream output = this.client.getOutputStream();
				output.write(
						("HTTP/1.1 500 Internal Server Error\r\nServer: Large_Hydar/1.1\r\n\r\n" + "500 Internal Server Error" + "").getBytes());
				output.flush();
				try {
					Thread.sleep(250);
				} catch (InterruptedException ee) {
					Thread.currentThread().interrupt();
				}
				output.close();
				this.client.close();
				return;
			} catch (IOException eee) {
                //failed to send the error code
				System.out.println("error");
			}
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
	public static String[] banned;
	private static ArrayList<String> compilerOptions;
	public static HashMap<String,Class> classes;
	public static HashMap<String,String> htmls;
	public static ConcurrentHashMap<String,ConcurrentHashMap<String,String>> attr = new ConcurrentHashMap<String,ConcurrentHashMap<String,String>>();
	public static void main(String[] args) {
		banned = new String[]{".class",".java",".jar",".bat"};
		compilerOptions = new ArrayList<String>();
		compilerOptions.add("-cp");
		compilerOptions.add("\".;./lib/mysql-connector-java-5.1.49-bin.jar\"");
		compilerOptions.add("-Xlint:deprecation");
		//checks if a port is specified
		File dir = new File(".");
		try{
			Class.forName("com.mysql.jdbc.Driver");
		}catch(Exception exc){
			exc.printStackTrace();
		}
		int diag=0;
		try{
			ArrayList<File> jsp = new ArrayList<File>();
			for(File f:dir.listFiles()){
				if(f.toPath().toString().endsWith(".jsp"))
					jsp.add(f);
			}
			File cache = new File("./HydarCompilerCache");
			if(cache.isDirectory()){
				Files.walk(cache.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
			classes = new HashMap<String,Class>();
			htmls = new HashMap<String,String>();
			new File("./HydarCompilerCache").mkdirs();
			ucl = new URLClassLoader(new URL[]{cache.toURI().toURL()});
			for(File j:jsp){
				String path=j.toPath().toString();
				String s = Files.readString(j.toPath(), StandardCharsets.ISO_8859_1);
				ArrayList<String> javas = new ArrayList<String>();
				while(s.indexOf("<%@")>-1){
					s=s.substring(s.indexOf("%>")+2);
				}int i_=0;
				String x_="this.jsp_attr_values=new ConcurrentHashMap<String,String>(jsp_attr);\nthis.jsp_urlParams=jsp_param;\nthis.jsp_attr_set=new ConcurrentHashMap<String,Boolean>();\nfor(String jsp_local_s:jsp_attr.keySet()){\nthis.jsp_attr_set.put(jsp_local_s,false);}\nthis.jsp_urlParams=new String(jsp_param);\nthis.jsp_redirect=null;\nthis.jsp_html=\"\";";
				String e=path.substring(path.lastIndexOf('\\')+1,path.lastIndexOf('.'))+".jsp";
				String n=path.substring(path.lastIndexOf('\\')+1,path.lastIndexOf('.'));
				String o="private String jsp_urlParams;\nprivate String jsp_redirect;\nprivate String jsp_html;\n";
				String v="private ConcurrentHashMap<String,Boolean> jsp_attr_set;\n";
				String i="private ConcurrentHashMap<String,String> jsp_attr_values;\n";
				String a="private void jsp_SA(Object jsp_arg0, Object jsp_arg1){\nif(jsp_arg1==null){\nthis.jsp_attr_values.remove(jsp_arg0.toString());\nthis.jsp_attr_set.put(jsp_arg0.toString(),true);\nreturn;\n}\nthis.jsp_attr_values.put(jsp_arg0.toString(),jsp_arg1.toString());\nthis.jsp_attr_set.put(jsp_arg0.toString(),true);\n}\n";
				String q="private String jsp_GA(Object jsp_arg0){\nreturn this.jsp_attr_values.get(jsp_arg0.toString());\n}\n";
				String u="private void jsp_OP(Object jsp_arg0){\nthis.jsp_html+=jsp_arg0.toString();\n}\n";
				String a_="private String jsp_GP(Object jsp_arg0){\nif(this.jsp_urlParams.indexOf(jsp_arg0.toString()+\"=\")>=0){\nreturn this.jsp_urlParams.substring(this.jsp_urlParams.indexOf(jsp_arg0.toString()+\"=\")+jsp_arg0.toString().length()+1).split(\"&\")[0];}\nreturn null;\n}\n";
				String r_="private void jsp_SR(Object jsp_arg0){\nthis.jsp_redirect=jsp_arg0.toString();\n}\n";
				String t="private void jsp__P(Object jsp_arg0){\nSystem.out.print(jsp_arg0.toString());\n}\n";
				String a__="private void jsp__Pln(Object jsp_arg0){\nSystem.out.println(jsp_arg0.toString());\n}\n";
				while(s.indexOf("<%")>-1){
					htmls.put(n+i_,s.substring(0,s.indexOf("<%")));
					javas.add(s.substring(s.indexOf("<%")+2,s.indexOf("%>")));
					s=s.substring(s.indexOf("%>")+2);
					i_++;
				}htmls.put(n+i_,s);
				//System.out.println(javas);
				int index=0;
				ArrayList<String> varNames = new ArrayList<String>();
				ArrayList<String> varTypes = new ArrayList<String>();
				String x__="import java.util.HashMap;\nimport java.util.concurrent.*;import java.sql.*;\npublic class "+n+"{\npublic "+n+"(){\n}\n"+o+v+i+a+q+u+a_+r_+t+a__+"public Object[] jsp_Main(String jsp_param, ConcurrentHashMap<String, String> jsp_attr) {\ntry{\n"+x_;
				for(String x:javas){
					//System.out.println(path);
					if(htmls.get(n+index)!=null){
						x__+="\nthis.jsp_OP(\""+htmls.get(n+index).replace("\"","\\\"").replace("\r","").replace("\n","\\n\"+\n\"")+"\");\n";
					}
					x=x.replace("session.getAttribute","this.jsp_GA");
					x=x.replace("session.setAttribute","this.jsp_SA");
					x=x.replace("System.out.print","this.jsp__P");
					x=x.replace("out.print","this.jsp_OP");
					x=x.replace("request.getParameter","this.jsp_GP");
					x=x.replace("response.sendRedirect","this.jsp_SR");
					x__+=x;
					index++;
				}if(htmls.get(n+index)!=null){
					x__+="\nthis.jsp_OP(\""+htmls.get(n+index).replace("\"","\\\"").replace("\r","").replace("\n","\\n\"+\n\"")+"\");\n";
				}
				x__+="}catch(Exception jsp_e){\njsp_e.printStackTrace();return new Object[]{};}\nreturn new Object[]{this.jsp_attr_set,this.jsp_attr_values,this.jsp_html,this.jsp_redirect};\n\n}\n}";
				JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
				DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
				
				StringWriter writer = new StringWriter();
				PrintWriter out = new PrintWriter(writer);
				Writer fileWriter = new FileWriter(".\\HydarCompilerCache\\"+n+".java", false);
				out.println(x__);
				//System.out.println(x__);
				fileWriter.write(x__);
				fileWriter.close();
				out.close();
				JavaFileObject file = new JavaSourceFromString(n, writer.toString());
				Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
				CompilationTask task = compiler.getTask(null, null, diagnostics, compilerOptions, null, compilationUnits);
				boolean success = task.call();
				for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
					  diag++;
					  System.out.println(diagnostic.getCode());
					  System.out.println(diagnostic.getKind());
					  System.out.println("line: "+diagnostic.getLineNumber());
					  System.out.println(diagnostic.getSource());
					  System.out.println(diagnostic.getMessage(null));

					}
				if(success){
					//System.out.println(path);
					File f = new File(".\\HydarCompilerCache\\"+n+".class");
					f.delete();
					Path moved = Files.move(Paths.get(n+".class"),Paths.get(".\\HydarCompilerCache\\"+n+".class"));
					try{
						//ucl = new URLClassLoader(new URL[]{cache.toURI().toURL()});
						Class c = ucl.loadClass(n);
						classes.put(n,c);
					}catch(Exception what){
						what.printStackTrace();
						System.exit(0);
					}
				}else{
					double r = Math.random();
					if(r<0.33)
						System.out.println(e+": Compilation failed. You are fat");
					else if(r<0.67)
						System.out.println(e+": Compilation failed. laugh at this person");
					else
						System.out.println(e+": Compilation failed + ratio");
					System.exit(0);
				}
				
			}
			File hydr = new File("./lib/Amogus.jar");
			System.out.println(Files.readString(hydr.toPath()));
			
			if(diag>0){
				System.out.println("Compilation successful with "+diag+" warning(s)! Starting server.");
			}else System.out.println("Compilation successful! Starting server.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			return;	
		}
		if (args.length == 0) {
			System.out.println("No port specified");
			System.exit(0);
		}
		int port = Integer.parseInt(args[0]);
		//checks if port is valid
		if (port < 1024 || port > 65535) {
			System.out.println("Invalid port");
			System.exit(0);
		}
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException f) {
			System.out.println("Cannot open port " + port);
			return;
		}
		
		
		
		//server loop(only ends on ctrl-c)
		ArrayList<ServerThread> threads = new ArrayList<ServerThread>(5);
		while (true) {
			Socket client = null;
			try {
				client = server.accept();
			} catch (IOException e) {
				System.out.println("Failed to accept");
			}
			ServerThread connection = new ServerThread(client);
			int alives = 0;
			int index = -1;
			boolean run = false;
            //find dead threads and replace them
			for (int i = 0; i < threads.size(); i++) {
				if (index<0&&!threads.get(i).isAlive()) {
					index = i;
					threads.set(i, connection);
					break;
				} else
					alives++;
			}
            //all threads are dead -> reset threadpool
			if (alives == 0) {
				threads = new ArrayList<ServerThread>(5);
				threads.add(connection);
				index = 0;
				run = true;
				threads.get(index).start();
                continue;
			}else if(index>-1){
                //at least 1 thread is dead, so just replace it
				run = true;
				threads.get(index).start();
                continue;
            }
			//expand threadpool, or give 505 if already 50+
			if (!run && alives >= threads.size()) {
				if (!run && alives < 50) {
					threads.add(connection);
					index = threads.size() - 1;
					run = true;
					threads.get(index).start();
                    continue;
				} else {
					try {
						OutputStream output = client.getOutputStream();
						output.write(("HTTP/1.1 505 Service Unavailable\r\nServer: Large_Hydar/1.1\r\n\r\n505 Service Unavailable").getBytes());
						output.flush();
						try {
							Thread.sleep(250);
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

		}
	}

}
