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
import java.util.Random;

public class Server {
    static ArrayList<Event> activeEvents = new ArrayList<>();
    static ArrayList<Event> upcomingEvents = new ArrayList<>();
    static HashMap<String, EventHandler> allEvents = new HashMap<>(); //stored IDs and event handlers
    static ArrayList<String[]> credentials = new ArrayList<>();


    public static void main (String args[]) throws IOException {

        // Hardcoded credentials
        credentials.add(new String[]{SecurityUtil.applySha256("user1"), SecurityUtil.applySha256("password1")});
        credentials.add(new String[]{SecurityUtil.applySha256("user2"), SecurityUtil.applySha256("password2")});
        credentials.add(new String[]{SecurityUtil.applySha256("user3"), SecurityUtil.applySha256("password3")});


        //Create an event
        Date scheduledTime = new Date(124, 4, 12, 13, 14); // May 15, 2024, 14:30
        Date scheduledTime2 = new Date(124, 4, 12, 1, 20);
        Event sampleEvent = new Event("Ev_2", "Test you communication!", scheduledTime2);
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

                String hashedUserName = SecurityUtil.applySha256(username);
                String hashedPass = SecurityUtil.applySha256(password);

                // Authenticate the user
                boolean authenticated = authenticate(hashedUserName, hashedPass);

                // Send authentication result to the client
                if (authenticated) {
                    // Generate random OTP
                    String otp = generateOTP(6);

                    // Send OTP to the client
                    out.writeUTF(SecurityUtil.encrypt(otp, cipher, secretKey));

                    String receivedOTP = in.readUTF();
                    if(SecurityUtil.decrypt(receivedOTP, cipher, secretKey).equals(otp))
                        out.writeUTF(SecurityUtil.encrypt("authenticated", cipher, secretKey));
                    else
                        out.writeUTF(SecurityUtil.encrypt("AuthenticationDenied", cipher, secretKey));

                } else {
                    out.writeUTF(SecurityUtil.encrypt("authentication_failed", cipher, secretKey));
                    clientSocket.close(); // Close connection for failed authentication
                    break;
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
                System.out.println(decryptedRequest);
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
            System.out.println("A client has disconnected!");
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
    private static String generateOTP(int length) {
        // Generate a random OTP
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private static boolean authenticate(String username, String password) {
        for (String[] cred : credentials) {
            if (cred[0].equals(username) && cred[1].equals(password)) {
                System.out.println("Stored Credentials "+cred[0]+ " "+ cred[1] +"\nUser Credentials " + username+ " "+ password +"\n AUTHENTICATED!");
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


