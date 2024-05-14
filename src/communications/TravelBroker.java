package communications;

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
    private HashMap<UUID, List<String>> confirmedFlights = new HashMap<>();
    private HashMap<UUID, List<String>> canceledFlights = new HashMap<>();
    private HashMap<UUID, List<String>> confirmedHotels = new HashMap<>();
    private HashMap<UUID, List<String>> canceledHotels = new HashMap<>();
    private HashMap<UUID, Integer> amountOfHotelsInBooking = new HashMap<>();
    private HashMap<UUID, Integer> amountOfFlightsInBooking = new HashMap<>();
    private int retryAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.retries"));
    private int retryDelay = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.retryDelay"));


    public ArrayList<String> findUnconfirmed (UUID processId, ArrayList<String> allBookings){
        // f20 5, h2 5
        ArrayList<String> unconfirmedList = new ArrayList<>();
        List<String> allResponsesList = new ArrayList<>();
        if(confirmedFlights.containsKey(processId) && !confirmedFlights.get(processId).isEmpty()){
            allResponsesList.addAll(confirmedFlights.get(processId));
        }
        if(confirmedHotels.containsKey(processId) && !confirmedHotels.get(processId).isEmpty()){
            allResponsesList.addAll(confirmedHotels.get(processId));
        }
        if(canceledFlights.containsKey(processId) && !canceledFlights.get(processId).isEmpty()){
            allResponsesList.addAll(canceledFlights.get(processId));
        }
        if(canceledHotels.containsKey(processId) && !canceledHotels.get(processId).isEmpty()){
            allResponsesList.addAll(canceledHotels.get(processId));
        }

        for(String singleResponse: allBookings){
            if(!allResponsesList.contains(singleResponse)){
                unconfirmedList.add(singleResponse);
            }
        }
        return unconfirmedList;
    }

    public void checkCurrentBookingSituation(UUID processId, ArrayList<String> AllBookingsFromOneClientRQ){
        int count = 0;
        ArrayList<String> unconfirmedList = new ArrayList<>();
        try{
            do{
                unconfirmedList = findUnconfirmed(processId, AllBookingsFromOneClientRQ);
                for(String e : unconfirmedList){
                    if(e.split(" ")[0].contains("f")) {
                        System.out.println("TravelBroker - Sending BookingRequest to MessageBroker: " + processId + " flight " + e);
                        MessageSenderService.sendMessageToMessageBroker("BookingRq " + processId + " flight " + e);
                    } else {
                        System.out.println("TravelBroker - Sending BookingRequest to MessageBroker: " + processId + " flight " + e);
                        MessageSenderService.sendMessageToMessageBroker("BookingRq " + processId + " hotel " + e);
                    }
                }
                count++;
                if(count == retryAmount){
                    for(String e : unconfirmedList){
                        if(e.split(" ")[0].contains("f")) {
                            handleCancellation(processId, "flight", e);
                        } else {
                            handleCancellation(processId, "hotel", e);
                        }
                    }
                    break;
                }
                Thread.sleep(retryDelay);
            }while(!unconfirmedList.isEmpty());
        } catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public TravelBroker() {
        Properties properties = PropertyLoader.loadProperties();
        port = Integer.parseInt(properties.getProperty("travelbroker.port"));
    }

    public void start(int backlog) {
        try (ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            System.out.println("TravelBroker running on port " + port);
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

                System.out.println("TravelBroker - Received ClientRequest: '" + message +"'");
                int flights = 0;
                int hotels = 0;
                List<BookingRequest> bookingRequests = BookingRequestParser.parse(messageSplit[2]);
                ArrayList <String> allBookingsFromOneClientRQ = new ArrayList<>();
                for (BookingRequest bookingRequest : bookingRequests) {
                    //Send Flight/Hotel BookingRq to Message broker
                    if (bookingRequest.getType().equals("flight")) {
                        flights++;
                    } else {
                        hotels++;
                    }
                    allBookingsFromOneClientRQ.add(bookingRequest.getName() + " " + bookingRequest.getQuantity());
                }
                amountOfFlightsInBooking.put(processId, flights);
                amountOfHotelsInBooking.put(processId, hotels);

                Thread checkBookingThread = new Thread(() -> {
                    checkCurrentBookingSituation(processId, allBookingsFromOneClientRQ);
                });
                checkBookingThread.start();
                break;

            case "Response":
                //<WhatAmI> <processId> <confirmation (true/false)> <type> <Flight/Hotel number> <amount>
                System.out.println("TravelBroker - Received Response: '" + message +"'");
                handleResponse(message, processId);
                break;

            case "CancellationConfirmation":
                System.out.println("TravelBroker - Received CancellationConfirmation: '" + message +"'");
                System.out.println("TravelBroker - Sending ClientResponse(false) with processID : '" + processId +"'");
                String clientMessage = "ClientResponse " + processId + " " + "false";
                MessageSenderService.sendMessageToMessageBroker(clientMessage);
                break;

            default:
                System.out.println("TravelBroker - Message not recognized: " + message);
                break;
        }
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
        if (canceledFlights.containsKey(processId) || canceledHotels.containsKey(processId)) {
            System.out.println("TravelBroker - "+responseType + " " + details + "of" + processId +" was confirmed, but since another booking was canceled, it will be canceled as well.");
            sendCancellationRequest(processId, responseType, details);
            addToCanceledMap(processId, responseType, details);
            return;
        }
        if (responseType.equals("flight")) {
            confirmedFlights.putIfAbsent(processId, new ArrayList<>());
            confirmedFlights.get(processId).add(details);
            System.out.println("TravelBroker - Confirmed Flights of "+processId+": " + confirmedFlights.get(processId));
        } else {
            confirmedHotels.putIfAbsent(processId, new ArrayList<>());
            confirmedHotels.get(processId).add(details);
            System.out.println("TravelBroker - Confirmed Hotels of "+processId+": " + confirmedHotels.get(processId));
        }
        List<String> confirmedFlightsList = confirmedFlights.get(processId);
        List<String> confirmedHotelsList = confirmedHotels.get(processId);
        Integer amountOfFlights = amountOfFlightsInBooking.get(processId);
        Integer amountOfHotels = amountOfHotelsInBooking.get(processId);

        boolean allFlightsConfirmed = confirmedFlightsList != null && !confirmedFlightsList.isEmpty() && amountOfFlights != 0 && confirmedFlightsList.size() == amountOfFlights;
        boolean allHotelsConfirmed = confirmedHotelsList != null && !confirmedHotelsList.isEmpty() && amountOfHotels != 0 && confirmedHotelsList.size() == amountOfHotels;
        if ((allFlightsConfirmed && allHotelsConfirmed) || ((allFlightsConfirmed && amountOfHotels == 0) || (amountOfFlights == 0 && allHotelsConfirmed))) {
            System.out.println("TravelBroker - " + responseType + " " + details + " of "+processId +" was confirmed, and all other bookings were successful as well.");
            sendResponse(processId, true);
            return;
        }
        System.out.println("TravelBroker - " +responseType + " " + details + " of "+processId +" was confirmed. Waiting for other responses.");

    }

    private synchronized void handleCancellation(UUID processId, String responseType, String details) {
        addToCanceledMap(processId, responseType, details);
        if (confirmedFlights.containsKey(processId)) {
            System.out.println("TravelBroker - " +responseType + " " + details + " of " +processId +" was canceled, and since a flight was confirmed, it will be canceled.");
            for (String flight : confirmedFlights.get(processId)) {
                sendCancellationRequest(processId, "flight", flight);
            }
            canceledFlights.putIfAbsent(processId, new ArrayList<>());
            canceledFlights.get(processId).addAll(confirmedFlights.get(processId));
            confirmedFlights.remove(processId);
            return;
        }
        if (confirmedHotels.containsKey(processId)) {
            System.out.println("TravelBroker - " +responseType + " " + details + " of " +processId+" was canceled, and since a hotel was confirmed, it will be canceled.");
            for (String hotel : confirmedHotels.get(processId)) {
                sendCancellationRequest(processId, "hotel", hotel);
            }
            canceledHotels.putIfAbsent(processId, new ArrayList<>());
            canceledHotels.get(processId).addAll(confirmedHotels.get(processId));
            confirmedHotels.remove(processId);
            return;
        }
        List<String> canceledFlightsList = canceledFlights.get(processId);
        List<String> canceledHotelsList = canceledHotels.get(processId);
        Integer amountOfFlights = amountOfFlightsInBooking.get(processId);
        Integer amountOfHotels = amountOfHotelsInBooking.get(processId);

        boolean allFlightsCanceled = canceledFlightsList != null && !canceledFlightsList.isEmpty() && amountOfFlights != 0 && canceledFlightsList.size() == amountOfFlights;
        boolean allHotelsCanceled = canceledHotelsList != null && !canceledHotelsList.isEmpty() && amountOfHotels != 0 && canceledHotelsList.size() == amountOfHotels;

        if ((allFlightsCanceled && allHotelsCanceled) || ((allFlightsCanceled && amountOfHotels == 0) || (amountOfFlights == 0 && allHotelsCanceled))) {
            System.out.println("TravelBroker - " +responseType + " " + details + " of " +processId +" was canceled, and all other bookings were canceled as well.");
            sendResponse(processId, false);
            return;
        }
        System.out.println("TravelBroker - " +responseType + " " + details + " of " +processId + " was canceled. Waiting for other responses.");


    }

    private void addToCanceledMap(UUID processId, String responseType, String details) {
        if (responseType.equals("flight")) {
            canceledFlights.putIfAbsent(processId, new ArrayList<>());
            canceledFlights.get(processId).add(details);
            System.out.println("TravelBroker - Canceled Flights of " +processId +": " + canceledFlights.get(processId));
        } else {
            canceledHotels.putIfAbsent(processId, new ArrayList<>());
            canceledHotels.get(processId).add(details);
            System.out.println("TravelBroker - Canceled Hotels of " +processId +": " + canceledHotels.get(processId));
        }
    }

    private void sendResponse(UUID processId, boolean success) {
        String clientMessage = "ClientResponse " + processId + " " + success;
        System.out.println("TravelBroker - Sending Response to Message Broker: " + clientMessage);
        MessageSenderService.sendMessageToMessageBroker(clientMessage);
    }

    private void sendCancellationRequest(UUID processId, String type, String details) {
        String cancelMessage = "CancellationRq " + processId + " " + type + " " + details;
        System.out.println("TravelBroker - Sending Cancellation Request to Message Broker: " + cancelMessage);
        MessageSenderService.sendMessageToMessageBroker(cancelMessage);
    }
}

