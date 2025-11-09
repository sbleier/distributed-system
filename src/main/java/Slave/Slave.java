package Slave;

import java.io.IOException;
import java.net.Socket;

public class Slave {


    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Slave <A|B>");
            return;
        }

        String slaveType = args[0].toUpperCase();
        System.out.println("Starting Slave-" + slaveType);

        try {
            // Connect to master
            MasterConnection connection = new MasterConnection("localhost", 5000);

            // Register with master
            connection.sendMessage(slaveType);

            // Listen for jobs
            while (true) {
                String job = connection.readMessage();
                if (job != null) {
                    processJob(job, slaveType, connection);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processJob(String job, String slaveType, MasterConnection connection) {
        String[] parts = job.split("\\|");
        String jobType = parts[0];
        String jobId = parts[1];

        System.out.println("Received job: " + job);

        // Determine sleep time based on job type
        int sleepTime = jobType.equals(slaveType) ? 2 : 10;

        if (sleepTime == 2) {
            System.out.println("Sleeping for 2 seconds for optimal job type…, be back soon");
        } else {
            System.out.println("Sleeping for 10 seconds for non-optimal job type…, be back soon");
        }

        try {
            Thread.sleep(sleepTime * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Job " + jobId + " complete");

        // Send completion message to master
        connection.sendMessage("COMPLETE|" + jobId);
    }
}
