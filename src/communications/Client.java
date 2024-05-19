package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.UUID;

/**
 * A class representing a client.
 */
public class Client {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public final UUID processID = UUID.randomUUID();
    Properties properties = PropertyLoader.loadProperties();

    /**
     * Starts the client with the specified input.
     *
     * @param input the input to send to the server.
     * @return {@code true} if the booking was successful, {@code false} otherwise.
     */
    public boolean start(String input) {
        String response = "";
        try {
            //connect to messageBroker

            Socket socket = new Socket("localhost", Integer.parseInt(properties.getProperty("messagebroker.port.start")) + (int) (Math.random() * Integer.parseInt(properties.getProperty("messagebroker.amount"))));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String message = "ClientRq " + processID + " " + input;
            out.println(message);

            //wait for Final booking Confirmation. Socket is kept open for this time.
            response = in.readLine();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (response.equals("Yay! Your booking " + processID + " is confirmed!")) {
            System.out.println(ANSI_GREEN + ANSI_BOLD + response + ANSI_RESET);
        } else {
            System.out.println(ANSI_RED + ANSI_BOLD + response + ANSI_RESET);
        }
        return response.equals("Yay! Your booking " + processID + " is confirmed!");
    }
}
