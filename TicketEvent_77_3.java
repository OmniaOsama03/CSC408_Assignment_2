import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/*
    Omnia Osama Ahmed  1084505
    Maryam Mohammaed Ali 1079679
    Nourhan Ahmed Elmehalawy 1078096
*/

public class TicketEvent_77_3 extends Event_77_3 {
    private static String[][] tickets = {{"Movie", "Concert", "Museum"}, {"100", "200", "150"}};
    private static String[] ticketTypes = {"Normal", "Golden"};
    private static String[] questions = {"What category of tickets would you like?", "How many tickets are you purchasing? ", "What ticket type would you like?"};
    private static Map<Integer, String[][]> clientInfo = new HashMap<>();

    public TicketEvent_77_3(String id, String name, Date scheduledTime) {
        super(id, name, scheduledTime);
    }

    @Override
    public void InitiateEvent(int clientID, DataInputStream in, DataOutputStream out) {
        try {
            //Setting session time to 5 mins ahead of now
            if (!clientSessionTimes.containsKey(clientID)) {

                clientSessionTimes.put(clientID, System.currentTimeMillis() + 300000);
            }

            // Generating secret key & cipher
            SecretKeySpec secretKey = SecurityUtil_77_3.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            String encryptedOut = SecurityUtil_77_3.encrypt("\n---___ ___ ___ ___ ___ TICKET BOOKING EVENT ___ ___ ___ ___ ___ ___ ___ ___ ---", cipher, secretKey);
            out.writeUTF(encryptedOut);

            String[][] clientData;
            if (!clientInfo.containsKey(clientID)) {

                // Creating a new array for the client
                clientData = new String[2][questions.length];
                clientInfo.put(clientID, clientData);
            }

            // Retrieving saved client responses
            clientData = clientInfo.get(clientID);

            // Looping through questions
            for (int i = 0; i < questions.length; i++) {

                if(i != 1) // No reply from client is required
                   out.writeUTF(SecurityUtil_77_3.encrypt("---" + questions[i], cipher, secretKey));
                else
                    out.writeUTF(SecurityUtil_77_3.encrypt(questions[i], cipher, secretKey));

                // Checking if question is saved
                if (!"saved".equals(clientData[0][i])) {

                    // Print ticket options
                    if(i == 0)
                        printAvailableTickets(out, cipher, secretKey);
                    else if(i == 2)
                        printTicketTypes(out, cipher, secretKey);

                    String answer = null;
                    String decryptedAnswer = null;

                    // Detecting an incoming message
                    boolean received = false;
                    while(!received) {
                        if (in.available() > 0) {

                            answer = in.readUTF();
                            decryptedAnswer = SecurityUtil_77_3.decrypt(answer, cipher, secretKey);
                            received = true;
                        }
                    }

                    // Saving the answer in the client's 2d array
                    clientData[0][i] = "saved";
                    clientData[1][i] = decryptedAnswer;

                    //Updating hashmap after response
                    clientInfo.put(clientID, clientData);

                    // Checking if session's time is up
                    if(SessionTimedOut(clientID, out, cipher, secretKey))
                    {
                        return;
                    }

                } else {

                    // Send the corresponding cell value if already answered
                    out.writeUTF(SecurityUtil_77_3.encrypt("---Saved response: " + clientData[1][i], cipher, secretKey));
                }
            }

            out.writeUTF(SecurityUtil_77_3.encrypt("EVENT HAS ENDED! GOODBYE! ", cipher, secretKey));

        } catch (NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalBlockSizeException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        } catch (InvalidKeySpecException ex) {
            throw new RuntimeException(ex);
        } catch (BadPaddingException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
           System.out.println("CLIENT " + clientID + " HAS DISCONNECTED WHILE IN EVENT");
           ex.getStackTrace();
        } catch (InvalidKeyException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void printAvailableTickets(DataOutputStream out, Cipher cipher, SecretKeySpec secretKey) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String availableTickets = new String("Available tickets:\n");

        for (int i = 0; i < tickets[0].length; i++) {
            availableTickets += "Category: " + tickets[0][i] + ", Available: Tickets" + tickets[1][i] + "\n";
        }

        String encryptedMsg = SecurityUtil_77_3.encrypt(availableTickets, cipher, secretKey);
        out.writeUTF(encryptedMsg);
    }

    private void printTicketTypes(DataOutputStream out, Cipher cipher, SecretKeySpec secretKey) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String ticketTypeMsg = new String("Ticket types available: ");
        for (String type : ticketTypes) {
            ticketTypeMsg += type + "   ";
        }

        String encryptedMsg = SecurityUtil_77_3.encrypt(ticketTypeMsg.toString(), cipher, secretKey);
        out.writeUTF(encryptedMsg);
    }
}



