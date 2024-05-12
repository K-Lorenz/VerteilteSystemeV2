import booking.FlightBookingSystem;
import booking.HotelBookingSystem;
import communications.Client;
import communications.MessageBroker;
import communications.TravelBroker;
import misc.PropertyLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int testAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.test.amount"));
        int clientAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.client.amount"));
        int bookingRequestAmountMax = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.bookingrequest.amount.max"));
        int bookingRequestAmountMin = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.bookingrequest.amount.min"));
        int bookingHotelAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.amount"));
        int bookingFlightAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.amount"));
        int bookingHotelPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.port.start"));
        int bookingFlightPortStart = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.port.start"));

        List<String> testingInputs = new ArrayList<>();
        for (int i = 0; i<testAmount; i++){
            testingInputs.add("book "+ randomString(bookingRequestAmountMin, bookingRequestAmountMax, bookingHotelAmount, bookingFlightAmount));
        }
        Thread mBThread = new Thread(()->{
            MessageBroker messageBroker = new MessageBroker();
            messageBroker.start(clientAmount*50);
        });
        Thread tBThread = new Thread(()->{
            TravelBroker travelBroker = new TravelBroker();
            travelBroker.start(clientAmount*50);
        });

        tBThread.start();
        mBThread.start();
        for(int i = 0; i<clientAmount; i++){
            Thread clThread = new Thread(()->{
                Client client = new Client();
                client.start(testingInputs);
            });
            clThread.start();
        }
        HashMap<Integer, String> hotelNames = setHotelNames(bookingHotelAmount, bookingHotelPortStart);
        for(int i = 0; i<bookingHotelAmount; i++){
            int finalI = i;
            Thread bHThread = new Thread(()->{
                HotelBookingSystem hotelBookingSystem = new HotelBookingSystem(bookingHotelPortStart + finalI, hotelNames);
                hotelBookingSystem.start(clientAmount*50);
            });
            bHThread.start();
        }
        HashMap<Integer, String> airlineNames = setAirlineNames(bookingFlightAmount, bookingFlightPortStart);
        for(int i = 0; i<bookingFlightAmount; i++){
            int finalI = i;
            Thread bFThread = new Thread(()->{
                FlightBookingSystem flightBookingSystem = new FlightBookingSystem(bookingFlightPortStart + finalI, airlineNames);
                flightBookingSystem.start(clientAmount*50);
            });
            bFThread.start();
        }

    }
    public static String randomString(int min, int max, int bookingHotelAmount, int bookingFlightAmount){
        Random rn = new Random();
        int paramAmount = rn.nextInt(min, max);
        StringBuilder builderString = new StringBuilder();
        for(int i = 0; i<paramAmount; i++){
            boolean type = rn.nextBoolean();
            if(type){
                builderString.append("--flight 'f").append(rn.nextInt(0, bookingFlightAmount-1));
            }else{
                builderString.append("--hotel 'h").append(rn.nextInt(0, bookingHotelAmount-1));
            }
            builderString.append("' ").append(rn.nextInt(1, 10)).append(" ");
        }
        return  builderString.toString();
    }

    public static HashMap<Integer, String> setAirlineNames(int bookingFlightAmount, int bookingFlightPortStart){
        HashMap<Integer, String> airlineList = new HashMap<>();
        final String[] airLineNames = {"Zaun Airways", "Demacia Airways", "Air Piltover", "Fly Freljord", "Air Noxus", "Ionia Air", "Bandle Airways", "Shurima Skyline", "Bilgewater Airways", "Fly Void"};
        for (int i = bookingFlightPortStart; i < bookingFlightAmount + bookingFlightPortStart; i++) {
            airlineList.put(i, airLineNames[new Random().nextInt(airLineNames.length)]);
        }
        return airlineList;
    }
    public static HashMap<Integer, String> setHotelNames(int bookingHotelAmount, int bookingHotelPortStart){
        final HashMap<Integer, String> hotelList = new HashMap<>();
        final String[] hotelNames = {"Schachtelwirt", "Hotel zur Kluft", "Gasthof zum Löwen", "Hotel zur Post", "Hotel zur Sonne", "Hotel zum Bären", "Hotel zum Hirschen", "Hotel zum Ochsen", "Hotel zum Schwan", "Hotel zum Stern", "Hotel zum Storchen", "Hotel zum Taunus", "Hotel zum Turm", "Hotel zum weißen Ross", "Hotel zum weißen Schwan", "Hotel zur alten Post", "Hotel zur alten Schule", "Hotel zur alten Stadtmauer"};
        for (int i = bookingHotelPortStart; i < bookingHotelAmount + bookingHotelPortStart; i++) {
            hotelList.put(i, hotelNames[new Random().nextInt(hotelNames.length)]);
        }
        return hotelList;
    }
}