<%@page import="java.net.InetAddress"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file="SkeleAdd.jsp" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Submitting Post... - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<body style="
	body{
		background-image:url('hydarface.png');
		background-repeat:no-repeat;
		background-attachment:fixed;
		background-size:100% 150%;
		background-color:rgb(51, 57, 63);
		background-position: 0% 50%;
	}">
<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
//dont allow direct invoke
Long time=(Long)request.getAttribute("HYDAR_TIMESTAMP");
if(time==null){
	response.sendRedirect(response.encodeURL("MainMenu.jsp"));
	return;
}
	
try(Connection conn=dataSource.getConnection()){
	//UPDATE DATABASE AFTER SUBMITTING POST
	
	int board = Integer.parseInt(request.getParameter("board_num")); 
	int uid = (int)session.getAttribute("userid");
	
	// CHECK PERM
	
	String str = "SELECT isin.user, isin.board FROM isin WHERE isin.board = ? AND isin.user = ?";
	var ps = conn.prepareStatement(str);
	ps.setInt(1,board);
	ps.setInt(2,uid);
	var result=ps.executeQuery();
	if(!result.next()){
		throw new Exception();
	}	
	int newID=-1;
	if(request.getParameter("input_text") != null){
		String inputText = request.getParameter("input_text");
		//TODO: actualContents limits the length of output a lot
		//TODO: is html inject even prevented
		
		try(PreparedStatement addPost=conn.prepareStatement("INSERT INTO post(`contents`, `board`, `created_date`,`addr`)"
		+ " VALUES (?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
			addPost.setString(1,inputText);
			addPost.setInt(2,board);
			addPost.setLong(3,time);
			if(uid==3){
				byte[] addr=((InetAddress)session.getAttribute("ip")).getAddress();
				Objects.requireNonNull(addr);
				try(var ps2=conn.prepareStatement("SELECT 1 FROM ban WHERE addr=?")){
					ps2.setBytes(1,addr);
					try(var rs=ps.executeQuery()){
						if(rs.next())throw new Exception();	
					}
				}
				addPost.setBytes(4,addr);
			}else addPost.setNull(4,Types.VARBINARY);
			addPost.executeUpdate();
			ResultSet keys = addPost.getGeneratedKeys();
			if(keys.next())
				newID=keys.getInt(1);
			else throw new Exception("no newID");
		}
		try(PreparedStatement addPosts=conn.prepareStatement("INSERT INTO posts(`user`, `post`) VALUES(?,?);")){
			addPosts.setInt(1,uid);
			addPosts.setInt(2,newID);
			addPosts.executeUpdate();
		}
		
	}
	
	//REDIRECT BACK TO PREVIOUS BOARD
	response.sendRedirect(response.encodeURL("Homepage.jsp?board="+board));
	
}catch(Exception e){
	response.setStatus(500);
	return;
}
%>
</body>
</html>