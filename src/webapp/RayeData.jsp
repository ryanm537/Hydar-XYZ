<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*,java.sql.*, java.nio.file.Files, java.nio.file.Paths"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Raye Data</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<body>
<style>
	
</style>
<center>
<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	
	try{
		
		Files.deleteIfExists(Paths.get("./bots/raye_questions.txt"));
		Files.deleteIfExists(Paths.get("./bots/raye_responses.txt"));
	}catch(Exception e){
		e.printStackTrace();
	}
	FileWriter f = new FileWriter("./bots/raye_questions.txt", true);
	FileWriter f2 = new FileWriter("./bots/raye_responses.txt", true);
	
	
	Statement stmt = conn.createStatement();
	String str = "SELECT number FROM board";
	ResultSet result = stmt.executeQuery(str);
	while(result.next()){
		Statement stmt2 = conn.createStatement();
		String str2 = "SELECT post.contents, post.created_date, posts.user FROM post, posts, isin WHERE post.board = " + result.getInt("number") + " AND posts.post = post.id AND post.board = isin.board AND isin.user = 2";
		ResultSet result2 = stmt2.executeQuery(str2);
		String prevContents = "";
		long prevTime = 0;
		int prevPoster = 0;
		if(result2.next()){
	prevContents = result2.getString("post.contents");
	prevTime = result2.getLong("post.created_date");
	prevPoster = result2.getInt("posts.user");
		}
		while(result2.next()){
	String currentContents = result2.getString("post.contents");
	long currentTime = result2.getLong("post.created_date");
	int currentPoster = result2.getInt("posts.user");
	if((currentTime-prevTime)<60000 && prevPoster!=currentPoster && currentPoster!=2 && !prevContents.startsWith("raye") && !currentContents.startsWith("raye ") 
	&& prevContents.length()>0 && currentContents.length()>0 && !prevContents.startsWith("/") && !currentContents.startsWith("/") 
	&& !prevContents.startsWith("warning: no auctions found for") && !currentContents.startsWith("warning: no auctions found for") 
	&& !prevContents.contains("\n")  && !currentContents.contains("\n")
	&& !prevContents.startsWith("Read only has been switched to O") && !currentContents.startsWith("Read only has been switched to O")
	&& !prevContents.startsWith("removed user #") && !currentContents.startsWith("removed user #")){
		f.write(prevContents + "\n");
		f2.write(currentContents + "\n");
	}
	prevContents = currentContents;
	prevTime = currentTime;
	prevPoster = currentPoster;
		}
	}
	
	f.close();
	f2.close();
	
	conn.close();
} catch (Exception e){
	out.print("<style> body{color:rgb(255,255,255); font-family:calibri; text-align:center; font-size:20px;}</style>");
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