package gabrielmendessc.com.vulkan.book.api.eng.graph;

import gabrielmendessc.com.vulkan.book.api.eng.EngineProperties;
import gabrielmendessc.com.vulkan.book.api.eng.Window;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Device;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Instance;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.PhysicalDevice;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Queue;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Surface;
import gabrielmendessc.com.vulkan.book.api.eng.scene.Scene;

public class Render {

    private final Instance instance;
    private final Device device;
    private final Queue.GraphicsQueue graphQueue;
    private final PhysicalDevice physicalDevice;
    private final Surface surface;

    public Render(Window window, Scene scene) {
        EngineProperties engineProperties = EngineProperties.getInstance();
        instance = new Instance(engineProperties.isValidate());
        physicalDevice = PhysicalDevice.createPhysicalDevice(instance, engineProperties.getPhysDeviceName());
        device = new Device(physicalDevice);
        surface = new Surface(physicalDevice, window.getWindowHandle());
        graphQueue = new Queue.GraphicsQueue(device, 0);
    }

    public void cleanUp() {
        surface.cleanUp();
        device.cleanUp();
        physicalDevice.cleanUp();
        instance.cleanUp();
    }

    public void render(Window window, Scene scene) {
        //TODO - Render render
    }

}
