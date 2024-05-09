package communications;

import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class TCPServer {
    public static void main(String[] args)throws IOException {

        Properties props = PropertyLoader.loadProperties();
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(props.getProperty("tcp.port")));


        System.out.println("Server is running on port "+serverSocket.getLocalPort());
        while(true){
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected from "+clientSocket.getInetAddress().getHostAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            while((inputLine = in.readLine()) != null){
                System.out.println("Received message: "+inputLine);
            }
            in.close();
            clientSocket.close();
        }
    }
}
