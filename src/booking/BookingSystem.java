package booking;

public interface BookingSystem {

    boolean cancel(int requestedRooms);

    boolean book(int requestedRooms);

    //Generate/Get Name of Airline/Hotelchain
    String getName();
}
