<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Profile - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<script type="text/javascript" src ="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>

<body>
<body style = "background-color:rgb(51, 57, 63);"> 
<center>
<style type="text/css">
form{ display: inline-block; }
</style>

<div id="show">
</div>



<%

Class.forName("com.mysql.jdbc.Driver").newInstance();
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");

try{
	
	// MARGIN SETUP
	%>
	<style>
		.fix-div{
			background-color:rgb(41, 47, 53);
			opacity:90%;
			position:fixed; 
			width:100%; 
			left:0; 
			top:0;
			box-shadow: 0 0 10px rgba(0,0,0,20);
			z-index:1;}
		
		.margin{
			margin-top:50px;
		}
	</style>
	<div class = "fix-div">
	
	<%
	
	//TOP BAR
	
	out.print("<h1 style = \"color:rgb(255,255,255); font-size:15px; font-family:arial; text-align:right;position:relative;\"></style>");
	out.print("Hello <div id=\"profileName\" style=\"display:inline\">" + session.getAttribute("username").toString() + "</div>! | ");
	out.print("<style type=\"text/css\"> a{color:LightGrey; font-family:arial; text-align:right; font-size:15px; display:inline-block;padding-top:15px;}</style>");
	out.print("<a href=\"Homepage.jsp\"> Home</a>&nbsp;| ");
	out.print("<a href=\"Logout.jsp\"> Log out</a> &nbsp;&nbsp;");
	
	out.print("<style type=\"text/css\"> h1{color:rgb(255,255,255); text-align:left; margin-right:10px; font-size:15px;}</style>");
	out.print("<img src=\"hydar.png\" alt=\"hydar\" width = \"25px\" height = \"40px\" align = \"center\">");
	
	out.print("</h1></div><div class = \"margin\">");
	
	// DISPLAY USERNAME
	
	out.print("<style> p4 .test{} .test:hover{transform:scale(1.3);} </style>");
	
	out.print("<br><style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:30px;position:relative; top:20px}</style>");
	
	out.print("<p4 class = \"test\"><b>" + session.getAttribute("username").toString() +"</b>&nbsp&nbsp(#" + session.getAttribute("userid").toString()+ ")</p4>");
	
	%>

	
	<div id='bar' style='width: 720px; height: 35px; border-bottom: 2px solid LightSlateGray; text-align: center;'></div>
	<%
	
	// GET USER PFP
	
	Statement stmt = conn.createStatement();
	String checkPostsStr="SELECT user.pfp, user.username FROM user WHERE user.username = \"" + session.getAttribute("username").toString()+"\"";
	ResultSet result = stmt.executeQuery(checkPostsStr);
	
	// GREY TEXT
	
	out.print("<style> p3{color:White; font-family:arial; text-align:center; font-size:25px;}</style>");
	out.print("<p3><br> Profile Picture: </p3>");
	
	out.print("<style> p{color:LightSlateGrey; font-family:arial; text-align:center; font-size:15px;}</style>");
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
		</style>
	<%
	
	// PROFILE PIC
	String pfp = "";
	while(result.next()){
		pfp = result.getString("user.pfp");
	}
	String[] pfps = {"yeti.png", "hydar2.png", "emp.png", "gw.png", "grim.png"};
	
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
			out.print("<ul><li><a href=\"ChangePfp.jsp?new_pfp="+ pfps[i]+"\"><img src=\"" + pfps[i] +"\" alt=\"hydar"+ i +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
		}else{
			out.print("<ul><li2><a href=\"#\"><img src=\"" + pfps[i] +"\" alt=\"hydar"+ i +"\" width = \"100px\" height = \"100px\"></a></li2> </ul> </div>");
		}
	}
	out.print("</div></div><br>");
	
	%>
	<div id='bar' style='width: 720px; height: 20px; border-bottom: 2px solid LightSlateGray; text-align: center;'></div>
	<%
	
	// CREATE / JOIN BOARD BUTTONS
	
	%>
	<style>
		.button{
			display:inline-block;
			background-color:rgb(41, 47, 53);
			color:white;border:none;
			padding:20px 20px; 
			position:relative; 
			top:35px; 
			left:-20px; 
			border-radius:8px;
		}
		.button:hover{
			background-color:rgb(61, 97, 183);
			cursor:pointer;
		}
		.button2{
			background-color:rgb(41, 47, 53);
			color:white;border:none;
			padding:20px 20px; 
			position:relative; 
			top:35px; 
			left:20px; 
			border-radius:8px;
		}
		.button2:hover{
			background-color:rgb(61, 97, 183);
			cursor:pointer;
		}
	</style>
	
	<input id = "createPrivate" value="Create Private Board"  type="submit" class="button"></input>
	<input id = "joinPrivate" value="Join Private Board"  type="submit" class="button2"></input>

		<style>
			.nav{text-align:center; font-family:arial; list-style-type:none; margin:0; padding:0;} 
			.nav li{color:rgb(255,255,255); display:block; font-size:20px; padding:5px; position:relative; top:45px;}
			.button3{
				dsiplay:inline-block;
				background-color:rgb(41, 47, 53);
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
		<ul hidden id = "createBar" class = "nav" >
			<li>
				<br>
				<%out.print("<form method = \"get\" action = \"CreateBoard.jsp\">");%>
						<input id="input_create" type="text" name="input_create" size="80" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;" placeholder = "New board name..."/>
						<input value="  Go  "  type="submit" class = "button3" ></input>
				</form>
			</li>
		</ul>
		
		<ul hidden id = "joinBar" class = "nav" >
			<li>
				<br>
				<form method = "get" action = "Homepage.jsp">
						<input id="input_join" type="text" name="input_join" size="80" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;" placeholder = "Enter Board ID (#)..."/>
						<input value="  Go  "  type="submit" class = "button3" ></input>
				</form>
			</li>
		</ul>
	
	<script>
		function buttons(){
			const createPrivate = document.getElementById('createPrivate');
			createPrivate.addEventListener("click", () => {
				document.getElementById("createBar").removeAttribute("hidden");
				document.getElementById("joinBar").setAttribute("hidden", true);
				}
			);
			
			const joinPrivate = document.getElementById('joinPrivate');
			joinPrivate.addEventListener("click", () => {
				document.getElementById("joinBar").removeAttribute("hidden");
				document.getElementById("createBar").setAttribute("hidden", true);
				}
			);
		}
		buttons();
	</script>
	
	
	<div id='bar' style='width: 720px; height: 65px; border-bottom: 2px solid LightSlateGray; text-align: center;'></div>
	
	<%
	
	out.print("<style> p3{color:White; font-family:arial; text-align:center; font-size:25px;}</style>");
	
	out.print("<p3><br> Your Boards: </p3>");
	
	// DISPLAY BOARDS
	
	%>
		<style>
			P.blocktext {
				font-family:arial;
				font-size:18px;
				color: White;
			    margin-left: auto;
			    margin-right: auto;
			    width:700px;
			    text-align: left;
			}
		</style> 
		<br>
	<%
	String checkBoardsStr = "SELECT board.name FROM isin, board WHERE isin.user = \"" + session.getAttribute("userid").toString()+"\" AND board.number = isin.board";
	result = stmt.executeQuery(checkBoardsStr);
	
	out.print("<P class = \"blocktext\">");
	while(result.next()){
		out.print(result.getString("board.name") + "<br>");
	}
	out.print("</P>");
	
	%>
	<div id='bar' style='width: 720px; height: 20px; border-bottom: 2px solid LightSlateGray; text-align: center;'></div><br>
	<%
	
	
	// DISPLAY INVITES
	
	
	out.print("<style> p3{color:White; font-family:arial; text-align:center; font-size:25px;}</style>");
	
	out.print("<p3> Pending Invites:<br></p3><P class = \"blocktext\">");
	
	String checkInvites = "SELECT board.name FROM invitedto, board WHERE invitedto.user = \"" + session.getAttribute("userid").toString()+"\" AND board.number = invitedto.board";
	result = stmt.executeQuery(checkInvites);
	
	int x = 0;
	while(result.next()){
		x++;
		out.print(result.getString("board.name") + "<br>");
	}
	if(x == 0){
		out.print("No pending invites");
	}
	out.print("</P>");
	
	%>
	<div id='bar' style='width: 720px; height: 20px; border-bottom: 2px solid LightSlateGray; text-align: center;'></div><br>
	</div>
	<%
	
}catch (Exception e){
	out.print("<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style>");
	out.print("A known error has occurred\n");
	out.print("<br><br>");
	out.print("<form method=\"get\" action=\"Logout.jsp\">");
	out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
	out.print("</form>");
	e.printStackTrace();
}


%>

</body>
</html>
