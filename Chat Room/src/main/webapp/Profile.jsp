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
	
	%>
	<style>
		.fix-div{
			background-color:rgb(41, 47, 53);
			opacity:90%;
			position:fixed; 
			width:100%; 
			left:0; 
			top:0;}
		
		.margin{
			margin-top:50px;
		}
	</style>
	<div class = "fix-div">
	
	<%
	
	//TOP BAR
	
	out.print("<h1 style = \"color:rgb(255,255,255); font-size:15px; font-family:arial; text-align:right;position:relative;\"></style>");
	out.print("Hello <div id=\"profileName\" style=\"display:inline\">" + session.getAttribute("username").toString() + "</div>! | ");
	out.print("<style type=\"text/css\"> a{color:LightGrey; font-family:arial; text-align:right; font-size:15px; display:inline-block;padding-top:15px;}</style>");
	out.print("<a href=\"Homepage.jsp\"> Home</a>&nbsp;| ");
	out.print("<a href=\"Logout.jsp\"> Log out</a> &nbsp;&nbsp;");
	
	out.print("<style type=\"text/css\"> h1{color:rgb(255,255,255); text-align:left; font-size:15px;}</style>");
	out.print("<img src=\"hydar.png\" alt=\"hydar\" width = \"25px\" height = \"40px\" align = \"center\">");
	
	out.print("</h1></div><div class = \"margin\">");
	
	Statement stmt = conn.createStatement();
	String checkPostsStr="SELECT user.pfp, user.username FROM user WHERE user.username = \"" + session.getAttribute("username").toString()+"\"";
	ResultSet result = stmt.executeQuery(checkPostsStr);

	out.print("<style> p{color:LightSlateGrey; font-family:arial; text-align:center; font-size:15px;}</style>");
	out.print("<br><p> - Profile Picture - </p>");
	
	%> 
		<style>
			ul {
				padding:0px;
				padding-top: 10px;
		        margin: 10px 20px;
		        list-style: none;
		    }
		    ul li {
		        margin: -10px;
		        display: inline-block;
		    }
		    ul li a img {
		        width: 150px;
		        height: 150px;
		        display: block;
		    }
			ul li a:hover img {
	    		transform: scale(1.3);
	    		box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
			}
		</style>
	<%
	
	// PROFILE PIC
	
	while(result.next()){
		out.print("<ul><li><a href=\"#\"><img src=\"" + result.getString("user.pfp") +"\" alt=\"hydar\" width = \"100px\" height = \"100px\" align = \"left\"></a></li> </ul>");
	}
	
	
	// DISPLAY NAME & TEXT
	
	out.print("<style> p2{color:LightSlateGrey; font-family:arial; text-align:top-center; font-size:15px;} </style>");

	out.print("<p2> - Currently Selected - </p2>");
	
	%>
	<div id='bar' style='width: 720px; height: 20px; border-bottom: 2px solid LightSlateGray; text-align: center;'></div>
	<%
	
	out.print("<style> p4.test:hover, p4.test:active{transform:scale(1.3)} </style>");
	
	out.print("<br><style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:30px;}</style>");
	
	out.print("<p4 class = \"test\">" + session.getAttribute("username").toString() +"</p4>");
	
	%>
	<style>
		.bar{
			width: 720px; 
			height: 35px; 
			border-bottom: 2px solid LightSlateGray; 
			text-align: center;
			display:block;
			position:relative;
			top:20;
		}
	</style>
	<div class='bar'></div>
	<%
	
	// CREATE / JOIN BOARD BUTTONS
	
	%>
	<form method="get" action="Homepage.jsp">
	<style>
		.button{
			background-color:rgb(71, 107, 193);
			color:white;border:none;
			padding:10px 20px; 
			position:relative; 
			top:30px; 
			left:-20px; 
			border-radius:8px;
		}
		.button:hover{
			background-color:rgb(61, 97, 183);
			cursor:pointer;
		}
		.button2{
			background-color:rgb(71, 107, 193);
			color:white;border:none;
			padding:10px 20px; 
			position:relative; 
			top:30px; 
			left:20px; 
			border-radius:8px;
		}
		.button2:hover{
			background-color:rgb(61, 97, 183);
			cursor:pointer;
		}
	</style>
	<input value="Create Private Board"  type="submit" class="button" >
	</form>
	
	<form method="get" action="Homepage.jsp">
	<input value="Join Private Board"  type="submit" class="button2" >
	</form>
	
	<div id='bar' style='width: 720px; height: 65px; border-bottom: 2px solid LightSlateGray; text-align: center;'></div>
	<%
	
	out.print("<style> p3{color:LightSlateGrey; font-family:arial; text-align:center; font-size:25px;}</style>");
	
	out.print("<p3><br> Your Boards: </p3>");
	
	// DISPLAY BOARDS
	
	%>
		<style>
			P.blocktext {
				font-family:arial;
				font-size:20px;
				color:White;
			    margin-left: auto;
			    margin-right: auto;
			    width:8em;
			    text-align: left;
			}
		</style> 
		<br>
	<%
	String checkBoardsStr = "SELECT user.username, user.boards FROM user, board WHERE user.username = \"" + session.getAttribute("username").toString()+"\"";
	result = stmt.executeQuery(checkBoardsStr);
	String boards = "";
	while(result.next()){
		boards = result.getString("user.boards");
	}

	out.print("<P class=\"blocktext\">");
	String matchBoardsStr = "SELECT board.name, board.number FROM board";
	result = stmt.executeQuery(matchBoardsStr);
	
	while(result.next()){
		if(boards.contains(" " + result.getString("board.number") + ",")){
			out.print(result.getString("board.name") + "<br>");
		}
	}
	out.print("</P>");
	
	%>
	<div id='bar' style='width: 720px; height: 20px; border-bottom: 2px solid LightSlateGray; text-align: center;'></div><br>
	<%
	
	
	
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
