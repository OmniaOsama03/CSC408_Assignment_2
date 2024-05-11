import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Scanner;


public class Client{
    public static void main (String args[]) {
        // args[0] = ID
        // args[1] = Server IP

        int clientID = Integer.parseInt(args[0]);

        int serverPort = 40000;
        String serverIP = args[1];

        Socket s = null;
        ObjectInputStream objectIn = null;
        try{
            s = new Socket(serverIP, serverPort);
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            // Generate AES key with appropriate length
            SecretKeySpec secretKey = SecurityUtil.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            objectIn = new ObjectInputStream(s.getInputStream());

            //Decerialize into objects
            ArrayList<Event> upcomingEvents = (ArrayList<Event>) objectIn.readObject();
            ArrayList<Event> activeEvents = (ArrayList<Event>) objectIn.readObject();


            Scanner scanner = new Scanner(System.in);

            // Display welcome message
            System.out.println("Welcome to the Event Management System!");

            // Display upcoming events
            System.out.println("Upcoming Events:");
            if (upcomingEvents.isEmpty()) {
                System.out.println("No upcoming events! Sorry :(");
            } else {
                for (Event event : upcomingEvents) {
                    System.out.println(event.getId() + " - " + event.getName() + " - " + event.getScheduledTime());
                }
            }

            // Display active events
            System.out.println("\nActive Events:");

            if (activeEvents.isEmpty()) {
                System.out.println("No active events! Sorry :(");
            } else {
                for (Event event : activeEvents) {
                    System.out.println(event.getId() + " - " + event.getName() + " - " + event.getScheduledTime());
                }
            }

            // Prompt user for action
            System.out.println("\nPlease enter your action (e.g., join <event_id>):");
            String action = scanner.nextLine();
            String fullMessage = action + " " + clientID;

            String encryptedMessage = SecurityUtil.encrypt(fullMessage, cipher, secretKey);
            out.writeUTF(encryptedMessage);


            while (true) {
                    // Read input from the server
                    if (in.available() > 0) {
                        String encryptedData = in.readUTF();
                        String decryptedData = SecurityUtil.decrypt(encryptedData, cipher, secretKey);
                        System.out.println(decryptedData);

                        if(!decryptedData.startsWith("---") && !decryptedData.startsWith("\n---")){
                            String output = scanner.nextLine();

                            encryptedMessage = SecurityUtil.encrypt(output, cipher, secretKey);
                            out.writeUTF(encryptedMessage);
                        }
                    }
            }

        }catch (UnknownHostException e) {System.out.println("Error Socket:"+e.getMessage());
        }catch (IOException e){
            System.out.println("Exception: " + e.getMessage()); e.printStackTrace();}
        catch (ClassNotFoundException e) {
           throw new RuntimeException(e);}
        catch (NoSuchPaddingException e) {
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
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    System.out.println("Error closing object input stream: " + e.getMessage());
                }
            }
        }
    }
}