/*
Sara Bleier
T00510465
Leora Spinner
T00498388
 */
package Master;

import Slave.MasterConnection;

import java.net.Socket;
import java.util.Queue;

public class SlaveWriter implements Runnable {
    private final String slaveType;
    private final MasterConnection masterConnection;

    public SlaveWriter(String slaveType, Socket socket) {
        this.slaveType = slaveType;
        this.masterConnection = new MasterConnection(socket);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Queue<String> queue;
                synchronized (Master.getSlaveQueueLock()) {
                    queue = Master.getSlaveOutgoingQueue(slaveType);
                }

                if (queue != null && !queue.isEmpty()) {
                    String job;
                    synchronized (queue) {
                        job = queue.poll();
                    }
                    if (job != null) {
                        masterConnection.sendMessage(job); // send job to slave
                    }
                }

                Thread.sleep(50); // small delay to prevent busy-waiting
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}