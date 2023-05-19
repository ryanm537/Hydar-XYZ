<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" session='false'%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head> 
<style>
	body{
		font-family:calibri, arial;
		background-image:url('images/hydarface.png');
		background-repeat:no-repeat;
		background-attachment:fixed;
		background-size:100% 150%;
		background-color:rgb(51, 57, 63);
		background-position: 0% 50%;
		margin:0;
		padding:0;
		overflow-x:hidden;
	}
	.top{
		#background-color:rgb(51, 57, 63);
		opacity:90%;
		position:relative; 
		width:100%; 
		left:0; 
		top:0;
		z-index:1;}
	.button{
		
	}
	.button3{
		font-size:16px;
		background-color:rgb(31, 67, 143);
		color:white;border:none;
		padding:8px 12px; 
		position:absolute; 
		margin-left:160px;
		width:100px;
		border-radius:8px;
		top:570px;
		}
	.button3:hover{
		background-color:rgb(61, 97, 183);
		cursor:pointer;
	}
	.welcome{
		text-align:left;
		color:rgb(255, 255, 255);
		font-size:70px;
		position:absolute;
		top:360px;
		margin-left:160px;
	}
	.welcome2{
		text-align:left;
		color:rgb(255, 255, 255);
		font-size:20px;
		margin-top:15px;
	}
	.center1{
		display:flex;
		justify-content:center;
	}
	.page1{
		background-color:white;
		width:1520px;
		height:2500px;
		position:relative;
		margin-top:0;
		top:-4px;
	}
	.text1{
		color:rgb(41, 47, 53);
		font-size:40px;
		margin-left:640px;
		text-align:left;
		margin-right:80px;
		position:relative;
	}
	.text2{
		color:rgb(41, 47, 53);
		font-size:20px;
	}
	.img2{
		position:absolute;
		top:106px;
		left:60px;
		display:inline;
	}
	.bar1{
		position:relative;
		width: 720px;
		height: 2px;
		background:rgb(210,210,210);
		top:100px;
		margin-left:auto;
		margin-right:auto;
	}
	.text3{
		color:rgb(41, 47, 53);
		font-size:20px;
		position:relative;
		margin-right:780px;
		margin-left:240px;
		top: 300px;
		text-align:center;
	}
	.img3{
		position:absolute;
		display:inline;
		margin-left: 810px;
		top: 555px;
	}
	
	.bar2{
		position:relative;
		width: 720px;
		height: 2px;
		background:rgb(210,210,210);
		top:510px;
		margin-left:auto;
		margin-right:auto;
	}
	
	.text4{
		color:rgb(41, 47, 53);
		font-size:20px;
		position:relative;
		margin-right:780px;
		margin-left:240px;
		top: 700px;
		text-align:center;
	}
	.img4{
		position:absolute;
		display:inline;
		margin-left: 810px;
		top: 1079px;
	}
	
	.bar3{
		position:relative;
		width: 720px;
		height: 2px;
		background:rgb(210,210,210);
		top:915px;
		margin-left:auto;
		margin-right:auto;
	}
	
	.text5{
		color:rgb(41, 47, 53);
		font-size:20px;
		position:relative;
		margin-right:60px;
		margin-left:640px;
		top: 1100px;
		text-align:left;
	}
	.img5{
		position:absolute;
		display:inline;
		margin-left: 80px;
		top: 1580px;
	}
	
	.bar4{
		position:relative;
		width: 720px;
		height: 2px;
		background:rgb(210,210,210);
		top:1295px;
		margin-left:auto;
		margin-right:auto;
	}
	
	.text6{
		color:rgb(41, 47, 53);
		font-size:20px;
		position:relative;
		margin-right:840px;
		margin-left:310px;
		top: 1440px;
		text-align:center;
	}
	.img6{
		position:absolute;
		display:inline;
		margin-left: 720px;
		top: 2025px;
	}
	
</style>

<body>



<div class = "top"></div>
	
	<img src = "images/banner.png" width = 100% style = "margin-top:-40px"></img>
	
	<div class = "welcome">
		The Hydar Project
		<div class = "welcome2">
			Public chat, voice channels, direct messages,<br> screen sharing, bots, and anonymous support!
			<br>
			Everything is open source at our <a href = 'https://github.com/ryanm537/Hydar-XYZ' style = "color:rgb(120,130,220)">github repository</a>
		</div>
	</div>

	<div class = "button">
		<form method="get" action="MainMenu.jsp" >
			<input type="submit" name="submit" value = "Try it out!" class= "button3" autofocus></input>
		</form>
	</div>
<div class = "center1">
	<div class = "page1">
		<div class = "text1">
		<br><br>
			<b>What IS Hydar?</b>
		
			<div class = "text2">
				<br>
				Hydar is a web application which primarily offers chat services, which are centered around boards. Users can create <b>boards</b>, 
				which can be set to either public or private. Public boards can be joined by anyone who is given the board's 
				unique ID, whereas private boards (the default) are invite-only. As of now, there are certain 
				boards that will be displayed for every user. These boards are <b>Everything Else</b>, <b>Skyblock</b>, and <b>SAS4</b>.
				As one can guess, these are primarily focused on individual online games, but users can create boards for whatever topics they'd like!
				There are also direct-message channels, which are similar to boards in all ways except that they are private and between two people.
				All you need to send someone a direct-message is their unique user ID.
			</div>
		</div>
		<div class = "img2">
			<img src = "images/main-menu-image.png", width = 500px>
		</div>
		
		
		<div class = "bar1"></div>
		
		<div class = "text3">
				Every board also has a <b>voice channel</b>. This is a chat room in which users can communicate via their microphone. Users can 
				also <b>share their screen</b> while in the voice channel, or can choose to view other users' screens (assuming they are sharing theirs).
		</div>
		<div class = "img3">
			<img src = "images/voice-channel-image.png", width = 500px>
		</div>
		
		<div class = "bar2"></div>
		
		<div class = "text4">
				An example of <b>screen sharing</b>, where the user is watching another user's stream.
		</div>
		<div class = "img4">
			<img src = "images/screen-share-image.png", width = 500px>
		</div>
		
		<div class = "bar3"></div>
		
		<div class = "text5">
				Within boards, there are certain bot commands that can be used. These commands are focused on the games currently supported by this
				web app. There is also a chat bot that can commune with users in the official public boards, and can be invited to private boards or directly
				messaged. For more information on bots and other commands that can be used, simply type <b>/help</b> in a board.
		</div>
		<div class = "img5">
			<img src = "images/anonymous-and-bot-use.png", width = 500px>
		</div>
		
		<div class = "bar4"></div>
		<div class = "text6">
				And finally, this web app is completely <b>anonymous</b>! That means you don't need an account to access the site or use any of it's features.
				Pressing the button to create an <b>anonymous account</b> will allow you to use the site, and no personal information (including email) is required.
				It is also possible to access the site under a <b>guest</b> account, with reduced privledges.
				
		</div>
		<div class = "img6">
			<img src = "images/anonymous-creation.png", width = 500px>
		</div>
	</div>
</div>

<br>
</body>
</html>
