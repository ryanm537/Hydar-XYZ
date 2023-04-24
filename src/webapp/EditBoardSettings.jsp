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
<title>Changing Parameter ...</title>
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
	String inviteOnOff = "";
	String readonlyOnOff = "";
	String newName = "";
	
	boolean changedInvite = false;
	try{
		inviteOnOff = request.getParameter("inviteonly").replaceAll("\"", "");
		changedInvite = true;
	}catch(Exception e){
		
	}
	
	boolean changedRead = false;
	try{
		readonlyOnOff = request.getParameter("readonly").replaceAll("\"", "");
		changedRead = true;
	}catch(Exception e){
	
	}
	
	boolean changedName = false;
	try{
		newName = request.getParameter("newName").replaceAll("\"", "");
		changedName = true;
	}catch(Exception e){
	
	}
	
	
	int isPublic = 0;
	String image = "misc.png";
	
	if(inviteOnOff.equals("off")){
		isPublic = 1;
		image = "PublicBoard.png";
	}
	int readonly = 0;
	if(readonlyOnOff.equals("on")){
		readonly = 1;
	}
	
	// CHECK PERMS
	int uid=(int)session.getAttribute("userid");
	String str = "SELECT user.permission_level FROM user WHERE user.id = ?";
	var ps=conn.prepareStatement(str);
	ps.setInt(1,uid);
	ResultSet result = ps.executeQuery();
	
	String perms = "";
	while(result.next()){
		perms = result.getString("user.permission_level");
	}

	if(!perms.equals("water_hydar") && !perms.equals("great_white")){
		throw new Exception();
	}
	
	// CHECK ADMIN PERM THEN ADD TO DB
	
	str = "SELECT board.creator, board.channelof FROM board WHERE board.number = ?";
	ps=conn.prepareStatement(str);
	ps.setInt(1,board);
	result = ps.executeQuery();
	while(result.next()){
		if(result.getInt("board.creator")==uid){
	if(changedInvite){
		if(isPublic > 1){
	throw new Exception();
		}
		int parentBoard = result.getInt("board.channelof");
		String str2 = "UPDATE board SET public = ?, image = ? WHERE number = ? OR channelof = ? OR number = ?";
		ps=conn.prepareStatement(str2);
		ps.setInt(1,isPublic);
		ps.setString(2,image);
		ps.setInt(3,board);
		ps.setInt(4,board);
		ps.setInt(5,parentBoard);
		ps.executeUpdate();
	}
	if(changedRead){
		if(isPublic > 1 && result.getInt("board.channelof")==-1){
	throw new Exception();
		}
		String str2 = "UPDATE board SET readonly = ? WHERE number = ?";
		ps=conn.prepareStatement(str2);
		ps.setInt(1,readonly);
		ps.setInt(2,board);
		int update = ps.executeUpdate();
	}
	if(changedName){
		String str2 = "UPDATE board SET name = ? WHERE number = ?";
		ps=conn.prepareStatement(str2);
		ps.setString(1,newName);
		ps.setInt(2,board);
		int update = ps.executeUpdate();
	}
	
		}else{
	throw new Exception();
		}
	}
	
	
	
	out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("Homepage.jsp?board="+board));
	out.print("</form>");
	
}catch (Exception e){
	out.print("<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style>");
	out.print("A known error has occurred. You are getting this screen because we don't have the time to make custom error messages for every issue.\n");
	out.print("<br><br>");
	out.print("<form method=\"post\" action=\""+response.encodeURL("Logout.jsp")+"\">");
	out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
	out.print("</form>");
	e.printStackTrace();
}
%>

</body>
</html>