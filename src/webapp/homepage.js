let myHostname = window.location.hostname;
let timer=10;
let canDc=true;
if (!myHostname) {
  myHostname = "localhost";
}
let thisName=null;
let constraints = {
	audio: 
	{ 
		"autoGainControl": true,
		"echoCancellation": true,
		"noiseSuppression": true
	}
}
let connection=null;
let serverUrl;
let muted=false;
let pleaseRefresh=-1;
let userAudio=null;
let userVideo=null;
/**Add mic and local screenshare(if already active) to a peer connection*/
async function addLocalMedia(target){
	let stream = await getUserAudio();
	let tar=users.get(target);
	try {
		stream.getTracks().forEach(
			(track)=>{
			if(muted&&track.kind=="audio")
				track.enabled=false;
			tar.pc.addTrack(track, stream);}
		);
		if(userVideo)
			userVideo.getTracks().forEach(
			 (track)=>{
				if(muted&&track.kind=="audio")
					track.enabled=false;
			tar.pc.addTrack(track,stream);}
			);
	} catch(err) {
		console.dir(err);
	}
}
/**Obtain an audio stream - activated when joining VC(stream not sent until peers connect)*/
async function getUserAudio(){
	//todo: possibly close properly when leaving vc
	if(!userAudio||!userAudio.active)
		userAudio = await navigator.mediaDevices.getUserMedia(constraints);
	
	return userAudio;
}
/**Obtain screenshare stream - activated when clicking 'share screen'(stream not sent until peers connect or were already connected, but is previewable)*/
async function getDisplayVideo(){
	//todo: constraints.
	if(!userVideo||!userVideo.active){
		userVideo = await navigator.mediaDevices.getDisplayMedia({
			video:true,
			audio:true,
			frameRate: { max: 30 },
			width: {max: 1280 },
			height: {max: 720 }
		});
		
		document.getElementById("susRectangleSMALL").removeAttribute("hidden");
		let e=document.getElementById("stream_preview");
		if(!e){
			e = document.createElement("video");
			e.setAttribute("id","stream_preview");
			e.setAttribute("style","width: 100%;height: 100%;object-fit: contain;display: inline-block;");
			document.getElementById("susRectangleSMALL").append(e);
		}
		e.srcObject = userVideo;
		e.volume = 0;
		e.play();
	}
	return userVideo;
}
/**Stop the screen sharing stream and remove it from all peer connections*/
async function stopSharing(){
	if(userVideo)
		userVideo.getTracks().forEach(track=>track.stop());
	document.getElementById("susRectangleSMALL").setAttribute("hidden", true);
	var preview=document.getElementById("stream_preview");
	if(preview){
		document.getElementById("susRectangleSMALL").removeChild(preview);
	}
	document.getElementById("ssButton2").setAttribute("hidden", true);
	document.getElementById("ssButton").style.backgroundColor = "rgb(21, 27, 33)";
	document.getElementById("ssButton").removeAttribute("hidden");
	//stop transceivers that contain video
	getPeers()
		.flatMap(u=>u.pc.getTransceivers())
		.filter(t=>t.sender&&t.sender.track&&t.sender.track.kind=="video")
		.forEach(t=>t.stop());
	userVideo=null;
	
}
/**Obtain a screenshare stream and add it to peer connections(if any). Triggered by ss button.*/
async function shareScreen(){
	var stream = await getDisplayVideo();
	for(var target of getPeers()){
		console.log(target.vc);
		try {
			stream.getTracks().forEach((track)=>target.pc.addTrack(track,stream));
		} catch(err) {
			console.dir(err);
			console.log("couldnt add track");
		}
	}
	document.getElementById("ssButton").setAttribute("hidden", true);
	document.getElementById("ssButton2").style.backgroundColor = "rgb(201, 27, 33)";
	document.getElementById("ssButton2").removeAttribute("hidden");
}
/**Toggle deafened status*/
function deafen(){
	if(vcvolume>0){
		vcvolume=0;
		try{
			getPeers().forEach((x)=>document.getElementById("hydar_audio"+x.id).volume=0);
		}catch(e){}
	}else{
		vcvolume=vol() * 0.2 * vcvol();
		try{
			getPeers().forEach((x)=>document.getElementById("hydar_audio"+x.id).volume=vcvolume);
		}catch(e){}
	}
}
/**Toggle muted status*/
function mute(){
	muted=!muted;
	getPeers()
		.flatMap(conn=>conn.pc.getSenders())
		.filter(strm=>strm.track&&strm.track.kind=="audio")
		.forEach((strm)=>strm.track.enabled=!muted);
}
/**Join VC(alert signaling server) when clicking join button*/
function joinHandler(){
	if(canJoinVc){
		sendToServer("+");
		me.vc=true;
		getUserAudio().then(()=>
			[...users.values()]
				.filter(x=>x.vc && x!=me)
				.forEach((x)=>vc_invite(x.id))
		);
	}
}
/**Leave VC - alert signaling server if possible and end streams*/
function leaveVC(send=true){
	if(send)
		sendToServer("-");
	if(userAudio)
		userAudio.getTracks().forEach(track=>track.stop());
	document.getElementById("susRectangle").setAttribute("hidden","true");
	document.getElementById("susRectangleSMALL").setAttribute("hidden","true");
	//possibly close first
	userAudio=null;
	thisName=null;
	stopSharing();
	getPeers().forEach((x)=>closeVc(x.id));
	updateInfo();
}
let backOffTime=3;
let lastBackOff=3;
let reconnectInterval=null;
function tryReconnect(shouldWait){
	if(!reconnectInterval)return;
	clearInterval(reconnectInterval);
	reconnectInterval=null;
	document.getElementById("reconnect").innerText="...";
	if(!shouldWait){
		lastBackOff=backOffTime=3;
	}else{
		lastBackOff=lastBackOff<15?lastBackOff+3:(lastBackOff<300?lastBackOff*2:lastBackOff);
		backOffTime=lastBackOff;
	}
	makeSocket();
}
function startBackOff(){
	reconnectInterval = setInterval(()=>{
		backOffTime--;
		if(backOffTime<=0){
			tryReconnect(true);
		}else 
			document.getElementById("reconnect").innerText=backOffTime+(backOffTime==1?' second':' seconds');
	},1000);
}
/**WebSocket error handler - try to reconnect every 3 seconds*/
function dcHandler(){
	if(!canDc)
		return;
	canDc=false;
	clearInterval(vcInterval);
	clearInterval(pingInterval);
	let reconnect=document.querySelectorAll("[id='two']")[1];
	reconnect.innerHTML="<a style = 'color:Red'>Connection lost - retrying in <a style = 'color:Red' id='reconnect'>...</a></div>";
	reconnect.href="#";
	reconnect.onclick=()=>tryReconnect(false);
	try{leaveVC(false);}catch(e){
		//always continue
	}
	//pleaseRefresh=setTimeout(()=>document.querySelectorAll("[id='two']")[1].innerHTML="<a style = 'color:Red'><b id = 'reconnect'>Connection lost - Please Refresh<b></a>",5000);
	startBackOff();
	
	document.getElementById("VC-disconnect").removeEventListener("click",joinHandler);
	document.getElementById("VC-connect").removeEventListener("click",leaveVC);
	document.getElementById("VC-deafen").removeEventListener("click",deafen);
	document.getElementById("VC-undeafen").removeEventListener("click",deafen);
	
	document.getElementById("VC-mute").removeEventListener("click",mute);
	document.getElementById("VC-unmute").removeEventListener("click",mute);
}

function getPeers(){
	return [...users.values()].filter(u=>u.pc&&u!=me);
}
/**Wrapper for MediaStreamTrack::applyConstraints*/
async function apply(t,c) {
  await t.applyConstraints(Object.assign(track.getSettings(), c));
}
/**Send a WebSocket message(and log it)*/
function sendToServer(s){
	connection.send(s);
	console.log("OUT"+s.substring(0,s.indexOf("\n")));
}
/**Attempt to initialize the WebSocket connection*/
function makeSocket(){
	var scheme = document.location.protocol==="https:"?"wss":"ws";
	serverUrl = scheme + "://" + myHostname + ":"+document.location.port+"/Relay.jsp?";
	var id=new URLSearchParams(document.location.search).get("HYDAR_sessionID");
	if(id)serverUrl+="HYDAR_sessionID="+id+"&";
	serverUrl+="board="+boardId;
	console.log("Connecting to server: "+serverUrl);
	if(connection){
		connection.close();
		connection=null;
	}
	try{
		connection = new WebSocket(serverUrl);
	}catch(e1893){
		console.dir(e1893);
		dcHandler();
	}

	connection.onopen = function(_evt) {
		canDc=true;
		clearTimeout(pleaseRefresh);
		vcInterval=setInterval(updateVC,1000);
		pingInterval=setInterval(ping,8000);
		document.querySelectorAll("[id='two']")[1].innerHTML="<a style = 'color:Lime'>Connected</div></a>";
		lastBackOff=3;
		backOffTime=3;
		//document.getElementById("leaveVC").removeAttribute("hidden");
		//setInterval(()=>{sendToServer("hydar\n"+clientID+"\n"+<%out.print(board);%>+"\n")},2000);
		document.getElementById("VC-disconnect").addEventListener("click",joinHandler);
		document.getElementById("VC-connect").addEventListener("click",leaveVC);
		document.getElementById("VC-deafen").addEventListener("click",deafen);
		document.getElementById("VC-undeafen").addEventListener("click",deafen);
		
		document.getElementById("VC-mute").addEventListener("click",mute);
		document.getElementById("VC-unmute").addEventListener("click",mute);
	}
	/**On close/error try to leave VC and reconnect.*/
	connection.onclose=function(_evt){
		canDc=true;
		dcHandler();
	}
	connection.onerror = function(evt) {
		console.dir(evt);
	}
	/**Handle signaling packets.*/
	connection.onmessage = async function(evt) {
		timer=7;
		var q = evt.data.split(",");
		switch(q[0]){
			case 'X'://default message - update everything
				update(q);
				return;
			case 'N'://new message
				newMessage(q);
				return;
			case 'O'://vc offer
				var target = parseInt(q[1]);
				var sdp = q.slice(2).join(",");
				var desc = new RTCSessionDescription({"sdp":sdp,"type":"offer"});
				var usr=users.get(target);
				if(!usr.vc){
					usr.vc=true;
				}
				if(!usr.pc)
					usr.pc=await createPeerConnection(target);
				console.log ("- Setting remote description");
				console.log(desc);
				await usr.pc.setRemoteDescription(desc);
				await addLocalMedia(target);
				await usr.pc.setLocalDescription(await usr.pc.createAnswer());
				console.log("we made it to the end of offer thing, there was probably an error if this isn't here");
				sendToServer("A,"+target+","+usr.pc.localDescription.sdp);
				return;
			case 'V'://vc list
				/**if(me.vc){
					getPeers().forEach(u=>
						u.pc.get
						);
				}*/
				initUsers(JSON.parse(q.slice(1).join(",")),me.id,creator?creator.id:-1);
				updateInfo();
				return;
			case 'I'://new ICE candidate
				var target = parseInt(q[1]);
				var remoteMid=q[2];//b64 these maybe
				var remoteMLI=q[3];
				var sdp = q.slice(4).join(",");
				var candidate = new RTCIceCandidate({candidate:sdp,sdpMid:remoteMid,sdpMLineIndex:parseInt(remoteMLI)});
				try {
					 await users.get(target).pc.addIceCandidate(candidate);
				} catch(err) {
					console.log("ADD ICE ERROR");
					console.log(err); 
				}
				return;
			case 'A'://VC answer
				console.log("got vc answer");
				var target = parseInt(q[1]);
				var sdp = q.slice(2).join(",");
				var desc = new RTCSessionDescription({"sdp":sdp,"type":"answer"});
				await users.get(target).pc.setRemoteDescription(desc);//handle error maybe
				return;
			case '+'://user join
				//idk
				return;
			case '-'://user leave
				closeVc(parseInt(q[1]));
				return;
			case '>'://redirect
				var redir=parseInt(q[1]);
				var usp = new URLSearchParams(document.location.search);
				if(redir==0){
					document.location.replace("MainMenu.jsp?"+usp.toString());
				}else{
					usp.set("board",redir);
					document.location.replace("Homepage.jsp?"+usp.toString());
				}
				return;
			case '*':
				const ownerID=updateSettings(q);
				if(!me){
					window.location.pathname="MainMenu.jsp";
					return;
				}else if(me.username){
					if(!creator || creator.id!=ownerID){
						initUsers([...users.values()],me.id,ownerID);
					}
					updateInfo();
				}else if(!creator){
					creator={id:ownerID};
				}
				return;
			case ':':
				updateChannels(q[1]);
				updateInfo();
				return;
			case '@'://my id
				me={id:parseInt(q[1])};
				return;
			default:
				return;
		}
	}
}
makeSocket();
/**WebRTC invite - sent to all peers upon joining*/
async function vc_invite(target){
	console.log("Setting up connection to invite user: " + target);
	const tar=users.get(target);
	if (!tar.vc || tar&&tar.pc&&tar.active) {
		console.log("REJECTING (no invite)%%%");
		return;
	}
	await createPeerConnection(target);
	
	await addLocalMedia(target);
}
/**Relay an ICE candidate over the signaling server.*/
function handleICECandidateEvent(target, event) {
	//make sure candidate exists(the event is triggered with no candidate to indicate end of trickle ice)
	if(event.candidate){
		sendToServer("I,"+target+","+event.candidate.sdpMid+","+event.candidate.sdpMLineIndex+","+event.candidate.candidate);
	}
}
/**Closes VC when leaving it, or when disconnected(used by dcHandler).*/
async function closeVc(target){ 
	let tar=users.get(target);
	console.log("Closing the call");
	if(!tar||!tar.vc){
		return;
	}
	let thePC=tar.pc;
	if (thePC) {
		console.log("--> Closing the peer connection");
		tar.active=false;
		for(var s1 of thePC.getSenders())
			thePC.removeTrack(s1);
		thePC.ontrack = null;
		thePC.onnicecandidate = null;
		thePC.oniceconnectionstatechange = null;
		thePC.onsignalingstatechange = null;
		thePC.onicegatheringstatechange = null;
		thePC.onnotificationneeded = null;
		await thePC.close();
		tar.pc=null;
		//transceivers.splice(targets.indexOf(target),1);
		var remoteAudio = document.getElementById("hydar_audio"+target);
		if(remoteAudio)
			remoteAudio.remove();
		var remoteVideo = document.getElementById("hydar_video"+target);
		if(remoteVideo)
			remoteVideo.remove();
		try{
			stopWatching(target);
		}catch(e99){
			
		}
	}
}
/**Handle ICE connection failures. 'disconnected' is sometimes recoverable, but 'failed' is not.*/
function handleICEConnectionStateChangeEvent(target,_event) {
	console.log("*** ICE connection state changed for "+target);
	if(!users.get(target)||!users.get(target).vc)
		return;
	var thePC=users.get(target).pc;
	if(thePC)
		switch(thePC.iceConnectionState) {
			case "closed":
			case "failed":
				closeVc(target);
				break;
			case "disconnected":
				setTimeout(()=>{
					thePC=users.get(target).pc;
					if(thePC&&thePC.iceConnectionState=="disconnected"){
						thePC.restartIce();
					}
				},3000);
			break;
		}
}
/**Handle WebRTC signaling failures.*/
function handleSignalingStateChangeEvent(target,_event) {
	console.log("*** WebRTC signaling state changed to: " + users.get(target).pc.signalingState);
	switch(users.get(target).pc.signalingState) {
		case "closed":
			closeVc(target);//possible connection param
		break;
	}
}
/**Create WebRTC offer when negotiation is needed, and relay it.*/
async function handleNegotiationNeededEvent(target) {
	try {
		console.log("---> Creating offer");
		var thePC=users.get(target).pc;
		var offer = await thePC.createOffer();
		if (thePC.signalingState != "stable") {
			return;
		}
		console.log("---> Setting local description to the offer");
		await thePC.setLocalDescription(offer);
		sendToServer("O,"+target+","+thePC.localDescription.sdp);
		console.log("didnt create offer eee");
		
	} catch(err) {
		console.log("*** The following error occurred while handling the negotiationneeded event:\neeeeeeeee");
		console.dir(err);
	};
}
/**Shrink stream player*/
var smaller = false;
window.addEventListener('click', (evt)=>{
	const ele=document.getElementById("susRectangle");
	const overlay=document.getElementById("overlay");
	if(!ele.hasAttribute("hidden"))
	if(ele!=null && !ele.contains(evt.target)&&!overlay.contains(evt.target)){
		if(smaller == false){
			smaller = true;
			ele.style.top = "80%";
			ele.style.left = "80%";
			ele.style.marginLeft = "-208px";
			ele.style.marginTop = "-124px";
			ele.style.width = "416px";
			ele.style.height = "248px";
		}
	}
});
/**Enlarge stream player*/
function makebig(){
	const ele=document.getElementById("susRectangle");
	smaller = false;
	ele.style.top = "50%";
	ele.style.left = "50%";
	ele.style.marginLeft = "-540px";
	ele.style.marginTop = "-320px";
	ele.style.width = "1080px";
	ele.style.height = "640px";
	
}
/**Connect to an existing video stream, activating a video player.*/
function startWatching(target){
	stopWatchingAll();
	let thePC=users.get(target).pc;
	if(thePC){
		thePC.getReceivers().filter(x=>x.track.kind=="video").forEach(x=>{
			//x.track.enabled=true;
			x.enabled=true;
		});
	}
	let theVideo=document.getElementById("hydar_video"+target);
	if(theVideo){
		theVideo.style.display="inline-block";
	}
	document.getElementById("susRectangle").removeAttribute("hidden");
	setTimeout(makebig, 1);
	//makebig();
}
/**Stop watching all video streams and close all (non-preview) video players. Triggered by 'X' button*/
var stopWatchingAll=function(){
	getPeers().forEach(u=>stopWatching(u.id));
}
document.getElementById("rectXButton").addEventListener('click',stopWatchingAll);
document.getElementById("rectMaxButton").addEventListener('click',enterFullScreen);
document.getElementById("rectMinButton").addEventListener('click',enterFullScreen);
/**Stop watching a single video stream, closing the player for that target.*/
function stopWatching(target){
	let thePC=users.get(target).pc;
	if(thePC)
		thePC.getReceivers().filter(x=>x.track.kind=="video").forEach(x=>{
			//x.track.enabled=false;
			x.enabled=false;
		});
	
	let shouldHide=true;
	let videoE=document.getElementById("hydar_video"+target);
	if(videoE){
		//document.getElementById("hydar_video"+target).setAttribute("hidden","true");
		videoE.style.display="none";
	}
	for(var e of document.getElementById("susRectangle").children){
		if(e.getAttribute("class")!="rectSUS"&&e.style.display!="none")
			shouldHide=false;
	}
	if(shouldHide){
		if(document.fullscreenElement){
			document.exitFullscreen();
		}
		document.getElementById("susRectangle").setAttribute("hidden","true");
	}
	
}
/**Makes the video player container fullscreen. Uses workarounds to ensure cross-platform compatibility.*/
function enterFullScreen() {
	if(document.fullscreenElement){
		document.exitFullscreen();
		makeMaxButton();
		return;
	}
	makeMinButton();
	//var e=document.getElementById("hydar_video"+target);
	var e=document.getElementById("susRectangle");
	if (e.requestFullscreen) e.requestFullscreen();
	else if (e.webkitRequestFullscreen) e.webkitRequestFullscreen();
	else if (e.mozRequestFullScreen) e.mozRequestFullScreen();
	else if (e.msRequestFullscreen) e.msRequestFullscreen();
	else if (e.webkitEnterFullscreen) e.webkitEnterFullscreen();
}
/**Add a video or audio track from a peer, depending on what is stored in 'event'. This generally implies a successful connection.*/
function handleTrackEvent(target,event) {
	let tar=users.get(target);
	tar.active=true;
	let tracks=event.streams[0].getVideoTracks();
	console.log("*** Track event");
	
	if(tracks.length>0&&!tracks.includes(null)){
		let e=document.getElementById("hydar_video"+target);
		if(!e){
			e = document.createElement("video");
			e.setAttribute("id","hydar_video"+target);
			e.setAttribute("ondblclick","enterFullScreen()");
			e.setAttribute("style","width: 100%;height: 100%;object-fit: contain;display: inline-block;");
			document.getElementById("susRectangle").append(e);
		}e.srcObject = event.streams[0];
		console.log(event.streams);
		e.volume = vcvolume;
		e.play();
		tar.streaming=true;
		stopWatching(target);
	}
	//if(stream.getVideoTracks().length==0){
	else{
		users.get(target).streaming=false;
		//stopWatching(target);
		let e=document.getElementById("hydar_audio"+target);
		if(!e){
			e = document.createElement("audio");
			e.setAttribute("id","hydar_audio"+target);
			document.body.append(e);
		}
		e.srcObject = event.streams[0];
		e.volume = vcvolume;
		e.play();
	}
}
/**Wrapper for obtaining cookies(used for turn credentials mostly)*/
function getCookie(name) {
	var match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
	if (match) return match[2];
}
/**Initialize a peer connection for a specific peer.*/
function createPeerConnection(target) {
	return new Promise((resolve)=>{
		console.log("Setting up a connection...");
		var thePC=new RTCPeerConnection({
			iceServers: [ // Information about ICE servers - Use your own!
			{ urls: ["stun:"+myHostname+":3501"]},
				{ "urls": ["turn:"+myHostname+":3501"],
				"credential":getCookie("HYDAR_turnCred"),
				"username":getCookie("HYDAR_turnUser")
				}
				
				 // ,
				 // {urls: ["stun:stun3.l.google.com:19302"]}
			]
			//,
			//iceTransportPolicy : "relay" 
		});
		
		thePC.onicecandidate = (event)=>handleICECandidateEvent(target,event);
		thePC.oniceconnectionstatechange = (event)=>handleICEConnectionStateChangeEvent(target,event);
		thePC.onicegatheringstatechange = (_)=>{};
		thePC.onsignalingstatechange = (event)=>handleSignalingStateChangeEvent(target,event);
		thePC.onnegotiationneeded = ()=>handleNegotiationNeededEvent(target);
		thePC.ontrack = (event)=>handleTrackEvent(target, event);
		
		users.get(target).timer=3;
		users.get(target).pc = thePC;
		resolve(thePC);
	});
}
document.getElementById("ssButton").addEventListener('click', shareScreen);
document.getElementById("ssButton2").addEventListener('click', stopSharing);
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
var memberschannels = false;
var memberschannels2 = true;
if(isDm==1){
	memberschannels = true;
	members();
}else{
	viewChannels();
}
var showmemberslistener = document.getElementById('showMembers');
showmemberslistener.addEventListener("click", members);

var showchannellistener = document.getElementById('showChannels');
showchannellistener.addEventListener("click", viewChannels);

function members(){
	if(memberschannels){
		document.getElementById("showMembers").style.color = "rgb(230,230,230)";
		if(document.getElementById("memberslist"))
			document.getElementById("memberslist").removeAttribute("hidden");
		if(document.getElementById("channelslist"))
			document.getElementById("channelslist").setAttribute("hidden", true);
		document.getElementById("showChannels").style.color = "rgb(91, 97, 103)";
		document.getElementById("showChannels").style.scale = "1";
		memberschannels = false;
		memberschannels2 = true;
	}
}
function viewChannels(){
	if(!memberschannels){
		document.getElementById("showChannels").style.color = "rgb(230,230,230)";
		if(document.getElementById("channelslist"))
			document.getElementById("channelslist").removeAttribute("hidden");
		if(document.getElementById("memberslist"))
			document.getElementById("memberslist").setAttribute("hidden", true);
		document.getElementById("showMembers").style.color = "rgb(91, 97, 103)";
		document.getElementById("showMembers").style.scale = "1";
		memberschannels = true;
		memberschannels2 = false;
	}
}
const makeMinButton = () =>{
		document.getElementById("rectMaxSUS").setAttribute("hidden", true);
		document.getElementById("rectMinSUS").removeAttribute("hidden");
	}
	const makeMaxButton = () =>{
		document.getElementById("rectMinSUS").setAttribute("hidden", true);
		document.getElementById("rectMaxSUS").removeAttribute("hidden");
	}
	
	
	document.getElementById("susRectangle").addEventListener('click', makebig);