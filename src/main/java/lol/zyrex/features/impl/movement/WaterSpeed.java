package lol.zyrex.features.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;

import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.events.player.SwimmingEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WaterSpeed extends Module {

    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите режим обхода").value("FunTime");

    public WaterSpeed() {
        super("WaterSpeed", "Water Speed", ModuleCategory.MOVEMENT);
        setup(modeSetting);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (modeSetting.isSelected("FunTime") && mc.player.isSwimming() && mc.player.isOnGround()) {
            mc.player.jump();
            mc.player.velocity.y = 0.1;
        }
    }

    
    @EventHandler

    public void onSwimming(SwimmingEvent e) {
        if (modeSetting.isSelected("FunTime")) {
            if (mc.options.jumpKey.isPressed()) {
                float pitch = TurnsConnection.INSTANCE.getRotation().getPitch();
                float boost = pitch >= 0 ? MathHelper.clamp(pitch / 45, 1, 2) : 0.8F;
                e.getVector().y = 0.5 * boost;
            } else if (mc.options.sneakKey.isPressed()) {
                e.getVector().y = -0.8;
            }
        }
    }
}
