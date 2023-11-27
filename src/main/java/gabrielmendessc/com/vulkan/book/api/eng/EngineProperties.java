package gabrielmendessc.com.vulkan.book.api.eng;

import lombok.Getter;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class EngineProperties {

    private static final int DEFAULT_UPS = 30;
    private static final int DEFAULT_REQUESTED_IMAGES = 3;
    private static final String FILENAME = "eng.properties";
    private static EngineProperties instance;
    @Getter
    private int ups;
    @Getter
    private int requestedImages;
    @Getter
    private boolean validate;
    @Getter
    private boolean vSync;
    @Getter
    private String physDeviceName;

    private EngineProperties() {

        Properties properties = new Properties();

        try (InputStream inputStream = EngineProperties.class.getResourceAsStream("/".concat(FILENAME))) {

            properties.load(inputStream);

            ups = Integer.parseInt(properties.getOrDefault("ups", DEFAULT_UPS).toString());
            validate = Boolean.parseBoolean(properties.getOrDefault("vkValidate", false).toString());
            physDeviceName = properties.getProperty("physDeviceName");
            requestedImages = Integer.parseInt(properties.getOrDefault("requestedImages", DEFAULT_REQUESTED_IMAGES).toString());
            vSync = Boolean.parseBoolean(properties.getOrDefault("vsync", true).toString());

        } catch (IOException e) {

            Logger.error("Could not read [{}] properties file.", FILENAME, e);

        }

    }

    public static synchronized EngineProperties getInstance() {

        if (Objects.isNull(instance)) {

            instance = new EngineProperties();

        }

        return instance;

    }

}
