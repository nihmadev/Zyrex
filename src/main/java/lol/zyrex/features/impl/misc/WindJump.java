package lol.zyrex.features.impl.misc;

import lol.zyrex.utils.features.aura.warp.Turns;
import lol.zyrex.utils.features.aura.warp.TurnsConfig;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;
import lol.zyrex.utils.math.task.TaskPriority;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.BindSetting;
import lol.zyrex.events.keyboard.HotBarScrollEvent;
import lol.zyrex.events.keyboard.KeyEvent;
import lol.zyrex.events.player.HotBarUpdateEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.events.render.WorldRenderEvent;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.interactions.inv.InventoryTask;
import lol.zyrex.utils.math.time.StopWatch;
import lol.zyrex.utils.math.script.Script;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.features.impl.render.ProjectilePrediction;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WindJump extends Module {
    private final Turns rot = new Turns(0, 0);
    BindSetting windChargeBind = new BindSetting("Заряд ветра", "Бросить заряд ветра");
    StopWatch stopWatch = new StopWatch();
    Script script = new Script();

    public WindJump() {
        super("WindJump", "Wind Jump", ModuleCategory.MISC);
        setup(windChargeBind);
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        if (!script.isFinished()) e.cancel();
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (!script.isFinished()) e.cancel();
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyReleased(windChargeBind.getKey())) {
            if (stopWatch.finished(0)) {
                InventoryTask.swapAndUse(Items.WIND_CHARGE);
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (PlayerInteractionHelper.isKey(windChargeBind)) {
            rot.setYaw(mc.player.getYaw());
            rot.setPitch(90);
            TurnsConnection.INSTANCE.rotateTo(rot, TurnsConfig.DEFAULT, TaskPriority.LOW_PRIORITY, this);
            ItemStack stack = Items.WIND_CHARGE.getDefaultStack();
            ProjectilePrediction.getInstance().drawPredictionInHand(e.getStack(), List.of(stack), MathAngle.cameraAngle());
        }
    }

    @EventHandler

    public void onTick(TickEvent e) {
        if (!script.isFinished() && stopWatch.every(250)) {
            script.update();
        }
    }
}