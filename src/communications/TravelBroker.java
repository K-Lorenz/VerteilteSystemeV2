package communications;

import misc.BookingRequest;
import misc.BookingRequestParser;
import misc.MessageSenderService;
import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;


// This needs to be a Server aswell ...I think?
// It needs to allow multiple requests to be handled at the same time from the message broker
// afaik it doesnt need to be runnable (no main function). The message broker can run it. It should be (run) multithreaded
public class TravelBroker {
    private int port;
    static List<String> waitForOtherBooking = new ArrayList<>();
    public TravelBroker(){
        Properties properties = PropertyLoader.loadProperties();
        port = Integer.parseInt(properties.getProperty("travelbroker.port"));
    }
    public void start(){
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TravelBroker running on port " + port);
            while (true) {
                Socket travelSocket = serverSocket.accept();
                Thread travelThread = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(travelSocket.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            //Handle message and answer
                            //Switch case for different messages
                            handleRequest(inputLine, travelSocket);
                            System.out.println("TravelBroker - Received message: " + inputLine);
                        }
                        in.close();
                        travelSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                travelThread.start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void handleRequest(String message, Socket socket){
        String[] arr = message.split(" ", 3);
        UUID ProcessId = UUID.fromString(arr[1]);
        String whatAmI = arr[0];
        switch (whatAmI) {
            case "ClientRq":
                List<BookingRequest> bookingRequests = BookingRequestParser.parse(arr[2]);
                for (BookingRequest bookingRequest : bookingRequests) {
                    //Send Flight BookingRq to Messagebroker
                    String newMessage = "BookingRq " + ProcessId + " " + bookingRequest.type() + " " + bookingRequest.name() + " " + bookingRequest.quantity();
                    MessageSenderService.sendMessageToMessageBroker(newMessage);
                }
                break;
            case "Confirmation":
                //<WhatAmI> <ProcessId> < --flight '12312' true --hotel 'Hotel' false>
                break;

            case "CancellationConfirmation":
                break;

            default:

                break;
        }
    }
}

