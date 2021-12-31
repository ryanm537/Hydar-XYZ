<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Profile - Hydar</title>
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
	Statement stmt = conn.createStatement();
	String checkPostsStr="SELECT user.pfp, user.username FROM user WHERE user.username = \"" + session.getAttribute("username").toString()+"\"";
	ResultSet result = stmt.executeQuery(checkPostsStr);

	out.print("<style> p{color:LightSlateGrey; font-family:arial; text-align:center; font-size:15px;}</style>");
	out.print("<p> - Profile Picture - </p>");
	
	%> 
		<style>
			ul {
				padding: 0;
		        margin: 50px 20px;
		        list-style: none;
		    }
		    ul li {
		        margin: 5px;
		        display: inline-block;
		    }
		    ul li a img {
		        width: 200px;
		        height: 200px;
		        display: block;
		    }
			ul li a:hover img {
	    		transform: scale(1.5);
	    		box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
			}
		</style>
	<%
	
	// PROFILE PIC
	
	while(result.next()){
		out.print("<ul><li><a href=\"#\"><img src=\"" + result.getString("user.pfp") +"\" alt=\"hydar\" width = \"200px\" height = \"200px\" align = \"left\"></a></li> </ul>");
	}
	
	
	// DISPLAY NAME & TEXT
	
	out.print("<style> p2{color:LightSlateGrey; font-family:arial; text-align:top-center; font-size:15px;}</style>");
	out.print("<p2> - Currently Selected - </p2>");
	
	out.print("<br>________________<br><br><style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:30px;}</style>");
	out.print("" + session.getAttribute("username").toString() +"");
	
	out.print("<style> p3{color:LightSlateGrey; font-family:arial; text-align:center; font-size:25px;}</style>");
	out.print("<p3><br><br> Your Boards: </p3>");
	
	// DISPLAY BOARDS
	%>
		<style>
			p4.blocktext {
				font-family:arial;
				font-size:25px;
				color:White;
			    margin-left: auto;
			    margin-right: auto;
			    width:8em
			    text-align: left;
			}
		</style> 
		<br>
	<%
	String checkBoardsStr = "SELECT board.name, user.username, user.boards FROM user, board WHERE user.username = \"" + session.getAttribute("username").toString()+"\"";
	result = stmt.executeQuery(checkBoardsStr);
	String boards = "";
	out.print("<p4 class=\"blocktext\">");
	while(result.next()){
		out.println(result.getString("board.name") + "<br>");
	}
	out.print("</p4>");
	
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
