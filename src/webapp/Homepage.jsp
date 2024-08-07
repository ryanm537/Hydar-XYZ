<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.sql.DataSource"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file="SkeleAdd.jsp" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<link rel="stylesheet" href="homepage.css">
<title>Home - Hydar</title>
<link rel="shorcut icon" href="favicon.ico"/>
</head> 

<body style = "background-color:rgb(51, 57, 63);"> 
<input id="fileElem" multiple style="display:none" type="file" autocomplete="off"/>

<div id="show">
</div>
<%!
//These are different from the mainmenu ones to allow indexing of name/image
static final List<Integer> DEFAULT_BOARDS=List.of(1,2,3);
static final List<String> DEFAULT_BOARDNAMES=List.of("","Everything Else","SAS4","Skyblock");
static final List<String> DEFAULT_BOARDIMAGES=List.of("","everythingelse.png","sas4.png","skyblock.png");
static final String DEFAULT_FALLBACK_IMAGE="misc.png";
%>
<%

//add session id to urls(if needed)
String PROFILE=response.encodeURL("Profile.jsp");
String MAIN_MENU=response.encodeURL("MainMenu.jsp");
String LOGOUT=response.encodeURL("Logout.jsp");
String HOMEPAGE=response.encodeURL("Homepage.jsp");

int uid=(int)session.getAttribute("userid");
String getBoard = request.getParameter("board");
int board = 1;
if(getBoard != null){
	board = Integer.parseInt(getBoard);
}
boolean isDefault = DEFAULT_BOARDS.contains(board);

	//CHECK IF BOARD IS SPECIFIED, and redirect if the user does not have perms.
	
	
try{
	//GET BOARD IMAGE
	int isPublic = 0;
	int boardDM = 0;
	String boardImage = DEFAULT_FALLBACK_IMAGE;
	if(!isDefault){
		/**String getBoardAttributes="SELECT board.image, board.public, board.dm FROM board WHERE board.number = ?";
		var ps = conn.prepareStatement(getBoardAttributes);
		ps.setInt(1,board);
		var result1 = ps.executeQuery();
		
		if(result1.next()){
			isPublic = result1.getInt("board.public");
			boardImage = result1.getString("board.image");
			boardDM = result1.getInt("board.dm");
		}*/
	}else{
		boardImage=DEFAULT_BOARDIMAGES.get(board);
		isPublic=1;
		boardDM=0;
	}
	//CHECK IF AUTO REFRESH IS ON
	
	String autoRefresh = request.getParameter("autoOn");
	if(autoRefresh == null){
		autoRefresh = "autoOn";
	}
	
	//CHECK IF USER IS LOGGED IN
	/**
	if(session.getAttribute("username")==null){
		throw new Exception();
	}
	*/
	
	
	out.print("</center>");
	
	
	%>
	<style>
		#showMembers{
			/**
			search showMembers in board.js(classnames are changed there)
			*/
			color:rgb(91, 97, 103);
			font-size:20px;
		}
		.showMembersDM{
			float:left;
			margin-left:65px;
			margin-right:unset;
		}
		.showMembersNoDM{
			float:right;
			margin-left:unset;
			margin-right:15px;
		}
		#showMembers:hover{
			background-color:rgb(61, 67, 73);
			border-radius:5px;
			padding:3px;
			padding-left:7px;
			padding-right:7px;
			margin-top:-3px;
		}
		.showMembersDM:hover{
			margin-left:58px;
			margin-right:inherit;
		}
		.showMembersNoDM:hover{
			margin-left:inherit;
			margin-right:8px;
		}
	</style>
	
	<div id = "sidebar" class = "sidebar">
		
	<p id='boardInfo'>
	<%
		
		// SIDE BAR
		
		if(board<=3){
		
			out.print("" + " </p><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); display:block; text-align: center; position:relative; top:-15px;'></div>");
			%>
			
			
			<div class="sideButtons">  </div>
			<div id="showMembers" class="sideButtons showMembersNoDM"></div>
			<div id="showChannels" class="sideButtons"></div>
			
			
			<%
			out.print(" </p><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:-15px;' hidden></div><p6>");
			out.print("<div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:15px; left:-5px;' hidden></div>");
			
		}else{
			
			boolean showingMembers = true;
			
			%>
			
			
			<div class="sideButtons"> <br> </div>
			<div id="showMembers" class="sideButtons showMembersNoDM">Members</div>
			<div id="showChannels" class="sideButtons">Channels</div>
			
			<%
			
			out.print(" </p><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:-15px;'></div><p6>");
			
			//find the creator of this board
			
			
			//member list
			out.print("<div id = 'memberslist'><b>Owner:</b><br> &nbsp<div id= 'boardCreator' style='display:inline'>"+"loading..." + "</div></p6><div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:20px; left:-5px'></div>");
			
			//find boards where this user is a member (not creator)
		
			
			out.print("<p7><b>Members:</b><br><div id='members' style='display:inline'>");
			
			out.print("loading..");
			out.print("</div></p7></div>");
			
			//channel list
			out.print("<div id = 'channelslist'>");
			out.print("loading...");
			out.print("</div>");
			
			out.print("<div style='width: 100%; border-bottom: 2px solid rgba(0,0,0,20); text-align: center; position:relative; top:15px; left:-5px;'></div>");
		}
		// hdaryhdaryhdayrhdyahryda
		
		// VCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC HYDAR
		
		// DHYARHYDHYARDHYADRYRR
		// ye, it is true that hydar
		%>
		<div class = "vcName"><b>Voice Channel</b></div>
		<img id = "VC-disconnect" class = "vcSwitches" src = "images/VC-disconnect.png" width = 40px height=40px>
		<img hidden id = "VC-connect" class = "vcSwitches" src = "images/VC-connect.png" width = 40px height=40px>
		<img hidden id = "VC-mute" class = "vcSwitches" src = "images/VC-muted.png" width = 40px height=40px>
		<img id = "VC-unmute" class = "vcSwitches" src = "images/VC-unmuted.png" width = 40px height=40px>
		<img hidden id = "VC-deafen" class = "vcSwitches" src = "images/VC-deafened.png" width = 40px height=40px>
		<img id = "VC-undeafen" class = "vcSwitches" src = "images/VC-undeafened.png" width = 40px height=40px>
		<div class ="vcMemberText">
		<b>Members:</b>
			<div id = "vcList" class ="vcMembers">
			
			Loading Members...
			
			</div>
		</div>
		
		
		<%-- creator id check removed, moved to js(adminButtons() function)--%>
	
	<div class = "ssButton" id = "ssButton"><b>Share Screen</b></div>
	<div class = "ssButton" id = "ssButton2" hidden><b>Stop Sharing</b></div>
	</div>
	
	<div hidden=1 id = "bottom_bar" class="bottom_bar">
		<input id = "Add" value="Add user"  type="submit" class = "button" style='margin-top:15px; margin-left:20px' >
		
		<input id = "Remove" value="Remove user"  type="submit" class = "button" style='margin-top:15px; margin-left:10px' >
		
		<form id = "addform" onsubmit="invite();return false;" action="" target="">
			<input id="ia1" type="text"  accept-charset="UTF-8" name="input_create" size="23" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "New user id (#)..."/>
			<input id = "ia2" value="Add"  type="submit" class = "button3" >
		</form>
		
		<form id = "removeform" onsubmit="kick();return false;" action="" target="">
			<input id="ir1" type="text"  accept-charset="UTF-8" name="input_remove" size="23" style="background-color:rgb(71, 77, 83);color:white;border:none;padding:8px 10px;border-radius:8px;margin-top:10px; margin-left:22px" placeholder = "Removing user id (#)..."/>
			<input id = "ir2" value="Remove"  type="submit" class = "button3" >
		</form>	
			
	</div>
	
	<script>
		function invite(){
			postString("/invite "+encodeURIComponent(document.getElementById("ia1").value));
		}

		function kick(){
			postString("/kick "+encodeURIComponent(document.getElementById("ir1").value));
		}

		const x1 = document.getElementById('Add');
		x1.addEventListener("click", () => {
			document.getElementById("bottom_bar").style.top = "calc(100% - 90px)";
			document.getElementById("bottom_bar").style.width = "260px";
			document.getElementById("sidebar").style.height = "calc(100% - 260px)";
			document.getElementById("ir1").style.display="none";
			document.getElementById("ir2").style.display="none";
			document.getElementById("ia1").style.display="inline-block";
			document.getElementById("ia2").style.display="inline-block";
			}
		);
		
		const x2 = document.getElementById('Remove');
		x2.addEventListener("click", () => {
			document.getElementById("bottom_bar").style.top = "calc(100% - 90px)";
			document.getElementById("bottom_bar").style.width = "280px";
			document.getElementById("sidebar").style.height = "calc(100% - 260px)";
			document.getElementById("ir1").style.display="inline-block";
			document.getElementById("ir2").style.display="inline-block";
			document.getElementById("ia1").style.display="none";
			document.getElementById("ia2").style.display="none";
			}
		);
		const minimizeAdminButtons = () => {
			document.getElementById("bottom_bar").style.top = "calc(100% - 45px)";
			document.getElementById("bottom_bar").style.width = "210px";
			document.getElementById("sidebar").style.height = "calc(100% - 215px)";
			document.getElementById("ir1").style.display="none";
			document.getElementById("ir2").style.display="none";
			document.getElementById("ia1").style.display="none";
			document.getElementById("ia2").style.display="none";
		};
		function adminButtons(admin){
			let bottom_bar=document.getElementById("bottom_bar");
			if(admin && bottom_bar.hidden){
				let x3=document.getElementById("sidebar");
				x3.style.height = "calc(100% - 215px)";
				bottom_bar.removeAttribute("hidden");
				x3.addEventListener("click",minimizeAdminButtons);
			}else if(!admin && !bottom_bar.hidden){
				let x3=document.getElementById("sidebar");
				x3.style.height = "calc(100% - 170px)";
				bottom_bar.setAttribute("hidden",1);
				x3.removeEventListener("click",minimizeAdminButtons);
			}
		}
	</script>
	
	<div class = "fix-div">
	
	<%
	
	//TOP BAR
	
	out.print("<h1 style = \"color:rgb(255,255,255); font-size:15px; font-family:calibri, arial; text-align:right;position:relative;\"></style>");
	out.print("Hello <div id=\"profileName\" style=\"display:inline\">" + session.getAttribute("username").toString() + "</div>! | ");
	out.print("<a href="+PROFILE+"> Profile</a>&nbsp;| ");
	out.print("<a href="+MAIN_MENU+"> Home</a>&nbsp;| ");
	%><a <%=uid==3?"hidden=''":""%>id='logout_link' href=<%=LOGOUT%> > Log out</a><%
	%><a <%=uid!=3?"hidden=''":""%> id='login_link' href=<%=response.encodeURL("Login.jsp") %>>Log in</a> &nbsp;&nbsp;<%
	
	
	out.print("<style type=\"text/css\"> h1{color:rgb(255,255,255); text-align:left; font-size:15px}</style>");
	out.print("<img src=\"images/hydar.png\" alt=\"hydar\" width = \"25px\" height = \"40px\" align = \"center\" style =\"margin-right:10px\">");

	
	//out.print("&nbsp;&nbsp;&nbsp;Pick a board: ");
	%>
	<div style = "display:none;">
	<%
	//board selector(removed)
	out.printf("<form method=\"get\" action=%s>",HOMEPAGE);
	out.print("<select name=\"board\">" );
	out.print("<option value = \"" + board + "\"> ---</div>");
	
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
	<img id = "boardImage" class = "boardImage" src = "<%out.print("menuImages/" + boardImage);%>" width = 200px>
	<%
	
	
	
	
	//out.print("<li>Search posts: ");
	//out.print("<form method=\"get\" action=\"Homepage.jsp\">");
	//out.print("<input type=\"text\" accept-charset=\"UTF-8\" name=\"searchquery\" size=\"30\">");
	//out.print("<input value=\"Search\"  type=\"submit\"> </form> </li>");
	
	// TYPE MESSAGE BAR
	
	
	
	out.print("<style> .nav{text-align:center; font-family:calibri, arial; list-style-type:none; margin:0; padding:0} .nav li{color:rgb(255,255,255); display:inline-block;  font-size:20px;  position:relative; top:-12px;}</style>");
	out.print("<ul class=\"nav\"><li>");
	out.print("<form id = \"PostButton\" onsubmit=\"post();return false;\" accept-charset=\"UTF-8\" action=\"204.html\" target=\"\">");
	%>
	<script>
	function fileBrowser(){
		document.getElementById("fileElem").click();
	}
	</script>
	<%
	//if(parentBoard == -1){
	//	out.print("<div class = 'toptext' id = 'posting'>Posting in Channel: Main</div>");
	//}else{
		//if(readonly == 1 && isAdmin == 0){
			out.print("<div class = 'toptext' id = 'posting'>Viewing " + "" + " (read only)</div>");
		//}else{
		//	out.print("<div class = 'toptext' id = 'posting'>Posting in Channel: " + "" + "</div>");
		//}
	//}
	

		%>
		<div id = "postArea" style="display:inline">
		</div>
		<%
	
	
	// GREY TEXT BAR
	
		out.print("<style> p{color:LightSlateGrey; font-family:calibri, arial; text-align:center; font-size:15px; position:relative; top:5px;line-height:1px;}</style>");
		
		out.print("<p>Auto update posts: ");
		out.print("<style> #two{color:LightSlateGrey; font-family:calibri, arial; text-align:center; font-size:15px;}</style>");
		if(autoRefresh.equals("autoOff")){
			String encoded=response.encodeURL("\"Homepage.jsp?autoOn=autoOn&board="+board+"\"");
			out.print("<a id = \"two\" href="+encoded+">");
			out.print("Off</a>");
		}else{
			String encoded=response.encodeURL("\"Homepage.jsp?autoOn=autoOff&board="+board+"\"");
			out.print("<a id = \"two\" href="+encoded+">");
			out.print("On</a>");
		}
		out.print("&nbsp&nbsp|&nbsp&nbsp");
		out.print("<a id = \"two\" href=\"javascript:void(0);\">");
		out.print("Instant update</a>");
		out.print("</p>");
	%>
	</div>
	<div id="overlay" hidden=1></div>
	<div id="imageViewer" class="popup" hidden=1>
		<div id="imageViewerTopCaption">
			Viewing attachment: <a></a>
		</div>
		<a id="imageViewerNewTabCaption" target="_blank">Open in new tab</a>
		<a id="imageViewerDownloadCaption" >Download...</a>
	</div>
	<div class = "susRectangle" id = "susRectangle" hidden = true>
	
		<div class = "rectSUS" id = "rectSUS">
			<div class = "rectXButton" id = "rectXButton">
				<div class = "rectXButtonText">x</div>
			</div>
		</div>
		
		
		<div class = "rectSUS" id = "rectMaxSUS">
			<div class = "rectMaxButton" id = "rectMaxButton">
				<div class = "rectMaxButtonText">
					<img src = "images/ssfullscreen.png" width = 20 height = 20></img>
				</div>
			</div>
		</div>
		
		<div class = "rectSUS" id = "rectMinSUS" hidden=''>
			<div class = "rectMaxButton" id = "rectMinButton">
				<div class = "rectMaxButtonText">
					<img src = "images/ssminimize.png" width = 20 height = 20></img>
				</div>
			</div>
		</div>
		
	</div>
	
	<div class = "susRectangleSMALL" id = "susRectangleSMALL" hidden=''></div>
	<div class="margin" style="margin-top:140px">
	</form>
	</li>
	</ul>
	
	<!-- NEW MESSAGES  BAR -->
	
	<style> body{color:rgb(255,255,255); font-family:calibri, arial; text-align:left; font-size:15px;}</style>
		
	
	<div hidden='' id='bar' style='width: 100%; height: 1px; border-bottom: 2px solid #be4949; text-align: center'>
		
		<span style='color:White; font-size: 12px; display:block;line-height:8px;'><br>
			<b> - New Messages - </b>
		</span>
	</div>
	<div id="txtHint" style="line-height:25px;">Posts will be listed here...</div>
	<%
	// SHOW MESSAGES
	
	%>
	<div id = 'trash' style='display:inline'>
	</div>
	<div id = 'msgs' style='display:inline'>
	</div>
	<%//VC SCRIPT %>
	<script src="./board.js"></script>
	<script src="./homepage.js"></script>
	<%
	
	//AUTO RELOAD MESSAGES
	/**
	if(autoRefresh.equals("autoOn")){
		out.print("<script>");
		out.print("</script>");
	}else{
		out.print("<script>");
		out.print("</script>");
	}*/
	
	//conn.close();
	//LASTVISITED
	//MOVED TO ONCLOSE.jsp
	/**String updateLastVisitedS="UPDATE isin SET lastvisited = ? WHERE user = ? AND board = ?";
	ps = conn.prepareStatement(updateLastVisitedS);
	ps.setLong(1,System.currentTimeMillis());
	ps.setInt(2,uid);
	ps.setInt(3,board);
	int updateLastVisited = ps.executeUpdate();*/
} catch (Exception e) {
	out.print("<style> body{color:rgb(255,255,255); font-family:calibri, arial; text-align:center; font-size:20px;}</style>");
	out.print("<center>");
	out.print("A known error has occurred.\n");
	out.print("<br><br>");;
	out.print("<form method=\"post\" action=\""+response.encodeURL("Logout.jsp")+"\">");
	out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
	out.print("</form>");
	e.printStackTrace();
}
%>
</body>
</html>
