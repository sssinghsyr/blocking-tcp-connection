package _BlockingEchoTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class BlockingEchoTestServer {
    private final ExecutorService service = Executors.newCachedThreadPool(new NamedThreadFactory("echo", true));
    private final ServerSocketChannel ssc;

    public BlockingEchoTestServer(int port) throws IOException, InterruptedException {
        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        try {
            ssc.socket().bind(new InetSocketAddress(port));
        } catch (IOException e) {
            Thread.sleep(100);
            ssc.socket().bind(new InetSocketAddress(port));
        }
        service.execute(new Acceptor());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 12345;
        System.out.println("Listening on port " + port);
        BlockingEchoTestServer es = new BlockingEchoTestServer(port);
        System.in.read();
    }

    public void stop() {
        service.shutdown();
        try {
            ssc.close();
        } catch (IOException ignored) {
        }
    }

    class Acceptor implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    SocketChannel sc = ssc.accept();
                    service.execute(new ConnectionHandler(sc));
                }
            } catch (Exception e) {
                if (!service.isShutdown())
                    e.printStackTrace();
            } finally {
                try {
                    ssc.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    static class ConnectionHandler implements Runnable {
        private final SocketChannel sc;

        public ConnectionHandler(SocketChannel sc) {
            this.sc = sc;
        }

        @Override
        public void run() {
            try {
                ByteBuffer bb = ByteBuffer.allocateDirect(1024); // Maximum 1MB data flow
                while (sc.isOpen()) {
                    if (sc.read(bb) < 0)
                        break;
                    bb.flip();
                    sc.write(bb);
                    bb.flip();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    sc.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}

class NamedThreadFactory implements ThreadFactory {
    private final String name;
    private final boolean daemon;

    public NamedThreadFactory(String name, boolean daemon) {
        this.name = name;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, name);
        t.setDaemon(daemon);
        return t;
    }
}