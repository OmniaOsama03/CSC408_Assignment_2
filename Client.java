import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class Client{
    public static void main (String args[]) {
        // args[0] = ID
        // args[1] = Server IP

        int clientID = Integer.parseInt(args[0]);
        boolean isClientConnected;

        int serverPort = 20000;
        String serverIP = args[1];

        Socket s = null;
        ObjectInputStream objectIn = null;
        try{
            s = new Socket(serverIP, serverPort);
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            objectIn = new ObjectInputStream(s.getInputStream());

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


            // Send action to the server
            out.writeUTF(action + " " + clientID);

            while(true)
            {
                if(in.available() > 1) {
                    String input = in.readUTF();
                    System.out.println(input);
                }
                else {
                    String output = scanner.nextLine();
                    out.writeUTF(output);
                }

            }

        }catch (UnknownHostException e) {System.out.println("Error Socket:"+e.getMessage());
        }catch (IOException e){
            System.out.println("Exception: " + e.getMessage()); e.printStackTrace();
        } //catch (InterruptedException e) {e.printStackTrace();}
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);}
        finally {
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
