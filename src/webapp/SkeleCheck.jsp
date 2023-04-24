<%@page import="java.util.Objects"%>
<%
{
	Integer uid=(Integer)session.getAttribute("userid");
	if(uid==null || uid==3){
		response.sendRedirect(response.encodeURL("Login.jsp"));
		return;
	}
}%>