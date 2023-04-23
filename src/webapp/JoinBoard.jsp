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
<title>Joining Board ...</title>
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
	int boardID = Integer.parseInt(request.getParameter("input_join").replaceAll("\"", ""));
	
	// CHECK INVITES
	int uid=(int)session.getAttribute("userid");
	String str = "SELECT invitedto.board FROM invitedto WHERE invitedto.user = ?";
	var ps = conn.prepareStatement(str);
	ps.setInt(1,uid);
	ResultSet result = ps.executeQuery();
	
	int count = 0;
	while(result.next()){
		if(boardID == result.getInt("invitedto.board")){
	count++;
		}
	}
	
	// CHECK IF BOARD IS PUBLIC
	
	str = "SELECT board.public FROM board WHERE board.number = ?";
	ps = conn.prepareStatement(str);
	ps.setInt(1,boardID);
	result = ps.executeQuery();
	int isPublic = 0;
	while(result.next()){
		if(result.getInt("board.public") == 1){
	isPublic = 1;
		}
	}
	
	if(isPublic != 1 && count == 0){
		throw new Exception();
	}
	
	// PERM LEVEL
	
	str = "SELECT user.permission_level FROM user WHERE user.id = ?";
	ps = conn.prepareStatement(str);
	ps.setInt(1,uid);
	result = ps.executeQuery();
	
	String perms = "";
	while(result.next()){
		perms = result.getString("user.permission_level");
	}

	if(!perms.equals("water_hydar") && !perms.equals("great_white")){
		//throw new Exception();
	}

	
	// UPDATE THE USER'S INVITES
	if(count != 0){
		str = "DELETE FROM invitedto WHERE user = ? AND board = ?";

		ps = conn.prepareStatement(str);
		ps.setInt(1,uid);
		ps.setInt(2,boardID);
		ps.executeUpdate();
	}

	// UPDATE THE USER'S BOARDS
	
	str = "INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (?, ?, 0)";
	ps = conn.prepareStatement(str);
	ps.setInt(1,uid);
	ps.setInt(2,boardID);
	ps.executeUpdate();
	
	// ADD ALL CHANNELS
	str = "SELECT board.number FROM board WHERE board.channelof = ?";
	ps = conn.prepareStatement(str);
	ps.setInt(1,boardID);
	var result2=ps.executeQuery();
	
	int addChannel = 0;
	while(result2.next()){
		try{
			str = "INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (?, ?, 0)";
			ps = conn.prepareStatement(str);
			ps.setInt(1,uid);
			ps.setInt(2,result2.getInt("board.number"));
			addChannel = ps.executeUpdate();
		}catch(Exception e){
	
		}
	}
	out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("Homepage.jsp?board="+boardID));
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