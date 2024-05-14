package communications;

import misc.PropertyLoader;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cli {

    // ANSI escape codes for colors
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";

    public static void main(String[] args) {

        Properties properties = PropertyLoader.loadProperties();
        while (true) {
            System.out.println(ANSI_CYAN + "------------------------------------------------------------------------------------------------------------" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "Please input the booking that you want to send to the server!" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "The only accepted format is: " + ANSI_BOLD + "book --flight '<flightNumber>' <ticketCount> --hotel '<hotelName>' <roomCount>" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "Example: " + ANSI_BOLD + "book --flight 'f1' 2 --hotel 'h2' 2" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "Please note that you can have multiple flights and/or hotels in one booking!" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "------------------------------------------------------------------------------------------------------------" + ANSI_RESET);
            System.out.print(ANSI_BLUE + "> " + ANSI_RESET);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            boolean valid = false;
            String input = "";
            while (!valid) {
                try {
                    // Read input line
                    input = reader.readLine();
                    valid = checkValidity(input);
                    if (!valid) {
                        System.out.println(ANSI_RED + "Invalid input! Please try again!" + ANSI_RESET);
                        System.out.print(ANSI_BLUE + "> " + ANSI_RESET);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println(ANSI_GREEN + "Input is valid! Sending to server!" + ANSI_RESET);
            try {
                UUID processID = UUID.randomUUID();
                // Connect to messageBroker
                Socket socket = new Socket("localhost", Integer.parseInt(properties.getProperty("messagebroker.port")));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Remove unnecessary "book" from input
                String[] arr = input.split(" ", 2);
                String message = "ClientRq " + processID + " " + arr[1];
                out.println(message);
                // Loop for receiving multiple messages
                // Count = Steps of the process
                for (int i = 0; i < 1; i++) {
                    System.out.println(in.readLine());
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean checkValidity(String input) {
        // Check if input is only "book"
        if (Objects.equals(input, "book")) return false;

        // Check if input is in the correct format aka "book --flight '<flightNumber>' <ticketCount> --hotel '<hotelName>' <roomCount>"
        String regex = "^book(?:\\s*--(?:flight\\s+'.*?'\\s+\\d+|hotel\\s+'.*?'\\s+\\d+))*$";
        Matcher matcher = Pattern.compile(regex).matcher(input);

        return matcher.matches();
    }
}
