package misc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookingRequestParser {
    public static List<BookingRequest> parse(String request){
        List<BookingRequest> bookings = new ArrayList<>();

        //match the input
        Pattern pattern = Pattern.compile("--(hotel|flight) '(.*?)' (\\d+)");
        Matcher matcher = pattern.matcher(request);
        while(matcher.find()){
            String type = matcher.group(1);
            String name = matcher.group(2);
            int quantity = Integer.parseInt(matcher.group(3));
            BookingRequest booking = new BookingRequest(type, name, quantity);
            if(quantity < 1){
                continue;
            }
            AtomicInteger index = new AtomicInteger();
            if(bookings.stream().anyMatch(obj ->{
                index.set(bookings.indexOf(obj));
                return obj.equals(booking);
            })){
                bookings.get(index.get()).setQuantity(bookings.get(index.get()).getQuantity() + booking.getQuantity());
                continue;
            }
            bookings.add(booking);
        }
        return bookings;
    }
}