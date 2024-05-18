package misc;

import java.util.Objects;

public class BookingRequest {
    private final String type;
    private final String name;
    private int quantity;
    private int timesSent;
    private int cancellationsSent;
    private boolean isConfirmed;
    private boolean isRejected;
    private boolean isCanceled;
    private int retries = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.retries"));
    private int cancelRetries = Integer.parseInt(PropertyLoader.loadProperties().getProperty("travelbroker.cancelretries"));

    public BookingRequest(String type, String name, int quantity) {
        this.type = type;
        this.name = name;
        this.quantity = quantity;
    }

    public synchronized void sendMessage() {
        this.timesSent++;
    }

    public synchronized void sendCancellation() {
        this.cancellationsSent++;
    }

    public synchronized boolean isFailed() {
        return cancellationsSent >= cancelRetries;
    }

    public synchronized void reject() {
        this.isRejected = true;
    }

    public synchronized boolean isCompleted() {
        return isConfirmed || isCanceled || isFailed();
    }

    public synchronized boolean retriesExceeded() {
        return timesSent >= retries;
    }

    public synchronized void confirm() {
        this.isConfirmed = true;
    }

    public synchronized void confirmCancel() {
        this.isCanceled = true;
    }
    public synchronized boolean isCanceled() {
        return isCanceled;
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingRequest bookingRequest = (BookingRequest) o;
        return Objects.equals(type, bookingRequest.type) && Objects.equals(name, bookingRequest.name);
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(type, name);
    }

    public synchronized void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public synchronized int getQuantity() {
        return this.quantity;
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
