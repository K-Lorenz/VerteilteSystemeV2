package booking;

/**
 * Interface representing a booking system that allows for booking and canceling of items.
 */
public interface BookingSystem {

    /**
     * Attempts to cancel a specified number of items for a given process ID.
     *
     * @param requestedAmount the number of items to cancel.
     * @param processId       the unique identifier for the booking process.
     * @return {@code true} if the cancellation was successful,
     * {@code false} otherwise.
     */
    boolean cancel(int requestedAmount, String processId);

    /**
     * Attempts to book a specified number of items for a given process ID.
     *
     * @param requestedAmount the number of items to book.
     * @param processId       the unique identifier for the booking process.
     * @return {@code true} if the booking was successful,
     * {@code false} otherwise.
     */
    boolean book(int requestedAmount, String processId);

    /**
     * Retrieves the name of the booking system.
     *
     * @return the name of the booking system.
     */
    String getName();
}