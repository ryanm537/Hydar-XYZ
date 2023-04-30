package xyz.hydar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.zip.CRC32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import xyz.hydar.server.HydarUtil;
//realms are not checked + peers cannot be remote and cant receive data(virtual peers
//hmacsha256/other recent features not implemented
//reservations/dont fragment are ignored
public class HydarTURN implements AutoCloseable{
	public final InetAddress ip;
	public final UDPHydarStunInstance udp_instance;
	public final TCPHydarStunInstance tcp_instance;
	public final Function<String,String> auth;
	public static final boolean SHOULD_FINGERPRINT=false;
	/**
	 * Wrapper for STUN attributes.
	 * Each attribute has a type and data array and can be encoded as:
	 * (2 bytes)type (2 bytes) length (? bytes) data
	 * Can be read from a stream or converted to a byte array.
	 * The payload must be padded to a multiple of 4 bytes, without affecting the length.
	 */
	static class Attr{
		// STUN attributes
		public static final short MAPPED_ADDRESS = (short) 0x01;
		public static final short USERNAME = (short) 0x06;
		public static final short MESSAGE_INTEGRITY = (short) 0x08;
		public static final short ERROR_CODE = (short) 0x09;
		public static final short UNKNOWN_ATTRIBUTES = (short) 0x0A;
		public static final short REALM = (short) 0x14;
		public static final short NONCE = (short) 0x15;
		public static final short XOR_MAPPED_ADDRESS = (short) 0x20;
		public static final short MESSAGE_INTEGRITY_SHA256 = (short) 0x1C;
		public static final short PASSWORD_ALGORITHM = (short) 0x1D;
		public static final short USERHASH = (short) 0x1E;
		// TURN attributes
		public static final short CHANNEL_NUMBER = (short) 0x0C;
		public static final short LIFETIME = (short) 0x0D;
		public static final short XOR_PEER_ADDRESS = (short) 0x12;
		public static final short DATA = (short) 0x13;
		public static final short XOR_RELAYED_ADDRESS = (short) 0x16;
		public static final short EVEN_PORT = (short) 0x18;
		public static final short REQUESTED_TRANSPORT = (short) 0x19;
		public static final short DONT_FRAGMENT = (short) 0x1A;
		public static final short RESERVATION_TOKEN = (short) 0x22;
		// TURN - comprehension optional
		public static final short FINGERPRINT = (short) 0x8028;
		/**
		 * Attribute format:
		 * (2 bytes)type (2 bytes) length (? bytes) data
		 * The payload must be padded to a multiple of 4 bytes, without affecting the length.
		 */
		private Attr() {}
		public static int byteLength(int length) {
			int pad = (length % 4 == 0 ? 0 : (4 - length % 4));
			return length+pad+4;
		}
		public static void write(short type, byte[] data, ByteBuffer b) {
			int pad = (data.length % 4 == 0 ? 0 : (4 - data.length % 4));
			b.putShort(type)
			.putShort((short) data.length)
			.put(data)
			.position(b.position()+pad);
		}
	}
	/**
	 * This class represents a STUN packet
	 * askjfdslgk
	 */
	public class Packet {
		// 0b00 req 0b01 indic 0b10 resp 0b11 error
		public final int messageClass;
		// 12 bits :skull
		public final int messageType;
		// length of attributes only
		public short forcedLength=-1;
		public short length;
		//mask used for XOR'd addresses
		public final byte[] mask;
		public final byte[] transaction;// byte[12]
		// message classes
		public static final int REQUEST = 0;
		public static final int INDICATION = 1;
		public static final int RESPONSE = 2;
		public static final int ERROR = 3;
		// STUN methods
		public static final short BINDING = (short) 0x01;
		// TURN methods
		public static final short ALLOCATE = (short) 0x03;
		public static final short REFRESH = (short) 0x04;
		public static final short SEND = (short) 0x06;
		public static final short DATA = (short) 0x07;
		public static final short CREATEPERMISSION = (short) 0x08;
		public static final short CHANNELBIND = (short) 0x09;
		//fake
		public static final short CHANNELDATA = (short) 0xee;
	
		public volatile int error = 0;
		public final ArrayList<byte[]> xorPeers= new ArrayList<>();
		public final LinkedHashMap<Short, byte[]> attributes= new LinkedHashMap<>();
		//problem: attributes are out of order
		public Packet(int messageClass, int messageType, byte[] transaction) {
			this.messageClass = messageClass;
			this.messageType = messageType;
			this.transaction = transaction;
			mask=ByteBuffer.allocate(16)
				.putInt(0x2112A442)
				.put(transaction)
				.array();
		}
		public void forceLength(short l) {
			this.forcedLength=l;
		}
		public void copy(Packet s, short type) {
			setAttribute(type,s.getAttribute(type));
		}
	
		public byte[] getAttribute(short type) {
			return attributes.get(type);
		}
	
		public void setAttribute(short type,byte[] value) {
			if(type==Attr.XOR_PEER_ADDRESS)
				xorPeers.add(value);
			byte[] old=attributes.get(type);
			if (old != null)
				length -= Attr.byteLength(old.length);
			attributes.put(type, value);
			length += Attr.byteLength(value.length);
		}
		public static byte[] wrapInt(int h) {
			return ByteBuffer.allocate(4).putInt(h).array();
		}
		public static byte[] wrapShort(short h) {
			return new byte[] {(byte)(((h) & 0xFF00) >> 8),(byte)((h) & 0xFF)};
		}
		public static short getShort(byte[] input) {
			return (short)(((input[0]&0xff)<<8)+(input[1]&0xff));
		}
		public static int getInt(byte[] input) {
			return ByteBuffer.wrap(input).getInt();
		}
		
		/**
		 * Static utility to get message class(int) from the first 2 bytes of a STUN packet.
		 */
		public static int cl(byte[] t){
		    return ((t[0]&0b1)<<1) | ((t[1]&0x10)>>4);
	    	}
		/**
		 * Static utility to get message type(int) from the first 2 bytes of a STUN packet.
		 */
		public static int ty(byte[] t){
		    return (t[1]&0b1)|
			    (t[1]&0b10)|
			    (t[1]&0b100)|
			    (t[1]&0b1000)|
				((t[1]&0b100000)>>1)|
				((t[1]&0b1000000)>>1)|
				((t[1]&0b10000000)>>1)|
				((t[1]&0b100000000)>>1)|
				((t[0]&0b10)<<7)|
				((t[0]&0b100)<<7)|
				((t[0]&0b1000)<<7)|
				((t[0]&0b10000)<<7)|
				((t[0]&0b100000)<<7);
	    	}
		/**
		 * Static utility to create the first 2 bytes of a STUN packet given a class and type.
		 */
		public static byte[] type(int c, int t){
		    return new byte[]{(byte)(
				((c&0b10)>>1) |
				((t&0b10000000)>>6) |
				((t&0b100000000)>>6) |
				((t&0b1000000000)>>6) |
				((t&0b10000000000)>>6) |
				((t&0b100000000000)>>6)),(byte)((t&0b1) |
				(t&0b10) |
				(t&0b100) |
				(t&0b1000) |
				((c&0b1)<<4) |
				((t&0b10000)<<1) |
				((t&0b100000)<<1) |
				((t&0b1000000)<<1))};
	    	}
		public int byteLength() {
			return 20+length;
		}
		public byte[] toByteArray() {
			var out=ByteBuffer.allocate(20+length)
					.put(type(messageClass,messageType))
					.putShort(forcedLength<0?length:forcedLength)
					.putInt(0x2112A442)
					.put(transaction);
			attributes.forEach((k,v)->Attr.write(k,v,out));
			return out.array();
		}
		@Override
		public String toString() {
			return HexFormat.of().formatHex(toByteArray());
		}
		public String toStringV() {
			return "Packet Class="+messageClass+" Type= "+messageType+"Attributes: "+attributes.values();
		}
	
		public String toBinary() {
			return new BigInteger(this.toByteArray()).toString(2);
		}
	
		public void setMi() {
			try {
				forceLength((short)(this.length+24));
				String un = new String(getAttribute(Attr.USERNAME)).trim();
				String realm = new String(getAttribute(Attr.REALM)).trim();
				String password = authenticate(un);// can change later(check username in hashtable)
	
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] key = (un + ":" + realm + ":" + password).getBytes(StandardCharsets.UTF_8);
				md.update(key);
				byte[] digest = md.digest();
				setAttribute(Attr.MESSAGE_INTEGRITY, mi("HmacSHA1", digest));
				forceLength((short)-1);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				forceLength((short)-1);
				return;
			}
		}
		public void setFp() {
			if(HydarTURN.SHOULD_FINGERPRINT) {
				forceLength((short)(this.length+8));
				CRC32 md = new CRC32();
				byte[] hydr = toByteArray();
				md.update(hydr);
				int digest = (int)md.getValue();
				digest^=0x5354554e;
				setAttribute(Attr.FINGERPRINT,wrapInt(digest));
				forceLength((short)-1);
			}
		}
		public boolean checkMi(byte[] mi_) {
				String un = new String(getAttribute(Attr.USERNAME)).trim();
				String realm = new String(getAttribute(Attr.REALM)).trim();
				try {
					String password = authenticate(un);
	
					// System.out.println("h"+this);
					MessageDigest md = MessageDigest.getInstance("MD5");
					md.update((un + ":" + realm + ":" + password).getBytes(StandardCharsets.UTF_8));
					byte[] digest = md.digest();
					byte[] m = mi("HmacSHA1", digest);
					if (Arrays.equals(m, mi_))
						return true;
	
				} catch (Exception e) {// NoSuchAlgorithmException | UnsupportedEncoding
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return false;
			
		}
	
		public static byte[] wrapError(int errorCode) {
			byte c1 = (byte) (errorCode / 100);
			byte c2 = (byte) (errorCode % 100);
			return ByteBuffer.allocate(4)
					.putShort((short) 0)
					.put((byte) (c1 & 0b111))
					.put(c2)
					.array();
		}
	
		public byte[] mi(String algorithm, byte[] key) {
			try {
				SecretKeySpec secretKeySpec = new SecretKeySpec(key, algorithm);
				Mac mac = Mac.getInstance(algorithm);
				mac.init(secretKeySpec);
				return mac.doFinal(toByteArray());
			} catch (InvalidKeyException | NoSuchAlgorithmException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	public class Client {
		public static Map<Client, Nonce> nonces = new ConcurrentHashMap<>();
		public static Map<Client, Allocation> alloc = new ConcurrentHashMap<>(99);
		public Nonce nonce;// used
		// 5-tuple
		// client addr client port
		// server addr server port
		// transport protocol
		
		// ===========================
		public final InetAddress client;
		public final int clientPort;
		public final InetAddress server;
		public final int serverPort;
		public final int protocol;
		// ==========================
		// pointer to the allocation
		public volatile Allocation allocation;
		//protocols
		public static final short UDP = 0;
		public static final short TCP = 1;
		public static final short TCP_OVER_TLS = 2;
		public Client(DatagramSocket s, DatagramPacket d) {
			server = ip;
			serverPort = s.getPort();
			client = d.getAddress();
			clientPort = d.getPort();
			protocol = UDP;
			allocation = null;
			if(nonces.get(this)==null) {
				nonces.put(this, new Nonce(this));
				
			}this.nonce = nonces.get(this);
		}
	
		public Client(InetAddress client, int clientPort, int serverPort) {
			server = ip;
			this.serverPort = serverPort;
			this.client = client;
			this.clientPort = clientPort;
			protocol = TCP;
			allocation = null;
			if(nonces.get(this)==null) {
				nonces.put(this, new Nonce(this));
				
			}this.nonce = nonces.get(this);
		}
	
		public void setAllocation(Allocation allocation) {
			this.allocation = allocation;
			alloc.put(this, allocation);
		}
	
		// new constructor for TCP/TCP over TLS/DTLS over UDP
		@Override
		public int hashCode() {
			return (client.hashCode()*31+clientPort)*31+protocol;
		}
		@Override
		public boolean equals(Object o) {
			return (o instanceof Client c)
					&& client.equals(c.client)
					&& clientPort == c.clientPort && protocol == c.protocol;
		}
		@Override
		public String toString() {
			return ""+client+":"+clientPort+"?"+protocol;
		}
		
	
		public byte[] xor(Packet s) {
			return xorAddress(client.getAddress(), s.mask, clientPort);
		}
		public byte[] xorRelay(Packet s) {
			var a = alloc.get(this);
			return xorAddress(ip.getAddress(), s.mask, a.port);
		}
		static byte[] xorAddress(byte[] addr, byte[] mask, int port) {
			int len=addr.length;
			boolean ipv4 = (len == 4);
			byte[] p = new byte[len + 4];
			p[0] = 0x00;
			p[1] = (byte) (ipv4?0x01:0x02);
			p[2] = (byte) ((((port) & 0xFF00) >> 8) ^ 0x21);
			p[3] = (byte) (((port) & 0xFF) ^ 0x12);
			for (int i = 0; i < len; i++)
				p[4 + i] = (byte) (addr[i] ^ mask[i]);
			return p;
		}
	}
	public abstract class Expireable{
		//private static Set<Expireable> set = null;
		public static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
		
		//init-> start the Runnable if its null ig
		public final AtomicInteger ttl;
		public volatile boolean alive;
		public Expireable(int ttl) {
			timer.schedule(new TURNUpdateTask(this), 1000,TimeUnit.MILLISECONDS);
			this.ttl = new AtomicInteger(ttl);
			alive = true;
		}
		public void set(int ttl) {
			timer.schedule(new TURNUpdateTask(this,ttl), 250,TimeUnit.MILLISECONDS);
		}
		public HydarStunInstance getInstance(Client c) {
			return HydarTURN.this.getInstance(c);
		}
		public abstract void kill();
		//@Override
		//public abstract void run();
	}
	public class Allocation extends HydarTURN.Expireable{
		// 5-tuple
		public final Client client;
		
		// transport address
		public final DatagramSocket server;
		public final int port;
		public final Map<Short, TURNChannel> channels=new ConcurrentHashMap<>();
		public final Map<Client, Permission> permissions=new ConcurrentHashMap<>();
		public volatile static int nextPort = 32400;
		// first packet, stores username password realm nonce
		//username in allocation maybe hydar
		/**@TODO 
		 * All requests after the initial Allocate must use the same username as
		   that used to create the allocation
		   */
		public boolean bind(short num, Client c) {
			if(num<16384)
				return false;
			if(Client.alloc.get(c)==null)
				return false;
			if(channels.get(num)!=null) {
				if(!channels.get(num).peer.equals(c))
					return false;
				else {
					channels.get(num).ttl.set(600);
					Client.alloc.get(c).createPerm(this.client);
					this.createPerm(c);
					return true;
				}
			}
			int failed=0;
			for(TURNChannel ch:channels.values()){
				if(ch.peer.equals(c)) {
					if(ch.number!=num)
						failed++;
					else {
						ch.ttl.set(600);
						this.createPerm(c);
						Client.alloc.get(c).createPerm(this.client);
						failed=-999;
					}
				}
			}
			if(failed>0)
				return false;
			if(failed<0)
				return true;
			TURNChannel channel = new TURNChannel(num,client,c);
			channels.put(num, channel);
			this.createPerm(c);
			Client.alloc.get(c).createPerm(this.client);
			return true;
			//The channel number is not currently bound to a different transport
			//The transport address is not currently bound to a different channel number
			
		}
		public boolean createPerm(Client peer) {
			System.out.println("Creating perm "+client+" -> "+peer);
			//System.out.println(permissions);
			this.ttl.getAndUpdate((x)->Math.min(3600,x+300));
			if(permissions.get(peer)!=null) {
				
				permissions.get(peer).ttl.set(300);
				return true;
			}
			if(Client.alloc.get(peer)==null)
				return false;
			Permission perm = new Permission(client, peer);
			permissions.put(peer, perm);
			return true;
		}
		public Allocation(Client c, int ttl) {
			super(ttl);
			try {
				nextPort += 2;
				if(nextPort>65000)
					nextPort=32400;
				port = nextPort;
				this.server = new DatagramSocket(port);
				this.server.setReceiveBufferSize(2000);
				this.server.setSoTimeout(5000);
				c.setAllocation(this);
				this.client=c;
				//this.server.connect(c.client,c.clientPort);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		public void write(byte[] data){
			try {	
				//System.out.println("sending "+HexFormat.of().formatHex(data)+" to "+client.client+":"+client.clientPort);
				
				this.server.send(new DatagramPacket(data,data.length,client.client,client.clientPort));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void kill() {
			server.close();
			if(client!=null) {
				Client.alloc.remove(client);
				client.allocation=null;
			}else System.out.println(Client.alloc);
			this.alive=false;
			for(Permission s:permissions.values())
				s.ttl.set(0);
			for(TURNChannel c:channels.values())
				c.ttl.set(0);
		}
	}
	public class Permission extends HydarTURN.Expireable{
		public final Client peer;
		public final Client client;
		public Permission(Client client, Client peer) {
			super(300);
			this.peer=peer;
			this.client=client;
		}
		@Override
		public void kill() {
			if(Client.alloc.get(client)!=null)
				Client.alloc.get(client).permissions.remove(peer);
			this.alive=false;
		}
		@Override
		public String toString() {
			return (""+client+"->"+peer+"["+ttl+"]");
		}
	}
	public class Nonce extends HydarTURN.Expireable{
		public final Client client;
		public final byte[] nonce;
		public Nonce(Client c) {
			super(3600);
			this.client=c;
			this.nonce = HydarUtil.noise(16).getBytes();
		}
		@Override
		public void kill() {
			//Client.nonces.put(client, new Nonce(client));
			Client.nonces.remove(client);
			client.nonce=Client.nonces.get(client);
			this.alive=false;
		}
		@Override
		public String toString() {
			return new String(nonce);
		}
	}
	public class TURNUpdateTask implements Runnable{
		final HydarTURN.Expireable e;
		final int ttl;
		public TURNUpdateTask(HydarTURN.Expireable e) {
			this(e,-1);
		}
		public TURNUpdateTask(HydarTURN.Expireable e, int ttl) {
			this.e=e;
			this.ttl=ttl;
		}
		@Override
		public void run() {
			//System.out.println(""+e.getClass().getCanonicalName()+"\t"+e.ttl.get());
			if(ttl==-1) {
				e.ttl.decrementAndGet();
			}else e.set(ttl);
			if(e.ttl.get()<=0) {
				e.alive=false;
			}
			
			if(e.alive){
				if(ttl==-1)
					Expireable.timer.schedule(this, 1000,TimeUnit.MILLISECONDS);
			}else {
				e.kill();
				System.out.println("Expiring "+e.getClass().getCanonicalName());
			}
		}
	}
	public class TURNChannel extends HydarTURN.Expireable{
		public final short number;
		public final Client peer;
		public final Client client;
		public TURNChannel(short number, Client client, Client peer) {
			super(600);
			this.number=number;
			this.peer=peer;
			this.client=client;
		}
		public void write(byte[] j) {
			//TURNChannelData client->peer
			AtomicInteger sent = new AtomicInteger();
			//attempt to send TURNChannelData
			if(Client.alloc.get(peer)==null||!Client.alloc.get(client).permissions.containsKey(peer)) {
				System.out.println("no perm - channeldata");
				return;
			}Client.alloc.get(peer).channels.values().forEach((v)->{
				if(v.peer.equals(client)) {
					sent.incrementAndGet();
					v.read(j);
				}
			});
			if(sent.get()==0) {
				var response = new Packet(Packet.INDICATION, Packet.DATA, new byte[12]);
				response.setAttribute(Attr.DATA,j);
				response.setAttribute(Attr.XOR_PEER_ADDRESS,client.xorRelay(response));
				response.setFp();
								//send(s.getAttribute(Attribute.DATA).data,peer);
				getInstance(peer).send(response, peer);
			}
			//peer.allocation.write(j);
		}
		public void read(byte[] j) {
			//TURNChannelData peer->client
			int pad = (j.length % 4 == 0 ? 0 : (4 - j.length % 4));
			byte[] response = ByteBuffer.allocate(j.length+pad+4)
					.putShort(number)
					.putShort((short)j.length)
					.put(j)
					.array();
							//send(s.getAttribute(Attribute.DATA).data,peer);
			getInstance(peer).send(response, client);
			//peer.allocation.write(j);
		}
		@Override
		public void kill() {
			if(Client.alloc.get(client)!=null)
				Client.alloc.get(client).channels.remove(number);
			this.alive=false;
		}
	}
	public class UDPHydarStunInstance extends HydarStunInstance{
		public final DatagramSocket server;
		public UDPHydarStunInstance(int port) {
			super();
			try {
				this.port = port;
				this.server = new DatagramSocket(port);
				//HydarTURN.ip=server.getLocalAddress();
				this.server.setReceiveBufferSize(2000);
				this.server.setSoTimeout(5000);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		@Override
		public void send(Packet response, Client c){
			try {
				this.server.send(
					new DatagramPacket(response.toByteArray(), response.byteLength(), c.client, c.clientPort));
				//this.server.send(
				//		new DatagramPacket(response.toByteArray(), response.toByteArray().length, c.client, c.clientPort));
				//this.server.send(
				//		new DatagramPacket(response.toByteArray(), response.toByteArray().length, c.client, c.clientPort));
			}catch(IOException e) {
				e.printStackTrace();
				return;
			}
		}
		
		@Override
		public void send(byte[] response, Client c) {
			try {
				this.server.send(
					new DatagramPacket(response, response.length, c.client, c.clientPort));
				//this.server.send(
				//		new DatagramPacket(response, response.length, c.client, c.clientPort));
				//this.server.send(
				//		new DatagramPacket(response, response.length, c.client, c.clientPort));
			}catch(IOException e) {
				e.printStackTrace();
				return;
			}
		}
		@Override
		public void run(){
			System.out.println("Starting TURN(udp) server...");
	//		Packet j = new Packet(3, 0,
	//				new byte[] { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 });
	//		j.setAttribute(new Attribute(Packet.ALLOCATE, new byte[] { (byte) 0xa1, (byte) 0xa1, (byte) 0xa1 }));
	//		System.out.println(j);
	//		System.out.println(j.toBinary());
			// System.out.println();
			try {
				while (alive) {
					try {
						byte[] d = new byte[4096];
						DatagramPacket receive = new DatagramPacket(d, 4096);
						this.server.receive(receive);
						Packet s = parsePacket(d);
						Client c = new Client(server, receive);
						/**InetAddress addr = receive.getAddress();
						if(!HydarTURN.rateLimiter.containsKey(addr)){
							HydarTURN.rateLimiter.put(addr,new AtomicInteger());
						}
						int ratecount = HydarTURN.rateLimiter.get(addr).incrementAndGet();
						if(ratecount>150){
							continue;
						}*/
						recv(s,c);
						//authenticate - check nonce etc
						//or maybe do that in parse?
						
	
					} catch (SocketTimeoutException e) {
	
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
	/**TODO:extend Allocate  -  441 (Wrong Credentials)*/
	/**
	 * 3. The server checks if the request contains a REQUESTED-TRANSPORT attribute.
	 * If the REQUESTED-TRANSPORT attribute is not included or is malformed, the
	  server rejects the request with a 400 (Bad Request) error. Otherwise, if the
	 * attribute is included but specifies a protocol other that UDP, the server
	 * rejects the request with a 442 (Unsupported Transport Protocol) error. 4. The
	 * request may contain a DONT-FRAGMENT attribute. If it does, but the server
	 * does not support sending UDP datagrams with the DF bit set to 1 (see Section
	 * 12), then the server treats the DONT- FRAGMENT attribute in the Allocate
	 * request as an unknown comprehension-required attribute.
	 * 
	 * 5. The server checks if the request contains a RESERVATION-TOKEN attribute.
	 * If yes, and the request also contains an EVEN-PORT attribute, then the server
	 * rejects the request with a 400 (Bad Request) error. Otherwise, it checks to
	 * see if the token is valid (i.e., the token is in range and has not expired
	 * and the corresponding relayed transport address is still available). If the
	 * token is not valid for some reason, the server rejects the request with a 508
	 * (Insufficient Capacity) error.
	 * 
	 * 6. The server checks if the request contains an EVEN-PORT attribute. If yes,
	 * then the server checks that it can satisfy the request (i.e., can allocate a
	 * relayed transport address as described below). If the server cannot satisfy
	 * the request, then the server rejects the request with a 508 (Insufficient
	 * Capacity) error.
	 * 
	 * 7. At any point, the server MAY choose to reject the request with a 486
	 * (Allocation Quota Reached) error if it feels the client is trying to exceed
	 * some locally defined allocation quota. The server is free to define this
	 * allocation quota any way it wishes, but SHOULD define it based on the
	 * username used to authenticate the request, and not on the client's transport
	 * address.
	 * 
	 * 8. Also at any point, the server MAY choose to reject the request with a 300
	 * (Try Alternate) error if it wishes to redirect the client to a different
	 * server. The use of this error code and attribute follow the specification in
	 * [RFC5389].
	 * 
	 * If all the checks pass, the server creates the allocation.
	 * 
	 */
	public class TCPHydarStunInstance extends HydarStunInstance{
		class TCPHydarThread extends Thread{
			public static final Map<Client,TCPHydarThread> threads = new ConcurrentHashMap<>();
			public volatile boolean alive;
			public final Socket socket;
			public final Client client;
			public final InetAddress client_addr;
			private final OutputStream output;
			public volatile int timeouts;
			public final TCPHydarStunInstance instance;
			public TCPHydarThread(TCPHydarStunInstance instance, Socket client) throws IOException {
				this.socket=client;
				this.instance=instance;
				this.timeouts=0;
				this.client=new Client(socket.getInetAddress(),socket.getPort(),socket.getLocalPort());
				this.client_addr=socket.getInetAddress();
				this.output=new BufferedOutputStream(client.getOutputStream());
				threads.put(this.client, this);
				this.alive=true;
			}
			public void write(byte[] j){
				try {
					output.write(j);
					output.flush();
				}catch(IOException e) {
					alive=false;
					return;
				}
			}
			@Override
			public void run() {
				try(socket;output) {
					DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					while(this.alive&&timeouts<15) {
						
						//input.skip(999999999);
						if(input.available()>1024000){
							this.alive=false;
							break;
						}
						Packet s = parsePacket(input);
						if(socket.isClosed())
							break;
						//System.out.println(s);
						if(s==null) {
							//input.skip(999999999);
							timeouts++;
							input.skip(input.available());
							continue;
						}
						timeouts=0;
						instance.recv(s,client);
						
					}
				}catch(IOException e) {
					this.alive=false;
					e.printStackTrace();
				}
				/**TODO:
				if(HydarTURN.limiter.get(client_addr)!=null){
					int j = HydarTURN.limiter.get(client_addr).decrementAndGet();
					if(j<=0){
						HydarTURN.limiter.remove(client_addr);
					}
				}*/
				TCPHydarStunInstance.threadCount.decrementAndGet();
				threads.remove(client);
			}
		}


		public ServerSocket server;
		public volatile boolean alive;
		public static AtomicInteger threadCount=new AtomicInteger();
		public static int maxThreads=256;
		public TCPHydarStunInstance(int port) {
			super();
			this.port = port;
			try {
				this.server = new ServerSocket(port);
				//HydarTURN.ip=server.getLocalAddress();
				this.server.setSoTimeout(5000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		@Override
		public void send(Packet response, Client c) {
			TCPHydarThread.threads.get(c).write(response.toByteArray());
			
		}
	
		@Override
		public void send(byte[] response, Client c) {
			TCPHydarThread.threads.get(c).write(response);
		}
	
	
		@Override
		public void run() {
			System.out.println("Starting TURN(tcp) server...");
			try {
				while (alive) {
					try {
						Socket socket = this.server.accept();
						/**TODO:
						InetAddress addr = socket.getInetAddress(); if(!HydarTURN.limiter.containsKey(addr)){
							HydarTURN.limiter.put(addr,new AtomicInteger());
						}if(!HydarTURN.rateLimiter.containsKey(addr)){
							HydarTURN.rateLimiter.put(addr,new AtomicInteger());
						}
						int count = HydarTURN.limiter.get(addr).incrementAndGet();
						int ratecount = HydarTURN.rateLimiter.get(addr).addAndGet(10);
						if(count>8||ratecount>100){
							HydarTURN.limiter.get(addr).decrementAndGet();
							continue;
						}*/
						socket.setTcpNoDelay(true);
						new Thread(new TCPHydarThread(this,socket)).start();
						//authenticate - check nonce etc
						//or maybe do that in parse?
						
	
					} catch (SocketTimeoutException e) {
	
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		
	}
	/**
	 * A transport-independent STUN/TURN instance
	 * (override run() to call recv())
	 * */
	public abstract class HydarStunInstance extends Thread {
		public int port;
		public boolean alive=true;
	
		public abstract void send(Packet response, Client c);
		public abstract void send(byte[] response, Client c);
		
		@Override
		public abstract void run();
	
		public Client fromXor(byte[] addr, Packet p) {
			return parseClient(addr,port, p);
		}
		public void recv(Packet s, Client c) {
			Packet response = null;
			switch (s.messageType) {
			case Packet.BINDING:
			case Packet.CHANNELDATA:
				s.error = 0;
				break;
			default:
				if (s.messageClass != Packet.REQUEST)
					break;
				if (s.error == 0 && (s.getAttribute(Attr.NONCE) == null
						|| !Arrays.equals(Client.nonces.get(c).nonce, s.getAttribute(Attr.NONCE)))) {
					response = new Packet(Packet.ERROR, s.messageType, s.transaction);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(438));
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					this.send(response, c);
				}
				break;
			}
			if (response != null)
				return;
			switch (s.messageType) {
			case Packet.BINDING:
				response = new Packet(Packet.RESPONSE, Packet.BINDING, s.transaction);
				response.setAttribute(Attr.XOR_MAPPED_ADDRESS, c.xor(s));
				this.send(response, c);
				break;
			case Packet.ALLOCATE:
				switch (s.error) {
				case 0:
					break;
				case 400:
					response = new Packet(Packet.ERROR, Packet.ALLOCATE, s.transaction);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(400));
					break;
				case 401:
					response = new Packet(Packet.ERROR, Packet.ALLOCATE, s.transaction);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(401));
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					break;
				}
	
				if (response != null) {
					send(response, c);
					break;
				}
				if (Client.alloc.get(c) != null) {
					response = new Packet(Packet.ERROR, Packet.ALLOCATE, s.transaction);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(437));
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					response.copy(s, Attr.USERNAME);
					response.setMi();
					send(response, c);
				} else {
					System.out.println("Creating allocation for " + c);
					int ttl = 3600;
					if (s.getAttribute(Attr.LIFETIME) != null)
						ttl = Math.min(3600, Packet.getInt(s.getAttribute(Attr.LIFETIME)));
	
					var a = new Allocation(c, ttl);
					response = new Packet(Packet.RESPONSE, Packet.ALLOCATE, s.transaction);
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					response.copy(s, Attr.USERNAME);
					// reservation token whatever that is
					response.setAttribute(Attr.LIFETIME, Packet.wrapInt(a.ttl.get()));
					response.setAttribute(Attr.XOR_RELAYED_ADDRESS, c.xorRelay(s));
					response.setAttribute(Attr.XOR_MAPPED_ADDRESS, c.xor(s));
	
					response.setMi();
					send(response, c);
					send(response, c);
					send(response, c);
				}
				break;
			case Packet.DATA:
				// doesn't exist - can only be sent(or '''received''' from relay address)
				break;
			case Packet.SEND:
	
				if (s.messageClass != Packet.INDICATION) {
					response = new Packet(Packet.ERROR, Packet.SEND, s.transaction);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(400));
					send(response, c);
					break;
				}
				if (s.getAttribute(Attr.DATA) == null || s.getAttribute(Attr.XOR_PEER_ADDRESS) == null) {
					// discard
					break;
				}
				/** @TODO check permission */
				Client peer = fromXor(s.getAttribute(Attr.XOR_PEER_ADDRESS), s);
				if (peer == null)
					break;
				Allocation alloc=Client.alloc.get(c);
				if (alloc== null || !alloc.permissions.containsKey(peer)) {
					// no allocation OR no permission
					System.out.println(
							"no perm - send " +peer+ "<-" + c);
					break;
				}
				boolean sent = false;
				for(TURNChannel v:Client.alloc.get(peer).channels.values()){
					if (v.peer.equals(c)) {
						v.read(s.getAttribute(Attr.DATA));
						sent=true;
					}
				}
				if (!sent) {
					// Client.alloc.get(peer)
					response = new Packet(Packet.INDICATION, Packet.DATA, s.transaction);
					response.copy(s, Attr.DATA);
					response.setAttribute(Attr.XOR_PEER_ADDRESS, c.xorRelay(response));
					response.setFp();
					// send(s.getAttribute(Attribute.DATA).data,peer);
					// Client.alloc.get(peer).write(s.getAttribute(Attribute.DATA).data);
					send(response, peer);
				}
				// Allocation peer =
				break;
			case Packet.CHANNELBIND:
				if (s.getAttribute(Attr.CHANNEL_NUMBER) == null
						|| s.getAttribute(Attr.XOR_PEER_ADDRESS) == null) {
					// 400
					response = new Packet(Packet.ERROR, Packet.CHANNELBIND, s.transaction);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(400));
					break;
				}
				byte[] channel = s.getAttribute(Attr.CHANNEL_NUMBER);
				short cn = Packet.getShort(channel);
				// TURNChannel number is in the range 0x4000 through 0x7FFE
				if (Client.alloc.get(c) != null) {
					Client.alloc.get(c).bind(cn, fromXor(s.getAttribute(Attr.XOR_PEER_ADDRESS), s));
					response = new Packet(Packet.RESPONSE, Packet.CHANNELBIND, s.transaction);
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					response.copy(s, Attr.USERNAME);
					response.setMi();
					send(response, c);
					send(response, c);
					send(response, c);
				} else {
	
					response = new Packet(Packet.ERROR, Packet.CHANNELBIND, s.transaction);
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					response.copy(s, Attr.USERNAME);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(437));
					response.setMi();
					send(response, c);
					// 437
				}
				break;
			case Packet.CHANNELDATA:
				byte[] channel2 = s.getAttribute(Attr.CHANNEL_NUMBER);
				short cn2 = Packet.getShort(channel2);
				var theAlloc=Client.alloc.get(c);
				if (theAlloc != null) {
					var theChannel=theAlloc.channels.get(cn2);
					if(theChannel !=null)
						theChannel.write(s.getAttribute(Attr.DATA));
				}
				break;
			case Packet.REFRESH:
				if (Client.alloc.get(c) == null || Client.alloc.get(c).ttl.get() <= 0) {
					response = new Packet(Packet.ERROR, Packet.REFRESH, s.transaction);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(437));
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					response.copy(s, Attr.USERNAME);
					response.setMi();
					send(response, c);
				} else {
					System.out.println("'Refreshing' allocation");
					int ttl;
					if (s.getAttribute(Attr.LIFETIME) == null)
						ttl = 3600;
					else
						ttl = Packet.getInt(s.getAttribute(Attr.LIFETIME));
					Client.alloc.get(c).ttl.set(Math.min(ttl, 3600));
					response = new Packet(Packet.RESPONSE, Packet.REFRESH, s.transaction);
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					response.setAttribute(Attr.LIFETIME, Packet.wrapInt(ttl));
					response.copy(s, Attr.USERNAME);
					// reservation token whatever that is
					response.setMi();
					send(response, c);
				}
				break;
			case Packet.CREATEPERMISSION:
				if (s.getAttribute(Attr.XOR_PEER_ADDRESS) == null) {
					// 400
					response = new Packet(Packet.ERROR, Packet.CREATEPERMISSION, s.transaction);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(400));
					break;
				}
				// create permission
				Allocation myAlloc=Client.alloc.get(c);
				if (myAlloc != null) {
					boolean failed=false;
					for(var xorPeer:s.xorPeers){
						Client xor=fromXor(xorPeer, s);
						if (!myAlloc.createPerm(xor)) {
							failed=true;
							break;
						}
						Allocation peerAlloc=Client.alloc.get(xor);
						if (peerAlloc == null || !peerAlloc.createPerm(c)) {
							failed=true;
							break;
						}
					}
					if (!failed) {
						response = new Packet(Packet.RESPONSE, Packet.CREATEPERMISSION, s.transaction);
						response.setAttribute(Attr.REALM, "hydar".getBytes());
						response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
						response.copy(s, Attr.USERNAME);
						response.setMi();
						send(response, c);
					} else {
						response = new Packet(Packet.ERROR, Packet.CREATEPERMISSION, s.transaction);
						response.setAttribute(Attr.REALM, "hydar".getBytes());
						response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
						response.copy(s, Attr.USERNAME);
						response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(403));
						response.setMi();
						send(response, c);
					}
				} else {
					response = new Packet(Packet.ERROR, Packet.CREATEPERMISSION, s.transaction);
					response.setAttribute(Attr.REALM, "hydar".getBytes());
					response.setAttribute(Attr.NONCE, Client.nonces.get(c).nonce);
					response.copy(s, Attr.USERNAME);
					response.setAttribute(Attr.ERROR_CODE, Packet.wrapError(437));
					response.setMi();
					send(response, c);
					// 437
				}
				break;
			default:
				break;
			}
		}
	}
	public Client parseClient(byte[] xAddr, int serverPort, Packet s) {
		//boolean ipv4 = (xAddr[1] == (byte) 0x01);
		int port = (((xAddr[2]&0xff) ^ (byte) 0x21) << 8) | ((xAddr[3] ^ (byte) 0x12)&0xff);
		byte[] ip = new byte[xAddr.length - 4];
		for (int i = 0; i < ip.length; i++)
			ip[i] = (byte) ((xAddr[i + 4]) ^ s.mask[i]);
		try {
			InetAddress c = InetAddress.getByAddress(ip);
			if(c.equals(HydarTURN.this.ip)) {
				//client that created allocation w/ that port
				return Client.alloc.entrySet().stream()
					.filter(x->x.getValue().port==port)
					.map(x->x.getKey())
					.findFirst().orElse(null);
			}else return new Client(c, port, serverPort);
		} catch (UnknownHostException u) {
			u.printStackTrace();
			return null;
		}
	}
	public Packet parsePacket(byte[] input) throws EOFException {
		return parsePacket(new DataInputStream(new ByteArrayInputStream(input)));
	}
	public Packet parsePacket(DataInputStream dis) throws EOFException{
		try {
			
			short h = dis.readShort();
			
			if ((h & 0xc000) != 0) {
				if(h>=0x4000&&h<0x7FFF) {
					short length = dis.readShort();
					byte[] data = new byte[length];
					dis.read(data,0,length);
					dis.skip(length%4==0?0:(4-length%4));
					var s=new Packet(Packet.INDICATION, Packet.CHANNELDATA, new byte[12]);
					s.setAttribute(Attr.DATA,data);
					s.setAttribute(Attr.CHANNEL_NUMBER,Packet.wrapShort(h));
					return s;
				}else{
					System.out.println("bad channel #");
					return null;
				}
			}
			int c = Packet.cl(Packet.wrapShort(h));
			int t = Packet.ty(Packet.wrapShort(h));
			short length = dis.readShort();
			if (dis.readInt() !=  0x2112A442){
				System.out.println("bad cookie");
				return null;
			}
			byte[] r = dis.readNBytes(12);
			Packet s = new Packet(c, t, r);
			//s.forceLength(length);
			boolean reachedMi=false;
			short actualLength=0;
			while (actualLength< length) {
				short atype = dis.readShort();
				short alen = dis.readShort();
				byte[] adata = dis.readNBytes(alen);
				if(adata.length<alen)
					return null;
				if (adata.length % 4 != 0) {
					dis.skip(4 - alen % 4);
				}
				if (!reachedMi&&atype == Attr.MESSAGE_INTEGRITY) {
					s.forceLength((short)(s.length+24));
					s.error = 99;
					if (s.getAttribute(Attr.USERNAME) == null)
						s.error = 400;
					if (!s.checkMi(adata)){
						System.out.println("integrity check failed");
						s.error = 401;
					}s.setAttribute(atype, adata);
					reachedMi=true;
					s.forceLength((short)-1);
				}
				//if (a.type == Attribute.NONCE && !(new String(a.data).equals("hydar hydar"))) {
				//	s.error = 438;
				//}
				if(!reachedMi) {
					s.setAttribute(atype, adata);
				}
				actualLength+=Attr.byteLength(alen);
			}
			s.length=actualLength;
			if (s.error == 0 && s.messageClass != Packet.INDICATION && s.messageType != Packet.BINDING) {
				s.error = 401;
			} else
				s.error = 0;
			
			return s;
		} catch (EOFException e) {
			//e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IOException e) {
			//e.printStackTrace();
			return null;
		}
	}
	public HydarStunInstance getInstance(Client c) {
		switch(c.protocol) {
		case Client.UDP:
			return udp_instance;
		case Client.TCP:
			return tcp_instance;
		default:
			return null;
		}
	}
	public String authenticate(String user) {
		return auth.apply(user);
	}
	@Override
	public void close() {
		udp_instance.alive=false;
		tcp_instance.alive=false;
		Expireable.timer.shutdownNow();
	}
	public HydarTURN(UnaryOperator<String> auth, String ip, int port) {
		System.out.println("Creating TURN servers @"+ip+":"+port+"...");
		try {
			this.auth=auth;
			this.ip=InetAddress.getByName(ip);
			udp_instance = new UDPHydarStunInstance(port);
			new Thread(udp_instance).start();
			tcp_instance = new TCPHydarStunInstance(port);
			new Thread(tcp_instance).start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static void main(String[] args) throws InterruptedException {
		System.out.println("Using default authenticator(any username->'password')");
		//
		try(var turn=new HydarTURN(x->"password","localhost",3478)){
			Thread.sleep(Long.MAX_VALUE);
		}
	}
}