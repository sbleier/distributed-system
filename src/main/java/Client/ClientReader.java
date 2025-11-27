package Client;

import Master.Master;

import java.io.*;
import java.net.Socket;

public class ClientReader implements Runnable {
    private final String clientId;
    private final Socket socket;

    public ClientReader(String clientId, Socket socket) {
        this.clientId = clientId;
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String msg;
            while ((msg = reader.readLine()) != null) {
                // Assume msg format: "JobType|JobId"
                Master.addJobToQueue(msg, clientId, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}