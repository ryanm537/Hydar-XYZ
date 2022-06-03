<%
Class.forName("com.mysql.jdbc.Driver");
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");
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
	
	if(session.getAttribute("username").toString().equals("null")){
		throw new Exception();
	}
	
	
	%>
	<%
		
		
		// hdaryhdaryhdayrhdyahryda
		
		// VCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC HYDAR
		
		// DHYARHYDHYARDHYADRYRR
		// ye, it is true that hydar
		%>
		
	
	
	<%

	// SHOW MESSAGES

	
	Statement stmt = conn.createStatement();
	String checkPostsStr="SELECT post.id, user.id, user.username, user.pfp, post.board, post.contents, post.created_date"
				+ " FROM user, posts, post"
				+ " WHERE posts.post = post.id AND user.id = posts.user AND post.board = " + board
				+ " ORDER BY post.id DESC";
	ResultSet result = stmt.executeQuery(checkPostsStr);
	int count = 25; // <- DISPLAYED POSTS LIMIT XXXXXXXXXXXXXXXXXX
	int maxCount = count;
	String getStart = request.getParameter("last_id");
	int start = -1;
	if(getStart != null){
		start = Integer.parseInt(getStart);
	}
	//format
	//id(-1 if below 25 or is ahead, other discrepancy) 0
	//username 1
	//pfp 2
	//time 3
	//length 4
	//msg 5
	
	while(result.next() && count > 0){
		//time
		int id = result.getInt("post.id");
		
		if(id==start){
			break;
		}
		if(count==maxCount){
			if(start==-1||id<start||(start-id>maxCount)){
				out.print("-1");
				break;
			}
		}
		String fixedString = result.getString("post.contents").replaceAll("<", "&lt;");
		fixedString=fixedString.replaceAll("&lt;href", "<href").replaceAll("&lt;img", "<img").replaceAll("&lt;br", "<br");
		out.print(id);
		out.print("<br>");
		out.print(result.getString("user.username"));
		out.print("<br>");
		out.print(result.getString("user.pfp"));
		out.print("<br>");
		out.print(result.getLong("post.created_date"));
		out.print("<br>");
		out.print(fixedString.length());
		out.print("<br>");
		out.print(fixedString);
		out.print("<br>");
		count-=1;
	}
	
	conn.close();
	
	// hydar hydar
	
	
	%>
