package Master;

import java.net.Socket;

//class to handle communication with client
public class ClientHandler implements Runnable {

    Socket socket;

    //takes socket from master
    public ClientHandler(Socket s) {
        socket = s;
    }

    //TODO ovveride run method
}
