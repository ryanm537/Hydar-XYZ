<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.naming.Context"%>
<%@page import = "java.sql.*" %>
<%!
public static class SQL implements AutoCloseable{
	final PreparedStatement stmt;
	int idx=0;
	public SQL(Connection conn, String query) throws SQLException{
		stmt=conn.prepareStatement(query);
	}
	public SQL args(Object... args) throws SQLException{
		for(Object o:args){
			set(o);
		}
		return this;
	}
	public SQL set(Object val) throws SQLException{
		if(val instanceof Integer i){
			return setInt(i);
		}else if(val instanceof Long l){
			return setLong(l);
		}else{
			return setString(val);
		}
	}
	public SQL setInt(int val) throws SQLException{
		stmt.setInt(++idx,val);
		return this;
	}
	public SQL setInt(String val) throws SQLException{
		return setInt(Integer.parseInt(val));
	}
	public SQL setInt(Object val) throws SQLException{
		return setInt(val.toString());
	}
	public SQL setLong(long val) throws SQLException{
		stmt.setLong(++idx,val);
		return this;
	}
	public SQL setString(Object val) throws SQLException{
		stmt.setString(++idx,val.toString());
		return this;
	}
	public SQL setString(String val) throws SQLException{
		stmt.setString(++idx,val);
		return this;
	}
	public SQL setInt(int idx, int val) throws SQLException{
		stmt.setInt((this.idx=idx),val);
		return this;
	}
	public SQL setLong(int idx, long val) throws SQLException{
		stmt.setLong((this.idx=idx),val);
		return this;
	}
	public SQL setString(int idx, Object val) throws SQLException{
		stmt.setString((this.idx=idx),val.toString());
		return this;
	}
	public SQL setString(int idx, String val) throws SQLException{
		stmt.setString((this.idx=idx),val);
		return this;
	}
	public SQL update() throws SQLException{
		stmt.executeUpdate();
		return this;
	}
	public ResultSet query() throws SQLException{
		return stmt.executeQuery();
	}
	@Override
	public void close() throws SQLException{
		stmt.close();
	}
}
%>