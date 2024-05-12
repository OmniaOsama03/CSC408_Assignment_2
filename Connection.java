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

    public Connection (int clientID, Socket clientSocket, Event event) {
        try {
            this.clientSocket = clientSocket;
            this.event = event;
            this.clientID = clientID;

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            this.start();

       }catch(IOException e) {System.out.println("Error Connection:"+ e.getMessage());}
    }

    public void run() {

            event.InitiateEvent(clientID, in, out);

    }
}





