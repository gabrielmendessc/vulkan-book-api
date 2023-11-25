package gabrielmendessc.com.vulkan.book.api.eng;

import lombok.Getter;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

@Getter
public class MouseInput {

    private final Vector2f previousPos = new Vector2f(-1, -1);
    private final Vector2f currentPos = new Vector2f();
    private final Vector2f displayVec = new Vector2f();
    private boolean inWindow;
    private boolean leftButtonPressed;
    private boolean rightButtonPressed;

    public MouseInput(long windowHandle) {

        GLFW.glfwSetCursorPosCallback(windowHandle, (handle, xpos, ypos) -> {
            currentPos.x = (float) xpos;
            currentPos.y = (float) ypos;
        });
        GLFW.glfwSetCursorEnterCallback(windowHandle, (handle, entered) -> inWindow = entered);
        GLFW.glfwSetMouseButtonCallback(windowHandle, (handle, buttom, action, mode) -> {
            leftButtonPressed = buttom == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS;
            rightButtonPressed = buttom == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS;
        });

    }

    public void input() {

        getDisplayVec().x = 0;
        getDisplayVec().y = 0;

        if (getPreviousPos().x > 0 && getPreviousPos().y > 0 && isInWindow()) {

            double deltaX = currentPos.x - previousPos.x;
            double deltaY = currentPos.y - previousPos.y;
            if (deltaX != 0) {
                getDisplayVec().y = (float) deltaX;
            }

            if (deltaY != 0) {
                getDisplayVec().x = (float) deltaY;
            }

            getPreviousPos().x = getCurrentPos().x;
            getPreviousPos().y = getCurrentPos().y;

        }

    }

}
