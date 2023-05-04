package xyz.hydar.app;
import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import xyz.hydar.ee.HydarEE;
import xyz.hydar.ee.HydarWS;
import xyz.hydar.ee.HydarEE.HttpServletRequest;
import xyz.hydar.ee.HydarEE.HttpServletResponse;
import xyz.hydar.ee.HydarEE.HttpSession;

public class HydarEndpoint extends HydarWS.Endpoint{
	int id;
	Board board;
	HydarEE.HttpSession session;
	public static Map<Integer,Board> boards = new ConcurrentHashMap<>();//board id => board
	static volatile String PYTHON_PATH=null;
	boolean vc=false;
	public HydarEndpoint(HydarWS websocket) {
		super(websocket);
		
	}
	@Override
	public void onOpen() throws IOException{
		this.session=getSession();
		Integer tmp=(Integer)session.getAttribute("userid");
		if(tmp==null) {
			close();
			return;
		} 
		if(Board.REFRESH_TIMER==-1) {
			String refr=session.getServletContext().getInitParameter("BOARD_REFRESH_DELAY");
			Board.REFRESH_TIMER=refr==null?45000:Long.parseLong(refr);
		}
		if(PYTHON_PATH==null){
			String path=session.getServletContext().getInitParameter("PYTHON_PATH");
			PYTHON_PATH=path==null?"python":path;
		}
		//TODO: better way to do this
		HttpServletRequest req = new HttpServletRequest("PermCheck", search+"&last_id=0")
				.withAddr(new InetSocketAddress(getRemoteAddress(),0))
				.withSession(this.session,true);
		HydarEE.HttpServletResponse ret = HydarEE.jsp_invoke(req);
		//only guests have ip attr
		if(tmp==3) {
			session.setAttribute("ip",getRemoteAddress());
		}else
			session.removeAttribute("ip");
		if(ret.getStatus()>=300){
			print(">,0");
			close();
			return;
		}
		print("@,"+tmp);
		this.id = tmp;
		
		int boardNum = parseInt(search.substring(search.indexOf("board=")+6));
		board = HydarEndpoint.boards.computeIfAbsent(boardNum,Board::new);
		board.addUser(this);
	}
	public boolean isGuest(){
		return id==3;
	}
	@Override
	public void onClose() {
		if(board!=null) {
			board.dropUser(this, false);//already being closed
			HydarEE.jsp_invoke("OnClose",session,"board="+board.boardId);
		}
	}

	@Override
	public void onMessage(String message) throws IOException{
		if(!message.startsWith("]"))
			System.out.write(("WS: uid="+this.id+",length="+message.length()+">>"+message.substring(0,Math.min(message.length(),50))+"\n").getBytes(UTF_8));
		Integer tmp=(Integer)session.getAttribute("userid");
		if(tmp==null || (this.id!=tmp)) {
			close();
			return;
		}
		this.id = tmp;
		String[] split=message.split(",",2);
		String type=split[0];
		if(split.length==2)
			message=split[1];
		switch(type){  
			case "N":
				board.processInput(message,id,session);
				break;
			case "+":
				if(isGuest()) {
					close();//TODO: custom msg
					return;
				}
				this.vc=true;
				board.addVc(id);
				break;
			case "-":
				if(isGuest()) {
					close();//TODO: custom msg
					return;
				}
				this.vc=false;
				board.dropVc(id);
				break;
			case "O":
			case "I":
			case "A":
				if(isGuest()) {
					close();//TODO: custom msg
					return;
				}
				split=message.split(",",2);
				if(split.length==2)
					message=split[1];
				else return;
				int targetId = parseInt(split[0]);
				message=split[1];
				if(!board.members.get(targetId).vc) {
					return;
				}board.writeTo(targetId,type+","+id+","+message);
				break;
			case "]":
				websocket.ping=15;
				break;
		}
	}

}

class Message{
	public final int id;
	public final int uid;
	public final long time;
	public final String message;
	public final int transaction;
	//reply thing maybe.
	public Message(int id, int uid, long time, String message, int transaction){
		this.id=id;
		this.uid=uid;
		this.time=time;
		this.message=message;
		this.transaction=transaction;
	}
	private String toString(boolean includeTID) {
		int length=message.length();
		var x= new StringBuilder(128+length)
			.append("{\"id\":").append(id)
			.append(",\"uid\":").append(uid)
			.append(",\"time\":").append(time)
			.append(",\"message\":\"").append(URLEncoder.encode(message,UTF_8));
		if(includeTID&&transaction!=1)
			x.append("\",\"transaction\":").append(this.transaction);
		else x.append("\"");
		return x.append("}").toString();
		
	}
	@Override
	public String toString(){
		return toString(true);
	}
}

class Member{
	static final String DEFAULT_USERNAME="Anonymous";
	static final String DEFAULT_PFP="images/yeti.png";
	static final String DEFAULT_VOLS="50,50,50,0";
	public final int id;
	public final String username;
	public final String pfp;
	//public ServerThread thread;//null if dead
	public volatile boolean vc;
	public volatile boolean online;
	public final String perms;
	public final String vols;
	public final boolean owner;
	//reply thing maybe.
	public Member(int id, String username, String pfp, String perms, String vols,boolean owner){
		this.id=id;
		this.username=username.isEmpty()?DEFAULT_USERNAME:username;
		this.pfp=pfp.isEmpty()?DEFAULT_PFP:pfp;
		this.online=false;
		this.vc=false;
		this.perms=perms;
		this.owner=owner;
		this.vols=vols;
	}
	@Override
	public String toString(){
		var sb= new StringBuilder(16);
		sb.append("{");
			if(!DEFAULT_USERNAME.equals(username))
				sb.append("\"username\":\"").append(username).append("\",");
			if(!DEFAULT_PFP.equals(pfp))
				sb.append("\"pfp\":\"").append(pfp).append("\",");
			if(vc)
				sb.append("\"vc\":1,");
			if(online)
				sb.append("\"online\":1,");
			if(!DEFAULT_VOLS.equals(vols))
				sb.append("\"vols\":\"").append(vols).append("\",");
		if(sb.length()==1) {
			return Integer.toString(id);
		}
		return sb.append("\"id\":").append(id).append("}").toString();	
	}
}


//id(-1 if below 25 or is ahead, other discrepancy) 0
//username 1
//pfp 2
//time 3
//length 4
//msg 5
//444 start of ws loop
record Channel(int id, String name){
	Channel(String api){
		this(parseInt(api.substring(0,api.indexOf(","))),
			api.substring(api.indexOf(",")+1)
		);
	}
	@Override
	public String toString(){
		return id+":"+name;
	}
}
/**TODO: DAL and separate hydar packages or something*/
/**todo: scale or something*/
class Board{
	public final List<HydarEndpoint> users= new CopyOnWriteArrayList<>();
	public final Map<Integer,Member> members = new  ConcurrentHashMap<>();
	public final Map<Integer,Message> messages = new ConcurrentHashMap<>(25);
	public final List<Channel> channels = new ArrayList<>();
	public volatile static long REFRESH_TIMER=-1;
	public volatile int boardId;
	public volatile String name;
	public volatile String image;
	
	public volatile int isPublic=0;
	public volatile int channelOf=0;
	public volatile int isDm=0;
	public volatile int readOnly=0;
	public volatile int ownerID=0;
	public volatile boolean hasRaye=false;
	public volatile boolean alive=true;
	//hashes of last updates
	private volatile byte[][] lastUpdates=new byte[][]{null,null,null,null};
	public static final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
	
	private static final String BITS = "Getting bits data...";
	private static final String FORGE = "Getting forge data...";
	
	public static AtomicInteger lastId = new AtomicInteger();
	public static final ReentrantLock lock = new ReentrantLock();
	private final Runnable UPDATE=()->{
		if(alive)
			this.apiRefresh();
		//alive state may change
		if(alive)
			timer.schedule(this.UPDATE,REFRESH_TIMER,TimeUnit.MILLISECONDS);
	};
	public Board(int id){//, String apiStr
		this.boardId=id;
		this.name="";
		//parseApi(apiStr);//drop on error?? probably not needed
		//add users to api
		timer.schedule(UPDATE,5000,TimeUnit.MILLISECONDS);
	}
	
	public void addUser(HydarEndpoint user) throws IOException{
		lock.lock();
		try{
			users.add(user);
			if(users.stream().map(x->x.id).distinct().count()==1 || !members.containsKey(user.id)) {
				apiRefresh(user);
			}
			members.get(user.id).online=true;
			update(user);
		}finally {
			lock.unlock();
		}
		updateVc(user.id);//skip this user
		
		//send them current messages, vc users, full member list maybe, etc
	}
	public void apiRefresh() {
		apiRefresh(null);
	}
	public void apiRefresh(HydarEndpoint skip){//choose a user
		lock.lock();
		try{
			if(!alive)return;
			for(HydarEndpoint t:users){
				var hResponse = HydarEE.jsp_invoke("MsgApi",t.session,"board="+this.boardId+"&last_id="+0);
				if(hResponse.getStatus()>=300)
					continue;
				String h=hResponse.getBuffer().toString(UTF_8);
				parseApi(h);
				if(members.get(t.id)==null){
					continue;
				}
				updateAll(skip);
				//ArrayList<Integer> ids=new ArrayList<Integer>();
				for(var user:users){
					if(!members.containsKey(user.id)){
						dropUser(user);
						updateAll(skip);
					}
				}
				return;
			}
		}finally{
			lock.unlock();
		}
		//no one has the permission to see this board
		writeAll(">,"+(channelOf<0?0:channelOf));
		kill();
	}
	public void kill(){
		HydarEndpoint.boards.remove(this.boardId);
		this.alive=false;
		for(HydarEndpoint t:users){
			dropUser(t);
		}
	}
	public String settingsStr() {
		return Stream.of("*",this.boardId,ownerID,name,image,channelOf,readOnly,isDm)
			.map(Object::toString)
			.collect(Collectors.joining(","));
	}
	public void updateSettings(){
		writeAll(settingsStr());
	}
	public String channelsStr() {
		return ":,"+channels.stream().map(Object::toString).collect(Collectors.joining(";"));
	}
	public void updateChannels(){
		writeAll(channelsStr());
	}
	public void updateAll(HydarEndpoint skip){
		//enum map maybe
		String[] updates = {
			settingsStr(),
			"V,"+members.values().toString(),
			channelsStr(),
			updateStr()
		};
		MessageDigest md5=null;
		byte[] digest=null;
		//if not many ppl, assume many afk prob
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {}
		for(int i=0;i<updates.length;i++) {
			//if too many users, assume msgs always change
			if(i==3 && users.size()>4) {
				writeFrom(skip,updates[i]);
				break;
			}
			md5.update(updates[i].getBytes());
			digest=md5.digest();
			if(!Arrays.equals(digest,lastUpdates[i])) {
				writeFrom(skip,updates[i]);
			}	
			lastUpdates[i]=digest;
		}
			
	}
	//TODO: apiRefresh checks when one of these was actually modified and this isn't needed anymore
	public String updateStr() {
		return "X,"+messages.values();
	}
	public void update(HydarEndpoint target) throws IOException{
		target.print(settingsStr());
		target.print("V,"+members.values().toString());
		target.print(channelsStr());
		target.print(updateStr());
	}
	public void addVc(int id){
		members.get(id).vc=true;
		updateVc();
	}
	public void dropVc(int id){
		members.get(id).vc=false;
		writeAll("-,"+id);
		updateVc();
	}
	public void updateVc() {
		writeAll("V,"+members.values().toString());
	}
	public void updateVc(int skip) throws IOException{
		writeFrom(skip,"V,"+members.values().toString());
	}
	public void dropAll(int id) {
		for(var t:users)
			if(t.id==id)
				dropUser(t);
	}
	public void dropUser(HydarEndpoint user) {
		dropUser(user,true);
	}
	public void dropUser(HydarEndpoint user, boolean close){//separate from kick
		System.out.println("drop "+user.id);
		lock.lock();
		try {
			if(users.remove(user)) {
				if(close)
					try {
						user.close();
					} catch (IOException e) {}
				if(users.size()==0){
					System.out.println("drop board "+this.boardId+" (multiple is not an error)");
					HydarEndpoint.boards.remove(this.boardId);
					this.alive=false;
				}else{
					Member m=members.get(user.id);
					if(m!=null && users.stream().noneMatch(x->x.id==user.id)) {
						if(m.vc)
							dropVc(user.id);
						else {
							m.online=false;
							updateVc();
						}
					}
				}
			}
		}finally{
			lock.unlock();
		}
		//
	}
	public void writeTo(int id, String s) throws IOException{
		//dont break, since they might have multiple tabs
		for(var t:users) 
			if(id==t.id) 
				t.print(s);
	}
	public void writeFrom(HydarEndpoint skip, String s){
		for(var t:users)
			if(t!=skip)
				try {
					t.print(s);
				} catch (IOException e) {
					dropUser(t);
				}
	}
	public void writeFrom(int id, String s){
		for(var t:users)
			if(id!=t.id)
				try {
					t.print(s);
				} catch (IOException e) {
					dropUser(t);
				}
	}
	public void writeAll(String s){
		for(var q:users) {
			try {
				q.print(s);
			} catch (IOException e) {
				e.printStackTrace();
				dropUser(q);
			}
		}
	}
	
	public String processInput(String inputText, int uid, HydarEE.HttpSession session){
		String[] msg = inputText.split(",",3);
		int transaction=parseInt(msg[0]);
		String toReply=msg[1];
		inputText=msg[2];
		//maybe?
		//inputText = inputText.replace("\n", "");
		//inputText = inputText.replace("\r", "");
		Member u =members.get(uid);
		if(inputText.length()>=3000)
			inputText=new StringBuilder(3000).append(inputText,0,2997).append("...").toString();
		if(u==null)
			return null;
		if(channelOf != -1 && readOnly == 1 && !u.owner){
			return null;
		}
		inputText = inputText.replace("<", "&lt;");
		if(u.perms.equals("great_white") || u.perms.equals("water_hydar")){
			//whitelisted html
			inputText=inputText.replace("&lt;img", "<img")
				.replace("&lt;br>", "<br>");
		}else {
			inputText=inputText.replace(">","&gt;")
				.replace("&lt;br&gt;", "<br>");
		}
		if(inputText.startsWith("/")&& isDm == 0){
			inputText = processCommand(inputText,u,toReply,session,transaction);
		}
		if(inputText==null)return null;
		if(inputText.contains("https://www.youtube.com/watch?v=")){
			inputText = inputText.replace("https://www.youtube.com/watch?v=", "https://www.youtube.com/embed/");
			int idx=inputText.indexOf("https://www.youtube.com/embed/");
			if(inputText.indexOf(" ",idx)>0){
				inputText = inputText.substring(0,idx) 
						+ "<iframe width=853 height=505 src='" + inputText.substring(idx, inputText.indexOf(" ",idx)).replace("'","")+ "'></iframe>";
			}else{
				inputText = inputText.substring(0,idx) 
						+ "<iframe width=853 height=505 src='" + inputText.substring(idx).replace("'","")  + "'></iframe>";

			}
		} else if(inputText.contains(".") && (u.perms.equals("great_white") || u.perms.equals("water_hydar")) && !inputText.contains("<img src")){
			
			boolean containsLink = false;
			int indexOfDot = 0;
			String link = "";
			for(int i = 0; i < inputText.length()-2; i++){
				if(inputText.charAt(i)!=' ' &&inputText.charAt(i+1)=='.'&&inputText.charAt(i+2)!=' '
						//filter out ...
						&& !(inputText.charAt(i)=='.'||inputText.charAt(i+2)=='.')	
						){
					containsLink = true;
					indexOfDot = i+1;
				}
			}
			if(containsLink){
				//find link
				String firstPart = "";
				String secondPart = "";
				for(int i = indexOfDot; i >=0; i--){
					if(i == 0){
						firstPart = inputText.substring(i, indexOfDot);
						break;
					}else if(inputText.charAt(i)==' '){
						firstPart = inputText.substring(i+1, indexOfDot);
						break;
					}
				}
				for(int i = indexOfDot; i <inputText.length(); i++){
					if(i == inputText.length()-1){
						secondPart = inputText.substring(indexOfDot);
						break;
					}else if(inputText.charAt(i) == ' '){
						secondPart = inputText.substring(indexOfDot, i);
						break;
					}
				}
				link = firstPart + ""+ secondPart;
				//System.out.println(link);
				inputText = inputText.substring(0, inputText.indexOf(link)) + "<a href='" + link.replace("'","") + "' target='_blank'>" + link +"</a>" + inputText.substring(inputText.indexOf(link) + link.length());
				//System.out.println(inputText);
			}
		}
			
		int newID=newMessage(inputText,u,toReply,transaction,session,false);
		if(this.hasRaye && inputText.length()>5 && inputText.startsWith("raye ")){
			//newMessage(inputText,u,toReply,transaction, true);
			TasqueManager.add(new PythonBot(this,new String[]{HydarEndpoint.PYTHON_PATH,"./bots/raye.py", inputText.substring(5)},null,""+newID,0,true));
		}
		//direct comparison(==) to ensure it was actually triggered by the command
		//(so typing "Getting bits data..." won't actually do it)
		else if(inputText==FORGE){
			TasqueManager.add(new PythonBot(this,new String[]{HydarEndpoint.PYTHON_PATH,"./bots/HydarForgeCalculator_0.2.5.4.py"},u,""+newID,0,false));
		}else if(inputText==BITS){
			TasqueManager.add(new PythonBot(this,new String[]{HydarEndpoint.PYTHON_PATH,"./bots/HydarBitsCalculator.py"},u,""+newID,0,false));
		}else if(inputText.equals("/bloons")) {
			TasqueManager.add(new IframeBot(this,u,""+newID,"bloons.html",0));
		}
		return null;
	}
	public String processCommand(String inputText, Member u, String toReply, HydarEE.HttpSession session, int transaction){
		int done=0;
		if(inputText.startsWith("/")&& isDm == 0){
			//TODO: minor note - most of these dont check if jsp succeeds
			// /help
			if(inputText.equals("/help")){
				inputText = "User Commands: <br>(Unofficial boards only): <b>/leave</b><br>(View board owner commands): <b>/adminhelp</b><br>";
				done = 1;
			}else if(inputText.equals("/leave")){
				inputText = "Leaving board...";
				if(HydarEE.jsp_invoke("LeaveBoard",session,"board_num="+this.boardId).getStatus()<400) {
					members.remove(u.id);
					dropAll(u.id);
					updateVc();
				}
				done = 1;
			}else if(inputText.equals("/forge")){
				//ProcessBuilder pb = new ProcessBuilder("python", "py").inheritIO();
				inputText=FORGE;
				
				done = 1;
			}else if(inputText.equals("/bits")){
				//ProcessBuilder pb = new ProcessBuilder("python", "py").inheritIO();
				inputText=BITS;
				
				done=1;
			}else if(u.owner){
				// /admin
				if(inputText.equals("/adminhelp")){
					inputText = "Admin commands: <b><br>/kick (user id)</b><br><b>/invite (user id)"
								+"</b><br><b>/deleteboard</b><br>&emsp;Will delete the board if in main channel. If this command is used in another channel, only that channel will be deleted.<br>"
								+"<b>/inviteonly (on/off)</b><br>&emsp;Invite only on: users must be invited to the board to join<br>"
								+"<b>/createchannel (channel name)<br></b>&emsp;Creates a new channel with the given channel name. Refresh to see channel in menu. Users can be manually invited to the channel<br>"
								+"<b>/rename (new name)<br></b>&emsp;Renames the current channel to the given new name. If in main channel, will rename the board.<br>"
								+"<b>/readonly (on/off)<br></b>&emsp;Read only on: only an admin can post in the channel. Main board cannot be read-only.";
					done = 1;
					
				}
				
				// commands that take an input
				if(inputText.indexOf(" ") != -1){
					String cmd=inputText.substring(0, inputText.indexOf(" "));
					// /invite
					if(cmd.equals("/invite")){
						int invitedUser = parseInt(inputText.substring(inputText.indexOf(" ") + 1));
						inputText = "Sent invite to user #" + invitedUser;
						HydarEE.jsp_invoke("InviteUser",session,"invitedID="+invitedUser + "&board_num="+this.boardId);
						if(invitedUser==2) {
							hasRaye=true;
							apiRefresh();
						}done = 1;
					}
					
					// /kick
					else if(cmd.equals("/kick")){
						int kickedUser = parseInt(inputText.substring(inputText.indexOf(" ") + 1));
						inputText = "Removed user #" + kickedUser;
						var ret=HydarEE.jsp_invoke("KickUser",session,"kickID="+kickedUser + "&board_num="+this.boardId);
						if(ret.getStatus()<400) {
							members.remove(kickedUser);
							dropAll(kickedUser);
							if(kickedUser==2)
								hasRaye=false;
							apiRefresh();
						}
						done = 1;
					}
					
					// /inviteonly
					else if(cmd.equals("/inviteonly")){
						String onOff = inputText.substring(inputText.indexOf(" ") + 1);
						if(onOff.equalsIgnoreCase("on")){
							isPublic=0;
							image="misc.png";
							inputText = "Invite only has been switched to ON (users must have an invite to join this board)";
							HydarEE.jsp_invoke("EditBoardSettings",session,"inviteonly=on&board_num="+this.boardId);
						}
						if(onOff.equalsIgnoreCase("off")){
							isPublic=1;
							image="PublicBoard.png";
							inputText = "Invite only has been switched to OFF (anyone with the board ID can join this board now)";
							HydarEE.jsp_invoke("EditBoardSettings",session,"inviteonly=off&board_num="+this.boardId);
						}
						done = 1;
						updateSettings();
					}
					
					// /readonly
					else if(cmd.equals("/readonly") && channelOf != -1){
						String onOff = inputText.substring(inputText.indexOf(" ") + 1);
						if(onOff.equalsIgnoreCase("on")){
							//update & updateAll()
							readOnly=1;
							inputText = "Read only has been switched to ON (Only the board admin can post)";
							HydarEE.jsp_invoke("EditBoardSettings",session,"readonly=on&board_num="+this.boardId);
						}
						if(onOff.equalsIgnoreCase("off")){
							readOnly=0;
							inputText = "Read only has been switched to OFF (All users can post)";
							HydarEE.jsp_invoke("EditBoardSettings",session,"readonly=off&board_num="+this.boardId);
						}
						done = 1;
						updateSettings();
					}
					
					// /readonly
					else if(cmd.equals("/rename")){
						String newname = inputText.substring(inputText.indexOf(" ") + 1);
						inputText = "Renamed board to " + newname;
						newname=URLEncoder.encode(newname,UTF_8);
						this.name=newname;
						HydarEE.jsp_invoke("EditBoardSettings",session,"newName=" + newname + "&board_num="+this.boardId);
						done = 1;
						updateSettings();
					}
					
					
					// /createchannel
					else if(cmd.equals("/createchannel")){
						inputText = inputText.replace("<", "&lt;");
						String channelName = inputText.substring(inputText.indexOf(" ") + 1);
						channelName=URLEncoder.encode(channelName,UTF_8);
						HttpServletResponse ret;
						if(channelOf == -1){
							ret=HydarEE.jsp_invoke("CreateBoard",session,"input_create="+ channelName +"&channelof=" + this.boardId);
						}else{
							ret=HydarEE.jsp_invoke("CreateBoard",session,"input_create="+ channelName +"&channelof=" + this.channelOf);
						}
						if(ret.getStatus()<400) {
							String resp=ret.getBuffer().toString();
							int start=resp.indexOf("board=")+6;
							int end=resp.indexOf("&",start);
							resp=resp.substring(start,end<0?resp.length():end);
							channels.add(new Channel(Integer.parseInt(resp),channelName));
							updateChannels();
							
						}else inputText="Channel not created.";
						done = 1;
					}
					
				}
				
				
				// /delete - prompt comfirmation
				if(inputText.equals("/deleteboard")){
					inputText = "Requested to delete board. Type \"/confirm-delete\" to confirm and delete this board.";
				}
				// delete - actual
				if(inputText.equals("/confirm-delete")){
					inputText = "Deleting board...";
					done=0;
				}
			}else done=1;
			if(u.perms.equals("water_hydar")){
				String[] cmd=inputText.split(" ",3);
				/**
				 * /ipban user [uid]
				 * /ipban addr [addr]
				 *  /ipban message [mid]
				 *  /ipban message
				 *  -->(implicit replyTo id)
				 *  same for /unban
				 *  
				 *  /ban [uid]
				 *  -->deletes an acc
				 * */
				if(cmd[0].equals("/ban")) {
					if(cmd.length==1) {
						return "Requires 2 args: /... [uid]";
					}try {
						int kickedUser=parseInt(cmd[1]);
						HydarEE.jsp_invoke("BanUser",session,"kickID="+kickedUser+"&ip=no");
						
						dropAll(kickedUser);
					}catch(NumberFormatException nfe) {
						return "ID must be a number";
					}
					
				}else if(cmd[0].equals("/ipban")||cmd[0].equals("/unban")) {
					String bantype="user";
					boolean unban=cmd[0].equals("/unban");
					int kickedUser=-1;
					if(cmd.length==2) {
						if(cmd[1].equals("message")) {
							cmd[1]=toReply;
							bantype="message";
						}else {
							done=1;
							return "Requires 2-3 args: /... message(if in a reply), /... message [mid], /... user [uid], /... addr [addr]";
						}
						kickedUser = parseInt(cmd[1]);
					}else{
						if(cmd.length<3) {
							done=1;
							return "Requires 2-3 args: /... message(if in a reply), /... message [mid], /... user [uid], /... addr [addr]";
							
						}
						bantype=cmd[1];
						if(!("addr".equals(bantype))) {
							kickedUser = parseInt(cmd[2]);
						}else bantype=cmd[2];
					}
					HydarEE.jsp_invoke("BanUser",session,"kickID="+kickedUser+"&ip="+URLEncoder.encode(bantype,ISO_8859_1)+"&unban="+unban);
					if(bantype.equals("user"))
						dropAll(kickedUser);
					else dropAll(3);
					inputText = (unban?"Unbanned":"Banned")+" "+(kickedUser>=0?bantype:"IP")+" #" + kickedUser;
					done = 1;
				}
			}
			
			
			
		}
		if(done == 0 && inputText.equals("Deleting board...")){
			inputText=null;
			HydarEE.jsp_invoke("DeleteBoard",session,"board_num="+this.boardId);
			writeAll(">,"+(channelOf<0?0:channelOf));
			kill();
		}
		return inputText;
	}
	static final HydarEE.HttpSession RAYE = HydarEE.HttpSession.create(InetAddress.getLoopbackAddress());
	static {
		RAYE.setMaxInactiveInterval(Integer.MAX_VALUE);
		RAYE.setAttribute("userid",2);
	}
	public int newMessage(String inputText, Member u, String toReply, int transaction){
		return newMessage(inputText, u, toReply, transaction,null, false);
	}
	public int newMessage(String inputText, Member u, String toReply,HttpSession session, int transaction){
		return newMessage(inputText, u, toReply, transaction,session, false);
	}
	public int newRayeMessage(String inputText, Member u, String toReply, int transaction){
		return newMessage(inputText, u, toReply, transaction,null, true);
	}
	public int newMessage(String inputText, Member u, String toReply, int transaction, HttpSession session, boolean raye){
		//add reply header
		if(!members.containsKey(u.id))
			return -1;
		if(parseInt(toReply)>0){
			int idOfPost = parseInt(toReply);
			
			Message op =messages.get(idOfPost);
			String replyContents = op.message;
			int opLength=replyContents.length();
			String replyName = URLDecoder.decode(members.get(op.uid).username,UTF_8);
			int divStart=replyContents.indexOf(">",replyContents.indexOf("<div"))+1;
			int divEnd=replyContents.indexOf("</div>",divStart);
			boolean check1=divStart>0&&divStart<opLength;
			boolean check2=check1&&divStart>0&&divStart<opLength;
			int replyLength=opLength;
			if(check2&&replyContents.startsWith("<div hidden id = 'actualContents")){
				replyContents = replyContents.substring(divStart, Math.min(divEnd,divStart+67));
				replyLength=divEnd-divStart;
			}
			if(replyContents.contains("https://www.youtube.com/embed/") || replyContents.contains("<a href") || replyContents.contains("<img src")){
				replyContents = " ";
				replyLength=1;
			}
			String actualContents = inputText.substring(14+replyName.length()+replyContents.length());
			//System.out.println(actualContents);
			String trimmed=actualContents.trim();
			if(u.perms.equals("water_hydar")
					&&(trimmed.startsWith("/ipban")
					||trimmed.startsWith("/ban")
					||trimmed.startsWith("/unban")
					)
				) {
				inputText=processCommand(trimmed,u,toReply,session,transaction);
			}else {
				int lessThan=actualContents.indexOf("<");
				int quotableEnd = Math.min(64,lessThan>0?lessThan:actualContents.length());
				inputText = new StringBuilder(64+actualContents.length())
						.append("<div hidden id = 'actualContents<MESSAGE_ID>'>")
						.append(actualContents,0,quotableEnd)
						.append(actualContents.length()>64?"...":"")
						.append("</div><a href = '#reply_button")
						.append(idOfPost)
						.append("'><b>")
						.append(inputText,0,12+replyName.length())
						.append("</b><i>")
						.append(inputText,12+replyName.length(), 14+replyName.length()+Math.min(replyLength,67))
						.append("</i></a><br>")
						.append(actualContents)
						.toString();
			}
				
		}
		if(inputText==null)
			return -1;
		int newID=-1;
		lock.lock();
		HydarEndpoint t = null;
		for(HydarEndpoint m:users)
			if(m.id==u.id)
				t=m;
		if(t==null&&!raye)
			return -1;
		try{
			newID =lastId.incrementAndGet();
			inputText=inputText.replace("actualContents<MESSAGE_ID>","actualContents"+newID);
			messages.put(newID,new Message(newID,u.id,System.currentTimeMillis(),inputText,transaction));
			if(newID>=25)
				messages.remove(newID-25);
			
			HydarEE.HttpSession s=raye?RAYE:t.session;
			//avoid re-URLencode
			var req=new HydarEE.HttpServletRequest("SubmitPost",Map.of(
					"replyID",""+toReply,
					"board_num",""+this.boardId,
					"input_text",inputText
					)
				).withSession(s,true);
			req.setAttribute("HYDAR_TIMESTAMP",messages.get(newID).time);
			var resp = HydarEE.jsp_invoke(req);
			if(resp.getStatus()>=400) {
				dropAll(u.id);
				lastId.decrementAndGet();
			}else writeAll("N,"+messages.get(newID).toString());
			//"replyID="+toReply+"&board_num="+this.boardId+"&input_text="+URLEncoder.encode(inputText,UTF_8));
		}finally{
			lock.unlock();
		}
		return newID;
		//if(raye)
		//	apiRefresh();
	}
	public void parseApi(String apiStr){
		String[] lines = apiStr.lines().toArray(String[]::new);//split -1 doesnt ignore empty strings at the end(normal split does)
		messages.clear();
		var tmpMembers = new HashMap<Integer,Member>();
		StringBuilder message;
		int ul=-1;
		String[] properties = lines[0].split(",",-1);
		lastId.set(parseInt(properties[1]));
		this.isPublic = parseInt(properties[2]);
		this.channelOf = parseInt(properties[3]);
		this.isDm = parseInt(properties[4]);
		this.readOnly = parseInt(properties[5]);
		this.ownerID = parseInt(properties[6]);
		this.name = properties[7];
		this.image = properties[8];
		this.hasRaye = false;
		channels.clear();
		String[] channelStrings = lines[1].split(";");
		for(String s: channelStrings){
			if(s.contains(","))
				channels.add(new Channel(s));
		}
		if(lines[2].startsWith("har")){
			ul = parseInt(lines[2].substring(3));
		}
		for(int i=3;i<ul+2;i+=5){
			boolean vc=false;
			int uid=parseInt(lines[i]);
			if(members.get(uid)!=null){
				vc=members.get(uid).vc;
			}
			Member m = new Member(uid,lines[i+1],lines[i+2],lines[i+3],lines[i+4],uid==ownerID);
			m.vc=vc;
			if(uid==2)
				this.hasRaye=true;
			for(HydarEndpoint t:users)
				if(t.id==uid)
					m.online=true;
			tmpMembers.put(uid,m);		
		}
		members.clear();
		members.putAll(tmpMembers);
		for(int i=ul+3;i<lines.length-1;i+=5){
			int id=parseInt(lines[i]);
			int uid=parseInt(lines[i+1]);
			long time = Long.parseLong(lines[i+2]);
			int length = parseInt(lines[i+3]);
			message = new StringBuilder(length);
			message.append(lines[i+4]);
			while(message.length()<length){
				i++;
				if(i+1==lines.length)
					message.append("\n");
				else 
					message.append("\n").append(lines[i+4]);
			}
			messages.put(id,new Message(id,uid,time,message.toString(),1));
		}
	}
}
class TasqueManager{
	//TODO: redo bots
	public static final int MAX_TASQUES=10;
	public static final int TASQUE_DELAY=500;
	public static volatile boolean alive=true;
	public static volatile BlockingQueue<Tasque> taskQueue=new ArrayBlockingQueue<>(MAX_TASQUES, true);
	static {
		//if(Config.TASQUE_ENABLED){
			Thread manager=new Thread(TasqueManager::run);
			manager.setDaemon(true);
			manager.start();
		//}
	}
	public static void run(){
		while(alive) {
			try {
				taskQueue.take().run();
				Thread.sleep(TASQUE_DELAY);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	public static void add(Tasque b){
		if(!taskQueue.offer(b))
			b.fail();
	}
}
class PythonBot implements Tasque{
	private Board board;
	private Member u;
	private String[] command;
	protected String toReply;
	private int transaction;
	private boolean raye;
	protected String output;
	public PythonBot(Board b, String[] command, Member u, String toReply, int transaction, boolean raye){
		this.board=b;
		this.command=command;
		this.u=u;
		this.toReply=toReply;
		this.transaction=transaction;
		if(this.transaction==0){
			this.transaction=(int)(2E9+Math.random()*8E7);
		}
		this.raye=raye;
		this.output=null;
		if(raye)
			this.u=b.members.get(2);
	}
	@Override
	public void run(){
		try{
			this.output = Tasque.runCommand(command);
		}catch(IOException|InterruptedException e){
			e.printStackTrace();
			this.output=null;
		}
		if(this.output==null)
			fail();
		else success();
	}
	@Override
	public void success(){
		int idOfPost=Integer.parseInt(toReply);
		if(idOfPost>0&&board.messages.get(idOfPost)!=null){
			Message m=board.messages.get(idOfPost);
			String replyHeader="";
			if(m!=null){
				Member u = board.members.get(board.messages.get(idOfPost).uid);
				if(u!=null){
					String repliedText = m.message;
					if(repliedText.contains("<iframe") || repliedText.contains("<a href") || repliedText.contains("<img src"))
						repliedText=" ";
					replyHeader = "Replying to "+URLDecoder.decode(u.username,StandardCharsets.UTF_8)+" "+repliedText+": ";
				}
				
			}
			this.output=replyHeader+output;
		}
		board.newMessage(this.output,u,toReply,transaction,null,raye);
	}
	@Override
	public void fail(){
		if(raye)
			board.newRayeMessage("hydar??????",u,"-1",transaction);
		else board.newMessage("Interaction failed.",u,"-1",transaction);
	}
}

interface Tasque extends Runnable{
	public void success();
	public void fail();
	@Override
	public void run();
	public static String runCommand(String[] command) throws IOException, InterruptedException{
		Process p = Runtime.getRuntime().exec(command);
		
		if(!p.waitFor(60000,TimeUnit.MILLISECONDS)){
			p.destroy();
			return null;
		}
		String inputText = p.inputReader().lines().collect(joining("<br>"));
		if(inputText.length()>3000)
			inputText=inputText.substring(0,2997)+"...";
		p.getErrorStream().transferTo(System.err);
		return inputText;
	}
}
//TODO: common subclass
class IframeBot extends PythonBot{
	final String url;
	public IframeBot(Board b, Member u, String toReply, String url, int transaction) {
		super(b, null, u, toReply, transaction, false);
		this.url=url;
	}
	@Override
	public void run() {
		this.output=new StringBuilder(64)
				.append("hop on bloons<br><iframe width='400' height='400' frameBorder='0' src = '")
				.append(url).append("'></iframe>")
				.toString();//url is trusted field
		success();
	}
}