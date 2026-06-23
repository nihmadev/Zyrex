package lol.zyrex.features.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.features.module.setting.implement.ColorSetting;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.events.render.EntityColorEvent;
import lol.zyrex.events.render.WorldRenderEvent;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.display.interfaces.QuickImports;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Chams extends Module implements QuickImports {
    SelectSetting mode = new SelectSetting("Режим", "Режим отрисовки чеймсов")
            .value("Glow", "Outline")
            .selected("Glow");
    ColorSetting color = new ColorSetting("Цвет", "Цвет чеймсов")
            .setColor(new Color(255, 101, 57, 255).getRGB());

    MultiSelectSetting entityType = new MultiSelectSetting("Тип сущности", "Какие сущности подсвечивать")
            .value("Players", "Mobs", "Animals")
            .selected("Players");
    BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Показывать chams сквозь стены").setValue(true);

    Set<LivingEntity> throughWallsEntities = new HashSet<>();

    public Chams() {
        super("Chams", "Chams", ModuleCategory.RENDER);
        setup(mode, color, entityType, throughWalls);
    }

    @EventHandler
    public void onEntityColor(EntityColorEvent e) {
        LivingEntity entity = e.getEntity();
        if (entity == null || entity == mc.player) return;
        if (!isTypeSelected(entity)) return;

        int chamsColor = color.getColor();

        if (throughWalls.isValue()) {
            throughWallsEntities.add(entity);
        }

        switch (mode.getSelected()) {
            case "Glow" -> {
                e.cancel();
                e.setColor(chamsColor);
                e.setDepthTest(!throughWalls.isValue());
                e.setGlow(true);
            }
            case "Outline" -> {
                e.cancel();
                e.setColor(chamsColor);
                e.setDepthTest(!throughWalls.isValue());
                e.setOutline(true);
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (throughWallsEntities.isEmpty()) return;

        VertexConsumerProvider vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
        float tickDelta = e.getPartialTicks();
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();

        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        for (LivingEntity entity : throughWallsEntities) {
            if (entity.isRemoved()) continue;
            Vec3d pos = Calculate.interpolate(entity);
            mc.getEntityRenderDispatcher().render(
                    entity,
                    pos.x - cameraPos.x,
                    pos.y - cameraPos.y,
                    pos.z - cameraPos.z,
                    tickDelta,
                    e.getStack(),
                    vertexConsumers,
                    0xF000F0
            );
        }

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate imm) {
            imm.draw();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        throughWallsEntities.clear();
    }

    private boolean isTypeSelected(LivingEntity entity) {
        if (entity instanceof PlayerEntity) return entityType.isSelected("Players");
        if (entity instanceof AnimalEntity) return entityType.isSelected("Animals");
        if (entity instanceof MobEntity) return entityType.isSelected("Mobs");
        return false;
    }
}
