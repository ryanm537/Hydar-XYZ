import java.util.HashMap;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStream;
public class VerifyAnonymous{
public VerifyAnonymous(){
}
private String jsp_urlParams;
private String jsp_redirect;
private String jsp_html;
private ConcurrentHashMap<String,Boolean> jsp_attr_set;
private ConcurrentHashMap<String,String> jsp_attr_values;
private void jsp_SA(Object jsp_arg0, Object jsp_arg1){
if(jsp_arg1==null){
this.jsp_attr_values.remove(jsp_arg0.toString());
this.jsp_attr_set.put(jsp_arg0.toString(),true);
return;
}
this.jsp_attr_values.put(jsp_arg0.toString(),jsp_arg1.toString());
this.jsp_attr_set.put(jsp_arg0.toString(),true);
}
private String jsp_GA(Object jsp_arg0){
return this.jsp_attr_values.get(jsp_arg0.toString());
}
private void jsp_OP(Object jsp_arg0){
this.jsp_html+=jsp_arg0.toString();
}
private String jsp_GP(Object jsp_arg0){
if(this.jsp_urlParams.indexOf(jsp_arg0.toString()+"=")>=0){
try{return java.net.URLDecoder.decode(this.jsp_urlParams.substring(this.jsp_urlParams.indexOf(jsp_arg0.toString()+"=")+jsp_arg0.toString().length()+1).split("&")[0], StandardCharsets.UTF_8.name());}catch(Exception e){}}
return null;
}
private void jsp_SR(Object jsp_arg0){
this.jsp_redirect=jsp_arg0.toString();
}
private void jsp__P(Object jsp_arg0){
System.out.print(jsp_arg0.toString());
}
private void jsp__Pln(Object jsp_arg0){
System.out.println(jsp_arg0.toString());
}
public Object[] jsp_Main(String jsp_param, ConcurrentHashMap<String, String> jsp_attr) {
try{
this.jsp_attr_values=new ConcurrentHashMap<String,String>(jsp_attr);
this.jsp_urlParams=jsp_param;
this.jsp_attr_set=new ConcurrentHashMap<String,Boolean>();
for(String jsp_local_s:jsp_attr.keySet()){
this.jsp_attr_set.put(jsp_local_s,false);}
this.jsp_urlParams=new String(jsp_param);
this.jsp_redirect=null;
this.jsp_html="";
this.jsp_OP("\n"+
"<!DOCTYPE html>\n"+
"<html>\n"+
"<head>\n"+
"<meta charset=\"ISO-8859-1\">\n"+
"<title>Logging in as Anonymous...</title>\n"+
"<link rel=\"shorcut icon\" href=\"favicon.ico\"/>\n"+
"</head>\n"+
"<body>\n"+
"<style>\n"+
"	body{\n"+
"		background-image:url('images/hydarface.png');\n"+
"		background-repeat:no-repeat;\n"+
"		background-attachment:fixed;\n"+
"		background-size:100% 150%;\n"+
"		background-color:rgb(51, 57, 63);\n"+
"		background-position: 0% 50%;\n"+
"	}\n"+
"	.images{\n"+
"		height:140%;\n"+
"		width:calc(100% + 20px);\n"+
"		position:absolute;\n"+
"		overflow:hidden;\n"+
"		top:-40%;\n"+
"		left:-20px;\n"+
"		opacity:40%;\n"+
"	}\n"+
"	.textbox{\n"+
"		position: absolute;\n"+
"		top:50%;\n"+
"		left:50%;\n"+
"	}\n"+
"	.textboxmove{\n"+
"		background:rgb(51, 57, 63);\n"+
"		width:470px;\n"+
"		height:420px;\n"+
"		display:block;\n"+
"		position: absolute;\n"+
"		top:-210px;\n"+
"		left:-235px;\n"+
"		box-shadow:0 0 10px rgba(0,0,0,20);\n"+
"	}\n"+
"	.hydarlogo{\n"+
"		position:absolute;\n"+
"		top:calc(50% - 160px);\n"+
"		left:calc(50% - 220px);\n"+
"		opacity:100%;\n"+
"	}\n"+
"	.button3{\n"+
"			dsiplay:inline-block;\n"+
"			background-color:rgb(41, 47, 53);\n"+
"			color:white;border:none;\n"+
"			padding:12px 16px; \n"+
"			position:relative; \n"+
"			left:0px;\n"+
"			top:4px;\n"+
"			border-radius:8px;\n"+
"			font-size:15px;\n"+
"	}\n"+
"	.button3:hover{\n"+
"		background-color:rgb(61, 97, 183);\n"+
"		cursor:pointer;\n"+
"	}\n"+
"</style>\n"+
"<center>\n"+
"");
 
Class.forName("com.mysql.jdbc.Driver");
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "catfish2001");

try{
	
	String un = "Anonymous";

	Statement stmt = conn.createStatement();
	String str = "SELECT MAX(id) AS max FROM user";
	ResultSet result = stmt.executeQuery(str);
	
	result.next();
	int newID = result.getInt("max")+1;
	str = "INSERT INTO user(`username`, `password`, `id`, `pfp`, `permission_level`, `pings`, `volume`, `pingvolume`, `vcvolume`) "
			+ " VALUES(\"Anonymous\", \"\", "+ newID +", \"images/hydar2.png\", \"yeti\", 0, 50, 50, 50)";
	int updateUser = stmt.executeUpdate(str);
	
	//redirect to homepage
	this.jsp_SA("userid", newID);
	this.jsp_SA("username", un);
	this.jsp_OP("<form action=\"targetServlet\">");
	this.jsp_SR("MainMenu.jsp");
	this.jsp_OP("</form>");

	
	
	conn.close();
} catch (Exception e){
	this.jsp_OP("<style> body{color:rgb(255,255,255); font-family:calibri; text-align:center; font-size:20px;}</style>");
	this.jsp_OP("A known error has occurred\n");
	this.jsp_OP("<br><br>");
	this.jsp_OP("<form method=\"get\" action=\"Logout.jsp\">");
	this.jsp_OP("<td><input type=\"submit\" value=\"Back to login\"></td>");
	this.jsp_OP("</form>");
	e.printStackTrace();
}
 
this.jsp_OP("\n"+
"</body>\n"+
"</html>");
}catch(Exception jsp_e){
jsp_e.printStackTrace();return new Object[]{};}
return new Object[]{this.jsp_attr_set,this.jsp_attr_values,this.jsp_html,this.jsp_redirect};

}
}