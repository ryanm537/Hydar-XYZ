<%@page import="java.net.InetAddress"%>
<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="java.util.Base64,java.sql.*	"%>
<%@ include file="SkeleAdd.jsp" %>

<%
//for reverse proxies
/**String ip = request.getHeader("X-Forwarded-For");  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
  ip = request.getHeader("Proxy-Client-IP");  
}  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
  ip = request.getHeader("WL-Proxy-Client-IP");  
}  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
  ip = request.getHeader("HTTP_CLIENT_IP");  
}  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
  ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
}  
if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
  ip = request.getRemoteAddr();  
}*/
%><%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	int board = Integer.parseInt(request.getParameter("board")); 
	Statement stmt = conn.createStatement();
	int uid=(int)session.getAttribute("userid");
	// CHECK PERM
	if(uid==3){
		if(board>3||board<=0){
			response.sendRedirect(response.encodeURL("Login.jsp"));
		}else{
			String ip = request.getRemoteAddr();
			byte[] addr=InetAddress.getByName(ip).getAddress();
			try(var ps=conn.prepareStatement("SELECT 1 FROM ban WHERE addr=?")){
				ps.setBytes(1,addr);
				try(var rs=ps.executeQuery()){
					if(rs.next())
						response.sendRedirect(response.encodeURL("Login.jsp"));
				}
			}
		}
		return;
	}
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