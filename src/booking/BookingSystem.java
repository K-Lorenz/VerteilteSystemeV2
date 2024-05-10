package booking;

public interface BookingSystem {

    boolean cancel(String cancellation);
    boolean book(String booking);

    //Generate/Get Name of Airline/Hotelchain
    String getSystemName();
}
