package booking;

import misc.BookingRequest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a booking with a unique process ID and a list of booking requests.
 * This class is a record, providing a compact syntax for declaring data carrier classes.
 */
public record Booking(UUID processID, List<BookingRequest> requests) {

    /**
     * Constructs a new {@link Booking} with the specified process ID and list of {@link BookingRequest}.
     * The list of booking requests is wrapped in a {@link CopyOnWriteArrayList} to ensure thread safety.
     *
     * @param processID the unique identifier for this booking process.
     * @param requests  the list of {@link BookingRequest} associated with this booking.
     */
    public Booking(UUID processID, List<BookingRequest> requests) {
        this.processID = processID;
        this.requests = new CopyOnWriteArrayList<>(requests);
    }

    /**
     * Checks if the booking is in the process of being cancelled.
     * A booking is considered to be cancelling if any of its requests are rejected
     * or have exceeded their retry limit.
     *
     * @return {@code true} if any booking request is rejected or retries exceeded,
     * {@code false} otherwise.
     */
    public boolean isCancelling() {
        synchronized (requests) {
            for (BookingRequest request : requests) {
                if (request.isRejected() || request.retriesExceeded()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the booking is successful.
     * A booking is considered successful if none of its {@link BookingRequest} are {@link BookingRequest#isFailed()},{@link BookingRequest#isRejected()}, or {@link BookingRequest#isCancelled()}.
     *
     * @return {@code true} if all {@link BookingRequest} are successful,
     * {@code false} if at least 1 {@link BookingRequest} fails, is rejected, or is cancelled.
     */
    public boolean isSuccessful() {
        synchronized (requests) {
            for (BookingRequest request : requests) {
                if (request.isFailed() || request.isRejected() || request.isCancelled()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retrieves a list of uncompleted booking requests.
     * A booking request is considered uncompleted if {@link BookingRequest#isCompleted()} returns {@code false}.
     *
     * @return a list of uncompleted {@link BookingRequest}.
     */
    public List<BookingRequest> uncompletedRequests() {
        List<BookingRequest> uncompleted = new CopyOnWriteArrayList<>(requests);
        uncompleted.removeIf(BookingRequest::isCompleted);
        return uncompleted;
    }
}