package lol.zyrex.events.keyboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lol.zyrex.utils.client.managers.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class MouseRotationEvent extends EventCancellable {
    float cursorDeltaX, cursorDeltaY;
}
