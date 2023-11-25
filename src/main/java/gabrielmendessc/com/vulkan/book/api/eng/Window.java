package gabrielmendessc.com.vulkan.book.api.eng;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryUtil;

import java.util.Objects;

@Getter
public class Window {

    private final MouseInput mouseInput;
    private final long windowHandle;
    private int height;
    @Setter
    private boolean resized;
    private int width;

    public Window(String title) {

        this(title, null);

    }

    public Window(String title, GLFWKeyCallbackI keyCallback) {

        if (!GLFW.glfwInit()) {

            throw new IllegalStateException("Unable to initialize GLFW");

        }

        if (!GLFWVulkan.glfwVulkanSupported()) {

            throw new IllegalStateException("Cannot find a compatbile Vulkan installable client driver(ICB)");

        }

        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        this.width = vidMode.width();
        this.height = vidMode.height();

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_FALSE);

        //Create the window
        this.windowHandle = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {

            throw new RuntimeException("Failed to create the GLFW window");

        }

        GLFW.glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> resize(w, h));

        GLFW.glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {

            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {

                GLFW.glfwSetWindowShouldClose(window, true);

            }

            if (Objects.nonNull(keyCallback)) {

                keyCallback.invoke(window, key, scancode, action, mods);

            }

        });

        mouseInput = new MouseInput(windowHandle);

    }

    public void cleanUp() {

        Callbacks.glfwFreeCallbacks(windowHandle);

        GLFW.glfwDestroyWindow(windowHandle);
        GLFW.glfwTerminate();

    }

    public boolean isKeyPressed(int keyCode) {
        return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
    }

    public void pollEvents() {

        GLFW.glfwPollEvents();

        mouseInput.input();

    }

    public void resetResized() {
        resized = false;
    }

    public void resize(int width, int height) {
        resized = true;
        this.width = width;
        this.height = height;
    }

    public void setShouldClose() {
        GLFW.glfwSetWindowShouldClose(windowHandle, true);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(windowHandle);
    }

}
