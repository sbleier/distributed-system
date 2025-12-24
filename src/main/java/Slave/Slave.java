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
            MasterConnection connection = new MasterConnection("localhost", 5003);

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
            System.out.println("Slave sleeping for 2 seconds for optimal job");
        } else {
            System.out.println("Slave sleeping for 10 seconds for non-optimal job");
        }

        try {
            Thread.sleep(sleepTime * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[COMPLETED] Slave-" + slaveType + " finished job " + jobType + "|" + jobId);
        System.out.println("Slave informing master that job " + jobId + " is complete");

        // Send completion message to master
        connection.sendMessage("Job Complete|" + jobId);
    }
}
