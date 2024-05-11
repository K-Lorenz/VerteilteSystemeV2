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
        //ConfirmMessageSplit [0] = WhatAmI, [1] = processId, [2] = Successful, [3] = Type, [4] = Hotel/FlightNumber Quantity
        String[] confirmMessageSplit = message.split(" ", 5);
        if (confirmMessageSplit[2] == "true") {
            if (confirmMessageSplit[3] == "flight") {
                if (confirmedHotel.containsKey(processId)) {
                    //Send Confirmation to Client
                    System.out.println("Flight Confirmation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". Hotel Confirmation: " + confirmedHotel.get(processId));
                    System.out.println("Sending Confirmation to Client");
                    String clientMessage = "ClientConfirmation " + processId + " " + "true";
                    MessageSenderService.sendMessageToMessageBroker(clientMessage);
                } else if (canceledHotel.containsKey(processId)) {
                    //Send Cancellation to Client
                    System.out.println("Flight Confirmation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". Hotel Cancellation: " + canceledHotel.get(processId));
                    String cancelMessage = "CancellationRq " + processId + " " + confirmMessageSplit[3] + " " + confirmMessageSplit[4];
                    MessageSenderService.sendMessageToMessageBroker(cancelMessage);
                } else {
                    //Store Flight Confirmation
                    System.out.println("Flight Confirmation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". No response from Hotel yet");
                    confirmedFlight.put(processId, confirmMessageSplit[3] + " " + confirmMessageSplit[4]);
                }
            } else if (confirmMessageSplit[3] == "hotel") {
                if (confirmedFlight.containsKey(processId)) {
                    //Send Confirmation to Client
                    System.out.println("Hotel Confirmation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". Flight Confirmation: " + confirmedFlight.get(processId));
                    System.out.println("Sending Confirmation to Client");
                    String clientMessage = "ClientConfirmation " + processId + " " + "true";
                    MessageSenderService.sendMessageToMessageBroker(clientMessage);
                } else if (canceledFlight.containsKey(processId)) {
                    //Send Cancellation to Client
                    System.out.println("Hotel Confirmation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". Flight Cancellation: " + canceledFlight.get(processId) + ". No response from Flight yet");
                    String cancelMessage = "CancellationRq " + processId + " " + confirmMessageSplit[3] + " " + confirmMessageSplit[4];
                    MessageSenderService.sendMessageToMessageBroker(cancelMessage);
                } else {
                    //Store Hotel Confirmation
                    System.out.println("Hotel Confirmation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". No response from Flight yet");
                    confirmedHotel.put(processId, confirmMessageSplit[3] + " " + confirmMessageSplit[4]);
                }
            }
        } else if (confirmMessageSplit[2] == "false") {
            if (confirmMessageSplit[3] == "flight") {
                if (confirmedHotel.containsKey(processId)) {
                    //Send Cancellation to Client
                    System.out.println("Flight Cancellation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". Hotel Confirmation: " + confirmedHotel.get(processId));
                    String cancelMessage = "CancellationRq " + processId + " " + confirmedHotel.get(processId);
                    MessageSenderService.sendMessageToMessageBroker(cancelMessage);
                } else if (canceledHotel.containsKey(processId)) {
                    //Send Cancellation to Client
                    System.out.println("Flight Cancellation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". Hotel Cancellation: " + canceledHotel.get(processId));
                    System.out.println("Sending Cancellation to Client");
                    String clientMessage = "ClientConfirmation " + processId + " " + "false";
                    MessageSenderService.sendMessageToMessageBroker(clientMessage);
                } else {
                    //Store Flight Cancellation
                    System.out.println("Flight Cancellation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". No response from Hotel yet");
                    canceledFlight.put(processId, confirmMessageSplit[3] + " " + confirmMessageSplit[4]);
                }
            } else if (confirmMessageSplit[3] == "hotel") {
                if (confirmedFlight.containsKey(processId)) {
                    //Send Cancellation to Client
                    System.out.println("Hotel Cancellation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". Flight Confirmation: " + confirmedFlight.get(processId));
                    String cancelMessage = "CancellationRq " + processId + " " + confirmedFlight.get(processId);
                    MessageSenderService.sendMessageToMessageBroker(cancelMessage);
                } else if (canceledFlight.containsKey(processId)) {
                    //Send Cancellation to Client
                    System.out.println("Hotel Cancellation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". Flight Cancellation: " + canceledFlight.get(processId));
                    System.out.println("Sending Cancellation to Client");
                    String clientMessage = "ClientConfirmation " + processId + " " + "false";
                    MessageSenderService.sendMessageToMessageBroker(clientMessage);
                } else {
                    //Store Hotel Cancellation
                    System.out.println("Hotel Cancellation: " + confirmMessageSplit[3] + " " + confirmMessageSplit[4] + ". No response from Flight yet");
                    canceledHotel.put(processId, confirmMessageSplit[3] + " " + confirmMessageSplit[4]);
                }
            }
        }
    }
}

