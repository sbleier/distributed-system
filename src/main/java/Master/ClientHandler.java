package Master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

//class to handle communication with client
public class ClientHandler implements Runnable {

    Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    //takes socket from master
    public ClientHandler(Socket s) {
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
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("Received job from client: " + message);

                // Add job to shared queue
                Master.addJobToQueue(message, socket);

                // Send acknowledgment
                writer.println("Job " + message + " submitted to Master");
            }

        } catch (IOException e) {
            System.out.println("Client disconnected");
        }
    }
}
