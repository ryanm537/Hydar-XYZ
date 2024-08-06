package xyz.hydar.ee;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLRecoverableException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.sql.CommonDataSource;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;
/**
 * A JDBC connection pool.
 * Can be configured with builder()
 * also supports statement pooling
 * unclosed statements will be closed on connection close
 * closed statements will not be reused
 * */
public abstract class HydarDataSource implements DataSource, AutoCloseable{
	private static final Class<?>[] CONNECTION=new Class[] { Connection.class };
	protected static final DataSource NULL_DATA_SOURCE=(DataSource)Proxy.newProxyInstance(
			HydarDataSource.class.getClassLoader(),
			new Class[] {DataSource.class},
			(x,y,z)->{throw new UnsupportedOperationException("Method "+y.getName()+" not implemented.");}
		);
	private final static float LOAD_FACTOR=0.75f;
	private final static ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
	private static final List<HydarDataSource> pools=new CopyOnWriteArrayList<>();
	protected final String username;
	protected final String password;
	protected final int minIdle;
	protected final int maxIdle;
	protected final int maxTotal;
	protected final int maxWaitMillis;
	protected final int maxOpenPreparedStatements;
	protected final boolean clearStatementPoolOnReturn;
	protected final boolean defaultReadOnly;
	protected final boolean defaultAutoCommit;
	protected final String schema;
	protected final int initialSize;
	protected final OptionalInt defaultTransactionIsolation;
	private final BlockingQueue<PooledConn> pool;
	private final Map<PooledConnection,PooledConn> actives = new ConcurrentHashMap<>();
	private final ReentrantLock lock = new ReentrantLock(true);

	private volatile boolean closed=false;
	private final ScheduledFuture<?> h;
	/**
	 * Recycle a connection(return to pool) on close, dispose on error.
	 * */
	private final ConnectionEventListener l = new ConnectionEventListener(){
		@Override
		public void connectionClosed(ConnectionEvent event) {
			recycle((PooledConnection)event.getSource());
		}
		@Override
		public void connectionErrorOccurred(ConnectionEvent event) {
			lock.lock();
			try {
				dispose((PooledConnection)event.getSource());
			}finally {
				lock.unlock();
			}
		}
	};
	/**Set up the cleanup task(see ::clean).*/
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(()->pools.forEach(HydarDataSource::close)));
	}
	/**Copy info from builder.*/
	private HydarDataSource(Builder builder) throws SQLException {
		this.username = builder.username;
		this.password = builder.password;
		this.minIdle = builder.minIdle;
		this.maxIdle = Math.max(minIdle,builder.maxIdle);
		this.maxWaitMillis = builder.maxWaitMillis;
		this.maxOpenPreparedStatements = (builder.poolPreparedStatements?builder.maxOpenPreparedStatements:0);
		this.clearStatementPoolOnReturn = builder.clearStatementPoolOnReturn;
		this.defaultTransactionIsolation = builder.defaultTransactionIsolation;
		this.initialSize = builder.initialSize;
		this.maxTotal = Math.max(builder.maxTotal,maxIdle);
		this.defaultAutoCommit=builder.defaultAutoCommit;
		this.defaultReadOnly=builder.defaultReadOnly;
		this.schema=builder.defaultSchema;
		
		this.pool = new ArrayBlockingQueue<>(maxTotal, true);
		this.h= ses.scheduleWithFixedDelay(this::clean,60000,60000,TimeUnit.MILLISECONDS);
	}
	/**Create a builder with url, user and password.*/
	public static HydarDataSource of(String url, String user, String password) throws SQLException {
		return builder().url(url).username(user).password(password).build();
	}
	/**Set builder properties from the given Properties(uses reflection).*/
	public static HydarDataSource of(Properties settings) throws SQLException {
		return builder().properties(settings).build();
	}
	/**Return a new Builder.*/
	public static Builder builder() {
		return new Builder();
	}
	/**
	 * Initialization step - add new pooled connections.
	 * We call getConnection() or getConnection(user, password)
	 * depending on if user==null/empty.
	 * Reusing credentials is preferred.
	 * */
	public void start() throws SQLException {
		for(int i=0;i<initialSize;i++)
			pool.add(new PooledConn((username!=null&&username.length()>0),username, password, false));
	}
	/**
	 * Runs periodically to remove extra idle connections
	 * and connections that have been active for too long
	 * (most likely an application forgot to close)
	 * */
	private void clean() {
		lock.lock();
		try {
			long now = System.currentTimeMillis();
			//deactivate connections that have been active for too long
			for(var entry:actives.values()) {
				if(entry.activeSince < now - 600_000) {
					dispose(entry.pc);
				}
			}
			//lower amount of idle connections
			float extra = LOAD_FACTOR * (pool.size() + actives.size() - minIdle);
			if(extra<=0f)
				return;
			var rng=ThreadLocalRandom.current();
			extra += rng.nextFloat()>(extra-(int)extra)?0:1;
			for(int i=(int)extra;i>0;i--) {
				PooledConn pc = pool.poll();
				if(pc==null)
					break;
				pc.close();
			}
		}finally {
			lock.unlock();
		}
	}
	/**
	 * Close the entire pool.
	 * */
	@Override
	public void close() {
		closed=true;
		h.cancel(true);
		lock.lock();
		try {
			pool.forEach(x->x.close());
		}finally {
			lock.unlock();
			pools.remove(this);
		}
	}
	/**
	 * Add the given PooledConn to the active pool.
	 * Active pool is needed to clean up resources properly.
	 * If it was already there, throws an exception.
	 * */
	private void activateChecked(PooledConn pc) {
		pc.activeSince=System.currentTimeMillis();
		if(actives.putIfAbsent(pc.pc, pc)!=null)
			throw new RuntimeException("ACTIVE DUPLICATE");
	}
	/**
	 * Remove a connection from the active pool, 
	 * and return the underlying PooledConn.
	 * */
	private PooledConn deactivateChecked(PooledConnection pc) {
		var pooledConn=actives.remove(pc);
		if(pooledConn==null)
			throw new RuntimeException("MISSING CONNECTION");
		return pooledConn;
	}
	/**
	 * Destroy a connection, usually caused by errors
	 * or if too many idles.
	 * */
	private void dispose(PooledConnection conn) {
		var pc = deactivateChecked(conn);
		if(pc!=null)pc.close();
	}
	/**call getConnection() or getConnection(user, password)
	 * depending on if user==null/empty.
	 * Reusing credentials is preferred.*/
	@Override
	public Connection getConnection() throws SQLException {
		return getConn_(false, username, password);
	}
	/**We can't change the user name or password. 
	 * It might return brand new(non-pooled)
	 * connections instead of failing in the future.
	 * */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		if(this.username!=null && !(username.equals(this.username)&&password.equals(this.password)))
			throw new SQLException("Cannot change the username.");
		return getConn_(true, username, password);
	}
	private Connection getConn_(boolean login, String username, String password) throws SQLException{
		var conn= getConn(login,username,password);
		if(defaultTransactionIsolation.isPresent())
			conn.setTransactionIsolation(defaultTransactionIsolation.orElseThrow());
		if(schema!=null)
			conn.setSchema(schema);
		if(!defaultAutoCommit)
			conn.setAutoCommit(defaultAutoCommit);
		if(defaultReadOnly)
			conn.setReadOnly(defaultReadOnly);
		return conn;
	}
	private Connection getConn(boolean login, String username, String password) throws SQLException{
		//try {
			//System.out.write(("Hydar JDBC pool: size "+pool.size()+" active "+actives.size()+"\n").getBytes());
		//} catch (IOException e1) {}
		lock.lock();
		try {
			long remainingMillis=maxWaitMillis;
			long startTime=System.currentTimeMillis();
			while(pool.size()+actives.size()>=maxTotal) {
				try {
					PooledConn pc = pool.poll(remainingMillis,TimeUnit.MILLISECONDS);
					remainingMillis = maxWaitMillis -  (System.currentTimeMillis()-startTime);
					if(pc==null)
						break;
					activateChecked(pc);
					var conn=pc.getConnection();
					//if getConnection causes error
					if(!actives.containsKey(pc.pc))
						continue;
					return conn;
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch(SQLRecoverableException e) {
					//if one of the connections was dropped
					continue;
				}
			}
			if(actives.size()>=maxTotal) {
				throw new SQLException("No connections available");
				//return new PooledConn(login, username, password, true).getConnection();
			}return newConn(login, username, password);
		}finally {
			lock.unlock();
		}
	}
	private Connection newConn(boolean login, String username, String password) throws SQLException{
		var pc=new PooledConn(login,username,password, false);
		activateChecked(pc);
		return pc.getConnection();
	}
	private void recycle(PooledConnection conn) {
		//conn.addConnectionEventListener(l);
	//	conn.addStatementEventListener(sel);
		lock.lock();
		try {
			if(closed)
				dispose(conn);
			else {
				pool.add(deactivateChecked(conn));
			}
		}finally {
			lock.unlock();
		}
	}
	protected abstract PooledConnection newPC(boolean login,String username, String password) throws SQLException;
	protected static PooledConnection physicalHandle(Connection main) {
		final Set<ConnectionEventListener> connL=new HashSet<>();
		final Set<StatementEventListener> stmtL=new HashSet<>();
		
		return new PooledConnection() {
			@Override public void addConnectionEventListener(ConnectionEventListener listener) {connL.add(listener);}
			
			@Override public void addStatementEventListener(StatementEventListener listener) {stmtL.add(listener);}
			@Override
			public void close() throws SQLException {
				try {
					connL.forEach(x->x.connectionClosed(new ConnectionEvent(this)));
				}catch(Exception e) {}
				main.close();
			}
			private void onClose() throws SQLException{
				if(connL.size()>0) {
					var ce=new ConnectionEvent(this);
					connL.forEach(x->x.connectionClosed(ce));
				}
			}
			private void onError(SQLException se) throws SQLException{
				if(connL.size()>0) {
					var ce=new ConnectionEvent(this,se);
					connL.forEach(x->x.connectionErrorOccurred(ce));
				}
			}
			@Override
			public Connection getConnection() throws SQLException {
				//just used for mutability :skull
				final AtomicBoolean closed=new AtomicBoolean();
				main.beginRequest();
				return (Connection) Proxy.newProxyInstance(HydarDataSource.class.getClassLoader(),
						CONNECTION,
						(proxy,meth,args)->{
								if(meth.getName().equals("close")) {
									if(!closed.getOpaque()){
										closed.setOpaque(true);
										main.endRequest();
										onClose();
									}
									return null;
								}else if(!closed.getOpaque()){
									try {
										return meth.invoke(main,args);
									}catch(Exception e) {
										e.printStackTrace();
										if(e instanceof SQLException se)
											onError(se);
										else 
											onClose();
									}
									return null;
								}
							throw new SQLException("Already closed");
						});
			}
			@Override public void removeConnectionEventListener(ConnectionEventListener listener) { connL.remove(listener);}
			@Override public void removeStatementEventListener(StatementEventListener listener) { stmtL.remove(listener);}
		};
	}
	private class PooledConn implements AutoCloseable{
		private final Map<PreparedStatement,String> psActive = new HashMap<>();
		private final PooledConnection pc;
		private final StatementEventListener sel;
		private volatile long activeSince;
		private final boolean autoclose;
		private final Map<String,PreparedStatement> psIdle = 
		maxOpenPreparedStatements==0?Map.of():new LinkedHashMap<>() {
			private static final long serialVersionUID = 1L;
			protected boolean removeEldestEntry(Map.Entry<String,PreparedStatement> eldest) {
				if(size()>maxOpenPreparedStatements) {
					try {eldest.getValue().close();}catch(SQLException e) {}
					return true;
				}
				return false;
			};
		};
		public PooledConn(boolean login, String username, String password, boolean autoclose) throws SQLException {
			pc=newPC(login,username,password);
			this.autoclose=autoclose;
			sel=new StatementEventListener() {
				@Override
				public void statementClosed(StatementEvent event) {
					psActive.remove(event.getStatement());
				}
				
				@Override
				public void statementErrorOccurred(StatementEvent event) {
					psActive.remove(event.getStatement());
				}
			};
			if(!autoclose)
				pc.addConnectionEventListener(l);
			pc.addStatementEventListener(sel);
		}
		//called on dispose
		@Override
		public void close() {
			psIdle.values().removeIf(x->{try {x.close();} catch (SQLException e) {} return true;});
			psActive.keySet().removeIf(x->{try {x.close();} catch (SQLException e) {} return true;});
			pc.removeStatementEventListener(sel);
			if(!autoclose)
				pc.removeConnectionEventListener(l);
			try {pc.close();} catch (SQLException e) {}
		}
		public Connection getConnection() throws SQLException {
			final Connection tmp = pc.getConnection();
			return (Connection) Proxy.newProxyInstance(HydarDataSource.class.getClassLoader(),
					CONNECTION,
					(proxy, meth, args) -> {
						String name = meth.getName();
						if (name.equals("prepareStatement")) {
							String key =args.length==1?(String)args[0]:Arrays.deepToString(args);
							var ps = psIdle.remove(key);
							boolean isClosed=true;
							if(ps!=null)
								try {isClosed=ps.isClosed();}catch(SQLException e) {}
							if(ps==null || isClosed)
								ps = args.length==1?
									tmp.prepareStatement((String)args[0]):
									(PreparedStatement)meth.invoke(tmp,args);
							psActive.put(ps, key);
							return ps;
						} else if (name.equals("close")) {
							for(var entry:psActive.entrySet().stream().toList()) {
								var k=entry.getKey();
								try {
									if(!k.isClosed()&&(clearStatementPoolOnReturn || 
										psIdle.putIfAbsent(entry.getValue(),k)!=null)){
										k.close();
									}
								} catch (SQLException e) {
									//isClosed can throw this sometimes
								}
							}
							psActive.clear();
							tmp.close();
							if(autoclose)
								this.close();
							return null;
						} else {
							return meth.invoke(tmp, args);
						}
					});
			
		}
	}
	public static final class Builder {
		private static final List<String> KEYS;
		static{
			var NOT_KEYS = List.of("ds","driverClassLoader","properties","KEYS");
			KEYS= Stream.concat(Arrays.stream(Builder.class.getDeclaredFields())
					.map(Field::getName)
					.filter(x->!NOT_KEYS.contains(x))
						,Stream.of("name","user"))
					.toList();
		}
			
		private String username;
		private String password;
		private String url;
		private String driver;
		private int initialSize=2;
		private int minIdle=2;
		private int maxIdle=8;
		private int maxTotal=8;
		private int maxWaitMillis=15000;
		private int maxOpenPreparedStatements=32;
		private boolean poolPreparedStatements=true;
		private boolean defaultAutoCommit=true;
		private boolean defaultReadOnly=false;
		private String defaultSchema=null;
		
		private boolean clearStatementPoolOnReturn=false;
		private OptionalInt defaultTransactionIsolation=OptionalInt.empty();
		private String factory;
		private String type;
		private ClassLoader driverClassLoader;
		private CommonDataSource ds;
		private Properties properties=new Properties();
		private Builder() {}
		public HydarDataSource build() throws SQLException {
			if(ds!=null) {
				return ds instanceof ConnectionPoolDataSource pds?new Forwarding.FromPooledDS(this):new Forwarding.FromDS(this);
			}if(factory!=null && type!=null) {
				try {
					if(url!=null)
						properties.setProperty("url", url);
					Object ds = HydarUtil.getObject(factory,type,properties);
					if(ds instanceof ConnectionPoolDataSource pds) {
						this.ds=pds;
						System.out.printf("Resource loader: Creating connection pool (maxIdle=%d, maxTotal=%d, maxWaitMillis=%d)...\n",maxIdle,maxTotal,maxWaitMillis);
						return new Forwarding.FromPooledDS(this);
					}else if(ds instanceof DataSource ds_) {
						this.ds=ds_;
						return new Forwarding.FromDS(this);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				//datasource
			}else if(url!=null){
				if(driver!=null) {
					try {
						((driverClassLoader==null)?Class.forName(driver):Class.forName(driver,true,driverClassLoader))
							.toString();
					} catch (ClassNotFoundException e) {
						throw new IllegalArgumentException("Invalid driver class", e);
					}
				}
				return new Forwarding.FromDriver(this);
			}
			return null;
		}
		public Builder clearStatementPoolOnReturn(boolean clearStatementPoolOnReturn) {
			this.clearStatementPoolOnReturn=clearStatementPoolOnReturn;
			return this;
		}
		public Builder dataSource(DataSource ds) {
			this.ds = ds;
			return this;
		}
		public Builder dataSourceFactoryClassName(String name) {
			this.factory = name;
			return this;
		}
		public Builder dataSourceTypeName(String name) {
			this.type = name;
			return this;
		}
		public Builder defaultAutoCommit(boolean defaultAutoCommit) {
			this.defaultAutoCommit=defaultAutoCommit;
			return this;
			
		}
		public Builder defaultReadOnly(boolean defaultReadOnly) {
			this.defaultReadOnly=defaultReadOnly;
			return this;
			
		}
		public Builder defaultSchema(String defaultSchema) {
			this.defaultSchema=defaultSchema;
			return this;
		}
		public Builder defaultTransactionIsolation(int defaultTransactionIsolation) {
			this.defaultTransactionIsolation=OptionalInt.of(defaultTransactionIsolation);
			return this;
		}
		public Builder driverClass(String driver) {
			this.driver=driver;
			return this;
		}
		public Builder driverClassLoader(ClassLoader driverClassLoader) {
			this.driverClassLoader=driverClassLoader;
			return this;
		}
		public Builder initialSize(int initialSize) {
			this.initialSize = initialSize;
			return this;
		}
		public Builder maxIdle(int max) {
			this.maxIdle = max;
			return this;
		}
		public Builder maxOpenPreparedStatements(int maxOpenPreparedStatements) {
			this.maxOpenPreparedStatements = maxOpenPreparedStatements;
			return this;
		}
		public Builder maxTotal(int max) {
			this.maxTotal = max;
			return this;
		}
		public Builder maxWaitMillis(int maxWaitMillis) {
			this.maxWaitMillis = maxWaitMillis;
			return this;
		}
		public Builder minIdle(int min) {
			this.minIdle = min;
			return this;
		}public Builder password(String password) {
			this.password = password;
			return this;
		}public Builder poolPreparedStatements(boolean poolPreparedStatements) {
			this.poolPreparedStatements=poolPreparedStatements;
			return this;
			
		}
		public Builder properties(Map<String,String> table) {
			var prop=new Properties();
			prop.putAll(table);
			return properties(prop, false);
		}
		public Builder properties(Properties prop) {
			return properties(prop,true);
		}
		private Builder properties(Properties prop, boolean copy) {
			if(copy) {
				Properties prop_ = new Properties();
				prop.stringPropertyNames().forEach(k->prop_.setProperty(k, prop.get(k).toString()));
				this.properties=prop_;
			}else this.properties=prop;
			for(String k:KEYS) {
				if(!properties.containsKey(k) && !k.equals("name"))
					try {
						var v=getClass().getDeclaredField(k).get(this);
						if(v!=null)
							properties.setProperty(k,v.toString());
					}catch (Exception e) {
						continue;
					}
			}
			dataSourceFactoryClassName(properties.getProperty("factory"));
			dataSourceTypeName(properties.getProperty("type"));
			poolPreparedStatements(Boolean.parseBoolean(properties.getProperty("poolPreparedStatements")));
			maxOpenPreparedStatements(Integer.parseInt(properties.getProperty("maxOpenPreparedStatements")));
			clearStatementPoolOnReturn(Boolean.parseBoolean(properties.getProperty("clearStatementPoolOnReturn")));
			maxWaitMillis(Integer.parseInt(properties.getProperty("maxWaitMillis")));
			maxIdle(Integer.parseInt(properties.getProperty("maxIdle")));
			initialSize(Integer.parseInt(properties.getProperty("initialSize")));
			maxTotal(Integer.parseInt(properties.getProperty("maxTotal")));
			minIdle(Integer.parseInt(properties.getProperty("minIdle")));
			defaultAutoCommit(Boolean.parseBoolean(properties.getProperty("defaultAutoCommit")));
			defaultReadOnly(Boolean.parseBoolean(properties.getProperty("defaultReadOnly")));
			defaultSchema(properties.getProperty("schema"));
			if(properties.getProperty("user")!=null)
				username(properties.getProperty("user"));
			else
				username(properties.getProperty("username"));
			password(properties.getProperty("password"));
			url(properties.getProperty("url"));
			driverClass(properties.getProperty("driver"));
			var tiStr=properties.getProperty("defaultTransactionIsolation");
			if(!OptionalInt.empty().toString().equals(tiStr))
				defaultTransactionIsolation(Integer.parseInt(tiStr));
			/**
			this.properties = properties.entrySet().stream()
					.collect(toUnmodifiableMap(x->{
						var k=x.getKey().toString();
						return KEYS.contains(k.replace("\"",""))?k.replace("\"",""):k;
					},x->x.getValue().toString()));*/
			for(var key:properties.stringPropertyNames()) {
				if(KEYS.contains(key)||key.equals("name")) {
					properties.remove(key);
				}else {
					String newKey=key.replace("\"","");
					Object v;
					if(KEYS.contains(newKey) && (v=properties.remove(key))!=null) {
						properties.setProperty(newKey,v.toString());
					}
				}
			}
			return this;
		}
		public Builder url(String url) {
			this.url = url;
			return this;
		}
		
	
		public Builder username(String username) {
			this.username = username;
			return this;
		}
	}
	static abstract class Forwarding extends HydarDataSource{
		protected final CommonDataSource ds;
		public Forwarding(Builder builder) throws SQLException {
			super(builder);
			this.ds=builder.ds;
		}
		static class FromDriver extends HydarDataSource.Forwarding{
			final String url;
			final Properties properties;
			public FromDriver(HydarDataSource.Builder builder) throws SQLException {
				super(builder.dataSource(HydarDataSource.NULL_DATA_SOURCE));
				this.url=builder.url;
				if(builder.properties.size()>0) {
					properties=builder.properties;
					if(username!=null) {
						properties.setProperty("user",username);
						if(password!=null)
							properties.setProperty("password",password);
					}
				}else properties=null;
				start();
			}
			@Override
			protected PooledConnection newPC(boolean login, String username, String password) throws SQLException {
				Connection conn;
				if(properties!=null) {
					Properties props=properties;
					if(login) {
						props=new Properties(properties);
						if(username!=null) {
							props.setProperty("user",username);
							if(password!=null)props.setProperty("password",password);
						}
					}
					conn=DriverManager.getConnection(url, props);
				}else conn=
						login?
						DriverManager.getConnection(url, this.username, this.password):
						DriverManager.getConnection(url,username,password);
				return HydarDataSource.physicalHandle(conn);
			}
		}
		static class FromDS extends HydarDataSource.Forwarding{
			public FromDS(HydarDataSource.Builder builder) throws SQLException {
				super(builder);
				start();
			}
			@Override
			protected PooledConnection newPC(boolean login, String username, String password) throws SQLException {
				if(ds instanceof DataSource pds)
					return HydarDataSource.physicalHandle(!login?pds.getConnection():pds.getConnection(username, password));
				else throw new SQLException("invalid");
			}
		}
		static class FromPooledDS extends HydarDataSource.Forwarding{
			public FromPooledDS(HydarDataSource.Builder builder) throws SQLException {
				super(builder);
				start();
			}
			@Override
			protected PooledConnection newPC(boolean login, String username, String password) throws SQLException {
				if(ds instanceof ConnectionPoolDataSource pds)
					return !login?pds.getPooledConnection():pds.getPooledConnection(username, password);
				else throw new SQLException("invalid");
			}
		}
		@Override
		public int getLoginTimeout() throws SQLException {return ds.getLoginTimeout();}
		@Override
		public PrintWriter getLogWriter() throws SQLException {return ds.getLogWriter();}
		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {return ds.getParentLogger();}
		@Override
		public void setLoginTimeout(int seconds) throws SQLException {ds.setLoginTimeout(seconds);}
		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {ds.setLogWriter(out);}
		
	}
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface!=null&&iface.isInstance(this);
	}
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if(isWrapperFor(iface))
			return iface.cast(this);
		throw new SQLException("Not a proxy for that class");
	}
}
