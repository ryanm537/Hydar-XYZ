<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" %>
<%@ page import="java.io.*,java.util.*,java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Login - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<body style = "background-color:rgb(51, 57, 63);"> 

<center>
<img src="hydar.png" alt="hydar">
<p style = "color:rgb(255,255,255);">
<%
out.print("<p style = \"color:rgb(255,255,255); font-family:arial; font-size:20px\">"); %>
<br>
Welcome to Hydar!<br><br>
Returning users login:<br>
<form method="get" action="Verify.jsp" >
<p style = "color:rgb(255,255,255); font-family:arial; ">
Username: 
<input  type="text" name="username" size = "20"><br>
&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; Password: 
<input type="password" name="password">
<input type="submit" name="submit" value = "continue"><br><br>
</form>
<p style = "color:rgb(255,255,255); font-family:arial; ">
Don't have an account? rip!<br>

</form>

</p>
</body>
</html>