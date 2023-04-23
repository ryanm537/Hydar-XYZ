<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.nio.file.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%if(session.getAttribute("userid")!=null){
	response.sendRedirect(response.encodeURL("MainMenu.jsp"));
	return;
}%><!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Login - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head> 
<style> 
	body{
		background-image:url('images/hydarface.png');
		background-repeat:no-repeat;
		background-attachment:fixed;
		background-size:100% 150%;
		background-color:rgb(51, 57, 63);
		background-position: 0% 50%;
	}
</style>
<style>
	.images{
		height:140%;
		width:calc(100% + 20px);
		position:absolute;
		overflow:hidden;
		top:-40%;
		left:-20px;
		opacity:40%;
	}
	.textbox{
		position: absolute;
		top:50%;
		left:50%;
	}
	.textboxmove{
		background:rgb(51, 57, 63);
		width:470px;
		height:420px;
		display:block;
		position: absolute;
		top:-210px;
		left:-235px;
		box-shadow:0 0 10px rgba(0,0,0,20);
	}
	.hydarlogo{
		position:absolute;
		top:calc(50% - 160px);
		left:calc(50% - 220px);
		opacity:100%;
	}
	.button3{
			dsiplay:inline-block;
			background-color:rgb(41, 47, 53);
			color:white;border:none;
			padding:8px 12px; 
			position:relative; 
			left:0px;
			top:4px;
			border-radius:8px;
		}
	.button3:hover{
		background-color:rgb(61, 97, 183);
		cursor:pointer;
	}
	input:-webkit-autofill {
    -webkit-box-shadow: 0 0 0 50px rgb(71, 77, 83) inset;
    -webkit-text-fill-color: White;
	}
	
	input:-webkit-autofill:focus {
	    -webkit-box-shadow: 0 0 0 50px rgb(71, 77, 83) inset;
	    -webkit-text-fill-color: White;
	} 
</style>
<body>
<div class = "textbox"><div class = "textboxmove"></div></div>
<p style = "color:rgb(255,255,255);">

<div class = "hydarlogo">
<img src="images/hydar.png" alt="hydar" >
</div>

<%
//Files.writeString(Path.of(request.getServletContext().getResource("/log.txt").toURI()),"juydar");
out.print("<p style = \"color:rgb(255,255,255); font-family:calibri; font-size:20px; z-index:1; position:absolute; text-align:left; left:50%; display:block; top:calc(50% - 180px);\">");
%>
<br>
<b>
Welcome to Hydar</b><br><br>
Returning users login 
<br>
<form method="get" action="Verify.jsp"  >
<p style = "color:rgb(255,255,255); font-family:calibri; z-index:1; position:fixed; position:absolute; text-align:left; left:50%; display:block; top:calc(50% - 70px);">

<input  type="text" name="username" size = "20px" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;" placeholder = "Username" autofocus><br>
 

<input type="password" name="password" size = "20px" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px; position:relative;top:4px;" placeholder = "Password">
<input type="submit" name="submit" value = "Go" class= "button3"><br><br>
</form>
<p style = "color:rgb(255,255,255); font-family:calibri;  z-index:1; position:fixed; position:absolute; text-align:left; left:50%; display:block; top:calc(50% + 120px);">
Don't have an account? rip!<br><a href = '#' onclick='document.location.replace("<%=response.encodeURL("VerifyAnonymous.jsp")%>");' style ="color:rgb(170,220,255);"> Continue as Anonymous</a>
</p>
</body>
</html>