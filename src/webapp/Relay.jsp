<%@page import="xyz.hydar.HydarWS"%>
<%@page import="java.io.IOException" %>
<%!
//Class.forName("HydarWS");
static{
	/**HydarWS.registerEndpoint("Relay.jsp",
			HydarWS.endpointBuilder()
			.onOpen((session, out)->{
				out.print("Hello %s!".formatted(session.getAttribute("username")));
			})
			.onMessage((msg, out)->{
				out.print(msg);
				out.close();
			})
			.onClose((output)->System.out.println("Closing..."))
			);*/
	xyz.hydar.HydarWS.registerEndpoint("Relay.jsp",xyz.hydar.HydarEndpoint.class);
}

%>