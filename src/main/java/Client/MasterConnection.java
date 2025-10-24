package Client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MasterConnection {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public MasterConnection(String host, int port) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String msg) {
        writer.println(msg);
    }

    public String readMessage() throws IOException {
        return reader.readLine();
    }

    //TODO method that starts looping through list of jobs?
}
