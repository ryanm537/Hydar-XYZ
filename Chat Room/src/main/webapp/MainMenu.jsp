<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Select Board - Hydar</title>
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
	Statement stmt1 = conn.createStatement();
	
	//check parameters if a board needs to be cleared
	String removeInvite = request.getParameter("cleared_invite");
	if(removeInvite!= null){
		
		removeInvite = removeInvite.replace("\"", "");
		String remQuery = "DELETE FROM invitedto WHERE invitedto.user = " + session.getAttribute("userid").toString() + " AND board = " + removeInvite;
		int removeTheInvite = stmt1.executeUpdate(remQuery);
	}
	
	// Get list of boards
	
	
	String countBoards = "SELECT COUNT(isin.board) AS numBoards FROM isin WHERE isin.user = " + session.getAttribute("userid").toString();
	ResultSet result1 = stmt1.executeQuery(countBoards);
	int numBoards = 0;
	while(result1.next()){
		numBoards = Integer.parseInt(result1.getString("numBoards"));
	}

	String checkBoardsStr="SELECT isin.board, board.name FROM isin, board WHERE isin.user = " + session.getAttribute("userid").toString() + " AND board.number = isin.board ORDER BY board.number";
	result1 = stmt1.executeQuery(checkBoardsStr);
	
	int[] boardArray = new int[numBoards];
	String[] boardNames = new String[numBoards];
	int n = 0;
	while(result1.next()){
		boardArray[n] = Integer.parseInt(result1.getString("isin.board"));
		boardNames[n] = result1.getString("board.name");
		n++;
	}
	
	
	
	%>
	<style>
		.centeredText {position:relative;top:290px;color:white; font-family:arial; font-size:20px;-webkit-touch-callout: none;}
		ul {padding:0px;padding-top: 10px;margin: 10px 20px; list-style: none;}
	    ul li {display: inline-block;vertical-align:middle; }
	    ul li a img {margin-top: 30px;width: 200px; height: 200px;display: inline-block; border-radius:20px;}
		ul li a:hover img {transform: scale(1.1);box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);}
		.column {float: left;}
		.row::after {content: "";clear: both;display: block;} 
		.pfps{display:inline-block;margin-left:auto;margin-right:auto;}
		
		.fix-div{
			background-color:rgb(41, 47, 53);
			opacity:90%;
			position:fixed; 
			width:100%; 
			left:0; 
			top:0;
			box-shadow:0 0 10px rgba(0,0,0,20);
			z-index:1;}
		.margin{
			margin-top:50px;
		}
		.button{
			background-color:rgb(61, 67, 83);
			color:white;border:none;
			padding:8px; 
			margin-top:5px;
			position:relative; 
			left:3px;
			border-radius:8px;
		}
		.button:hover{
			background-color:rgb(61, 97, 183);
			cursor:pointer;
		}
		.bottom_bar{
			height:100%;
			width:240px;
			position:fixed;
			z-index:1;
			left:-20px;
			top:calc(100% - 45px);
			background-color: rgb(41, 47, 53);
			overflow-x:hidden;
			box-shadow:0 0 10px rgba(0,0,0,10);
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
		.centeredText2{
			position:relative;
			top:320px;
			color:white; 
			font-family:arial; 
			font-size:20px;
			-webkit-touch-callout: none;
		}
		p.xbutton{
			position:relative;
			top:-3px;
			left:-3px;
			font-family:calibri;
			display:block;
			font-size:20px;
			z-index:1;	
			border:3px solid white;
			-webkit-touch-callout: none;
		    -webkit-user-select: none;
		    -khtml-user-select: none;
		    -moz-user-select: none;
		    -ms-user-select: none;
		    user-select: none;
		}
		a.xbutton{
		
		}
		.xbutton{
			position:relative;
			top:60px;
			left:-5px;
			width:30px;
			height:30px;
			display:block;
			background-color:rgb(155, 30, 24);
			border-radius:30px;
			color:white;
			z-index:1;
		}
		.xbutton:hover {
		  background-color: rgb(185, 70, 54);
		  cursor:pointer;
		}
		
	</style>
	<div class = "fix-div">
	
		</div>
	
	<div id = "bottom_bar" class="bottom_bar">
		<input id = "Add" value="Create Board"  type="submit" class = "button" style='margin-top:5px; margin-left:20px' >
		
		<input id = "Remove" value="Join Board"  type="submit" class = "button" style='margin-top:5px; margin-left:10px' >
		
		<%out.print("<form method = \"get\" action = \"CreateBoard.jsp\">");%>
			<input hidden id="ia1" type="text" name="input_create" size="30" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "New board name..."/>
			<input hidden id = "ia2" value="Create"  type="submit" class = "button3" >
		</form>
		
		<%out.print("<form method = \"get\" action = \"JoinBoard.jsp\">");%>
			<input hidden id="ir1" type="text" name="input_join" size="30" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "Enter board ID (#)..."/>
			<input hidden id = "ir2" value="Join"  type="submit" class = "button3" >
		</form>	
			
	</div>
	
	<script>
		function adminButtons (){
			
			const x1 = document.getElementById('Add');
			x1.addEventListener("click", () => {
				document.getElementById("bottom_bar").style.width = "545px";
				document.getElementById("bottom_bar").style.top = "calc(100% - 55px)";
				document.getElementById("ia1").removeAttribute("hidden");
				document.getElementById("ia2").removeAttribute("hidden");
				document.getElementById("ir1").setAttribute("hidden", true);
				document.getElementById("ir2").setAttribute("hidden", true);
				}
			);
			
			const x2 = document.getElementById('Remove');
			x2.addEventListener("click", () => {
				document.getElementById("bottom_bar").style.width = "530px";
				document.getElementById("bottom_bar").style.top = "calc(100% - 55px)";
				document.getElementById("ir1").removeAttribute("hidden");
				document.getElementById("ir2").removeAttribute("hidden");
				document.getElementById("ia1").setAttribute("hidden", true);
				document.getElementById("ia2").setAttribute("hidden", true);
				}
			);
			
			window.addEventListener('click', function(e){
				if(document.getElementById('bottom_bar').contains(e.target)){
					
				}else{
					document.getElementById("bottom_bar").style.top = "calc(100% - 45px)";
					document.getElementById("bottom_bar").style.width = "240px";
					document.getElementById("ir1").setAttribute("hidden", true);
					document.getElementById("ir2").setAttribute("hidden", true);
					document.getElementById("ia1").setAttribute("hidden", true);
					document.getElementById("ia2").setAttribute("hidden", true);
				}
			});
			
		}
		
		
		adminButtons();
	</script>
	
	<div class = "fix-div">
	
	
	<%
	
	//TOP BAR
	
	out.print("<h1 style = \"color:rgb(255,255,255); font-size:15px; font-family:arial; text-align:right;position:relative;\"></style>");
	out.print("Hello <div id=\"profileName\" style=\"display:inline\">" + session.getAttribute("username").toString() + "</div>! | ");
	out.print("<style type=\"text/css\"> a{color:LightGrey; font-family:arial; text-align:right; font-size:15px}</style>");
	out.print("<a href=\"Profile.jsp\"> Profile</a>&nbsp;| ");
	out.print("<a href=\"Logout.jsp\"> Log out</a> &nbsp;&nbsp;");
	
	out.print("<style type=\"text/css\"> h1{color:rgb(255,255,255); text-align:left; font-size:15px}</style>");
	out.print("<img src=\"hydar.png\" alt=\"hydar\" width = \"25px\" height = \"40px\" align = \"center\">");
	out.print("&nbsp;&nbsp;&nbsp;Pick a board: ");
	out.print("<form method=\"get\" action=\"Homepage.jsp\">");
	out.print("<select name=\"board\">" );
	out.print("<option value = \"" + -1 + "\"> ---");
	
	// board selector
	
	for(int i = 0; i < boardArray.length; i++){
		String checkBoards = "SELECT board.name FROM board WHERE board.number = "+boardArray[i] + " ORDER BY board.number";	
		ResultSet boardsQuery = stmt1.executeQuery(checkBoards);
		String b = "";
		while(boardsQuery.next()){
			b=boardsQuery.getString("board.name");
		}
		out.print("<option value = \""+ boardArray[i] +"\"> " + b);
	}
	out.print("<input value=\"Go\"  type=\"submit\"></select></form>");
	
	out.print("</h1>");
	%>
	</div><div class = "margin">
	
	<%
	
	// LIST OF BOARDS
	String[] menuImages = {"menuImages/everythingElse.png", "menuImages/sas4.png", "menuImages/skyblock.png"};
	
	out.print("<style>p2{color:rgb(255,255,255); font-size:25px; font-family:arial;display:block; text-align:left;position:absolute;margin-top:30px; margin-left: 20px; overflow-x:hidden;}</style>");
	out.print("<p2><b>Your Boards</b></p2>");
	
	out.print("<div class = \"pfps\"><div class = \"row\">");
	

	String checkBoards = "SELECT board.name, board.number FROM isin, board WHERE isin.user = " + session.getAttribute("userid").toString() + " AND isin.board = board.number ORDER BY board.number";
	ResultSet result = stmt1.executeQuery(checkBoards);
	
	int imageCounter = 0;
	while(result.next()){
		out.print("<div class = \"column\">");
		out.print("<div class = \"centeredText\"><b>"+ result.getString("board.name") + "</b></div>");
		if(imageCounter<menuImages.length){
			out.print("<ul><li><a href=\"Homepage.jsp?board="+result.getString("board.number")+"\"><img src=\"" + menuImages[imageCounter] +"\" alt=\"hydar"+ imageCounter +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
		}else {
			out.print("<ul><li><a href=\"Homepage.jsp?board="+result.getString("board.number")+"\"><img src=\"menuImages/misc.png\" alt=\"hydar"+ imageCounter +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
		}
		imageCounter++;
	}
	out.print("</div></div><br><br>");

	// LIST OF INVITES
	
	out.print("<style>p3{color:rgb(255,255,255); font-size:25px; font-family:arial;display:block; text-align:left;position:relative;top:40px; margin-left: 20px;overflow-x:hidden;}</style>");
	out.print("<style>p4{color:LightSlateGray; font-size:18px; font-family:arial;display:block; text-align:center;position:relative;top:40px; margin-left: 20px;overflow-x:hidden;}</style>");
	out.print("<p3><br><b>Your Invites</b> (Click to accept)</p3>");

	
	
	String checkInvites = "SELECT board.name, board.number FROM invitedto, board WHERE invitedto.user = \"" + session.getAttribute("userid").toString()+"\" AND board.number = invitedto.board";
	result = stmt1.executeQuery(checkInvites);
	
	
	
	int x = 0;
	out.print("<div class = \"pfps\"><div class = \"row\">");
	while(result.next()){
		x++;
		out.print("<div class = \"column\">");
		out.print("<div class = \"centeredText2\"><b>"+ result.getString("board.name") + "</b></div>");
		out.print("<a href=\"MainMenu.jsp?cleared_invite=" + result.getInt("board.number") +"\">");
		out.print("<ul><li><input type = \"submit\" value = \"X\" class=\"xbutton\"></a><a href=\"JoinBoard.jsp?input_join="+result.getString("board.number")+"\"><img src=\"menuImages/misc.png\" alt=\"hydar"+ x +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
	}
	out.print("</div></div><br>");
	if(x == 0){
		out.print("<p4>No pending invites</p4>");
	}
	out.print("<br><br> &nbsp");
	
	conn.close();
} catch (Exception e) {
	out.print("<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style>");
	out.print("<center>");
	out.print("A known error has occurred.\n");
	out.print("<br><br>");
	out.print("<form method=\"get\" action=\"Logout.jsp\">");
	out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
	out.print("</form>");
	e.printStackTrace();
}
%>

</body>
</html>
