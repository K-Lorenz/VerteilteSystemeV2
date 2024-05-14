package misc;

import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MessageSenderService {
    public static void sendMessageToMessageBroker(String message) {
        //connect to messageBroker
        sendMessageToPort(message, Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.port")));
    }
    public static void sendMessageToTravelBroker(String message) {
        sendMessageToPort(message, Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.port")));
    }
    public static void sendMessageToBookingSystem(String message) {
        //MessageSplit [0] = WhatAmI, [1] = ProcessId, [2] = Type, [3] = Hotel/FlightNumber, [4] = Quantity
        String [] messageSplit = message.split(" ", 5);
        int portnum;
        /*Probabilty to crash server
        if(prob<0.05){
            //crash server
        }
        */
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

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = executor.submit(new SendMessageOrTimeout(newMessage, port));
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                sendError(message);
            } catch (Exception e){
                e.printStackTrace();
            }
            executor.shutdownNow();
            return;
        }

        String newMessage = "BookingRq " + messageSplit[1] + " " + messageSplit[4];
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new SendMessageOrTimeout(newMessage, port));
        try {
            future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            sendError(message);
        } catch (Exception e){
            e.printStackTrace();
        }
        executor.shutdownNow();
    }

    public static boolean sendMessageToPort(String message, int port) {
        try (Socket socket = new Socket("localhost", port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (ConnectException e){
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    public static void sendMessageWithSocket(String message, Socket socket) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void sendError(String errorMessage){
        // <WhatAmI> <ProcessId> <Message>
        String[] messageSplit = errorMessage.split(" ", 3);
        sendMessageToTravelBroker("Response " + messageSplit[1] + " false " + messageSplit[2]);
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

class SendMessageOrTimeout implements Callable<String> {
    private String message;
    private int port;

    public SendMessageOrTimeout(String message, int port){
        this.message = message;
        this.port = port;
    }
    @Override
    public String call() throws Exception {
        try{
            while(!MessageSenderService.sendMessageToPort(this.message, this.port)){
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "message sent successfully!";
    }
}
