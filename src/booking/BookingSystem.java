package booking;

public interface BookingSystem {
    boolean checkAvailability(String bookingDetails);
    boolean reserveBooking(String bookingDetails);
    boolean freeReservation(String reservationDetails);
    boolean finalizeBooking(String reservation);
    String getSystemName();
}
