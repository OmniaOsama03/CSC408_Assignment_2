import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TicketEvent extends Event{
    private static String[][] tickets = {
            {"Movie", "Concert", "Museum"}, // Categories
            {"100", "200", "150"} // Available counts
    };
    private static String[] ticketTypes = {"Normal", "Golden"}; // 1D array of ticket types
    private static String[] questions =
            {"What category of tickets would you like?",
             "How many tickets are you purchasing? ",
             "What ticket type would you like?"}; // 1D array of questions
    private static Map<Integer, String[][]> clientInfo = new HashMap<>(); // Hashmap to store client information

    public TicketEvent(String id, String name, Date scheduledTime) {
        super(id, name, scheduledTime);
    }

    @Override
    public void InitiateEvent(int clientID, DataInputStream in, DataOutputStream out) {
        try {
            //Setting session time to 5 mins ahead of now
            if (!clientSessionTimes.containsKey(clientID)) {

                clientSessionTimes.put(clientID, System.currentTimeMillis() + 300000);
            }

            // Generate AES key with appropriate length
            SecretKeySpec secretKey = SecurityUtil.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            System.out.println("CLIENT " + clientID + "(EVENT STARTED): " + System.nanoTime()/1e6);
            String encryptedOut = SecurityUtil.encrypt("\n---___ ___ ___ ___ ___ TICKET BOOKING EVENT ___ ___ ___ ___ ___ ___ ___ ___ ---", cipher, secretKey);
            out.writeUTF(encryptedOut);

            String[][] clientData;
            if (!clientInfo.containsKey(clientID)) {
                // Create a new array for the client
                clientData = new String[2][questions.length];
                clientInfo.put(clientID, clientData);
            }

            clientData = clientInfo.get(clientID);

            // Loop through questions
            for (int i = 0; i < questions.length; i++) {

                if(i != 1)
                   out.writeUTF(SecurityUtil.encrypt("---" + questions[i], cipher, secretKey));
                else
                    out.writeUTF(SecurityUtil.encrypt(questions[i], cipher, secretKey));

                //Check if question is saved
                if (!"saved".equals(clientData[0][i])) {

                    if(i == 0)
                        printAvailableTickets(out, cipher, secretKey);
                    else if(i == 2)
                        printTicketTypes(out, cipher, secretKey);

                    String answer = null;
                    String decryptedAnswer = null;


                    boolean received = false;
                    while(!received) {

                        if (in.available() > 0) {

                            answer = in.readUTF();
                            decryptedAnswer = SecurityUtil.decrypt(answer, cipher, secretKey);
                            received = true;
                        }
                    }

                    // Save the answer
                    clientData[0][i] = "saved";
                    clientData[1][i] = decryptedAnswer;


                    clientInfo.put(clientID, clientData); //Update hashmap after response

                    if(SessionTimedOut(clientID, out, cipher, secretKey))
                    {
                        return;
                    }

                } else {
                    // Send the corresponding cell value if already answered
                    out.writeUTF(SecurityUtil.encrypt("---Saved response: " + clientData[1][i], cipher, secretKey));
                }
            }

            out.writeUTF(SecurityUtil.encrypt("EVENT HAS ENDED! GOODBYE! ", cipher, secretKey));

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

        String encryptedMsg = SecurityUtil.encrypt(availableTickets, cipher, secretKey);
        out.writeUTF(encryptedMsg);
    }

    private void printTicketTypes(DataOutputStream out, Cipher cipher, SecretKeySpec secretKey) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String ticketTypeMsg = new String("Ticket types available: ");
        for (String type : ticketTypes) {
            ticketTypeMsg += type + "   ";
        }

        String encryptedMsg = SecurityUtil.encrypt(ticketTypeMsg.toString(), cipher, secretKey);
        out.writeUTF(encryptedMsg);
    }
}



