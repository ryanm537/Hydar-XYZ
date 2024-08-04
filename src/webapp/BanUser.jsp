<%@page import="java.util.concurrent.TimeUnit"%>
<%@page import="java.net.InetAddress"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file='SkeleCheck.jsp' %> 

<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Removing User ...</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>

<body>
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
	int kicked = Integer.parseInt(request.getParameter("kickID"));
	String ipBan = request.getParameter("ip");
	boolean unBan = Boolean.parseBoolean(request.getParameter("unban"));
	int uid=(int)session.getAttribute("userid");
	// CHECK PERMS
	
	String str = "SELECT user.permission_level FROM user WHERE user.id = ?" ;
	var ps = conn.prepareStatement(str);
	ps.setInt(1,uid);
	var result=ps.executeQuery();
	String perms = "";
	if(!result.next()||!result.getString("user.permission_level").equals("water_hydar")){
		throw new Exception();
	}
	// ADMIN PERM
	String ban="DELETE FROM user WHERE id = ?";
	String addrU="SELECT addr FROM user WHERE id = ?";
	String addrP="SELECT addr FROM post WHERE id = ?";
	String ban2="DELETE FROM user WHERE user.addr = ?";
	String ban3="INSERT INTO ban(id,type,addr) VALUES(?,?,?)";
	
	String deleteMsgA = "DELETE FROM post WHERE addr = ?";
	String deleteMsgU = "DELETE FROM post WHERE id IN (SELECT post FROM posts WHERE posts.user = ?)";
	
	String unbanIp="SELECT addr FROM ban WHERE id=? AND type=?";
	String unbanA="DELETE FROM ban WHERE addr=?";
	if(ipBan==null||ipBan.equals("no")){
		if(!unBan && kicked!=3){
			ps=conn.prepareStatement(deleteMsgU);
			ps.setInt(1,kicked);
			ps.executeUpdate();
			
			ps=conn.prepareStatement(ban);
			ps.setInt(1,kicked);
			ps.executeUpdate();
		}
	}else if(ipBan.equals("user")||ipBan.equals("message")){//Ban a user and their IP
		if(!unBan){
			ps=conn.prepareStatement(ipBan.equals("user")?addrU:addrP);
			ps.setInt(1,kicked);
			var rs=ps.executeQuery();
			if(rs.next()){
				byte[] ip = rs.getBytes(1);
				if(ip.length==0)
					throw new RuntimeException("Not anonymous");
				InetAddress addr=InetAddress.getByAddress(ip);
				if(!addr.isLoopbackAddress()){

					ps=conn.prepareStatement(deleteMsgA);
					ps.setBytes(1,ip);
					ps.executeUpdate();
					if(kicked!=3){
						ps=conn.prepareStatement(deleteMsgU);
						ps.setInt(1,kicked);
						ps.executeUpdate();
					}
					ps=conn.prepareStatement(ban2);
					ps.setBytes(1,ip);
					ps.executeUpdate();
					
					ps=conn.prepareStatement(ban3);
					ps.setInt(1,kicked);
					ps.setString(2,ipBan);
					ps.setBytes(3,ip);
					ps.executeUpdate();
					
					return;
				}
			}else throw new RuntimeException("IP not found");
		}else{
			ps=conn.prepareStatement(unbanIp);
			ps.setInt(1,kicked);
			ps.setString(2,ipBan);
			var rs =ps.executeQuery();
			if(rs.next()){
				ps=conn.prepareStatement(unbanA);
				ps.setBytes(1,rs.getBytes("addr"));
				ps.executeUpdate();
			}else throw new RuntimeException("IP not found");
		}
	}else{//Ban a string address
		InetAddress addr = InetAddress.getByName(request.getParameter("ip"));
		if(!addr.isLoopbackAddress()){
			if(!unBan){
				ps=conn.prepareStatement(ban2);
				ps.setBytes(1,addr.getAddress());
				ps.executeUpdate();
			
				ps=conn.prepareStatement(ban3);
				ps.setNull(1, Types.TINYINT);
				ps.setString(2,"addr");
				ps.setBytes(3,addr.getAddress());
				ps.executeUpdate();

				ps=conn.prepareStatement(deleteMsgA);
				ps.setBytes(1,addr.getAddress());
				ps.executeUpdate();
				
			}else{
				ps=conn.prepareStatement(unbanA);
				ps.setBytes(1,addr.getAddress());
				ps.executeUpdate();
			}
		}
	}
	out.print("<form action=\"targetServlet\">");
	out.print("</form>");
	
}catch (Exception e){
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