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
<title>Inviting User ...</title>
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
	int invited = Integer.parseInt(request.getParameter("invitedID").replaceAll("\"", ""));
	int uid=(int)session.getAttribute("userid");
	
	// CHECK PERMS
	if(invited==3)throw new IllegalArgumentException("can't invite guest");
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
	int channelof = -1;
	str = "SELECT board.creator, board.channelof FROM board WHERE board.number = ?";
	ps = conn.prepareStatement(str);
	ps.setInt(1,board);
	result=ps.executeQuery();
	while(result.next()){
		channelof = result.getInt("board.channelof");//also get the main board if its a channel
		if(Integer.parseInt(result.getString("board.creator"))!=uid){
	throw new Exception();
		}
	}
	board=channelof==-1?board:channelof;
	
	// CREATE THE INVITE AND ADD IT
	
	
	//automatically add raye
	
	if(invited==2){

		// UPDATE THE USER'S BOARDS
		
		str = "INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (2,?, 0)";
		ps = conn.prepareStatement(str);
		ps.setInt(1,board);
		int addBoard = ps.executeUpdate();
		
		// ADD ALL CHANNELS
		str = "SELECT board.number FROM board WHERE board.channelof = ?";
		ps = conn.prepareStatement(str);
		ps.setInt(1,board);
		ResultSet result2 = ps.executeQuery();
		int addChannel = 0;
		while(result2.next()){
	try{
		str = "INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (2, ?, 0)";
		ps = conn.prepareStatement(str);
		ps.setInt(1,result2.getInt("board.number"));
		addBoard = ps.executeUpdate();
		
	}catch(Exception e){
		
	}
		}
	}else{
		
		//not raye
		
			str = "INSERT INTO invitedto(`user`, `board`) VALUES (?,?)";
			ps = conn.prepareStatement(str);
			ps.setInt(1,invited);
			ps.setInt(2,channelof==-1?board:channelof);
			
			int addInvite = ps.executeUpdate();
	}
	

	out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("Homepage.jsp?board="+board));
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