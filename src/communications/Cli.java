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
    public static void main(String[] args) {

        Properties properties = PropertyLoader.loadProperties();
        UUID clientUUID = UUID.randomUUID();
        while (true) {
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println("Client started! Your UUID is: " + clientUUID);
            System.out.println("Please input the booking that you want to send to the server!");
            System.out.println("The only Accepted Format is: book --flight '<flightNumber>' <ticketCount> --hotel '<hotelName>' <roomCount>");
            System.out.println("Example: book --flight 'AA12323' 2 --hotel 'Hilton' 2");
            System.out.println("Please note that you can have multiple flights and/or hotels in one booking!");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            boolean valid = false;
            String input = "";
            while (valid == false) {
                try {
                    //read input line
                    input = reader.readLine();
                    valid = checkValidity(input);
                    if (valid == false) {
                        System.out.println("Invalid input! Please try again!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Input is valid! Sending to server!");
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
                //loop for receiving multiple messages
                //Count = Steps of the process
                for (int i = 0; i < 6; i++) {
                    System.out.println(in.readLine());
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static boolean checkValidity(String input){
        //check if input is only "book"
        if(Objects.equals(input, "book")) return false;

        //check if input is in the correct format aka "book --flight '<flightNumber>' <ticketCount> --hotel '<hotelName>' <roomCount>"
        String regex = "^book(?:\\s*--(?:flight\\s+'.*?'\\s+\\d+|hotel\\s+'.*?'\\s+\\d+))*$";
        Matcher matcher = Pattern.compile(regex).matcher(input);

        return matcher.matches();
    }
}
