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

/**
 * A class representing a flight-{@link BookingSystem}.
 */
public class FlightBookingSystem implements BookingSystem {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    private final int flightPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.port.start"));
    private final int minSeats = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.quantity.min"));
    private final int maxSeats = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.quantity.max"));
    private final int processingTime = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.processingtime"));
    private final int port;
    private final HashMap<Integer, String> airlineList;
    private final HashMap<String, Boolean> bookingList = new HashMap<>();
    private final List<String> cancelList = new ArrayList<>();
    private int seats = new Random().nextInt(minSeats, maxSeats);

    /**
     * Constructs a new {@link FlightBookingSystem} with the specified port and a list of names.
     *
     * @param port        the unique port of the {@link FlightBookingSystem}.
     * @param airlineList a {@link HashMap} containing the airline names.
     */
    public FlightBookingSystem(int port, HashMap<Integer, String> airlineList) {
        this.port = port;
        this.airlineList = airlineList;
    }

    /**
     * Starts the {@link FlightBookingSystem} with the specified {@code backlog} of the {@link ServerSocket}.
     *
     * @param backlog the maximum length of the queue of incoming connections.
     */
    public void start(int backlog) {
        try (ServerSocket serverSocket = new ServerSocket(port, backlog)) {
            System.out.println(ANSI_BLUE + "FlightBookingSystem(" + getName() + ") - running on port " + port + ANSI_RESET);
            while (true) {
                Socket flightSocket = serverSocket.accept();
                Thread flightThread = new Thread(() -> {
                    try {
                        Thread.sleep(processingTime);
                        BufferedReader in = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            //handle the request or "fails" the request with a given probability
                            System.out.println(ANSI_YELLOW + "FlightBookingSystem(" + getName() + ") -  Received message: " + inputLine + ANSI_RESET);
                            double randomNumber = Math.random();
                            double probability = Double.parseDouble(PropertyLoader.loadProperties().getProperty("bookingsystems.bookingfailure"));
                            if (randomNumber > probability) {
                                handleRequest(inputLine);
                            } else {
                                System.out.println(ANSI_RED + "FlightBookingSystem (" + getName() + ") - crashed and did not process the message." + ANSI_RESET);
                            }
                        }
                        in.close();
                        flightSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                flightThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the request to book or cancel a flight.
     *
     * @param booking String of with the format: <WhatAmI> <processId> <amount>
     */
    public void handleRequest(String booking) {
        String[] splitMessage = booking.split(" ", 3);
        String processId = splitMessage[1];
        String whatAmI = splitMessage[0];
        int airlineNumber = port - flightPortStart;
        boolean successful;
        int requestedSeats = Integer.parseInt(splitMessage[2]);
        double randomNumber = Math.random();
        double probability = Double.parseDouble(PropertyLoader.loadProperties().getProperty("bookingsystems.bookingnomessage"));

        if (whatAmI.equals("BookingRq")) {
            //Check if the request has already been processed
            if (bookingList.containsKey(processId)) {
                System.out.println(ANSI_YELLOW + "FlightBookingSystem (" + getName() + ") - Request Ignored Idempotency" + ANSI_RESET);
                if (randomNumber > probability) {
                    MessageSenderService.sendMessageToMessageBroker("Response " + processId + " " + bookingList.get(processId) + " flight f" + airlineNumber + " " + requestedSeats);
                    return;
                } else {
                    System.out.println(ANSI_RED + "FlightBookingSystem (" + getName() + ") - processed the request but failed to send a response." + ANSI_RESET);
                    return;
                }
            }
            successful = book(requestedSeats, processId);
            if (randomNumber > probability) {
                MessageSenderService.sendMessageToMessageBroker("Response " + processId + " " + successful + " flight f" + airlineNumber + " " + requestedSeats);
            } else {
                System.out.println(ANSI_RED + "FlightBookingSystem (" + getName() + ") - processed the request but failed to send a response." + ANSI_RESET);
            }
        } else if (whatAmI.equals("CancellationRq")) {
            if (cancelList.contains(processId)) {
                System.out.println(ANSI_YELLOW + "FlightBookingSystem (" + getName() + ") - Request Ignored Idempotency" + ANSI_RESET);
                if (randomNumber > probability) {
                    MessageSenderService.sendMessageToMessageBroker("CancellationConfirmation " + processId + " true" + " flight f" + airlineNumber + " " + requestedSeats);
                    return;
                } else {
                    System.out.println(ANSI_RED + "FlightBookingSystem (" + getName() + ") - processed the request but failed to send a response." + ANSI_RESET);
                    return;
                }
            }
            successful = cancel(requestedSeats, processId);
            if (randomNumber > probability) {
                MessageSenderService.sendMessageToMessageBroker("CancellationConfirmation " + processId + " " + successful + " flight f" + airlineNumber + " " + requestedSeats);
            } else {
                System.out.println(ANSI_RED + "FlightBookingSystem (" + getName() + ") - processed the request but failed to send a response." + ANSI_RESET);
            }
        }
    }

    /**
     * Cancels and frees a specified number of seats for a given process ID.
     *
     * @param requestedAmount the number of items to cancel.
     * @param processId       the unique identifier for the booking process.
     * @return {@code true} if the cancellation was successful,
     */
    @Override
    public synchronized boolean cancel(int requestedAmount, String processId) {
        seats += requestedAmount;
        System.out.println(ANSI_YELLOW + "FlightBookingSystem(" + getName() + ") - " + requestedAmount + " seats freed. Remaining seats: " + seats + "." + ANSI_RESET);
        cancelList.add(processId);
        return true;
    }

    /**
     * Books a specified number of seats for a given process ID.
     *
     * @param requestedAmount the number of items to book.
     * @param processId       the unique identifier for the booking process.
     * @return {@code true} if the booking was successful,
     */
    @Override
    public synchronized boolean book(int requestedAmount, String processId) {
        if (requestedAmount > seats) {
            System.out.println(ANSI_YELLOW + "FlightBookingSystem(" + getName() + ") - " + requestedAmount + " seats could not be booked. Remaining seats: " + seats + "." + ANSI_RESET);
            bookingList.put(processId, false);
            return false;
        }
        seats -= requestedAmount;
        System.out.println(ANSI_YELLOW + "FlightBookingSystem(" + getName() + ") - " + requestedAmount + " seats booked. Remaining seats: " + seats + "." + ANSI_RESET);
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
        return "f" + (port - flightPortStart) + " - " + airlineList.get(port);
    }
}
