package lol.zyrex.features.impl.movement;

import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;

import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.utils.interactions.simulate.Simulations;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoWeb extends Module {
    public static NoWeb getInstance() {
        return Instance.get(NoWeb.class);
    }

    public final SelectSetting webMode = new SelectSetting("Режим", "Выберите режим обхода").value("Grim");

    public NoWeb() {
        super("NoWeb", "No Web", ModuleCategory.MOVEMENT);
        setup(webMode);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
            double[] speed = Simulations.calculateDirection(0.35);
            mc.player.addVelocity(speed[0], 0, speed[1]);
            mc.player.velocity.y = mc.options.jumpKey.isPressed() ? 0.65f : mc.options.sneakKey.isPressed() ? -0.65f : 0;
        }
    }
}