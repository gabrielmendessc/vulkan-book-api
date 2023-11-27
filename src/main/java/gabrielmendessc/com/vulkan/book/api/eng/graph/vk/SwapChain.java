package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import gabrielmendessc.com.vulkan.book.api.eng.Window;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.tinylog.Logger;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK11.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK11.VK_SHARING_MODE_EXCLUSIVE;

public class SwapChain {

    private final Device device;
    private final ImageView[] imageViewArray;
    private final SurfaceFormat surfaceFormat;
    private final VkExtent2D swapChainExtent;
    private final long vkSwapChain;

    public SwapChain(Device device, Surface surface, Window window, int requestedImages, boolean vsync) {

        Logger.debug("Creating Vulkan SwapChain");
        this.device = device;

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            PhysicalDevice physicalDevice = this.device.getPhysicalDevice();

            //Get surface capabilities
            VkSurfaceCapabilitiesKHR surfCapabilities = VkSurfaceCapabilitiesKHR.calloc(memoryStack);
            VKUtils.vkCheck(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(this.device.getVkDevice().getPhysicalDevice(), surface.getVkSurface(), surfCapabilities),
                    "Failed to get surface capabilities");

            int numImages = calcNumImages(surfCapabilities, requestedImages);
            surfaceFormat = calcSurfaceFormat(physicalDevice, surface);
            swapChainExtent = calcSwapChainExtent(window, surfCapabilities);

            VkSwapchainCreateInfoKHR vkSwapchainCreateInfoKHR = VkSwapchainCreateInfoKHR.calloc(memoryStack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface.getVkSurface())
                    .minImageCount(numImages)
                    .imageFormat(surfaceFormat.imageFormat())
                    .imageColorSpace(surfaceFormat.colorSpace())
                    .imageExtent(swapChainExtent)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .preTransform(surfCapabilities.currentTransform())
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .clipped(true);
            if (vsync) {
                vkSwapchainCreateInfoKHR.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR);
            } else {
                vkSwapchainCreateInfoKHR.presentMode(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR);
            }

            //TODO - Validation error vkCreateFramebuffer(): pCreateInfo->renderPass is VK_NULL_HANDLE after createSwapchainKHR
            LongBuffer longBuffer = memoryStack.mallocLong(1);
            VKUtils.vkCheck(KHRSwapchain.vkCreateSwapchainKHR(device.getVkDevice(), vkSwapchainCreateInfoKHR, null, longBuffer), "Failed to get swap chain");

            vkSwapChain = longBuffer.get(0);
            imageViewArray = createImageViewArray(memoryStack, device, vkSwapChain, surfaceFormat.imageFormat);

        }

    }

    public record SurfaceFormat(int imageFormat, int colorSpace) {}

    public void cleanUp() {

        Logger.debug("Destroying Vulkan SwapChain");
        swapChainExtent.free();
        for (int i = 0; i < imageViewArray.length; i++) {
            imageViewArray[i].cleanUp();
        }
        KHRSwapchain.vkDestroySwapchainKHR(device.getVkDevice(), vkSwapChain, null);

    }

    private int calcNumImages(VkSurfaceCapabilitiesKHR surfCapabilities, int requestedImages) {

        int maxImages = surfCapabilities.maxImageCount();
        int minImages = surfCapabilities.minImageCount();
        int result = minImages;
        if (maxImages != 0) {
            result = Math.min(requestedImages, maxImages);
        }
        result = Math.max(result, minImages);
        Logger.debug("Requested [{}] images, got [{}] images. Surface capabilities, maxImages: [{}], minImages: [{}]", requestedImages, result, maxImages, minImages);

        return result;

    }

    private SurfaceFormat calcSurfaceFormat(PhysicalDevice physicalDevice, Surface surface) {

        int imageFormat;
        int colorSpace;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            IntBuffer intBuffer = memoryStack.mallocInt(1);
            VKUtils.vkCheck(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getVkPhysicalDevice(), surface.getVkSurface(), intBuffer, null),
                    "Failed to get the number of surface formats");
            int numFormats = intBuffer.get(0);
            if (numFormats <= 0) {
                throw new RuntimeException("No surface formats retrieved");
            }

            VkSurfaceFormatKHR.Buffer surfaceFormats = VkSurfaceFormatKHR.calloc(numFormats, memoryStack);
            VKUtils.vkCheck(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getVkPhysicalDevice(), surface.getVkSurface(), intBuffer, surfaceFormats),
                    "Failed to get surface formats");

            imageFormat = VK_FORMAT_B8G8R8A8_SRGB;
            colorSpace = surfaceFormats.get(0).colorSpace();
            for (int i = 0; i < numFormats; i++) {

                VkSurfaceFormatKHR surfaceFormatKHR = surfaceFormats.get(i);
                if (surfaceFormatKHR.format() == VK_FORMAT_B8G8R8A8_SRGB && surfaceFormatKHR.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    imageFormat = surfaceFormatKHR.format();
                    colorSpace = surfaceFormats.colorSpace();
                    break;
                }

            }

        }

        return new SurfaceFormat(imageFormat, colorSpace);

    }

    private VkExtent2D calcSwapChainExtent(Window window, VkSurfaceCapabilitiesKHR surfCapabilities) {

        VkExtent2D result = VkExtent2D.calloc();
        if (surfCapabilities.currentExtent().width() == 0xFFFFFFFF) {

            // Surface size undefined. Set to the window size if within bounds
            int width = Math.min(window.getWidth(), surfCapabilities.maxImageExtent().width());
            width = Math.max(width, surfCapabilities.minImageExtent().width());

            int height = Math.min(window.getHeight(), surfCapabilities.maxImageExtent().height());
            height = Math.max(height, surfCapabilities.minImageExtent().height());

            result.width(width);
            result.height(height);

        } else {

            result.set(surfCapabilities.currentExtent());

        }

        return result;

    }

    private ImageView[] createImageViewArray(MemoryStack memoryStack, Device device, long vkSwapChain, int format) {

        ImageView[] result;

        IntBuffer intBuffer = memoryStack.mallocInt(1);
        VKUtils.vkCheck(KHRSwapchain.vkGetSwapchainImagesKHR(device.getVkDevice(), vkSwapChain, intBuffer, null), "Failed to get number of surface images");
        int numImages = intBuffer.get(0);

        LongBuffer swapChainImages = memoryStack.mallocLong(numImages);
        VKUtils.vkCheck(KHRSwapchain.vkGetSwapchainImagesKHR(device.getVkDevice(), vkSwapChain, intBuffer, swapChainImages), "Failed to get surface images");

        result = new ImageView[numImages];
        ImageView.ImageViewData imageViewData = new ImageView.ImageViewData().format(format).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        for (int i = 0; i < numImages; i++) {

            result[i] = new ImageView(device, swapChainImages.get(i), imageViewData);

        }

        return result;

    }

}
