import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Server {
    static ArrayList<Event> activeEvents = new ArrayList<>();
    static ArrayList<Event> upcomingEvents = new ArrayList<>();
    static HashMap<String, EventHandler> allEvents = new HashMap<>(); //stored IDs and event handlers
    static ArrayList<String[]> credentials = new ArrayList<>();


    public static void main (String args[]) throws IOException {

        // Hardcoded credentials
        credentials.add(new String[]{"user1", "password1"});
        credentials.add(new String[]{"user2", "password2"});
        credentials.add(new String[]{"user3", "password3"});


        //Create an event
        Date scheduledTime = new Date(124, 4, 12, 13, 30); // May 15, 2024, 14:30
        TicketEvent ticketEvent = new TicketEvent("Ev_1", "Book a Ticket!", scheduledTime);

        //Create all event handlers
        for(int i = 0; i < activeEvents.size(); i++) {
            allEvents.put(activeEvents.get(i).getId(), new EventHandler(activeEvents.get(i)));
        }

        for(int i = 0; i < upcomingEvents.size(); i++) {
            allEvents.put(upcomingEvents.get(i).getId(), new EventHandler(upcomingEvents.get(i)));
        }


        ServerSocket listenSocket;
        try {
            int serverPort = 40000; // the server port
            listenSocket = new ServerSocket(serverPort);

            System.out.println("Server is ready and waiting for requests ... ");


            while (true) {
                Socket clientSocket = listenSocket.accept();

                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());


                // Generate AES key with appropriate length
                SecretKeySpec secretKey = SecurityUtil.generateAESKey();
                Cipher cipher = Cipher.getInstance("AES");

                // Read encrypted username and password from the client
                String encryptedUsername = in.readUTF();
                String encryptedPassword = in.readUTF();

                // Decrypt username and password using the secretKey
                String username = SecurityUtil.decrypt(encryptedUsername, cipher, secretKey);
                String password = SecurityUtil.decrypt(encryptedPassword, cipher, secretKey);

                // Authenticate the username and password
                if (authenticate(username, password)) {
                    // Send authentication success message to the client
                    out.writeUTF("authenticated");
                } else {
                    // Send authentication failure message to the client
                    out.writeUTF("authentication_failed");
                    // Close the connection
                    clientSocket.close();
                    continue;
                }

                //Sending the upcoming events
                if(upcomingEvents.size() == 0)
                {
                    String noUpcoming = "---Upcoming Events: \nNone!";
                    out.writeUTF(SecurityUtil.encrypt(noUpcoming, cipher, secretKey));
                }
                else {
                    String allUpcoming = "---Upcoming Events: ";
                    for (int i = 0; i < upcomingEvents.size(); i++) {
                        allUpcoming += "\n ID: " + upcomingEvents.get(i).getId() + "- Name: " + upcomingEvents.get(i).getName() + " - Time: " + upcomingEvents.get(i).getScheduledTime();
                    }

                    out.writeUTF(SecurityUtil.encrypt(allUpcoming, cipher, secretKey));
                }

                //Sending the active events
                if(activeEvents.size() == 0)
                {
                    String noActive = "---Active Events: \nNone!";
                    out.writeUTF(SecurityUtil.encrypt(noActive, cipher, secretKey));
                }
                else {
                    String allActive = "---Active Events: ";
                    for (int i = 0; i < activeEvents.size(); i++) {
                        allActive += "\n ID: " + activeEvents.get(i).getId() + "- Name: " + activeEvents.get(i).getName() + " - Time: " + activeEvents.get(i).getScheduledTime();
                    }

                    out.writeUTF(SecurityUtil.encrypt(allActive, cipher, secretKey));
                }

                String encryptedRequest = in.readUTF();
                String decryptedRequest = SecurityUtil.decrypt(encryptedRequest, cipher, secretKey);

                if (decryptedRequest.startsWith("join")) {
                    // Extract event ID from the request
                    String[] parts = decryptedRequest.split(" ");

                    if (parts.length < 3) {
                        String encryptedMessage = SecurityUtil.encrypt("---Invalid request. Please review your input", cipher, secretKey);
                        out.writeUTF(encryptedMessage);
                        return;
                    }

                    int clientID = Integer.parseInt(parts[2]); //Extract the clientID
                    String eventID = parts[1]; // Extract event ID from the request

                    // Find the event in activeEvents
                    Event event = findEventByID(eventID);
                    if (event == null) {
                        String encryptedMessage = SecurityUtil.encrypt("---Event not found.", cipher, secretKey);
                        out.writeUTF(encryptedMessage);
                    } else {
                        // Check if the event has started
                        if (event.isActive()) {
                            allEvents.get(eventID).addToQueue(clientID, clientSocket);

                        } else {
                            allEvents.get(eventID).addToPrequeue(clientID, clientSocket);
                        }
                    }
                } else {
                    // Handle other types of requests
                    String encryptedMessage = SecurityUtil.encrypt("---Unsupported request.", cipher, secretKey);
                    out.writeUTF(encryptedMessage);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
    private static boolean authenticate(String username, String password) {
        // Check if the provided username exists and the password matches
        for (String[] cred : credentials) {
            if (cred[0].equals(username) && cred[1].equals(password)) {
                return true;
            }
        }
        return false;
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


