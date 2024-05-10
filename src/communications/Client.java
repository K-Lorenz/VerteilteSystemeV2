package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class Client {
    Properties properties = PropertyLoader.loadProperties();
    UUID clientUUID = UUID.randomUUID();
    public void start(List<String> inputs){
        for(String input:inputs){
            try {
                UUID processUUID = UUID.randomUUID();
                //connect to messageBroker
                Socket socket = new Socket("localhost", Integer.parseInt(properties.getProperty("messagebroker.port")));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //Remove unneccessary "book" from input
                String[] arr = input.split(" ", 2);
                String message = clientUUID + " ClientRq " + processUUID + " " + arr[1];
                out.println(message);

                //wait for Final booking Confirmation. Socket is kept open for this Time.
                    System.out.println(in.readLine());
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
