package lol.zyrex.features.impl.movement;

import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.utils.math.projection.Projection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.managers.event.EventHandler;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFallDamage extends Module {

    SelectSetting mode = new SelectSetting("Режим", "Выберите тип")
            .value("SpookyTime")
            .selected("SpookyTime");

    public NoFallDamage() {
        super("NoFallDamage", "No Fall Damage", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.fallDistance > 0 && Projection.getDistanceToGround() >4) {
            mc.player.setVelocity(0, 0, 0);
        }
    }
}
