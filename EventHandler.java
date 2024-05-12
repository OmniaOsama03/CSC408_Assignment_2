import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;


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

        while (true) {

            try {
                SecretKeySpec secretKey = SecurityUtil.generateAESKey();
                Cipher cipher = Cipher.getInstance("AES");

                if (!prequeue.isEmpty()) {
                    Iterator<Integer> iterator = prequeue.iterator();

                    synchronized (prequeue) {
                        while (iterator.hasNext()) {
                            int clientID = iterator.next();

                            try {
                                Thread.sleep(1000);

                                // Get the client socket for the client ID
                                Socket socketClient = getClientSocket(clientID);
                                DataOutputStream out = new DataOutputStream(socketClient.getOutputStream());

                                long timeLeftMillis = event.getScheduledTimeMillis() - System.currentTimeMillis();
                                long minutesLeft = timeLeftMillis / (60 * 1000); // Convert milliseconds to minutes
                                long secondsLeft = (timeLeftMillis / 1000) % 60;

                                if (minutesLeft != 0) {
                                    String encryptedMessage = SecurityUtil.encrypt("---Event will start in " + minutesLeft + " minutes. Please be patient!", cipher, secretKey);
                                    out.writeUTF(encryptedMessage);
                                } else {
                                    String encryptedMessage = SecurityUtil.encrypt("---Event will start in " + secondsLeft + " seconds. Please be patient!", cipher, secretKey);
                                    out.writeUTF(encryptedMessage);
                                }

                            } catch (IOException e) {
                                if (prequeue.contains(clientID)) {
                                    System.out.println("Socket closed for client: " + clientID + "! They have been removed from the prequeue");
                                    iterator.remove(); // Remove the client from the prequeue
                                    System.out.println("Size of pre-queue after removal: " + prequeue.size());
                                } else {
                                    System.out.println("Socket closed for client: " + clientID + "! Their position will remain in the queue!");
                                }
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } catch (IllegalBlockSizeException e) {
                                throw new RuntimeException(e);
                            } catch (BadPaddingException e) {
                                throw new RuntimeException(e);
                            } catch (InvalidKeyException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }


                if (!event.isActive())// Checks if it's time to activate the event
                {
                    long currentTimeMillis = System.currentTimeMillis();

                    if (currentTimeMillis >= event.getScheduledTimeMillis()) {
                        event.setActive(true);
                        try {

                            initializeQueue();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        //Updating list of events in server
                        Server.upcomingEvents.remove(event);
                        Server.activeEvents.add(event);
                    }
                }


                if (event.isActive() && !queue.isEmpty()) {
                    try {
                        Thread.sleep(200);

                        // Iterate over the queue
                        Iterator<Integer> iterator = queue.iterator();

                        while (iterator.hasNext()) {
                            int clientID = iterator.next();
                            iterator.remove(); // Remove the client from the queue

                            // Get the client socket for the client ID
                            Socket socketClient = getClientSocket(clientID);
                            if (socketClient != null) {
                                // Create a Connection thread for the client
                                Connection connection = new Connection(clientID, socketClient, event);
                            }
                        }

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

              Thread.sleep(1);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addToQueue(Integer client, Socket clientSocket) throws IOException {
        try {
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            SecretKeySpec secretKey = SecurityUtil.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            // Check if the event is active before adding client to the queue
            if (event.isActive()) {
                if (!clientSockets.containsKey(client))
                    clientSockets.put(client, clientSocket); // Store the client socket in the map

                clientPositions.put(client, queue.size() + 1); // Store client position in the queue

                queue.add(client);

                String encryptedMessage = SecurityUtil.encrypt("--- You have been added to the event: " + event.getName() + ".\n" + "Your position in the queue: " + getClientPosition(client), cipher, secretKey);
                out.writeUTF(encryptedMessage);

                System.out.println("Client " + client + " has been added to the queue for event: " +
                        event.getName() + "\nQueue size: " + queue.size());
            }
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
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

    private void initializeQueue() throws IOException {
        // Randomize the order of clients in the prequeue and add them to the main queue
        Collections.shuffle((List<Integer>) prequeue);

        // Create an iterator for the shuffled prequeue
        Iterator<Integer> iterator = prequeue.iterator();

        // Iterate over the shuffled prequeue and move clients to the main queue
        while (iterator.hasNext()) {
            Integer client = iterator.next();
            iterator.remove(); // Remove the client using the iterator
            System.out.println("CLIENT " + client + "(ENTERED QUEUE): " + System.nanoTime()/1e6);
            addToQueue(client, getClientSocket(client));
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
