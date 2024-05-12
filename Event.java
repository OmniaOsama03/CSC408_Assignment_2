import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

public class Event
{
    private String id;
    private String name;
    private boolean active; //Set the value in the constructor.
    private long scheduledTimeMillis; // Scheduled time in milliseconds since the epoch
    static private Map<Integer, Socket> clientSockets; // Map to store client sockets
    EventHandler eventHandler;
    private long sessionTime;

    public Event(String id, String name, Date scheduledTime) {
        this.id = id;
        this.name = name;
        this.scheduledTimeMillis = scheduledTime.getTime(); // Convert Date to milliseconds since the epoch

        // Set the initial state of the event based on the scheduled time
        if (System.currentTimeMillis() >= scheduledTimeMillis) {
            active = true;
            Server.activeEvents.add(this); // Add event to the list of active events

        } else {
            active = false;
            Server.upcomingEvents.add(this); // Add event to the list of upcoming events
        }

    }


    public void setActive(Boolean active)
    {
        this.active = active;
    }
    public long getSessionTime() {
        return sessionTime;
    }
    public void setSessionTime(long sessionTime) {
        this.sessionTime = sessionTime;
    }
    public  void setClientSockets(Map<Integer, Socket> clientSockets) {
        Event.clientSockets = clientSockets;
    }

    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
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
        // Convert milliseconds to Date
        Date scheduledDate = new Date(scheduledTimeMillis);

        // Extract individual components of the date
        int year = scheduledDate.getYear() + 1900; // Adding 1900 because getYear() returns the year minus 1900
        int month = scheduledDate.getMonth() + 1; // Adding 1 because months are 0-based
        int day = scheduledDate.getDate();
        int hours = scheduledDate.getHours();
        int minutes = scheduledDate.getMinutes();
        int seconds = scheduledDate.getSeconds();

        // Construct a string representing the scheduled time
        return year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
    }

    public void InitiateEvent(int clientID, DataInputStream in, DataOutputStream out)
    {
        try {
            // Generate AES key with appropriate length
            SecretKeySpec secretKey = SecurityUtil.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            String encryptedOut = SecurityUtil.encrypt("\n---___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ___ ---", cipher, secretKey);
            out.writeUTF(encryptedOut);

            encryptedOut = SecurityUtil.encrypt("Welcome to your session in event: " + this.name +  "\n" + "Hello client, are you able to reply?", cipher, secretKey);
            out.writeUTF(encryptedOut);

            String encryptedIn = in.readUTF();
            System.out.println("Received from client " + clientID + ": " + new String(SecurityUtil.decrypt(encryptedIn, cipher, secretKey)));
            checkSessionValidity(out, cipher, secretKey);

            encryptedOut = SecurityUtil.encrypt("Oki doki! Goodbye client!", cipher, secretKey);
            out.writeUTF(encryptedOut);

        }catch(Exception e)
        {
            System.out.println("Exception: " + e.getMessage()); e.printStackTrace();
        }
    }

     void checkSessionValidity(DataOutputStream out, Cipher cipher, SecretKeySpec secretKey)
    {
        if(System.currentTimeMillis() >= sessionTime)
        {
            try {
                String message = "---Sorry! Your session has timed out! Goodbye!";
                out.writeUTF(SecurityUtil.encrypt(message, cipher, secretKey));

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
        }
    }

}
