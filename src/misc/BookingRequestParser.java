package misc;

import java.util.ArrayList;
import java.util.List;
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
            bookings.add(new BookingRequest(type, name, quantity));
        }
        return bookings;
    }
}
