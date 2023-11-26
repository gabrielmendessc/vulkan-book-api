package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.tinylog.Logger;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.vulkan.VK11.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK11.vkEnumerateDeviceExtensionProperties;
import static org.lwjgl.vulkan.VK11.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceMemoryProperties;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceProperties;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceQueueFamilyProperties;

@Getter
public class PhysicalDevice {

    private final VkExtensionProperties.Buffer vkDeviceExtensions;
    private final VkPhysicalDeviceMemoryProperties vkMemoryProperties;
    private final VkPhysicalDevice vkPhysicalDevice;
    private final VkPhysicalDeviceFeatures vkPhysicalDeviceFeatures;
    private final VkPhysicalDeviceProperties vkPhysicalDeviceProperties;
    private final VkQueueFamilyProperties.Buffer vkQueueFamilyProps;

    private PhysicalDevice(VkPhysicalDevice vkPhysicalDevice) {

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            this.vkPhysicalDevice = vkPhysicalDevice;
            IntBuffer intBuffer = memoryStack.mallocInt(1);

            //Device Properties
            vkPhysicalDeviceProperties  = VkPhysicalDeviceProperties.calloc();
            vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties);

            //Device Extensions
            VulkanUtils.vkCheck(vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, (String) null, intBuffer, null),
                    "Failed to get number of device extension properties");
            vkDeviceExtensions = VkExtensionProperties.calloc(intBuffer.get(0));
            VulkanUtils.vkCheck(vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, (String) null, intBuffer, vkDeviceExtensions),
                    "Failed to get extension properties");

            //Queue Family Properties
            vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, intBuffer, null);
            vkQueueFamilyProps = VkQueueFamilyProperties.calloc(intBuffer.get(0));
            vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, intBuffer, vkQueueFamilyProps);

            //Device Properties
            vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc();
            vkGetPhysicalDeviceFeatures(vkPhysicalDevice, vkPhysicalDeviceFeatures);

            //Memory Properties
            vkMemoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
            vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, vkMemoryProperties);

        }

    }

    public static PhysicalDevice createPhysicalDevice(Instance instance, String prefferredDeviceName) {

        Logger.debug("Selecting physical device");
        PhysicalDevice selectedPhysicalDevice = null;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            PointerBuffer pPhysicalDevices = getPhysicalDevices(instance, memoryStack);
            int numDevices = pPhysicalDevices.capacity();
            if (numDevices <= 0) {
                throw new RuntimeException("No physical devices found");
            }

            List<PhysicalDevice> physicalDeviceList = new ArrayList<>();
            for (int i = 0; i < numDevices; i++) {

                VkPhysicalDevice vkPhysicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(i), instance.getVkInstance());
                PhysicalDevice physicalDevice = new PhysicalDevice(vkPhysicalDevice);

                String deviceName = physicalDevice.getDeviceName();
                if (physicalDevice.hasGraphicsQueueFamily() && physicalDevice.hasKHRSwapChainExtension()) {
                    Logger.debug("Device [{}] supports required extensions", deviceName);
                    if (Objects.nonNull(prefferredDeviceName) && prefferredDeviceName.equals(deviceName)) {
                        selectedPhysicalDevice = physicalDevice;
                        break;
                    }

                    physicalDeviceList.add(physicalDevice);

                } else {

                    Logger.debug("Device [{}] does not support required extensions", deviceName);
                    physicalDevice.cleanUp();

                }

            }

            selectedPhysicalDevice = Objects.isNull(selectedPhysicalDevice) && !physicalDeviceList.isEmpty() ? physicalDeviceList.remove(0) : selectedPhysicalDevice;
            physicalDeviceList.forEach(PhysicalDevice::cleanUp);

            if (Objects.isNull(selectedPhysicalDevice)) {

                throw new RuntimeException("No suitable physical device found");

            }

            Logger.debug("Selected device: [{}]", selectedPhysicalDevice.getDeviceName());

        }

        return selectedPhysicalDevice;

    }

    public String getDeviceName() {
        return vkPhysicalDeviceProperties.deviceNameString();
    }

    public void cleanUp() {

        Logger.debug("Destroying physical device [{}]", vkPhysicalDeviceProperties.deviceNameString());
        vkMemoryProperties.free();
        vkPhysicalDeviceFeatures.free();
        vkQueueFamilyProps.free();
        vkDeviceExtensions.free();
        vkPhysicalDeviceProperties.free();

    }

    protected static PointerBuffer getPhysicalDevices(Instance instance, MemoryStack memoryStack) {

        PointerBuffer pPhysicalDevices;
        IntBuffer intBuffer = memoryStack.mallocInt(1);
        VulkanUtils.vkCheck(vkEnumeratePhysicalDevices(instance.getVkInstance(), intBuffer, null), "Failed to get number of physical devices");
        int numDevices = intBuffer.get(0);
        Logger.debug("Detected {} physical device(s)", numDevices);

        pPhysicalDevices = memoryStack.mallocPointer(numDevices);
        VulkanUtils.vkCheck(vkEnumeratePhysicalDevices(instance.getVkInstance(), intBuffer, pPhysicalDevices), "Failed to get physical devices");

        return pPhysicalDevices;

    }

    private boolean hasKHRSwapChainExtension() {

        int numExtensions = vkDeviceExtensions != null ? vkDeviceExtensions.capacity() : 0;
        for (int i = 0; i < numExtensions; i++) {

            String extensionName = vkDeviceExtensions.get(i).extensionNameString();

            if (KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(extensionName)) {

                return true;

            }

        }

        return false;

    }

    private boolean hasGraphicsQueueFamily() {

        int numQueueFamilies = Objects.nonNull(vkQueueFamilyProps) ? vkQueueFamilyProps.capacity() : 0;
        for (int i = 0; i < numQueueFamilies; i++) {

            VkQueueFamilyProperties familyProperties = vkQueueFamilyProps.get(i);
            if ((familyProperties.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {

                return true;

            }

        }

        return false;

    }

}
