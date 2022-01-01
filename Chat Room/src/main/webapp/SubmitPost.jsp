<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Submitting Post... - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<body>
<body style = "background-color:rgb(51, 57, 63);"> 
<%


Class.forName("com.mysql.jdbc.Driver").newInstance();
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");

try{
	//UPDATE DATABASE AFTER SUBMITTING POST
	
	int board = Integer.parseInt(request.getParameter("board_num").toString()); 
	
	Statement stmt = conn.createStatement();
	
	if(request.getParameter("input_text") != null){
		String searchPostsForIDStr = "SELECT MAX(id) AS max FROM post";
		ResultSet searchPosts = stmt.executeQuery(searchPostsForIDStr);
		searchPosts.next();
		int newID = searchPosts.getInt("max") + 1;
		
		String addPostStr="INSERT INTO post(`contents`, `id`, `board`, `created_date`)"
					+ " VALUES (\"" + request.getParameter("input_text").toString() + "\", " + newID + ", " + board + ", " + System.currentTimeMillis() + ")";
		int addPost = stmt.executeUpdate(addPostStr);
		
		String addPostsStr="INSERT INTO posts(`user`, `post`)"
				+ " VALUES (" + session.getAttribute("userid").toString() + ", " + newID + ")";
		int addPosts = stmt.executeUpdate(addPostsStr);
	}
	
	//REDIRECT BACK TO PREVIOUS BOARD
	
	out.print("</form>");
	//response.sendRedirect("Homepage.jsp?board="+board);
	
}catch(Exception e){
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
