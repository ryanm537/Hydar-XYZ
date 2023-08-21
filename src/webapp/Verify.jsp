<%@page import="javax.crypto.spec.SecretKeySpec"%>
<%@page import="javax.crypto.Mac"%>
<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="java.security.MessageDigest"%>
<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.sql.DataSource"%>
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
<center>
<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection();
	var stmt = conn.prepareStatement("SELECT id, username, password FROM user WHERE username = ?");){
	
	String inputtedU = request.getParameter("username");
	String inputtedP = request.getParameter("password");
	stmt.setString(1,inputtedU);
	ResultSet result = stmt.executeQuery();
	
	
	//check if users credentials match
	boolean success = false;
	
	while (result.next()) {
		int n = ((((12 + result.getInt("id")) * 49) - 86) / 2) + 15;
		int x = (((result.getInt("id") + 14) * 3) - 17) * 2;
		String pepper = request.getServletContext().getInitParameter("HYDAR_pepper");
		String encP = n + inputtedP + x + pepper;
		
		//encode password
		MessageDigest digest = MessageDigest.getInstance("SHA3-256");
		
		byte[] encodedhash = digest.digest(
		  encP.getBytes(StandardCharsets.UTF_8));
		/**StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
		for(int i = 0; i < encodedhash.length; i++){
			String hex = Integer.toHexString(0xff & encodedhash[i]);
			if(hex.length()==1){
				hexString.append('0');
			}
			hexString.append(hex);
		}
		
		encP = hexString.toString();*/
		
		if(result.getString("username").equals(inputtedU) && Arrays.equals(result.getBytes("password"),encodedhash)){
			success = true;
			session.removeAttribute("ip");
			break;
		}
	}
	if(success == false){
		//hydarlimiter will rate limit future requests based on this
		Thread.sleep(5000);
		//prompt user to go back to login page
%>
		<div class = "textbox"><div class = "textboxmove"></div></div>
		<p style = "color:rgb(255,255,255);">
		
		<div class = "hydarlogo">
		<img src="images/hydar.png" alt="hydar" >
		</div>
<%
		out.print("<p style = \"color:rgb(255,255,255); font-family:calibri, arial; font-size:20px; z-index:1; position:absolute; text-align:right; left:50%; display:block; top:calc(50% - 130px);\">"); 
		out.print("Username not found or <br>incorrect password<br>\n");
		out.print("<br>");
		out.print("<form method=\"get\" action=\"Login.jsp\" style = \"color:rgb(255,255,255); font-family:calibri, arial; font-size:20px; z-index:1; position:absolute; text-align:right; left:calc(50% + 130px); display:block; top:calc(50% - 50px);\">");
		out.print("<td><input value=\"Back\" type=\"submit\" class = \"button3\"></td>");
		out.print("</form>");
	}else{
		//redirect to homepage
		session.setAttribute("userid", result.getInt("id"));
		session.setAttribute("username", inputtedU);
		session.setMaxInactiveInterval(-1);
		out.print("<form action=\"targetServlet\">");
		response.sendRedirect(response.encodeURL("MainMenu.jsp"));
		out.print("</form>");
	}
	conn.close();
} catch (Exception e){
	out.print("<style> body{color:rgb(255,255,255); font-family:calibri, arial; text-align:center; font-size:20px;}</style>");
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