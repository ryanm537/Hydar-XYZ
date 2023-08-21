<%@page import="java.security.MessageDigest"%>
<%@page import="java.util.Base64"%>
<%@page import="java.security.SecureRandom"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="java.sql.Connection"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%-- Creates a user with a new random password. Only for admins! --%>
<%!
final SecureRandom rng=new SecureRandom();%>
<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");

try(Connection conn=dataSource.getConnection()){
	int uid=(int)session.getAttribute("userid");
	// CHECK PERMS
	
	String str = "SELECT user.permission_level FROM user WHERE user.id = ?" ;
	var ps = conn.prepareStatement(str);
	ps.setInt(1,uid);
	var result=ps.executeQuery();
	String perms = "";
	if(!result.next()||!result.getString("user.permission_level").equals("water_hydar")){
		response.sendRedirect(response.encodeURL("Login.jsp"));
		return;
	}
	
	
	
	if(request.getMethod().equals("POST")){
		
		str = "SELECT MAX(id)+1 AS newID FROM user" ;
		var stmt= conn.createStatement();
		result = stmt.executeQuery(str);
		if(!result.next()){
			throw new Exception();
		}
		
		
		String newUser=new String(request.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
		int newID=result.getInt("newID");
		byte[] pw = new byte[24];
		rng.nextBytes(pw);
		String password=Base64.getUrlEncoder().encodeToString(pw);
		
		int n = ((((12 + newID) * 49) - 86) / 2) + 15;
		int x = (((newID + 14) * 3) - 17) * 2;
		String pepper = request.getServletContext().getInitParameter("HYDAR_pepper");
		String encP = n + password + x + pepper;
		
		//encode password
		MessageDigest digest = MessageDigest.getInstance("SHA3-256");
		
		byte[] encodedhash = digest.digest(
		  encP.getBytes(StandardCharsets.UTF_8));
		
		try(PreparedStatement addUser = conn.prepareStatement("INSERT INTO user(`username`, `password`, `pfp`, `permission_level`, `created_date`, `pings`, `volume`, `pingvolume`, `vcvolume`, `addr`) "
			+ " VALUES(?, ?, \"images/hydar2.png\", \"great_white\", ?, 0, 50, 50, 50, ?)",Statement.RETURN_GENERATED_KEYS)){
			addUser.setString(1,newUser);	
			addUser.setBytes(2,encodedhash);
			addUser.setLong(3,System.currentTimeMillis());
			addUser.setBytes(4,new byte[0]);
				addUser.executeUpdate();
				ResultSet keys = addUser.getGeneratedKeys();
				if(!keys.next() || keys.getInt(1)!=newID){
					out.print("Bad id");
					return;
				}
		}

		try(PreparedStatement defaultBoards = conn.prepareStatement("INSERT INTO isin(`user`, `board`, `lastvisited`) VALUES (?,?,0)")){
			defaultBoards.setInt(1,newID);
			for(int i=1;i<=3;i++){
				defaultBoards.setInt(2,i);
				defaultBoards.executeUpdate();
			}
		}
		out.print("New GW user generated!<br>Your username is <i>"+newUser+"</i><br>Your password(PLEASE CHANGE IT) is <i>"+password+"</i>");
	}else{
		%>
		Add GW user....<br>
		<form id = "addform" onsubmit="send();return false;" action="" target="">
			<input id="ia1" type="text"   name="input_create" size="150" style="background-color:rgb(73, 76, 82);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "username"/>
			<input id = "ia2" value="Send"  type="submit" class = "button3" />
		</form>
				<div id="response" style="display:inline"></div>
		<script>
		var URL2="NewUser.jsp";
		function post(url,x){ 
			const xhr = new XMLHttpRequest();
			xhr.open("POST", url, true);
			xhr.setRequestHeader("content-type","text/plain");
			xhr.onreadystatechange = () => {
			  if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
				document.getElementById("response").innerHTML="<hr>Response: <br><hr>"+xhr.responseText+"<br>";
			  }
			}
			xhr.send(x);
		}
		async function send(){
			var query=document.getElementById("ia1").value;
			post(URL2,query)
			console.log(document.getElementById("ia1").value);
			document.getElementById("ia1").value="";
			return false;
		}

		</script>
		<% 
	}
}%>
