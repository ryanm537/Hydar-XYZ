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
<%!
//limits only for gw's
static final int MAX_TOTAL = 100_000_000;
static final int MAX_COUNT = 1024;
static final int MAX_PER=10_240_000;
static final long HYDAR_MAX_TOTAL = 60_000_000_000l;
static final int UPLOAD_SLEEP=2000;
static final String FILE_ROOT_PATH="/attachments";
static Path fileRoot = null;
static SecureRandom rng = new SecureRandom();
static final Pattern FILE_SAFE = Pattern.compile("[^a-zA-Z0-9-_.+() ]");
static volatile long lastUpdate=0;
%>
<%
if(fileRoot==null){
	fileRoot = Path.of(request.getServletContext().getRealPath(FILE_ROOT_PATH));
}
if(request.getMethod().equals("POST")){
	Class.forName("com.mysql.jdbc.Driver");
	DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/hydar");
	try(Connection conn=dataSource.getConnection()){
		String filename = request.getParameter("filename");
		String ordinal = request.getParameter("ordinal");
		if(filename==null || ordinal==null){
			response.sendError(400);
			return;
		}
		filename=FILE_SAFE.matcher(filename).replaceAll("");
		ordinal=FILE_SAFE.matcher(ordinal).replaceAll("");
		if(ordinal.length()==0){
			response.sendError(400);
			return;
		}
		char ord = ordinal.charAt(0);
		if(filename.length()>64){
			int dot=filename.lastIndexOf(".");
			if(dot>=0){
				String part2 = filename.substring(dot);
				filename=new StringBuilder(64)
						.append(filename,0,Math.max(0,64-part2.length()))
						.append(part2)
						.toString(); 
			}else filename=filename.substring(64);
		}
		String thumbSizeString = request.getParameter("thumbsize");
		int thumbSize = thumbSizeString==null?0:Integer.parseInt(thumbSizeString);
		
		int board = Integer.parseInt(request.getParameter("board").replaceAll("\"", ""));
		Integer uid = (Integer)session.getAttribute("userid");
		if(uid==null ||uid==3){
			response.sendError(403);
			return;
		}
		//1. check perm
		String str = "SELECT user.permission_level FROM user WHERE user.id = ?" ;
		var ps = conn.prepareStatement(str);
		ps.setInt(1,uid);
		var result=ps.executeQuery();
		String perms = "";
		while(result.next()){
			perms = result.getString("user.permission_level");
		}
	
		if(!perms.equals("water_hydar") && !perms.equals("great_white")){
			response.sendError(403);
			return;
		}
		//1.5 delete expired files
		long now=System.currentTimeMillis();
		if(now-lastUpdate > 3600*1000*12){
			lastUpdate=now;
			ps = conn.prepareStatement("SELECT path, filename FROM `file` WHERE post IS NULL OR user IS NULL OR board IS NULL OR (post = -1 AND date < ?)");
			ps.setLong(1,now-3600*1000*12);
			result = ps.executeQuery();
			while(result.next()){
				String path = result.getString("path");
				Files.deleteIfExists(fileRoot.resolve(path).resolve(result.getString("filename")));
				Files.deleteIfExists(fileRoot.resolve(path).resolve(result.getString("filename")+".jpg"));
				Files.deleteIfExists(fileRoot.resolve(path));
			}
			ps = conn.prepareStatement("DELETE FROM `file` WHERE post IS NULL OR user IS NULL OR board IS NULL OR (post = -1 AND date < ?)");
			ps.setLong(1,now-3600*1000*12);
			ps.executeUpdate();
		}
		//2. check max size in db(should be context param, hydar param for ee uploads too)
		long sizeLeft=Integer.MAX_VALUE;
		long totalUploadSize=0;
		int uploadCount = 0;
		int uploadSize = request.getContentLength();
		boolean hydar=perms.equals("water_hydar");
		if(!hydar && uploadSize>MAX_PER){
			response.sendError(400);
			return;
		}
		long maxTotal = hydar?HYDAR_MAX_TOTAL:MAX_TOTAL;
		int maxCount = hydar?Integer.MAX_VALUE:MAX_COUNT;
		/**ps = conn.prepareStatement("SELECT COUNT(*),SUM(size) FROM `file` WHERE user = ? AND ");
		ps.setInt(1,uid);
		result=ps.executeQuery();
		while(result.next()){
			int dailyUpload=result.getInt(1);
		}*/
		ps = conn.prepareStatement("SELECT COUNT(*),SUM(size) FROM `file` WHERE user = ?");
		ps.setInt(1,uid);
		result=ps.executeQuery();
		while(result.next()){
			uploadCount = result.getInt(1);
			totalUploadSize = result.getLong(2);
			
			sizeLeft = maxTotal - (totalUploadSize + uploadSize);
		}
		if(thumbSize > uploadSize || uploadSize > maxTotal){
			response.sendError(400);
			return;
		}
		ps = conn.prepareStatement("SELECT path, filename, size FROM `file` WHERE user = ? ORDER BY date ASC");
		ps.setInt(1,uid);
		result=ps.executeQuery();
		while((uploadCount > maxCount || sizeLeft < uploadSize) && result.next()){
			ps = conn.prepareStatement("DELETE FROM `file` WHERE path=?");
			String path = result.getString("path");
			ps.setString(1,path);
			ps.executeUpdate();
			Files.deleteIfExists(fileRoot.resolve(path).resolve(result.getString("filename")));
			Files.deleteIfExists(fileRoot.resolve(path).resolve(result.getString("filename")+".jpg"));
			Files.deleteIfExists(fileRoot.resolve(path));
			sizeLeft += result.getInt("size");
			uploadCount--;
		}
		if(sizeLeft < uploadSize){
			response.sendError(400);
			return;
		}
		
		//TODO: clean up -1 attachments
		//3. syphon thing into a new file with limited buffer, and db
		String path=null;
		byte[] pathBytes = new byte[12];
		rng.nextBytes(pathBytes);
		path = Base64.getUrlEncoder().encodeToString(pathBytes);
		path = ""+ord+path.substring(1);
		
		ps = conn.prepareStatement("INSERT INTO `file`(path, filename, user, board, post, size, date) VALUES(?,?,?,?,?,?,?)");
		ps.setString(1,path);
		ps.setString(2,filename);
		ps.setInt(3,uid);
		ps.setInt(4,board);
		ps.setInt(5,-1);
		ps.setInt(6,uploadSize);
		ps.setLong(7,System.currentTimeMillis());
		ps.executeUpdate();
		//on load, use thumb depending on extension
		//if not present use alt text
		//dont allow post until thumb found
		//add rendered preview to top and also use in msgs
		//click -> expand to full screen(like ss)
		conn.close();
		try{
			Path dir = fileRoot.resolve(path);
			Files.createDirectories(dir);
			if(thumbSize<=0){
				try(InputStream is=request.getInputStream();
					var os=Files.newOutputStream(dir.resolve(filename));){
						is.transferTo(os);
				}
			}else{
				
				try(InputStream is=request.getInputStream();
					var os=Files.newOutputStream(dir.resolve(filename));
					var thumbOS=Files.newOutputStream(dir.resolve(filename+".jpg"))){
					
						os.write(is.readNBytes(uploadSize-thumbSize));
						thumbOS.write(is.readNBytes(thumbSize));
				}
			}
			Thread.sleep(UPLOAD_SLEEP);
		}catch(Exception ioe){
			try(var conn2=dataSource.getConnection()){
				ps = conn2.prepareStatement("DELETE FROM `file` WHERE path=?");
				ps.setString(1,path);
				ps.executeUpdate();
			}finally{
				throw ioe;
			}
		}
		//3.5. callback to HydarEndpoint so it adds to an attach list
		String fullPath=path+"/"+filename;
		HydarEndpoint.addFile(board,fullPath,uid);
		//4. return id and 200
		response.resetBuffer();
		out.print(fullPath);
		return;
		//endpoint: Map<str,int> attachments
		//updated on callbacks and initialized with all attachments from the db
		//don't allow attachments that aren't in the map
		
		//include attachments in Message object and send them - done
				
		//js, upload: 1. get the bytes in js(all of them)
		//2. upload to here
		//3. add to visible attachments(wrapPosting) if successful, otherwise remove
		//4. on post, send visible attachments as part of args
		
		//submitpost: attachments param, update the file id, also update map based on it
				
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