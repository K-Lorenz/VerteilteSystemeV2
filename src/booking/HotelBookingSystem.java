package booking;

import misc.MessageSenderService;
import misc.PropertyLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class HotelBookingSystem implements BookingSystem {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";
    private final int hotelPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.port.start"));
    private final int minRooms = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.quantity.min"));
    private final int maxRooms = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.quantity.max"));
    private final int processingTime = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.processingtime"));
    private final int port;
    private final HashMap<Integer, String> hotelList;
    private final HashMap<String, Boolean> bookingList = new HashMap<>();
    private final List<String> cancelList = new ArrayList<>();
    private int rooms = new Random().nextInt(minRooms, maxRooms);

    /**
     * Constructs a new {@link HotelBookingSystem} with the specified port and a list of names.
     *
     * @param port      the unique port of the {@link HotelBookingSystem}.
     * @param hotelList a {@link HashMap} containing the hotel names.
     */
    public HotelBookingSystem(int port, HashMap<Integer, String> hotelList) {
        this.port = port;
        this.hotelList = hotelList;
    }

    /**
     * Starts the {@link HotelBookingSystem} with the specified {@code backlog} of the {@link ServerSocket}.
     *
     * @param backlog the maximum length of the queue of incoming connections.
     */
    public void start(int backlog) {
        try (ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            System.out.println(ANSI_BLUE + "HotelBookingSystem(" + getName() + ") - running on port " + port + ANSI_RESET);
            while (true) {
                Socket hotelSocket = serverSocket.accept();
                Thread hotelThread = new Thread(() -> {
                    try {
                        Thread.sleep(processingTime);
                        BufferedReader in = new BufferedReader(new InputStreamReader(hotelSocket.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            //handle the request or "fails" the request with a given probability
                            System.out.println(ANSI_YELLOW + "HotelBookingSystem(" + getName() + ") - Received message: " + inputLine + ANSI_RESET);
                            double randomNumber = Math.random();
                            double probability = Double.parseDouble(PropertyLoader.loadProperties().getProperty("bookingsystems.bookingfailure"));
                            if (randomNumber > probability) {
                                handleRequest(inputLine);
                            } else {
                                System.out.println(ANSI_RED + "HotelBookingSystem (" + getName() + ") - crashed and did not process the message." + ANSI_RESET);
                            }
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

    /**
     * Handles the request to book or cancel a flight.
     *
     * @param booking String of with the format: "WhatAmI ProcessId Quantity".
     */
    public void handleRequest(String booking) {
        String[] splitMessage = booking.split(" ", 3);
        String processId = splitMessage[1];
        String whatAmI = splitMessage[0];
        int hotelNumber = port - hotelPortStart;
        boolean successful;
        int requestedRooms = Integer.parseInt(splitMessage[2]);
        double randomNumber = Math.random();
        double probability = Double.parseDouble(PropertyLoader.loadProperties().getProperty("bookingsystems.bookingnomessage"));

        if (whatAmI.equals("BookingRq")) {
            //Check if the request has already been processed
            if (bookingList.containsKey(processId)) {
                System.out.println(ANSI_YELLOW + "HotelBookingSystem (" + getName() + ") - Request Ignored Idempotency");
                if (randomNumber > probability) {
                    MessageSenderService.sendMessageToMessageBroker("Response " + processId + " " + bookingList.get(processId) + " hotel h" + hotelNumber + " " + requestedRooms);
                    return;
                } else {
                    System.out.println(ANSI_RED + "HotelBookingSystem (" + getName() + ") - processed the request but failed to send a response." + ANSI_RESET);
                    return;
                }
            }
            successful = book(requestedRooms, processId);
            if (randomNumber > probability) {
                MessageSenderService.sendMessageToMessageBroker("Response " + processId + " " + successful + " hotel h" + hotelNumber + " " + requestedRooms);
            } else {
                System.out.println(ANSI_RED + "HotelBookingSystem (" + getName() + ") - processed the request but failed to send a response." + ANSI_RESET);
            }
        } else if (whatAmI.equals("CancellationRq")) {
            if(bookingList.get(processId)){
            if (cancelList.contains(processId)) {
                System.out.println(ANSI_YELLOW + "HotelBookingSystem (" + getName() + ") - Request Ignored Idempotency");
                if (randomNumber > probability) {
                    MessageSenderService.sendMessageToMessageBroker("CancellationConfirmation " + processId + " true" + " hotel h" + hotelNumber + " " + requestedRooms);
                    return;
                } else {
                    System.out.println(ANSI_RED + "HotelBookingSystem (" + getName() + ") - processed the request but failed to send a response." + ANSI_RESET);
                    return;
                }
            }}else{
                MessageSenderService.sendMessageToMessageBroker("CancellationConfirmation " + processId + " true" + " flight f" + hotelNumber + " " + requestedRooms);
                return;
            }
            successful = cancel(requestedRooms, processId);
            if (randomNumber > probability) {
                MessageSenderService.sendMessageToMessageBroker("CancellationConfirmation " + processId + " " + successful + " hotel h" + hotelNumber + " " + requestedRooms);
            } else {
                System.out.println(ANSI_RED + "HotelBookingSystem (" + getName() + ") - processed the request but failed to send a response." + ANSI_RESET);
            }
        }
    }


    /**
     * Cancels and frees a specified number of rooms for a given process ID.
     *
     * @param requestedAmount the number of items to cancel.
     * @param processId       the unique identifier for the booking process.
     * @return {@code true} if the cancellation was successful,
     */
    @Override
    public synchronized boolean cancel(int requestedAmount, String processId) {
        rooms += requestedAmount;
        System.out.println(ANSI_YELLOW + "HotelBookingSystem(" + getName() + ") -  " + requestedAmount + " rooms freed. Remaining rooms: " + rooms + "." + ANSI_RESET);
        cancelList.add(processId);
        return true;
    }

    /**
     * Books a specified number of rooms for a given process ID.
     *
     * @param requestedAmount the number of items to book.
     * @param processId       the unique identifier for the booking process.
     * @return {@code true} if the booking was successful,
     */
    @Override
    public synchronized boolean book(int requestedAmount, String processId) {
        if (requestedAmount > rooms) {
            System.out.println(ANSI_YELLOW + "HotelBookingSystem(" + getName() + ") - " + requestedAmount + " rooms could not be booked. Remaining rooms: " + rooms + "." + ANSI_RESET);
            bookingList.put(processId, false);
            return false;
        }
        rooms -= requestedAmount;
        System.out.println(ANSI_YELLOW + "HotelBookingSystem(" + getName() + ") - " + requestedAmount + " rooms booked. Remaining rooms: " + rooms + "." + ANSI_RESET);
        bookingList.put(processId, true);
        return true;
    }

    /**
     * Retrieves the name of the {@link BookingSystem}.
     *
     * @return the name of the {@link BookingSystem}.
     */
    @Override
    public String getName() {
        return "h" + (port - hotelPortStart) + " - " + hotelList.get(this.port);
    }
}
