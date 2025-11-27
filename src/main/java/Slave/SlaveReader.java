package Slave;

import Master.Master;

import java.io.IOException;
import java.net.Socket;

public class SlaveReader implements Runnable {
    private final String slaveType;
    private final MasterConnection masterConnection;

    public SlaveReader(String slaveType, Socket socket) {
        this.slaveType = slaveType;
        this.masterConnection = new MasterConnection(socket);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = masterConnection.readMessage();
                if (msg == null) break; // connection closed
                // If this is a job completion from the slave, notify master
                if (msg.startsWith("JOB_DONE|")) {
                    String jobId = msg.split("\\|")[1];
                    Master.jobCompleted(jobId, slaveType);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}