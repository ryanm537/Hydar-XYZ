<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.sql.DataSource"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file='SkeleCheck.jsp' %>
<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	Statement stmt = conn.createStatement();
	int uid=(int)session.getAttribute("userid");
	
	int pings = 0;
	if(request.getParameter("pings")!=null){
		pings = Integer.parseInt(request.getParameter("pings").replaceAll("\"", ""));
		if(pings == 1){
			String changePings =  "UPDATE user SET pings = 1 WHERE id = ?";
			try(var ps = conn.prepareStatement(changePings)){
				ps.setInt(1,uid);
				ps.executeUpdate();
			}
			int changePingInt = stmt.executeUpdate(changePings);
		}else if(pings == 0){
			String changePings =  "UPDATE user SET pings = 0 WHERE id = ?";
			try(var ps = conn.prepareStatement(changePings)){
				ps.setInt(1,uid);
				ps.executeUpdate();
			}
		}
	}
	
	int volume = 0;
	int pingvolume = 0;
	int vcvolume = 0;
	if(request.getParameter("mastervolume")!=null){
		volume = (Integer.parseInt(request.getParameter("mastervolume")));
		String changeVolume = "UPDATE user SET volume = ? WHERE id = ?";
		try(var ps = conn.prepareStatement(changeVolume)){
			ps.setInt(1,volume);
			ps.setInt(2,uid);
			ps.executeUpdate();
		}
	}
	if(request.getParameter("vcvolume")!=null){
		vcvolume = (Integer.parseInt(request.getParameter("vcvolume")));
		String changevcVolume = "UPDATE user SET vcvolume = ? WHERE id = ?";
		try(var ps = conn.prepareStatement(changevcVolume)){
			ps.setInt(1,vcvolume);
			ps.setInt(2,uid);
			ps.executeUpdate();
		}
	}
	if(request.getParameter("pingvolume")!=null){
		pingvolume = (Integer.parseInt(request.getParameter("pingvolume")));
		String changepingVolume = "UPDATE user SET pingvolume = ? WHERE id = ?";
		try(var ps = conn.prepareStatement(changepingVolume)){
			ps.setInt(1,pingvolume);
			ps.setInt(2,uid);
			ps.executeUpdate();
		}
	}
	
	
}catch (Exception e){
	//TODO: show if failed on profil maybe
	out.print("<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style>");
	out.print("A known error has occurred\n");
	out.print("<br><br>");
	out.print("<form method=\"post\" action=\""+response.encodeURL("Logout.jsp")+"\">");
	out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
	out.print("</form>");
	e.printStackTrace();
}
%>