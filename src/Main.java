import communications.Cli;
import communications.Client;
import communications.MessageBroker;
import communications.TravelBroker;
import misc.PropertyLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int testAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.test.amount"));
        int clientAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.client.amount"));
        int bookingRequestAmountMax = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.bookingrequest.amount.max"));
        int bookingRequestAmountMin = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.bookingrequest.amount.min"));
        int bookingHotelAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.hotel.amount"));
        int bookingFlightAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("bookingsystems.flight.amount"));
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
}