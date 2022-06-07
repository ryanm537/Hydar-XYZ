
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Home - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head>
<script type="text/javascript" src ="jquery-3.6.0.min.js"></script>

<body>
<body style = "background-color:rgb(51, 57, 63);"> 
<center>
<style type="text/css">
html {
  scroll-behavior: smooth;
}
form{ display: inline-block; }
</style>
<div id="show">
</div>



<%
Class.forName("com.mysql.jdbc.Driver");
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");
try{
	//CHECK IF BOARD IS SPECIFIED, and redirect if the user does not have perms.
	

	
	String getBoard = request.getParameter("board");
	int board = 1;
	if(getBoard != null){
		board = Integer.parseInt(getBoard);
	}
	
	Statement stmt1 = conn.createStatement();
	
	
	
	String countBoards = "SELECT COUNT(isin.board) AS numBoards FROM isin WHERE isin.user = " + session.getAttribute("userid").toString();
	ResultSet result1 = stmt1.executeQuery(countBoards);
	int numBoards = 0;
	while(result1.next()){
		numBoards = Integer.parseInt(result1.getString("numBoards"));
	}

	String checkBoardsStr="SELECT * FROM isin WHERE isin.user = " + session.getAttribute("userid").toString() + "";
	result1 = stmt1.executeQuery(checkBoardsStr);
	
	int[] boardArray = new int[numBoards];
	int n = 0;
	int check = 0;
	while(result1.next()){
		boardArray[n] = Integer.parseInt(result1.getString("isin.board"));
		n++;
		if(board == result1.getInt("isin.board")){
			check += 1;
		}
	}
	
	if(check == 0){
		%><meta http-equiv="refresh" content="0; url='MainMenu.jsp'" /><%
		board = 1;
	}
	
	if(numBoards == 0){
		board = 1;
	}
	
	//GET BOARD IMAGE
	
	String getBoardAttributes="SELECT board.image, board.public FROM board WHERE board.number = " + board;
	result1 = stmt1.executeQuery(getBoardAttributes);
	
	int isPublic = 0;
	String boardImage = "";
	while(result1.next()){
		isPublic = result1.getInt("board.public");
		boardImage = result1.getString("board.image");
	}
	
	// CHECK IF PINGS ARE ON, also get volume
	String getPings="SELECT user.pings, user.pingvolume, user.vcvolume, user.volume FROM user WHERE user.id = " + session.getAttribute("userid").toString();
	result1 = stmt1.executeQuery(getPings);
	
	double volume = 0.0;
	double voicevolume = 0.0;
	double pingvolume = 0.0;
	int pings = 0;
	while(result1.next()){
		pings = result1.getInt("user.pings");
		volume = result1.getInt("user.volume");
		voicevolume = result1.getInt("user.vcvolume");
		pingvolume = result1.getInt("user.pingvolume");
	}

	volume = volume/100;
	pingvolume = (pingvolume/100) * 2;
	voicevolume = (voicevolume/100) * 2;
	//CHECK IF AUTO REFRESH IS ON
	
	String autoRefresh = request.getParameter("autoOn");
	if(autoRefresh == null){
		autoRefresh = "autoOn";
	}
	
	//CHECK IF USER IS LOGGED IN
	
	if(session.getAttribute("username").toString().equals("null")){
		throw new Exception();
	}
	
	out.print("</center>");
	
	
	%>
	<style>
		.fix-div{
			background-color:rgb(41, 47, 53);
			opacity:90%;
			position:fixed; 
			width:100%; 
			left:0; 
			top:0;
			box-shadow:0 0 10px rgba(0,0,0,20);
			z-index:1;}
		
		.sidebar{
			height:calc(100% - 170px);
			margin-bottom:450px;
			width:210px;
			position:fixed;
			z-index:1;
			left:-10px;
			top:200px;
			background-color: rgb(41, 47, 53);
			overflow-x:hidden;
			box-shadow:0 0 10px rgba(0,0,0,10);
		}
		
		.sidebar p{top:-8px;line-height:20px;font-size:18px;color:white;}
		.sidebar p6{display:block;margin-left:20px;font-size:15px;color:White;text-align:left;}
		.sidebar p7{margin-top:35px;display:block;color:White;margin-left:20px;font-size:15px;text-align:left;}
		
		.bottom_bar{
			height:100%;
			width:210px;
			position:fixed;
			z-index:1;
			left:-10px;
			top:calc(100% - 45px);
			background-color: rgb(41, 47, 53);
			overflow-x:hidden;
			box-shadow:0 0 10px rgba(0,0,0,10);
		}
		
		.margin{margin-top:130px;margin-left:210px;}
		
		.button{
			background-color:rgb(61, 67, 83);
			color:white;border:none;
			padding:8px; 
			position:relative; 
			left:3px;
			border-radius:8px;
		}
		.button:hover{
			background-color:rgb(61, 97, 183);
			cursor:pointer;
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
	
	<div id = "sidebar" class = "sidebar">
		
	<p>
	<%
		
		// SIDE BAR
		
		//get board name
		String checkBoards = "SELECT board.name FROM board WHERE board.number = "+board;	
		ResultSet boardsQuery = stmt1.executeQuery(checkBoards);
		String b = "";
		while(boardsQuery.next()){
			b=boardsQuery.getString("board.name");
		}
	
		
		if(board<=3){
			out.print(b + " (#" +board+ ")" + " </p><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); display:block; text-align: center; position:relative; top:-15px;'></div>");
		}else{
			out.print(b + " (#" +board+ ")" + " </p><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:-15px;'></div><p6>");
			
			//find the creator of this board
			String findOwner = "SELECT board.creator, user.id, user.username FROM board, user WHERE board.number = " +board + " AND user.id = board.creator";
			ResultSet result = stmt1.executeQuery(findOwner);
			String creator = "";
			int creatorID = -1;
			while(result.next()){
				creator = result.getString("user.username");
				creatorID = result.getInt("user.id");
			}
			
			
			out.print("<b>Owner:</b><br> &nbsp"+creator + "</p6><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:20px;'></div>");
			
			//find boards where this user is a member (not creator)
			String findMembers = "SELECT user.username, user.id FROM user, isin WHERE isin.user = user.id AND isin.board = " + board;
			result = stmt1.executeQuery(findMembers);
			String member = "";
			
			out.print("<p7><b>Members:</b><br>");
			
			while(result.next()){
				if(result.getInt("user.id")!=creatorID){
					member = result.getString("user.username");
					if(session.getAttribute("userid").toString().equals(""+creatorID)){
						out.print("&nbsp"+member + " #" + result.getInt("user.id") + "<br>");
					}else{
						out.print("&nbsp"+member + "<br>");
					}
				}
			}
			out.print("</p7><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:15px;'></div>");
		}
		
		// hdaryhdaryhdayrhdyahryda
		
		// VCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC HYDAR
		
		// DHYARHYDHYARDHYADRYRR
		// ye, it is true that hydar
		%>
		<style>
			.vcName{
				font-size:15px;
				color:white;
				position:relative;
				top:25px;
				text-align:center;
			}
			.vcMemberText{
				font-size:15px;
				color:white;
				position:relative;
				top:40px;
				left:20px;
				text-align:left;
			}
			.vcMembers{
				position:relative;
				left:5px;
			}
			.vcSwitches{
				border-radius:5px;
				opacity:1;
				position:relative;
				align:center;
				left:15px;
				top:35px;
				padding:10px;
			}
			.vcSwitches:hover{
				opacity:1;
				box-shadow:0 0 10px rgba(255,255,255,20);
				cursor:pointer;
			}
		</style>
		<div class = "vcName"><b>Voice Channel</b></div>
		<img id = "VC-disconnect" class = "vcSwitches" src = "images/VC-disconnect.png" width = 40px height=40px>
		<img hidden id = "VC-connect" class = "vcSwitches" src = "images/VC-connect.png" width = 40px height=40px>
		<img hidden id = "VC-mute" class = "vcSwitches" src = "images/VC-muted.png" width = 40px height=40px>
		<img id = "VC-unmute" class = "vcSwitches" src = "images/VC-unmuted.png" width = 40px height=40px>
		<img hidden id = "VC-deafen" class = "vcSwitches" src = "images/VC-deafened.png" width = 40px height=40px>
		<img id = "VC-undeafen" class = "vcSwitches" src = "images/VC-undeafened.png" width = 40px height=40px>
		<script>
		var canJoinVc=false;
		const vcConnect = document.getElementById("VC-connect");
		vcConnect.addEventListener("click", () =>{
				document.getElementById("VC-connect").setAttribute("hidden", true);
				document.getElementById("VC-disconnect").removeAttribute("hidden");
				document.getElementById("VC-connect").style.opacity = "0.6";
		});
		const vcDisconnect = document.getElementById("VC-disconnect");
		vcDisconnect.addEventListener("click", () =>{
			if(canJoinVc){
				document.getElementById("VC-disconnect").setAttribute("hidden", true);
				document.getElementById("VC-connect").removeAttribute("hidden");
				document.getElementById("VC-connect").style.opacity = "1";
			}
		});
		
		const vcMute = document.getElementById("VC-mute");
		vcMute.addEventListener("click", () =>{
			document.getElementById("VC-mute").setAttribute("hidden", true);
			document.getElementById("VC-unmute").removeAttribute("hidden");
			document.getElementById("VC-unmute").style.opacity = "1";
		});
		const vcUnmute = document.getElementById("VC-unmute");
		vcUnmute.addEventListener("click", () =>{
			document.getElementById("VC-unmute").setAttribute("hidden", true);
			document.getElementById("VC-mute").removeAttribute("hidden");
			document.getElementById("VC-unmute").style.opacity = "0.6";
		});
		
		const vcDeafen = document.getElementById("VC-deafen");
		vcDeafen.addEventListener("click", () =>{
			document.getElementById("VC-deafen").setAttribute("hidden", true);
			document.getElementById("VC-undeafen").removeAttribute("hidden");
			document.getElementById("VC-undeafen").style.opacity = "1";
		});
		const vcUndeafen = document.getElementById("VC-undeafen");
		vcUndeafen.addEventListener("click", () =>{
			document.getElementById("VC-undeafen").setAttribute("hidden", true);
			document.getElementById("VC-deafen").removeAttribute("hidden");
			document.getElementById("VC-undeafen").style.opacity = "0.6";
		});
		</script>
		<div class ="vcMemberText">
		<b>Members:</b>
			<div id = "vcList" class ="vcMembers">
			
			Loading Members...
			
			</div>
		</div>
		
		
		<%
		
		
		String findOwner = "SELECT board.creator, user.id, user.username FROM board, user WHERE board.number = " + board + " AND user.id = board.creator";
		ResultSet result = stmt1.executeQuery(findOwner);
		String creator = "";
		int creatorID = -1;
		while(result.next()){
			creator = result.getString("user.username");
			creatorID = result.getInt("user.id");
		}
	
	%>
	</div>
	
	<div hidden id = "bottom_bar" class="bottom_bar">
		<input id = "Add" value="Add user"  type="submit" class = "button" style='margin-top:5px; margin-left:20px' >
		
		<input id = "Remove" value="Remove user"  type="submit" class = "button" style='margin-top:5px; margin-left:10px' >
		
		<form id = "addform" onsubmit="invite()" action="" target="dummyframe">
			<input hidden id="ia1" type="text" name="input_create" size="23" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "New user id (#)..."/>
			<input hidden id = "ia2" value="Add"  type="submit" class = "button3" >
		</form>
		
		<form id = "removeform" onsubmit="kick()" action="" target="dummyframe">
			<input hidden id="ir1" type="text" name="input_remove" size="23" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "Removing user id (#)..."/>
			<input hidden id = "ir2" value="Remove"  type="submit" class = "button3" >
		</form>	
			
	</div>
	
	<script>
		function adminButtons (){
			<%
			int isAdmin = 0;
			if(session.getAttribute("userid").toString().equals(""+creatorID)){
				isAdmin = 1;
			}
			if(isAdmin == 1){
				%>
				document.getElementById("sidebar").style.height = "calc(100% - 215px)";
				document.getElementById("bottom_bar").removeAttribute("hidden");
				
					const x3 = document.getElementById('sidebar');
						x3.addEventListener("click", () => {
						document.getElementById("bottom_bar").style.top = "calc(100% - 45px)";
						document.getElementById("bottom_bar").style.width = "210px";
						document.getElementById("sidebar").style.height = "calc(100% - 215px)";
						document.getElementById("ir1").setAttribute("hidden", true);
						document.getElementById("ir2").setAttribute("hidden", true);
						document.getElementById("ia1").setAttribute("hidden", true);
						document.getElementById("ia2").setAttribute("hidden", true);
						}
					);
				<%
			}
			%>
			
			const x1 = document.getElementById('Add');
			x1.addEventListener("click", () => {
				document.getElementById("bottom_bar").style.top = "calc(100% - 90px)";
				document.getElementById("bottom_bar").style.width = "260px";
				document.getElementById("sidebar").style.height = "calc(100% - 260px)";
				document.getElementById("ia1").removeAttribute("hidden");
				document.getElementById("ia2").removeAttribute("hidden");
				document.getElementById("ir1").setAttribute("hidden", true);
				document.getElementById("ir2").setAttribute("hidden", true);
				}
			);
			
			const x2 = document.getElementById('Remove');
			x2.addEventListener("click", () => {
				document.getElementById("bottom_bar").style.top = "calc(100% - 90px)";
				document.getElementById("bottom_bar").style.width = "280px";
				document.getElementById("sidebar").style.height = "calc(100% - 260px)";
				document.getElementById("ir1").removeAttribute("hidden");
				document.getElementById("ir2").removeAttribute("hidden");
				document.getElementById("ia1").setAttribute("hidden", true);
				document.getElementById("ia2").setAttribute("hidden", true);
				}
			);
			
			
		}
		function invite(){
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Homepage.jsp","SubmitPost.jsp");
			if(x.indexOf('?')<0)n=x.replace("Homepage.jsp","SubmitPost.jsp");
			var q=<%out.print(board);%>;
			if(x.indexOf("board=")<0)q=1;
			$.get(n+"?autoOn=autoOff&input_text=/invite "+encodeURIComponent(document.forms[0].input_create.value)+"&board_num="+q).then(apiRefresh).fail(function(){document.querySelectorAll("[id='two']")[2].innerHTML="Loading...</a>";});
			document.forms[0].input_create.value="";
		}

		function kick(){
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Homepage.jsp","SubmitPost.jsp");
			if(x.indexOf('?')<0)n=x.replace("Homepage.jsp","SubmitPost.jsp");
			var q=<%out.print(board);%>;
			if(x.indexOf("board=")<0)q=1;
			$.get(n+"?autoOn=autoOff&input_text=/kick "+encodeURIComponent(document.forms[1].input_remove.value)+"&board_num="+q).then(apiRefresh).fail(function(){document.querySelectorAll("[id='two']")[3].innerHTML="Loading...</a>";});
			document.forms[1].input_remove.value="";
		}
		adminButtons();
	</script>
	
	<div class = "fix-div">
	
	<%
	
	//TOP BAR
	
	out.print("<h1 style = \"color:rgb(255,255,255); font-size:15px; font-family:calibri; text-align:right;position:relative;\"></style>");
	out.print("Hello <div id=\"profileName\" style=\"display:inline\">" + session.getAttribute("username").toString() + "</div>! | ");
	out.print("<style type=\"text/css\"> a{color:LightGrey; font-family:calibri; text-align:right; font-size:15px}</style>");
	out.print("<a href=\"Profile.jsp\"> Profile</a>&nbsp;| ");
	out.print("<a href=\"MainMenu.jsp\"> Home</a>&nbsp;| ");
	out.print("<a href=\"Logout.jsp\"> Log out</a> &nbsp;&nbsp;");
	
	out.print("<style type=\"text/css\"> h1{color:rgb(255,255,255); text-align:left; font-size:15px}</style>");
	out.print("<img src=\"images/hydar.png\" alt=\"hydar\" width = \"25px\" height = \"40px\" align = \"center\" style =\"margin-right:10px\">");

	
	//out.print("&nbsp;&nbsp;&nbsp;Pick a board: ");
	%>
	<div style = "display:none;">
	<%
	out.print("<form method=\"get\" action=\"Homepage.jsp\">");
	out.print("<select name=\"board\">" );
	out.print("<option value = \"" + board + "\"> ---</div>");
	
	// board selector
	for(int i = 0; i < boardArray.length; i++){
		checkBoards = "SELECT board.name FROM board WHERE board.number = "+boardArray[i];	
		boardsQuery = stmt1.executeQuery(checkBoards);
		b = "";
		while(boardsQuery.next()){
			b=boardsQuery.getString("board.name");
		}
		out.print("<option value = \""+ boardArray[i] +"\"> " + b);
	}
	out.print("<input value=\"Go\"  type=\"submit\"></select></form>");

	
	out.print("</h1>");
	
	// PICTURE
	
	%>
	<style>
		img.boardImage{
			filter:brightness(70%);
			position:fixed;
			top:0px;
			left:0px;
			box-shadow:0 0 10px rgba(0,0,0,20);
		}
	</style>
	<img class = "boardImage" src = "<%out.print("menuImages/" + boardImage);%>" width = 200px>
	<%
	
	
	
	
	//out.print("<li>Search posts: ");
	//out.print("<form method=\"get\" action=\"Homepage.jsp\">");
	//out.print("<input type=\"text\" name=\"searchquery\" size=\"30\">");
	//out.print("<input value=\"Search\"  type=\"submit\"> </form> </li>");
	
	// TYPE MESSAGE BAR
	
	out.print("<style> .nav{text-align:center; font-family:calibri; list-style-type:none; margin:0; padding:0} .nav li{color:rgb(255,255,255); display:inline-block;  font-size:20px;  position:relative; top:-12px;}</style>");
	out.print("<ul class=\"nav\"><li>");
	out.print("<iframe sandbox name=\"dummyframe\" id=\"dummyframe\" style=\"display: none;\"></iframe>");
	out.print("<form onsubmit=\"post()\" action=\"NoContent.jsp\" target=\"dummyframe\">");

	%>
	<style>
	.replyingToText{
		position:absolute;
		top:-22px;
		font-size:15px;
		text-align:left;
	}
	</style>
	<div id = "replyingToText" class = "replyingToText">Posting in <%out.print(b); %>:</div>
	<input id="input_text" type="text" name="input_text" size="80" value = "hydar" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;" placeholder = "Enter text to post..." autofocus="autofocus" onfocus="this.select()"/>
	
	<%
	
	out.print("<input value=\"" + board + "\"  type=\"hidden\" name=\"board_num\">");	
	
	// POST BUTTON
	
	%>

	<input value="  Post  "  type="submit" class = "button" >
		
	<%
	// GREY TEXT BAR
	
		out.print("<style> p{color:LightSlateGrey; font-family:calibri; text-align:center; font-size:15px; position:relative; top:5px;line-height:1px;}</style>");
		
		out.print("<p>Auto update posts: ");
		out.print("<style> #two{color:LightSlateGrey; font-family:calibri; text-align:center; font-size:15px;}</style>");
		if(autoRefresh.equals("autoOff")){
			out.print("<a id = \"two\" href=\"Homepage.jsp?autoOn=autoOn&board="+board+"\">");
			out.print("Off</a>");
		}else{
			out.print("<a id = \"two\" href=\"Homepage.jsp?autoOn=autoOff&board="+board+"\">");
			out.print("On</a>");
		}
		out.print("&nbsp&nbsp|&nbsp&nbsp");
		out.print("<a id = \"two\" href=\"javascript:void(0);\">");
		out.print("Instant update</a>");
		out.print("</p>");
	%>
	</div>
	<div class="margin">
	</form>
	</li>
	</ul>
	
	<!-- NEW MESSAGES  BAR -->
	
	<style> body{color:rgb(255,255,255); font-family:calibri; text-align:left; font-size:15px;}</style>
		
	
	<div hidden id='bar' style='width: 100%; height: 1px; border-bottom: 2px solid #be4949; text-align: center'>
		
		<span style='color:White; font-size: 12px; display:block;line-height:8px;'><br>
			<b> - New Messages - </b>
		</span>
	</div>
	<div id="txtHint" style="line-height:25px;">Posts will be listed here...</div>
	<%

	// SHOW MESSAGES
	
	%>
	<style>
		.reply_button{
			opacity:0.2;
			position:relative;
			float:right;
			text-align:right;
			left:-20px;
			top:38px;
		}
		.reply_button:hover{
			opacity:1;
			scale:1.3;
			cursor:pointer;
		}
		</style>
		
		
		<script>
		var replyID = -1;
		</script>
	<%
	
	
	Statement stmt = conn.createStatement();
	String checkPostsStr="SELECT post.id, user.id, user.username, user.pfp, post.board, post.contents, post.created_date"
				+ " FROM user, posts, post"
				+ " WHERE posts.post = post.id AND user.id = posts.user AND post.board = " + board
				+ " ORDER BY post.id DESC";
	result = stmt.executeQuery(checkPostsStr);
	//int topPostID = result.getInt("post.id");
	
	int count = 25; // <- DISPLAYED POSTS LIMIT XXXXXXXXXXXXXXXXXX
	int maxCount = count;
	out.print("<div id='msgs'>");
	ArrayList<Long> timestamps = new ArrayList<Long>();
	while(result.next() && count > 0){
		//time
		//constantly updated array of timestamps in js
		//
		if(count==maxCount)
			out.print("<div hidden style=\"display:none\" id=\"lastID\">"+result.getInt("post.id")+"</div>");
		float timePassed = ((float)(System.currentTimeMillis() - result.getLong("post.created_date")) / 3600000);
		timestamps.add(result.getLong("post.created_date"));
		out.print("<img src=\"" + result.getString("user.pfp") +"\" alt=\"hydar\" width = \"40px\" style=\"border-radius:40px\" height = \"40px\" align = \"left\" hspace = \"10\" vspace = \"15\">");
		//other contents
		out.print("<style> body{color:LightGrey; font-family:calibri; text-align:left; font-size:15px; display:block}</style>");

		//replies?
		%>
		<style>
		#reply_button<%out.print(result.getString("post.id"));%>
		{
			scroll-margin-top:135px;
		}
		</style>
		<img id = "reply_button<%out.print(result.getString("post.id"));%>" class = "reply_button" src = "images/reply-arrow.png" width = 15px height=15px>
		<script>
		replyID = -1;
		var reply_button<%out.print(result.getString("post.id"));%> = document.getElementById(<%out.print("\"reply_button"+result.getString("post.id")+"\"");%>);
		reply_button<%out.print(result.getString("post.id"));%>.addEventListener("click", () =>{
			var isReply = document.getElementById(<%out.print("\"actualContents"+result.getString("post.id")+"\"");%>);
			if(isReply!=null){
				document.getElementById("input_text").value = "Replying to "+<%out.print("\""+result.getString("user.username")+" \"");%>+document.getElementById(<%out.print("\"actualContents"+result.getString("post.id")+"\"");%>).innerHTML + ":"
				+ document.getElementById("input_text").value;
				replyID = <%out.print(result.getString("post.id"));%>;
			}else{
				document.getElementById("input_text").value = "Replying to "+<%out.print("\""+result.getString("user.username")+" "+ result.getString("post.contents").replace("\"","\\\"")+":\"");%> 
				+ document.getElementById("input_text").value;
				replyID = <%out.print(result.getString("post.id"));%>;
			}
		});
		</script>
		<%
		
		out.print("<br><b><div id=\"msgUser\" style=\"display:inline\">"+ result.getString("user.username") + "</div></b> <div id=\"three\" style=\"display:inline\">");
		out.print("<style> #three{color:Grey; font-family:calibri; text-align:left; font-size:15px; display:inline}</style>");
		if((int)(timePassed * 60)==1){
			out.print("&nbsp;(1 minute ago): ");
		}else if((int)(timePassed)==1){
			out.print("&nbsp;(1 hour ago): ");
		}
		else if((timePassed * 60) < 1){
			out.print("&nbsp;(just now): ");
		}else if(timePassed < 1){
			out.print("&nbsp;(" + (int)(timePassed * 60) + " minutes ago): ");
		}else{
			out.print("&nbsp;(" + (int)(timePassed) + " hours ago): ");
		}
		
		//WHITELIST XxxxxxXXXXXXXXXXXXXXXXXXXX
		
		
		
		//html = html.replace(/</g, "&lt;").replace(/>/g, "&gt;");
		//String fixedString = result.getString("post.contents").replaceAll("<", "&lt;");
		//fixedString=fixedString.replaceAll("&lt;href", "<href").replaceAll("&lt;img", "<img").replaceAll("&lt;br", "<br");
		out.print("</div><br><div id=\"msgText\"  style=\"display:block; margin-left:60px; word-wrap: break-word;\">" + result.getString("post.contents") +"</div><br clear = \"left\">");
	
		count-=1;
	}
	out.print("<div hidden style=\"display:none\" id=\"timestamps\">"+timestamps+"</div>");
	if(count == 25){
		out.print("<div hidden style=\"display:none\" id=\"lastID\"> 0 </div>");
	}
	
	out.print("</div>");
	
	
	// SCRIPT TO AUTO UPDATE POSTS
	
	
	%>
	<script>
		var api;
		var idle = 0;
		var lastID = parseInt(document.getElementById("lastID").innerHTML);
		var timestamps = eval(document.getElementById("timestamps").innerHTML);
		document.addEventListener('click',()=>{idle=0;document.querySelector("link[rel*='icon']").href = "favicon.ico";document.getElementById("bar").setAttribute("hidden",true);});
		document.addEventListener('hover',()=>{idle=0;});
		document.addEventListener('keypress',()=>{idle=0;});
		document.addEventListener('mousemove',()=>{idle=0;});
		function updateTimestamps(){
			var now = Date.now()/1000;
			
			for(var i in document.getElementById("msgs").querySelectorAll("[id = 'three']")){
				var dt = now-timestamps[i]/1000;
				var tString="<style> #three{color:Grey; font-family:calibri; text-align:left; font-size:15px; display:inline}</style>";
				if(dt/3600>=2){
					tString+="&nbsp;("+Math.floor(dt/3600)+" hours ago):";
				}else if(dt/3600>=1){
					tString+="&nbsp;("+Math.floor(dt/3600)+" hour ago):";
				}else if(dt/60>=2){
					tString+="&nbsp;("+Math.floor(dt/60)+" minutes ago):";
				}else if(dt/60>=1){
					tString+="&nbsp;("+Math.floor(dt/60)+" minute ago):";
				}else{
					tString+="&nbsp;(just now):";
				}
				document.getElementById("msgs").querySelectorAll("[id = 'three']")[i].innerHTML=tString;
			}
		}
		setInterval(updateTimestamps,1000);
		function apiRefresh(){
			var x=document.location.toString().replace("Homepage.jsp","MsgApi.jsp");
			var n=x.substring(0,x.indexOf('?'));
			if(x.indexOf('?')<0)n=x;
			var q=<%out.print(board);%>;
			if(x.indexOf("board=")<0)q=1; 
			if(x.includes("input_text"))return;
			var toPrepend="";
			$.get(n+"?board="+q+"&last_id="+lastID).then(function (data) {
				var lines=[]
				try{
					lines = data.split("<br>");
					lines.splice(lines.length-1);
				}catch(how){
					return;
				}
				var lineEnd=lines.length;
				for(var i=0;i<lineEnd;i+=6){
					if(i==0&&eval(lines[i])==-1){
						refresh(true,true);
						return;
					}
					if(lines.length<6)
						return;
					try{
						while(lines[i+5].length<lines[i+4]){
							lines[i+5]+="<br>"+lines[i+6];
							lineEnd--;
							lines.splice(i+6,1);
						}
						lastID=eval(lines[0]);
						toPrepend+="<img src = '"+lines[i+2]+"' alt='hydar' style='border-radius:40px' width='40px' vspace='15' hspace='10' height='40px' align='left'>"
						toPrepend+="<style> body{color:LightGrey; font-family:calibri; text-align:left; font-size:15px; display:block}</style><br><b><div id='msgUser' style='display:inline'>"+lines[i+1]+"</div></b>";
						
						//the hyauctions.
						toPrepend+="<style>";
						toPrepend+="#reply_button"+lines[i];
						toPrepend+="{";
						toPrepend+="	scroll-margin-top:135px;";
						toPrepend+="}";
						toPrepend+="</style>";
						toPrepend+="<img id = 'reply_button"+lines[i]+"' class = 'reply_button' src = 'images/reply-arrow.png' width = 15px height=15px>";
						toPrepend+="<script>";
						toPrepend+="replyID = -1;";
						toPrepend+="var reply_button"+lines[i]+" = document.getElementById('reply_button"+lines[i]+"');";
						toPrepend+="reply_button"+lines[i]+".addEventListener('click', () =>{";
						toPrepend+="	var isReply = '"+ lines[i+5]+"'.substring(0,32).includes(\\"<div hidden id = 'actualContents\\");";
						toPrepend+="	if(isReply){";
						toPrepend+="		document.getElementById('input_text').value = 'Replying to '+"+"'"+lines[i+1]+"' "+"+document.getElementById('actualContents"+lines[i]+"'"+").innerHTML + ':'";
						toPrepend+="		+ document.getElementById('input_text').value;";
						toPrepend+="		replyID = "+lines[i]+";";
						toPrepend+="	}else{";
						toPrepend+="		document.getElementById('input_text').value = 'Replying to '+'"+lines[i+1]+" "+ lines[i+5]+"".replace("\\"","\\\\\\"")+":'"+" ";
						toPrepend+="		+ document.getElementById('input_text').value;";
						toPrepend+="		replyID = "+lines[i]+";";
						toPrepend+="	}";
						toPrepend+="});";
						toPrepend+="<"+"/script>";
						
						
						toPrepend+="<div id='three' style='display:inline'><style> "
							+"#three{color:Grey; font-family:calibri; text-align:left; font-size:15px; display:inline}</style>&nbsp;(just now): "
							+"</div><br><div id='msgText' style='display:block; margin-left:60px; word-wrap: break-word;'>"+lines[i+5]+"</div><br clear='left'>";
						
						timestamps = [lines[i+3]].concat(timestamps);
						while(timestamps.length>25){
							timestamps.splice(25);
						}
						if((!document.hasFocus()||idle>14)&&document.getElementById("profileName").innerHTML!=lines[i+1]){
							document.querySelector("link[rel*='icon']").href = "favicon2.ico";
							document.getElementById("bar").removeAttribute("hidden");
							try{
								h=new Notification(lines[i+1],{body:lines[i+5],icon:"images/notifhydar.png"});
								var pingSound = new Audio("audio/ping.mp3");
								pingSound.volume = <%out.print(volume * 0.2 * pingvolume);%>;
								<%
								if(pings == 1){
									%>pingSound.play();	<%
								}
								%>
							}catch(e){

							}
						}
					}
					catch(ee2){
						console.log(ee2);
					}
				}if(lines.length<6)
						return;
				document.getElementById("msgs").removeChild(document.getElementById("lastID"));
				toPrepend="<div style='display:none' id='lastID' hidden=''>"+eval(lines[0])+"</div>"+toPrepend;
				document.getElementById("msgs").innerHTML=toPrepend+document.getElementById("msgs").innerHTML;
				while(document.getElementById("msgs").children.length>(201+(lines.length-lineEnd))){
					document.getElementById("msgs").removeChild(document.getElementById("msgs").children[201+(lines.length-lineEnd)]);
					
				}
				
			}).fail(function () {document.querySelectorAll("[id='two']")[1].innerHTML="Loading...</a>";});
		}
		function refresh(a,b){
			idle+=1;
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?'));
			if(x.indexOf('?')<0)n=x;
			var q=<%out.print(board);%>;
			if(x.indexOf("board=")<0)q=1; 
			if(x.includes("input_text"))return;
			$.get(n+"?board="+q+"&autoOn=autoOff").then(function (data) {
				var parser = new DOMParser();
				var doc = parser.parseFromString(data, "text/html");
				const hdar = document.createElement("div");
				hdar.setAttribute("id","msgs");
				hdar.innerHTML=doc.getElementById("msgs").innerHTML;var x=(parseInt(doc.getElementById("lastID").innerHTML)-parseInt(document.getElementById("lastID").innerHTML));
				document.querySelectorAll("[id='two']")[1].innerHTML="Instant update</a>";
				timestamps = eval(doc.getElementById("timestamps").innerHTML);
				lastID = eval(doc.getElementById("lastID").innerHTML);
				if(document.querySelectorAll("[id='two']")[0].innerHTML.includes("Off")){
					if(!b)x-=1;
					document.getElementById("txtHint").innerHTML=""+(x>0?x:"No")+" new posts.<br>Post"+(x==1?"":"s")+" will be listed here...";
					//setTimeout(() => {
					//	document.getElementById("txtHint").innerHTML="Posts will be listed here...";
					//},8000);
				}
				if(!b){
					if(!(hdar.innerHTML==document.getElementById("msgs").innerHTML)){
						document.getElementById("msgs").parentNode.replaceChild(hdar,document.getElementById("msgs"));
					}
				}else{
					if(!(hdar.innerHTML==document.getElementById("msgs").innerHTML)){
						for(i in document.getElementById("msgs").querySelectorAll("[id = 'three']")){
							if(hdar.querySelectorAll("[id = 'three']").length>i){
								document.getElementById("msgs").querySelectorAll("[id = 'three']")[i].parentNode.replaceChild(hdar.querySelectorAll("[id = 'three']")[0],document.getElementById("msgs").querySelectorAll("[id = 'three']")[i]);
							}
						}hdar.innerHTML=doc.getElementById("msgs").innerHTML;
					if((x!=0)&&a){
						document.getElementById("msgs").parentNode.replaceChild(hdar,document.getElementById("msgs"));
						if((!document.hasFocus()||idle>14)&&document.getElementById("profileName").innerHTML!=document.getElementById("msgUser").innerHTML){
							document.querySelector("link[rel*='icon']").href = "favicon2.ico";
							document.getElementById("bar").removeAttribute("hidden");
							try{
								h=new Notification(document.getElementById("msgUser").innerHTML,{body:document.getElementById("msgText").innerHTML,icon:"images/notifhydar.png"});
								var pingSound = new Audio("audio/ping.mp3");
								pingSound.volume = <%out.print(volume * 0.2 * pingvolume);%>;
								<%
								if(pings == 1){
									%>pingSound.play();	<%
								}
								%>
							}catch(e){

							}
						}
					}
				}
			}	
			}).fail(fullRefresh);
		}function fullRefresh(){
			refresh(true,true);	
		}function halfRefresh(){
			refresh(false,true);	
		}function extraRefresh(){
			refresh(true,false);
		}function post(){
			clearInterval(api);
			api = setInterval(apiRefresh,1000);
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Homepage.jsp","SubmitPost.jsp");
			if(x.indexOf('?')<0)n=x.replace("Homepage.jsp","SubmitPost.jsp");
			var q=<%out.print(board);%>;
			if(x.indexOf("board=")<0)q=1;
			$.get(n+"?autoOn=autoOff&replyID="+replyID+"&input_text="+encodeURIComponent(document.forms[3].input_text.value)+"&board_num="+q).then(apiRefresh).fail(function(){document.querySelectorAll("[id='two']")[1].innerHTML="Loading...</a>";});
			document.forms[3].input_text.value="";
		}
		document.querySelectorAll("[id='two']")[1].addEventListener('click',fullRefresh);
	</script>
	
	
	
	<%//VC SCRIPT %>
	
	
	
	<script>
		var myHostname = window.location.hostname;
		var vcvolume=<%out.print(volume * 0.2 * voicevolume);%>;
		var timer=10;
		if (!myHostname) {
		  myHostname = "localhost";
		}var connection = null;
		var clientID = eval(<%out.print(session.getAttribute("userid"));%>);
		var thisName=null;
		var targets=[];
		function getPeer(id){
			for(var x in targets){
				if(targets[x].id==id)
					return x;
			}return null;
		}
		var constraints = {
			audio: 
			{ 
				"autoGainControl": true,
				"echoCancellation": true,
				"noiseSuppression": true
			}
		}
		var connection=null;
		var serverUrl;
		var muted=false;
		var leaveVC = ()=>{
			sendToServer("user-leave\n"+clientID+"\n"+<%out.print(board);%>+"\n");
			sendToServer("user-list\n"+clientID+"\n"+<%out.print(board);%>+"\n");
			thisName=null;
			targets.forEach((x)=>{closeVc(x.id)});
		}
		async function apply(t,c) {
		  await t.applyConstraints(Object.assign(track.getSettings(), c));
		}
		var sendToServer = (s)=>{
			connection.send(s);
			console.log("OUT"+s.substring(0,s.indexOf("\n")));
		}
		  var scheme = "ws";
		  if (document.location.protocol === "https:") {
		    scheme += "s";
		  }
		  serverUrl = scheme + "://" + myHostname + ":"+document.location.port+"/HydaRTCSignal.jsp";
		  console.log("Connecting to server: "+serverUrl);
		  var makeSocket = function(){
			  connection = new WebSocket(serverUrl);
			  
			  connection.onopen = function(evt) {
				//document.getElementById("leaveVC").removeAttribute("hidden");
				setInterval(()=>{sendToServer("hydar\n"+clientID+"\n"+<%out.print(board);%>+"\n")},2000);
				document.getElementById("VC-disconnect").addEventListener("click",()=>{
					if(canJoinVc){
						sendToServer("user-join\n"+clientID+"\n"+<%out.print(board);%>+"\n");
						targets.forEach((x)=>{vc_invite(x.id);sendToServer("user-list\n"+clientID+"\n"+<%out.print(board);%>+"\n");});
					}
				});
				document.getElementById("VC-connect").addEventListener("click",leaveVC);
				document.getElementById("VC-deafen").addEventListener("click",()=>{
					if(vcvolume>0){
						vcvolume=0;
						try{
							targets.forEach((x)=>{document.getElementById("hydar_audio"+x.id).volume=0;});
						}catch(e){}
					}else{
						vcvolume= <%out.print(volume * 0.2 * voicevolume);%>;
						try{
							targets.forEach((x)=>{document.getElementById("hydar_audio"+x.id).volume=vcvolume;});
						}catch(e){}
						}
				});
				document.getElementById("VC-undeafen").addEventListener("click",()=>{
					if(vcvolume>0){
						vcvolume=0;
						try{
							targets.forEach((x)=>{document.getElementById("hydar_audio"+x.id).volume=0;});
						}catch(e){}
					}else{
						vcvolume=<%out.print(volume * 0.2 * voicevolume);%>;
						try{
							targets.forEach((x)=>{document.getElementById("hydar_audio"+x.id).volume=vcvolume;});
						}catch(e){}
						}
				});
				
				document.getElementById("VC-mute").addEventListener("click",()=>{
					
					if(muted){
						muted=false;
						targets.forEach((conn)=>{conn.pc.getLocalStreams().forEach((strm)=>{strm.getAudioTracks().forEach((track)=>{track.enabled=true;})})});
					}else{
						muted=true;
						targets.forEach((conn)=>{conn.pc.getLocalStreams().forEach((strm)=>{strm.getAudioTracks().forEach((track)=>{track.enabled=false;})})});
						}
				});
				document.getElementById("VC-unmute").addEventListener("click",()=>{
					
					if(muted){
						muted=false;
						targets.forEach((conn)=>{conn.pc.getLocalStreams().forEach((strm)=>{strm.getAudioTracks().forEach((track)=>{track.enabled=true;})})});
					}else{
						muted=true;
						targets.forEach((conn)=>{conn.pc.getLocalStreams().forEach((strm)=>{strm.getAudioTracks().forEach((track)=>{track.enabled=false;})})});
						}
				});
			  }
			  connection.onclose=function(evt){
				//leaveVC();
				//setTimeout(joinVC,2500);
			  }
			  connection.onerror = function(evt) {
				//leaveVC();
				//setTimeout(joinVC,2500);
				console.dir(evt);
			  }
			  connection.onmessage = async function(evt) {
				  timer=7;
				  
				  var msg = evt.data;
				  var type = msg.substring(0,msg.indexOf('\n'));
				  msg = msg.substring(msg.indexOf('\n')+1);
				  var user = msg.substring(0,msg.indexOf('\n'));
				  msg = msg.substring(msg.indexOf('\n')+1);
				  var board = msg.substring(0,msg.indexOf('\n'));
				  msg = msg.substring(msg.indexOf('\n')+1);
						console.log("IN:"+type);
				  switch(type){
					  case "user-leave":
						break;
					  case "user-list":
						clientID=eval(user);
					var ids = msg.substring(0,msg.indexOf('\n'));
					msg = msg.substring(msg.indexOf('\n')+1);
					oldTargets = targets.concat();
					newTargets = eval(ids);
					var userList = eval(msg);
					var tempTargets=[];
					var isJoined=false;
					for(var x in newTargets){
						if(newTargets[x]==clientID){
							thisName=userList[x];
							isJoined=true;
							continue;
						}var oldIndex=-1;
						for(var i in oldTargets){
							if(oldTargets[i].id==newTargets[x]){
								oldIndex=i;
							}
						}
						if(oldTargets[oldIndex]){
							tempTargets.push({id:newTargets[x],pc:oldTargets[oldIndex].pc,active:oldTargets[oldIndex].active,name:userList[x],timer:oldTargets[oldIndex].timer});
						}else{
							tempTargets.push({id:newTargets[x],pc:null,active:false,name:userList[x],timer:100});
						}
					}
					targets = tempTargets.concat();
					document.getElementById("vcList").innerHTML="";
					for(var x in targets){
						var tAlive=false;
						var transportStates=[];
						if(targets[x]&&targets[x].pc)
							targets[x].pc.getSenders().forEach((x)=>{if(x.transport)transportStates.push(x.transport.state);});
						if(!targets[x]||targets[x].active==false||!targets[x].pc||transportStates.includes("failed"))
							document.getElementById("vcList").innerHTML+=targets[x].name+"<div style='display:inline;color:rgb(255,0,0)'></style>"+"<br>";
						else if(!targets[x].pc||targets[x].pc.iceConnectionState!="connected")
							document.getElementById("vcList").innerHTML+=targets[x].name+"<div style='display:inline;color:rgb(255,128,0)'> connecting...</style>"+"<br>";
						else if(transportStates.includes("connecting"))
							document.getElementById("vcList").innerHTML+=targets[x].name+"<div style='display:inline;color:rgb(255,255,0)'> encrypting...</style>"+"<br>";
						else if(targets[x].active==true){
							tAlive=true;
							targets[x].timer=3;
							document.getElementById("vcList").innerHTML+=targets[x].name+"<div style='display:inline;color:rgb(0,255,0)'> (connected)</style>"+"<br>";
						}if(!tAlive&&targets[x].timer<0){
							targets[x].timer=3;
							if(targets[x].pc)
								targets[x].pc.restartIce();
						}
					}if(thisName!=null){
						document.getElementById("vcList").innerHTML+=thisName+"<div style='display:inline;color:rgb(0,255,255)'> (you)</style>"+"<br>";
					}
					canJoinVc=true;
					break;
					  case "p2ptest":
					  break;
					  case "vc-offer":
						var target = parseInt(user);
						var sdp = msg.substring(msg.indexOf('\n')+1);
						var desc = new RTCSessionDescription({sdp:(new String(sdp)+"\r\n"),type:"offer"});
						if(!targets[getPeer(target)]){
							targets.push({id:target,pc:null});
						}
						  if (!targets[getPeer(target)].pc){
							  await createPeerConnection(target);
						  }
							console.log ("  - Setting remote description");
							await targets[getPeer(target)].pc.setRemoteDescription(desc);
							  var stream = await navigator.mediaDevices.getUserMedia(constraints);
			
							try {
							  stream.getTracks().forEach(
								(track)=>{
								if(muted)
									track.enabled=false;
								targets[getPeer(target)].pc.addTrack(track, stream);}
							  );
							} catch(err) {
							   console.dir(err);
							  return;
							}
						  
						  await targets[getPeer(target)].pc.setLocalDescription(await targets[getPeer(target)].pc.createAnswer());
						console.log("we made it to the end of offer thing, there was probably an error if this isn't here");
					  sendToServer("vc-answer\n"+clientID+"\n"+<%out.print(board);%>+"\n"+target+"\n"+targets[getPeer(target)].pc.localDescription.sdp);
					  break;
					  case "vc-answer":
					  console.log("got vc answer");
					  var target = parseInt(user);
						var sdp = msg.substring(msg.indexOf('\n')+1);
						var desc = new RTCSessionDescription({sdp:(new String(sdp)+"\r\n"),type:"answer"});
						await targets[getPeer(target)].pc.setRemoteDescription(desc);//handle error maybe
					  break;
					  case "new-ice-candidate":
						  var target = user;
						msg = msg.substring(msg.indexOf('\n')+1);
						  var remoteMid = msg.substring(0,msg.indexOf('\n'));
						msg = msg.substring(msg.indexOf('\n')+1);
						  var remoteMLI = msg.substring(0,msg.indexOf('\n'));
						  var sdp = msg.substring(msg.indexOf('\n')+1);
						var candidate = new RTCIceCandidate({candidate:new String(sdp),sdpMid:remoteMid,sdpMLineIndex:eval(remoteMLI)});
						try {
							 await targets[getPeer(target)].pc.addIceCandidate(candidate);
						  } catch(err) {
							  console.log("ADD ICE ERROR");
							console.log(err);
						  }
						  break;
					  case "user-leave":
						closeVc(parseInt(user));
						  break;
					  default:
						break;
				  }
			  }
		  }
		  makeSocket();
		async function vc_invite(target){
			if (targets[getPeer(target)]&&targets[getPeer(target)].pc&&targets[getPeer(target)].active) {
			return;
		  }if(!targets[getPeer(target)]){
			  targets.push({id:target,pc:null});
		  }
		    console.log("Setting up connection to invite user: " + target);
		    await createPeerConnection(target);
		
		    var stream = await navigator.mediaDevices.getUserMedia(constraints);
		    try {
		      stream.getTracks().forEach(
			   (track)=>{
				if(muted)
					track.enabled=false;
			   targets[getPeer(target)].pc.addTrack(track,stream);}
		      );
		    } catch(err) {
				console.log("couldnt add track");
		    }
		  
		}
		function handleICECandidateEvent(target, event) {
		//make sure candidate exists(the event is triggered with no candidate to indicate end of trickle ice)
		  if (event.candidate) {
		    sendToServer("new-ice-candidate\n"+clientID+"\n"+<%out.print(board);%>+"\n"+target+"\n"+event.candidate.sdpMid+"\n"+event.candidate.sdpMLineIndex+"\n"+event.candidate.candidate);
		  }
		}
		
		async function closeVc(target){
		
		  console.log("Closing the call");
		  if(!targets[getPeer(target)]){
			  return;
		  }
		  if (targets[getPeer(target)].pc) {
		    console.log("--> Closing the peer connection");
		targets[getPeer(target)].active=false;
			for(var s1 in targets[getPeer(target)].pc.getSenders())
				targets[getPeer(target)].pc.removeTrack(targets[getPeer(target)].pc.getSenders()[s1]);
		    targets[getPeer(target)].pc.ontrack = null;
		    targets[getPeer(target)].pc.onnicecandidate = null;
		    targets[getPeer(target)].pc.oniceconnectionstatechange = null;
		    targets[getPeer(target)].pc.onsignalingstatechange = null;
		    targets[getPeer(target)].pc.onicegatheringstatechange = null;
		    targets[getPeer(target)].pc.onnotificationneeded = null;
		    await targets[getPeer(target)].pc.close();
		    targets[getPeer(target)].pc=null;
		    //transceivers.splice(targets.indexOf(target),1);
			var localAudio = document.getElementById("hydar_audio"+target);
			if(localAudio)
			localAudio.remove();
		  }
		}
		function handleICEConnectionStateChangeEvent(target,event) {
		  console.log("*** ICE connection state changed for "+target);
		  if(getPeer(target)==null)
			  return;
			if(targets[getPeer(target)].pc)
		  switch(targets[getPeer(target)].pc.iceConnectionState) {
		    case "closed":
		    case "failed":
		    case "disconnected":
		      closeVc(target);
		      break;
		  }
		}
		function handleSignalingStateChangeEvent(target,event) {
		  console.log("*** WebRTC signaling state changed to: " + targets[getPeer(target)].pc.signalingState);
		  switch(targets[getPeer(target)].pc.signalingState) {
		    case "closed":
		      closeVc(target);//possible connection param
		      break;
		  }
		}
		async function handleNegotiationNeededEvent(target) {
		  try {
			console.log("---> Creating offer");
			var offer = await targets[getPeer(target)].pc.createOffer();
			if (targets[getPeer(target)].pc.signalingState != "stable") {
			  return;
			}
			console.log("---> Setting local description to the offer");
			await targets[getPeer(target)].pc.setLocalDescription(offer);
			sendToServer("vc-offer\n"+clientID+"\n"+<%out.print(board);%>+"\n"+target+"\n"+targets[getPeer(target)].pc.localDescription.sdp+"\n");
			console.log("didnt create offer eee");
			
		  } catch(err) {
		    console.log("*** The following error occurred while handling the negotiationneeded event:\neeeeeeeee");
		    console.dir(err);
		  };
		}
		function handleTrackEvent(target,event) {
			targets[getPeer(target)].active=true;
		  console.log("*** Track event");
		  if(!document.getElementById("hydar_audio"+target)){
			var e = document.createElement("audio");
			e.setAttribute("id","hydar_audio"+target);
			document.body.append(e);
		  }document.getElementById("hydar_audio"+target).srcObject = event.streams[0];
		  document.getElementById("hydar_audio"+target).volume = vcvolume;
		  document.getElementById("hydar_audio"+target).play();
		}
		function createPeerConnection(target) {
			return new Promise((resolve)=>{
		  console.log("Setting up a connection...");
		  targets[getPeer(target)].pc = new RTCPeerConnection({
		    iceServers: [     // Information about ICE servers - Use your own!
		      { urls: ["stun:"+myHostname+":3478"]}
			 // ,
			 // {urls: ["stun:stun3.l.google.com:19302"]}
		    ]
		  });
		  targets[getPeer(target)].pc.onicecandidate = (event)=>{handleICECandidateEvent(target,event)};
		  targets[getPeer(target)].pc.oniceconnectionstatechange = (event)=>{handleICEConnectionStateChangeEvent(target,event)};
		  targets[getPeer(target)].pc.onicegatheringstatechange = (event)=>{};
		  targets[getPeer(target)].pc.onsignalingstatechange = (event)=>{handleSignalingStateChangeEvent(target,event)};
		  targets[getPeer(target)].pc.onnegotiationneeded = ()=>{handleNegotiationNeededEvent(target)};
		  targets[getPeer(target)].pc.ontrack = (event)=>{handleTrackEvent(target, event)};
  		  targets[getPeer(target)].timer=3;
		  resolve(true);
			});
		}
	</script>
	<%
	
	//AUTO RELOAD MESSAGES
	
	if(autoRefresh.equals("autoOn")){
		out.print("<script>");
		out.print("api = setInterval(apiRefresh,1000);");
		out.print("</script>");
	}else{
		out.print("<script>");
		out.print("setInterval(halfRefresh,30000);");
		out.print("</script>");
	}
	
	conn.close();
} catch (Exception e) {
	out.print("<style> body{color:rgb(255,255,255); font-family:calibri; text-align:center; font-size:20px;}</style>");
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
