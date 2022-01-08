
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
	//CHECK IF BOARD IS SPECIFIED, and redirect if the user does not have perms.
	
	String getBoard = request.getParameter("board");
	int board = 1;
	if(getBoard != null){
		board = Integer.parseInt(getBoard);
	}
	
	Statement stmt1 = conn.createStatement();
	String checkBoardsStr="SELECT * FROM isin WHERE isin.user = " + session.getAttribute("userid").toString() + "";
	ResultSet result1 = stmt1.executeQuery(checkBoardsStr);
	String boardString = "";
	while(result1.next()){
		boardString = boardString + " " + result1.getString("isin.board") + ",";
	}
	
	if(!boardString.contains(" " + board + ",")){
		board = 1;
	}
	
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
			top:170px;
			background-color: rgb(41, 47, 53);
			overflow-x:hidden;
			box-shadow:0 0 10px rgba(0,0,0,10);
		}
		
		.sidebar p{font-size:20px;}
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
		
		.margin{margin-top:150px;margin-left:210px;}
		
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
		
			if(board>=1 && board <= 3){
				out.print("Public Board </p><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); display:block; text-align: center; position:relative; top:-15px;'></div>");
			}else{
				out.print("Private Board</p><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:-15px;'></div><p6>");
				
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
				out.print("</p7>");
				
			}
			
			String findOwner = "SELECT board.creator, user.id, user.username FROM board, user WHERE board.number = " +board + " AND user.id = board.creator";
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
						document.getElementById("ir1").setAttribue("hidden", true);
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
			$.get(n+"?autoOn=autoOff&input_text=/invite "+encodeURIComponent(document.forms[0].input_create.value)+"&board_num="+q).then(extraRefresh).fail(function(){document.querySelectorAll("[id='two']")[2].innerHTML="Loading...</a>";});
			document.forms[0].input_create.value="";
		}

		function kick(){
			var x=document.location.toString();
			var n=x.substring(0,x.indexOf('?')).replace("Homepage.jsp","SubmitPost.jsp");
			if(x.indexOf('?')<0)n=x.replace("Homepage.jsp","SubmitPost.jsp");
			var q=<%out.print(board);%>;
			if(x.indexOf("board=")<0)q=1;
			$.get(n+"?autoOn=autoOff&input_text=/kick "+encodeURIComponent(document.forms[1].input_remove.value)+"&board_num="+q).then(extraRefresh).fail(function(){document.querySelectorAll("[id='two']")[3].innerHTML="Loading...</a>";});
			document.forms[1].input_remove.value="";
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
	out.print("<option value = \"" + board + "\"> ---");
	
	// board selector
	int[] boardNums = new int[(boardString.length()+1)/3];
	for(int i = 0; i < boardString.length()-1; i++){
		if(boardString.substring(i, i+1).equals(" ")){
			for(int j = i+1; j < boardString.length(); j++){
				if(boardString.substring(j, j+1).equals(",")){
					boardNums[i/3] = Integer.parseInt((boardString.substring(i+1, j)));
					break;
				}
			}
		}
	}
	for(int i = 0; i < boardNums.length; i++){
		String checkBoards = "SELECT board.name FROM board WHERE board.number = "+boardNums[i];	
		ResultSet boardsQuery = stmt1.executeQuery(checkBoards);
		String b = "";
		while(boardsQuery.next()){
			b=boardsQuery.getString("board.name");
		}
		out.print("<option value = \""+ boardNums[i] +"\"> " + b);
	}
	out.print("<input value=\"Go\"  type=\"submit\"></select></form>");
	
	out.print("</h1>");
	
	// GREY TEXT BAR
	
	out.print("<style> p{color:LightSlateGrey; font-family:arial; text-align:center; font-size:25px; position:relative; top:-8px;}</style>");
	
	//get board name
	String checkBoards = "SELECT board.name FROM board WHERE board.number = "+board;	
	ResultSet boardsQuery = stmt1.executeQuery(checkBoards);
	String b = "";
	while(boardsQuery.next()){
		b=boardsQuery.getString("board.name");
	}
	
	//printing everythig else
	out.print("<p>Viewing board: " + b + " (#" + board + ")");
	
	out.print("&nbsp&nbsp|&nbsp&nbsp Auto update posts: ");
	out.print("<style> #two{color:LightSlateGrey; font-family:arial; text-align:center; font-size:25px;}</style>");
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
	
	//out.print("<li>Search posts: ");
	//out.print("<form method=\"get\" action=\"Homepage.jsp\">");
	//out.print("<input type=\"text\" name=\"searchquery\" size=\"30\">");
	//out.print("<input value=\"Search\"  type=\"submit\"> </form> </li>");
	
	// TYPE MESSAGE BAR
	
	out.print("<style> .nav{text-align:center; font-family:arial; list-style-type:none; margin:0; padding:0} .nav li{color:rgb(255,255,255); display:inline-block; font-size:20px; padding:5px; position:relative; top:-12px;}</style>");
	out.print("<ul class=\"nav\"><li>");
	out.print("<iframe name=\"dummyframe\" id=\"dummyframe\" style=\"display: none;\"></iframe>");
	out.print("<form onsubmit=\"post()\" action=\"\" target=\"dummyframe\">");

	%>
	<input id="input_text" type="text" name="input_text" size="80" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;" placeholder = "Enter text to post..." autofocus="autofocus" onfocus="this.select()"/>
	
	<%
	
	out.print("<input value=\"" + board + "\"  type=\"hidden\" name=\"board_num\">");	
	
	// POST BUTTON
	
	%>

	<input value="  Post  "  type="submit" class = "button" >
		
	</div>
	<div class="margin">
	</form>
	</li>
	</ul>
	<br>
	
	<!-- NEW MESSAGES  BAR -->
	
	<style> body{color:rgb(255,255,255); font-family:arial; text-align:left; font-size:15px;}</style>
		
	
	<div hidden id='bar' style='width: 100%; height: 10px; border-bottom: 2px solid #be4949; text-align: center'>
		
		<span style='color:White; font-size: 12px; display:block;'><br>
			<b> - New Messages - </b>
		</span>
	</div>
	<br>
	<div id="txtHint">Posts will be listed here...</div>
	<%

	// SHOW MESSAGES

	
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
	while(result.next() && count > 0){
		//time
		if(count==maxCount)
			out.print("<div hidden style=\"display:none\" id=\"lastID\">"+result.getInt("post.id")+"</div>");
		float timePassed = ((float)(System.currentTimeMillis() - result.getLong("post.created_date")) / 3600000);
	
		out.print("<img src=\"" + result.getString("user.pfp") +"\" alt=\"hydar\" width = \"40px\" height = \"40px\" align = \"left\" hspace = \"10\" vspace = \"15\">");
		//other contents
		out.print("<style> body{color:LightGrey; font-family:arial; text-align:left; font-size:15px; display:block}</style>");
		out.print("<br><b><div id=\"msgUser\" style=\"display:inline\">"+ result.getString("user.username") + "</div></b> <div id=\"three\" style=\"display:inline\">");
		out.print("<style> #three{color:Grey; font-family:arial; text-align:left; font-size:15px; display:inline}</style>");
		if((int)(timePassed * 60)==1){
			out.print("&nbsp;("1 minute ago): ");
		}else if((int)(timePassed)==1){
			out.print("&nbsp;("1 hour ago): ");
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
		String fixedString = result.getString("post.contents").replaceAll("<", "&lt;");
		fixedString=fixedString.replaceAll("&lt;href", "<href").replaceAll("&lt;img", "<img").replaceAll("&lt;br", "<br");
		out.print("</div><br><div id=\"msgText\" style=\"display:block; margin-left:60px; word-wrap: break-word;\">" + fixedString +"</div><br clear = \"left\">");
	
		count-=1;
	}
	
	if(count == 25){
		out.print("<div hidden style=\"display:none\" id=\"lastID\"> 0 </div>");
	}
	
	out.print("</div>");
	
	
	// SCRIPT TO AUTO UPDATE POSTS
	
	
	%>
		<script>
			document.addEventListener('click',()=>{document.querySelector("link[rel*='icon']").href = "favicon.ico";document.getElementById("bar").setAttribute("hidden",true);});
			function refresh(a,b){
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
								if(hdar.querySelectorAll("[id = 'three']").length>0){
									document.getElementById("msgs").querySelectorAll("[id = 'three']")[i].parentNode.replaceChild(hdar.querySelectorAll("[id = 'three']")[0],document.getElementById("msgs").querySelectorAll("[id = 'three']")[i]);
								}
							}hdar.innerHTML=doc.getElementById("msgs").innerHTML;
						if(!(hdar.innerHTML==document.getElementById("msgs").innerHTML)&&a){
							document.getElementById("msgs").parentNode.replaceChild(hdar,document.getElementById("msgs"));
							if(!document.hasFocus()&&document.getElementById("profileName").innerHTML!=document.getElementById("msgUser").innerHTML){
								document.querySelector("link[rel*='icon']").href = "https://cdn.discordapp.com/attachments/402933483200184321/929469849133006931/favicon2.ico";
								document.getElementById("bar").removeAttribute("hidden");
								try{
									h=new Notification(document.getElementById("msgUser").innerHTML,{body:document.getElementById("msgText").innerHTML,icon:"https://cdn.discordapp.com/attachments/315971359102599168/921456500747173908/h.png"});
								}catch(e){

								}
							}
						}
					}
				}	
				}).fail(function () {document.querySelectorAll("[id='two']")[1].innerHTML="Loading...</a>";});
			}function fullRefresh(){
				refresh(true,true);	
			}function halfRefresh(){
				refresh(false,true);	
			}function extraRefresh(){
				refresh(true,false);
			}function post(){
				
				var x=document.location.toString();
				var n=x.substring(0,x.indexOf('?')).replace("Homepage.jsp","SubmitPost.jsp");
				if(x.indexOf('?')<0)n=x.replace("Homepage.jsp","SubmitPost.jsp");
				var q=<%out.print(board);%>;
				if(x.indexOf("board=")<0)q=1;
				$.get(n+"?autoOn=autoOff&input_text="+encodeURIComponent(document.forms[3].input_text.value)+"&board_num="+q).then(extraRefresh).fail(function(){document.querySelectorAll("[id='two']")[1].innerHTML="Loading...</a>";});
				document.forms[3].input_text.value="";
			}
			document.querySelectorAll("[id='two']")[1].addEventListener('click',fullRefresh);
		</script>
	
	<%
	
	//AUTO RELOAD MESSAGES
	
	if(autoRefresh.equals("autoOn")){
		out.print("<script>");
		out.print("setInterval(fullRefresh,1000);");
		out.print("</script>");
	}else{
		out.print("<script>");
		out.print("setInterval(halfRefresh,30000);");
		out.print("</script>");
	}
	
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
