package lol.zyrex.events.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.entity.LivingEntity;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EntityColorEvent extends EventCancellable {
    int color;
    boolean invisibleToPlayer;
    boolean depthTest = true;
    boolean outline;
    boolean glow;
    LivingEntity entity;

    public EntityColorEvent(int color, boolean invisibleToPlayer, LivingEntity entity) {
        this.color = color;
        this.invisibleToPlayer = invisibleToPlayer;
        this.entity = entity;
    }
}
