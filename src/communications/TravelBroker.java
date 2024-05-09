package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;


// This needs to be a Server aswell ...I think?
// It needs to allow multiple requests to be handled at the same time from the message broker
// afaik it doesnt need to be runnable (no main function). The message broker can run it. It should be (run) multithreaded
public class TravelBroker {
    private Socket socket;
    public TravelBroker(Socket socket) {
        this.socket = socket;
    }



    //is this allowed????
    //I mean it is getting communicated through Sockets. But in the End its just a function call...
    //Maybe we DO need servers..
    public void ClientRq(){
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String inputLine;
            while((inputLine = in.readLine()) != null){
                System.out.println("TravelBroker received from MessageBroker");

            }
        }catch (Exception e ){
            e.printStackTrace();
        }
    }

}
