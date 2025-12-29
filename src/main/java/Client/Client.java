/*
Sara Bleier
T00510465
Leora Spinner
T00498388
 */
package Client;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        // Connect to master
        MasterConnection connection = new MasterConnection("localhost", 5002);

        //should start sending jobs to master - maybe we should make an array of jobs

        // Start thread to listen for completion messages
        new Thread(() -> {
            try {
                String message;
                while ((message = connection.readMessage()) != null) {
                    System.out.println("\n[COMPLETED] " + message);
                }
            } catch (IOException e) {
                System.out.println("Disconnected from Master");
            }
        }).start();

        // Get jobs from user
        Scanner scanner = new Scanner(System.in);
        int jobIdCounter = 1;
        while (true) {
            System.out.print("Enter job type (A or B): ");
            String type = scanner.nextLine().trim().toUpperCase();

            if (type.isEmpty()) {
                continue;
            }

            String id = String.valueOf(jobIdCounter);
            jobIdCounter++;

            String job = type + "|" + id;
            connection.sendMessage(job);
            System.out.println("Sent job: " + job);
        }

    }
}
