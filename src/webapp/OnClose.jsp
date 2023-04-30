<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="java.util.Base64,java.sql.*	"%>

<%

	Class.forName("com.mysql.jdbc.Driver");
	DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
	try(Connection conn=dataSource.getConnection()){
		int uid=(int)session.getAttribute("userid");
		int board = Integer.parseInt(request.getParameter("board")); 
		String updateLastVisitedS="UPDATE isin SET lastvisited = ? WHERE user = ? AND board = ?";
		var ps = conn.prepareStatement(updateLastVisitedS);
		ps.setLong(1,System.currentTimeMillis());
		ps.setInt(2,uid);
		ps.setInt(3,board);
		ps.executeUpdate();
		
	}catch(Exception e){
		
		//e.printStackTrace();
	}
%>