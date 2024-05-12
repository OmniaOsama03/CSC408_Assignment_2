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

/*
    Omnia Osama Ahmed  1084505
    Maryam Mohammaed Ali 1079679
    Nourhan Ahmed Elmehalawy 1078096
*/

public class EventHandler_77_3 extends Thread {
      Event_77_3 event;
      Queue<Integer> queue;
      Queue<Integer> prequeue;
      Map<Integer, Integer> clientPositions; // Map to store client positions in the queue
      static Map<Integer, Socket> clientSockets; // Map to store client sockets

      //Queue locks to prevent concurrent modifications
      boolean prequeueLock = false;
      boolean queuelock = false;


    public EventHandler_77_3(Event_77_3 event) {
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
                // Generating cipher & secret key
                SecretKeySpec secretKey = SecurityUtil_77_3.generateAESKey();
                Cipher cipher = Cipher.getInstance("AES");

                // Countdown for clients in pre-queue
                if (!prequeue.isEmpty() && prequeueLock == false) {

                    Iterator<Integer> iterator = prequeue.iterator();
                        while (iterator.hasNext())
                        {
                            int clientID = iterator.next();

                                // Getting the client socket for the client ID and creating output stream
                                Socket socketClient = getClientSocket(clientID);
                                DataOutputStream out = new DataOutputStream(socketClient.getOutputStream());

                                //Calculating time minutes & seconds left
                                long timeLeftMillis = event.getScheduledTimeMillis() - System.currentTimeMillis();
                                long minutesLeft = timeLeftMillis / (60 * 1000); // Convert milliseconds to minutes
                                long secondsLeft = (timeLeftMillis / 1000) % 60;

                                // Sending time to client
                                if (minutesLeft != 0) {
                                    String encryptedMessage = SecurityUtil_77_3.encrypt("---Event will start in " + minutesLeft + " minutes. Please be patient!", cipher, secretKey);
                                    out.writeUTF(encryptedMessage);
                                } else {
                                    String encryptedMessage = SecurityUtil_77_3.encrypt("---Event will start in " + secondsLeft + " seconds. Please be patient!", cipher, secretKey);
                                    out.writeUTF(encryptedMessage);
                                }
                        }
                }

                // Checks if it's time to activate the event
                if (!event.isActive())
                {
                    long currentTimeMillis = System.currentTimeMillis();

                    if (currentTimeMillis >= event.getScheduledTimeMillis()) {

                        //Initializing queue
                        event.setActive(true);
                        initializeQueue();

                        //Updating list of events in server
                        Server_77_3.upcomingEvents.remove(event);
                        Server_77_3.activeEvents.add(event);

                    }
                }

            // Checking if there's a client in the queue while event is active
            if (event.isActive() && !queue.isEmpty() && queuelock == false) {
                // Iterate over the queue
                Iterator<Integer> iterator = queue.iterator();

                while (iterator.hasNext()) {

                    //Extracting client socket
                    int clientID = iterator.next();
                    Socket socketClient = getClientSocket(clientID);

                    // Create a Connection thread for the client
                    if (socketClient != null) {
                        Connection_77_3 connection = new Connection_77_3(clientID, socketClient, event);
                    }

                    // Removing the client from the queue
                    iterator.remove();
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
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void addToQueue(Integer client, Socket clientSocket) throws IOException {
        try {

             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

             // Generating secret key and cipher
             SecretKeySpec secretKey = SecurityUtil_77_3.generateAESKey();
             Cipher cipher = Cipher.getInstance("AES");

             // locking the queue
             queuelock = true;

              // Checking if the event is active before adding client to the queue
              if (event.isActive()) {

                // Storing the client socket in the map
                if (!clientSockets.containsKey(client))
                    clientSockets.put(client, clientSocket);

                // Storing client position in the queue
                clientPositions.put(client, queue.size() + 1);

                // Adding client to queue
                queue.add(client);

                // Unlocking queue
                queuelock = false;

                // Updating the user
                String encryptedMessage = SecurityUtil_77_3.encrypt("--- You have been added to the event: " + event.getName() + ".\n" + "Your position in the queue: " + getClientPosition(client), cipher, secretKey);
                out.writeUTF(encryptedMessage);

                //Updating the server
                System.out.println("Client " + client + " has been added to the queue for event: " + event.getName() + "\nQueue size: " + queue.size());
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


    public synchronized void addToPrequeue(Integer client, Socket clientSocket) {

        // Locking the prequeue
        prequeueLock = true;

        // Checking if the event has not started yet before adding client to the prequeue
        if (!event.isActive()) {

            // Store the client socket in the map
            if (!clientSockets.containsKey(client))
                clientSockets.put(client, clientSocket);

            // Store client position in the queue
            clientPositions.put(client, queue.size() - 1);

            // Adding client to pre-queue
            prequeue.add(client);

            // Unlocking pre-queue
            prequeueLock = false;

            System.out.println("Client " + client + " has been added to the pre-queue for event: " + event.getName());
        }


    }

    private void initializeQueue() throws IOException {

        // Randomizing the order of clients in the prequeue and add them to the main queue
        Collections.shuffle((List<Integer>) prequeue);

        // Iterate over the shuffled prequeue and move clients to the main queue
        Iterator<Integer> iterator = prequeue.iterator();

        while (iterator.hasNext()) {

            // Storing client id
            Integer client = iterator.next();

            // Removing the client from pre-queue
            iterator.remove();

            // Adding client to queue
            addToQueue(client, getClientSocket(client));
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
