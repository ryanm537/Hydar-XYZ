package xyz.hydar.ee;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.http.HttpTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;


public class HydarUtil {
	public static final DateTimeFormatter SDF=DateTimeFormatter.RFC_1123_DATE_TIME;
	public static final DateTimeFormatter SDF3=new DateTimeFormatterBuilder()
		.appendOptional(SDF)
		.appendOptional(DateTimeFormatter.ofPattern("e, dd-MMM-yy HH:mm:ss z"))
		.appendOptional(DateTimeFormatter.ofPattern("EEE MMM ppd HH:mm:ss yyyy"))
		.toFormatter(Locale.US)
		.withZone(ZoneId.of("GMT"));
	private static final WatchEvent.Modifier UNSAFE_MODIFIER;
	static final ThreadFactory TFAC;
	//'conditional compile' for 19+
	static {
		ThreadFactory tmp;
		try {
			Method meth=Thread.class.getMethod("ofVirtual");
			Object threadBuilder=meth.invoke(null);
			Class<?> builderClass=meth.getReturnType();
			builderClass.getMethod("name",String.class,long.class)
				.invoke(threadBuilder,"client-vthread-",0);
			tmp=(ThreadFactory)builderClass
					.getMethod("factory")
					.invoke(threadBuilder);
			System.out.println("Using vthread factory");
		}catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			System.out.println("Using normal thread factory");
			tmp=Thread::new;
		}
		TFAC=tmp;
	}
	static {
		Modifier modif_=null;
		try {
			Class<?> modifClass=Class.forName("com.sun.nio.file.ExtendedWatchEventModifier");
			for(Field field:modifClass.getDeclaredFields()) {
				if(field.getName().equals("FILE_TREE"))
					modif_=(Modifier) field.get(null);
			}
			//test the service
			try(WatchService svc= FileSystems.getDefault().newWatchService()){
				var key=Path.of(".").register(svc,new Kind<?>[] {StandardWatchEventKinds.ENTRY_MODIFY},modif_);
				key.pollEvents();
				key.cancel();
			}
		}catch(Exception e) {
			System.out.println("Recursive file watcher not supported");
			modif_=null;
		}
		UNSAFE_MODIFIER=modif_;
	}
	public static WatchEvent.Modifier getUnsafe() {
		return Config.TRY_UNSAFE_WATCH_SERVICE?UNSAFE_MODIFIER:null;
	}
	private static final SecureRandom rng = new SecureRandom();
	public static boolean addKey(Path dir, Path root) throws IOException {
		if(getUnsafe()!=null&&root.equals(dir)) {
			WatchKey key = dir.register(Hydar.watcher,
					new WatchEvent.Kind<?>[] {ENTRY_CREATE,
				ENTRY_DELETE,
	            ENTRY_MODIFY},getUnsafe());
			
			Hydar.KEYS.put(dir,key);
			return true;
		}else if(getUnsafe()==null){
			Hydar.KEYS.put(dir,dir.register(Hydar.watcher,ENTRY_CREATE,
				ENTRY_DELETE,
	            ENTRY_MODIFY));
			return true;
		}
		return false;
	}
	public static List<Path> getFiles(Path root){
		List<Path> allFiles=new ArrayList<>();
		try {
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					if(!Config.FORBIDDEN_REGEX
							.filter(x->x.matcher(Hydar.dir.relativize(dir).toString().replace("\\","/")+"/")
									.find()
								)
							.isPresent()
							) {
						if(Config.USE_WATCH_SERVICE) {
							addKey(dir,root);
						}
						return FileVisitResult.CONTINUE;
					}else{
						//System.out.println(dir);
						return FileVisitResult.SKIP_SUBTREE;
					}
				}
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if(!Config.FORBIDDEN_REGEX
							.filter(x->x.matcher(Hydar.dir.relativize(file).toString()).find())
							.isPresent())
						allFiles.add(file);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			System.out.println("Failed to load web root directory. Please check permissions and hydar.properties");
		}
		return allFiles;
	}
	public static <E> Iterable<E> lazyConcat(Collection<E> list1,Collection<E> list2){
		return Stream.concat(list1.stream(),list2.stream())::iterator;
	}
	@SafeVarargs
	public static <E> Iterable<E> lazyConcat(Collection<E>... list1){
		return Arrays.stream(list1).flatMap(Collection::stream)::iterator;
	}
	public static <E> Iterable<E> iter(Stream<E> stream){
		return stream::iterator;
	}
	public static Iterable<String> lines(Path path) throws IOException{
		return Files.lines(path)::iterator;
	}
	public static String noise(int length) {
		char[] id = new char[length];
		id[0]='h';id[1]='y';id[2]='d';id[length-1]='r';
		for(int i=3;i<id.length-1;i++){
				if(rng.nextFloat()<0.5)
				id[i]=(char)('e'+rng.nextInt(22));
				else id[i]=(char)('E'+rng.nextInt(22));
			}
		return new String(id);
	}
	//gives the length of the error document, so we dont have to recompute it
	public static String memory() {
		int mb = 1024 * 1024;
		// get Runtime instance
		Runtime instance = Runtime.getRuntime();
		String usage = "";
		// available memory
		usage+=(", Threads: " +Thread.activeCount()+"\n");
		// used memory
		usage+=("Used Memory: "
				+ (instance.totalMemory() - instance.freeMemory()) / mb+" MB, ");
		// Maximum available memory
		usage+="Allocated: " + instance.totalMemory() / mb+" MB";
		usage+=("\nMax Memory: " + instance.maxMemory() / mb+" MB");
		return usage;
		}
	public static byte[] compress(byte[] data,Encoding enc){
		try(BAOS out1= new BAOS(data.length/5); 
			var out = enc.defOS(out1);){
			out.write(data);
			out.finish();
			return out1.toByteArray();
		}catch(IOException e){return null;}//not possible
	}
	public static String readString(Path p,int bufferSize) throws IOException{
		return new String(readAllBytes(p,bufferSize), StandardCharsets.UTF_8);
	}
	public static void mkOptDirs(Path p) throws IOException{

		if(Files.notExists(p))
			Files.createDirectories(p);
	}
	public static byte[] readAllBytes(Path p, int bufferSize) throws IOException{
		BAOS baos=new BAOS((int)Files.size(p));
		try(var f=Files.newInputStream(p);var is=new BufferedInputStream(f,16384)){
			is.transferTo(baos);
		}
		return baos.buf();
	}
	public static byte[] readAllBytes(Path p, int bufferSize, int fileSize) throws IOException{
		BAOS baos=new BAOS(fileSize);
		try(var f=Files.newInputStream(p);var is=new BufferedInputStream(f,16384)){
			is.transferTo(baos);
		}
		return baos.buf();
	}
	public static int write16Stream(OutputStream dest,InputStream src, byte[] buffer, int length) throws IOException{
		int len = Math.min(buffer.length,length);
		int l=src.read(buffer,0,len);
		if(l>0){
			dest.write(buffer,0,l);
		}
		return l;
	}

	public static String httpInfo(String status){
		if(status==null)
			return null;
		return switch(status){
			case "100"->"continue";
			case "101"->"switching protocols";
			case "200"->"ok";
			case "204"->"no content";
			case "206"->"partial content";
			case "301"->"moved permanently";
			case "302"->"found";
			case "304"->"not modified";
			case "400"->"bad request";
			case "403"->"forbidden";
			case "404"->"not found";
			case "408"->"request timeout";
			case "412"->"precondition failed";
			case "416"->"range not satisfiable";
			case "429"->"too many requests";
			case "431"->"Request Header Fields Too Large";
			case "500"->"Internal Server Error";
			case "501"->"Not Implemented";
			case "503"->"Service Unavailable";
			case "505"->"HTTP Version Not Supported";
			default -> null;
		};
	}
	/**TODO: more etag options(strong/weak matching), etags for jsp*/
	public static boolean etagMatch(String patterns, String target, boolean strong) {
		for(String etag:patterns.split(",")){
			int start = etag.indexOf("\"");
			int end = etag.lastIndexOf("\"");
			String inner="";
			if(strong&&inner.toLowerCase().startsWith("w/"))
				continue;
			if(start<0||end<0||start==end)
				inner=etag;
			else inner = etag.substring(start,end);
			if(inner.equals("*")||inner.equals(target)){
				return true;
			}
		}
		return false;
	}
	/**Obtain an object through JNDI given a name for an ObjectFactory class, a type, and properties.
	 * The properties will be packed into a Reference object as StringRefAddrs.
	 * */
	static Object getObject(String factory, String type, Map<?,?> properties) {
		try {
			var h = Class.forName(factory);
			var facInstance = (ObjectFactory)h.getConstructors()[0].newInstance();
			Reference ref = new Reference(type,factory,null);
			if(properties!=null)
				properties.forEach((x,y)->ref.add(new StringRefAddr(x.toString(),y.toString())));
			
			return facInstance.getObjectInstance(ref, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return null;
	}


}
@SuppressWarnings("sync-override")
class BAIS extends ByteArrayInputStream{

	public BAIS(byte[] buf) {
		super(buf);
	}public BAIS(byte[] buf,int off, int length) {
		super(buf,off,length);
	}@Override
    public int read() {
        return (pos < count) ? (buf[pos++] & 0xff) : -1;
    }@Override
    public int read(byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        if (pos >= count) {
            return -1;
        }
        int avail = count - pos;
        if (len > avail) {
            len = avail;
        }
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    } @Override
    public byte[] readAllBytes() {
    	if(pos==0&&count==buf.length) {
    		pos=count;
    		return buf;
    	}
        byte[] result = Arrays.copyOfRange(buf, pos, count);
        pos = count;
        return result;
    }@Override
    public long transferTo(OutputStream out) throws IOException {
        int len = count - pos;
        out.write(buf, pos, len);
        pos = count;
        return len;
    }@Override
    public long skip(long n) {
        long k = count - pos;
        if (n < k) {
            k = n < 0 ? 0 : n;
        }

        pos += (int) k;
        return k;
    }@Override
    public int available() {
        return count - pos;
    }
    @Override
    public void reset() {
        pos = mark;
    }
	
}

@SuppressWarnings("sync-override")
class BAOS extends ByteArrayOutputStream {
	public BAOS(int length) {
		super(length);
	}
	private void ensureCapacity(int minCapacity) {
		int oldCapacity = buf.length;
		int minGrowth = minCapacity - oldCapacity;
		if (minGrowth > 0) {
			buf = Arrays.copyOf(buf, Math.max(minCapacity, oldCapacity * 2));
		}
	}
	@Override
	public void write(int b) {
		ensureCapacity(count + 1);
		buf[count] = (byte) b;
		count += 1;
	}
	@Override
	public void write(byte[] b, int off, int len) {
		Objects.checkFromIndexSize(off, len, b.length);
		ensureCapacity(count + len);
		System.arraycopy(b, off, buf, count, len);
		count += len;
	}
	@Override
	public void writeTo(OutputStream out) throws IOException {
		out.write(buf, 0, count);
	}
	@Override
	public void reset() {
		count = 0;
	}
	@Override
	public byte[] toByteArray() {
		return Arrays.copyOf(buf, count);
	}
	public InputStream toInputStream() {
		return new BAIS(buf,0,count);
	}
	@Override
	public int size() {
		return count;
	}
	@Override
	public String toString() {
		return new String(buf,0,count,Charset.defaultCharset());
	}
	@Override
	public String toString(Charset ch) {
		return new String(buf,0,count,ch);
	}
	public byte[] buf() {
		return buf;
	}
}
class BufferedDIS extends BufferedInputStream{
	Limiter limiter;
	public BufferedDIS(InputStream in, Limiter limiter) {
		super(in);
		this.limiter=limiter;
	}
	public BufferedDIS(InputStream in, Limiter limiter, int i) {
		super(in,i);
		this.limiter=limiter;
	}
	boolean skipLF=false;
    private void ensureOpen() throws IOException {
        if (in == null)
            throw new IOException("Stream closed");
    }
    private int read1(byte[] cbuf, int off, int len) throws IOException {
        if (pos >= count) {
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, and if line feeds are not
               being skipped, do not bother to copy the characters into the
               local buffer.  In this way buffered streams will cascade
               harmlessly. */
            if (len >= buf.length && markpos <= -1 && !skipLF) {
                return in.read(cbuf, off, len);
            }
            fill2();
        }
        if (pos >= count) return -1;
        if (skipLF) {
            skipLF = false;
            if (buf[pos] == '\n') {
            	pos++;
                if (pos >= count)
                    fill2();
                if (pos >= count)
                    return -1;
            }
        }
        int n = Math.min(len, count - pos);
        System.arraycopy(buf, pos, cbuf, off, n);
        pos += n;
        return n;
    }
	@Override
	public int read(byte[] cbuf, int off, int len) throws IOException{
        ensureOpen();
        Objects.checkFromIndexSize(off, len, cbuf.length);
        if (len == 0) {
            return 0;
        }
		limiter.forceBuffer(len);
		if(!limiter.acquire(Token.IN,len))
			return -1;

        int n = read1(cbuf, off, len);
        if (n <= 0) return n;
        while ((n < len) && in.available()>0) {
            int n1 = read1(cbuf, off + n, len - n);
            if (n1 <= 0) break;
            n += n1;
        }
        return n;
	    
	}
	@Override
	public int read() throws IOException{
		ensureOpen();
		for (;;) {
            if (pos >= count) {
                fill2();
                if (pos >= count)
                    return -1;
            }
            if (skipLF) {
                skipLF = false;
                if (buf[pos] == (byte)'\n') {
                	pos++;
                	continue;
                }
            }
            return buf[pos++]&0xff;
        }
	}
	private void fill2() throws IOException{

		limiter.forceBuffer(buf.length);
		mark(1);
        super.read();
        reset();
	}
	public String readLineCRLFLatin1() throws IOException{
		return readCRLFLine(ISO_8859_1);

	}
	public String readCRLFLine(Charset ch) throws IOException{
		String line= readCRLFLineImpl(ch);
		if(line!=null && !limiter.acquire(Token.IN,line.length())) {
			line=null;
		}
		return line;
	}
	private String readCRLFLineImpl(Charset cs) throws IOException{
		StringBuilder s = null;
        int startChar;
        boolean omitLF = skipLF;
        for (;;) {

            if (pos >= count) {
            	fill2();
            }if (pos >= count) { /* EOF */
                if (s != null && s.length() > 0)
                    return s.toString();
                else
                    return null;
            }
            boolean eol = false;
            int c = 0;
            int i;

            /* Skip a leftover '\n', if necessary */
            if (omitLF && (buf[pos] == '\n'))
                pos++;
            skipLF = false;
            omitLF = false;
            
            for (i = pos; i < count; i++) {
                c = buf[i];
                if (c == '\r') {
                    eol = true;
                    break;
                }
            }
            startChar = pos;
            pos = i;

            if (eol) {
                String str;
                if (s == null) {
                    str = new String(buf, startChar, i - startChar, cs);
                } else {
                    s.append(new String(buf, startChar, i - startChar, cs));
                    str = s.toString();
                }
                pos++;
                skipLF = true;
                return str;
            }

            if (s == null)
                s = new StringBuilder(128);
            s.append(new String(buf, startChar, i - startChar, cs));
        }
	}
	public static byte[] noise2() {
		StringBuilder b=new StringBuilder();
		for(int i=0;i<Math.random()*2;i++) {
			b.append(HydarUtil.noise(5000+(int)(Math.random()*1000)));
			for(int j=0;j<10;j++)
			switch(i%100) {
				case 0:
					b.append("\r\n");
					break;
				case 1:
					b.append("\r");
					break;
				case 2:
					b.append("\n");
					break;
				default:
					break;
			}
		}
		return b.toString().getBytes();
	}
	public static void testInts() throws IOException{
		List<byte[]> testData =Stream.generate(BufferedDIS::noise2).limit(1000).parallel().toList();
		long ms=System.currentTimeMillis();
		//Limiter l=Limiter.UNLIMITER;
		long counter=0;
		for(byte[] b:testData) {
			
			BAIS bais=new BAIS(b);
			//BufferedDIS reader = new BufferedDIS(bais,l);
			var reader=new BufferedDIS(bais,Limiter.UNLIMITER);
			//DIS reader = new DIS(bdis,Limiter.UNLIMITER);
			//DataInputStream reader = new DataInputStream(bdis);
			//reader.noText();
			//BufferedDIS reader2=new BufferedDIS(InputStream.nullInputStream(),l);
			//DataInputStream reader = new DataInputStream(new BufferedInputStream(bais));
			//DataInputStream reader = new DIS(new BufferedInputStream(bais), Limiter.UNLIMITER);
			//for(int i=0;i<1000;i++)
			//counter+=
			for(int i=0;i<1000;i++)
			counter+=reader.read();
		}

		System.out.println(Duration.ofMillis(System.currentTimeMillis()-ms));
		ms=System.currentTimeMillis();
		System.out.println(counter);
	}
	public static void testStrings() throws IOException{
		List<byte[]> testData =Stream.generate(BufferedDIS::noise2).limit(1000).parallel().toList();
		Writer input=new StringWriter(10000);
		long ms=System.currentTimeMillis();
		for(byte[] b:testData) {
			
			BAIS bais=new BAIS(b);
			//BufferedReader reader=new BufferedReader(new InputStreamReader(bais,StandardCharsets.ISO_8859_1),16384);
			//DIS reader = new DIS(new BufferedInputStream(bais,16384),Limiter.UNLIMITER);
			//BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(bais),StandardCharsets.ISO_8859_1),16384);
			//DIS reader = new DIS(bais, Limiter.UNLIMITER);
			BufferedDIS bdis = new BufferedDIS(bais,Limiter.UNLIMITER);
			//DIS reader = new DIS(bdis, Limiter.UNLIMITER);
			String line;
			while((line=bdis.readCRLFLine(ISO_8859_1))!=null) {
				input.write(line);
			}
		}
		String result1=input.toString();
		input=new StringWriter(10000);
		System.out.println(Duration.ofMillis(System.currentTimeMillis()-ms));
		ms=System.currentTimeMillis();
		for(byte[] b:testData) {
			
			BAIS bais=new BAIS(b);
			//BufferedReader reader=new BufferedReader(new InputStreamReader(bais,StandardCharsets.ISO_8859_1),16384);
			//DIS reader = new DIS(new BufferedInputStream(bais,16384),Limiter.UNLIMITER);
			BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(bais),ISO_8859_1),16384);
			//DIS reader = new DIS(bais, Limiter.UNLIMITER);
			//BufferedDIS reader = new BufferedDIS(bais);
			String line;
			while((line=reader.readLine())!=null) {
				input.write(line);
			}
		}
		String result=input.toString();
		System.out.println(result.length());
		System.out.println(result1.equals(result));
		System.out.println(HydarUtil.memory());
		System.out.println(Duration.ofMillis(System.currentTimeMillis()-ms));
	}
}
class DoubleMap<E>{
	protected Map<E,Deque<Integer>> reverse;
	protected Map<Integer,E> forward;
	protected int offset=0;
	public DoubleMap() {
		reverse=new HashMap<>();
		forward=new HashMap<>();
	}
	public static <E> DoubleMap<E> synchronizedDoubleMap(DoubleMap<E> dm) {
		return new ConcurrentDoubleMap<>(dm);
	}
	static class ConcurrentDoubleMap<E> extends DoubleMap<E>{
		private final AtomicInteger offset;
		ConcurrentDoubleMap() {
			this(new DoubleMap<>());
		}
		ConcurrentDoubleMap(DoubleMap<E> dm) {
			reverse=new ConcurrentHashMap<>(dm.reverse);
			forward=new ConcurrentHashMap<>(dm.forward);
			offset=new AtomicInteger(dm.offset);
			
		}@Override
		protected int inc() {
			return offset.getAndIncrement();
		}@Override
		protected int offset() {
			return offset.get();
		}
	}
	public int size() {
		return forward.size();
	}
	protected int inc() {
		return offset++;
	}
	protected int offset() {
		return offset;
	}
	public int indexOf(E e) {
		var v=reverse.get(e);
		if(v==null)return -1;
		return size()-1-(v.peek()-offset());
	}
	public E get(Integer e) {
		int reversedIndex=size()-1-e+offset();
		return forward.get(reversedIndex);
	}
	public void push(E e) {
		Integer i=size()+offset();
		//forward.put(i,e);
		//reverse.put(e,i);
		if(forward.put(i,e)!=null) {
			throw new IllegalArgumentException("Key was not unique.");
		}
		var oldList=reverse.get(e);
		if(oldList!=null) {
			oldList.push(i);
			return;
		}
		Deque<Integer> newList=new ArrayDeque<>(2);
		newList.push(i);
		reverse.put(e,newList);
	}
	public E removeFirst() {
		E e=forward.remove(inc());
		Deque<Integer> bucket=reverse.get(e);
		if(bucket!=null) {
			bucket.removeFirst();
			if(bucket.isEmpty()) {
				reverse.remove(e);
			}
			return e;
		}return null;
	}
	@Override
	public String toString() {
		return forward.toString()+"\n"+reverse.toString();
	}
}

class Average{
	private int count=0;
	private int avg=1024;
	public void update(int data) {
		count++;
		avg=((avg*(count-1))+data)/count;
	}
	public int avg() {
		return avg;
	}
}
interface QueueTable<E>{
	public int size();
	public int indexOf(E e);
	public E get(int e);
	public void push(E e);
	public E removeFirst();
	public static <E> QueueTable<E> doubleAccess(String strategy){
		return switch(strategy) {
			case "MAP"->new DoubleMapQueueTable<>();
			case "LINKEDLIST"->new LinkedListQueueTable<>();
			default->null;
		};
	}
	public static <E> QueueTable<E> intAccess(String strategy){
		return switch(strategy) {
			case "MAP"->new IndexOnlyQueueTable<>();
			case "LINKEDLIST"->new LinkedListQueueTable<>();
			default->null;
		};
	}
	public static <E> QueueTable<E> eAccess(String strategy){
		return switch(strategy) {
			case "MAP"->new CounterOnlyQueueTable<>();
			case "LINKEDLIST"->new LinkedListQueueTable<>();
			default->null;
		};
	}
}
class IndexOnlyQueueTable<E> extends HashMap<Integer,E> implements QueueTable<E>{
	private static final long serialVersionUID = 392809410070860989L;
	int offset=0;
	@Override
	public int indexOf(E e) {
		throw new UnsupportedOperationException();
	}
	@Override
	public E get(int e) {
		return super.get((size()-1-e)+offset);
	}

	@Override
	public void push(E e) {
		if(put(size()+offset,e) != null) {
			throw new IllegalStateException("Index overlap: "+size()+", offset="+offset);
		}
	}
	@Override
	public E removeFirst() {
		return super.remove(offset++);
	}
	
}
class CounterOnlyQueueTable<E> extends LinkedHashMap<E,Integer> implements QueueTable<E>{
	private static final long serialVersionUID = -980613595004270736L;
	int offset=0;
	@Override
	public int indexOf(E e) {
		// TODO Auto-generated method stub
		Integer tmp=super.get(e);
		return tmp==null?-1:(size()-1-tmp)+offset;
	}

	@Override
	public void push(E e) {
		super.put(e,super.size()+offset);
	}

	@Override
	public E get(int e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E removeFirst() {
		// TODO Auto-generated method stub
		E key=keySet().iterator().next();
		if(remove(key) != null)offset++;
		return key;
	}
	
}
class DoubleMapQueueTable<E> extends DoubleMap<E> implements QueueTable<E>{
	@Override
	public E get(int e) {
		return super.get(e);
	}
}
class LinkedListQueueTable<E> extends LinkedList<E> implements QueueTable<E>{
	private static final long serialVersionUID = 417098369646560712L;
}
/**TODO: move to hydar(subclass)*/
abstract class Limiter{
	interface AbstractToken{
		public Map<Long, Long> tasks();
		public long getCount(long task);
	}
	public static final Limiter UNLIMITER=new Unlimiter();
	private static Function<? super InetAddress,? extends Limiter> provider=(x)->UNLIMITER;
	private static final class Unlimiter extends Limiter{
		@Override
		public boolean checkBuffer(int bytesToRead) {return true;}
		@Override
		public void forceBuffer(int bytesToRead){}
		@Override
		public void release(AbstractToken t, int amount) {}
		@Override
		public boolean acquire(AbstractToken t, int amount) {
			return true;
		}
		@Override
		public boolean acquireNow(AbstractToken t, int amount) {
			return true;
		}
	}
	public static void setProvider(Function<? super InetAddress,? extends Limiter> src) {
		provider=src;
	}
	public abstract boolean checkBuffer(int bytesToRead);
	public abstract void release(AbstractToken t, int amount);
	public abstract boolean acquire(AbstractToken t, int amount);
	public abstract boolean acquireNow(AbstractToken t, int amount);
	public void forceNow(AbstractToken t, int amount) throws IOException{
		if(!acquireNow(t, amount)) {
			throw new HttpTimeoutException("Limiter timed out: "+t);
		}
	}
	public void force(AbstractToken t, int amount) throws IOException{
		if(!acquire(t, amount)) {
			throw new HttpTimeoutException("Limiter timed out: "+t);
		}
	}

	public void forceBuffer(int bytesToRead) throws IOException{
		if(!checkBuffer(bytesToRead)) {
			throw new HttpTimeoutException("Buffer too large");
		}
	}
	public static Limiter from(InetAddress address) {
		return provider.apply(address);
	}
	public static Limiter from(Socket sock) {
		return from(sock.getInetAddress());
	}
}

enum Token implements Limiter.AbstractToken{
	PERMANENT_STATE(),//never resets
	IN(),
	OUT(),
	FAST_API(),
	SLOW_API();
	private Map<Long,Long> tasks; 
	public void setTasks(Map<Long,Long> tasks) {
		this.tasks=tasks;
	}
	@Override
	public Map<Long,Long> tasks(){
		return tasks;
	}
	@Override
	public long getCount(long task) {
		return tasks.get(task);
	}
	Token(){this(Map.of());}
	Token(Map<Long,Long> tasks){
		this.tasks=tasks;
	}
}
