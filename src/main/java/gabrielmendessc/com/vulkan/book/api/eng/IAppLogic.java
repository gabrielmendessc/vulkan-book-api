package gabrielmendessc.com.vulkan.book.api.eng;

import gabrielmendessc.com.vulkan.book.api.eng.graph.Render;
import gabrielmendessc.com.vulkan.book.api.eng.scene.Scene;

public interface IAppLogic {

    void cleanUp();

    void input(Window window, Scene scene, long diffTimeMillis);

    void init(Window window, Scene scene, Render render);

    void update(Window window, Scene scene, long diffTimeMillis);

}
