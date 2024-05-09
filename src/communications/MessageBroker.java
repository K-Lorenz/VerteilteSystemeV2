package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class MessageBroker {
    private int port;
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
                System.out.println("Client connected from "+clientSocket.getInetAddress().getHostAddress());
                //Start client thread for multi Threading
                Thread clientThread = new Thread(()->{
                    try{
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String inputLine;
                        while((inputLine = in.readLine()) != null){
                            System.out.println("Received message: "+inputLine);
                            String [] arr = inputLine.split(" ", 3);
                            // Check who the message is from to determine who it should be sent to
                            sentMessageToCorrectReceiver(arr[0], arr[1], UUID.fromString(arr[2]), arr[3]);
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

    private void sentMessageToCorrectReceiver(String whoAmI, String whatAmI, UUID processID, String message) {
        
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.port"));
        MessageBroker messageBroker = new MessageBroker(port);
        messageBroker.start();
    }
}
