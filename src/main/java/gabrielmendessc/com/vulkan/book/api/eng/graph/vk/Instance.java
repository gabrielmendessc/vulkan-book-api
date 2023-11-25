package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRPortabilitySubset;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_FALSE;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;

public class Instance {

    public static final int MESSAGE_SEVERITY_BITMASK = VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
    public static final int MESSAGE_TYPE_BITMASK = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
    private static final String PORTABILITY_EXTENSION = "VK_KHR_portability_enumeration";
    @Getter
    private final VkInstance vkInstance;

    private VkDebugUtilsMessengerCreateInfoEXT debugUtils;
    private long vkDebugHandle;

    public Instance(boolean validate) {

        Logger.debug("Creating Vulkan instance");
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            ByteBuffer appShortName = memoryStack.UTF8("VulkanBook");
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(memoryStack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(appShortName)
                    .applicationVersion(1)
                    .pEngineName(appShortName)
                    .engineVersion(0)
                    .apiVersion(VK_API_VERSION_1_0);

            List<String> validationLayerList = getSupportedValidationLayers();
            boolean supportsValidation = validate;
            int numValidationLayers = validationLayerList.size();

            if (validate && validationLayerList.isEmpty()) {

                supportsValidation = false;
                Logger.warn("Validation was requested but no supported validation layers were found. Falling back to no validation");

            }
            Logger.debug("Validation {}", supportsValidation);

            PointerBuffer requiredLayers = null;
            if (supportsValidation) {

                requiredLayers = memoryStack.mallocPointer(numValidationLayers);
                for (int i = 0; i < numValidationLayers; i++) {

                    Logger.debug("Using validation layer [{}]", validationLayerList.get(i));
                    requiredLayers.put(i, memoryStack.ASCII(validationLayerList.get(i)));

                }

            }

            Set<String> instanceExtesionSet = getInstanceExtensions();
            PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (Objects.isNull(glfwExtensions)) {

                throw new RuntimeException("Failed to find the GLFW platform surface extensions");

            }

            PointerBuffer requiredExtension;
            boolean usePoratability = instanceExtesionSet.contains(PORTABILITY_EXTENSION) && VulkanUtils.getOS() == VulkanUtils.OSType.MACOS;
            if (supportsValidation) {

                ByteBuffer vkDebugUtilsExtensions = memoryStack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
                int numExtensions = usePoratability ? glfwExtensions.remaining() + 2 : glfwExtensions.remaining() + 1;
                requiredExtension = memoryStack.mallocPointer(numExtensions);
                requiredExtension.put(glfwExtensions).put(vkDebugUtilsExtensions);
                if (usePoratability) {
                    requiredExtension.put(memoryStack.UTF8(PORTABILITY_EXTENSION));
                }

            } else  {

                int numExtensions = usePoratability ? glfwExtensions.remaining() + 1 : glfwExtensions.remaining();
                requiredExtension = memoryStack.mallocPointer(numExtensions);
                requiredExtension.put(glfwExtensions);
                if (usePoratability) {
                    requiredExtension.put(memoryStack.UTF8(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME));
                }

            }

            requiredExtension.flip();

            long extension = MemoryUtil.NULL;
            if (supportsValidation) {
                debugUtils = createDebugCallBack();
                extension = debugUtils.address();
            }

            VkInstanceCreateInfo instanceInfo = VkInstanceCreateInfo.calloc(memoryStack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pNext(extension)
                    .pApplicationInfo(appInfo)
                    .ppEnabledLayerNames(requiredLayers)
                    .ppEnabledExtensionNames(requiredExtension);
            if (usePoratability) {
                instanceInfo.flags(0x00000001); // VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR
            }

            PointerBuffer pInstance = memoryStack.mallocPointer(1);
            VulkanUtils.vkCheck(vkCreateInstance(instanceInfo, null, pInstance), "Error creating instance");

            vkInstance = new VkInstance(pInstance.get(0), instanceInfo);

            vkDebugHandle = VK_NULL_HANDLE;
            if (supportsValidation) {
                LongBuffer longBuffer = memoryStack.mallocLong(1);
                VulkanUtils.vkCheck(vkCreateDebugUtilsMessengerEXT(vkInstance, debugUtils, null, longBuffer), "Error creating debug utils");
                vkDebugHandle = longBuffer.get(0);
            }

        }

    }

    public void cleanUp() {

        Logger.debug("Destroying Vulkan instace");
        if (vkDebugHandle != VK_NULL_HANDLE) {
            vkDestroyDebugUtilsMessengerEXT(vkInstance, vkDebugHandle, null);
        }
        if (Objects.nonNull(debugUtils)) {
            debugUtils.pfnUserCallback().free();
            debugUtils.free();
        }
        vkDestroyInstance(vkInstance, null);

    }

    private static VkDebugUtilsMessengerCreateInfoEXT createDebugCallBack() {

        return VkDebugUtilsMessengerCreateInfoEXT
                .calloc()
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(MESSAGE_SEVERITY_BITMASK)
                .messageType(MESSAGE_TYPE_BITMASK)
                .pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                    VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                    if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
                        Logger.info("VkDebugUtilsCallback, {}", callbackData.pMessageString());
                    } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                        Logger.warn("VkDebugUtilsCallback, {}", callbackData.pMessageString());
                    } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                        Logger.error("VkDebugUtilsCallback, {}", callbackData.pMessageString());
                    } else {
                        Logger.debug("VkDebugUtilsCallback, {}", callbackData.pMessageString());
                    }
                    return VK_FALSE;
                });

    }

    /*Validation Layers are optional components that adds extra operations to Vulkan's functions calls.
    * It's usually used for debugging operations, as:
    *   Tracking parameters and creations and destruction of objects;
    *   Thread-safety;
    *   Logs.
    * It's not needed for the application to run. It can be added in debug builds and removed from
    * release builds.*/
    private List<String> getSupportedValidationLayers() {

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            /*Firstly, we call the vkEnumerateInstanceLayerProperties to store in the intBuffer
            * the number of layers in the device.*/
            IntBuffer numLayerArray = memoryStack.callocInt(1);
            vkEnumerateInstanceLayerProperties(numLayerArray, null);

            int numLayers = numLayerArray.get(0);
            Logger.debug("Instance supports [{}] layers", numLayers);

            /*After getting the number of layers, we pass a property buffer to the function.
            * This buffer will store the properties, e.g. layer names.*/
            VkLayerProperties.Buffer propertiesBuffer = VkLayerProperties.calloc(numLayers, memoryStack);
            vkEnumerateInstanceLayerProperties(numLayerArray, propertiesBuffer);

            List<String> supportedLayerList = new ArrayList<>();
            for (int i = 0; i < numLayers; i++) {

                VkLayerProperties properties = propertiesBuffer.get(i);
                String layerName = properties.layerNameString();

                supportedLayerList.add(layerName);
                Logger.debug("Supported layer [{}]", layerName);

            }

            List<String> layersToUseList = new ArrayList<>();

            //Main validation layer
            if (supportedLayerList.contains("VK_LAYER_KHRONOS_validation")) {

                layersToUseList.add("VK_LAYER_KHRONOS_validation");
                return layersToUseList;

            }

            //Fallback 1
            if (supportedLayerList.contains("VK_LAYER_LUNARG_standard_validation")) {

                layersToUseList.add("VK_LAYER_LUNARG_standard_validation");
                return layersToUseList;

            }

            /*The validations added above were the main layer package. If they weren't
            * present, then we add the individual layers*/
            List<String> requestedLayerList = new ArrayList<>();
            requestedLayerList.add("VK_LAYER_GOOGLE_threading");
            requestedLayerList.add("VK_LAYER_LUNARG_parameter_validation");
            requestedLayerList.add("VK_LAYER_LUNARG_object_tracker");
            requestedLayerList.add("VK_LAYER_LUNARG_core_validation");

            return requestedLayerList.stream().filter(supportedLayerList::contains).toList();

        }

    }

    /*Extensions are additional features that extend the Vulkan API. The add new contest as:
    *   New structs;
    *   New functions;
    *   Alter the functionality of some API functions.*/
    private Set<String> getInstanceExtensions() {

        Set<String> instanceExtensionSet = new HashSet<>();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {


            /*Firstly, we call the vkEnumerateInstanceExtensionProperties to store in the intBuffer
             * the number of extensions in the device.*/
            IntBuffer numExtensionBuf = memoryStack.callocInt(1);
            vkEnumerateInstanceExtensionProperties((String) null, numExtensionBuf, null);
            int numExtensions = numExtensionBuf.get(0);
            Logger.debug("Instance supports [{}] extensions", numExtensions);

            /*After getting the number of extensions, we pass a property buffer to the function.
             * This buffer will store the properties, e.g. extensions names.*/
            VkExtensionProperties.Buffer instanceExtensionsProperties = VkExtensionProperties.calloc(numExtensions, memoryStack);
            vkEnumerateInstanceExtensionProperties((String) null, numExtensionBuf, instanceExtensionsProperties);
            for (int i = 0; i < numExtensions; i++) {

                VkExtensionProperties properties = instanceExtensionsProperties.get(i);
                String extensionName = properties.extensionNameString();

                instanceExtensionSet.add(extensionName);
                Logger.debug("Supported instance extension [{}]", extensionName);

            }

            return instanceExtensionSet;

        }

    }

}
