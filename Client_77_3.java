import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;


public class Client_77_3 {
    private static Scanner scanner = new Scanner(System.in);
    public static void main(String args[]) {

        /* Arguments:
          args[0] = ID
          args[1] = Server IP "localhost"
         */

        int clientID = Integer.parseInt(args[0]);
        String serverIP = args[1];

        int serverPort = 40000;
        Socket s = null;
        DataInputStream in = null;
        DataOutputStream out = null;
        try {
            s = new Socket(serverIP, serverPort);

            // Creating the data streams
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());

            // Generating the secret key & cipher
            SecretKeySpec secretKey = SecurityUtil_77_3.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            // Prompting user for credentials
            System.out.println("Enter your username:");
            String username = scanner.nextLine();

            System.out.println("Enter your password:");
            String password = scanner.nextLine();

            // Sending encrypted username and password to the server
            out.writeUTF(SecurityUtil_77_3.encrypt(username, cipher, secretKey));
            out.writeUTF(SecurityUtil_77_3.encrypt(password, cipher, secretKey));

            // Receiving OTP from the server
            String encryptedOtp = in.readUTF();
            String otp = SecurityUtil_77_3.decrypt(encryptedOtp, cipher, secretKey);

            // if received isn't otp, either username || password are incorrect
            if (otp.equals("authentication_failed")) {
                System.out.println("Authentication failed. Exiting...");
                return;
            }

            System.out.println("Received OTP: " + otp);

            // Prompting user for OTP input
            System.out.println("Enter the OTP received:");
            String userOTP = scanner.nextLine();

            // Sending encrypted OTP input to the server
            out.writeUTF(SecurityUtil_77_3.encrypt(userOTP, cipher, secretKey));

            // Receive authentication response from the server
            String encyptedResponse = in.readUTF();
            String authenticationResponse = SecurityUtil_77_3.decrypt(encyptedResponse, cipher, secretKey);


            if (authenticationResponse.equalsIgnoreCase("authenticated")) {

                System.out.println("Welcome to the Event Management System!");

                // If authenticated, receive event data from the serve
                String upcomingEvents = in.readUTF();
                System.out.println(SecurityUtil_77_3.decrypt(upcomingEvents, cipher, secretKey));

                String ActiveEvents = in.readUTF();
                System.out.println(SecurityUtil_77_3.decrypt(ActiveEvents, cipher, secretKey));

                // Prompt user for action
                System.out.println("\nPlease enter your action (join [event_id]):");
                String action = scanner.nextLine();

                // Adding client's ID to the message
                String fullMessage = action + " " + clientID;

                // Sending requested event & client ID
                String encryptedMessage = SecurityUtil_77_3.encrypt(fullMessage, cipher, secretKey);
                out.writeUTF(encryptedMessage);


                while (true) {
                    // Read input from the server
                    if (in.available() > 0) {

                        // Receiving & Decrypting message from server
                        String encryptedData = in.readUTF();
                        String decryptedData = SecurityUtil_77_3.decrypt(encryptedData, cipher, secretKey);

                        System.out.println(decryptedData);

                        //Checking is server is expecting response (message doesn't start with ---)
                        if (!decryptedData.startsWith("---") && !decryptedData.startsWith("\n---")) {
                            String output = scanner.nextLine();

                            // Encrypting & sending user response
                            encryptedMessage = SecurityUtil_77_3.encrypt(output, cipher, secretKey);
                            out.writeUTF(encryptedMessage);
                        }
                    }
                }
            } else {
                System.out.println("Authentication failed. Exiting...");
            }
        } catch (UnknownHostException e) {
            System.out.println("Error Socket:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
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
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }
}


