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
<title>Changing Profile Picture ...</title>
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


<%!
%>
<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	String newPfp = request.getParameter("new_pfp").replaceAll("\"","");
	String[] pfps = {"images/yeti.png", "images/hydar2.png", "images/emp.png", "images/gw.png", "images/grim.png"};
	int uid=(int)session.getAttribute("userid");
	String str = "SELECT user.permission_level FROM user WHERE user.id = ?";
	var ps=conn.prepareStatement(str);
	ps.setInt(1,uid);
	ResultSet result = ps.executeQuery();
	
	String perms = "";
	while(result.next()){
		perms = result.getString("user.permission_level");
	}
	
	// CHECK PERMS
	
	int isAnOption = 0;
	for(int i = 0; i < pfps.length; i++){
		if(pfps[i].equals(newPfp)){
			isAnOption = 1;
		}
	}
	
	if(!perms.equals("water_hydar") && isAnOption == 0){
		throw new Exception();
	}
	
	// CHANGE PROFILE PIC
	
	str="UPDATE user SET user.pfp = ? WHERE user.id = ?";
	ps=conn.prepareStatement(str);
	ps.setString(1,newPfp);
	ps.setInt(2,uid);
	ps.executeUpdate();
	
	out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("Profile.jsp"));
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