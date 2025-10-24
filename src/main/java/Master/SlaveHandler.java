package Master;

import java.net.Socket;

public class SlaveHandler implements Runnable {

    Socket socket;

    public SlaveHandler(Socket s) {
        socket = s;
    }

    //TODO ovveride run method

}
