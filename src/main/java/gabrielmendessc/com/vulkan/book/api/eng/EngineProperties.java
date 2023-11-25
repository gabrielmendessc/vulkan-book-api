package gabrielmendessc.com.vulkan.book.api.eng;

import lombok.Getter;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class EngineProperties {

    private static final int DEFAULT_UPS = 30;
    private static final String FILENAME = "eng.properties";
    private static EngineProperties instance;
    @Getter
    private int ups;

    private EngineProperties() {

        Properties properties = new Properties();

        try (InputStream inputStream = EngineProperties.class.getResourceAsStream("/".concat(FILENAME))) {

            properties.load(inputStream);

            ups = Integer.parseInt(properties.getOrDefault("ups", DEFAULT_UPS).toString());

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
