package booking;

public class HotelBookingSystem implements BookingSystem {
    @Override
    public boolean cancel(String cancellation) {
        return false;
    }

    @Override
    public boolean book(String booking) {
        return false;
    }

    @Override
    public String getSystemName() {
        return null;
    }
}
