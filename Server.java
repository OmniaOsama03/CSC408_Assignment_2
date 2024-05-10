import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    static ArrayList<Event> activeEvents = new ArrayList<>();
    static ArrayList<Event> upcomingEvents = new ArrayList<>();
    public static void main (String args[]) throws IOException {

        //Create an event
        Date scheduledTime = new Date(124, 4, 10, 15, 25); // May 15, 2024, 14:30
        Event sampleEvent = new Event("Event1", "Sample Event", scheduledTime);

        //Create the event handler
        EventHandler sampleEventHandler = new EventHandler(sampleEvent);


        ServerSocket listenSocket;
        try {
            int serverPort = 20000; // the server port
            listenSocket = new ServerSocket(serverPort);

            System.out.println("Server is ready and waiting for requests ... ");


            while (true) {
                Socket clientSocket = listenSocket.accept();

                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

                ObjectOutputStream objectOut = new ObjectOutputStream(clientSocket.getOutputStream());

                objectOut.writeObject(upcomingEvents);
                objectOut.writeObject(activeEvents);


                String request = in.readUTF();
                if (request.startsWith("join")) {
                    // Extract event ID from the request
                    String[] parts = request.split(" ");

                    if (parts.length < 3) {
                        out.writeUTF("Invalid request. Please review your input");
                        return;
                    }

                    int clientID = Integer.parseInt(parts[2]); //Extract the clientID
                    String eventID = parts[1]; // Extract event ID from the request

                    // Find the event in activeEvents
                    Event event = findEventByID(eventID);
                    if (event == null) {
                        out.writeUTF("Event not found.");
                    } else {
                        // Check if the event has started
                        if (event.isActive()) {
                            sampleEventHandler.addToQueue(clientID, clientSocket);

                        } else {
                            // Calculate time left until event starts
                            long timeLeftMillis = event.getScheduledTimeMillis() - System.currentTimeMillis();
                            long minutesLeft = timeLeftMillis / (60 * 1000); // Convert milliseconds to minutes

                            sampleEventHandler.addToPrequeue(clientID, clientSocket);
                            out.writeUTF("Event will start in " + minutesLeft + " minutes. You have been added to the pre-queue!");

                        }
                    }
                } else {
                    // Handle other types of requests
                    out.writeUTF("Unsupported request.");
                }

                if (sampleEventHandler.event.isActive() && !sampleEventHandler.queue.isEmpty()) {
                    // Iterate over the queue
                    Iterator<Integer> iterator = sampleEventHandler.queue.iterator();

                    while (iterator.hasNext()) {
                        try {
                            Thread.sleep(300);

                            int clientID = iterator.next();
                            iterator.remove(); // Remove the client from the queue

                            // Get the client socket for the client ID
                            Socket socketClient = sampleEventHandler.getClientSocket(clientID);
                            if (socketClient != null) {
                                // Create a Connection thread for the client
                                Connection connection = new Connection(clientID, socketClient, sampleEventHandler.event, out, in);
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Event findEventByID(String eventID) {
        // Search for the event in activeEvents
        for (Event event : activeEvents) {
            if (event.getId().equalsIgnoreCase(eventID)) {
                return event; // Event found
            }
        }

        // Search for the event in upcomingEvents
        for (Event event : upcomingEvents) {
            if (event.getId().equalsIgnoreCase(eventID)) {
                return event; // Event found
            }
        }
        return null; // Event not found
    }
}
