var messages=[];
var channels=[];
var channelof=-1;
var readonly=-1;
var isDm=-1;
var replyID = -1;
var users=new Map();
var allFiles = new Map();
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
const ATTACHMENT_PATH="/attachments/"
const DEFAULT_VOLS="50,50,50,0";
const MSGS = document.getElementById("msgs");
const fe=document.getElementById("fileElem");
const MAX_FILE_PER_MSG=8;
function probeType(x){
	let url = ATTACHMENT_PATH+x;
	if(!url.includes(".") || url.endsWith("."))
		return "file";
	switch(url.substring(url.lastIndexOf("."))){
		case ".png": case ".apng": case ".jpg": case ".jpeg": case ".gif": case ".tiff": case ".tif": case ".webp": case ".svg": case ".bmp": case ".avif":
			return "image";
		case ".mp4": case ".3gp": case ".flv": case ".webm": case ".mov": case ".avi": case ".wmv":
			return "video";
		case ".mp3": case ".ogg": case ".wav": case ".midi": case ".flac": case ".m4a": case ".aac":
			return "audio";
		default:
			return "file";
	}
}
function preview(x){//show big sus rectangle with thing
	let filename=x.substring(x.lastIndexOf('/')+1);
	let vwr=document.getElementById("imageViewer");
	let a = document.getElementById("imageViewerTopCaption").children[0];
	let a2 = document.getElementById("imageViewerNewTabCaption");
	let a3 = document.getElementById("imageViewerDownloadCaption");
	a.innerText=filename;
	fetch(ATTACHMENT_PATH+x,{method:"HEAD"}).then(response=>{
		console.log(response.headers.get("content-length"));
		a.innerText+=" - "+response.headers.get("content-type")+", "+formatSize(parseInt(response.headers.get("content-length")));
	});
	a.href=ATTACHMENT_PATH+x;
	a.target="_blank";
	
	
	a2.href=ATTACHMENT_PATH+x;
	a3.href=ATTACHMENT_PATH+x;
	a3.download=filename;
	vwr.hidden=0;
	document.getElementById("overlay").hidden=0;
	let type=probeType(x);
	if(type=="image"||type=="video"||type=="audio"){
		vwr.style.overflow="visible";
		let img = document.createElement(type=="image"?"img":type);
		if(type=="video"||type=="audio")
			img.setAttribute("controls","");
		img.src=ATTACHMENT_PATH+x;
		img.style.width="auto";
		img.style.display="block";
		img.style.height="auto";
		if(type=="image")
			img.onerror=()=>img.src="images/file.png";
		else img.autoplay=true;
		vwr.appendChild(img);
	}else{
		vwr.style.overflow="visible";
		let frame=document.createElement('iframe');
		frame.src=ATTACHMENT_PATH+x;
		frame.style.width="100%";
		frame.style.height="100%";
		frame.style.background="lightgray";
		vwr.appendChild(frame);
	}
	return false;
}
document.getElementById("overlay").addEventListener('click', (evt)=>{
	const ele=document.getElementById("imageViewer");
	if(!ele.hasAttribute("hidden")){
		if(ele!=null && !ele.contains(evt.target)){
			[...ele.querySelectorAll("img,audio,video,iframe")].forEach(x=>ele.removeChild(x));
			ele.setAttribute("hidden",1);
			document.getElementById("overlay").setAttribute("hidden",1);
		}
	}
});
function wrapFile(x){//html for a file
	//name2 = x.substring(0,x.lastIndexOf(".")).substring(0,Math.min(16,))
	let filename=x.substring(x.lastIndexOf('/')+1);
	let type=probeType(x);
	let playButton=(type=="video"||type=="audio"||x.endsWith(".gif"))?"<img class='play_button' src='images/play_button.png'>":"";
	switch(type){
		case "image":
		case "video":
			
			return `
			<a href='${ATTACHMENT_PATH+x}' onclick="return preview('${x}')">
				<div class='attGridSquare'">
					<div class='attGridName'>
						${filename}
					</div>
					<img onerror='this.onerror=null;this.src="/images/file.png"' src='${ATTACHMENT_PATH+x}.jpg'>
					${playButton}
				</div>
			</a>
			`;
		default:
			return `
			<a href='${ATTACHMENT_PATH+x}' download='${filename}' onclick="return preview('${x}')">
				<div class='attGridSquare'>
					<div class='attGridName'>
						${filename}
					</div>
					<img src='images/file.png'>
					${playButton}
				</div>
			</a>
			`;
	}
		/** 
	switch(probeType(x)){
		case "image":
			return `<img src='${ATTACHMENT_PATH+x}'></img><br>`;
		case "video":
			return `<video controls src='${ATTACHMENT_PATH+x}'></video><br>`;
		case "audio":
			return `<audio controls src='${ATTACHMENT_PATH+x}'></audio><br>`;
		default:
			return `<i>Attachment:</i> <a href='${ATTACHMENT_PATH+x}'>${x.split('/')[1]}</a><br>`;
	}*/
}
function wrapMessage(x){//generate html for a message element
	var user=users.get(x.uid);
	if(!user)
		user={pfp:"images/yeti.png",id:x.uid,username:"Banned User"}
	let fileLen = x.files&&x.files.length>1?"s ("+x.files.length+")":"";
	var html=`<div id = 'msg_${x.id}'>
	<img src = '${user.pfp.replaceAll("'","")}' alt='hydar' style="border-radius:40px;" width='40px' vspace='15' hspace='10' align='left'">
	<img id = 'reply_button${x.id}' class = 'reply_button' src = 'images/reply-arrow.png' width = 15px height=15px>
	<br><b><div class='msgUser' id = 'msgUser${x.id}'>${user.username}</div></b>
	<div hidden class = 'rectangle' id = 'rectangle${x.id}'>
	<img src = '${user.pfp}' alt='hydar' style='border-radius:60px' width='60px' vspace='10' hspace='10' height='60px' align='left'></img>
	<div class = 'rectangleText'><b>${user.username}<br>Id# - ${user.id}</b><br><a href = 'CreateBoard.jsp?input_dm=${user.id}'>Send direct message</a></div></div>
	<div id='three_${x.id}' class='three'>&nbsp;(just now): </div><br>
	<div class='msgBody' style='display:block'>
	<div class='msgText' id='msgText_${x.id}' data-tid='${x.transaction}' 
		style='opacity:${x.verified?1:0.5}'>${decodeURIPlus(x.message)}</div>
	${(x.files&&x.files.length)?`<b>Attachment${fileLen}:</b><br><div class='attGrid'>${x.files.map(wrapFile).join('')}</div>`:""}
	<br clear='left'>
	</div>
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
fe.onchange=()=>{
	try{
		if( !me || (me.username=="Guest" || me.username=="Anonymous")){
			return;
		}
		for(let file of fe.files){
			console.log(allFiles);
			rescaleImage(file).then(thumbnail=>{
				if(allFiles.size>=MAX_FILE_PER_MSG){
					return;
				}
				let target='/UploadFile.jsp?board='+boardId+"&filename="+file.name+"&thumbsize="+thumbnail.size;
				let newFile=new Blob([file,thumbnail],{"type":"application/octet-stream"});
				allFiles.set(file.name,{"prog":0,"file":file,"path":null});
				var posting=wrapPosting();
				test=document.getElementById("posting");
				if(test.innerHTML!=posting)
					test.innerHTML=posting;
				let request = new XMLHttpRequest();
			    request.upload.addEventListener('progress', function (e) {
					let attElem=document.getElementById('attachment_'+encodeURIComponent(file.name));
		            var percent = Math.round(e.loaded / (newFile.size/0.99) * 100);
		            allFiles.get(file.name).prog=percent;
		            if(attElem)
		           	 	attElem.children[0].innerHTML = percent + '%';
			    });    
			    request.addEventListener('loadend', function (e) {
					if(request.status==200){
						let attElem=document.getElementById('attachment_'+encodeURIComponent(file.name));
			            allFiles.get(file.name).prog=100;
						if(attElem)attElem.children[0].innerHTML = '100%';
						let text=request.responseText;
						
				        console.log(text);
				        allFiles.get(file.name).path=text;
				        //TODO: img preview and stuff
				        if(attElem)attElem.setAttribute("href",ATTACHMENT_PATH+text);
			        }else{
						allFiles.delete(file.name);
						try{
							document.getElementById("posting").removeChild(document.getElementById('attachment_'+encodeURIComponent(file.name)));
						}catch(e){}
						console.log(request.status);
					}
			    }); 
			    request.open('post', target);
			    request.timeout = 30000;
			    request.send(newFile);
			});
		}
	}catch(e){
		console.log(e);
	}finally{
		//document.getElementById('fileElem').files=[];
    	fe.value='';
	}
}
function videoToImg(file){
	if(probeType(file.name)=="image"){
		return new Promise((resolve,_)=>resolve(file));
	}else if(probeType(file.name)!="video"){
		return new Promise((resolve,_)=>resolve(null));
	}
	let video = document.createElement("video");
	let source = document.createElement("source");
	let ref=URL.createObjectURL(file);
	source.setAttribute('src', ref);
    video.appendChild(source);
	video.setAttribute('crossorigin', 'anonymous');
    video.setAttribute('preload', 'metadata');
	video.setAttribute('muted', 'true');
    video.style.display = 'none';
    document.body.appendChild(video);
	var p = new Promise((resolve,_)=>{
		video.currentTime = 0.001;
		video.load();
		video.addEventListener('loadedmetadata', function() {
	        video.oncanplay=function(){
				setTimeout(()=>{
					createImageBitmap(video).then(x=>{
						resolve(x);
						URL.revokeObjectURL(ref);
						video.remove();
					});
				},2000);
			}
		});
	});
	return p;
}
async function removeTransparent(file){
	return new Promise((resolve,_)=>{
		if(!file || file.length==0)
			resolve(file);
		let onImgLoad=function(img) {
			let canvas = newCanvas(img.width,img.height), ctx = canvas.getContext("2d");
			ctx.fillStyle="rgb(51, 57, 63)";
			ctx.fillRect(0,0,canvas.width,canvas.height);
			ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
			createImageBitmap(canvas).then(x=>{
				resolve(x);
				if(img.remove){
					img.remove();
					URL.revokeObjectURL(img.src);
				}
				if(canvas.remove)
					canvas.remove();
			});
		};
		workOnImageOrBlob(file,onImgLoad);
	});
}
function newCanvas(w,h){
	return OffscreenCanvas?new OffscreenCanvas(w,h):document.createElement('canvas');
}
function workOnImageOrBlob(file, onImgLoad){
	if(file instanceof Blob){
		let img = document.createElement("img");
		img.src=file instanceof Blob? URL.createObjectURL(file):file;
		img.onerror=()=>resolve(new Blob());
		img.addEventListener('load', _=>onImgLoad(img));
	}else{
		onImgLoad(file);
	}
}
async function rescaleImage(file_) {
	let width=100;
	return new Promise((resolve,_)=>{
		videoToImg(file_).then(removeTransparent).then(file=>{
			if(!file){
				resolve(file_);
				return;
			}
			let onImgLoad=function(img) {
				let canvas = newCanvas(width, width * img.height / img.width), ctx = canvas.getContext("2d");
				var cur = {
					width: Math.floor(img.width * 0.5),
					height: Math.floor(img.height * 0.5)
				};
				let oc = newCanvas(cur.width,cur.height), octx = oc.getContext('2d');
				octx.drawImage(img, 0, 0, cur.width, cur.height);
				while (cur.width * 0.5 > width) {
					cur = {
						width: Math.floor(cur.width * 0.5),
						height: Math.floor(cur.height * 0.5)
					};
					octx.drawImage(oc, 0, 0, cur.width * 2, cur.height * 2, 0, 0, cur.width, cur.height);
				}
				ctx.drawImage(oc, 0, 0, cur.width, cur.height, 0, 0, canvas.width, canvas.height);
				if(!OffscreenCanvas){
					canvas.toBlob(x=>{
						resolve(x);
						if(img.remove){
							img.remove();
							URL.revokeObjectURL(img.src);
						}
						oc.remove();
						canvas.remove();
					},"image/jpeg",0.25);
				}else{
					canvas.convertToBlob({type:"image/jpeg",quality:0.25}).then(resolve);
				}
			}
			workOnImageOrBlob(file,onImgLoad);
		});
	});
}
function formatSize(b){
	if(b<1024){
		return b+" B";
	}else if(b<1024**2){
		return Math.round(b/102.4)/10+" KB";
	}else if(b<1024**3){
		return Math.round(b/(1024**2/10))/10+" MB";
	}else{
		return Math.round(b/(1024**3/10))/10+" GB";
	}
}
var allFiles = new Map();
window.addEventListener('paste', e => {
  fe.files = e.clipboardData.files;
  fe.onchange();
});
document.body.addEventListener('dragover', (e) => {
    e.preventDefault()
});
document.body.addEventListener('drop', (e) => {
	console.log(e);
  fe.files = e.dataTransfer.files;
  fe.onchange();
  e.preventDefault();
});
function wrapPosting(){
	function label(){
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
	out=label();
	if(allFiles.size>0){
		out+=" | Files: ";
		for (let [_,f] of allFiles){
			link=f.path?`href='${ATTACHMENT_PATH+f.path}'`:"";
			out+=` <a ${link} target='_blank' id="attachment_${encodeURIComponent(f.file.name)}">${encodeURIComponent(f.file.name)}, ${formatSize(f.file.size)} - <b>${f.prog}%</b></a>`;
		}
	}
	return out;
}


function wrapPostArea(){
	//comment and substring thing is just to fix np++ formatting
	if(readonly==1&&!me.owner)return "";
	let att=(me&&me.id!=3&&me.username!="Anonymous")?`
	//`.substring(0,0)+`
	<div id = "attachmentButton" class = "attachmentButton">
		<img src="images/attachment.png" width=25px height=25px alt="paperclip" onclick="fileBrowser();">
		<input type="file" id="attach" hidden>
	</div>`:"";
	return `
	//`.substring(0,0)+`
	${att}
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
	if(toReply.length>64)
		toReply=toReply.substring(0,64)+"...";
	document.getElementById("reply_button"+m.id).addEventListener('click',()=>{
		document.getElementById("input_text").focus();
		if(replyID!=repliedID)
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
			test.style.display='inline'
			test.style.opacity=(creator.online?1:0.5);
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
	if(test.innerHTML.lastIndexOf('<')!=paStr.lastIndexOf('<'))
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
			let element=e.getRootNode().getElementById("three_"+id);
			element.textContent=tString;
			element.title=new Date(message.time).toLocaleString();
		}catch(e1893){
			
		}
	}
}
setInterval(updateTimestamps,10000);
function post(){
	const textbox=document.getElementById("input_text");
	var contents=textbox.value.replaceAll("\n", "<br>");
	if(contents.length==0&&!([...allFiles.values()].filter(x=>x.path).length))
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
	var toSend = [...allFiles.values()].filter(x=>x.path);
	for(let f of toSend){
		allFiles.delete(f.file.name);
	}
	var posting=wrapPosting();
		test=document.getElementById("posting");
		if(test.innerHTML!=posting)
			test.innerHTML=posting;
	var fileNameList=toSend.map(x=>x.path);
	var msg = {id:estId,message:x,uid:me.id,time:(Date.now()*1000),transaction:n,verified:false,files:fileNameList};
	//todo: make this less dumb
	messages.push(msg);
	
	if(messages.length>25)
		messages.splice(0,1);
	insertMessage(msg);
	
	sendToServer("N,"+n+","+replyID+","+fileNameList.join(';')+';'+","+x);
	//$.get(n+"?autoOn=autoOff&replyID="+replyID+"&input_text="+encodeURIComponent(document.forms[3].input_text.value)+"&board_num="+q).fail(function(){document.querySelectorAll("[id='two']")[1].innerHTML="Loading...</a>";});
}
document.addEventListener('keypress', (event)=>{
	if(event.key == 'Enter' && event.shiftKey== false){
		const textbox=document.getElementById("input_text");
		if(textbox.value.trim().length || (allFiles.size && [...allFiles.values()].filter(x=>x.path).length)){
			document.getElementById("PostButton").onsubmit();
			textbox.value="";
		}
		if(event.preventDefault)
			event.preventDefault();
		return false;
		//document.getElementById("PostButton").dispatchEvent(new Event("submit" ,{cancelable:false}))
	}
});