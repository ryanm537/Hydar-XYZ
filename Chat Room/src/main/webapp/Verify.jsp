<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*,java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Verifying credentials...</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<body>
<body style = "background-color:rgb(51, 57, 63);">
<center>
<% 
Class.forName("com.mysql.jdbc.Driver").newInstance();
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");

try{
	
	Statement stmt = conn.createStatement();
	String inputtedU = request.getParameter("username");
	String inputtedP = request.getParameter("password");
	String str = "SELECT * FROM user";
	ResultSet result = stmt.executeQuery(str);

	//check if users credentials match
	boolean success = false;
	
	while (result.next()) {
		
		if(result.getString("username").equals(inputtedU) && result.getString("password").equals(inputtedP)){
			success = true;
			break;
		}
	}
	if(success == false){
		//prompt user to go back to login page
		out.print("<img src=\"hydar.png\" alt=\"hydar\">");
		out.print("<p style = \"color:rgb(255,255,255); font-family:arial; \">");
		out.print("Username not found or incorrect password<br>\n");
		out.print("<br>");
		out.print("<form method=\"get\" action=\"Login.jsp\">");
		out.print("<td><input value=\"back\" type=\"submit\"></td>");
		out.print("</form>");
	}else{
		//redirect to homepage
		session.setAttribute("userid", result.getString("id"));
		session.setAttribute("username", inputtedU);
		out.print("<form action=\"targetServlet\">");
		response.sendRedirect("Homepage.jsp");
		out.print("</form>");
	}
	conn.close();
} catch (Exception e){
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
