package gabrielmendessc.com.vulkan.book.api;

import gabrielmendessc.com.vulkan.book.api.eng.Engine;
import gabrielmendessc.com.vulkan.book.api.eng.IAppLogic;
import gabrielmendessc.com.vulkan.book.api.eng.Window;
import gabrielmendessc.com.vulkan.book.api.eng.graph.Render;
import gabrielmendessc.com.vulkan.book.api.eng.scene.Scene;
import org.tinylog.Logger;

public class Main implements IAppLogic {

    public static void main(String[] args) {

        Logger.info("Starting application");

        Engine engine = new Engine("Vulkan Book", new Main());
        engine.start();

    }

    @Override
    public void cleanUp() {
        // To be implemented
    }

    @Override
    public void init(Window window, Scene scene, Render render) {
        // To be implemented
    }

    @Override
    public void input(Window window, Scene scene, long diffTimeMillis) {
        // To be implemented
    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMillis) {
        // To be implemented
    }

}
