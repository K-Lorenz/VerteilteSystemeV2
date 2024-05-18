package communications;

import misc.MessageSenderService;
import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class MessageBroker {
    private final int port;
    private ArrayList<UUID> finishedProcessList = new ArrayList<>();
    private final Map<UUID, List<String>> processMessages = new HashMap<>();
    private final Map<UUID, Socket> clientSockets = new HashMap<>();
    private final int delay;
    public MessageBroker() {
        port = Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.port"));
        delay = Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.delay"));
    }

    public void start(int backlog) {
        //Start server
        try (ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            System.out.println("MessageBroker running on port " + serverSocket.getLocalPort());
            while (true) {
                //Accept client connection
                Socket clientSocket = serverSocket.accept();
                //Start client thread for multi Threading
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

    private void sendMessageToCorrectReceiver(String message, Socket socket) {
        //MessageSplit [0] = WhatAmI, [1] = ProcessId, [2] = Message
        String[] messageSplit = message.split(" ", 3);
        UUID processID = UUID.fromString(messageSplit[1]);
        Socket clientSocket = clientSockets.get(processID);
        String whatAmI = messageSplit[0];
        switch (whatAmI) {
            //CLient Request coming from Client
            case "ClientRq":
                synchronized (clientSockets){
                    clientSockets.put(processID, socket);
                }
                //Send message to TravelBroker
                MessageSenderService.sendMessageToTravelBroker(message);
                break;


            //Confirmation coming from Hotel and Flight Systems
            case "Response":
                //Send messages to TravelBroker
                MessageSenderService.sendMessageToTravelBroker(message);
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
                MessageSenderService.sendMessageToTravelBroker(message);
                break;

            //Client Confirmation coming from TravelBroker
            case "ClientResponse":
                //deconstruct message and send to correct client
                if (messageSplit[2].equals("false"))
                {
                    if(!finishedProcessList.contains(processID)){
                        finishedProcessList.add(processID);
                        MessageSenderService.sendMessageToClient(clientSocket, "Sorry! Your booking " + processID + " is not confirmed!"); 
                    }
                }
                else{
                    MessageSenderService.sendMessageToClient(clientSocket, "Yay! Your booking " + processID + " is confirmed!");
                }

                break;
            case "Error":
                MessageSenderService.sendMessageToClient(clientSocket, "Error! " + messageSplit[2]);
                break;
                
            default:
                System.out.println("MessageBroker - Message not recognized: " + message);
                break;
        }
    }

}
