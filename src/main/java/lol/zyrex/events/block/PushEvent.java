package lol.zyrex.events.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lol.zyrex.utils.client.managers.event.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class PushEvent extends EventCancellable {
    private Type type;

    public enum Type {
        COLLISION, BLOCK, WATER
    }
}
