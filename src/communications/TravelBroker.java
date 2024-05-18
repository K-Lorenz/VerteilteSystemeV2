package communications;

import booking.Booking;
import misc.BookingRequest;
import misc.BookingRequestParser;
import misc.MessageSenderService;
import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class TravelBroker {
    private int port;
    private int retryDelay = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.retryDelay"));
    private List<Booking> bookings = new ArrayList<>();
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";

    public TravelBroker() {
        Properties properties = PropertyLoader.loadProperties();
        port = Integer.parseInt(properties.getProperty("travelbroker.port"));
    }

    public void start(int backlog) {
        try (ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            System.out.println(ANSI_BLUE + "TravelBroker running on port " + port + ANSI_RESET);
            while (true) {
                Socket travelSocket = serverSocket.accept();
                Thread travelThread = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(travelSocket.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            handleRequest(inputLine);
                        }
                        in.close();
                        travelSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                travelThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRequest(String message) {
        //MessageSplit [0] = WhatAmI, [1] = processId, [2] = Message
        String[] messageSplit = message.split(" ", 3);

        UUID processId = UUID.fromString(messageSplit[1]);
        String whatAmI = messageSplit[0];
        switch (whatAmI) {
            case "ClientRq":
                System.out.println(ANSI_CYAN + "TravelBroker - Received ClientRequest: '" + message +"'" + ANSI_RESET);
                Booking booking = new Booking(processId, BookingRequestParser.parse(messageSplit[2]));
                synchronized (bookings){
                    bookings.add(booking);
                    startBooking(bookings.get(bookings.indexOf(booking)));
                }
                //HandleClientRq
                break;

            case "Response":
                //<WhatAmI> <processId> <confirmation (true/false)> <type> <Flight/Hotel number> <amount>
                System.out.println(ANSI_CYAN + "TravelBroker - Received Response: '" + message +"'" + ANSI_RESET);
                handleResponse(message, processId);
                break;

            case "CancellationConfirmation":
                System.out.println(ANSI_CYAN + "TravelBroker - Received CancellationConfirmation: '" + message +"'"+ ANSI_RESET);
                handleCancellationConfirmation(message, processId);
                break;

            default:
                System.out.println(ANSI_RED + "TravelBroker - Message not recognized: " + message+ ANSI_RESET);
                break;
        }
    }
    public void startBooking(Booking booking){
        Thread bookingThread = new Thread(()->{
            while (!booking.uncompletedRequests().isEmpty()) {
                for (BookingRequest request : booking.uncompletedRequests()) {
                    //MessageSplit [0] = WhatAmI, [1] = ProcessId, [2] = Type, [3] = Hotel/FlightNumber, [4] = Quantity
                    if (booking.isCancelling()) {
                        if (!request.isCompleted()) {
                            request.sendCancellation();
                            sendCancellationRequest(booking.processID(), request.getType(), request.getName() + " " + request.getQuantity());
                        }
                    } else {
                        request.sendMessage();
                        System.out.println(ANSI_CYAN + "TravelBroker - Sending Booking Request to Message Broker: " + "BookingRq " + booking.processID() + " " + request.getType() + " " + request.getName() + " " + request.getQuantity() + ANSI_RESET);
                        MessageSenderService.sendMessageToMessageBroker("BookingRq " + booking.processID() + " " + request.getType() + " " + request.getName() + " " + request.getQuantity());
                    }
                }


                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
                sendResponse(booking.processID(), booking.isSuccessful());
        });
        bookingThread.start();
    }
    public synchronized void handleResponse(String message, UUID processId) {
        //<WhatAmI> <processId> <confirmation (true/false)> <type> <Flight/Hotel number> <amount>
        //example confirmMessageSplit [0] = Response, [1] = UUID, [2] = true, [3] = flight, [4] = F20 5
        String[] confirmMessageSplit = message.split(" ", 5);
        boolean confirmationType = Boolean.parseBoolean(confirmMessageSplit[2]);
        String responseType = confirmMessageSplit[3];
        String details = confirmMessageSplit[4];

        if (confirmationType) {
            handleConfirmation(processId, responseType, details);
        } else {
            handleCancellation(processId, responseType, details);
        }
    }
    private synchronized void handleConfirmation(UUID processId, String responseType, String details) {
        Objects.requireNonNull(getBookingRequestByTypeAndDetails(Objects.requireNonNull(getBookingByUUID(processId)), responseType, details)).confirm();
        System.out.println(ANSI_GREEN + "TravelBroker - " +responseType + " " + details + " of "+processId +" was confirmed. Waiting for other responses." + ANSI_RESET);
    }
    private Booking getBookingByUUID(UUID processId){
        for(Booking booking:bookings){
            if(booking.processID().equals(processId)){
                return booking;
            }
        }
        return null;
    }
    private BookingRequest getBookingRequestByTypeAndDetails(Booking booking, String type, String details){
        for(BookingRequest request:booking.requests()){if(request.getType().equals(type) && (request.getName() +" "+ request.getQuantity()).equals(details)){
                return request;
            }
        }
        return null;
    }
    private synchronized void handleCancellationConfirmation(String message, UUID processId) {
        String responseType = message.split(" ", 5)[3];
        String details = message.split(" ", 5)[4];
        getBookingRequestByTypeAndDetails(getBookingByUUID(processId), responseType, details).confirmCancel();
    }
    private synchronized void handleCancellation(UUID processId, String responseType, String details) {
        System.out.println(ANSI_RED + "TravelBroker - " +responseType + " " + details + " of " +processId + " was rejected. Waiting for other responses." + ANSI_RESET);
        getBookingRequestByTypeAndDetails(getBookingByUUID(processId), responseType, details).reject();
        getBookingRequestByTypeAndDetails(getBookingByUUID(processId), responseType, details).confirmCancel();
    }
    private void sendResponse(UUID processId, boolean success) {
        String clientMessage = "ClientResponse " + processId + " " + success;
        System.out.println(ANSI_CYAN + "TravelBroker - Sending Response to Message Broker: " + clientMessage+ ANSI_RESET);
        MessageSenderService.sendMessageToMessageBroker(clientMessage);
    }
    private void sendCancellationRequest(UUID processId, String type, String details) {
        String cancelMessage = "CancellationRq " + processId + " " + type + " " + details;
        System.out.println(ANSI_CYAN+"TravelBroker - Sending Cancellation Request to Message Broker: " + cancelMessage+ ANSI_RESET);
        MessageSenderService.sendMessageToMessageBroker(cancelMessage);
    }
}

