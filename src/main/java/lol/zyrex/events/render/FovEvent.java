package lol.zyrex.events.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.events.callables.EventCancellable;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FovEvent extends EventCancellable {
    int fov;
}
