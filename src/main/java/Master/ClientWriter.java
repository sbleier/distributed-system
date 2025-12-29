/*
Sara Bleier
T00510465
Leora Spinner
T00498388
 */
package Master;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;

public class ClientWriter implements Runnable {
    private final String clientId;
    private final Socket socket;

    public ClientWriter(String clientId, Socket socket) { this.clientId = clientId; this.socket = socket; }

    @Override
    public void run() {
        try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            while (true) {
                Queue<String> queue;
                synchronized (Master.getClientQueueLock()) {
                    queue = Master.getClientOutgoingQueue(clientId);
                }
                if (queue != null && !queue.isEmpty()) {
                    synchronized (queue) {
                        String msg = queue.poll();
                        if (msg != null) writer.println(msg);
                    }
                }
                Thread.sleep(50);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}