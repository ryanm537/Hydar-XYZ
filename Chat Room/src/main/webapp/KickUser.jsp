<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Removing User ...</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<script type="text/javascript" src ="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>

<body>
<body style = "background-color:rgb(51, 57, 63);"> 
<center>
<style type="text/css">
form{ display: inline-block; }
</style>

<div id="show">
</div>



<%

Class.forName("com.mysql.jdbc.Driver").newInstance();
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");

try{
	int board = Integer.parseInt(request.getParameter("board_num").replaceAll("\"",""));
	int kicked = Integer.parseInt(request.getParameter("kickID").replaceAll("\"",""));
	
	// CHECK PERMS
	
	Statement stmt = conn.createStatement();
	String str = "SELECT user.permission_level FROM user WHERE user.id = " + session.getAttribute("userid").toString();
	ResultSet result = stmt.executeQuery(str);
	
	String perms = "";
	while(result.next()){
		perms = result.getString("user.permission_level");
	}

	if(!perms.equals("water_hydar") && !perms.equals("great_white")){
		throw new Exception();
	}
	
	// ADMIN PERM
	
	str = "SELECT board.creator FROM board WHERE board.number = " + board;
	result = stmt.executeQuery(str);
	while(result.next()){
		if(Integer.parseInt(result.getString("board.creator"))==Integer.parseInt(session.getAttribute("userid").toString())){
			String str2 = "DELETE FROM isin WHERE user =  " + kicked + " AND board = " + board;
			int kick = stmt.executeUpdate(str2);
		}else{
			throw new Exception();
		}
	}
	
	
	
	out.print("<form action=\"targetServlet\">");
	response.sendRedirect("Homepage.jsp?board="+board);
	out.print("</form>");
	
}catch (Exception e){
	out.print("<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style>");
	out.print("A known error has occurred\n");
	out.print("<br><br>");
	out.print("<form method=\"get\" action=\"Logout.jsp\">");
	out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
	out.print("</form>");
	e.printStackTrace();
}


%>

</body>
</html>