package Master;

import Client.ClientReader;
import Client.ClientWriter;
import Slave.SlaveReader;
import Slave.SlaveWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Master {

    private static final Object jobQueueLock = new Object();
    private static final Object slaveWorkloadLock = new Object();
    private static final Object clientMapLock = new Object();

    private static Queue<String> jobQueue = new LinkedList<>();
    private static Map<String, Integer> slaveWorkload = new HashMap<>();
    private static Map<String, Socket> jobToClientMap = new HashMap<>();

    private static Map<String, Queue<String>> clientOutgoingQueues = new HashMap<>();
    private static Map<String, Queue<String>> slaveOutgoingQueues = new HashMap<>();

    private static final Object clientQueueLock = new Object();
    private static final Object slaveQueueLock = new Object();

    public static void main(String[] args) throws IOException {
        slaveWorkload.put("A", 0);
        slaveWorkload.put("B", 0);

        int clientPort = 5002;
        int slavePort = 5003;

        ServerSocket clientServer = new ServerSocket(clientPort);
        ServerSocket slaveServer = new ServerSocket(slavePort);

        // Accept clients
        new Thread(() -> {
            while (true) {
                try {
                    Socket s = clientServer.accept();
                    String clientId = UUID.randomUUID().toString();
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
                    // For simplicity, let slaves send their type as first message
                    Scanner sc = new Scanner(s.getInputStream());
                    String slaveType = sc.nextLine().trim();
                    registerSlave(slaveType, s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        startJobDispatcher();
    }

    private static void startJobDispatcher() {
        new Thread(() -> {
            while (true) {
                String job;
                synchronized (jobQueueLock) {
                    while (jobQueue.isEmpty()) {
                        try { jobQueueLock.wait(); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                    job = jobQueue.poll();
                }
                String bestSlave = calculateBestSlave(job);
                assignJobToSlave(job, bestSlave);
            }
        }).start();
    }

    private static String calculateBestSlave(String job) {
        String jobType = job.split("\\|")[0];
        synchronized(slaveWorkloadLock) {
            int timeA = slaveWorkload.get("A") + (jobType.equals("A") ? 2 : 10);
            int timeB = slaveWorkload.get("B") + (jobType.equals("B") ? 2 : 10);
            String chosen = (timeA <= timeB) ? "A" : "B";
            int jobTime = jobType.equals(chosen) ? 2 : 10;
            slaveWorkload.put(chosen, slaveWorkload.get(chosen) + jobTime);
            return chosen;
        }
    }

    private static void assignJobToSlave(String job, String slaveType) {
        synchronized(slaveQueueLock) {
            Queue<String> q = slaveOutgoingQueues.get(slaveType);
            if (q != null) q.add(job);
        }
    }

    public static void addJobToQueue(String job, String clientId, Socket clientSocket) {
        synchronized(jobQueueLock) {
            jobQueue.add(job);
            synchronized(clientMapLock) { jobToClientMap.put(job.split("\\|")[1], clientSocket); }
            jobQueueLock.notify();
        }
    }

    public static void jobCompleted(String jobId, String slaveType) {
        synchronized(slaveWorkloadLock) {
            slaveWorkload.put(slaveType, Math.max(0, slaveWorkload.get(slaveType) - 2));
        }
        synchronized(clientMapLock) {
            Socket clientSocket = jobToClientMap.get(jobId);
            if (clientSocket != null) {
                synchronized(clientQueueLock) {
                    Queue<String> q = clientOutgoingQueues.get(getClientIdFromSocket(clientSocket));
                    if (q != null) q.add("Job " + jobId + " completed");
                }
            }
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

    private static String getClientIdFromSocket(Socket socket) {
        // In practice, you would map socket -> clientId
        // For this skeleton, return a placeholder
        return socket.toString();
    }
}