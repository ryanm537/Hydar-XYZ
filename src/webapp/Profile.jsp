<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="java.security.MessageDigest"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file='SkeleCheck.jsp' %>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Profile - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<style type="text/css">
form{ display: inline-block; }
	.fix-div{
		background-color:rgb(41, 47, 53);
		opacity:90%;
		position:fixed; 
		width:100%; 
		left:0; 
		top:0;
		box-shadow: 0 0 10px rgba(0,0,0,20);
		z-index:1;}
	.bar{
		margin-left:auto;
		margin-right:auto;
	}
	.margin{
		margin-top:50px;
	}
</style>
<body style = "background-color:rgb(51, 57, 63); overflow-x:hidden;"> 


<div id="show">
</div>
<%@ include file = "Util.jsp" %>

	<div class = "fix-div">

<%
Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	
	
	// MARGIN SETUP
		// GET USERNAME/PASSWORD CHANGE INFO IF APPLICABLE
			int uid = (int)session.getAttribute("userid");
			String newUsername = "";
			String newPassword = "";
			String confirmPassword = "";
			String perms = "";
			
			try(var checkperms = conn.prepareStatement("SELECT permission_level FROM user WHERE user.id = ?")){
				checkperms.setInt(1,uid);
				checkperms.executeQuery();
				ResultSet resultForPerms = checkperms.executeQuery();
				while(resultForPerms.next()){
					perms = resultForPerms.getString("permission_level");
				}
			}
			
			
			if((perms.equals("great_white") || perms.equals("water_hydar")) && request.getParameter("new_username")!=null){
				newUsername = request.getParameter("new_username").toString();
				var checkAvailable = conn.prepareStatement("SELECT user.username FROM user WHERE user.username = ?");
				checkAvailable.setString(1,newUsername);
				ResultSet checkAvail = checkAvailable.executeQuery();
				int taken = 0;
				while(checkAvail.next()){
					taken = 1;
				}
				if(taken == 0){
					var updateUsername = conn.prepareStatement("UPDATE user SET user.username = ? WHERE user.id = ?");
					updateUsername.setString(1,newUsername);
					updateUsername.setInt(2,uid);
					session.setAttribute("username", newUsername);
					updateUsername.executeUpdate();
				}else{
					out.print("<body onload = \"displayUsernameWarning();\">");
				}
			}
			if((perms.equals("great_white") || perms.equals("water_hydar")) && request.getParameter("new_password")!=null && request.getParameter("new_password").length() <= 30){
				newPassword = request.getParameter("new_password").toString();
				if(request.getParameter("confirm_password")!=null){
			confirmPassword = request.getParameter("confirm_password");
			if(newPassword.equals(confirmPassword)){
				int n = ((((12 + (int)session.getAttribute("userid")) * 49) - 86) / 2) + 15;
				int x = ((((int)session.getAttribute("userid") + 14) * 3) - 17) * 2;

				String pepper = request.getServletContext().getInitParameter("HYDAR_pepper");
				String encP = n + newPassword + x + pepper;
				
				//encode password
				MessageDigest digest = MessageDigest.getInstance("SHA3-256");
				byte[] encodedhash = digest.digest(
				  encP.getBytes(StandardCharsets.UTF_8));
				/**
				StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
				for(int i = 0; i < encodedhash.length; i++){
					String hex = Integer.toHexString(0xff & encodedhash[i]);
					if(hex.length()==1){
						hexString.append('0');
					}
					hexString.append(hex);
				}
				
				encP = hexString.toString();
				*/
				try(var updatePass=conn.prepareStatement("UPDATE user SET user.password = ? WHERE user.id = ?")){
					updatePass.setBytes(1,encodedhash);
					updatePass.setInt(2,uid);
					int update = updatePass.executeUpdate();
				}
			}else{
				out.print("<body onload = \"displayPasswordWarning();\">");
			}
				}else{
			out.print("<body onload = \"displayPasswordWarning();\">");
				}
			}
			
			//TOP BAR
			
			out.print("<h1 style = \"color:rgb(255,255,255); font-size:15px; font-family:calibri, arial; text-align:right;position:relative;\"></style>");
			out.print("Hello <div id=\"profileName\" style=\"display:inline\">" + session.getAttribute("username").toString() + "</div>! | ");
			out.print("<style type=\"text/css\"> a{color:LightGrey; font-family:calibri, arial; text-align:right; font-size:15px; display:inline-block;padding-top:15px;}</style>");
			out.print("<a href=\""+response.encodeURL("MainMenu.jsp")+"\"> Home</a>&nbsp;| ");
			out.print("<a href=\""+response.encodeURL("Logout.jsp")+"\"> Log out</a> &nbsp;&nbsp;");
			
			out.print("<style type=\"text/css\"> h1{color:rgb(255,255,255); text-align:left; margin-right:10px; font-size:15px;}</style>");
			out.print("<img src=\"images/hydar.png\" alt=\"hydar\" width = \"25px\" height = \"40px\" align = \"center\">");
			
			out.print("</h1></div><div class = \"margin\">");
			
			// DISPLAY USERNAME
			
			out.print("<style> p4 .test{} .test:hover{transform:scale(1.3);} </style>");
			
			out.print("<br><style> body{color:rgb(255,255,255); font-family:calibri, arial; text-align:center; font-size:30px;position:relative; top:20px}</style>");
			
			out.print("<p4 class = \"test\"><b>" + session.getAttribute("username").toString() +"</b>&nbsp&nbsp(#" + session.getAttribute("userid").toString()+ ")</p4>");
		%>

	
	<div id='bar' class='bar'  style='width: 720px; height: 35px; border-bottom: 2px solid rgb(71, 77, 83); text-align: center;'></div>
	<%
	// GET USER PFP
			
			ResultSet result = new SQL(conn,"SELECT user.pfp, user.username FROM user WHERE user.username = ?")
				.setString(session.getAttribute("username"))
				.query();
			
			// GREY TEXT
			
			out.print("<style> p3{color:White; font-family:calibri, arial; text-align:center; font-size:25px;}</style>");
			out.print("<p3><br> Profile Picture: </p3>");
			
			out.print("<style> p{color:LightSlateGrey; font-family:calibri, arial; text-align:center; font-size:15px;}</style>");
			out.print("<br><p> - Currently Selected - </p>");
			
			
			// PFP STYLING
	%> 
		<style>
			ul {padding:0px;padding-top: 10px;margin: 10px 20px; list-style: none;}
		    ul li {display: inline-block;vertical-align:middle; }
		    ul li a img {margin-top: 0px;width: 100px; height: 100px;display: inline-block;}
			ul li a:hover img {transform: scale(1.3);box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);}
			
		    ul li2 a img {margin-top: -20px;width: 150px;height: 150px;display: inline-block;box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);}
			ul li2 a:hover img {transform: scale(1.1);}
			 
			.column {float: left;}
			.row::after {content: "";clear: both;display: block;} 
			
			.pfps{display:inline-block;margin-left:auto;margin-right:auto;}
			.button{
				background-color:rgb(61, 67, 83);
				color:white;border:none;
				padding:10px 8px; 
				position:relative; 
				left:3px;
				border-radius:8px;
			}
			.button:hover{
				background-color:rgb(61, 97, 183);
				cursor:pointer;
			}
		</style>
	<%
	// PROFILE PIC
			String pfp = "";
			while(result.next()){
		pfp = result.getString("user.pfp");
			}
			String[] pfps = {"images/yeti.png", "images/hydar2.png", "images/emp.png", "images/gw.png", "images/grim.png"};
			
			//order the pfp array so that yours is in the mididle
			int indexOfPfp = 0;
			for(int i = 0; i < 5; i++){
				if (pfps[i].equals(pfp)){
					indexOfPfp=i;
					break;
				}
			}
			
			if(!pfps[2].equals(pfp)){
				String temp = pfps[2];
				pfps[2]=pfp;
				pfps[indexOfPfp] = temp;
			}
			out.print("<div class = \"pfps\"><div class = \"row\">");
			for(int i =0; i<5; i++){
				out.print("<div class = \"column\">");
				if(i!=2){
					String ch=response.encodeURL("ChangePfp.jsp?new_pfp="+ pfps[i]);
					out.print("<ul><li><a href=\""+ch+"\"><img src=\"" + pfps[i] +"\" alt=\"hydar"+ i +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
				}else{
					out.print("<ul><li2><a href=\"#\"><img src=\"" + pfps[i] +"\" alt=\"hydar"+ i +"\" width = \"100px\" height = \"100px\"></a></li2> </ul> </div>");
				}
			}
			out.print("</div></div><br>");
	%>
	<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(71, 77, 83); text-align: center;'></div>
	</div>
	
	<style>
		.accountsettings{
			font-size:25px;
			margin-top:20px;
		}
		.wordNotif{
			position:relative;
			left:-220px;
			top:28px;
			font-size: 15px;
			color: white;
			overflow:hidden;
		}
		.switchText{
			position:relative;
			font-size: 15px;
			color: white;
			left:-50px;
			top:8px;
			overflow:hidden;
			margin-left:auto;
			margin-right:auto;
		}
		.switch{
			display:block;
			background-color:LightSlateGray;
			border-radius:10px;
			width:30px;
			height:15px;
			position:relative;
			left:40px;
			margin-left:auto;
			margin-right:auto;
			top:-16px;
			overflow:hidden;
			opacity:0.9;
		}
		.switch:hover{
			opacity:1;
		}
		.switchButton{
			background-color:rgb(61, 97, 183);
			height:15px;
			width:15px;
			border-radius:15px;
			margin-left:auto;
			margin-right:auto;
			position:relative;
			left:5px;
			overflow:hidden;
		}
		#switch{
			cursor:pointer;
			margin-left:auto;
			margin-right:auto;
		}
		#switchButton{
			cursor:pointer;
		}
		
	</style>
	
	<div class ="accountsettings"> Account</div>
	<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(51, 57, 63); text-align: center;'></div>
	
	
	<div class = "wordNotif">Pings: 
	</div>
	<div class = "switchText">&nbsp
		<onThing id = "onThing" >On
		</onThing>
		<offThing id = "offThing" hidden>Off
		</offThing>
		<div class = "switch" id = "switch">
			<div class = "switchButton" id = "switchButton">
			</div>
		</div>
	</div>
	
	
	<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(51, 57, 63); text-align: center;'></div>
	
	<style>
		.notifsText{
			position:relative;
			left:-170px;
			top:30px;
			font-size: 15px;
			color: white;
			overflow:hidden;
		}
		.notifbutton{
			background-color:rgb(61, 67, 83);
			color:white;border:none;
			padding:10px 8px; 
			position:relative; 
			left:0px;
			top:-2px;
			border-radius:8px;
		}
		.notifbutton:hover{
			background-color:rgb(61, 97, 183);
			cursor:pointer;
		}
	</style>
	<div class="notifsText">Desktop Notifications: </div>
	<input id = "enableNotifications" value="Click to enable..."  type="submit" class="notifbutton"></input>
	
	
	<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(51, 57, 63); text-align: center;'></div>
	
	<style>
		.volumeText{
			position:relative;
			left:-190px;
			top:28px;
			font-size: 15px;
			color: white;
			overflow:hidden;
		}
		.pingvolumeText{
			position:relative;
			left:-198px;
			top:28px;
			font-size: 15px;
			color: white;
			overflow:hidden;
		}
		.vcvolumeText{
			position:relative;
			left:-167px;
			top:28px;
			font-size: 15px;
			color: white;
			overflow:hidden;
		}
		.volumeSlider{
			-webkit-appearance: none; 
			width:100px;
			height:15px;
			background:lightslategray;
			border-radius:20px;
			left:0px;
			position:relative;
			top:0px;
			opacity:0.9;
			cursor:pointer;
		}
		.volumeSlider:hover{
			opacity:1;
		}
		.volumeSlider::-webkit-slider-thumb{
			-webkit-appearance: none;
			width:15px;
			height:15px;
			background:rgb(61, 97, 183);
			cursor:pointer;
		}
		.volumeSlider::-moz-range-thumb{
			width:15px;
			height:15px;
			background:rgb(61, 97, 183);
			cursor:pointer;
		}
		.volumeSlider[type="range"]::-moz-range-progress {
  			background-color: rgb(51, 87, 173); 
  			height:15px;
  			border-radius:20px;
		}
	</style>
	
	<%
		int volume = 0;
			int pingvolume = 0;
			int vcvolume = 0;
			result = new SQL(conn,
					"SELECT user.volume, user.vcvolume, user.pingvolume FROM user WHERE user.id = ?")
				.setInt(uid).query();
			while(result.next()){
				volume = result.getInt("user.volume");
				pingvolume = result.getInt("user.pingvolume");
				vcvolume = result.getInt("user.vcvolume");
			}
		%>
	
	<div class="volumeText">Master Volume: </div>
	<input  type="range" class="volumeSlider" id="volume" min ="0" value = <%out.print("\"" + volume + "\"");%> max="100" step='1'>
	
	
	<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(51, 57, 63); text-align: center;'></div>
	
	<div class="pingvolumeText">Ping Volume: </div>
	<input  type="range" class="volumeSlider" id="pingvolume" min ="0" value = <%out.print("\"" + pingvolume + "\"");%> max="100" step='1'>
	
	
	<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(51, 57, 63); text-align: center;'></div>
	
	<div class="vcvolumeText">Voice (output) Volume: </div>
	<input  type="range" class="volumeSlider" id="vcvolume" min ="0" value = <%out.print("\"" + vcvolume + "\"");%> max="100" step='1'>
	
	
	<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(51, 57, 63); text-align: center;'></div>
	
	<style>
		.usernameText{
			position:relative;
			left:-10px;
			top:10px;
			margin-bottom:10px;
			font-size: 15px;
			color: white;
			overflow:hidden;
		}
		.usernameInput{
			background-color:rgb(71, 77, 83); color:white; border:none; padding:8px 10px; border-radius:8px; margin-top:10px; margin-left:67px;
		}
		.usernameInput:focus{
			 outline: none;
		}
		.passwordText{
			position:relative;
			left:-35px;
			top:10px;
			margin-bottom:10px;
			font-size: 15px;
			color: white;
			overflow:hidden;
		}
		.confirmBox{
			position:relative;
			left:83px;
		}
		.passwordInput{
			background-color:rgb(71, 77, 83); color:white; border:none; padding:8px 10px; border-radius:8px; margin-top:10px; margin-left:71px;
		}
		.passwordInput:focus{
			 outline: none;
		}
		.notMatchText{
			color:rgb(248, 61, 72);
			position:relative;
			left: 93px;
			font-size:15px;
			top: 5px;
		}
		.takenText{
			color:rgb(248, 61, 72);
			position:relative;
			left: 57px;
			font-size:15px;
			top: 5px;
		}
		.button3{
			dsiplay:inline-block;
			background-color:rgb(61, 67, 83);
			color:white;border:none;
			padding:8px; 
			position:relative; 
			left:3px;
			top:0px;
			border-radius:8px;
		}
		.button3:hover{
			background-color:rgb(61, 97, 183);
			cursor:pointer;
		}
	</style>
	
	
	
	
	<form id = "changeUsername" method="get" action="#">
		<div class="usernameText">Change Username: 
			<%
			if(request.isRequestedSessionIdFromURL()){
				%><input type="hidden" name="HYDAR_sessionID" value=<%=session.getId() %>><%
			}
			 %> 
			<input id="ir1" type="text" name="new_username" size="30" class="usernameInput" placeholder = "Enter new username..."></input>
			<input id = "ia2" value="Enter"  type="submit" class = "button3" ></input>
		</div>
		
		<div class = "takenText" hidden='' id = "takenText"> Username is already taken!
		</div>
		
	</form>
	
	<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(51, 57, 63); text-align: center;'>
	</div>
	
	<form id = "changePassword" method="get" action="#">
		<div class="passwordText">Change Password:
			<%
			if(request.isRequestedSessionIdFromURL()){
				%><input type="hidden" name="HYDAR_sessionID" value=<%=session.getId() %>><%
			}
			 %> 
			<input id="ir1" type="password" name="new_password" size="30" class="passwordInput" placeholder = "Enter new password..."><br>
			<div class = "confirmBox">
				<input id="ir1" type="password" name="confirm_password" size="30" class="passwordInput" placeholder = "Confirm new password..."></input>
				<input id = "ia2" value="Enter"  type="submit" class = "button3" ></input>
			</div>
			
			<div class = "notMatchText" hidden='' id = "notMatchText"> Passwords do not match!
			</div>
			
			<div id='bar' class='bar'  style='width: 720px; height: 20px; border-bottom: 2px solid rgb(51, 57, 63); text-align: center;'>
			</div>
		</div>
	</form>
	
	<script>
		//username/password controls
		function displayPasswordWarning(){
			document.getElementById("notMatchText").removeAttribute("hidden");
		}	
		
		function displayUsernameWarning(){
			document.getElementById("takenText").removeAttribute("hidden");
		}
		function $get(url){
			return new Promise((resolve,reject)=>{
				const xhr = new XMLHttpRequest();
				xhr.open("GET", url, true);
				//xhr.setRequestHeader("content-type","text/plain");
				xhr.onreadystatechange = () => {
					if (xhr.readyState === XMLHttpRequest.DONE){
						if(xhr.status === 200||xhr.status ===204) {
							resolve();
						}else if(xhr.status >=400||xhr.status===0){
							reject(); 
						}
					}
				}
				xhr.send();
			});
		}
		//volume controls
		const changeVol = document.getElementById("volume");
		changeVol.addEventListener('change', changeVolume);
		const changepingVol = document.getElementById("pingvolume");
		changepingVol.addEventListener('change', changepingVolume);
		const changevcVol = document.getElementById("vcvolume");
		changevcVol.addEventListener('change', changevcVolume);
		
		function changeVolume(){
			var vol = this.value;
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Profile.jsp","UserSettings.jsp");
			if(x.indexOf('?')<0)n=x.replace("Profile.jsp","UserSettings.jsp");
			$get(n+"?mastervolume=" + vol).then();
	    };

		function changevcVolume(){
			var vol = this.value;
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Profile.jsp","UserSettings.jsp");
			if(x.indexOf('?')<0)n=x.replace("Profile.jsp","UserSettings.jsp");
			$get(n+"?vcvolume=" + vol).then();
	    };
		
	    function changepingVolume(){
			var vol = this.value;
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Profile.jsp","UserSettings.jsp");
			if(x.indexOf('?')<0)n=x.replace("Profile.jsp","UserSettings.jsp");
			$get(n+"?pingvolume=" + vol).then();
	    };
	    
	    //notification controls
		const notif = document.getElementById('enableNotifications');
				notif.addEventListener("click", () => {
					Notification.requestPermission().then(function(permission) {});
				}
		);
				

		//ping on/off
				
		<%result = new SQL(conn,"SELECT user.pings FROM user WHERE user.id = ?").setInt(uid).query();
		int pings = 0;
		while(result.next()){
			pings = result.getInt("user.pings");
		}%>	
		let switched = 0;
		<%if(pings == 0){
			System.out.println(pings);%>
			document.getElementById("switchButton").style.left = "-8px";
			document.getElementById("offThing").removeAttribute("hidden");
			document.getElementById("onThing").setAttribute("hidden", true);
			switched = 1;
			<%}else if (pings == 1){%>
			document.getElementById("switchButton").style.left = "8px";
			document.getElementById("onThing").removeAttribute("hidden");
			document.getElementById("offThing").setAttribute("hidden", true);
			switched = 0;
			<%}%>
		
		function left (){
			document.getElementById("switchButton").style.left = "8px";
			document.getElementById("onThing").removeAttribute("hidden");
			document.getElementById("offThing").setAttribute("hidden", true);
			switched -= 1;

			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Profile.jsp","UserSettings.jsp");
			if(x.indexOf('?')<0)n=x.replace("Profile.jsp","UserSettings.jsp");
			$get(n+"?pings=1").then();
		}
		function right (){
			document.getElementById("switchButton").style.left = "-8px";
			document.getElementById("offThing").removeAttribute("hidden");
			document.getElementById("onThing").setAttribute("hidden", true);
			switched += 1;
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Profile.jsp","UserSettings.jsp");
			if(x.indexOf('?')<0)n=x.replace("Profile.jsp","UserSettings.jsp");
			$get(n+"?pings=0").then();
		}
				
		
		const pingswitch = document.getElementById('switch');
		pingswitch.addEventListener("click", () => {
			if(switched == 0){
				right();
			}else{
				left();
			}
			
			}
		);
			
	</script>
	<%
	}catch (Exception e){
			out.print("<style> body{color:rgb(255,255,255); font-family:calibri, arial; text-align:center; font-size:20px;}</style>");
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