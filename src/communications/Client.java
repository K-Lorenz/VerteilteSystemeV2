package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.UUID;

public class Client {
    Properties properties = PropertyLoader.loadProperties();
    public final UUID processID = UUID.randomUUID();
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";

    public boolean start(String input) {
        String response = "";
        try {
            //connect to messageBroker
            Socket socket = new Socket("localhost", Integer.parseInt(properties.getProperty("messagebroker.port")));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //Remove unneccessary "book" from input
            String[] arr = input.split(" ", 2);
            String message = "ClientRq " + processID + " " + arr[1];
            out.println(message);

            //wait for Final booking Confirmation. Socket is kept open for this Time.
            //System.out.println(in.readLine());
            response = in.readLine();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(response.equals("Yay! Your booking " + processID + " is confirmed!")){
            System.out.println(ANSI_GREEN +ANSI_BOLD+response+ANSI_RESET);
        }else {
            System.out.println(ANSI_RED+ANSI_BOLD +response+ANSI_RESET);
        }
        return response.equals("Yay! Your booking " + processID + " is confirmed!");
    }
}
