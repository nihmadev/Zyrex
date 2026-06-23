package lol.zyrex.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import lol.zyrex.utils.client.managers.event.events.callables.EventCancellable;

@Setter
@Getter
@AllArgsConstructor
public class SwimmingEvent extends EventCancellable {
    Vec3d vector;
}
