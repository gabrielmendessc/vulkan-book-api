package gabrielmendessc.com.vulkan.book.api.eng.graph.vk;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static gabrielmendessc.com.vulkan.book.api.eng.graph.vk.VKUtils.vkCheck;
import static org.lwjgl.vulkan.VK11.*;

public class Semaphore {

    private final Device device;
    @Getter
    private final long vkSemaphore;

    public Semaphore(Device device) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            vkCheck(vkCreateSemaphore(device.getVkDevice(), semaphoreCreateInfo, null, lp), "Failed to create Semaphore");
            vkSemaphore = lp.get(0);
        }
    }

    public void cleanUp() {
        vkDestroySemaphore(device.getVkDevice(), vkSemaphore, null);
    }

}
