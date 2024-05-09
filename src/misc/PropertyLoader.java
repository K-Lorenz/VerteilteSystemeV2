package misc;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyLoader {
    public static void main(String[] args) {

    }
    public static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("src/misc/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
