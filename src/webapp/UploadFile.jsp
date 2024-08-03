<%@page import="xyz.hydar.app.HydarEndpoint"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="java.nio.file.Path"%>
<%@page import="java.nio.file.Files"%>
<%@page import="java.security.SecureRandom"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.*,java.util.*, java.time.*, java.text.*, java.util.Date, java.sql.*"%>
<%@ page import="javax.servlet.http.*,javax.servlet.*"%>
<%@ include file='SkeleCheck.jsp' %>
<%!
//limits only for gw's
static final int MAX_TOTAL = 1_000_000_000;
static final int MAX_PER=10_240_000;
static final String FILE_ROOT_PATH="/attachments";
static Path fileRoot = null;
static SecureRandom rng = new SecureRandom();
static final Pattern FILE_SAFE = Pattern.compile("[^a-zA-Z0-9-_.]");
%>
<%
if(fileRoot==null)
	fileRoot = Path.of(request.getServletContext().getRealPath(FILE_ROOT_PATH));
if(request.getMethod().equals("POST")){
	Class.forName("com.mysql.jdbc.Driver");
	DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
	try(Connection conn=dataSource.getConnection()){
		String filename = FILE_SAFE.matcher(request.getParameter("filename")).replaceAll("");
		int board = Integer.parseInt(request.getParameter("board").replaceAll("\"", ""));
		int uid=(int)session.getAttribute("userid");
		// CHECK PERMS
		
		String str = "SELECT user.permission_level FROM user WHERE user.id = ?" ;
		var ps = conn.prepareStatement(str);
		ps.setInt(1,uid);
		var result=ps.executeQuery();
		String perms = "";
		while(result.next()){
			perms = result.getString("user.permission_level");
		}
	
		if(!perms.equals("water_hydar") && !perms.equals("great_white")){
			throw new Exception();
		}
		//1. check perm
		//2. check max size in db(should be context param, hydar param for ee uploads too)
		int sizeLeft=Integer.MAX_VALUE;
		int totalUploads=0;
		int uploadSize = request.getContentLength();
		System.out.println("hdyar");
		if(perms.equals("great_white")){
			
			ps = conn.prepareStatement("SELECT SUM(size) FROM `file` WHERE user = ?");
			ps.setInt(1,uid);
			result=ps.executeQuery();
			while(result.next()){
				totalUploads = result.getInt(1) ;
				sizeLeft = MAX_TOTAL - (totalUploads + uploadSize);
			}
			if(sizeLeft < -1 * uploadSize){
				response.sendError(400);
				return;
			}
			ps = conn.prepareStatement("SELECT path, filename, size FROM `file` ORDER BY post WHERE user = ? AND post <> -1");
			ps.setInt(1,uid);
			result=ps.executeQuery();
			while(sizeLeft < uploadSize && result.next()){
				ps = conn.prepareStatement("DELETE FROM `file` WHERE path=?");
				String path = result.getString("path");
				ps.setString(1,path);
				ps.executeUpdate();
				Files.delete(fileRoot.resolve(path).resolve(result.getString("filename")));
				Files.delete(fileRoot.resolve(path));
				sizeLeft += result.getInt("size");
			}
			if(sizeLeft < uploadSize){
				response.sendError(400);
				return;
			}
		}
		//TODO: clean up -1 attachments
		//3. syphon thing into a new file with limited buffer, and db
		byte[] pathBytes = new byte[12];
		rng.nextBytes(pathBytes);
		String path = Base64.getUrlEncoder().encodeToString(pathBytes);
		System.out.println(path);
		ps = conn.prepareStatement("INSERT INTO `file`(path, filename, user, board, post, size, date) VALUES(?,?,?,?,?,?,?)");
		ps.setString(1,path);
		ps.setString(2,filename);
		ps.setInt(3,uid);
		ps.setInt(4,board);
		ps.setInt(5,-1);
		ps.setInt(6,uploadSize);
		ps.setLong(7,System.currentTimeMillis());
		ps.executeUpdate();
		
		try{
			Path dir = fileRoot.resolve(path);
			Files.createDirectory(dir);
			request.getInputStream().transferTo(Files.newOutputStream(dir.resolve(filename)));
		}catch(Exception ioe){
			ps = conn.prepareStatement("DELETE FROM `file` WHERE path=?");
			ps.setString(1,path);
			ps.executeUpdate();
		}
		//3.5. callback to HydarEndpoint so it adds to an attach list
		HydarEndpoint.addFile(board,path,uid);
		//4. return id and 200
		response.resetBuffer();
		out.print(path+"/"+filename);
		return;
		//endpoint: Map<str,int> attachments
		//updated on callbacks and initialized with all attachments from the db
		//don't allow attachments that aren't in the map
				
		//js, upload: 1. get the bytes in js(all of them)
		//2. upload to here
		//3. add to visible attachments(wrapPosting) if successful, otherwise remove
		//4. on post, send visible attachments as part of args
		//5. submitpost: add the file as thing? doesnt have to be ig
				
		//js, render:
		//wrapmsg will add the attachment, attachments stored in the messages map
		/**
		const response = await fetch("https://example.org/post", {
			  body: JSON.stringify({ username: "example" }),
			  // ...
			});
		*/
		//but theres no post id yet???
		//move deleted posts to Deleted user
	}catch (Exception e){
		response.setStatus(500);
		out.print("<style> body{color:rgb(255,255,255); font-family:arial; text-align:center; font-size:20px;}</style>");
		out.print("A known error has occurred\n");
		out.print("<br><br>");
		out.print("<form method=\"post\" action=\""+response.encodeURL("Logout.jsp")+"\">");
		out.print("<td><input type=\"submit\" value=\"Back to login\"></td>");
		out.print("</form>");
		e.printStackTrace();
	}
}else{
	response.sendError(400);
}
%>