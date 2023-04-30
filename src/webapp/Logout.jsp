<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*,java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Logging out... - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<body>
<%
if(session.getAttribute("username")!=null&&((Integer)session.getAttribute("userid")!=3)){
	session.setAttribute("userid", null);
	session.setAttribute("username", null);
}

out.print("<form action=\"targetServlet\">");
response.sendRedirect(response.encodeURL("MainMenu.jsp"));
%>
</body>
</html>