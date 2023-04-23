<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="java.util.Base64,java.sql.*	"%>

<%

Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	int board = Integer.parseInt(request.getParameter("board")); 
	Statement stmt = conn.createStatement();
	int uid=(int)session.getAttribute("userid");
	// CHECK PERM
	
	try(var q = conn.prepareStatement("SELECT board FROM isin WHERE board = ? AND user = ?")){
		q.setInt(1,board);
		q.setInt(2,uid);
		ResultSet result = q.executeQuery();
		if(!result.next()){
			response.sendRedirect(response.encodeURL("Login.jsp"));
		}
	}
	//Thread.startVirtualThread(()->{
		//String updateLastVisitedS="UPDATE isin SET lastvisited = ? WHERE user = ? AND board = ?";
		//try(var ps = conn.prepareStatement(updateLastVisitedS)){
		//	ps.setLong(1,System.currentTimeMillis());
		//	ps.setInt(2,uid);
		//	ps.setInt(3,board);
		//	int updateLastVisited = ps.executeUpdate();
		//}catch(Exception e){
			
		//}
	//});
	}catch(Exception e){
		response.sendRedirect(response.encodeURL("Login.jsp"));
		//e.printStackTrace();
	}
%>