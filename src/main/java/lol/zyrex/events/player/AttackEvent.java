package lol.zyrex.events.player;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import lol.zyrex.utils.client.managers.event.events.callables.EventCancellable;

@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttackEvent extends EventCancellable {
    Entity entity;
}
