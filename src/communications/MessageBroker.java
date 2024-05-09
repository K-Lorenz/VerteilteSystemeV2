package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class MessageBroker {
    private int port;
    private Map<UUID, List<String>> processMessages = new HashMap<>();
    private Map<UUID, Socket> clientSockets = new HashMap<>();
    public MessageBroker(int port) {
        this.port = port;
    }
    public void start() {
        //Start server
        try(ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Server is running on port "+serverSocket.getLocalPort());
            while(true){
                //Accept client connection
                Socket clientSocket = serverSocket.accept();
                //Start client thread for multi Threading
                Thread clientThread = new Thread(()->{
                    try{
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String inputLine;
                        while((inputLine = in.readLine()) != null){
                            System.out.println("Received message: "+inputLine);
                            sendMessageToCorrectReceiver(inputLine, clientSocket);
                        }
                        in.close();
                        clientSocket.close();
                    } catch (IOException e) {
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
        String[] arr = message.split(" ", 4);
        UUID processID = UUID.fromString(arr[2]);
        UUID clientID = UUID.fromString(arr[0]);
        Socket clientSocket = clientSockets.get(clientID);
        String whatAmI = arr[1];
        if(!checkMessageAndAdd(message)) return;
        switch (whatAmI) {
            //CLient Request coming from Client
            case "ClientRq":
                clientSockets.put(clientID, socket);
                sendMessageToClient(socket, "Your request is now being handled by the TravelBroker!");
                //Send message to TravelBroker
                TravelBroker tb = new TravelBroker(socket);
                tb.start();
                break;

            //Reservation Request coming from TravelBroker
            case "ReservationRq":
                sendMessageToClient(clientSocket, "The TravelBroker has sent a request for reservation to the Hotel and Flight Systems!");
                //Send message to Hotel and Flight Systems respectively
                break;

            //Confirmation coming from Hotel and Flight Systems
            case "Confirmation":
                sendMessageToClient(clientSocket, "Flight/Hotel Systems responded to the reservation request!");
                //Send messages to TravelBroker
                break;

            //Booking Request coming from TravelBroker
            case "BookingRq":
                sendMessageToClient(clientSocket, "The TravelBroker sent a request for booking to the Hotel and Flight Systems!");
                //Send message to Hotel and Flight Systems respectively
                break;

            //Cancellation Request coming from TravelBroker
            case "CancellationRq":
                sendMessageToClient(clientSocket, "Something went wrong! The TravelBroker is sending cancellation requests to the Hotel and Flight Systems!");
                //Send message to Hotel and Flight Systems respectively
                break;

            //Client Confirmation coming from TravelBroker
            case "ClientConfirmation":
                //deconstruct message and send to correct client
                if(arr[3].equals("false"))
                    sendMessageToClient(clientSocket, "Sorry! Your booking is not confirmed!");
                else
                    sendMessageToClient(clientSocket, "Yay! Your booking is confirmed!");

                break;
            default:
                System.out.println("Message type not recognized");
                break;
        }
    }
    private boolean checkMessageAndAdd(String message){
        String[] arr = message.split(" ", 4);
        UUID processID = UUID.fromString(arr[2]);
        List<String> messages = processMessages.get(processID);
        if (messages == null){
            messages = new ArrayList<>();
            messages.add(message);
            processMessages.put(processID, messages);
            return true;
        }
        if(!messages.contains(message)){
            messages.add(message);
            processMessages.put(processID, messages);
            return true;
        }
        return false;
    }
    private void sendMessageToClient(Socket socket, String message){
        try{
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.port"));
        MessageBroker messageBroker = new MessageBroker(port);
        messageBroker.start();
    }
}
