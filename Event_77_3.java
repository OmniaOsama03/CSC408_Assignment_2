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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
/*
    Omnia Osama Ahmed  1084505
    Maryam Mohammaed Ali 1079679
    Nourhan Ahmed Elmehalawy 1078096
*/

public class Event_77_3
{
    private String id;
    private String name;
    private boolean active;
    private long scheduledTimeMillis; // Scheduled time in milliseconds since the epoch
    static private Map<Integer, Socket> clientSockets; // Map to store client sockets
    static HashMap<Integer, Long> clientSessionTimes = new HashMap<>();


    public Event_77_3(String id, String name, Date scheduledTime) {
        this.id = id;
        this.name = name;
        this.scheduledTimeMillis = scheduledTime.getTime(); // Convert Date to milliseconds since the epoch

        // Set the initial state of the event based on the scheduled time
        if (System.currentTimeMillis() >= scheduledTimeMillis) {

            // Event is active
            active = true;
            Server_77_3.activeEvents.add(this);

        } else {

            // Event is upcoming
            active = false;
            Server_77_3.upcomingEvents.add(this);
        }

    }


    // Necessary setters and getters
    public void setActive(Boolean active)
    {
        this.active = active;
    }
    public String getId() {
        return id;
    }
    public long getScheduledTimeMillis()
    {
        return this.scheduledTimeMillis;
    }
    public String getName()
    {
        return this.name;
    }
    public boolean isActive()
    {
        return this.active;
    }
    public String getScheduledTime() {
        // Converting milliseconds to Date
        Date scheduledDate = new Date(scheduledTimeMillis);

        // Extracting individual components of the date
        int year = scheduledDate.getYear() + 1900; // Adding 1900 because getYear() returns the year minus 1900
        int month = scheduledDate.getMonth() + 1; // Adding 1 because months are 0-based
        int day = scheduledDate.getDate();
        int hours = scheduledDate.getHours();
        int minutes = scheduledDate.getMinutes();
        int seconds = scheduledDate.getSeconds();


        return year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
    }


    public void InitiateEvent(int clientID, DataInputStream in, DataOutputStream out)
    {
        try {
            //Setting session time to 5 mins ahead of now if they're new
            if (!clientSessionTimes.containsKey(clientID)) {
                clientSessionTimes.put(clientID, System.currentTimeMillis() + 300000);
            }

            // Generating cipher & secret key
            SecretKeySpec secretKey = SecurityUtil_77_3.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            String encryptedOut = SecurityUtil_77_3.encrypt("\n---___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ---", cipher, secretKey);
            out.writeUTF(encryptedOut);

            encryptedOut = SecurityUtil_77_3.encrypt("Welcome to your session in event: " + this.name +  "\n" + "Hello client, are you able to reply?", cipher, secretKey);
            out.writeUTF(encryptedOut);

            String encryptedIn = in.readUTF();
            System.out.println("Received from client " + clientID + ": " + new String(SecurityUtil_77_3.decrypt(encryptedIn, cipher, secretKey)));

            // Checking if event timed out
            boolean endSession = SessionTimedOut(clientID, out, cipher, secretKey);
            if(endSession)
            {
                return;
            }

            encryptedOut = SecurityUtil_77_3.encrypt("Oki doki! Goodbye client!", cipher, secretKey);
            out.writeUTF(encryptedOut);

        }catch(Exception e)
        {
            System.out.println("Exception: " + e.getMessage()); e.printStackTrace();
        }
    }

     boolean SessionTimedOut(int clientID, DataOutputStream out, Cipher cipher, SecretKeySpec secretKey)
    {
        // Checking if current time >= session time
        if(System.currentTimeMillis() >= clientSessionTimes.get(clientID))
        {
            try {
                String message = "---Sorry! Your session has timed out! Goodbye!";
                out.writeUTF(SecurityUtil_77_3.encrypt(message, cipher, secretKey));

                return true;

            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }else
            return false;
    }

}
