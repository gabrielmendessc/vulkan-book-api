package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo;
import org.lwjgl.vulkan.VkDevice;
import org.tinylog.Logger;

import static gabrielmendessc.com.vulkan.book.api.eng.graph.vk.VKUtils.vkCheck;
import static org.lwjgl.vulkan.VK11.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK11.VK_COMMAND_BUFFER_LEVEL_SECONDARY;
import static org.lwjgl.vulkan.VK11.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT;
import static org.lwjgl.vulkan.VK11.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static org.lwjgl.vulkan.VK11.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO;
import static org.lwjgl.vulkan.VK11.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK11.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK11.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK11.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK11.vkResetCommandBuffer;

public class CommandBuffer {

    private final CommandPool commandPool;
    private final boolean oneTimeSubmit;
    @Getter
    private final VkCommandBuffer vkCommandBuffer;
    private boolean primary;

    public CommandBuffer(CommandPool commandPool, boolean primary, boolean oneTimeSubmit) {
        Logger.trace("Creating command buffer");
        this.commandPool = commandPool;
        this.primary = primary;
        this.oneTimeSubmit = oneTimeSubmit;
        VkDevice vkDevice = commandPool.getDevice().getVkDevice();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool.getVkCommandPool())
                    .level(primary ? VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK_COMMAND_BUFFER_LEVEL_SECONDARY)
                    .commandBufferCount(1);
            PointerBuffer pb = stack.mallocPointer(1);
            vkCheck(vkAllocateCommandBuffers(vkDevice, cmdBufAllocateInfo, pb),
                    "Failed to allocate render command buffer");

            vkCommandBuffer = new VkCommandBuffer(pb.get(0), vkDevice);
        }
    }

    public record InheritanceInfo(long vkRenderPass, long vkFrameBuffer, int subPass) {}

    public void beginRecording() {
        beginRecording(null);
    }

    public void beginRecording(InheritanceInfo inheritanceInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            if (oneTimeSubmit) {
                cmdBufInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            }
            if (!primary) {
                if (inheritanceInfo == null) {
                    throw new RuntimeException("Secondary buffers must declare inheritance info");
                }
                VkCommandBufferInheritanceInfo vkInheritanceInfo = VkCommandBufferInheritanceInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
                        .renderPass(inheritanceInfo.vkRenderPass)
                        .subpass(inheritanceInfo.subPass)
                        .framebuffer(inheritanceInfo.vkFrameBuffer);
                cmdBufInfo.pInheritanceInfo(vkInheritanceInfo);
                cmdBufInfo.flags(VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT);
            }
            vkCheck(vkBeginCommandBuffer(vkCommandBuffer, cmdBufInfo), "Failed to begin command buffer");
        }
    }

    public void endRecording() {
        vkCheck(vkEndCommandBuffer(vkCommandBuffer), "Failed to end command buffer");
    }

    public void cleanUp() {
        Logger.trace("Destroying command buffer");
        vkFreeCommandBuffers(commandPool.getDevice().getVkDevice(), commandPool.getVkCommandPool(), vkCommandBuffer);
    }

    public void reset() {
        vkResetCommandBuffer(vkCommandBuffer, VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
    }

}
