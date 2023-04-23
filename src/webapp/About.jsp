<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="java.nio.CharBuffer"%>
<%@page import="java.util.stream.Stream"%>
<%@page import="xyz.hydar.HydarUtil"%>
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
		font-family:calibri;
		background-image:url('images/hydarface.png');
		background-repeat:no-repeat;
		background-attachment:fixed;
		background-size:100% 150%;
		background-color:rgb(51, 57, 63);
		background-position: 0% 50%;
		margin:0;
		padding:0;
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
		float:left;
		margin-left:10%;
		
	}
	.button3{
		font-size:16px;
		background-color:rgb(31, 67, 143);
		color:white;border:none;
		padding:8px 12px; 
		position:relative; 
		left:0px;
		width:100px;
		border-radius:8px;
		top:-320px;
		}
	.button3:hover{
		background-color:rgb(61, 97, 183);
		cursor:pointer;
	}
	.welcome{
		text-align:left;
		color:rgb(255, 255, 255);
		font-size:70px;
		position:relative;
		top:-360px;
		left:10%;
	}
	.welcome2{
		text-align:left;
		color:rgb(255, 255, 255);
		font-size:20px;
		margin-top:15px;
	}
	.page{
		background-color:white;
		margin-left:auto;
		margin-right:auto;
		width:1080px;
		height:900px;
		position:relative;
		margin-top:0;
		top:-154px;
	}
	.text1{
		color:rgb(41, 47, 53)
		margin-left:auto;
		margin-right:auto;
		position:relative;
		font-size:40px;
		text-align:left;
		left:180px;
	}
	.text2{
		color:rgb(41, 47, 53)
		margin-left:100px;
		margin-right:400px;
		position:relative;
		font-size:20px;
		text-align:left;
		left:180px;
	}
</style>

<body>



<div class = "top"></div>
	
	<img src = "images/banner.png" width = 100% style = "margin-top:-40px">
	
	<div class = "welcome">
		The Hydar Project
		<div class = "welcome2">
			Public chat, voice channels, direct messages,<br> screen sharing, bots, and anonymous support!
		</div>
	</div>

	<div class = "button">
		<form method="get" action="Login.jsp" >
			<input type="submit" name="submit" value = "Try it out!" class= "button3" autofocus></input>
		</form>
	</div>

<div class = "page">
	<div class = "text1">
	<br><br>
		<b>What IS Hydar?</b>
	</div>
	<div class = "text2">
		<br>
		Hydar is a web application which primarily offers chat services, which are centered around boards. Users can create <b>boards</b>, 
		which can be set to either public or private. Public boards can be joined by anyone who is given the board's 
		unique ID, whereas private boards (the default) are invite-only. As of now, there are certain 
		boards that will be displayed for every user. These boards are <b>Everything Else</b>, <b>Skyblock</b>, and <b>SAS4</b>.
		As one can guess, these are primarily focused on individual online games, but users can create boards for whatever topics they'd like!
		There are also direct-message channels, which are similar to boards in all ways except that they are private and between two people.
		All you need to send someone a direct-message is their unique user ID.
		<br><br>
		Every board also has a <b>voice channel</b>. This is a chat room in which users can communicate via their microphone. Users can 
		also <b>share their screen</b> while in the voice channel, or can choose to view other users' screens (assuming they are sharing theirs).
		<br><br>
		Within boards, there are certain bot commands that can be used. These commands are focused on the games currently supported by this
		web app. There is also a chat bot that can commune with users in the official public boards, and can be invited to private boards or directly
		meessaged. For more information on bots and other commands that can be used, simply type <b>/help</b> in a board.
		<br><br>
		And finally, this web app is completely <b>anonymous</b>! That means you don't need an account to access the site or use any of it's features.
		<br><br>
		<b>This site does use cookies.</b>
	</div>
</div>

<br>
</body>
</html>