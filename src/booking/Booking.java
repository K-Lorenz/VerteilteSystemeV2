package booking;

import misc.BookingRequest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public record Booking(UUID processID, List<BookingRequest> requests) {
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
    public boolean isSuccessful(){
        synchronized (requests) {
            for (BookingRequest request : requests) {
                if (request.isFailed() || request.isRejected() || request.isCanceled()) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<BookingRequest> uncompletedRequests() {
        List<BookingRequest> uncompleted = new CopyOnWriteArrayList<>(requests);
        uncompleted.removeIf(BookingRequest::isCompleted);
        return uncompleted;
    }

    public Booking(UUID processID, List<BookingRequest> requests) {
        this.processID = processID;
        this.requests = new CopyOnWriteArrayList<>(requests);
    }
}
