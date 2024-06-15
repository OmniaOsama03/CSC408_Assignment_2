import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;


public class Server_77_3 {

    static ArrayList<Event_77_3> activeEvents = new ArrayList<>();
    static ArrayList<Event_77_3> upcomingEvents = new ArrayList<>();
    static HashMap<String, EventHandler_77_3> allEvents = new HashMap<>(); //stored IDs and event handlers
    static ArrayList<String[]> credentials = new ArrayList<>();


    public static void main (String args[]) throws IOException {

        // Storing hardcoded credentials
        credentials.add(new String[]{SecurityUtil_77_3.applySha256("user1"), SecurityUtil_77_3.applySha256("password1")});
        credentials.add(new String[]{SecurityUtil_77_3.applySha256("user2"), SecurityUtil_77_3.applySha256("password2")});
        credentials.add(new String[]{SecurityUtil_77_3.applySha256("user3"), SecurityUtil_77_3.applySha256("password3")});


        // Scheduling times for events
        Date scheduledTime = new Date(124, 4, 14, 15, 55); // May 15, 2024, 14:30
        Date scheduledTime2 = new Date(124, 4, 14, 1, 20);

        // Creating sample events
        Event_77_3 sampleEvent = new Event_77_3("Ev_2", "Test you communication!", scheduledTime2);
        TicketEvent_77_3 ticketEvent = new TicketEvent_77_3("Ev_1", "Book a Ticket!", scheduledTime);


        // Creating event handlers for active events
        for(int i = 0; i < activeEvents.size(); i++) {
            allEvents.put(activeEvents.get(i).getId(), new EventHandler_77_3(activeEvents.get(i)));
        }

        // Creating event handlers for upcoming events
        for(int i = 0; i < upcomingEvents.size(); i++) {
            allEvents.put(upcomingEvents.get(i).getId(), new EventHandler_77_3(upcomingEvents.get(i)));
        }

        ServerSocket listenSocket;
        try {
            int serverPort = 40000; // the server port
            listenSocket = new ServerSocket(serverPort);

            System.out.println("Server is ready and waiting for requests ... ");


            while (true) {
                Socket clientSocket = listenSocket.accept();

                // Creating the data streams
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());


                // Generating the cipher & secret key
                SecretKeySpec secretKey = SecurityUtil_77_3.generateAESKey();
                Cipher cipher = Cipher.getInstance("AES");

                // Reading encrypted username and password from the client
                String encryptedUsername = in.readUTF();
                String encryptedPassword = in.readUTF();

                // Decrypting username and password using the secretKey
                String username = SecurityUtil_77_3.decrypt(encryptedUsername, cipher, secretKey);
                String password = SecurityUtil_77_3.decrypt(encryptedPassword, cipher, secretKey);

                // Hashing the username and password for comparison
                String hashedUserName = SecurityUtil_77_3.applySha256(username);
                String hashedPass = SecurityUtil_77_3.applySha256(password);

                // Authenticating the user
                boolean authenticated = authenticate(hashedUserName, hashedPass);


                if (authenticated) {
                    // Generating random OTP
                    String otp = generateOTP(6);

                    // Sending the OTP to the client
                    out.writeUTF(SecurityUtil_77_3.encrypt(otp, cipher, secretKey));

                    String receivedOTP = in.readUTF();

                    // Checking if received OTP matches generated  one
                    if(SecurityUtil_77_3.decrypt(receivedOTP, cipher, secretKey).equals(otp))
                        out.writeUTF(SecurityUtil_77_3.encrypt("authenticated", cipher, secretKey));
                    else
                        out.writeUTF(SecurityUtil_77_3.encrypt("AuthenticationDenied", cipher, secretKey));


                } else {

                    //terminating the connection if authentication failed
                    out.writeUTF(SecurityUtil_77_3.encrypt("authentication_failed", cipher, secretKey));
                    clientSocket.close();
                    break;
                }

                // Sending the upcoming events to the user
                if(upcomingEvents.size() == 0)
                {
                    String noUpcoming = "---Upcoming Events: \nNone!";
                    out.writeUTF(SecurityUtil_77_3.encrypt(noUpcoming, cipher, secretKey));
                }
                else {
                    String allUpcoming = "---Upcoming Events: ";

                    for (int i = 0; i < upcomingEvents.size(); i++) {
                        allUpcoming += "\n ID: " + upcomingEvents.get(i).getId() + "- Name: " + upcomingEvents.get(i).getName() + " - Time: " + upcomingEvents.get(i).getScheduledTime();
                    }

                    out.writeUTF(SecurityUtil_77_3.encrypt(allUpcoming, cipher, secretKey));
                }

                //Sending the active events to the user
                if(activeEvents.size() == 0)
                {
                    String noActive = "---Active Events: \nNone!";
                    out.writeUTF(SecurityUtil_77_3.encrypt(noActive, cipher, secretKey));
                }
                else {
                    String allActive = "---Active Events: ";
                    for (int i = 0; i < activeEvents.size(); i++) {
                        allActive += "\n ID: " + activeEvents.get(i).getId() + "- Name: " + activeEvents.get(i).getName() + " - Time: " + activeEvents.get(i).getScheduledTime();
                    }

                    out.writeUTF(SecurityUtil_77_3.encrypt(allActive, cipher, secretKey));
                }

                // Receiving join request
                String encryptedRequest = in.readUTF();
                String decryptedRequest = SecurityUtil_77_3.decrypt(encryptedRequest, cipher, secretKey);


                if (decryptedRequest.startsWith("join")) {

                    // Tokenizing the request
                    String[] parts = decryptedRequest.split(" ");

                    //Handling invalid request
                    if (parts.length < 3) {
                        String encryptedMessage = SecurityUtil_77_3.encrypt("---Invalid request. Please review your input", cipher, secretKey);
                        out.writeUTF(encryptedMessage);
                        return;
                    }

                    // Extract event ID & client ID from the request
                    int clientID = Integer.parseInt(parts[2]);
                    String eventID = parts[1];

                    // Find the event based on received ID
                    Event_77_3 event = findEventByID(eventID);

                    if (event == null) {

                        // Handling non-existent event ID
                        String encryptedMessage = SecurityUtil_77_3.encrypt("---Event not found.", cipher, secretKey);
                        out.writeUTF(encryptedMessage);

                    } else {

                        // Checking if requested event is active or not, then adding to respective queue/pre-queue
                        if (event.isActive()) {
                            allEvents.get(eventID).addToQueue(clientID, clientSocket);

                        } else {
                            allEvents.get(eventID).addToPrequeue(clientID, clientSocket);
                        }

                    }
                } else {

                    // Handle other types of requests
                    String encryptedMessage = SecurityUtil_77_3.encrypt("---Unsupported request.", cipher, secretKey);
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

        // Generating a random OTP
        Random random = new Random();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();

    }

    private static boolean authenticate(String username, String password) {

        //Checking if stored credentials == received
        for (String[] cred : credentials) {
            if (cred[0].equals(username) && cred[1].equals(password)) {
                System.out.println("Stored Credentials "+cred[0]+ " "+ cred[1] +"\nUser Credentials " + username+ " "+ password +"\n AUTHENTICATED!");
                return true;
            }
        }

        return false;
    }

    private static Event_77_3 findEventByID(String eventID) {

        // Searching for the event in activeEvents
        for (Event_77_3 event : activeEvents) {
            if (event.getId().equalsIgnoreCase(eventID)) {
                return event; // Event found
            }
        }

        // Searching for the event in upcomingEvents
        for (Event_77_3 event : upcomingEvents) {
            if (event.getId().equalsIgnoreCase(eventID)) {
                return event; // Event found
            }
        }

        return null; // Event not found
    }

}


