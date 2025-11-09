package Master;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Master {
    //Lock for the job queue - when clients add jobs
    private static Object sharedObjJobQueue = new Object();
    //Lock for slave status
    private static Object sharedObjSlaveStatus = new Object();
    //Lock for client connections
    private static Object sharedObjClientMap = new Object();

    //====SHARED DATA STRUCTURES - ACCESSED BY MULTIPLE THREADS - MUST BE PROTECTED
    //Queue of jobs waiting to be assigned to slaves
    private static Queue<String> jobQueue = new LinkedList<>();
    //track each slaves current workload
    private static Map<String, Integer> slaveWorkload = new HashMap<>();
    //Map jobIds to the client socket that submitted them
    private static Map<String, Socket> jobToClientMap = new HashMap<>();
    //Map to store slave sockets (SlaveA and SlaveB)
    private static Map<String, Socket> slaveConnections = new HashMap<>();


    public static void main(String[] args) {
        //initilize slave workload to 0
        slaveWorkload.put("A", 0);
        slaveWorkload.put("B", 0);

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

    // Job dispatcher thread (consumer in producer-consumer pattern)
    private static void startJobDispatcher() {
        new Thread(() -> {
            while (true) {
                String job = null;

                synchronized(sharedObjJobQueue) {
                    while (jobQueue.isEmpty()) {
                        try {
                            sharedObjJobQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    job = jobQueue.poll();
                }

                if (job != null) {
                    String bestSlave = calculateBestSlave(job);
                    System.out.println("Assigning job " + job + " to Slave-" + bestSlave);
                    assignJobToSlave(job, bestSlave);
                }
            }
        }).start();
    }

    // Calculate which slave should receive the job based on current workload
    private static String calculateBestSlave(String job) {
        String[] parts = job.split("\\|");
        String jobType = parts[0];

        synchronized(sharedObjSlaveStatus) {
            int slaveALoad = slaveWorkload.get("A");
            int slaveBLoad = slaveWorkload.get("B");

            // Calculate completion time for each slave
            int timeIfSlaveA = slaveALoad + (jobType.equals("A") ? 2 : 10);
            int timeIfSlaveB = slaveBLoad + (jobType.equals("B") ? 2 : 10);

            System.out.println("Load calculation: Slave-A would take " + timeIfSlaveA +
                    " sec, Slave-B would take " + timeIfSlaveB + " sec");

            // Choose slave with lower completion time
            String chosenSlave = (timeIfSlaveA <= timeIfSlaveB) ? "A" : "B";

            // Update workload
            int jobTime = jobType.equals(chosenSlave) ? 2 : 10;
            slaveWorkload.put(chosenSlave, slaveWorkload.get(chosenSlave) + jobTime);

            return chosenSlave;
        }
    }

    // Send job to the appropriate slave
    private static void assignJobToSlave(String job, String slaveType) {
        synchronized(sharedObjSlaveStatus) {
            Socket slaveSocket = slaveConnections.get(slaveType);
            if (slaveSocket != null) {
                try {
                    java.io.PrintWriter writer = new java.io.PrintWriter(slaveSocket.getOutputStream(), true);
                    writer.println(job);
                } catch (IOException e) {
                    System.out.println("Error sending job to Slave-" + slaveType);
                    e.printStackTrace();
                }
            }
        }
    }

    // Add job to queue (called by ClientHandler)
    public static void addJobToQueue(String job, Socket clientSocket) {
        synchronized(sharedObjJobQueue) {
            jobQueue.add(job);
            System.out.println("Job added to queue: " + job);

            synchronized(sharedObjClientMap) {
                String jobId = job.split("\\|")[1];
                jobToClientMap.put(jobId, clientSocket);
            }

            sharedObjJobQueue.notify();
        }
    }

    // Handle job completion (called by SlaveHandler)
    public static void jobCompleted(String jobId, String slaveType) {
        synchronized(sharedObjSlaveStatus) {
            slaveWorkload.put(slaveType, Math.max(0, slaveWorkload.get(slaveType) - 2));
            System.out.println("Slave-" + slaveType + " completed job " + jobId);
        }

        // Notify the client
        synchronized(sharedObjClientMap) {
            Socket clientSocket = jobToClientMap.get(jobId);
            if (clientSocket != null) {
                try {
                    java.io.PrintWriter writer = new java.io.PrintWriter(clientSocket.getOutputStream(), true);
                    writer.println("DISTRIB SYS – Job Completion Confirmation for " + jobId);
                    System.out.println("Notified client of completion for job " + jobId);
                } catch (IOException e) {
                    System.out.println("Error notifying client");
                    e.printStackTrace();
                }
            }
        }
    }

    // Register slave connection (called by SlaveHandler)
    public static void registerSlave(String slaveType, Socket socket) {
        synchronized(sharedObjSlaveStatus) {
            slaveConnections.put(slaveType, socket);
            System.out.println("Slave-" + slaveType + " registered");
        }
    }
}