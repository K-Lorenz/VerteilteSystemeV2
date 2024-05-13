package booking;

public interface BookingSystem {

    boolean cancel(int requestedRooms);

    boolean book(int requestedRooms);

    String getName();

}
