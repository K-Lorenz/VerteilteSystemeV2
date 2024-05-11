package booking;

import misc.MessageSenderService;
import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

public class HotelBookingSystem implements BookingSystem {

    private final int hotelPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.port.start"));
    private final int minRooms = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.quantity.min"));
    private final int maxRooms = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.quantity.max"));
    private final int hotels = hotelPortStart + Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.amount"));
    private final int port;
    private final HashMap<Integer, String> hotelList = new HashMap<>();
    private final String[] names = {"Schachtelwirt", "Hotel zur Kluft", "Gasthof zum Löwen", "Hotel zur Post", "Hotel zur Sonne", "Hotel zum Bären", "Hotel zum Hirschen", "Hotel zum Ochsen", "Hotel zum Schwan", "Hotel zum Stern", "Hotel zum Storchen", "Hotel zum Taunus", "Hotel zum Turm", "Hotel zum weißen Ross", "Hotel zum weißen Schwan", "Hotel zur alten Post", "Hotel zur alten Schule", "Hotel zur alten Stadtmauer"};
    private int hotelNumber;
    private int rooms = new Random().nextInt(minRooms, maxRooms);


    public HotelBookingSystem(int port) {
        this.port = port;
    }

    public void start(int backlog) {
        for (int i = hotelPortStart; i < hotels; i++) {
            hotelList.put(i, names[new Random().nextInt(names.length)]);
        }
        try (ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            System.out.println("HotelBookingSystem running on port " + port);
            while (true) {
                Socket hotelSocket = serverSocket.accept();
                Thread hotelThread = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(hotelSocket.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            //Handle message and answer
                            handleRequest(inputLine, hotelSocket);
                            System.out.println("HotelBookingSystem - Received message: " + inputLine);
                        }
                        in.close();
                        hotelSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                hotelThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRequest(String booking, Socket hotelSocket) {
        //splitMessage -> [0]=BookingRq [1]=UUID [2]=Menge
        String[] splitMessage = booking.split(" ", 3);
        String processId = splitMessage[1];
        String whatAmI = splitMessage[0];
        hotelNumber = port - hotelPortStart;
        boolean successful;
        int requestedRooms = Integer.parseInt(splitMessage[2]);
        if (whatAmI.equals("BookingRq")) {
            successful = book(requestedRooms);
            //<WhatAmI> <processId> <confirmation (true/false)> <type> <Hotelnumber> <amount>
            MessageSenderService.sendMessageToMessageBroker("Response " + processId + " " + successful + " hotel H" + hotelNumber + " " + requestedRooms);
        } else if (whatAmI.equals("CancellationRq")) {
            successful = cancel(requestedRooms);
            //<WhatAmI> <processId> <false>
            MessageSenderService.sendMessageToMessageBroker("CancellationConfirmation " + processId + " " + successful);
        }


    }


    @Override
    public boolean cancel(int requestedRooms) {
        rooms += requestedRooms;
        System.out.println("HotelBookingSystem: " + requestedRooms + " rooms could not be booked. Remaining rooms: " + rooms + ".");
        return true;
    }

    @Override
    public boolean book(int requestedRooms) {
        if (requestedRooms > rooms) {
            System.out.println("HotelBookingSystem: " + requestedRooms + " rooms could not be booked. Remaining rooms: " + rooms + ".");
            return false;
        }
        rooms -= requestedRooms;
        System.out.println("HotelBookingSystem: " + getName() + " " + requestedRooms + " rooms booked. Remaining rooms: " + rooms + ".");
        return true;
    }


    @Override
    public String getName() {
        return hotelList.get(this.port);
    }
}
