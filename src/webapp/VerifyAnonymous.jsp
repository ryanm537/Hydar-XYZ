<%@page import="java.net.InetAddress"%>
<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.sql.DataSource"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*,java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
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
<style>
	body{
		background-image:url('images/hydarface.png');
		background-repeat:no-repeat;
		background-attachment:fixed;
		background-size:100% 150%;
		background-color:rgb(51, 57, 63);
		background-position: 0% 50%;
	}
	.images{
		height:140%;
		width:calc(100% + 20px);
		position:absolute;
		overflow:hidden;
		top:-40%;
		left:-20px;
		opacity:40%;
	}
	.textbox{
		position: absolute;
		top:50%;
		left:50%;
	}
	.textboxmove{
		background:rgb(51, 57, 63);
		width:470px;
		height:420px;
		display:block;
		position: absolute;
		top:-210px;
		left:-235px;
		box-shadow:0 0 10px rgba(0,0,0,20);
	}
	.hydarlogo{
		position:absolute;
		top:calc(50% - 160px);
		left:calc(50% - 220px);
		opacity:100%;
	}
	.button3{
			dsiplay:inline-block;
			background-color:rgb(41, 47, 53);
			color:white;border:none;
			padding:12px 16px; 
			position:relative; 
			left:0px;
			top:4px;
			border-radius:8px;
			font-size:15px;
	}
	.button3:hover{
		background-color:rgb(61, 97, 183);
		cursor:pointer;
	}
</style>
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
		try(PreparedStatement addUser = conn.prepareStatement("INSERT INTO user(`username`, `password`, `pfp`, `permission_level`, `pings`, `volume`, `pingvolume`, `vcvolume`, `addr`) "
		+ " VALUES(\"Anonymous\", \"sfj67\", \"images/hydar2.png\", \"yeti\", 0, 50, 50, 50, ?)",Statement.RETURN_GENERATED_KEYS)){
			addUser.setBytes(1,addr);
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
	//redirect to homepage
	out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("MainMenu.jsp"));
	out.print("</form>");
	
	
	conn.close();
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