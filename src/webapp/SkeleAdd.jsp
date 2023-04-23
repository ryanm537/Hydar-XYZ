<%
if(session.getAttribute("userid")==null){
	session.setAttribute("userid",3);
	session.setAttribute("username","Guest");
}%>