package misc;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.UUID;

public class MessageSenderService {
    private static final int mbAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.amount"));
    private static HashMap<UUID, Integer> mbInstanceMap = new HashMap<>();
    public static void sendMessageToMessageBroker(String message) {
        //connect to messageBroker
        int port = mbInstanceMap.get(UUID.fromString(message.split(" ", 3)[1]));

        sendMessageToPort(message, port);
    }
    public static void sendMessageToTravelBroker(String message) {
        sendMessageToPort(message, Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.port")));
    }
    public static void sendMessageToTravelBroker(String message, UUID processId, int port){
        mbInstanceMap.put(processId, port);
        sendMessageToTravelBroker(message);
    }
    public static void sendMessageToBookingSystem(String message) {
        //MessageSplit [0] = WhatAmI, [1] = ProcessId, [2] = Type, [3] = Hotel/FlightNumber, [4] = Quantity
        String [] messageSplit = message.split(" ", 5);
        int portnum;

        //Strip F/H from name. Example => F12 -> 12
        portnum = Integer.parseInt(messageSplit[3].substring(1));

        int port = 0;
        //get Port from Flight/Hotel
        if(messageSplit[2].equals("flight")) {
            //Calculate Port from Flightnumber and startport
            port = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.port.start")) + portnum;
        }
        else{
            //Calculate Port from Hotelnumber and startport
            port = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.port.start")) + portnum;

        }
        if(messageSplit[0].equalsIgnoreCase("CancellationRq")){
            //<WhatAmI> <ProcessId> <Quantity>
            String newMessage = "CancellationRq " + messageSplit[1] + " " + messageSplit[4];
            MessageSenderService.sendMessageToPort(newMessage,port);
            return;
        }

        String newMessage = "BookingRq " + messageSplit[1] + " " + messageSplit[4];
        MessageSenderService.sendMessageToPort(newMessage,port);
    }

    private static void sendMessageToPort(String message, int port) {
        try (Socket socket = new Socket("localhost", port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendMessageToClient(Socket socket, String message){
        try{
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
