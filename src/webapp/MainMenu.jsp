<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file="Util.jsp" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Select Board - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>

<style type="text/css">
form{ display: inline-block; } 
</style>
<style>
		.centeredText {position:relative;top:290px;color:white; font-family:calibri; font-size:20px;-webkit-touch-callout: none;}
		.newMessagesText {
			top:105px;
			margin-top:-25px;
			position:relative;
			color:rgb(241, 128, 128); 
			font-family:calibri; 
			font-size:20px;
			-webkit-touch-callout: none;  
		}
		ul {padding:0px;padding-top: 10px;margin: 10px 20px; list-style: none;}
	    ul li {display: inline-block;vertical-align:middle; }
	    ul li a img {margin-top: 30px;width: 200px; height: 200px;display: inline-block; border-radius:20px;}
		ul li a:hover img {transform: scale(1.1);box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);}
		.column {display: inline-block;}
		.row::after {content: "";clear: both;display: block;} 
		.pfps{display:inline-block;}
		
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
			z-index:2;
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
			font-family:calibri; 
			font-size:20px;
			-webkit-touch-callout: none;
		}
		.centeredText3{
			position:relative;
			top:340px;
			color:white; 
			font-family:calibri; 
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
		
		.xbutton2{
			position:relative;
			top:80px;
			left:15px;
			width:30px;
			height:30px;
			display:block;
			background-color:rgb(155, 30, 24);
			border-radius:30px;
			color:white;
			z-index:1;
		}
		.xbutton2:hover {
		  background-color: rgb(185, 70, 54);
		  cursor:pointer;
		}
		
		.bottom_bar2{
			height:70px;
			width:640px;
			position:relative;
			z-index:2;
			top:30px;
			overflow-x:hidden;
		}
	</style>
<body>
<body style = "background-color:rgb(51, 57, 63);text-align:center;"> 
<div id="show">
</div>

<%
//System.out.println(System.currentTimeMillis());
Class.forName("com.mysql.jdbc.Driver");
//System.out.println(System.currentTimeMillis());
String CREATE=response.encodeURL("CreateBoard.jsp");
String JOIN=response.encodeURL("JoinBoard.jsp");
String PROFILE=response.encodeURL("Profile.jsp");
String LOGOUT=response.encodeURL("Logout.jsp");
String HOMEPAGE=response.encodeURL("Homepage.jsp");


Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	
	//check parameters if a board needs to be cleared
	int uid = (int)session.getAttribute("userid");
	String removeInvite = request.getParameter("cleared_invite");
	if(removeInvite!= null){
		
		new SQL(conn,"DELETE FROM invitedto WHERE invitedto.user = ? AND board = ?")
			.setInt(uid)
			.setInt(removeInvite)
			.update();
	}
	
	String removeDM = request.getParameter("cleared_dm");
	if(removeDM!= null){
		new SQL(conn,"DELETE FROM isin WHERE board = ?").setInt(removeDM).update();
	}
	
	
	// Get list of boards
	
	
	
	
	var result1 = new SQL(conn,"SELECT isin.board, board.name, board.image, board.dm FROM isin, board WHERE isin.user = ? AND board.number = isin.board AND board.channelof = -1 ORDER BY board.number")
			.setInt(uid).query();
	
	List<Integer> boardArray =new ArrayList<>();
	List<String> boardNames = new ArrayList<>();
	List<String> boardImages = new ArrayList<>();
	List<Integer> boardDm = new ArrayList<>();
	while(result1.next()){
		boardArray.add(result1.getInt("isin.board"));
		boardNames.add(result1.getString("board.name"));
		boardImages.add(result1.getString("board.image"));
		boardDm.add(result1.getInt("board.dm"));
	}
	//ban
	if(boardArray.isEmpty()){
		response.sendRedirect(LOGOUT);
		return;
	}
%>
	
	<div class = "fix-div">
	
		</div>
	
	<div id = "bottom_bar" class="bottom_bar">
		<input id = "Add" value="Create Board"  type="submit" class = "button" style='margin-top:5px; margin-left:20px' >
		
		<input id = "Remove" value="Join Board"  type="submit" class = "button" style='margin-top:5px; margin-left:10px' >
		
		<form method = "get" action = <%=CREATE %>>
			<input hidden='' id="ia1" type="text" name="input_create" size="30" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "New board name..."/>
			<input hidden='' id = "ia2" value="Create"  type="submit" class = "button3" >
		</form>
		<form method = "get" action = <%=JOIN %>>
			<input hidden='' id="ir1" type="text" name="input_join" size="30" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "Enter board ID (#)..."/>
			<input hidden='' id = "ir2" value="Join"  type="submit" class = "button3" >
		</form>	
			
	</div>
	
	
	
	<script>
		function adminButtons (){
			
			const x1 = document.getElementById('Add');
			x1.addEventListener("click", () => {
				document.getElementById("bottom_bar").style.width = "645px";
				document.getElementById("bottom_bar").style.top = "calc(100% - 55px)";
				document.getElementById("ia1").removeAttribute("hidden");
				document.getElementById("ia2").removeAttribute("hidden");
				document.getElementById("ir1").setAttribute("hidden", true);
				document.getElementById("ir2").setAttribute("hidden", true);
				}
			);
			
			const x2 = document.getElementById('Remove');
			x2.addEventListener("click", () => {
				document.getElementById("bottom_bar").style.width = "630px";
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
					out.print("<h1 style = \"color:rgb(255,255,255); font-size:15px; font-family:calibri; text-align:right;position:relative;\"></style>");
					out.print("Hello <div id=\"profileName\" style=\"display:inline\">" + session.getAttribute("username").toString() + "</div>! | ");
					out.print("<style type=\"text/css\"> a{color:LightGrey; font-family:calibri; text-align:right; font-size:15px}</style>");
					%><a href=<%=PROFILE %>> Profile</a>&nbsp;|<%
					%><a href=<%=LOGOUT %>> Log out</a> &nbsp;&nbsp;<%
					
					out.print("<style type=\"text/css\"> h1{color:rgb(255,255,255); text-align:left; font-size:15px}</style>");
					out.print("<img src=\"images/hydar.png\" alt=\"hydar\" width = \"25px\" height = \"40px\" align = \"center\">");
					out.print("&nbsp;&nbsp;&nbsp;Pick a board: ");
					%><form method="get" action=<%=HOMEPAGE %>></form><%
					out.print("<select name=\"board\">" );
					out.print("<option value = \"" + -1 + "\"> ---");
					
					// board selector
					
					for(int i=0;i<boardNames.size();i++){
						/**ResultSet boardsQuery = new SQL(conn,"SELECT board.name FROM board WHERE board.number = ? AND board.channelof = -1 ORDER BY board.number")
							.setInt(board).query();
						String b = "";
						while(boardsQuery.next()){
							b=boardsQuery.getString("board.name");
						}*/
						
						out.print("<option value = \""+ boardArray.get(i) +"\"> " + boardNames.get(i));
					}
					out.print("<input value=\"Go\"  type=\"submit\"></select></form>");
					
					out.print("</h1>");
			%>
	</div><div class = "margin">
	
	<%
		// LIST OF BOARDS
			String[] menuImages = {"menuImages/everythingelse.png", "menuImages/sas4.png", "menuImages/skyblock.png"};
			
			out.print("<style>p2{color:rgb(255,255,255); font-size:25px; font-family:calibri;display:block; text-align:left;position:absolute;margin-top:30px; margin-left: 20px; overflow-x:hidden;}</style>");
			out.print("<p2><b>Your Boards</b></p2>");
			
			out.print("<div class = \"pfps\"><div class = \"row\">");
			

			ResultSet result =new SQL(conn,"SELECT board.name, board.number, board.image FROM isin, board WHERE isin.user = ? AND isin.board = board.number AND board.channelof = -1  AND board.dm = 0 ORDER BY board.number")
					.setInt(uid).query();
			
			/**
			alternative(might be faster since ids are probably sorted better)
			
			SELECT board FROM isin 
			WHERE user = 1 AND lastVisited>0 AND 
			lastVisited+1000>(
				SELECT CREATED_DATE FROM post 
				WHERE board = isin.board 
				ORDER BY id DESC LIMIT 1
			);
			*/
			int imageCounter = 0;
			ResultSet lastModif = 
					/**new SQL(conn,"SELECT isin.board, isin.lastVisited FROM post, isin "+
					"WHERE isin.board = post.board AND isin.user=? "+
					"GROUP BY isin.board "+
					"HAVING isin.lastVisited > 0 AND isin.lastVisited + 1000 < MAX(post.CREATED_DATE)")*/
					new SQL(conn,"SELECT board FROM isin "+
							"WHERE user = ? AND lastVisited>0 AND "+
							"lastVisited<("+
								"SELECT CREATED_DATE-1000 FROM post "+
								"WHERE board = isin.board "+
								"ORDER BY id DESC LIMIT 1"+
							")")
					.setInt(uid)
					.query();
			Set<Integer> unread = new HashSet<>();
			
			while(lastModif.next()){
				unread.add(lastModif.getInt("isin.board"));
			}
			for(int i=0;i<boardNames.size();i++){
				if(boardDm.get(i)>0)
					continue;
				out.print("<div class = \"column\">");
				int board=boardArray.get(i);
				if(unread.contains(board)){
					out.print("<div id = \"newMessagesText"+imageCounter+"\"class = \"newMessagesText\"><b>New Messages</b></div>");
				}
				out.print("<div class = \"centeredText\"><b>"+ boardNames.get(i) + "</b></div>");
				String homepage=response.encodeURL("Homepage.jsp?board="+board);
				out.print("<ul><li><a href="+homepage+" /><img src=\"menuImages/" + boardImages.get(i) +"\" alt=\"hydar"+ imageCounter +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
				imageCounter++;
			}
			out.print("</div></div><br><br>");
			
			// LIST OF DIRECT MESSAGE CHANNELS
			
			out.print("<style>p3{color:rgb(255,255,255); font-size:25px; font-family:calibri;display:block; text-align:left;position:relative;top:40px; margin-left: 20px;overflow-x:hidden;}</style>");
			out.print("<style>p4{color:LightSlateGray; font-size:18px; font-family:calibri;display:block; text-align:center;position:relative;top:40px; margin-left: 20px;overflow-x:hidden;}</style>");
			out.print("<p3><br><b>Your Direct Message Boards</b></p3>");
		%>
	<div id = "bottom_bar2" class="bottom_bar2" style="margin-left:auto;margin-right:auto;">
		
		<form method = "get" action = <%=CREATE %>>
			<input  id="dm1" type="text" name="input_dm" size="50" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "User ID of person to chat with"/>
			<input  id = "dm2" value="Create DM Board"  type="submit" class = "button3" >
		</form>
		</div>
	</div>
	
	<%
	
			int x = 0;
			out.print("<div class = \"pfps\"><div class = \"row\">");
			for(int i=0;i<boardNames.size();i++){
				if(boardDm.get(i)==0)
					continue;
				x++;
				out.print("<div class = \"column\">");
				out.print("<div class = \"centeredText2\"><b>"+ boardNames.get(i) + "</b></div>");
				out.print("<ul><li>");
				String clearedDm=response.encodeURL("MainMenu.jsp?cleared_dm=" + boardArray.get(i));
				String homepage=response.encodeURL("Homepage.jsp?board="+boardArray.get(i));
				out.print("<a href="+clearedDm+"><input type = \"submit\" value = \"X\" class=\"xbutton\"></a></input>");
				out.print("<a href="+homepage+"><img src=\"menuImages/" + boardImages.get(i)+"\" alt=\"hydar"+ x +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
			}
			out.print("</div></div><br>");
			if(x == 0){
				out.print("<p4>No Direct Message Boards</p4>");
			}
			out.print("<br><br> &nbsp");
			
			
			// LIST OF INVIITES TO DIRECT MESSAGE CHANNELS
			
			
			// LIST OF INVITES TO BOARDS
			
			out.print("<style>p3{color:rgb(255,255,255); font-size:25px; font-family:calibri;display:block; text-align:left;position:relative;top:40px; margin-left: 20px;overflow-x:hidden;}</style>");
			out.print("<style>p4{color:LightSlateGray; font-size:18px; font-family:calibri;display:block; text-align:center;position:relative;top:40px; margin-left: 20px;overflow-x:hidden;}</style>");
			out.print("<p3><br><b>Your Invites</b> (Click to accept)</p3>");

			
			
			String checkInvites = "SELECT board.name, board.number, board.image FROM invitedto, board WHERE invitedto.user = ? AND board.number = invitedto.board";
			result = new SQL(conn,checkInvites).setInt(uid).query();
			
			
			
			x = 0;
			out.print("<div class = \"pfps\"><div class = \"row\">");
			while(result.next()){
				x++;
				out.print("<div class = \"column\">");
				out.print("<div class = \"centeredText2\"><b>"+ result.getString("board.name") + "</b></div>");
				out.print("<a href=\""+response.encodeURL("MainMenu.jsp?cleared_invite=" + result.getInt("board.number")) +"\">");
				String join="JoinBoard.jsp?input_join="+result.getString("board.number");
				out.print("<ul><li><input type = \"submit\" value = \"X\" class=\"xbutton\"></a><a href=\""+join+"\"><img src=\"menuImages/" + result.getString("board.image")+"\" alt=\"hydar"+ x +"\" width = \"50px\" height = \"50px\"></a></li> </ul> </div>");
			}
			out.print("</div></div><br>");
			if(x == 0){
				out.print("<p4>No pending invites</p4>"); 
			}
			out.print("<br><br> &nbsp");
			
			conn.close();
				} catch (Exception e) {
					out.print("<style> body{color:rgb(255,255,255); font-family:calibri; text-align:center; font-size:20px;}</style>");
					out.print("<center>");
					out.print("A known error has occurred.\n");
					out.print("<br><br>");
					out.print("<form method=\"post\" action=\""+response.encodeURL("Logout.jsp")+"\">");
					out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
					out.print("</form>");
					e.printStackTrace();
				}
		%>

</body>
</html>