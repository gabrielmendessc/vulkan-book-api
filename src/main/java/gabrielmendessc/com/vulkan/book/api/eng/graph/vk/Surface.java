package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.tinylog.Logger;

import java.nio.LongBuffer;

public class Surface {

    private final PhysicalDevice physicalDevice;
    @Getter
    private final long vkSurface;

    public Surface(PhysicalDevice physicalDevice, long windowHandle) {

        Logger.debug("Creating Vulkan Surface for device [{}] and window [{}]", physicalDevice.getDeviceName(), windowHandle);
        this.physicalDevice = physicalDevice;

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            LongBuffer pSurface = memoryStack.mallocLong(1);
            GLFWVulkan.glfwCreateWindowSurface(this.physicalDevice.getVkPhysicalDevice().getInstance(), windowHandle, null, pSurface);
            vkSurface = pSurface.get(0);

        }

    }

    public void cleanUp() {

        Logger.debug("Destroying Vulkan Surface [{}]", vkSurface);
        KHRSurface.vkDestroySurfaceKHR(physicalDevice.getVkPhysicalDevice().getInstance(), vkSurface, null);

    }

}
