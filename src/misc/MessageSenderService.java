package misc;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * A class representing a message sender service.
 */
public class MessageSenderService {
    private static final int messageBrokerPort = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.port.start"));
    private static final int messageBrokerAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.amount"));
    private static final HashMap<UUID, Integer> mbInstanceMap = new HashMap<>();
    private static final HashMap<UUID, Integer> tbInstanceMap = new HashMap<>();

    /**
     * Sends a message to the message broker.
     *
     * @param message the message to send.
     */
    public static synchronized void sendMessageToMessageBroker(String message) {
        //Connect to messageBroker
        int port = mbInstanceMap.get(UUID.fromString(message.split(" ", 3)[1]));

        sendMessageToPort(message, port);
    }

    /**
     * Sends a message to the travel broker.
     *
     * @param message   the message to send.
     * @param processId the process id of the message.
     */
    public static synchronized void sendMessageToTravelBroker(String message, UUID processId) {
        sendMessageToPort(message, tbInstanceMap.get(processId));
    }

    /**
     * Sends a message to the travel broker and sets the Correct Message and Travelbroker for this process.
     *
     * @param message   the message to send.
     * @param processId the process id of the message.
     * @param port      the port of the message broker.
     */
    public synchronized static void sendMessageToTravelBroker(String message, UUID processId, int port) {
        mbInstanceMap.put(processId, port);
        tbInstanceMap.put(processId, messageBrokerPort + new Random().nextInt(messageBrokerAmount));
        sendMessageToTravelBroker(message, processId);
    }

    /**
     * Sends a message to the booking system.
     *
     * @param message the message to send.
     */
    public static void sendMessageToBookingSystem(String message) {
        //MessageSplit [0] = WhatAmI, [1] = ProcessId, [2] = Type, [3] = Hotel/FlightNumber, [4] = Quantity
        String[] messageSplit = message.split(" ", 5);
        int portnum;

        //Strip F/H from name. Example => F12 -> 12
        portnum = Integer.parseInt(messageSplit[3].substring(1));

        int port = 0;
        //get Port from Flight/Hotel
        if (messageSplit[2].equals("flight")) {
            //Calculate Port from Flightnumber and startport
            port = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.port.start")) + portnum;
        } else {
            //Calculate Port from Hotelnumber and startport
            port = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.port.start")) + portnum;

        }
        if (messageSplit[0].equalsIgnoreCase("CancellationRq")) {
            //<WhatAmI> <ProcessId> <Quantity>
            String newMessage = "CancellationRq " + messageSplit[1] + " " + messageSplit[4];
            MessageSenderService.sendMessageToPort(newMessage, port);
            return;
        }

        String newMessage = "BookingRq " + messageSplit[1] + " " + messageSplit[4];
        MessageSenderService.sendMessageToPort(newMessage, port);
    }

    /**
     * Sends a message to a port.
     *
     * @param message the message to send.
     * @param port    the port to send the message to.
     */
    private static void sendMessageToPort(String message, int port) {
        try (Socket socket = new Socket("localhost", port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to a client.
     *
     * @param socket  the client socket to send the message to.
     * @param message the message to send.
     */
    public static void sendMessageToClient(Socket socket, String message) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
