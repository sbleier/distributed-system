package Master;

import java.net.ServerSocket;
import java.net.Socket;

public class Master {

    public static void main(String[] args) {

        int port = 5000;

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Master listening on port " + port);

            Socket s = ss.accept();

            while (true) {

            }

        }


    }
}
