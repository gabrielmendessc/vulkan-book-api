package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.tinylog.Logger;

import static org.lwjgl.vulkan.VK11.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK11.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK11.vkQueueWaitIdle;

@Getter
public class Queue {

    private final VkQueue vkQueue;

    public Queue(Device device, int queueFamilyIndex, int queueIndex) {

        Logger.debug("Creating Queue for device [{}], at family [{}] with index [{}]", device.getPhysicalDevice().getDeviceName(), queueFamilyIndex, queueIndex);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            PointerBuffer pQueue = memoryStack.mallocPointer(1);
            vkGetDeviceQueue(device.getVkDevice(), queueFamilyIndex, queueIndex, pQueue);
            long queue = pQueue.get(0);
            vkQueue = new VkQueue(queue, device.getVkDevice());

        }

    }

    public void waitIdle() {

        vkQueueWaitIdle(vkQueue);

    }

    public static class GraphicsQueue extends Queue {

        public GraphicsQueue(Device device, int queueIndex) {

            super(device, getGraphicsQueueFamilyIndex(device), queueIndex);

        }

        private static int getGraphicsQueueFamilyIndex(Device device) {

            int index = -1;
            PhysicalDevice physicalDevice = device.getPhysicalDevice();
            VkQueueFamilyProperties.Buffer queueFamilyPropBuff = physicalDevice.getVkQueueFamilyProps();
            int numQueuesFamilies = queueFamilyPropBuff.capacity();
            for (int i = 0; i < numQueuesFamilies; i++) {

                VkQueueFamilyProperties vkQueueFamilyProperties = queueFamilyPropBuff.get(i);
                boolean graphicsQueue = (vkQueueFamilyProperties.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0;
                if (graphicsQueue) {
                    index = i;
                    break;
                }

            }

            if (index < 0) {
                throw new RuntimeException("Failed to get Graphics Queue Family index");
            }

            return index;

        }

    }

}
