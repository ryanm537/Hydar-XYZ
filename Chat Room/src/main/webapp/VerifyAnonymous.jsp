<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*,java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Logging in as Anonymous...</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<body>
<style>
	body{
		background-image:url('images/hydarface.png');
		background-repeat:no-repeat;
		background-attachment:fixed;
		background-size:100% 150%;
		background-color:rgb(51, 57, 63);
		background-position: 0% 50%;
	}
	.images{
		height:140%;
		width:calc(100% + 20px);
		position:absolute;
		overflow:hidden;
		top:-40%;
		left:-20px;
		opacity:40%;
	}
	.textbox{
		position: absolute;
		top:50%;
		left:50%;
	}
	.textboxmove{
		background:rgb(51, 57, 63);
		width:470px;
		height:420px;
		display:block;
		position: absolute;
		top:-210px;
		left:-235px;
		box-shadow:0 0 10px rgba(0,0,0,20);
	}
	.hydarlogo{
		position:absolute;
		top:calc(50% - 160px);
		left:calc(50% - 220px);
		opacity:100%;
	}
	.button3{
			dsiplay:inline-block;
			background-color:rgb(41, 47, 53);
			color:white;border:none;
			padding:12px 16px; 
			position:relative; 
			left:0px;
			top:4px;
			border-radius:8px;
			font-size:15px;
	}
	.button3:hover{
		background-color:rgb(61, 97, 183);
		cursor:pointer;
	}
</style>
<center>
<% 
Class.forName("com.mysql.jdbc.Driver");
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");

try{
	
	String un = "Anonymous";

	Statement stmt = conn.createStatement();
	String str = "SELECT MAX(id) AS max FROM user";
	ResultSet result = stmt.executeQuery(str);
	
	result.next();
	int newID = result.getInt("max")+1;
	str = "INSERT INTO user(`username`, `password`, `id`, `pfp`, `permission_level`, `pings`, `volume`, `pingvolume`, `vcvolume`) "
			+ " VALUES(\"Anonymous\", \"\", "+ newID +", \"images/hydar2.png\", \"yeti\", 0, 50, 50, 50)";
	int updateUser = stmt.executeUpdate(str);
	
	str = "INSERT INTO isin(`user`, `board`) VALUES (" + newID + ", " + 1 + ")";
	int addBoard = stmt.executeUpdate(str);
	str = "INSERT INTO isin(`user`, `board`) VALUES (" + newID + ", " + 3 + ")";
	addBoard = stmt.executeUpdate(str);
	
	//redirect to homepage
	session.setAttribute("userid", newID);
	session.setAttribute("username", un);
	out.print("<form action=\"targetServlet\">");
	response.sendRedirect("MainMenu.jsp");
	out.print("</form>");

	
	
	conn.close();
} catch (Exception e){
	out.print("<style> body{color:rgb(255,255,255); font-family:calibri; text-align:center; font-size:20px;}</style>");
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