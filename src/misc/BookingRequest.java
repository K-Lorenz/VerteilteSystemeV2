package misc;

import java.util.Objects;

/**
 * A class representing a booking request.
 */
public class BookingRequest {
    private final String type;
    private final String name;
    private int quantity;
    private int timesSent;
    private int cancellationsSent;
    private boolean isConfirmed;
    private boolean isRejected;
    private boolean isCanceled;
    private final int retries = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.retries"));
    private final int cancelRetries = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.cancelretries"));

    /**
     * Constructs a new {@link BookingRequest} with the specified type, name and quantity.
     *
     * @param type     the type of the booking request.
     * @param name     the name of the booking request.
     * @param quantity the quantity of the booking request.
     */
    public BookingRequest(String type, String name, int quantity) {
        this.type = type;
        this.name = name;
        this.quantity = quantity;
    }

    /**
     * Increments the counter of the sent messages.
     */
    public synchronized void sendMessage() {
        this.timesSent++;
    }

    /**
     * Increments the counter of the sent cancellations.
     */
    public synchronized void sendCancellation() {
        this.cancellationsSent++;
    }

    /**
     * Returns whether the booking request is failed. Meaning that the number of cancellations sent is greater than the number of specified cancellationRetries.
     *
     * @return {@code true} if the booking request is failed, {@code false} otherwise.
     */
    public synchronized boolean isFailed() {
        return cancellationsSent >= cancelRetries;
    }

    /**
     * Sets the {@link BookingRequest} as rejected.
     */
    public synchronized void reject() {
        this.isRejected = true;
    }

    /**
     * Returns whether the booking request is completed. Meaning that the booking request is confirmed, canceled or failed.
     *
     * @return {@code true} if the booking request is completed, {@code false} otherwise.
     */
    public synchronized boolean isCompleted() {
        return isConfirmed || isCanceled || isFailed();
    }

    /**
     * Returns whether the number of sent messages is greater than the number of specified retries.
     *
     * @return {@code true} if the number of sent messages is greater than the number of specified retries, {@code false} otherwise.
     */
    public synchronized boolean retriesExceeded() {
        return timesSent >= retries;
    }
    /**
     * Confirms the booking request.
     */
    public synchronized void confirm() {
        this.isConfirmed = true;
    }

    /**
     * Returns whether the booking request is confirmed.
     * @return {@code true} if the booking request is confirmed, {@code false} otherwise.
     */
    public synchronized boolean isConfirmed(){
        return isConfirmed;
    }
    /**
     * Confirms the cancellation of the booking request.
     */
    public synchronized void confirmCancel() {
        this.isCanceled = true;
    }

    /**
     * Returns whether the booking request is confirmed canceled.
     *
     * @return {@code true} if the booking request is confirmed canceled, {@code false} otherwise.
     */
    public synchronized boolean isCancelled() {
        return isCanceled || isFailed();
    }

    /**
     * Returns whether the booking request is confirmed.
     *
     * @param o the object to compare.
     * @return {@code true} if the booking request is equal, {@code false} otherwise.
     */
    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingRequest bookingRequest = (BookingRequest) o;
        return Objects.equals(type, bookingRequest.type) && Objects.equals(name, bookingRequest.name);
    }

    /**
     * Returns the hash code of the booking request.
     *
     * @return the hash code of the booking request.
     */
    @Override
    public synchronized int hashCode() {
        return Objects.hash(type, name);
    }

    public synchronized int getQuantity() {
        return this.quantity;
    }

    public synchronized void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public synchronized String getType() {
        return type;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized boolean isRejected() {
        return isRejected;
    }
}
