package lol.zyrex.features.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.BoatEntity;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.events.player.PlayerTravelEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.utils.interactions.simulate.Simulations;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Speed extends Module {

    SelectSetting mode = new SelectSetting("Режим", "Выберите режим скорости")
            .value("Normal", "Grim", "FunTime One Block", "All Servers")
            .selected("Grim");

    SliderSettings speed = new SliderSettings("Скорость", "Настройка скорости передвижения")
            .range(1.0f, 5.0f)
            .setValue(1.5f)
            .visible(() -> mode.isSelected("Normal"));

    BooleanSetting up = new BooleanSetting("Усиление","Увеличивает дистанцию ускорения до цели в Aura").setValue(true).visible(()-> mode.isSelected("Grim"));

    SliderSettings strength = new SliderSettings("Сила", "Фактор умножения до цели")
            .range(1.0f, 6.0f)
            .setValue(1.5f)
            .visible(() -> mode.isSelected("Grim") && up.isValue());


    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        setup(mode, up, strength, speed);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mode.isSelected("Normal")) {
            Simulations.setVelocity(speed.getValue() / 3);
        }
        if (mode.isSelected("All Servers")) {
            // Timer 1.00005 - очень маленькое ускорение, не детектится
            setTimer(1.00005f);
        }
    }
    
    private void setTimer(float multiplier) {
        try {
            var counter = (lol.zyrex.mixins.client.IRenderTickCounter) mc.getRenderTickCounter();
            counter.setTickTime(50.0f / multiplier);
        } catch (Exception ignored) {}
    }
    
    private void resetTimer() {
        try {
            var counter = (lol.zyrex.mixins.client.IRenderTickCounter) mc.getRenderTickCounter();
            counter.setTickTime(50.0f);
        } catch (Exception ignored) {}
    }

    @Override
    public void deactivate() {
        resetTimer();
    }

    @EventHandler
    public void onMotion(PlayerTravelEvent e) {
        if (mode.isSelected("FunTime One Block")) {
            if (!mc.player.isSwimming() && !mc.player.isGliding() && !mc.player.isSneaking()) {
                if (mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY < 1.5f) {
                    float motion = mc.player.hasStatusEffect(StatusEffects.SPEED) ? 0.32f : 0.28f;
                    Simulations.setVelocity(motion);
                }
            }
        }
        if ((mode.isSelected("Grim")) && e.isPre() && Simulations.hasPlayerMovement()) {
            int collisions = 0;
            float box = 0.4F;
            if (Aura.getInstance().isState() && Aura.getInstance().getTarget() != null && Aura.getInstance().getTarget().isSprinting() && mc.player.isSprinting() && up.isValue()) {
                box = strength.getValue();
            }
            for (Entity ent : mc.world.getEntities())
                if (ent != mc.player && (!(ent instanceof ArmorStandEntity)) && (ent instanceof LivingEntity || ent instanceof BoatEntity) && mc.player.getBoundingBox().expand(box).intersects(ent.getBoundingBox()))
                    collisions++;
            double[] motion = Simulations.forward(0.08 * collisions);
            mc.player.addVelocity(motion[0], 0, motion[1]);
        }

    }

    private boolean hasSprintingTarget() {
        return false;
    }
}