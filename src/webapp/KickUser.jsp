<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file='SkeleCheck.jsp' %>
<% if(response.getStatus()==302)return; %> 
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
	int board = Integer.parseInt(request.getParameter("board_num").replaceAll("\"", ""));
	int kicked = Integer.parseInt(request.getParameter("kickID").replaceAll("\"", ""));
	int uid=(int)session.getAttribute("userid");
	// CHECK PERMS
	
	String str = "SELECT user.permission_level FROM user WHERE user.id = ?" ;
	var ps = conn.prepareStatement(str);
	ps.setInt(1,uid);
	var result=ps.executeQuery();
	String perms = "";
	while(result.next()){
		perms = result.getString("user.permission_level");
	}

	if(!perms.equals("water_hydar") && !perms.equals("great_white")){
		throw new Exception();
	}
	
	// ADMIN PERM
	
	str = "SELECT creator, channelof FROM board WHERE board.number = ?";
	ps = conn.prepareStatement(str);
	ps.setInt(1,board);
	result=ps.executeQuery();
	while(result.next()){
		if(result.getInt("creator")==uid){
			int channelof=result.getInt("channelof");
			int mainBoard=channelof==-1?board:channelof;
			ps = conn.prepareStatement("DELETE FROM isin WHERE user =  ? AND (board = ? OR board IN (SELECT number FROM board WHERE channelof=?))");
			ps.setInt(1,kicked);
			ps.setInt(2,mainBoard);
			ps.setInt(3,mainBoard);
			int kick = ps.executeUpdate();
		}else{
			throw new Exception();
		}
	}

	out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("Homepage.jsp?board="+board));
	out.print("</form>");
	
}catch (Exception e){
	response.setStatus(500);
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