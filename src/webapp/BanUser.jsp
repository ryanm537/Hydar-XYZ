<%@page import="java.net.InetAddress"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file='SkeleCheck.jsp' %> 
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Removing User ...</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>

<body>
<body style = "background-color:rgb(51, 57, 63);"> 
<center>
<style type="text/css">
form{ display: inline-block; }
</style>

<div id="show">
</div>



<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	int kicked = Integer.parseInt(request.getParameter("kickID"));
	boolean ipBan = Boolean.parseBoolean(request.getParameter("ip"));
	boolean unBan = Boolean.parseBoolean(request.getParameter("unban"));
	int uid=(int)session.getAttribute("userid");
	// CHECK PERMS
	
	String str = "SELECT user.permission_level FROM user WHERE user.id = ?" ;
	var ps = conn.prepareStatement(str);
	ps.setInt(1,uid);
	var result=ps.executeQuery();
	String perms = "";
	if(!result.next()||!result.getString("user.permission_level").equals("water_hydar")){
		throw new Exception();
	}
	// ADMIN PERM
	String ban="DELETE FROM user WHERE id = ?";
	String addr="SELECT addr FROM user WHERE id = ?";
	String ban2="DELETE FROM user WHERE user.addr = ?";
	String ban3="INSERT INTO ban(user,addr) VALUES(?,?)";
	String unban="DELETE FROM ban WHERE user=?";
	if(!ipBan){
		ps=conn.prepareStatement(ban);
		ps.setInt(1,kicked);
		ps.executeUpdate();
	}else if(unBan){
		ps=conn.prepareStatement(unban);
		ps.setInt(1,kicked);
		ps.executeUpdate();
	}else{

		ps=conn.prepareStatement(addr);
		ps.setInt(1,kicked);
		var rs=ps.executeQuery();
		if(rs.next()){
			byte[] ip = rs.getBytes(1);
			if(!InetAddress.getByAddress(ip).isLoopbackAddress()){
				ps=conn.prepareStatement(ban2);
				ps.setBytes(1,ip);
				ps.executeUpdate();
				
				ps=conn.prepareStatement(ban3);
				ps.setInt(1,kicked);
				ps.setBytes(2,ip);
				ps.executeUpdate();
				return;
			}
		}
	}
	out.print("<form action=\"targetServlet\">");
	out.print("</form>");
	
}catch (Exception e){
	out.print("<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style>");
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