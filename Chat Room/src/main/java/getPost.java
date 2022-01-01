package src;

import java.io.*;
import java.util.*;
import java.time.*;
import java.text.*;
import java.util.Date;
import java.sql.*;
import java.servlet.http.*;
import java.servlet.*;

class getPost{
	public static void main(String[] args) {
		Statement stmt = conn.createStatement();
		String checkPostsStr="SELECT post.id, user.id, user.username, user.pfp, post.board, post.contents, post.created_date"
					+ " FROM user, posts, post"
					+ " WHERE posts.post = post.id AND user.id = posts.user AND post.board = " + board
					+ " ORDER BY post.id DESC";
		ResultSet result = stmt.executeQuery(checkPostsStr);
		//int topPostID = result.getInt("post.id");
		
		int count = 100;
		out.print("<br>");
		while(result.next() && count > 0){
			//time
			float timePassed = ((float)(System.currentTimeMillis() - result.getLong("post.created_date")) / 3600000);

			out.print("<img src=\"" + result.getString("user.pfp") +"\" alt=\"hydar\" width = \"40px\" height = \"40px\" align = \"left\" hspace = \"10\" vspace = \"15\">");
			//other contents
			out.print("<style> body{color:LightGrey; font-family:arial; text-align:left; font-size:15px; display:block}</style>");
			out.print("<br><b>"+ result.getString("user.username") + "</b> <p id=\"three\">");
			out.print("<style> #three{color:Grey; font-family:arial; text-align:left; font-size:15px; display:inline}</style>");
			if((timePassed * 60) < 1){
				out.print("&nbsp;(just now): ");
			}else if(timePassed < 1){
				out.print("&nbsp;(" + (int)(timePassed * 60) + " minutes ago): ");
			}else{
				out.print("&nbsp;(" + (int)(timePassed) + " hours ago): ");
			}
			String fixedString = result.getString("post.contents").replaceAll("<i>", "");
			fixedString = result.getString("post.contents").replaceAll("<b", "");
			out.print("</p><br>" + fixedString +"<br clear = \"left\">");

			count-=1;
		}
	}
	
}