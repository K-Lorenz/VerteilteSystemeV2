package booking;

public class HotelBookingSystem implements BookingSystem {
    @Override
    public boolean checkAvailability(String bookingDetails) {
        return false;
    }

    @Override
    public boolean reserveBooking(String bookingDetails) {
        return false;
    }

    @Override
    public boolean freeReservation(String reservationDetails) {
        return false;
    }

    @Override
    public boolean finalizeBooking(String reservation) {
        return false;
    }

    @Override
    public String getSystemName() {
        return "HotelBookingSystem";
    }
}
