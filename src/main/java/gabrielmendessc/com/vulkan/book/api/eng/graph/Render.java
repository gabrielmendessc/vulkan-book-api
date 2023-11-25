package gabrielmendessc.com.vulkan.book.api.eng.graph;

import gabrielmendessc.com.vulkan.book.api.eng.EngineProperties;
import gabrielmendessc.com.vulkan.book.api.eng.Window;
import gabrielmendessc.com.vulkan.book.api.eng.graph.vk.Instance;
import gabrielmendessc.com.vulkan.book.api.eng.scene.Scene;

public class Render {

    private final Instance instance;

    public Render(Window window, Scene scene) {
        EngineProperties engineProperties = EngineProperties.getInstance();
        instance = new Instance(engineProperties.isValidate());
    }

    public void cleanUp() {
        instance.cleanUp();
    }

    public void render(Window window, Scene scene) {
        //TODO - Render render
    }

}
