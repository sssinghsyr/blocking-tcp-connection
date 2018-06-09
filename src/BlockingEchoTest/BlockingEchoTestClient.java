package _BlockingEchoTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class BlockingEchoTestClient {
	static final int echoingCnt = 100;
	private final int port = 12345;
	private static int _connection_count;
	private static boolean latFlag;
	private static int _buffer_size; // in Kb
	private static final ExecutorService service = Executors.newCachedThreadPool(new NamedThreadFactory("blockingTCP", false));
	//private static long _times[];
	private static List<Long> _times = new ArrayList<Long>();
	private static List<Long> _rates = new ArrayList<Long>();
	private static Semaphore semaphore ;
	private static Object lock = new Object();
	
	public static void main(String[] args) throws Exception {
		if(args.length != 3)
			throw new Exception("Wrong arg count");
		_connection_count = Integer.parseInt(args[0]);
		int size = Integer.parseInt(args[1]);
		latFlag = Integer.parseInt(args[2]) == 1 ? true : false;
		_buffer_size = size > 0 ? (size % 1024) : size;
		//_times = new long[echoingCnt * _connection_count];
		semaphore = new Semaphore(_connection_count * -1);
		if(latFlag)
			new BlockingEchoTestClient(12345).printLatencyResult();
		else
			new BlockingEchoTestClient(12345).printThrPutResult();
		service.shutdownNow();
		
	}
	
	public BlockingEchoTestClient(int port) throws IOException {
		if(latFlag) {
		for(int i=0;i<_connection_count;i++)
			service.execute(new ConnectionLatStream(i));
		}else {
			for(int i=0;i<_connection_count;i++)
				service.execute(new ConnectionThrStream(i));
		}
	}
	
	public void printThrPutResult() throws InterruptedException {
		semaphore.acquire();
		_rates.sort((a,b) -> a > b ? 1 : -1);
		System.out.printf("Socket Throughput was min %,d K/s, max %,d K/s%n", _rates.get(0), _rates.get(_rates.size()-1));
	}
	
	public void printLatencyResult() throws InterruptedException {
		semaphore.acquire();
		_times.sort((a,b) -> a > b ? 1 : -1);
        System.out.printf("Socket latency was 1/50/99%%tile %.1f/%.1f/%.1f us%n",
                _times.get(_times.size() / 100) / 1e3,
                _times.get(_times.size() / 2) / 1e3,
                _times.get(_times.size() - _times.size() / 100 - 1) / 1e3
        );
	}
	class ConnectionThrStream implements Runnable {
		//private int i;
		SocketChannel sc;
		
		public ConnectionThrStream(int idx) throws IOException{
			//this.i = idx;
			sc = SocketChannel.open(new InetSocketAddress("localhost", port));
		}
		
        @Override
        public void run() {
        	ByteBuffer bb = ByteBuffer.allocateDirect(4096);
            try {
            	long start = System.nanoTime();
                int runs = 10000;
                for (int i = 0; i < runs; i++) {
                    bb.position(0);
                    bb.limit(1024);
                    sc.write(bb);

                    bb.clear();
                    sc.read(bb);
                }
                long time = System.nanoTime() - start;

                sc.close();
                synchronized (lock) {
					_rates.add(runs * 1000000L / time);
				}
                semaphore.release();
                //System.out.println("Thread ends:"+i);
            } catch (Exception e) {
                if (!service.isShutdown())
                    e.printStackTrace();
            } finally {
                try {
                	semaphore.release();
                	sc.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
	class ConnectionLatStream implements Runnable {
		//private int i;
		SocketChannel sc;
		List<Long> times;
		
		public ConnectionLatStream(int idx) throws IOException{
			//this.i = idx;
			sc = SocketChannel.open(new InetSocketAddress("localhost", port));
			times = new ArrayList<Long>();
		}
		
        @Override
        public void run() {
        	ByteBuffer bb = ByteBuffer.allocateDirect(_buffer_size+10);
            try {
            	//sc.connect(new InetSocketAddress("localhost", port));
            	//long times[] = new long[echoingCnt];
                for (int i = -10; i < echoingCnt; i++) {
                    long start = System.nanoTime();
                    bb.position(0);
                    bb.limit(_buffer_size);
                    sc.write(bb);

                    bb.clear();
                    sc.read(bb);
                    long end = System.nanoTime();
                    long err = System.nanoTime() - end;
                    long time = end - start - err;
                    if (i >= 0)
                        times.add(time);
                }
                sc.close();
                synchronized (lock) {
					_times.addAll(times);
				}
                semaphore.release();
                //System.out.println("Thread ends:"+i);
            } catch (Exception e) {
                if (!service.isShutdown())
                    e.printStackTrace();
            } finally {
                try {
                	semaphore.release();
                	sc.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
