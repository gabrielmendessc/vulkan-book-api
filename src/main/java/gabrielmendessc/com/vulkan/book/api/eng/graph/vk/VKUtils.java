package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import java.util.Locale;

import static org.lwjgl.vulkan.VK11.VK_SUCCESS;

public class VKUtils {

    public static OSType getOS() {

        OSType result;
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((os.contains("mac")) || (os.contains("darwin"))) {
            result = OSType.MACOS;
        } else if (os.contains("win")) {
            result = OSType.WINDOWS;
        } else if (os.contains("nux")) {
            result = OSType.LINUX;
        } else {
            result = OSType.OTHER;
        }

        return result;

    }

    public static void vkCheck(int err, String errMsg) {
        if (err != VK_SUCCESS) {
            throw new RuntimeException(errMsg + ": " + err);
        }
    }

    public enum OSType {WINDOWS, MACOS, LINUX, OTHER}

}
