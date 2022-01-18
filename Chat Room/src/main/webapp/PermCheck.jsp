<%

	try{
		int board = Integer.parseInt(request.getParameter("board").replace("\\", "").replace("\"", "").toString()); 
Class.forName("com.mysql.jdbc.Driver");
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatroom?autoReconnect=true&useSSL=false", "root", "hydar");
	Statement stmt = conn.createStatement();
	
	// CHECK PERM
	
	String str = "SELECT isin.user, isin.board FROM isin WHERE isin.board = " + board + " AND isin.user = " + session.getAttribute("userid").toString();
	ResultSet result = stmt.executeQuery(str);
	if(!result.next()){
		response.sendRedirect("Login.jsp");
	}
	}catch(Exception e){
		response.sendRedirect("Login.jsp");
	}
%>