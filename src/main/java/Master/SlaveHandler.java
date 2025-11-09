package Master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SlaveHandler implements Runnable {

    Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String slaveType;

    public SlaveHandler(Socket s) {
        socket = s;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO ovveride run method
    @Override
    public void run() {
        try {
            // First message from slave tells us its type
            slaveType = reader.readLine();
            Master.registerSlave(slaveType, socket);

            // Listen for completion messages
            String message;
            while ((message = reader.readLine()) != null) {
                String[] parts = message.split("\\|");
                if (parts[0].equals("COMPLETE")) {
                    String jobId = parts[1];
                    Master.jobCompleted(jobId, slaveType);
                }
            }

        } catch (IOException e) {
            System.out.println("Slave-" + slaveType + " disconnected");
        }
    }

}
