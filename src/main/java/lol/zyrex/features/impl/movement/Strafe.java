package lol.zyrex.features.impl.movement;

import com.google.common.eventbus.Subscribe;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.features.aura.warp.Turns;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;
import lol.zyrex.utils.features.aura.warp.TurnsConfig;
import lol.zyrex.utils.features.aura.warp.TurnsConstructor;
import lol.zyrex.utils.interactions.simulate.Simulations;
import lol.zyrex.utils.math.task.TaskPriority;
import net.minecraft.client.MinecraftClient;

public class Strafe extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public SelectSetting mode = new SelectSetting("Режим", "Выберите тип стрейфов")
            .value("Matrix", "Grim")
            .selected("Matrix");
    SliderSettings speed = new SliderSettings("Скорость", "Выберите скорость для стрейфа")
            .setValue(0.42F).range(0F, 1F).visible(() -> mode.isSelected("Matrix"));

    private float lastYaw, lastPitch;
    private final Turns rot = new Turns(0, 0);

    public Strafe() {
        super("Strafe", "Strafe", ModuleCategory.MOVEMENT);
        setup(mode, speed);
    }

    public static Strafe getInstance() {
        return Instance.get(Strafe.class);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        boolean moving = Simulations.hasPlayerMovement();

        float yaw = mc.player.getYaw();

        if (mode.isSelected("Matrix")) {
            if (moving) {
                yaw = Simulations.moveYaw(mc.player.getYaw());
                double motion = speed.getValue() * 1.5f;
                Simulations.setVelocity(motion);
            } else {
                Simulations.setVelocity(0);
            }
            mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
        } else if (mode.isSelected("Grim")) {
            if (moving) {
                yaw = Simulations.moveYaw(mc.player.getYaw());
                rot.setYaw(yaw);
                rot.setPitch(mc.player.getPitch());
                if (Aura.getInstance().getTarget() == null) {
                    TurnsConnection.INSTANCE.rotateTo(rot, TurnsConfig.DEFAULT, TaskPriority.LOW_PRIORITY , this);
                }
            }
        }

        lastYaw = yaw;
        lastPitch = 0;
    }


    @Override
    public void activate() {
        super.activate();
        lastYaw = mc.player != null ? mc.player.getYaw() : 0;
        lastPitch = mc.player != null ? mc.player.getPitch() : 0;
    }
}
