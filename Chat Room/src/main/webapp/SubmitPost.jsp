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
<script type="text/javascript" src ="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
<style>
	body{
		background-image:url('hydarface.png');
		background-repeat:no-repeat;
		background-attachment:fixed;
		background-size:100% 150%;
		background-color:rgb(51, 57, 63);
		background-position: 0% 50%;
	}
</style>
<%


Class.forName("com.mysql.jdbc.Driver").newInstance();
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");

try{
	
	//UPDATE DATABASE AFTER SUBMITTING POST
	
	int board = Integer.parseInt(request.getParameter("board_num").replace("\\", "").replace("\"", "").toString()); 
	
	Statement stmt = conn.createStatement();
	
	// CHECK PERM
	
	String str = "SELECT isin.user, isin.board FROM isin WHERE isin.board = " + board + " AND isin.user = " + session.getAttribute("userid").toString();
	ResultSet result = stmt.executeQuery(str);
	if(!result.next()){
		throw new Exception();
	}
	
	if(request.getParameter("input_text") != null){
		String inputText = request.getParameter("input_text").toString();
		inputText = inputText.replace("\"", "\"\"");
		inputText = inputText.replace("\\", "\\\\");
		String searchPostsForIDStr = "SELECT MAX(id) AS max FROM post";
		ResultSet searchPosts = stmt.executeQuery(searchPostsForIDStr);
		searchPosts.next();
		int newID = searchPosts.getInt("max") + 1;

		// BOT COMMANDS
		if(inputText.substring(0,1).equals("/")){
			
			//administrator commands
			String checkIfAdmin = "SELECT board.creator FROM board WHERE board.number =" + board;
			ResultSet checkAdmin = stmt.executeQuery(checkIfAdmin);
			int boardCreator = -1;
			while(checkAdmin.next()){
				boardCreator = checkAdmin.getInt("board.creator");
			}
			if(boardCreator == Integer.parseInt(session.getAttribute("userid").toString())){
				// /admin
				if(inputText.equals("/admin")){
					inputText = "Admin commands: <br>/kick (user id)<br>/invite (user id)<br>/deleteboard<br>/inviteonly (on/off)";
					
				}
				
				// commands that take an input
				if(inputText.indexOf(" ") != -1){
					// /invite
					if(inputText.substring(0, inputText.indexOf(" ")).equals("/invite")){
						int invitedUser = Integer.parseInt(inputText.substring(inputText.indexOf(" ") + 1));
						inputText = "Sent invite to user #" + invitedUser;
						response.sendRedirect("InviteUser.jsp?invitedID=" + invitedUser + "&board_num="+board);
					}
					
					// /invite
					if(inputText.substring(0, inputText.indexOf(" ")).equals("/kick")){
						int kickedUser = Integer.parseInt(inputText.substring(inputText.indexOf(" ") + 1));
						inputText = "Removed user #" + kickedUser;
						response.sendRedirect("KickUser.jsp?kickID=" + kickedUser + "&board_num="+board);
					}
					
				}
				
				
				// /delete - prompt comfirmation
				if(inputText.equals("/deleteboard")){
					inputText = "Requested to delete board. Type \"\"/confirm-delete\"\" to confirm and delete this board.";
				}
				
				// delete - actual
				if(inputText.equals("/confirm-delete")){
					inputText = "Deleting board...";
				}
				
			}
			
			
			
		}
		String addPostStr="INSERT INTO post(`contents`, `id`, `board`, `created_date`)"
					+ " VALUES (\"" + inputText + "\", " + newID + ", " + board + ", " + System.currentTimeMillis() + ")";
		int addPost = stmt.executeUpdate(addPostStr);
		
		String addPostsStr="INSERT INTO posts(`user`, `post`, `board`)"
				+ " VALUES (" + session.getAttribute("userid").toString() + ", " + newID + ", " + board + ")";
		int addPosts = stmt.executeUpdate(addPostsStr);
		
		if(inputText.equals("Deleting board...")){
			response.sendRedirect("DeleteBoard.jsp?board_num="+board);
		}
		
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
