import java.util.ArrayList;
import java.util.List;

public class OrderThreadManager {
    private final List<Thread> serverThreads = new ArrayList<>();
    private final ThreadedOrderProcessor processor;

    public OrderThreadManager(ThreadedOrderProcessor processor) {
        this.processor = processor;
    }

    public void startServers(int count) {
        for (int i = 1; i <= count; i++) {
            ServerThread task = new ServerThread("Server-" + i, processor);
            Thread thread = new Thread(task);
            thread.start();
            serverThreads.add(thread);
        }
    }

    public void stopServers() {
        for (Thread thread : serverThreads) {
            thread.interrupt();
        }
    }
}