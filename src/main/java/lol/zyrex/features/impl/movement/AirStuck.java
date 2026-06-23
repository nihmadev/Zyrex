package lol.zyrex.features.impl.movement;

import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AirStuck extends Module {

    public AirStuck() {
        super("AirStuck", "Air Stuck", ModuleCategory.MOVEMENT);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        mc.player.setVelocity(0, 0, 0);
        mc.player.setMovementSpeed(0);

        switch (e.getPacket()) {
            case PlayerMoveC2SPacket move -> e.cancel();
            default -> {}
        }

    }
}
