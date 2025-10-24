package Client;

import java.net.Socket;

public class Client {

    public static void main(String[] args) {

        //connect to master;
        MasterConnection connection = new MasterConnection("localhost", 5001);

        //should start sending jobs to master - maybe we should make an array of jobs
        //TODO make list (maybe hashmap?) of jobs of either type A or B
        //TODO call method from MasterConnection to start sending jobs?


    }

}
