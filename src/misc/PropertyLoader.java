package misc;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A class representing a property loader.
 */
public class PropertyLoader {
    /**
     * Loads the properties from the specified file.
     *
     * @return the properties loaded from the file.
     */
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
