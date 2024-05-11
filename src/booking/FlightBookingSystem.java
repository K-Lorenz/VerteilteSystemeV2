package booking;

import misc.MessageSenderService;
import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

public class FlightBookingSystem implements BookingSystem {

    private int port;
    private int airlineNumber;
    private final int flightPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.port.start"));
    private final int minSeats = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.quantity.min"));
    private final int maxSeats = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.quantity.max"));
    private int seats = new Random().nextInt(minSeats, maxSeats);
    private HashMap<Integer, String> airlineList = new HashMap<>();
    private final int airlines = flightPortStart + Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.amount"));
    private String[] names = {"Zaun Airways", "Demacia Airways", "Air Piltover", "Fly Freljord", "Air Noxus", "Ionia Air", "Bandle Airways", "Shurima Skyline", "Bilgewater Airways", "Fly Void"};


    public FlightBookingSystem(int port) {
        this.port = port;
    }

    public void start(int backlog){
        for(int i = flightPortStart; i < airlines; i++){
            airlineList.put(i, names[new Random().nextInt(names.length)]);
        }
        try(ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            System.out.println("FlightBookingSystem running on port " + port);
            while (true) {
                Socket flightSocket = serverSocket.accept();
                Thread flightThread = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            //Handle message and answer
                            handleRequest(inputLine, flightSocket);
                            System.out.println("FlightBookingSystem - Received message: " + inputLine);
                        }
                        in.close();
                        flightSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                flightThread.start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void handleRequest(String booking, Socket flightSocket){
        //splitMessage -> [0]=BookingRq [1]=UUID [2]=Menge
        String[] splitMessage = booking.split(" ", 3);
        String processId = splitMessage[1];
        String whatAmI = splitMessage[0];
        airlineNumber = port - flightPortStart;
        boolean successful;
        int requestedSeats = Integer.parseInt(splitMessage[2]);
        if (whatAmI.equals("BookingRq")) {
            successful = this.book(requestedSeats);
            //<WhatAmI> <processId> <confirmation (true/false)> <type> <Flightnumber> <amount>
            MessageSenderService.sendMessageToMessageBroker("Response " + processId + " " + successful+ " flight F" + airlineNumber + " " + requestedSeats);
        }else if(whatAmI.equals("CancellationRq")) {
            successful = this.cancel(requestedSeats);
            //<WhatAmI> <processId> <false>
            MessageSenderService.sendMessageToMessageBroker("CancellationConfirmation " + processId + " " + successful);
        }
    }


    @Override
    public boolean cancel(int requestedSeats) {
        this.seats += requestedSeats;
        System.out.println("HotelBookingSystem: " + requestedSeats + " rooms could not be booked. Remaining rooms: " + seats + ".");
        return true;
    }

    @Override
    public boolean book(int requestedSeats) {
        if (requestedSeats <= this.seats) {
            this.seats -= requestedSeats;
            System.out.println("HotelBookingSystem: " + requestedSeats + " seats booked. Remaining seats: " + seats + ".");
            return true;
        } else {
            System.out.println("HotelBookingSystem: " + requestedSeats + " seats could not be booked. Remaining seats: " + seats + ".");
            return false;
        }
    }

    @Override
    public String getName() {
        return airlineList.get(this.port);
    }
}
