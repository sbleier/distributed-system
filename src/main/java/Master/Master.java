package Master;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Master {

    public static void main(String[] args) {

        int clientPort = 5001;
        int slavePort = 5000;
        try{

            ServerSocket clientSocket = new ServerSocket(clientPort);
            ServerSocket slaveSocket = new ServerSocket(slavePort);

            //thread for client
            new Thread(() -> {
                while (true) {
                    try {
                        Socket s = clientSocket.accept();
                        ClientHandler ch = new ClientHandler(s);
                        new Thread(ch).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            //thread for slaves
            new Thread(() -> {
                while (true) {
                    try {
                        Socket s = slaveSocket.accept();
                        SlaveHandler sh = new SlaveHandler(s);
                        new Thread(sh).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
