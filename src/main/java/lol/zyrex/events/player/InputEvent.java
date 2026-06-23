package lol.zyrex.events.player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.PlayerInput;
import lol.zyrex.utils.client.managers.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InputEvent extends EventCancellable {
    PlayerInput input;

    public void setJumping(boolean jump) {
        input = new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), jump, input.sneak(), input.sprint());
    }

    public void setDirectional(boolean forward, boolean backward, boolean left, boolean right) {
        input = new PlayerInput(forward, backward, left, right, input.jump(), input.sneak(), input.sprint());
    }

    public void inputNone() {
        input = new PlayerInput(false, false, false, false, false, false, false);
    }

    public void setAll(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
        input = new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
    }

    public int forward() {
        return input.forward() ? 1 : input.backward() ? -1 : 0;
    }

    public float sideways() {
        return input.left() ? 1 : input.right() ? -1 : 0;
    }
}