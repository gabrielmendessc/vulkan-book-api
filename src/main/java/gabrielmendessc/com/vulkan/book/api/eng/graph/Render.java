package gabrielmendessc.com.vulkan.book.api.eng.graph;

import gabrielmendessc.com.vulkan.book.api.eng.EngineProperties;
import gabrielmendessc.com.vulkan.book.api.eng.Window;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.CommandPool;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Device;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Instance;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.PhysicalDevice;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Queue;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Surface;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.SwapChain;
import gabrielmendessc.com.vulkan.book.api.eng.scene.Scene;

public class Render {

    private final CommandPool commandPool;
    private final Queue.PresentQueue presentQueue;
    private final ForwardRenderActivity fwdRenderActivity;
    private final Instance instance;
    private final Device device;
    private final Queue.GraphicsQueue graphQueue;
    private final PhysicalDevice physicalDevice;
    private final Surface surface;
    private SwapChain swapChain;


    public Render(Window window, Scene scene) {
        EngineProperties engineProperties = EngineProperties.getInstance();
        instance = new Instance(engineProperties.isValidate());
        physicalDevice = PhysicalDevice.createPhysicalDevice(instance, engineProperties.getPhysDeviceName());
        device = new Device(physicalDevice);
        surface = new Surface(physicalDevice, window.getWindowHandle());
        graphQueue = new Queue.GraphicsQueue(device, 0);
        presentQueue = new Queue.PresentQueue(device, surface, 0);
        swapChain = new SwapChain(device, surface, window, engineProperties.getRequestedImages(), engineProperties.isVSync(), presentQueue, new Queue[]{graphQueue});
        commandPool = new CommandPool(device, graphQueue.getQueueFamilyIndex());
        fwdRenderActivity = new ForwardRenderActivity(swapChain, commandPool);
    }

    public void cleanUp() {
        presentQueue.waitIdle();
        graphQueue.waitIdle();
        device.waitIdle();
        fwdRenderActivity.cleanUp();
        commandPool.cleanUp();
        swapChain.cleanUp();
        surface.cleanUp();
        device.cleanUp();
        physicalDevice.cleanUp();
        instance.cleanUp();
    }

    public void render(Window window, Scene scene) {
        swapChain.acquireNextImage();

        fwdRenderActivity.submit(graphQueue);

        swapChain.presentImage(presentQueue);
    }

}
