import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class Connection extends Thread {
    Event event;
    Socket clientSocket;
    int clientID;
    DataOutputStream out;
    DataInputStream in;

    public Connection (int clientID, Socket clientSocket, Event event, DataOutputStream out, DataInputStream in) {
        //try {
        this.clientSocket = clientSocket;
        this.event = event;
        this.clientID = clientID;
        this.out = out;
        this.in = in;

        //in = new DataInputStream(clientSocket.getInputStream());
        //out = new DataOutputStream(clientSocket.getOutputStream());

        this.start();
        // }catch(IOException e) {System.out.println("Error Connection:"+ e.getMessage());}
    }

    public void run() {
        try {
            // Set up input and output streams for communication with the client

            // Perform event interaction logic
            event.InitiateEvent(clientID, in, out);

        } finally {
            // Close the client socket when the interaction is done
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
