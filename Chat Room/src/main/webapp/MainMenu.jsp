<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Select Board - Hydar</title>
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
	//CHECK IF BOARD IS SPECIFIED, and redirect if the user does not have perms.
	
	
	Statement stmt1 = conn.createStatement();
	
	String countBoards = "SELECT COUNT(isin.board) AS numBoards FROM isin WHERE isin.user = " + session.getAttribute("userid").toString();
	ResultSet result1 = stmt1.executeQuery(countBoards);
	int numBoards = 0;
	while(result1.next()){
		numBoards = Integer.parseInt(result1.getString("numBoards"));
	}

	String checkBoardsStr="SELECT isin.board, board.name FROM isin, board WHERE isin.user = " + session.getAttribute("userid").toString() + " AND board.number = isin.board ORDER BY board.number";
	result1 = stmt1.executeQuery(checkBoardsStr);
	
	int[] boardArray = new int[numBoards];
	String[] boardNames = new String[numBoards];
	int n = 0;
	while(result1.next()){
		boardArray[n] = Integer.parseInt(result1.getString("isin.board"));
		boardNames[n] = result1.getString("board.name");
		n++;
	}
	
	
	
	%>
	<style>
		.centeredText {position:relative;top:290px;color:white;z-index:1; font-family:arial; font-size:20px;-webkit-touch-callout: none;}
		ul {padding:0px;padding-top: 10px;margin: 10px 20px; list-style: none;}
	    ul li {display: inline-block;vertical-align:middle; }
	    ul li a img {margin-top: 30px;width: 200px; height: 200px;display: inline-block; border-radius:20px;}
		ul li a:hover img {transform: scale(1.1);box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);}
		.column {float: left;}
		.row::after {content: "";clear: both;display: block;} 
		.pfps{display:inline-block;margin-left:auto;margin-right:auto;}
		
		.fix-div{
			background-color:rgb(41, 47, 53);
			opacity:90%;
			position:fixed; 
			width:100%; 
			left:0; 
			top:0;
			box-shadow:0 0 10px rgba(0,0,0,20);
			z-index:1;}
		.margin{
			margin-top:50px;
		}
			
		
	</style>
	<div class = "fix-div">
	
	<%
	
	//TOP BAR
	
	out.print("<h1 style = \"color:rgb(255,255,255); font-size:15px; font-family:arial; text-align:right;position:relative;\"></style>");
	out.print("Hello <div id=\"profileName\" style=\"display:inline\">" + session.getAttribute("username").toString() + "</div>! | ");
	out.print("<style type=\"text/css\"> a{color:LightGrey; font-family:arial; text-align:right; font-size:15px}</style>");
	out.print("<a href=\"Profile.jsp\"> Profile</a>&nbsp;| ");
	out.print("<a href=\"Logout.jsp\"> Log out</a> &nbsp;&nbsp;");
	
	out.print("<style type=\"text/css\"> h1{color:rgb(255,255,255); text-align:left; font-size:15px}</style>");
	out.print("<img src=\"hydar.png\" alt=\"hydar\" width = \"25px\" height = \"40px\" align = \"center\">");
	out.print("&nbsp;&nbsp;&nbsp;Pick a board: ");
	out.print("<form method=\"get\" action=\"Homepage.jsp\">");
	out.print("<select name=\"board\">" );
	out.print("<option value = \"" + -1 + "\"> ---");
	
	// board selector
	
	for(int i = 0; i < boardArray.length; i++){
		String checkBoards = "SELECT board.name FROM board WHERE board.number = "+boardArray[i] + " ORDER BY board.number";	
		ResultSet boardsQuery = stmt1.executeQuery(checkBoards);
		String b = "";
		while(boardsQuery.next()){
			b=boardsQuery.getString("board.name");
		}
		out.print("<option value = \""+ boardArray[i] +"\"> " + b);
	}
	out.print("<input value=\"Go\"  type=\"submit\"></select></form>");
	
	out.print("</h1>");
	%>
	</div><div class = "margin">
	
	<%
	String[] menuImages = {"menuImages/everythingElse.png", "menuImages/sas4.png", "menuImages/skyblock.png"};
	
	out.print("<div class = \"pfps\"><div class = \"row\">");
	for(int i =0; i<numBoards; i++){
		out.print("<div class = \"column\">");
		out.print("<div class = \"centeredText\"><b>"+ boardNames[i] + "</b></div>");
		if(i<menuImages.length){
			out.print("<ul><li><a href=\"Homepage.jsp?board="+(i+1)+"\"><img src=\"" + menuImages[i] +"\" alt=\"hydar"+ i +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
		}else {
			out.print("<ul><li><a href=\"Homepage.jsp?board="+(i+1)+"\"><img src=\"menuImages/misc.png\" alt=\"hydar"+ i +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
		}
	}
	out.print("</div></div><br>");
	
	
	
	conn.close();
} catch (Exception e) {
	out.print("<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style>");
	out.print("<center>");
	out.print("A known error has occurred.\n");
	out.print("<br><br>");
	out.print("<form method=\"get\" action=\"Logout.jsp\">");
	out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
	out.print("</form>");
	e.printStackTrace();
}
%>

</body>
</html>