package communications;

import misc.BookingRequest;
import misc.BookingRequestParser;
import misc.MessageSenderService;
import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;


public class TravelBroker {
    private final int port;
    private final HashMap<UUID, String> confirmedFlight = new HashMap<>();
    private final HashMap<UUID, String> confirmedHotel = new HashMap<>();
    private final HashMap<UUID, String> canceledFlight = new HashMap<>();
    private final HashMap<UUID, String> canceledHotel = new HashMap<>();

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
                List<BookingRequest> bookingRequests = BookingRequestParser.parse(messageSplit[2]);
                for (BookingRequest bookingRequest : bookingRequests) {
                    //Send Flight/Hotel BookingRq to Messagebroker
                    System.out.println("TravelBroker - Sending BookingRq to MessageBroker: " + bookingRequest.toString());
                    String newMessage = "BookingRq " + processId + " " + bookingRequest.type() + " " + bookingRequest.name() + " " + bookingRequest.quantity();
                    MessageSenderService.sendMessageToMessageBroker(newMessage);
                }
                break;

            case "Response":
                System.out.println("TravelBroker - Received Response: " + message);
                System.out.println("Checking if all responses from one booking are received and successful");
                handleResponse(message, processId);
                break;

            case "CancellationConfirmation":
                System.out.println("TravelBroker - Received CancellationConfirmation: " + message);
                System.out.println("Sending Confirmation to Client");
                String clientMessage = "ClientConfirmation " + processId + " " + "false";
                MessageSenderService.sendMessageToMessageBroker(clientMessage);
                break;

            default:
                System.out.println("TravelBroker - Message not recognized: " + message);
                break;
        }
    }

    public void handleResponse(String message, UUID processId) {
        String[] confirmMessageSplit = message.split(" ", 5);
        String confirmationType = confirmMessageSplit[2];
        String responseType = confirmMessageSplit[3];

        switch (confirmationType) {
            case "true":
                switch (responseType) {
                    case "flight":
                        handleFlightConfirmation(processId, confirmMessageSplit);
                        break;

                    case "hotel":
                        handleHotelConfirmation(processId, confirmMessageSplit);
                        break;
                }
                break;

            case "false":
                switch (responseType) {
                    case "flight":
                        handleFlightCancellation(processId, confirmMessageSplit);
                        break;

                    case "hotel":
                        handleHotelCancellation(processId, confirmMessageSplit);
                        break;
                }
                break;
            default:
                System.out.println("TravelBroker - Confirmation not recognized: " + message);
                break;
        }
    }

    private void handleFlightConfirmation(UUID processId, String[] confirmMessageSplit) {
        if (confirmedHotel.containsKey(processId)) {
            System.out.println("Flight Confirmation: flight " + confirmMessageSplit[4] + ". Hotel Confirmation: " + confirmedHotel.get(processId));
            sendResponse(processId, true);
        } else if (canceledHotel.containsKey(processId)) {
            System.out.println("Flight Confirmation: flight " + confirmMessageSplit[4] + ". Hotel Cancellation: " + canceledHotel.get(processId));
            sendCancellationRequest(processId, "flight", confirmMessageSplit[4]);
        } else {
            System.out.println("Flight Confirmation: flight " + confirmMessageSplit[4] + ". No response from Hotel yet");
            confirmedFlight.put(processId, "flight " + confirmMessageSplit[4]);
        }
    }

    private void handleHotelConfirmation(UUID processId, String[] confirmMessageSplit) {
        if (confirmedFlight.containsKey(processId)) {
            System.out.println("Hotel Confirmation: hotel " + confirmMessageSplit[4] + ". Flight Confirmation: " + confirmedFlight.get(processId));
            sendResponse(processId, true);
        } else if (canceledFlight.containsKey(processId)) {
            System.out.println("Hotel Confirmation: hotel " + confirmMessageSplit[4] + ". Flight Cancellation: " + canceledFlight.get(processId) + ". No response from Flight yet");
            sendCancellationRequest(processId, "hotel", confirmMessageSplit[4]);
        } else {
            System.out.println("Hotel Confirmation: hotel " + confirmMessageSplit[4] + ". No response from Flight yet");
            confirmedHotel.put(processId, "hotel " + confirmMessageSplit[4]);
        }
    }

    private void handleFlightCancellation(UUID processId, String[] confirmMessageSplit) {
        if (confirmedHotel.containsKey(processId)) {
            System.out.println("Flight Cancellation: flight " + confirmMessageSplit[4] + ". Hotel Confirmation: " + confirmedHotel.get(processId));
            sendCancellationRequest(processId, "hotel", confirmedHotel.get(processId));
        } else if (canceledHotel.containsKey(processId)) {
            System.out.println("Flight Cancellation: flight " + confirmMessageSplit[4] + ". Hotel Cancellation: " + canceledHotel.get(processId));
            sendResponse(processId, false);
        } else {
            System.out.println("Flight Cancellation: flight " + confirmMessageSplit[4] + ". No response from Hotel yet");
            canceledFlight.put(processId, "flight " + confirmMessageSplit[4]);
        }
    }

    private void handleHotelCancellation(UUID processId, String[] confirmMessageSplit) {
        if (confirmedFlight.containsKey(processId)) {
            System.out.println("Hotel Cancellation: hotel " + confirmMessageSplit[4] + ". Flight Confirmation: " + confirmedFlight.get(processId));
            sendCancellationRequest(processId, "flight", confirmedFlight.get(processId));
        } else if (canceledFlight.containsKey(processId)) {
            System.out.println("Hotel Cancellation: hotel " + confirmMessageSplit[4] + ". Flight Cancellation: " + canceledFlight.get(processId));
            sendResponse(processId, false);
        } else {
            System.out.println("Hotel Cancellation: hotel " + confirmMessageSplit[4] + ". No response from Flight yet");
            canceledHotel.put(processId, "hotel " + confirmMessageSplit[4]);
        }
    }

    private void sendResponse(UUID processId, boolean success) {
        String clientMessage = "ClientResponse " + processId + " " + success;
        System.out.println("Sending Response to Message Broker");
        MessageSenderService.sendMessageToMessageBroker(clientMessage);
    }

    private void sendCancellationRequest(UUID processId, String type, String details) {
        String cancelMessage = "CancellationRq " + processId + " " + type + " " + details;
        System.out.println("Sending Cancellation Request to Message Broker");
        MessageSenderService.sendMessageToMessageBroker(cancelMessage);
    }
}

