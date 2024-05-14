package booking;

public interface BookingSystem {

    boolean cancel(int requestedRooms, String processId);

    boolean book(int requestedRooms, String processId);

    String getName();

}
