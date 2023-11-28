package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static gabrielmendessc.com.vulkan.book.api.eng.graph.vk.VKUtils.vkCheck;
import static org.lwjgl.vulkan.VK11.*;

public class Fence {

    private final Device device;
    @Getter
    private final long vkFence;

    public Fence(Device device, boolean signaled) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(signaled ? VK_FENCE_CREATE_SIGNALED_BIT : 0);

            LongBuffer lp = stack.mallocLong(1);
            vkCheck(vkCreateFence(device.getVkDevice(), fenceCreateInfo, null, lp),
                    "Failed to create semaphore");
            vkFence = lp.get(0);
        }
    }

    public void cleanUp() {
        vkDestroyFence(device.getVkDevice(), vkFence, null);
    }

    public void fenceWait() {
        vkWaitForFences(device.getVkDevice(), vkFence, true, Long.MAX_VALUE);
    }

    public void reset() {
        vkResetFences(device.getVkDevice(), vkFence);
    }

}
