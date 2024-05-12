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


public class Client {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String args[]) {
        // args[0] = ID
        // args[1] = Server IP "localhost"

        int clientID = Integer.parseInt(args[0]);

        int serverPort = 40000;
        String serverIP = args[1];

        Socket s = null;
        DataInputStream in = null;
        try {
            s = new Socket(serverIP, serverPort);
            in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            // Generate AES key with appropriate length
            SecretKeySpec secretKey = SecurityUtil.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            // Prompt user for credentials
            System.out.println("Enter your username:");
            String username = scanner.nextLine();
            System.out.println("Enter your password:");
            String password = scanner.nextLine();

            // Send username and password to the server
            out.writeUTF(SecurityUtil.encrypt(username, cipher, secretKey));
            out.writeUTF(SecurityUtil.encrypt(password, cipher, secretKey));

            // Receive OTP from the server
            String encryptedOtp = in.readUTF();
            String otp = SecurityUtil.decrypt(encryptedOtp, cipher, secretKey);
            if (otp.equals("authentication_failed")) {
                System.out.println("Authentication failed. Exiting...");
                return;
            }
            System.out.println("Received OTP: " + otp);

            // Prompt user for OTP input
            System.out.println("Enter the OTP received:");
            String userOTP = scanner.nextLine();

            // Send OTP input to the server
            out.writeUTF(SecurityUtil.encrypt(userOTP, cipher, secretKey));

            // Receive authentication response from the server
            String encyptedResponse = in.readUTF();
            String authenticationResponse = SecurityUtil.decrypt(encyptedResponse, cipher, secretKey);
            if (authenticationResponse.equalsIgnoreCase("authenticated")) {
                // If authenticated, receive event data from the serve

                // Display welcome message
                System.out.println("Welcome to the Event Management System!");

                String upcomingEvents = in.readUTF();
                System.out.println(SecurityUtil.decrypt(upcomingEvents, cipher, secretKey));

                String ActiveEvents = in.readUTF();
                System.out.println(SecurityUtil.decrypt(ActiveEvents, cipher, secretKey));

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

                        if (!decryptedData.startsWith("---") && !decryptedData.startsWith("\n---")) {
                            String output = scanner.nextLine();

                            encryptedMessage = SecurityUtil.encrypt(output, cipher, secretKey);
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
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.out.println("Error closing object input stream: " + e.getMessage());
                }
            }
        }
    }
}


