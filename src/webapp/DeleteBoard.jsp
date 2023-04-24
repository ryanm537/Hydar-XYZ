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
<title>Deleting Board ...</title>
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
	int uid=(int)session.getAttribute("userid");
	// CHECK PERMS
	
	String str = "SELECT user.permission_level FROM user WHERE user.id = ?";
	var ps = conn.prepareStatement(str);
	ps.setInt(1,uid);
	ResultSet result = ps.executeQuery();
	
	String perms = "";
	while(result.next()){
		perms = result.getString("user.permission_level");
	}

	if(!perms.equals("water_hydar") && !perms.equals("great_white")){
		throw new Exception();
	}
	
	// ADMIN PERM
	try{
		str = "SELECT board.creator, board.channelof FROM board WHERE board.number = ?";
		ps = conn.prepareStatement(str);
		ps.setInt(1,board);
		result = ps.executeQuery();
		if(result.next()){
			int channelof = result.getInt("board.channelof");
			if(result.getInt("board.creator")==uid){
				//if board id is 'number'
				String orNumber = channelof>=0?" WHERE number = ?":" WHERE number = ? OR channelof=?";
				//if board id is 'board'
				//have to delete it last(otherwise boards no exist)
				String orBoard = channelof>=0?" WHERE board = ?":" WHERE board = ? OR board IN (SELECT number FROM board WHERE channelof=?)";
				//delete the board
				List<String> queries = 
				List.of(//"DELETE FROM invitedto"+orBoard,
						//"DELETE FROM isin"+orBoard,
						"DELETE FROM board"+orNumber
						);
				for(String s:queries){
					ps = conn.prepareStatement(s);
					ps.setInt(1,board);
					if(channelof==-1)
						ps.setInt(2,board);
					System.out.println(s+ps.executeUpdate());
				}
			}else{
				throw new Exception();
			}
		}
	}catch(SQLException e){
		 e.printStackTrace();
	}
	out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("MainMenu.jsp"));
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