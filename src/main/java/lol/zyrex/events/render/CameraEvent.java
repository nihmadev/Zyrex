package lol.zyrex.events.render;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.events.callables.EventCancellable;
import lol.zyrex.utils.features.aura.warp.Turns;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraEvent extends EventCancellable {
    boolean cameraClip;
    float distance;
    Turns angle;
}
