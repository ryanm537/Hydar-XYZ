var messages=[];
var channels=[];
var channelof=-1;
var readonly=-1;
var isDm=-1;
var replyID = -1;
var users=new Map();
var me=null;
var creator=null;
var vcvolume=-1;
var boardId=new URLSearchParams(window.location.search).get("board")
boardId=boardId?parseInt(boardId):1;
var boardName="";
var boardImage="";
var vcInterval=-1;
var pingInterval=-1;
const DEFAULT_USERNAME="Anonymous";
const DEFAULT_PFP="images/hydar2.png";
const DEFAULT_VOLS="50,50,50,0";
const MSGS = document.getElementById("msgs");
function wrapMessage(x){//generate html for a message element
	var user=users.get(x.uid);
	if(!user)
		user={pfp:"images/yeti.png",id:x.uid,username:"Banned User"}
		
	var html=`<div id = 'msg_${x.id}' style='display:inline,position:absolute'>
	<img src = '${user.pfp.replaceAll("'","")}' alt='hydar' style='border-radius:40px' width='40px' vspace='15' hspace='10' height='40px' align='left'>
	<img id = 'reply_button${x.id}' class = 'reply_button' src = 'images/reply-arrow.png' width = 15px height=15px>
	<br><b><div class='msgUser' id = 'msgUser${x.id}'>${user.username}</div></b>
	<div hidden class = 'rectangle' id = 'rectangle${x.id}'>
	<img src = '${user.pfp}' alt='hydar' style='border-radius:60px' width='60px' vspace='10' hspace='10' height='60px' align='left'></img>
	<div class = 'rectangleText'><b>${user.username}<br>Id# - ${user.id}</b><br><a href = 'CreateBoard.jsp?input_dm=${user.id}'>Send direct message</a></div></div>
	<div id='three_${x.id}' class='three'>&nbsp;(just now): </div><br>
	<div class='msgText' id='msgText_${x.id}' data-tid='${x.transaction}' 
		style='opacity:${x.verified?1:0.5}'>${decodeURIPlus(x.message)}</div>
	<br clear='left'>
	</div>`;
	
	return html;
}
function replaceID(e, newID){//replace ID of a message element, used before trashing/if id estimated wrong
	//(example: you send x and msg gets created with id 1100, someone posts that id in a different board
	//then id needs to be replaced when ungraying it
	var tmpId=parseInt(e.id.substring(e.id.indexOf('_')+1));
	var mt=document.getElementById("msgText_"+tmpId);
	var ts=document.getElementById("three_"+tmpId);
	var rep=document.getElementById("reply_button"+tmpId);
	mt.setAttribute("id","msgText_"+newID);
	ts.setAttribute("id","three_"+newID);
	rep.setAttribute("id","reply_button"+newID);
	e.setAttribute("id","msg_"+newID);
	for(m of messages){
		if(m.id==tmpId){
			m.id=newID;
			break;
		}
	}
}
function trash(e){//trash a message element
	console.log(e);
	var tmpId=parseInt(e.id.substring(e.id.indexOf('_')+1));
	var mt=document.getElementById("msgText_"+tmpId);
	var tid=mt.dataset.tid;
	var ts=document.getElementById("three_"+tmpId);
	var rep=document.getElementById("reply_button"+tmpId);
	e.removeChild(e.lastChild);
	//add retry and discard buttons
	var retry=document.createElement("div");
	retry.setAttribute("id","retry_"+tid);
	retry.setAttribute("style",'display:inline');
	retry.innerHTML="[Retry...]	";
	e.appendChild(retry);
	var discard=document.createElement("div");
	discard.setAttribute("id","discard_"+tid);
	discard.setAttribute("style",'display:inline');
	discard.innerHTML="[Discard...] ";
	var br = document.createElement("br");
	br.setAttribute("clear","left");
	e.appendChild(discard);
	e.appendChild(br);
	//make text red(we know it was grayed before)
	mt.setAttribute("style",mt.getAttribute("style").replace("opacity:0.5","opacity:1;color:Red"));
	mt.setAttribute("id","msgText_"+tid);
	mt.setAttribute("data-tid",tid);
	ts.setAttribute("id","three_"+tid);
	ts.innerHTML=" <i>(failed - please check your connection and try again)</i>"
	rep.setAttribute("id","reply_button"+tid);
	e.setAttribute("id","msg_"+tid);
	document.getElementById("trash").appendChild(e);
	
	if(document.getElementById("trash").children.length>5){
		document.getElementById("trash").removeChild(document.getElementById("trash").children[0]);
	}
	//add event listeners
	document.getElementById("retry_"+tid).addEventListener('click',()=>{
		postString(document.getElementById("msgText_"+tid).innerHTML);
		discardTrash(tid);
	});
	document.getElementById("discard_"+tid).addEventListener('click',()=>{
		discardTrash(tid);
	});
}
function discardTrash(t){//remove trash(t is transaction id)
	if(t==1||t=="1")
		return;
	var trash=document.getElementById("trash");
	[...trash.children]
		.filter(e=>e.id.split("_")[1]==t)
		.forEach(e=>trash.removeChild(e));
}
function replaceMessage(m){//given a message object, replace its element
	var ms=document.getElementById("msg_"+m.id);
	var mt=document.getElementById("msgText_"+m.id);
	var tid=mt?mt.dataset.tid:-1;
	var tmp=false;
	if(ms!=null&&m.transaction!=parseInt(tid)&&tid!="-1"
	&&window.getComputedStyle(mt).opacity==0.5){
		tmp=true;
		trash(ms);
	}
	if(tmp ||ms!=null){
		var isNew=window.getComputedStyle(mt).opacity==0.5;
		var tmpDiv=document.createElement("div");
		tmpDiv.innerHTML=decodeURIPlus(m.message);
		var diff=(mt.textContent!=tmpDiv.textContent)||(tmpDiv.children.length != mt.children.length);
		if(diff){
			//console.log("DIFF%%%");
			//console.log(mt.textContent);
			//console.log(tmpDiv.textContent);
		}
		if(isNew&&!diff){
			/**mt.setAttribute("style",
				mt.getAttribute("style")
					.replace("opacity:0.5","opacity:1")
				);*/
				if(tid=="-1"){
					mt.setAttribute("data-tid",m.transaction);
				}
				mt.removeAttribute("style");
			return;
		}
		else if(isNew || diff){
			console.log("Replacing "+m.id);
			//console.log(mt.innerHTML);
			//console.log(m.message);
				//	document.getElementById("msgs").removeChild(ms);
				//else
			if(!tmp)
				MSGS.removeChild(ms);
			insertMessage(m);
		}
	}
}
function lowestId(){//lowest id on page
	var min=Infinity;
	for(e of MSGS.children){
		if(parseInt(e.id.substring(e.id.indexOf('_')+1))<min)
			min=parseInt(e.id.substring(e.id.indexOf('_')+1));
	}
	return min;
}
function ping(){//hydar hydar hydar
	sendToServer("]");
}
function wrapPosting(){
	if(channelof == -1){
		return "Posting in Channel: Main";
	}else{
		if(readonly == 1 && !me.owner){
			return "Viewing " + boardName + " (read only)";
		}else{
			return "Posting in Channel: " + boardName;
		}
	}
}



function wrapPostArea(){
	//comment and substring thing is just to fix np++ formatting
	if(readonly==1&&!me.owner)return "";
	else return `

	//`.substring(0,0)+`<div id = "attachmentButton" class = "attachmentButton"><img src="images/attachment.png" width=25px height=25px onclick="fileBrowser();"><input type="file" id="attach" hidden></div>
	<style>#input_text::-webkit-scrollbar {display: none;}</style>
	<textarea id="input_text" 
				type="text" 
				name="input_text" 
				size="80" 
				rows = "1" 
				cols = "60" 
				value = "hydar" 
				placeholder = "Enter text to post..." 
				autofocus="autofocus" 
				onfocus="this.select()"></textarea>
	<input value="${boardId}"  type="hidden" name="board_num">
	<input value="  Post  "  type="submit" class = "button" >`;
}

function insertMessage(m){//given message object, add a new element
	var inserted=false;
	discardTrash(m.transaction);
	//find the place to insert it(before the msg with highest id lower than it)
	var low = lowestId();
	for(var i = m.id;i>=low;i--){
		var msg=document.getElementById("msg_"+i);
		if(msg){
			inserted=true;
			msg.insertAdjacentHTML("beforebegin", wrapMessage(m));
			if(MSGS.children.length>25&&i!=low){
				MSGS.removeChild(document.getElementById("msg_"+low));
			}
			break;
		}//if(i<m.id-25&&i>0)
		//	break;
		
		
	}
	//probably the first message
	if(!inserted){
		MSGS.insertAdjacentHTML("beforeend", wrapMessage(m));
	}
	//add reply button event listener
	var toReply="";
	var repliedName;
	if(!users.get(m.uid))
		repliedName="Banned User"
	else
		repliedName=users.get(m.uid).username;
	var repliedID=m.id;
	var element = document.getElementById("msgText_"+m.id);
	if(element.children.length>0&&element.children[0].id.startsWith("actualContents")){
		var cont = document.getElementById("actualContents"+m.id);
		if(!cont.innerHTML.includes("https://www.youtube.com/embed/") 
				&& !cont.innerHTML.includes("<a href") 
				&& !cont.innerHTML.includes("<img src")){
			
			toReply = cont.innerHTML;
			
		}else{
			
			toReply = " ";
			
		}
		
	}else{
		if(!element.innerHTML.includes("https://www.youtube.com/embed/") 
				&& !element.innerHTML.includes("<a href") 
				&& !element.innerHTML.includes("<img src")){
			
			toReply = element.innerHTML;
			
		}else{
			
			toReply = " ";
			
		}
	}
	document.getElementById("reply_button"+m.id).addEventListener('click',()=>{
		document.getElementById("input_text").focus();
		document.getElementById("input_text").value = "Replying to "+repliedName+" "+toReply+": "+document.getElementById("input_text").value;
		replyID=repliedID;
	});
	let rectangle = false;
	if(!rectangle){
		document.getElementById("msgUser"+m.id).addEventListener('click',()=>{
			document.getElementById("rectangle"+m.id).removeAttribute("hidden");
			rectangle = true;
			
		});
	}
	window.addEventListener('click', (susrectangle)=>{ 
		var rectElement=document.getElementById("rectangle" + m.id);
		if(rectElement!=null && !rectElement.contains(susrectangle.target) && !document.getElementById("msgUser" + m.id).contains(susrectangle.target)){
			rectangle = false;
			rectElement.setAttribute("hidden", true);
		}
	});
	
}

function wrapMembers(){//HTML string of "messages" element
	var uString="";
	if(boardId<=3)
		return uString;
	[...users.values()].filter(u=>!u.owner).sort(u=>!u.online).forEach(u=>{
		uString+="<div id='member_"+u.id+"' style='display:inline; opacity:"+(u.online?1:0.5)+"'>";
		if(me.owner){
			uString+="&nbsp"+u.username + " #" + u.id + "<br>";	
		}else{
			uString+="&nbsp"+u.username + "<br>";
		}
		uString+="</div>";
	});
	return uString;
}
function wrapChannels(){
	const usp = new URLSearchParams(document.location.search);
	var hs="";
	if(isDm == 0){
		if(channelof != -1){
			usp.set("board",channelof);
			hs+="<a href = 'Homepage.jsp?"+usp.toString()+ "'>Main</a><br>";
		}else{
			hs+="<a href = '#'>Main</a><br>";
		}
	}
	channels.forEach(x=>{
		usp.set("board",x.id);
		hs+="<a href = 'Homepage.jsp?" +usp.toString() + "'>" + x.name.replaceAll("<","&lt;").replaceAll(">","&gt;") + "</a><br>";
	});
	return hs;
}
function updateInfo(){//update general board info(things other than msgs p much)
	var test;
	if(creator){
		test=document.getElementById("boardCreator");
		if(test){
			test.setAttribute("style","display:inline;opacity:"+(creator.online?1:0.5));
			if(test.innerHTML!=creator.username){
				test.innerHTML=creator.username;
			}
		}
	}
	
	test=document.getElementById("boardInfo");
	if(test.innerHTML!=boardName+" (#"+boardId+")")
		test.innerHTML=boardName+" (#"+boardId+")";
		
	test=document.getElementById("boardImage");
	if(test.getAttribute('src')!=boardImage)
		test.setAttribute('src',boardImage);
		
	test=document.getElementById("members");
	if(test){
		var mb=wrapMembers();
		if(test.innerHTML!=mb)
			test.innerHTML=mb;
	}
	
	test=document.getElementById("profileName");
	if(me && test.innerHTML!=me.username)
		test.innerHTML=me.username;
	let inlink=document.getElementById("login_link");
	let outlink=document.getElementById("logout_link");
	if(me && me.id!=3){
		inlink.setAttribute("hidden","");
		outlink.removeAttribute("hidden");
	}else{
		outlink.setAttribute("hidden","");
		inlink.removeAttribute("hidden");
	}
	
	var chStr=wrapChannels();
	var paStr=wrapPostArea();
	var posting=wrapPosting();
	
	test=document.getElementById("channelslist");
	if(test&&test.innerHTML!=chStr){
		test.innerHTML=chStr;
	}
	if(chStr.length==0){
		members();
	}
	test=document.getElementById("postArea");
	if(test.innerHTML.indexOf('<')!=paStr.indexOf('<'))
		test.innerHTML=paStr;
		
	test=document.getElementById("posting");
	if(test.innerHTML!=posting)
		test.innerHTML=posting;
	updateVC();
}

function wrapVC(){
	var hstr="";
	for(var [_,t] of users){
		if(!t.vc||t==me)
			continue;
		var tAlive=false;
		var transportStates=[];
		if(t&&t.pc)
			transportStates=t.pc.getSenders()
				.filter(x=>x.transport)
				.map(x=>x.transport.state);
				
		if(!t||!t.active||!t.pc||transportStates.includes("failed"))
			hstr+=t.username+"<div style='display:inline;color:rgb(255,0,0)'></div><br>";
		else if(!t.pc||t.pc.iceConnectionState!="connected")
			hstr+=t.username+"<div style='display:inline;color:rgb(255,128,0)'> connecting...</div><br>";
		else if(transportStates.includes("connecting"))
			hstr+=t.username+"<div style='display:inline;color:rgb(255,255,0)'> encrypting...</div><br>";
		else if(t.active){
			tAlive=true;
			t.timer=3;
			hstr+=t.username+"<div style='display:inline;color:rgb(0,255,0)'> (connected)</div>"+"<br>";
			if(t.streaming){
				hstr+="<div style='display:inline;color:rgb(255,255,255)' id='watch_"+t.id+"'>&nbsp;&nbsp;&nbsp;<susrunes style = 'color:rgb(255,0,0)'><b>LIVE</b></susrunes>&nbsp;&nbsp;<a href=#>[Watch Stream]</a></div>";
				hstr+="<br>";
			}
		}if(!tAlive&&t.timer<0){
			t.timer=3;
			if(t.pc)
				t.pc.restartIce();
		}
	}
	if(me && me.username && me.vc){
		hstr+=me.username+"<div style='display:inline;color:rgb(0,255,255)'> (you)</div><br>";
	}
	if(me && me.id==3)
		hstr+="<i>You need to log in to join VC!</i>"
	return hstr;
}
function updateVC(){
	
	var hstr = wrapVC();
	
	//stop local video if it isnt active
	if(userVideo&&!userVideo.active){
		//userVideo.getTracks().filter(x=>x.kind=="video").forEach(s=>
		
		//);
		stopSharing();
	}
	const preview=document.getElementById("stream_preview");
	if(preview){
		if((!document.hasFocus())||idle>15){
			//document.getElementById("stream_preview").style.display="none";
			preview.pause();
		}else{
			preview.play();
		}
	}
	if(document.getElementById("vcList").innerHTML!=hstr)
		document.getElementById("vcList").innerHTML=hstr;
	if(vcvolume!=0) 
		vcvolume=vol() * 0.2 * vcvol();
	for(var [_,u] of users){
		if(u.pc&&u!=me){
			if(!u.pc.getReceivers().find(s=>s.track && s.track.kind=="video")){
				u.streaming=false;
				stopWatching(u.id);
			}else if(u.streaming){
				const copy=u.id;
				var watch=document.getElementById("watch_"+copy);
				if(watch)
					watch.onclick=()=>startWatching(copy);
			}
		}
		
	}
	if(me && me.vc==false&&getPeers().length){
		leaveVC();
	}
}
//replace a user but not their peer connection basically
function vol(){
	return me.vols?me.vols.split(",")[0]/100:0;
}
function vcvol(){
	return me.vols?me.vols.split(",")[1]/50:0;
}
function pingvol(){
	return me.vols?me.vols.split(",")[2]/50:0;
}
function pings(){
	return me.vols?me.vols.split(",")[3]:0;
}
function initUsers(list, myId=me.id, ownerID=-1){
	var setMe=false;
	list=list.map(u=>u.id==null?{"id":u}:u);
	for(var u of list){
		u.username=!u.username?DEFAULT_USERNAME:decodeURIPlus(u.username);
		u.pfp=!u.pfp?DEFAULT_PFP:decodeURIPlus(u.pfp);
		if(!u.vols)
			u.vols=DEFAULT_VOLS;
		//nullables to boolean
		u.online=!!u.online;
		u.vc=!!u.vc;
		if(users.has(u.id)){
			if(!u.vc)
				u.streaming=false;
			Object.assign(users.get(u.id),u)
		}else
			users.set(u.id,u);
	}
	for(var [_,u] of users){
		if(u.id==myId){
			setMe=true;
			me=u;
		}else if(u.id==2)
			u.online=true;
		u.owner=(u.id==ownerID);
		if(u.owner){
			creator=u;
		}
		if(!list.find(x=>x.id==u.id)){
			closeVc(u.id);
			users.delete(u.id);
		}
	}
	
	if(!setMe){
		document.location.replace("MainMenu.jsp?"+new URLSearchParams(document.location.search).toString());
		return;
	}
}function updateSettings(cmd){
	boardId=parseInt(cmd[1]);
	
	const ownerID=parseInt(cmd[2]);
	boardName=decodeURIPlus(cmd[3]);
	boardImage="menuImages/"+decodeURIPlus(cmd[4]);
	channelof=parseInt(cmd[5]);
	readonly=parseInt(cmd[6]);
	isDm=parseInt(cmd[7]);
	return ownerID;
}function updateChannels(cmd){
	channels =cmd.split(";")
		.filter(x=>x.length>0)
		.map(x=>x.split(":"))	
		.map(x=>({"id":x[0],"name":decodeURIPlus(x[1])}));
}
function update(cmd){
	//parse board name/icon/... from server message
	//channels :skull
	messages = JSON.parse(cmd.slice(1));
	messages.filter(x=>!x.transaction).forEach(x=>x.transaction=1);
	for(var m of messages){
		m.verified=true;
		//document.getElementById("msg_"+m.transaction)||
		if(document.getElementById("msg_"+m.id)){
			replaceMessage(m);
		}else{
			insertMessage(m);
		}
	}
	for(var e of MSGS.children){
		var tmpId=parseInt(e.id.substring(e.id.indexOf('_')+1));
		if(!messages.find(x=>x.id==tmpId)&&tmpId!="-1"&&tmpId!=1){
			if(window.getComputedStyle(document.getElementById("msgText_"+tmpId)).opacity==0.5){
				
				trash(e);
			}else MSGS.removeChild(e);
			
		}
	}
	updateTimestamps();
	canJoinVc=true;
}
function forId(j){
	return messages.find(m=>m.id==j) || -1;
}
function forIndex(j){
	var ret=-1;
	messages.some((m,i)=>{if(m.id==j){ret=i;return true;}return false;});
	return ret;
}
function newMessage(cmd){//new message from server(different packet from update)
	var message = JSON.parse(cmd.slice(1).join(","));
	//
	var oldId=-1;
	var lastId=-1;
	for(var e of MSGS.children){
		var tmpId=parseInt(e.id.substring(e.id.indexOf('_')+1));
		if(window.getComputedStyle(e.getElementsByClassName("msgText")[0]).opacity==1){
			lastId=tmpId;
			break;
		}
	}
	for(var i=Math.min(message.id,lastId+50);i>=lastId;i--){
		var mt=document.getElementById("msgText_"+i);
		if(mt&&mt.dataset.tid&&message.transaction!=0){
			var q = parseInt(mt.dataset.tid);
			console.log("%%%"+q+"%%%"+message.transaction);
			if(q==message.transaction){
				oldId = i;
				break;
			}
		}
	}
	if(oldId!=-1){
		replaceID(document.getElementById("msg_"+oldId),message.id);
	}
	var index = forIndex(message.id);
	if(index==-1&&oldId==-1){
		messages.push(message);
		message.verified=true;
		if(messages.length>25)
			 messages.splice(0,1);
		insertMessage(message);
	}else{
		message.verified=true;
		
		messages[index]=message;
		replaceMessage(message);
		index = forIndex(message.id);
	}
	if((!document.hasFocus()||idle>14)&&me.id!=message.uid){
		try{//.replaceAll("\"\"","\"").replaceAll("\\\\","\\")
			let sender=users.get(message.uid);
			h=new Notification(sender.username,{body:document.getElementById("msgText_"+message.id).innerText,icon:sender.pfp});
			var pingSound = new Audio("audio/ping.mp3");
			pingSound.volume = vol()*0.2*pingvol();
			if(pings())pingSound.play();
		}catch(e){

		}
		document.querySelector("link[rel*='icon']").href = "favicon2.ico";
		document.getElementById("bar").removeAttribute("hidden");
	}
}
function decodeURIPlus(x){
	return [...new URLSearchParams(x).keys()].join()
}
function decode(x){
	var a=atob(x);
	return JSON.parse(a);
}

var api;
var idle = 0;
document.addEventListener('click',()=>{
	idle=0;document.querySelector("link[rel*='icon']").href = "favicon.ico";
	document.getElementById("bar").setAttribute("hidden",true);
});
document.addEventListener('hover',()=>{idle=0;});
document.addEventListener('keypress',()=>{idle=0;});
document.addEventListener('mousemove',()=>{idle=0;});
function updateTimestamps(){
	var now = Date.now()/1000;
	
	for(var e of MSGS.children){
		var id=parseInt(e.id.substring(4));
		var message =forId(id);
		
		if(!message.verified)//might indicate a greater problem...
			return;
		try{
			var dt = now-message.time/1000;
			var tString="";
			//\u00a0 is &nbsp;
			if(dt/3600>=2){
				tString+="\u00a0("+Math.floor(dt/3600)+" hours ago):";
			}else if(dt/3600>=1){
				tString+="\u00a0("+Math.floor(dt/3600)+" hour ago):";
			}else if(dt/60>=2){
				tString+="\u00a0("+Math.floor(dt/60)+" minutes ago):";
			}else if(dt/60>=1){
				tString+="\u00a0("+Math.floor(dt/60)+" minute ago):";
			}else{
				tString+="\u00a0(just now):";
			}
			e.getRootNode().getElementById("three_"+id).textContent=tString;
		}catch(e1893){
			
		}
	}
}
setInterval(updateTimestamps,10000);
function post(){
	const textbox=document.getElementById("input_text");
	var contents=textbox.value.replaceAll("\n", "<br>");
	if(contents.length==0)
		return;
	postString(contents);
	textbox.value="";
	textbox.focus();
	replyID=-1;
}

var callbacks=0;
	setInterval(()=>{
		try{
			callbacks=0;
			document.forms[3].input_text.disabled=false;
			document.forms[3].input_text.readOnly=false;
			document.forms[3].input_text.placeholder="Enter text to post...";
		}catch(e1893){
			
		}
},5000);

function postString(x){
	callbacks++;
	if(callbacks>20){
		document.forms[3].input_text.disabled=true;
		document.forms[3].input_text.readOnly=true;
		document.forms[3].input_text.placeholder="processing...";
		return;
	}var n=1000000000+Math.floor(Math.random()*1000000000);
	var estId=0;
	if(MSGS.children.length>0)
		estId=parseInt(MSGS.children[0].id.substring(4))+1;
	
	var msg = {id:estId,message:x,uid:me.id,time:(Date.now()*1000),transaction:n,verified:false};
	//todo: make this less dumb
	messages.push(msg);
	
	if(messages.length>25)
		messages.splice(0,1);
	insertMessage(msg);
	
	sendToServer("N,"+n+","+replyID+","+x);
	//$.get(n+"?autoOn=autoOff&replyID="+replyID+"&input_text="+encodeURIComponent(document.forms[3].input_text.value)+"&board_num="+q).fail(function(){document.querySelectorAll("[id='two']")[1].innerHTML="Loading...</a>";});
}
function insertScript(script, toReply, func){
	const el=document.getElementById("extraLoader_"+toReply);
	if(!el)
		return;
	el.onload=null;
	const targetE="msgText_"+toReply;
	function appendScript(){
		document.getElementById("p5").setAttribute("loaded",'true');
		if(!document.getElementById(targetE))
			return;
		if(!window[func]){
			var botScript=document.getElementById("bot_"+func);
			if(!botScript){
				botScript=document.createElement("script");
				botScript.setAttribute("id", "bot_"+func);
				//botScript.setAttribute("async","");
				botScript.setAttribute("src",script);
				document.head.appendChild(botScript);
			}
			var newP5=()=>{
				new p5(window[func],targetE);
				botScript.removeEventListener('load',newP5);
			};
			botScript.addEventListener('load',newP5);
		}else new p5(window[func],targetE);
	}
	var p5e=document.getElementById("p5");
	if(!p5e){
		p5e=document.createElement("script");
		p5e.setAttribute("id", "p5");
		//p5.setAttribute("async","");
		p5e.setAttribute("src", "p5.min.js");
		
		document.head.appendChild(p5e);
		p5e.addEventListener('load',appendScript);
		
	}else if(!p5e.getAttribute("loaded")){
		p5e.addEventListener('load',appendScript);
	}
	else{
		appendScript();
	}
}
document.addEventListener('keypress', (event)=>{
	if(event.key == 'Enter' && event.shiftKey== false){
		const textbox=document.getElementById("input_text");
		if(textbox.value.trim().length){
			document.getElementById("PostButton").onsubmit();
			textbox.value="";
		}
		if(event.preventDefault)
			event.preventDefault();
		return false;
		//document.getElementById("PostButton").dispatchEvent(new Event("submit" ,{cancelable:false}))
	}
});