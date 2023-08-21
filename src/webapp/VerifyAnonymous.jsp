<%@page import="java.net.InetAddress"%>
<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.sql.DataSource"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*,java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%!
static volatile long lastSweep=0;
%>
<%
//for reverse proxies
/**String ip = request.getHeader("X-Forwarded-For");  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
    ip = request.getHeader("Proxy-Client-IP");  
}  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
    ip = request.getHeader("WL-Proxy-Client-IP");  
}  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
    ip = request.getHeader("HTTP_CLIENT_IP");  
}  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
    ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
}  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
    ip = request.getRemoteAddr();  
}*/
String ip = request.getRemoteAddr();
byte[] addr=InetAddress.getByName(ip).getAddress();
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Logging in as Anonymous...</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<body>
<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	Integer uid=(Integer)session.getAttribute("userid");
	if(uid == null || uid==3){
		String un = "Anonymous";
		int newID=-1;
		try(var ps=conn.prepareStatement("SELECT 1 FROM ban WHERE addr=?")){
			ps.setBytes(1,addr);
			try(var rs=ps.executeQuery()){
				if(rs.next())throw new Exception();	
			}
		}
		try(PreparedStatement addUser = conn.prepareStatement("INSERT INTO user(`username`, `password`, `pfp`, `permission_level`, `created_date`, `pings`, `volume`, `pingvolume`, `vcvolume`, `addr`) "
		+ " VALUES(\"Anonymous\", \"sfj67\", \"images/hydar2.png\", \"yeti\",?, 0, 50, 50, 50, ?)",Statement.RETURN_GENERATED_KEYS)){
			addUser.setLong(1,System.currentTimeMillis());
			addUser.setBytes(2,addr);
			addUser.executeUpdate();
			ResultSet keys = addUser.getGeneratedKeys();
			if(keys.next())
				newID=keys.getInt(1);
			else throw new Exception("no newID");
		}
		try(PreparedStatement defaultBoards = conn.prepareStatement("INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (?,?,0)")){
			defaultBoards.setInt(1,newID);
			for(int i=1;i<=3;i++){
				defaultBoards.setInt(2,i);
				defaultBoards.executeUpdate();
			}
		}
		session.setAttribute("userid", newID);
		session.setAttribute("username", un);
		session.removeAttribute("ip");
		session.setMaxInactiveInterval(2600000);
	}
	long now=System.currentTimeMillis();
	if(now-lastSweep > 7*24*60*3600l){
		lastSweep=now;
		var stmt = conn.prepareStatement("""
			DELETE FROM user WHERE 
			(SELECT MAX(lastVisited) FROM isin WHERE user.id=isin.user) < ? 
			AND user.permission_level='yeti'
		""");
		stmt.setLong(1,now-24*60*3600*30l);//30 days ago
		stmt.executeUpdate();
	}
	
	//redirect to homepage
	out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("MainMenu.jsp"));
	out.print("</form>");
	
	
} catch (Exception e){
	out.print("<style> body{color:rgb(255,255,255); font-family:calibri, arial; text-align:center; font-size:20px;}</style>");
	out.print("A known error has occurred\n");
	out.print("<br><br>");
	out.print("<form method=\"post\" action=\""+response.encodeURL("Logout.jsp")+"\">");
	out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
	out.print("</form>");
	e.printStackTrace();
}
%>
</body>
</html>