import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.net.*;


public class EventHandler extends Thread {
    Event event;
    Queue<Integer> queue;
    Queue<Integer> prequeue;
    Map<Integer, Integer> clientPositions; // Map to store client positions in the queue
    static Map<Integer, Socket> clientSockets; // Map to store client sockets


    public EventHandler(Event event) {
        this.event = event;
        this.queue = new LinkedList<>();
        this.prequeue = new LinkedList<>();
        this.clientPositions = new HashMap<>();
        this.clientSockets = new HashMap<>();

        this.start();
    }

    @Override
    public void run() {

        while(true) {

            if(!event.isActive())// Checks if it's time to activate the event
            {
                long currentTimeMillis = System.currentTimeMillis();

                if (currentTimeMillis >= event.getScheduledTimeMillis()) {
                    event.setActive(true);
                    initializeQueue();

                    //Updating list of events in server
                    Server.upcomingEvents.remove(event);
                    Server.activeEvents.add(event);
                }
            }


            /*if (event.isActive() && !queue.isEmpty()) {
                // Iterate over the queue
                Iterator<Integer> iterator = queue.iterator();

                while (iterator.hasNext()) {
                    try {
                        Thread.sleep(300);

                        int clientID = iterator.next();
                        iterator.remove(); // Remove the client from the queue

                        // Get the client socket for the client ID
                        Socket socketClient = getClientSocket(clientID);
                        if (socketClient != null) {
                            // Create a Connection thread for the client
                            Connection connection = new Connection(clientID, socketClient, event, out, in);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                */

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void addToQueue(Integer client, Socket clientSocket) throws IOException {

        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

        // Check if the event is active before adding client to the queue
        if (event.isActive()) {
            if (!clientSockets.containsKey(client))
                clientSockets.put(client, clientSocket); // Store the client socket in the map

            clientPositions.put(client, queue.size() + 1); // Store client position in the queue

            queue.add(client);
            out.writeUTF("You have been added to the event: " + event.getName() + ". \n Your position in the queue: " + queue.size());
            System.out.println("Client " + client + " has been added to the queue for event: " + event.getName() + "\n Queue size:" + queue.size());

        }
    }

    public void addToPrequeue(Integer client, Socket clientSocket) {
        // Check if the event has not started yet before adding client to the prequeue
        if (!event.isActive()) {
            if (!clientSockets.containsKey(client))
                clientSockets.put(client, clientSocket); // Store the client socket in the map


            clientPositions.put(client, queue.size() - 1); // Store client position in the queue

            prequeue.add(client);
            System.out.println("Client " + client + " has been added to the pre-queue for event: " + event.getName());
        }
    }

    private void initializeQueue() {
        // Randomize the order of clients in the prequeue and add them to the main queue
        Collections.shuffle((List<Integer>) prequeue);

        // Create an iterator for the shuffled prequeue
        Iterator<Integer> iterator = prequeue.iterator();

        // Iterate over the shuffled prequeue and move clients to the main queue
        while (iterator.hasNext()) {
            Integer client = iterator.next();
            iterator.remove(); // Remove the client using the iterator
            queue.add(client); // Add the removed client to the main queue
            clientPositions.put(client, queue.size() - 1); // Store client position in the queue
        }
        System.out.println("Queue initialized for event: " + event.getName());
    }

    public int getClientPosition(int clientId) {
        // Get the position of the client in the queue
        return clientPositions.get(clientId);
    }

    public void setClientSocket(Integer clientId, Socket socket) {
        // Store the client socket in the map
        clientSockets.put(clientId, socket);
    }

    public Socket getClientSocket(Integer clientId) {
        // Retrieve the client socket from the map
        return clientSockets.get(clientId);
    }
}