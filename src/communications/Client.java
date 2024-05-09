package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    public static void main(String[] args) {
        Properties properties = PropertyLoader.loadProperties();
        System.out.println("Client started!");
        System.out.println("Please input the booking that you want to send to the server!");
        System.out.println("The only Accepted Format is: book --flight '<flightNumber>' <ticketCount> --hotel '<hotelName>' <roomCount>");
        System.out.println("Example: book --flight 'AA12323' 2 --hotel 'Hilton' 2");
        System.out.println("Please note that you can have multiple flights and/or hotels in one booking!");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean valid = false;
        String input = "";
        while (true) {

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
            //connect to messageBroker
            Socket socket = new Socket("localhost", Integer.parseInt(properties.getProperty("messagebroker.port")));
            OutputStream out = socket.getOutputStream();
            //Remove unneccessary "book" from input
            String[] arr = input.split(" ",2);
            String message = "client " + arr[1];

            //send message
            out.write(message.getBytes());
            out.flush();
            socket.close();

            //reset for next iteration
            input = "";
            valid = false;
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
