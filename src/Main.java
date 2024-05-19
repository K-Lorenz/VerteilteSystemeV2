import booking.FlightBookingSystem;
import booking.HotelBookingSystem;
import communications.Client;
import communications.MessageBroker;
import communications.TravelBroker;
import misc.PropertyLoader;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static Random random = new Random();

    public static void main(String[] args) throws InterruptedException {
        int clientAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.client.amount"));
        int bookingRequestAmountMax = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.bookingrequest.amount.max"));
        int bookingRequestAmountMin = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.bookingrequest.amount.min"));
        int bookingHotelAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.amount"));
        int bookingFlightAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.amount"));
        int bookingHotelPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.port.start"));
        int bookingFlightPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.port.start"));
        int messageBrokerPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.port.start"));
        int messageBrokerAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("messagebroker.amount"));
        int travelBrokerAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.amount"));
        int travelBrokerPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.port.start"));

        for (int i = 0; i < messageBrokerAmount; i++) {
            int finalI = i;
            Thread mBThread = new Thread(() -> {
                MessageBroker messageBroker = new MessageBroker(finalI + messageBrokerPortStart);
                messageBroker.start(clientAmount * 50);
            });
            mBThread.start();
        }
        for (int i = 0; i < travelBrokerAmount; i++) {
            int finalI = i;
            Thread tBThread = new Thread(() -> {
                TravelBroker travelBroker = new TravelBroker(finalI + travelBrokerPortStart);
                travelBroker.start(clientAmount * 50);
            });
            tBThread.start();
        }

        ConcurrentHashMap<UUID, Boolean> finishedProcessList = new ConcurrentHashMap<>();
        for (int i = 0; i < clientAmount; i++) {
            Thread clThread = new Thread(() -> {
                Client client = new Client();
                boolean result = client.start("book " + randomString(bookingRequestAmountMin, bookingRequestAmountMax, bookingHotelAmount, bookingFlightAmount));
                finishedProcessList.put(client.processID, result);
            });
            clThread.start();
        }
        HashMap<Integer, String> hotelNames = setHotelNames(bookingHotelAmount, bookingHotelPortStart);
        for (int i = 0; i < bookingHotelAmount; i++) {
            int finalI = i;
            Thread bHThread = new Thread(() -> {
                HotelBookingSystem hotelBookingSystem = new HotelBookingSystem(bookingHotelPortStart + finalI, hotelNames);
                hotelBookingSystem.start(clientAmount * 50);
            });
            bHThread.start();
        }
        HashMap<Integer, String> airlineNames = setAirlineNames(bookingFlightAmount, bookingFlightPortStart);
        for (int i = 0; i < bookingFlightAmount; i++) {
            int finalI = i;
            Thread bFThread = new Thread(() -> {
                FlightBookingSystem flightBookingSystem = new FlightBookingSystem(bookingFlightPortStart + finalI, airlineNames);
                flightBookingSystem.start(clientAmount * 50);
            });
            bFThread.start();
        }
        startServer(bookingHotelPortStart, bookingFlightPortStart, hotelNames, airlineNames);
        if (clientAmount != 0) {
            while (true) {
                Thread.sleep(2000);
                if (finishedProcessList.size() == clientAmount) {
                    System.out.println("All Clients finished");
                    int success = 0;
                    for (UUID processID : finishedProcessList.keySet()) {
                        if (Boolean.TRUE.equals(finishedProcessList.get(processID))) {
                            success++;
                        }
                        System.out.println((Boolean.TRUE.equals(finishedProcessList.get(processID)) ? ANSI_GREEN : ANSI_RED) + processID + " finished with " + finishedProcessList.get(processID) + ANSI_RESET);
                    }
                    System.out.println(ANSI_BOLD + ANSI_YELLOW + "Success Rate: " + (success * 100 / clientAmount) + "%" + ANSI_RESET);
                    break;
                }
            }
        }
    }

    public static String randomString(int min, int max, int bookingHotelAmount, int bookingFlightAmount) {
        int paramAmount = random.nextInt(min, max);
        StringBuilder builderString = new StringBuilder();
        for (int i = 0; i < paramAmount; i++) {
            boolean type = random.nextBoolean();
            if (type) {
                builderString.append("--flight 'f").append(random.nextInt(0, bookingFlightAmount));
            } else {
                builderString.append("--hotel 'h").append(random.nextInt(0, bookingHotelAmount));
            }
            builderString.append("' ").append(random.nextInt(1, 10)).append(" ");
        }
        return builderString.toString();
    }


    public static HashMap<Integer, String> setAirlineNames(int bookingFlightAmount, int bookingFlightPortStart) {
        HashMap<Integer, String> airlineList = new HashMap<>();
        final String[] airLineNames = {"Zaun Airways", "Air Piltover", "Fly Freljord", "Ionia Air", "Bandle Airways", "Shurima Skyline"};
        for (int i = bookingFlightPortStart; i < bookingFlightAmount + bookingFlightPortStart; i++) {
            airlineList.put(i, airLineNames[random.nextInt(airLineNames.length)]);
        }
        return airlineList;
    }

    public static HashMap<Integer, String> setHotelNames(int bookingHotelAmount, int bookingHotelPortStart) {
        final HashMap<Integer, String> hotelList = new HashMap<>();
        final String[] hotelNames = {"Schachtelwirt", "Hotel zur Kluft", "Gasthof zum Löwen", "Hotel zur Post", "Hotel zur Sonne", "Hotel zum Bären", "Hotel zum Hirschen", "Hotel zum Ochsen", "Hotel zum Schwan", "Hotel zum Stern", "Hotel zum Storchen", "Hotel zum Taunus", "Hotel zum Turm", "Hotel zum weißen Ross", "Hotel zum weißen Schwan", "Hotel zur alten Post", "Hotel zur alten Schule", "Hotel zur alten Stadtmauer"};
        for (int i = bookingHotelPortStart; i < bookingHotelAmount + bookingHotelPortStart; i++) {
            String hotelName = hotelNames[random.nextInt(hotelNames.length)];
            while (hotelList.containsValue(hotelName)) {
                hotelName = hotelNames[random.nextInt(hotelNames.length)];
            }
            hotelList.put(i, hotelName);
        }
        return hotelList;
    }


    //after executing Main, visit localhost:8080 to see the hotel and airline Names, keep Main running to see them
    public static void startServer(int bookingHotelPortStart, int bookingFlightPortStart, HashMap<Integer, String> hotelNames, HashMap<Integer, String> airlineNames) {
        StringBuilder hotelListHtml = new StringBuilder("<h2>Hotels:</h2>");
        for (Integer port : hotelNames.keySet()) {
            hotelListHtml.append("<p>").append("H").append(port - bookingHotelPortStart).append(": ").append(hotelNames.get(port)).append("</p>");
        }

        StringBuilder airlineListHtml = new StringBuilder("<h2>Airlines:</h2>");
        for (Integer port : airlineNames.keySet()) {
            airlineListHtml.append("<p>").append("F").append(port - bookingFlightPortStart).append(": ").append(airlineNames.get(port)).append("</p>");
        }

        String htmlContent = "<!DOCTYPE html>" + "<html lang=\"en\">" + "<head>" + "<meta charset=\"UTF-8\">" + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + "<title>Airlines and Hotels</title>" + "</head>" + "<body>" + "<h1>Airlines and Hotels</h1>" + hotelListHtml + airlineListHtml + "</body>" + "</html>";

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8080)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html");
                    out.println();
                    out.println(htmlContent);
                    out.close();
                    clientSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}