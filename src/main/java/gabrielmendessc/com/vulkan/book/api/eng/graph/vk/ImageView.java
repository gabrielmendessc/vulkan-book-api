package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.vkDestroyImageView;
import static org.lwjgl.vulkan.VK11.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK11.vkCreateImageView;

public class ImageView {

    private final int aspectMask;
    private final Device device;
    @Getter
    private final long vkImageView;
    private final int mipLevels;

    public ImageView(Device device, long vkImageView, ImageViewData imageViewData) {

        this.device = device;
        this.aspectMask = imageViewData.aspectMask;
        this.mipLevels = imageViewData.mipLevels;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            LongBuffer longBuffer = memoryStack.mallocLong(1);
            VkImageViewCreateInfo vkImageViewCreateInfo = VkImageViewCreateInfo.calloc(memoryStack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(vkImageView)
                    .format(imageViewData.format)
                    .viewType(imageViewData.viewType)
                    .format(imageViewData.format)
                    .subresourceRange(it -> it
                            .aspectMask(aspectMask)
                            .baseMipLevel(0)
                            .levelCount(mipLevels)
                            .baseArrayLayer(imageViewData.baseArrayLayer)
                            .layerCount(imageViewData.layerCount));

            VKUtils.vkCheck(vkCreateImageView(device.getVkDevice(), vkImageViewCreateInfo, null, longBuffer), "Failed to create image view");
            this.vkImageView = longBuffer.get(0);

        }

    }

    public static class ImageViewData {
        private int aspectMask;
        private int baseArrayLayer;
        private int format;
        private int layerCount;
        private int mipLevels;
        private int viewType;

        public ImageViewData() {
            this.baseArrayLayer = 0;
            this.layerCount = 1;
            this.mipLevels = 1;
            this.viewType = VK_IMAGE_VIEW_TYPE_2D;
        }

        public ImageView.ImageViewData aspectMask(int aspectMask) {
            this.aspectMask = aspectMask;
            return this;
        }

        public ImageView.ImageViewData baseArrayLayer(int baseArrayLayer) {
            this.baseArrayLayer = baseArrayLayer;
            return this;
        }

        public ImageView.ImageViewData format(int format) {
            this.format = format;
            return this;
        }

        public ImageView.ImageViewData layerCount(int layerCount) {
            this.layerCount = layerCount;
            return this;
        }

        public ImageView.ImageViewData mipLevels(int mipLevels) {
            this.mipLevels = mipLevels;
            return this;
        }

        public ImageView.ImageViewData viewType(int viewType) {
            this.viewType = viewType;
            return this;
        }

    }

    public void cleanUp() {

        vkDestroyImageView(device.getVkDevice(), vkImageView, null);

    }

}
