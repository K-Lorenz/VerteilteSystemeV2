package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.GregorianCalendar;
import java.util.Properties;


// This needs to be a Server aswell ...I think?
// It needs to allow multiple requests to be handled at the same time from the message broker
// afaik it doesnt need to be runnable (no main function). The message broker can run it. It should be (run) multithreaded
public class TravelBroker {
    private int port;
    public TravelBroker(){
        Properties properties = PropertyLoader.loadProperties();
        port = Integer.parseInt(properties.getProperty("travelbroker.port"));
    }
    public void start(){
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TravelBroker running on port " + port);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
