import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/*
    Omnia Osama Ahmed  1084505
    Maryam Mohammaed Ali 1079679
    Nourhan Ahmed Elmehalawy 1078096
*/

class Connection_77_3 extends Thread {
    Event_77_3 event;
    Socket clientSocket;
    int clientID;
    DataOutputStream out;
    DataInputStream in;

    public Connection_77_3(int clientID, Socket clientSocket, Event_77_3 event) {
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

        //Initiating the client's event
        event.InitiateEvent(clientID, in, out);
    }
}





