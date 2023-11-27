package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.tinylog.Logger;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK11.vkCreateDevice;
import static org.lwjgl.vulkan.VK11.vkDestroyDevice;
import static org.lwjgl.vulkan.VK11.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK11.vkEnumerateDeviceExtensionProperties;

@Getter
public class Device {

    private final PhysicalDevice physicalDevice;
    private final VkDevice vkDevice;

    public Device(PhysicalDevice physicalDevice) {

        Logger.debug("Creating logical device for [{}]", physicalDevice.getDeviceName());

        this.physicalDevice = physicalDevice;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            //Getting required extensions
            Set<String> deviceExtension = getDeviceExtensionSet();
            boolean usePortability = deviceExtension.contains(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME) && VKUtils.OSType.MACOS.equals(VKUtils.getOS());
            int numExtensions = usePortability ? 2 : 1;
            PointerBuffer requiredExtensions = memoryStack.mallocPointer(numExtensions);
            requiredExtensions.put(memoryStack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            if (usePortability) {
                requiredExtensions.put(memoryStack.ASCII(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME));
            }
            requiredExtensions.flip();

            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(memoryStack);

            //Enable all the queue families
            VkQueueFamilyProperties.Buffer queuePropertiesBuff = physicalDevice.getVkQueueFamilyProps();
            int numQueuesFamilies = queuePropertiesBuff.capacity();
            VkDeviceQueueCreateInfo.Buffer queueCreationInfoBuff = VkDeviceQueueCreateInfo.calloc(numQueuesFamilies, memoryStack);
            for (int i = 0; i < numQueuesFamilies; i++) {
                FloatBuffer priorities = memoryStack.callocFloat(queuePropertiesBuff.get(i).queueCount());
                queueCreationInfoBuff.get(i)
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(i)
                        .pQueuePriorities(priorities);
            }

            VkDeviceCreateInfo vkDeviceCreateInfo = VkDeviceCreateInfo.calloc(memoryStack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .ppEnabledExtensionNames(requiredExtensions)
                    .pEnabledFeatures(features)
                    .pQueueCreateInfos(queueCreationInfoBuff);
            PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
            VKUtils.vkCheck(vkCreateDevice(physicalDevice.getVkPhysicalDevice(), vkDeviceCreateInfo, null, pointerBuffer), "Failed to create device");
            vkDevice = new VkDevice(pointerBuffer.get(0), physicalDevice.getVkPhysicalDevice(), vkDeviceCreateInfo);

        }

    }

    public void cleanUp() {
        Logger.debug("Destroying Vulkan Device");
        vkDestroyDevice(vkDevice, null);
    }

    public void waitIdle() {

        vkDeviceWaitIdle(vkDevice);

    }

    private Set<String> getDeviceExtensionSet() {

        Set<String> deviceExtensionSet = new HashSet<>();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            IntBuffer numExtensionsBuff = memoryStack.callocInt(1);
            vkEnumerateDeviceExtensionProperties(physicalDevice.getVkPhysicalDevice(), (String) null, numExtensionsBuff, null);
            int numExtensions = numExtensionsBuff.get(0);
            Logger.debug("Device supports [{}] extensions", numExtensions);

            VkExtensionProperties.Buffer propertiesBuff = VkExtensionProperties.calloc(numExtensions, memoryStack);
            vkEnumerateDeviceExtensionProperties(physicalDevice.getVkPhysicalDevice(), (String) null, numExtensionsBuff, propertiesBuff);
            for (int i = 0; i < numExtensions; i++) {

                VkExtensionProperties properties = propertiesBuff.get(i);
                String extensionName = properties.extensionNameString();
                deviceExtensionSet.add(extensionName);
                Logger.debug("Supported device extension [{}]", extensionName);

            }

        }

        return deviceExtensionSet;

    }

}
