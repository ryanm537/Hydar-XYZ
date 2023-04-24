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
<title>Creating Board ...</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>

<body style = "background-color:rgb(51, 57, 63);"> 
<center>
<style type="text/css">
form{ display: inline-block; }
</style>

<div id="show">
</div>



<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	String newBoard=request.getParameter("input_create");
	if(newBoard!=null){
		newBoard=newBoard.replace("<", "&lt;").replace(">","&gt;");
	}else newBoard="";
	//CREATE AN ID
	String str;
	ResultSet result;
	int uid=(int)session.getAttribute("userid");
	// CHECK IF BOARD IS CHANNEL, AND GET PUBLICITY SETTING
	int channelof = -1;
	int publicity = 0;
	try{
		if(request.getParameter("channelof")!=null){
			channelof = Integer.parseInt(request.getParameter("channelof").replace("\"", ""));
			str = "SELECT board.creator, board.public FROM board WHERE board.number = ?";
			var ps=conn.prepareStatement(str);
			ps.setInt(1,channelof);
			result = ps.executeQuery();
			if(result.next()){
				publicity = result.getInt("board.public");
				if(result.getInt("board.creator")!=(int)session.getAttribute("userid")){
					channelof = -1;
				}
			}
		}
	}catch(Exception e){
		
	}
	
	//CHECK IF THE BOARD IS A DM SERVER
	int dm = 0;
	int dmID = -1;
	String dmName = "";
	String tmp_id = request.getParameter("input_dm");
	if(tmp_id!=null){
		dmID=Integer.parseInt(tmp_id);
		if(dmID==3){
			response.sendRedirect(response.encodeURL("MainMenu.jsp"));
			return;
		}
		//user to dm is found, get his name
		
		str = "SELECT user.username FROM user WHERE user.id = ?";
		try{
				var ps=conn.prepareStatement(str);
				ps.setInt(1,dmID);
				result = ps.executeQuery();
				
				if(result.next()){
					dmName = result.getString("user.username") + " and " + session.getAttribute("username");
				}
				
				dm = 1;
			
		}catch(Exception e){
			dm = 0;
		}
	}
	// if dm is 1, check if the board already exists
	int exists = 0;
	int boardID = 1;
	if(dm == 1){
		str = "SELECT DISTINCT x.board "
			+"FROM (SELECT isin.board, isin.user FROM board, isin WHERE isin.user = ?) x, "
			+"(SELECT isin.board, isin.user FROM board, isin WHERE isin.user = ?) y, "
			+"(SELECT board.number FROM board WHERE board.dm = 1) z "
			+"WHERE x.board = y.board and z.number = x.board and z.number = y.board";
		var ps=conn.prepareStatement(str);
		ps.setInt(1,dmID);
		ps.setInt(2,uid);
		result = ps.executeQuery();
		if(result.next()){
			exists = 1;
			boardID = result.getInt("x.board");
		}
	}
	
	
	if(exists == 0 && dmID!=uid){
		// CHECK PERMS
		str = "SELECT user.permission_level FROM user WHERE user.id = ?";
		var ps=conn.prepareStatement(str);
		ps.setInt(1,uid);
		result = ps.executeQuery();
		
		String perms = "";
		if(result.next()){
			perms = result.getString("user.permission_level");
		}

		if(!perms.equals("water_hydar") && !perms.equals("great_white")){
			//throw new Exception();
		}

		
		// CREATE THE BOARD
		str = "INSERT INTO board(`creator`, `name`, `public`, `image`, `channelof`, `dm`) VALUES (?,?,?,?,?,?)";
		ps=conn.prepareStatement(str,Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1,uid);
		ps.setString(2,dm==0?newBoard:dmName);
		ps.setInt(3,publicity);
		ps.setString(4,dm==0?"misc.png":"PrivateMessage.png");
		ps.setInt(5,channelof);
		ps.setInt(6,dm);

		int addBoard = ps.executeUpdate();

		ResultSet newID = ps.getGeneratedKeys();
		if(newID.next())
			boardID=newID.getInt(1);
		else throw new Exception("no newid????");
		// UPDATE THE USER'S BOARDS
		str = "INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (?, ?, 0)";
		ps=conn.prepareStatement(str);
		ps.setInt(1,uid);
		ps.setInt(2,boardID);
		addBoard = ps.executeUpdate();
		
		if(dm!=0){
			ps.setInt(1,dmID);
			ps.executeUpdate();
		}
		
		// ADD USERS FROM MAIN BOARD TO CHANNEL IF ITS A CHANNEL
		str = "SELECT isin.user FROM isin WHERE isin.board = ?";
		ps=conn.prepareStatement(str);
		ps.setInt(1,channelof);
		ResultSet result2 = ps.executeQuery();
		while(result2.next()){
			try{
				str = "INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (?,?, 0)";
				ps=conn.prepareStatement(str);
				ps.setInt(1,result2.getInt("isin.user"));
				ps.setInt(2,boardID);
				ps.executeUpdate();
			}catch(Exception e){}
		}
	}
	
	
	
	//out.print("<form action=\"targetServlet\">");
	response.sendRedirect(response.encodeURL("Homepage.jsp?board="+boardID));
	return;
	//out.print("</form>");
	
}catch (Exception e){
	response.setStatus(500);
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