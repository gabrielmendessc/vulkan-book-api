package gabrielmendessc.com.vulkan.book.api.eng.graph;

import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.CommandBuffer;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.CommandPool;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Device;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Fence;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.FrameBuffer;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.ImageView;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Queue;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.SwapChain;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.SwapChainRenderPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class ForwardRenderActivity {

    private final CommandBuffer[] commandBuffers;
    private final Fence[] fences;
    private final FrameBuffer[] frameBuffers;
    private final SwapChainRenderPass renderPass;
    private final SwapChain swapChain;

    public ForwardRenderActivity(SwapChain swapChain, CommandPool commandPool) {
        this.swapChain = swapChain;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Device device = swapChain.getDevice();
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            ImageView[] imageViews = swapChain.getImageViews();
            int numImages = imageViews.length;

            renderPass = new SwapChainRenderPass(swapChain);

            LongBuffer pAttachments = stack.mallocLong(1);
            frameBuffers = new FrameBuffer[numImages];
            for (int i = 0; i < numImages; i++) {
                pAttachments.put(0, imageViews[i].getVkImageView());
                frameBuffers[i] = new FrameBuffer(device, swapChainExtent.width(), swapChainExtent.height(), pAttachments, renderPass.getVkRenderPass());
            }

            commandBuffers = new CommandBuffer[numImages];
            fences = new Fence[numImages];
            for (int i = 0; i < numImages; i++) {
                commandBuffers[i] = new CommandBuffer(commandPool, true, false);
                fences[i] = new Fence(device, true);
                recordCommandBuffer(commandBuffers[i], frameBuffers[i], swapChainExtent.width(), swapChainExtent.height());
            }
        }
    }

    public void cleanUp() {

        for(FrameBuffer frameBuffer : frameBuffers) {
            frameBuffer.cleanUp();
        }
        renderPass.cleanUp();
        for(CommandBuffer commandBuffer : commandBuffers) {
            commandBuffer.cleanUp();
        }
        for(Fence fence : fences) {
            fence.cleanUp();
        }
    }

    public void submit(Queue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = swapChain.getCurrentFrame();
            CommandBuffer commandBuffer = commandBuffers[idx];
            Fence currentFence = fences[idx];
            currentFence.fenceWait();
            currentFence.reset();
            SwapChain.SyncSemaphores syncSemaphores = swapChain.getSyncSemaphoresList()[idx];
            queue.submit(stack.pointers(commandBuffer.getVkCommandBuffer()),
                    stack.longs(syncSemaphores.imgAcquisitionSemaphore().getVkSemaphore()),
                    stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                    stack.longs(syncSemaphores.renderCompleteSemaphore().getVkSemaphore()), currentFence);

        }
    }

    private void recordCommandBuffer(CommandBuffer commandBuffer, FrameBuffer frameBuffer, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.apply(0, v -> v.color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1));
            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass.getVkRenderPass())
                    .pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height))
                    .framebuffer(frameBuffer.getVkFrameBuffer());

            commandBuffer.beginRecording();
            vkCmdBeginRenderPass(commandBuffer.getVkCommandBuffer(), renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
            vkCmdEndRenderPass(commandBuffer.getVkCommandBuffer());
            commandBuffer.endRecording();
        }
    }

}
