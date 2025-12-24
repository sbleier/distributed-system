// java
package Master;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Master {

    //object locks
    private static final Object jobQueueLock = new Object();
    private static final Object slaveWorkloadLock = new Object();
    private static final Object clientMapLock = new Object();
    private static final Object clientQueueLock = new Object();
    private static final Object slaveQueueLock = new Object();
    private static int nextClientNum = 1;

    //queue for jobs
    private static Queue<String> jobQueue = new LinkedList<>();
    //maps slave type to its current workload (only ever 2 entries)
    private static Map<String, Integer> slaveWorkload = new HashMap<>();
    //maps jobId to clientId
    private static Map<String, String> jobToClientMap = new HashMap<>();
    //map to store job's time (optimal or nonoptimal)
    private static Map<String, Integer> jobTimeMap = new HashMap<>();
    //maps clientId to its message queue
    private static Map<String, Queue<String>> clientOutgoingQueues = new HashMap<>();
    //maps slave type to its message queue
    private static Map<String, Queue<String>> slaveOutgoingQueues = new HashMap<>();



    public static void main(String[] args) throws IOException {
        //initialize slave workloads
        slaveWorkload.put("A", 0);
        slaveWorkload.put("B", 0);

        //initialize ports and server sockets
        int clientPort = 5002;
        int slavePort = 5003;
        ServerSocket clientServer = new ServerSocket(clientPort);
        ServerSocket slaveServer = new ServerSocket(slavePort);

        System.out.println("Master started. Listening for clients on " + clientPort + " and slaves on " + slavePort);

        // Accept clients
        new Thread(() -> {
            while (true) {
                try {
                    Socket s = clientServer.accept();
                    //incremental client ID
                    String clientId = "Client-" + nextClientNum++;
                    System.out.println("Client connected: " + clientId + " from " + s.getRemoteSocketAddress());
                    registerClient(clientId, s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Accept slaves
        new Thread(() -> {
            while (true) {
                try {
                    Socket s = slaveServer.accept();
                    //slaves passes in type as argument
                    Scanner sc = new Scanner(s.getInputStream());
                    String slaveType = sc.nextLine().trim();
                    System.out.println("[CONNECT] Slave connected: type=" + slaveType + " from " + s.getRemoteSocketAddress());
                    registerSlave(slaveType, s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //start dispatching jobs from client to slaves
        startJobDispatcher();
    }

    //method to dispatch jobs to slaves
    private static void startJobDispatcher() {
        new Thread(() -> {
            while (true) {
                String job;
                synchronized (jobQueueLock) {
                    while (jobQueue.isEmpty()) {
                        //wait until there's a job in the queue
                        try { jobQueueLock.wait(); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                    //when there's a job added, get it
                    job = jobQueue.poll();
                }
                System.out.println("[DISPATCH] Dispatching job: " + job);
                //find best slave
                String bestSlave = calculateBestSlave(job);
                System.out.println("[ASSIGNED] Assigned job " + job + " to Slave- " + bestSlave);
                //assign job to slave
                assignJobToSlave(job, bestSlave);
            }
        }).start();
    }

    //method to calculate best slave for a job
    private static String calculateBestSlave(String job) {
        //get job type from job string
        String jobType = job.split("\\|")[0];
        synchronized(slaveWorkloadLock) {
            //calculate expected time for each slave (considering all jobs already in workload)
            int timeA = slaveWorkload.get("A") + (jobType.equals("A") ? 2 : 10);
            int timeB = slaveWorkload.get("B") + (jobType.equals("B") ? 2 : 10);
            //choose slave with less expected time
            String chosen = (timeA <= timeB) ? "A" : "B";
            int jobTime = jobType.equals(chosen) ? 2 : 10;
            //upload workload map
            slaveWorkload.put(chosen, slaveWorkload.get(chosen) + jobTime);
            //upload job time map
            String jobId = job.split("\\|")[1];
            jobTimeMap.put(jobId, jobTime);
            return chosen;
        }
    }

    //method to add job to slave's queue
    private static void assignJobToSlave(String job, String slaveType) {
        synchronized(slaveQueueLock) {
            Queue<String> q = slaveOutgoingQueues.get(slaveType);
            if (q != null) {
                q.add(job);
                System.out.println("[QUEUE] Enqueued job for slave " + slaveType + ": " + job);
            } else {
                System.out.println("No slave queue for type " + slaveType + " (job dropped): " + job);
            }
        }

    }

    //method to add job to job queue
    public static void addJobToQueue(String job, String clientId) {
        synchronized(jobQueueLock) {
            jobQueue.add(job);
            // expect job format "type|jobId"
            String jobId = job.split("\\|")[1];
            synchronized(clientMapLock) {
                jobToClientMap.put(jobId, clientId);
            }
            System.out.println("[RECEIVED] Job " + job + " from " + clientId);
            jobQueueLock.notify();
        }
    }

    public static void jobCompleted(String jobId, String slaveType) {
        synchronized(slaveWorkloadLock) {
            //remove job's time from slave workload
            slaveWorkload.put(slaveType, Math.max(0, slaveWorkload.get(slaveType) - jobTimeMap.get(jobId)));
        }
        String clientId;
        synchronized(clientMapLock) {
            clientId = jobToClientMap.get(jobId);
        }
        if (clientId != null) {
            synchronized(clientQueueLock) {
                clientOutgoingQueues.get(clientId).add("Job " + jobId + " completed");
                System.out.println("[NOTIFIED] Sent completion of job " + jobId + " to " + clientId);
            }
        } else {
            System.out.println("No client mapping for completed job " + jobId);
        }
    }

    public static void registerClient(String clientId, Socket socket) {
        synchronized(clientQueueLock) { clientOutgoingQueues.put(clientId, new LinkedList<>()); }
        new Thread(new ClientReader(clientId, socket)).start();
        new Thread(new ClientWriter(clientId, socket)).start();
    }

    public static void registerSlave(String slaveType, Socket socket) {
        synchronized(slaveQueueLock) { slaveOutgoingQueues.put(slaveType, new LinkedList<>()); }
        new Thread(new SlaveReader(slaveType, socket)).start();
        new Thread(new SlaveWriter(slaveType, socket)).start();
    }

    public static Object getClientQueueLock() { return clientQueueLock; }
    public static Object getSlaveQueueLock() { return slaveQueueLock; }
    public static Queue<String> getClientOutgoingQueue(String clientId) { return clientOutgoingQueues.get(clientId); }
    public static Queue<String> getSlaveOutgoingQueue(String slaveType) { return slaveOutgoingQueues.get(slaveType); }

}
