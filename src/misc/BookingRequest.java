package misc;

import java.util.Objects;

public class BookingRequest{
    String type;
    String name;
    int quantity;
    public BookingRequest(String type, String name, int quantity){
        this.type = type;
        this.name = name;
        this.quantity = quantity;
    }
    @Override
    public  boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingRequest bookingRequest = (BookingRequest) o;
        return Objects.equals(type, bookingRequest.type) && Objects.equals(name, bookingRequest.name);
    }
    public void setQuantity(int quantity){
        this.quantity = quantity;
    }
    public int getQuantity(){
        return this.quantity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
