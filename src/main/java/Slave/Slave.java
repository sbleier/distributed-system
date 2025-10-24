package Slave;

import java.net.Socket;

public class Slave {


    public static void main(String[] args) {

        //start listening for instructions
        MasterConnection connection = new MasterConnection("localhost", 5000);
    }
}
