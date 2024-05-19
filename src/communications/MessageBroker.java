package communications;

import misc.MessageSenderService;
import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * A class representing a message broker.
 */
public class MessageBroker {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";
    private final int port;
    private final Map<UUID, List<String>> processMessages = new HashMap<>();
    private final Map<UUID, Socket> clientSockets = new HashMap<>();
    private final int delay;
    private final ArrayList<UUID> finishedProcessList = new ArrayList<>();

    /**
     * Constructs a new {@link MessageBroker} with the specified port.
     *
     * @param port the unique port of the {@link MessageBroker}.
     */
    public MessageBroker(int port) {
        this.port = port;
        delay = Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.delay"));
    }

    /**
     * Starts the {@link MessageBroker} with the specified {@code backlog} of the {@link ServerSocket}.
     *
     * @param backlog the maximum length of the queue of incoming connections.
     */
    public void start(int backlog) {
        try (ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            System.out.println(ANSI_BLUE + "MessageBroker running on port " + serverSocket.getLocalPort() + ANSI_RESET);
            while (true) {
                //Accept client connection
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            Thread.sleep(delay);
                            sendMessageToCorrectReceiver(inputLine, clientSocket);
                        }
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                clientThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the message and forwards it to the correct receiver
     *
     * @param message the received message
     * @param socket  the socket of the client
     */
    private void sendMessageToCorrectReceiver(String message, Socket socket) {
        //MessageSplit [0] = WhatAmI, [1] = ProcessId, [2] = Message
        String[] messageSplit = message.split(" ", 3);
        UUID processID = UUID.fromString(messageSplit[1]);
        Socket clientSocket = clientSockets.get(processID);
        String whatAmI = messageSplit[0];
        switch (whatAmI) {
            //CLient Request coming from Client
            case "ClientRq":
                synchronized (clientSockets) {
                    clientSockets.put(processID, socket);
                }
                //Send message to TravelBroker
                MessageSenderService.sendMessageToTravelBroker(message, processID, port);
                break;


            //Confirmation coming from Hotel and Flight Systems
            case "Response":
                //Send messages to TravelBroker
                MessageSenderService.sendMessageToTravelBroker(message, processID);
                break;

            //Booking Request coming from TravelBroker
            case "BookingRq":
                //Send message to Hotel and Flight Systems respectively
                MessageSenderService.sendMessageToBookingSystem(message);
                break;

            //Cancellation Request coming from TravelBroker
            case "CancellationRq":
                //Send message to Hotel and Flight Systems respectively
                MessageSenderService.sendMessageToBookingSystem(message);
                break;

            case "CancellationConfirmation":
                //Send message to TravelBroker
                MessageSenderService.sendMessageToTravelBroker(message, processID);
                break;

            //Client Confirmation coming from TravelBroker
            case "ClientResponse":
                //deconstruct message and send to correct client
                if (messageSplit[2].equals("false")) {
                    if (!finishedProcessList.contains(processID)) {
                        finishedProcessList.add(processID);
                        MessageSenderService.sendMessageToClient(clientSocket, "Sorry! Your booking " + processID + " is not confirmed!");
                    }
                } else {
                    MessageSenderService.sendMessageToClient(clientSocket, "Yay! Your booking " + processID + " is confirmed!");
                }

                break;
            case "Error":
                MessageSenderService.sendMessageToClient(clientSocket, "Error! " + messageSplit[2]);
                break;

            default:
                System.out.println(ANSI_RED + "MessageBroker - Message not recognized: " + message + ANSI_RESET);
                break;
        }
    }

}
