package xyz.hydar;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
abstract class HTree{
	public int sym;
	public int length;
	protected HTree() {
		this(-1,8);
	}
	protected HTree(int index, int len) {
		this.sym=index;
		this.length=len;
	}
	public abstract void put(int index, HTree h);
	public abstract HTree get(int v);
	public abstract H build();
	public static Factory factory(String strategy) {
		return switch(strategy) {
			case "MAP"->Factory.MAP;
			case "ARRAY"->Factory.ARRAY;
			default->null;
		};
	}
	public static HTree getArrayInstance(int cap) {
		return new ArrayHTree(cap);
	}
	public static HTree getArrayInstance(int sym,int length,int cap) {
		return new ArrayHTree(sym,length,cap);
	}
	private static class MapHTree extends HTree{
		public final Map<Integer,HTree> children=new HashMap<Integer,HTree>();
		private MapHTree(int sym, int length) {
			super(sym,length);
		}
		private MapHTree() {
			super();
		}
		@Override
		public void put(int index, HTree h) {
			children.put(index,h);
		}
		@Override
		public HTree get(int v) {
			return children.get(v);
		}
		@Override
		public H build() {
			if(children.size()==0) {
				return new MapH(Collections.emptyMap(),sym,length);
			}
			Map<Integer,H> newMap = Map.copyOf(children.entrySet().stream()
				.collect(Collectors.toMap(
					entry->entry.getKey(),
					entry->entry.getValue().build())
				)
			);
			return new MapH(newMap,sym,length);
		}
		private static class MapH extends H{
			public final Map<? extends Integer, ? extends H> children;
			private MapH(Map<? extends Integer, ? extends H> entries,int sym, int length) {
				super(sym,length);
				children=entries;
			}
			@Override
			public H get(int index) {
				return children.get(index);
			}
			@Override
			public String toString() {
				return children.toString();
			}
		}
		@Override
		public String toString() {
			//return "";
			return children.toString();
		}
	}
	private static class ArrayHTree extends HTree{
		public final HTree[] children;
		private static final H[] EMPTY=new H[256];
		private static final H[] EMPTY4=new H[16];
		private ArrayHTree(int sym, int length, int cap) {
			super(sym,length);
			children=new HTree[cap];
		}
		private ArrayHTree(int cap) {
			super();
			children=new HTree[cap];
		}
		@Override
		public void put(int index, HTree h) {
			children[index]=h;
		}
		@Override
		public HTree get(int v) {
			return children[v];
		}
		@Override
		public H build() {
			if(Arrays.equals(children,EMPTY))return new ArrayH(EMPTY,sym,length);
			if(Arrays.equals(children,EMPTY4))return new ArrayH(EMPTY4,sym,length);
			H[] newHs=Arrays.stream(children).map(HTree::build).toArray(H[]::new);
			return new ArrayH(newHs,sym,length);
		}
		@Override
		public String toString() {
			//return "";
			return children.toString();
		}
		private static class ArrayH extends H{
			public final H[] children;
			private ArrayH(H[] entries,int sym, int length) {
				super(sym,length);
				children=entries;
			}
			@Override
			public H get(int index) {
				return children[index];
			}
			@Override
			public String toString() {
				return children==EMPTY?"EMPTY":Arrays.toString(children);
			}
		}
	}

	enum Factory{
		MAP,ARRAY;
		public HTree newInstance() {
			if(this==MAP) {
				return new MapHTree();
			}else{
				return new ArrayHTree(256);
			}
		}
		public HTree newInstance(int sym, int length, int cap) {
			if(this==MAP) {
				return new MapHTree(sym,length);
			}else{
				return new ArrayHTree(sym,length,cap);
			}
		}
	};
}
abstract class H{
	public static final int[] HVAL = {0x1ff8, 0x7fffd8, 0xfffffe2, 0xfffffe3, 0xfffffe4, 0xfffffe5, 0xfffffe6, 0xfffffe7, 0xfffffe8, 0xffffea, 0x3ffffffc, 0xfffffe9, 0xfffffea, 0x3ffffffd, 0xfffffeb, 0xfffffec, 0xfffffed, 0xfffffee, 0xfffffef, 0xffffff0, 0xffffff1, 0xffffff2, 0x3ffffffe, 0xffffff3, 0xffffff4, 0xffffff5, 0xffffff6, 0xffffff7, 0xffffff8, 0xffffff9, 0xffffffa, 0xffffffb, 0x14, 0x3f8, 0x3f9, 0xffa, 0x1ff9, 0x15, 0xf8, 0x7fa, 0x3fa, 0x3fb, 0xf9, 0x7fb, 0xfa, 0x16, 0x17, 0x18, 0x0, 0x1, 0x2, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x5c, 0xfb, 0x7ffc, 0x20, 0xffb, 0x3fc, 0x1ffa, 0x21, 0x5d, 0x5e, 0x5f, 0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70, 0x71, 0x72, 0xfc, 0x73, 0xfd, 0x1ffb, 0x7fff0, 0x1ffc, 0x3ffc, 0x22, 0x7ffd, 0x3, 0x23, 0x4, 0x24, 0x5, 0x25, 0x26, 0x27, 0x6, 0x74, 0x75, 0x28, 0x29, 0x2a, 0x7, 0x2b, 0x76, 0x2c, 0x8, 0x9, 0x2d, 0x77, 0x78, 0x79, 0x7a, 0x7b, 0x7ffe, 0x7fc, 0x3ffd, 0x1ffd, 0xffffffc, 0xfffe6, 0x3fffd2, 0xfffe7, 0xfffe8, 0x3fffd3, 0x3fffd4, 0x3fffd5, 0x7fffd9, 0x3fffd6, 0x7fffda, 0x7fffdb, 0x7fffdc, 0x7fffdd, 0x7fffde, 0xffffeb, 0x7fffdf, 0xffffec, 0xffffed, 0x3fffd7, 0x7fffe0, 0xffffee, 0x7fffe1, 0x7fffe2, 0x7fffe3, 0x7fffe4, 0x1fffdc, 0x3fffd8, 0x7fffe5, 0x3fffd9, 0x7fffe6, 0x7fffe7, 0xffffef, 0x3fffda, 0x1fffdd, 0xfffe9, 0x3fffdb, 0x3fffdc, 0x7fffe8, 0x7fffe9, 0x1fffde, 0x7fffea, 0x3fffdd, 0x3fffde, 0xfffff0, 0x1fffdf, 0x3fffdf, 0x7fffeb, 0x7fffec, 0x1fffe0, 0x1fffe1, 0x3fffe0, 0x1fffe2, 0x7fffed, 0x3fffe1, 0x7fffee, 0x7fffef, 0xfffea, 0x3fffe2, 0x3fffe3, 0x3fffe4, 0x7ffff0, 0x3fffe5, 0x3fffe6, 0x7ffff1, 0x3ffffe0, 0x3ffffe1, 0xfffeb, 0x7fff1, 0x3fffe7, 0x7ffff2, 0x3fffe8, 0x1ffffec, 0x3ffffe2, 0x3ffffe3, 0x3ffffe4, 0x7ffffde, 0x7ffffdf, 0x3ffffe5, 0xfffff1, 0x1ffffed, 0x7fff2, 0x1fffe3, 0x3ffffe6, 0x7ffffe0, 0x7ffffe1, 0x3ffffe7, 0x7ffffe2, 0xfffff2, 0x1fffe4, 0x1fffe5, 0x3ffffe8, 0x3ffffe9, 0xffffffd, 0x7ffffe3, 0x7ffffe4, 0x7ffffe5, 0xfffec, 0xfffff3, 0xfffed, 0x1fffe6, 0x3fffe9, 0x1fffe7, 0x1fffe8, 0x7ffff3, 0x3fffea, 0x3fffeb, 0x1ffffee, 0x1ffffef, 0xfffff4, 0xfffff5, 0x3ffffea, 0x7ffff4, 0x3ffffeb, 0x7ffffe6, 0x3ffffec, 0x3ffffed, 0x7ffffe7, 0x7ffffe8, 0x7ffffe9, 0x7ffffea, 0x7ffffeb, 0xffffffe, 0x7ffffec, 0x7ffffed, 0x7ffffee, 0x7ffffef, 0x7fffff0, 0x3ffffee, 0x3fffffff};
	public static final BigInteger[] BIG_HVAL=
		Arrays.stream(HVAL)
			.mapToObj(BigInteger::valueOf)
			.toArray(BigInteger[]::new);
	public static final byte[] HLEN = {13, 23, 28, 28, 28, 28, 28, 28, 28, 24, 30, 28, 28, 30, 28, 28, 28, 28, 28, 28, 28, 28, 30, 28, 28, 28, 28, 28, 28, 28, 28, 28, 6, 10, 10, 12, 13, 6, 8, 11, 10, 10, 8, 11, 8, 6, 6, 6, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 7, 8, 15, 6, 12, 10, 13, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 7, 8, 13, 19, 13, 14, 6, 15, 5, 6, 5, 6, 5, 6, 6, 6, 5, 7, 7, 6, 6, 6, 5, 6, 7, 6, 5, 5, 6, 7, 7, 7, 7, 7, 15, 11, 14, 13, 28, 20, 22, 20, 20, 22, 22, 22, 23, 22, 23, 23, 23, 23, 23, 24, 23, 24, 24, 22, 23, 24, 23, 23, 23, 23, 21, 22, 23, 22, 23, 23, 24, 22, 21, 20, 22, 22, 23, 23, 21, 23, 22, 22, 24, 21, 22, 23, 23, 21, 21, 22, 21, 23, 22, 23, 23, 20, 22, 22, 22, 23, 22, 22, 23, 26, 26, 20, 19, 22, 23, 22, 25, 26, 26, 26, 27, 27, 26, 24, 25, 19, 21, 26, 27, 27, 26, 27, 24, 21, 21, 26, 26, 28, 27, 27, 27, 20, 24, 20, 21, 22, 21, 21, 23, 22, 22, 25, 25, 24, 24, 26, 23, 26, 27, 26, 26, 27, 27, 27, 27, 27, 28, 27, 27, 27, 27, 27, 26, 30};
	public static final H THE_H;
	public static final H THE_H4;
	public final int sym;
	public final int length;
	static {
		var fac=HTree.factory(Config.H2_HPACK_TREE_STRATEGY);
		if(fac==null) {
			System.out.println("HydarHP init: Invalid htree strategy: "+fac);
			System.out.println("Invalid htree strategy: "+fac);
		}
		HTree THE_HB=fac.newInstance();
		for(int i=0;i<HVAL.length;i++) {
			HTree ptr = THE_HB; 
			int v=HVAL[i];
			int l;
			for(l=HLEN[i];l>8;l-=8) {
				if(ptr.sym>-1){
					System.out.println("warning: HydarHP failed to load");
					break;
				}//if(len>0)
				v=(HVAL[i]>>>(l-8))&0xff;
				HTree ch=ptr.get(v);
				if(ch==null) {
					HTree h=fac.newInstance(-1,8,256);
					ptr.put(v,h);
					ptr=h;
				}else ptr=ch;
			}
			
			HTree h=fac.newInstance(i,l,256);
			int shift=8-l;
		    int start = (HVAL[i]<<shift) & 0xff;
		    int end = 1 << shift;
		    for (int j = start; j < start + end; j++) {
		    	ptr.put(j,h);
		    }
		}
		THE_H=THE_HB.build();
		THE_H4=null;
	}
	public H(int index, int len) {
		this.sym=index;
		this.length=len;
	}
	public abstract H get(int index);
}
record Entry(String name,String value){
	static final Comparator<Entry> NAME_ONLY = comparing(Entry::name,nullsFirst(naturalOrder()));
	static final Comparator<Entry> VALUE_ONLY = comparing(Entry::value,nullsFirst(naturalOrder()));
	static final Comparator<Entry> NAME_AND_VALUE = NAME_ONLY.thenComparing(VALUE_ONLY);
	
}
record Prefix(int mask, int length) {
	static final Prefix INDEXED_ADD=new Prefix(0x80,1);
	static final Prefix INCREMENTAL=new Prefix(0x40,2);
	static final Prefix NO_INDEX=new Prefix(0x00,4);
	static final Prefix NEVER_INDEX=new Prefix(0x10,4);
	static final Prefix TABLE_SIZE_CHANGE=new Prefix(0x20,3);
	static final Prefix STRING_H=new Prefix(0x80,1);
	static final Prefix STRING_NO_H=new Prefix(0x00,1);
}

public class HydarHP {
	private static final Entry[] STATIC_TABLE={
			new Entry(null,null),
			new Entry(":authority",null),
			new Entry(":method","GET"),
			new Entry(":method","POST"),
			new Entry(":path","/"),
			new Entry(":path","/index.html"),
			new Entry(":scheme","http"),
			new Entry(":scheme","https"),
			new Entry(":status","200"),
			new Entry(":status","204"),
			new Entry(":status","206"),
			new Entry(":status","304"),
			new Entry(":status","400"),
			new Entry(":status","404"),
			new Entry(":status","500"),
			new Entry("accept-charset",null),
			new Entry("accept-encoding","gzip, deflate"),
			new Entry("accept-language",null),
			new Entry("accept-ranges",null),
			new Entry("accept",null),
			new Entry("access-control-allow-origin",null),
			new Entry("age",null),
			new Entry("allow",null),
			new Entry("authorization",null),
			new Entry("cache-control",null),
			new Entry("content-disposition",null),
			new Entry("content-encoding",null),
			new Entry("content-language",null),
			new Entry("content-length",null),
			new Entry("content-location",null),
			new Entry("content-range",null),
			new Entry("content-type",null),
			new Entry("cookie",null),
			new Entry("date",null),
			new Entry("etag",null),
			new Entry("expect",null),
			new Entry("expires",null),
			new Entry("from",null),
			new Entry("host",null),
			new Entry("if-match",null),
			new Entry("if-modified-since",null),
			new Entry("if-none-match",null),
			new Entry("if-range",null),
			new Entry("if-unmodified-since",null),
			new Entry("last-modified",null),
			new Entry("link",null),
			new Entry("location",null),
			new Entry("max-forwards",null),
			new Entry("proxy-authenticate",null),
			new Entry("proxy-authorization",null),
			new Entry("range",null),
			new Entry("referer",null),
			new Entry("refresh",null),
			new Entry("retry-after",null),
			new Entry("server",null),
			new Entry("set-cookie",null),
			new Entry("strict-transport-security",null),
			new Entry("transfer-encoding",null),
			new Entry("user-agent",null),
			new Entry("vary",null),
			new Entry("via",null),
			new Entry("www-authenticate",null)
	};
	//private LinkedList<Entry> table=new LinkedList<>();
	private QueueTable<Entry> table;
	private volatile int tableSize;
	private int maxTableSize;
	private int tableSizeLimit=65536;
	protected Lock lock;
	public HydarHP() {
		this(65536);
	}
	public HydarHP(int tableSizeLimit) {
		this.tableSize=0;
		this.tableSizeLimit=tableSizeLimit;
		this.maxTableSize=tableSizeLimit;
		this.lock=new ReentrantLock() {
			private static final long serialVersionUID = 1L;
			@Override
			public void lock() {}
			@Override
			public void unlock() {}
		};
		table=QueueTable.newInstance(Config.H2_HPACK_TABLE_STRATEGY);
		//System.arraycopy(STATIC_TABLE,0,table,0,STATIC_TABLE.length);
	}public HydarHP(int tableSizeLimit, boolean compress) {
		this(tableSizeLimit);
		table=compress?
				QueueTable.eAccess()
				:QueueTable.intAccess();
		//System.arraycopy(STATIC_TABLE,0,table,0,STATIC_TABLE.length);
	} 
	private Entry get(int index) {
		int oldIndex=index-STATIC_TABLE.length;
		
		var ret= (index>=0&&index<STATIC_TABLE.length)?STATIC_TABLE[index]:table.get(oldIndex);
		if(ret==null)
			throw new RuntimeException(""+oldIndex);
		return ret;
	}
	private void set(String k, String v) {
		Entry entry=new Entry(k,v);
		//if(table.reverse.containsKey(entry))
		//	return;
		tableSize+=k.length()+v.length()+32;
		lock.lock();
		try{
			adjustTable();
			if(tableSize<=maxTableSize) {
				table.push(entry);
			}
		}finally{
			lock.unlock();
		}
	}
	//
	public Map<String,String> readFields(InputStream dis) throws IOException{
		var fields = new HashMap<String, String>();
		readFields(dis,fields);
		return fields;
	}
	public void readFields(InputStream dis, Map<String,String> block) throws IOException{
		while(readField(dis,block));
		//System.out.println("in: "+block);
	}
	public boolean readField(InputStream dis, Map<String,String> block) throws IOException{
		int first = dis.read();
		if(first==-1)//EOF
			return false;
		int index=-1;
		boolean IF=false;//increment flag(adds to dynamic table)
		//indexed-add
		if((first&0x80)!=0) {
			//System.out.println("INDEXED-ADD");
			index=decodeInt((byte)first,dis,7);
			Entry entry = get(index);
			//System.out.println("--> "+entry);
			blockPut(block,entry.name(), entry.value());
			return true;
		}
		//incremental-add
		else if((first&0x40)!=0) {
			//System.out.println("incremental-ADD");
			index=decodeInt((byte)first,dis,6);
			IF=true;
		}
		//dtable size update
		else if((first&0x20)!=0) {
			//System.out.println("WINDOW");
			//byte second=(byte)(dis.read()&0xff);
			int newSize = decodeInt((byte)first,dis,5);
			setMaxSize(newSize);
		}
		//literal never indexed
		else if((first&0x10)!=0) {
			//System.out.println("LITERAL-NEVER");
			index=decodeInt((byte)first,dis,4);
			/**@TODO make sure its actually never indexed*/
		}
		//literal without index
		else{
			//System.out.println("LITERAL-NO");
			index=decodeInt((byte)first,dis,4);
		}
		//always add to the header block, and dynamic table if if
		if(index>=0) {
			String name;
			if(index==0) {
				name = decodeString(dis);
			}else {
				name=get(index).name();
			}
			String value = decodeString(dis);
			//System.out.println("-->"+name+", "+value);
			blockPut(block,name,value);
			if(IF) {
				set(name,value);
			}
		}
		return true;
	}
	private static void blockPut(Map<String,String> block, String k, String v){
		if(k.equalsIgnoreCase("cookie")) {
			block.compute("cookie",(ki,vi)-> vi==null?v:vi+";"+v);
		}else block.put(k,v);
	}
	public void setMaxTableSize(int bytes) {
		setMaxSize_(tableSizeLimit=bytes);
	}
	private void setMaxSize(int newSize) throws IOException{
		if(newSize!=maxTableSize) {
			//System.err.println(newSize);
			//System.exit(0);
		}
		if(newSize>tableSizeLimit) {
			throw new IOException("Table size not allowed");
		}
		setMaxSize_(newSize);
	}
	private void setMaxSize_(int newSize){
		if(maxTableSize==newSize)return;
		lock.lock();
		try{
			maxTableSize=Math.min(tableSizeLimit,newSize);
			adjustTable();
		}finally{
			lock.unlock();
		}
	}
	private void adjustTable(){
		//System.out.println("adjusting table");
		while(tableSize>maxTableSize&&table.size()>0) {
			var e=table.removeFirst();
			tableSize-=(e.name().length()+e.value().length()+32);
		}
	}
	public void writeField(OutputStream dos, Entry e, boolean huffman) throws IOException{
		String k = e.name();
		String v = e.value();
		int d=table.indexOf(e);
		if(d>=0) {
			//System.out.println("INDEXED-ADD DYNAMIC" +d);
			encodeInt(Prefix.INDEXED_ADD,dos,STATIC_TABLE.length+d);
		}else {
			int si=Arrays.binarySearch(STATIC_TABLE,e,Entry.NAME_AND_VALUE);
			if(si<0) {
				int ni=Arrays.binarySearch(STATIC_TABLE,e,Entry.NAME_ONLY);
				if(ni<0) {
					//System.out.println("INC-ADD DYNAMIC");
					//Prefix prefix = k.equals("set-cookie")?Prefix.NEVER_INDEX:Prefix.INCREMENTAL;
					encodeInt(Prefix.INCREMENTAL,dos,0);
					encodeString(dos,k,huffman);
					encodeString(dos,v,huffman);
					//if(prefix==Prefix.INCREMENTAL)
					set(k,v);
				}
				else {
					//System.out.println("INC-ADD DYNAMIC[with static name] "+ni);
					encodeInt(Prefix.INCREMENTAL,dos,ni);
					encodeString(dos,v,huffman);
					set(k,v);
					//incremental(v)
				}/**TODO: maybe never-index some of them*/
			}else {
				//System.out.println("INDEXED-ADD STATIC "+si);
				encodeInt(Prefix.INDEXED_ADD,dos,si);
			}
		}
	}
	public void writeFields(OutputStream dos, Map<String,String> block) throws IOException{
		writeFields(dos,block,true);
	}
	public void writeFields(OutputStream dos, Map<String,String> block, boolean huffman) throws IOException{
		//List<Entry> l = Arrays.asList(STATIC_TABLE);
		for(var e:block.entrySet()){
			String k2=e.getKey().toLowerCase(),v=e.getValue();
			if(k2.equals("set-cookie")){
				for(String x:Arrays.asList(v.split(",")))
					writeField(dos,new Entry(k2,x),huffman);
			}else writeField(dos,new Entry(k2,v),huffman);
		}
		//System.out.println("Out: "+block);
	}
	public static void encodeInt(Prefix prefix, OutputStream dos, int i) throws IOException{
		int n = 8-prefix.length();
		int max=(1<<n)-1;
		if(i<max) {
			dos.write(prefix.mask()|i);
			return;
		}
		dos.write(prefix.mask()|max);
		i-=max;
		while(i>=128) {
			dos.write(i%128+128);
			i>>>=7;
		}
		dos.write(i);
	}
	public static void encodeString(OutputStream dos, String s, boolean h) throws IOException{
		byte[] bytes = s.getBytes(ISO_8859_1);
		if(!h) {
			encodeInt(Prefix.STRING_NO_H, dos, bytes.length);
			dos.write(bytes);
			return;
		}
		var hbytes=new BAOS(bytes.length+1);
		//ByteBuffer hbytes=ByteBuffer.wrap(bytes);
		long code=0;
		int offset=0;
		//int carry=0;
		for (int i = 0; i < bytes.length; i++) {
			int b = bytes[i]&0xff;
			int hv = H.HVAL[b];
			int hl = H.HLEN[b];
			offset+=hl;
			//System.out.write(b);
			
			//h2.
			code=(code<<hl)|hv;
			while (offset>=8) {
				offset-=8;
				hbytes.write((int)(code>>>offset));
			}
		}
		if(offset>0){//eos
			int head=(int)code;
			int trail=(head<<8>>>offset)|(0xff>>>(offset));
			hbytes.write(trail);
			//hbytes.put();
		}
		encodeInt(Prefix.STRING_H, dos, hbytes.size());
		hbytes.writeTo(dos);
		//}
	}
	public static int decodeInt(byte first,InputStream dis,  int prefixLength)  throws IOException{
		//System.out.println(((1<<prefixLength)-1));
		int shifted=(1<<prefixLength)-1;
		int i = first&shifted;
		//System.out.println(""+i+", "+Math.pow(2,prefixLength));
		if(i>=shifted) {
			int m=0;
			int b=128;
			do{
				if(m>32)
					throw new IOException("HPACK integer overflow");
				b=dis.read();
				i+=(b&127) * (1<<m);
				m+=7;
				
			}while((b&128)==128);
		}
		return i;
	}
	public static String decodeString(InputStream dis) throws IOException{
		byte first = (byte)dis.read();
		int length = decodeInt(first, dis, 7);
		//not Huffman encoded
		if((first&(byte)0x80)==0) {
			return new String(dis.readNBytes(length),ISO_8859_1);
		}
		BAOS baos = new BAOS(length+1);
		int total=0;
		long code=0;
		int offset=0;
		H ptr = H.THE_H;
		//boolean cf=false;
		while(total<length) {
			if(offset<8) {
				offset+=8;
				int h=dis.read();
				total++;
				if(h<0) {
					break;
				}
				//code=java.math.
				code=(code<<8)|h;
			}
			int i=(int)(code>>>(offset-8))&0xff;
			ptr=ptr.get(i);
			offset-=ptr.length;
			if(ptr.sym>=0) {
				baos.write(ptr.sym);
				ptr=H.THE_H;
			}
		}
		while(offset > 0) {
			int h=(int)(code<<8>>>offset)&0xff;
			ptr = ptr.get(h);
			if (ptr.sym>=0 &&ptr.length<=offset) {
				offset-=ptr.length;
				baos.write(ptr.sym);
				ptr=H.THE_H;
			}else { 
				break;
			}
		}
		return baos.toString(ISO_8859_1);
		
	}
	public static void tests(String[] args) {
		/**
		try {
			//InputStream dis0=new InputStream(new ByteArrayInputStream(new byte[] {(byte)0b10000100,(byte)0b11100101,(byte)0b01111001,(byte)0b00111000,(byte)0b00110111}));
			//System.out.println(decodeString(dis0));
			//System.out.println(");
			//System.out.println(Arrays.binarySearch(STATIC_TABLE, new Entry("expires",null)));
			//H.init();
			//System.out.println("?"+H.THE_H.children[0xa8]);
			byte[] har = new byte[] {(byte)0x86,(byte)0xa8,(byte)0xeb,(byte)0x10,(byte)0x64,(byte)0x9c,(byte)0xbf};
			har=new byte[] {
				(byte)0x89, 0x25,(byte)0xa8,0x49,(byte)0xe9,0x5b,(byte)0xb8,(byte)0xe8,(byte)0xb4,(byte)0xbf	
			};
			har=new byte[] {
					(byte)0x89, 0x25,(byte)0xa8,0x49,(byte)0xe9,0x5b,(byte)0xb8,(byte)0xe8,(byte)0xb4,(byte)0xbf	
				};
			//non huffman examples
			byte[] har2=HexFormat.of().parseHex("82418c0be25c2e3cb85772e32cb60f870084b958d33f8d60d48e62a189fd483b17aea9bf4085aec1cd48ff86a8eb10649cbf5886a8eb10649cbf4092b6b9ac1c8558d520a4b6c2ad617b5a54251f01317ad8d07f66a281b0dae053fae46aa43f8429a77a8102e0fb5391aa71afb53cb8d7f6a435d74179163cc64b0db2eaecb8a7f59b1efd19fe94a0dd4aa62293a9ffb52f4f61e92b0f32b8176820657702a6e1ca3b0cc36cbabb2e7f53dd497ca589d34d1f43aeba0c41a4c7a98f33a69a3fdf9a68fa1d75d0620d263d4c79a68fbed00177fe8d48e62b1e0b1d7f46a4731581d754df5f2c7cfdf6800bbdf43aeba0c41a4c7a9841a6a8b22c5f249c754c5fbef046cfdf6800bbff408a4148b4a549275906497f872587421641925f408a4148b4a549275a93c85f86a87dcd30d25f408a4148b4a549275ad416cf023f31408a4148b4a549275a42a13f8690e4b692d49f508d9bd9abfa5242cb40d25fa523b3518b2d4b70ddf45abefb4005df");
			byte[] har3=HexFormat.of().parseHex("828684be58086e6f2d6361636865");
			byte[] har4=HexFormat.of().parseHex("828785bf400a637573746f6d2d6b65790c637573746f6d2d76616c7565");
			//huffman examples
			byte[] har5=HexFormat.of().parseHex("828684418cf1e3c2e5f23a6ba0ab90f4ff");
			byte[] har6=HexFormat.of().parseHex("828684be5886a8eb10649cbf");
			//byte[] har7=HexFormat.of().parseHex("828785bf408825a849e95ba97d7f8925a849e95bb8e8b4bf");
			byte[] har7=HexFormat.of().parseHex("8287");
			InputStream dis7 = new DataInputStream(new ByteArrayInputStream(har7));
			InputStream dis6 = new DataInputStream(new ByteArrayInputStream(har6));
			InputStream dis5 = new DataInputStream(new ByteArrayInputStream(har5));
			InputStream dis4 = new DataInputStream(new ByteArrayInputStream(har4));
			InputStream dis3 = new DataInputStream(new ByteArrayInputStream(har3));
			InputStream dis2 = new DataInputStream(new ByteArrayInputStream(har2));
			//not necessarily linkedhashmap
			HashMap<String,String> headers = new LinkedHashMap<String,String>();
			HashMap<String,String> headers2 = new LinkedHashMap<String,String>();
			HashMap<String,String> headers3 = new LinkedHashMap<String,String>();
			HashMap<String,String> headers4 = new LinkedHashMap<String,String>();
			HashMap<String,String> headers5 = new LinkedHashMap<String,String>();
			HashMap<String,String> headers6 = new LinkedHashMap<String,String>();
			var the_har = new HydarHP();
			var the_har2 = new HydarHP();
			while(the_har.readField(dis2,headers));
			System.out.println(headers);
			while(the_har.readField(dis3,headers2));
			System.out.println(headers2);
			while(the_har.readField(dis4,headers3));	
			System.out.println(headers3);
			while(the_har2.readField(dis5,headers4));
			System.out.println(headers4);
			while(the_har2.readField(dis6,headers5));
			System.out.println(headers5);
			while(the_har2.readField(dis7,headers6));	
			System.out.println(headers6);		
			System.out.println(Arrays.asList(the_har.table));
			var baos = new ByteArrayOutputStream();
			var dos=new DataOutputStream(baos);
			//encodeInt(Prefix.TABLE_SIZE_CHANGE,dos,10);
			encodeString(dos,"hydar",true);
			dos.close();
			for(byte x:baos.toByteArray()) {
			
			}
			//System.out.println("?"+new BigInteger(baos.toByteArray()).toString(2));
			//System.out.println("?"+new BigInteger(baos.toByteArray()).toString(2));
			System.out.println("..."+decodeString(new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))));
			
			var heads = new LinkedHashMap<String,String>();
			heads.put(":method", "GET");
			heads.put(":scheme", "http");
			heads.put(":authority", "www.example.com");
			heads.put("hydar", "hydar");
			heads.put("har\r\nhdar\r\nh\r\ny\r\nd\r\na\r\nr\r\nhydar", "nljkaf8923jojs");
			
			baos = new ByteArrayOutputStream();
			dos=new DataOutputStream(baos);
			var the_har3 =new HydarHP();
			the_har3.writeFields(dos,heads);
			var finalBlock = new LinkedHashMap<String,String>();
			byte[] finalA=baos.toByteArray();
			var dis =new DataInputStream(new ByteArrayInputStream(finalA));
			while(the_har3.readField(dis,finalBlock));
			System.out.println(finalBlock);
			System.out.println(HexFormat.of().formatHex(baos.toByteArray()));
			//System.out.println(new BigInteger(baos.toByteArray()).toString(2));
			//System.out.println(decodeString(new InputStream(new ByteArrayInputStream(har))));
			//System.out.println(decodeInt((byte)0b10111111,new InputStream(new ByteArrayInputStream(new byte[] {(byte)0b10011010,(byte)0b00001010})),5	));
		}catch(Exception e) {
			e.printStackTrace();
		}*/
	}	
}