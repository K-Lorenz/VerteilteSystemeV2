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
    private final int port;
    private final HashMap<UUID, List<String>> confirmedFlights = new HashMap<>();
    private final HashMap<UUID, List<String>> canceledFlights = new HashMap<>();
    private final HashMap<UUID, List<String>> confirmedHotels = new HashMap<>();
    private final HashMap<UUID, List<String>> canceledHotels = new HashMap<>();
    private final HashMap<UUID, Integer> amountOfHotelsInBooking = new HashMap<>();
    private final HashMap<UUID, Integer> amountOfFlightsInBooking = new HashMap<>();

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
                            //Handle message and answer
                            //Switch case for different messages
                            System.out.println("TravelBroker - Received message: " + inputLine);
                            handleRequest(inputLine, travelSocket);
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

    public void handleRequest(String message, Socket socket) {
        //MessageSplit [0] = WhatAmI, [1] = processId, [2] = Message
        String[] messageSplit = message.split(" ", 3);
        UUID processId = UUID.fromString(messageSplit[1]);
        String whatAmI = messageSplit[0];
        switch (whatAmI) {
            case "ClientRq":
                int flights = 0;
                int hotels = 0;
                List<BookingRequest> bookingRequests = BookingRequestParser.parse(messageSplit[2]);
                for (BookingRequest bookingRequest : bookingRequests) {
                    //Send Flight/Hotel BookingRq to Messagebroker
                    if (bookingRequest.type().equals("flight")) {
                        flights++;
                    } else {
                        hotels++;
                    }
                    System.out.println("TravelBroker - Sending BookingRq to MessageBroker: " + bookingRequest);
                    String newMessage = "BookingRq " + processId + " " + bookingRequest.type() + " " + bookingRequest.name() + " " + bookingRequest.quantity();
                    MessageSenderService.sendMessageToMessageBroker(newMessage);
                }
                amountOfFlightsInBooking.put(processId, flights);
                amountOfHotelsInBooking.put(processId, hotels);
                break;

            case "Response":
                //<WhatAmI> <processId> <confirmation (true/false)> <type> <Flightnumber> <amount>
                System.out.println("TravelBroker - Received Response: " + message);
                handleResponse(message, processId);
                break;

            case "CancellationConfirmation":
                System.out.println("TravelBroker - Received CancellationConfirmation: " + message);
                System.out.println("Sending Confirmation to Client");
                String clientMessage = "ClientResponse " + processId + " " + "false";
                MessageSenderService.sendMessageToMessageBroker(clientMessage);
                break;

            default:
                System.out.println("TravelBroker - Message not recognized: " + message);
                break;
        }
    }

    public void handleResponse(String message, UUID processId) {
        //<WhatAmI> <processId> <confirmation (true/false)> <type> <Flightnumber> <amount>
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


    private void handleConfirmation(UUID processId, String responseType, String details) {
        if (canceledFlights.containsKey(processId) || canceledHotels.containsKey(processId)) {
            System.out.println(responseType + " " + details + " was confirmed, but since another booking was canceled, it will be canceled as well.");
            sendCancellationRequest(processId, responseType, details);
            if (responseType.equals("flight")) {
                canceledFlights.get(processId).add(details);
                System.out.println("Canceled Flights: " + canceledFlights.get(processId));
            } else {
                canceledHotels.get(processId).add(details);
                System.out.println("Canceled Hotels: " + canceledHotels.get(processId));
            }
            return;
        }
        if (responseType.equals("flight")) {
            confirmedFlights.putIfAbsent(processId, new ArrayList<>());
            confirmedFlights.get(processId).add(details);
            System.out.println("Confirmed Flights: " + confirmedFlights.get(processId));
        } else {
            confirmedHotels.putIfAbsent(processId, new ArrayList<>());
            confirmedHotels.get(processId).add(details);
            System.out.println("Confirmed Hotels: " + confirmedHotels.get(processId));
        }
        List<String> confirmedFlightsList = confirmedFlights.get(processId);
        List<String> confirmedHotelsList = confirmedHotels.get(processId);
        Integer amountOfFlights = amountOfFlightsInBooking.get(processId);
        Integer amountOfHotels = amountOfHotelsInBooking.get(processId);

        if (confirmedFlightsList != null && confirmedHotelsList != null && amountOfFlights != null && amountOfHotels != null && confirmedFlightsList.size() == amountOfFlights && confirmedHotelsList.size() == amountOfHotels) {
            System.out.println(responseType + " " + details + " was confirmed, and all other bookings were successful as well.");
            sendResponse(processId, true);
            return;
        }
        System.out.println(responseType + " " + details + " was confirmed. Waiting for other responses.");
    }

    private void handleCancellation(UUID processId, String responseType, String details) {
        if (responseType.equals("flight")) {
            canceledFlights.putIfAbsent(processId, new ArrayList<>());
            canceledFlights.get(processId).add(details);
            System.out.println("Canceled Flights: " + canceledFlights.get(processId));
        } else {
            canceledHotels.putIfAbsent(processId, new ArrayList<>());
            canceledHotels.get(processId).add(details);
            System.out.println("Canceled Hotels: " + canceledHotels.get(processId));
        }

        if (confirmedFlights.containsKey(processId)) {
            System.out.println(responseType + " " + details + " was canceled, and since a flight was confirmed, it will be canceled.");
            for (String flight : confirmedFlights.get(processId)) {
                sendCancellationRequest(processId, "flight", flight);
            }
            canceledFlights.get(processId).addAll(confirmedFlights.get(processId));
            confirmedFlights.remove(processId);
            return;
        }
        if (confirmedHotels.containsKey(processId)) {
            System.out.println(responseType + " " + details + " was canceled, and since a hotel was confirmed, it will be canceled.");
            for (String hotel : confirmedHotels.get(processId)) {
                sendCancellationRequest(processId, "hotel", hotel);
            }
            canceledHotels.get(processId).addAll(confirmedHotels.get(processId));
            confirmedHotels.remove(processId);
            return;
        }
        List<String> canceledFlightsList = canceledFlights.get(processId);
        List<String> canceledHotelsList = canceledHotels.get(processId);

        if (canceledFlightsList != null && canceledHotelsList != null && canceledFlightsList.size() == amountOfFlightsInBooking.get(processId) && canceledHotelsList.size() == amountOfHotelsInBooking.get(processId)) {
            System.out.println(responseType + " " + details + " was canceled, and all other bookings were canceled as well.");
            sendResponse(processId, false);
            return;
        }
        System.out.println(responseType + " " + details + " was canceled. Waiting for other responses.");


    }

    private void sendResponse(UUID processId, boolean success) {
        String clientMessage = "ClientResponse " + processId + " " + success;
        System.out.println("Sending Response to Message Broker: " + clientMessage);
        MessageSenderService.sendMessageToMessageBroker(clientMessage);
    }

    private void sendCancellationRequest(UUID processId, String type, String details) {
        String cancelMessage = "CancellationRq " + processId + " " + type + " " + details;
        System.out.println("Sending Cancellation Request to Message Broker: " + cancelMessage);
        MessageSenderService.sendMessageToMessageBroker(cancelMessage);
    }
}

