<%@page import="java.net.URLEncoder"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.stream.Stream"%>
<%@page import="java.util.stream.Collectors"%>
<%@page import="java.util.Base64,java.nio.charset.StandardCharsets	"%>
<%@include file="Util.jsp" %>
<%@ include file="SkeleAdd.jsp" %>
<%!
static final String DEFAULT_USERNAME="Anonymous";
static final String DEFAULT_PFP="images/hydar2.png";
static final String USER_QUERY=(
	"SELECT user.id, IF(username='%s','',username) as username, IF(pfp='%s','',pfp) as pfp, "
	+"permission_level,volume,vcvolume,pingvolume,pings "
	+"FROM user, isin WHERE isin.user = user.id AND isin.board= ?"
		).formatted(DEFAULT_USERNAME,DEFAULT_PFP);


static String urlenc(String in){
	return URLEncoder.encode(in,StandardCharsets.UTF_8);
} 
%>
<%
response.resetBuffer();

Class.forName("com.mysql.jdbc.Driver");
DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
try(Connection conn=dataSource.getConnection()){
	//CHECK IF BOARD IS SPECIFIED, and redirect if the user does not have perms.
	String getBoard = request.getParameter("board");
	int board = 1; 
	if(getBoard != null){
		board = Integer.parseInt(getBoard);
	}
	int uid = (int)session.getAttribute("userid");
	int maxId=0;
	try(Statement stmt=conn.createStatement()){
		String searchPostsForIDStr = "SELECT MAX(id) AS max FROM post";
		ResultSet searchPosts = stmt.executeQuery(searchPostsForIDStr);
		searchPosts.next();
		maxId = searchPosts.getInt("max");
	}
	var sql = new SQL(conn,"SELECT board FROM isin WHERE user = ? AND board = ?")
			.setInt(uid)
			.setInt(board);
	var result=sql.query();
	if(!result.next()){
		response.sendRedirect(response.encodeURL("Logout.jsp"));
		return;
	}
	// SHOW MESSAGES

	
	String image = "";
	int creatorID = -1;
	String users="";
	int lines=0;
	
	int isPublic=0;
	int channelof=0;
	int isDm=0;
	int isReadOnly=0;
	String name="error";

	sql = new SQL(conn,
			"SELECT name, image, public, channelof, dm, readonly, creator FROM board WHERE board.number = ?"
			).setInt(board);
	result = sql.query();
	if(result.next()){ 
		name = urlenc(result.getString("board.name"));
		image = urlenc(result.getString("board.image"));
		
		isPublic = result.getInt("board.public");
		channelof = result.getInt("board.channelof");
		isDm = result.getInt("board.dm");
		isReadOnly = result.getInt("board.readonly");
		

		creatorID = result.getInt("board.creator");
	}else throw new Exception();
	
	//users+=""+creatorID+"<br>"+creator+"<br>"+creatorPfp+"<br>"+perms+"<br>";;
	//member list
	
	//count used to estimate buffer length 
	
	StringBuilder userBuffer=new StringBuilder(1024);
	//find boards where this user is a member (not creator)
	
	sql = new SQL(conn,USER_QUERY)
			.setInt(board);
	result=sql.query();
	while(result.next()){
		lines+=5;
		String un=result.getString("username");
		String pfp=result.getString("pfp");
		userBuffer.append(result.getInt("user.id"))
			.append("\n");
			if(!DEFAULT_USERNAME.equals(un))
				userBuffer.append(urlenc(un));
			userBuffer.append("\n");
			if(!DEFAULT_PFP.equals(pfp))
				userBuffer.append(urlenc(pfp));
			userBuffer.append("\n")
			.append(result.getString("permission_level"))
			.append("\n")
			.append(result.getInt("volume"))
			.append(",")
			.append(result.getInt("vcvolume"))
			.append(",")
			.append(result.getInt("pingvolume"))
			.append(",")
			.append(result.getInt("pings"))
			.append("\n");
	}
	
	users = userBuffer.toString();
	//get channels list
	//
	//
	StringBuilder channelBuffer=new StringBuilder(512);
	sql =  new SQL(conn,"SELECT number,name FROM board WHERE channelof = ?")
			.setInt(channelof>=0?channelof:board);
	var results=sql.query();
	while(results.next()){
		channelBuffer.append(results.getInt("number"))
			.append(",")
			.append(urlenc(results.getString("name")))
			.append(";");
	} 
	String channels = channelBuffer.toString();
	/**
	board and member list info
	*/
	String info=Stream.of(
		board,
		maxId,
		isPublic,
		channelof,
		isDm,
		isReadOnly,
		creatorID,
		name,
		image).map(Object::toString).collect(Collectors.joining(","));
	String checkPostsStr="SELECT post.id, COALESCE(posts.user,-1) as `postUser`, post.contents, post.created_date "
		+ "		,GROUP_CONCAT(CONCAT(`file`.`path`,'/',`file`.`filename`)) as files"
		+ " FROM posts, post LEFT JOIN `file` ON `file`.post = post.id"
		+ " WHERE posts.post = post.id AND post.board = ?"
		+ " GROUP BY post.id ORDER BY post.id DESC LIMIT 25";
	
	int count = 25; // <- DISPLAYED POSTS LIMIT XXXXXXXXXXXXXXXXXX
	int maxCount = count;
	String getStart = request.getParameter("last_id");
	int start = (getStart != null)? Integer.parseInt(getStart) : -1;
	//format
	//id(-1 if below 25 or is ahead, other discrepancy) 0
	//username 1---no, replaced with uid
	//pfp 2----
	//time 3
	//length 4
	//msg 5
	sql =  new SQL(conn,checkPostsStr).setInt(board);
	result=sql.query();
	while(result.next() && count > 0){
		//time
		int id = result.getInt("post.id");
		
		if(id==start){
			break;
		}
		if(count==maxCount){
			if(start==-1||id<start||(start-id>maxCount)){
				response.sendError(500);
				break;
			}
			out.print(info); out.print('\n');
			out.print(channels); out.print('\n');
			out.print("har"); out.print(lines); out.print('\n');
			out.print(users);
		}

		String fixedString = result.getString("post.contents");//.replaceAll("<", "&lt;");
		//fixedString=fixedString.replaceAll("&lt;href", "<href").replaceAll("&lt;img", "<img").replaceAll("&lt;br", "<br");
		out.print(id);out.print('\n');
		out.print(result.getInt("postUser"));out.print('\n');
		out.print(result.getLong("post.created_date"));out.print('\n');
		out.print(result.getString("files"));out.print('\n');
		out.print(fixedString.length());out.print('\n');
		out.print(fixedString);out.print('\n');
		
		count-=1;
	}
	if(count==25){
		out.print(info); out.print('\n');
		out.print(channels); out.print('\n');
		out.print("har");
		out.print(lines); out.print('\n');
		out.print(users);
	}
	
}

	// hydar hydar
%>