package lol.zyrex.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.features.module.setting.implement.ColorSetting;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.events.render.FogEvent;
import lol.zyrex.events.render.WorldRenderEvent;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Atmosphere extends Module {

    public static Atmosphere getInstance() {
        return Instance.get(Atmosphere.class);
    }

    public final SelectSetting presetSetting = new SelectSetting("Прессет", "Выберите пресет атмосферы")
            .value("Clear", "Overcast", "Hell", "Twilight", "BloodMoon", "Misty", "Nightmare", "Custom");

    public final ColorSetting customSkyColor = new ColorSetting("Цвет неба", "Свой цвет неба")
            .value(new Color(100, 150, 255, 255).getRGB())
            .visible(() -> presetSetting.getSelected().equals("Custom"));

    public final ColorSetting customCloudColor = new ColorSetting("Цвет облаков", "Свой цвет облаков")
            .value(new Color(200, 200, 210, 255).getRGB())
            .visible(() -> presetSetting.getSelected().equals("Custom"));

    public final ColorSetting customFogColor = new ColorSetting("Цвет тумана", "Свой цвет тумана")
            .value(new Color(180, 190, 210, 255).getRGB())
            .visible(() -> presetSetting.getSelected().equals("Custom"));

    public final SliderSettings fogDistance = new SliderSettings("Дист. тумана", "Дистанция тумана")
            .range(10, 200).setValue(80);

    public final BooleanSetting lightningEnabled = new BooleanSetting("Молнии", "Эффект молний на небе")
            .setValue(false);

    public final SliderSettings lightningFrequency = new SliderSettings("Частота молний", "Секунд между разрядами")
            .range(0.5F, 10.0F).setValue(3.0F)
            .visible(() -> lightningEnabled.isValue());

    public final BooleanSetting thunderSound = new BooleanSetting("Звук грома", "Звук при разряде молнии")
            .setValue(true)
            .visible(() -> lightningEnabled.isValue());

    public final SliderSettings starBrightness = new SliderSettings("Яркость звёзд", "Яркость звёзд")
            .range(0.0F, 3.0F).setValue(1.0F);

    public final SliderSettings rainStrength = new SliderSettings("Дождь", "Сила дождя")
            .range(-1.0F, 1.0F).setValue(-1.0F);

    public final SliderSettings thunderStrength = new SliderSettings("Гроза", "Сила грозы")
            .range(-1.0F, 1.0F).setValue(-1.0F);

    public final SliderSettings rainDensity = new SliderSettings("Густота дождя", "Количество капель")
            .range(0.5F, 3.0F).setValue(1.5F)
            .visible(() -> rainStrength.getValue() >= 0.0F);

    public final ColorSetting customRainColor = new ColorSetting("Цвет дождя", "Свой цвет дождя")
            .value(new Color(150, 170, 255, 200).getRGB())
            .visible(() -> presetSetting.getSelected().equals("Custom") && rainStrength.getValue() >= 0.0F);

    final List<LightningBolt> bolts = new ArrayList<>();
    final List<RainDrop> rainDrops = new ArrayList<>();
    long lastBoltTime = 0;
    long lastRainSpawn = 0;
    final Random random = Random.create();

    public Atmosphere() {
        super("Atmosphere", "Atmosphere", ModuleCategory.RENDER);
        setup(presetSetting, customSkyColor, customCloudColor, customFogColor,
                fogDistance, lightningEnabled, lightningFrequency, thunderSound, starBrightness,
                rainStrength, thunderStrength, rainDensity, customRainColor);
    }

    @Override
    public void activate() {
        super.activate();
        lastBoltTime = System.currentTimeMillis();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        bolts.clear();
        rainDrops.clear();
    }

    @EventHandler
    public void onFog(FogEvent e) {
        e.setDistance(fogDistance.getValue());
        e.setColor(getEffectiveFogColor());
        e.cancel();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;

        tickLightning();
        tickRain();
        renderBolts(e.getStack());
        renderRain(e.getStack());
    }

    void tickLightning() {
        if (!lightningEnabled.isValue()) {
            bolts.clear();
            return;
        }

        long now = System.currentTimeMillis();
        float freq = lightningFrequency.getValue();
        boolean isStorm = thunderStrength.getValue() >= 0.3F;
        if (isStorm) freq = Math.max(0.5F, freq * 0.4F);

        long interval = (long) (freq * 1000);
        if (now - lastBoltTime > interval) {
            int count = isStorm ? 2 + random.nextInt(3) : 1;
            if (isStorm && count > 1) {
                Vec3d playerPos = mc.player != null ? mc.player.getPos() : Vec3d.ZERO;
                double cRange = 20 + random.nextDouble() * 30;
                double cAngle = getViewBiasedAngle(100);
                double cx = playerPos.x + Math.cos(cAngle) * cRange;
                double cz = playerPos.z + Math.sin(cAngle) * cRange;
                for (int i = 0; i < count; i++) {
                    spawnBolt(cx, cz);
                }
            } else {
                for (int i = 0; i < count; i++) {
                    spawnBolt();
                }
            }
            lastBoltTime = now;
        }

        Iterator<LightningBolt> it = bolts.iterator();
        while (it.hasNext()) {
            LightningBolt bolt = it.next();
            bolt.age++;
            if (bolt.age > bolt.maxAge) {
                it.remove();
            }
        }
    }

    void tickRain() {
        float rain = rainStrength.getValue();
        if (rain < 0.0F) {
            rainDrops.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long interval = Math.max(5, (long) (30 / (rain * rainDensity.getValue())));
        if (now - lastRainSpawn > interval && rainDrops.size() < 500 * rainDensity.getValue()) {
            int count = Math.max(1, (int) (rain * rainDensity.getValue() * 3));
            for (int i = 0; i < count; i++) {
                spawnRainDrop();
            }
            lastRainSpawn = now;
        }

        Iterator<RainDrop> it = rainDrops.iterator();
        while (it.hasNext()) {
            RainDrop drop = it.next();
            drop.update();
            if (drop.dead) {
                it.remove();
            }
        }
    }

    double getViewBiasedAngle(double spreadDeg) {
        float yaw = mc.player != null ? mc.player.getYaw() : 0;
        double viewAngle = Math.toRadians(yaw) + Math.PI / 2;
        return viewAngle + (random.nextDouble() - 0.5) * Math.toRadians(spreadDeg);
    }

    void spawnBolt() {
        spawnBolt(Double.NaN, Double.NaN);
    }

    void spawnBolt(double centerX, double centerZ) {
        if (mc.player == null || mc.world == null) return;
        Vec3d playerPos = mc.player.getPos();
        double baseY = playerPos.y + 90;

        double x, z;
        if (Double.isNaN(centerX)) {
            double range = 20 + random.nextDouble() * 30;
            double angle = getViewBiasedAngle(100);
            x = playerPos.x + Math.cos(angle) * range;
            z = playerPos.z + Math.sin(angle) * range;
        } else {
            x = centerX + (random.nextDouble() - 0.5) * 6;
            z = centerZ + (random.nextDouble() - 0.5) * 6;
        }
        double y = baseY + random.nextDouble() * 50;
        Vec3d start = new Vec3d(x, y, z);
        double endY = baseY - 40 - random.nextDouble() * 30;
        Vec3d end = new Vec3d(
                x + (random.nextDouble() - 0.5) * 8,
                endY,
                z + (random.nextDouble() - 0.5) * 8
        );
        bolts.add(new LightningBolt(start, end, random));

        if (thunderSound.isValue()) {
            mc.world.playSound(mc.player, BlockPos.ofFloored(x, y, z),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER,
                    10000.0F, 0.8F + random.nextFloat() * 0.2F);
        }
    }

    void spawnRainDrop() {
        if (mc.player == null) return;
        Vec3d playerPos = mc.player.getPos();
        double range = 40;
        double x = playerPos.x + (random.nextDouble() - 0.5) * range * 2;
        double z = playerPos.z + (random.nextDouble() - 0.5) * range * 2;
        double y = 50 + random.nextDouble() * 50;
        rainDrops.add(new RainDrop(x, y, z, random));
    }

    void renderBolts(MatrixStack stack) {
        if (bolts.isEmpty()) return;
        MatrixStack.Entry entry = stack.peek();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(1.5f);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        for (LightningBolt bolt : bolts) {
            float alpha = getBoltAlpha(bolt);
            if (alpha <= 0.01f) continue;
            int color = ColorAssist.getColor(255, 255, 255, (int) (alpha * 255));
            for (int i = 0; i < bolt.points.size() - 1; i++) {
                addLineSegment(buf, entry, bolt.points.get(i), bolt.points.get(i + 1), color);
            }
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    float getBoltAlpha(LightningBolt bolt) {
        float progress = (float) bolt.age / bolt.maxAge;
        if (progress < 0.1f) return 1.0f;
        float fade = 1.0f - (progress - 0.1f) / 0.9f;
        return MathHelper.clamp(fade, 0, 1);
    }

    void addLineSegment(BufferBuilder buf, MatrixStack.Entry entry, Vec3d p1, Vec3d p2, int color) {
        Vector3f s = p1.toVector3f();
        Vector3f e = p2.toVector3f();
        Vector3f normal = new Vector3f(e).sub(s).normalize();
        if (normal.length() < 0.01f) normal = new Vector3f(0, 1, 0);
        buf.vertex(entry, s).color(color).normal(entry, normal);
        buf.vertex(entry, e).color(color).normal(entry, normal);
    }

    void renderRain(MatrixStack stack) {
        if (rainDrops.isEmpty()) return;
        if (rainStrength.getValue() < 0.0F) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Matrix4f mat = stack.peek().getPositionMatrix();
        int rainColor = getEffectiveRainColor();
        BufferBuilder buf = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR
        );

        for (RainDrop drop : rainDrops) {
            float alpha = MathHelper.clamp(drop.life / 30.0f, 0.0f, 1.0f);
            int col = ColorAssist.setAlpha(rainColor, (int) (ColorAssist.alpha(rainColor) * alpha));

            Vec3d dir = new Vec3d(drop.vx, drop.vy, drop.vz).normalize();
            Vec3d up = new Vec3d(0, 1, 0);
            if (Math.abs(dir.dotProduct(up)) > 0.99) up = new Vec3d(1, 0, 0);
            Vec3d right = dir.crossProduct(up).normalize();

            float halfW = drop.size * 0.25f;
            float halfH = drop.size;

            buf.vertex(mat, (float) (drop.x - right.x * halfW), (float) drop.y, (float) (drop.z - right.z * halfW)).color(col);
            buf.vertex(mat, (float) (drop.x + right.x * halfW), (float) drop.y, (float) (drop.z + right.z * halfW)).color(col);
            buf.vertex(mat, (float) (drop.x + right.x * halfW + dir.x * halfH), (float) (drop.y + dir.y * halfH), (float) (drop.z + right.z * halfW + dir.z * halfH)).color(col);
            buf.vertex(mat, (float) (drop.x - right.x * halfW + dir.x * halfH), (float) (drop.y + dir.y * halfH), (float) (drop.z - right.z * halfW + dir.z * halfH)).color(col);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public int getEffectiveRainColor() {
        if (presetSetting.getSelected().equals("Custom")) return customRainColor.getColor();
        return switch (presetSetting.getSelected()) {
            case "Hell" -> ColorAssist.getColor(220, 60, 30, 200);
            case "BloodMoon" -> ColorAssist.getColor(200, 35, 25, 200);
            case "Nightmare" -> ColorAssist.getColor(90, 70, 140, 180);
            default -> ColorAssist.getColor(150, 180, 255, 220);
        };
    }

    public int getEffectiveSkyColor() {
        if (presetSetting.getSelected().equals("Custom")) return customSkyColor.getColor();
        return switch (presetSetting.getSelected()) {
            case "Overcast" -> ColorAssist.getColor(90, 90, 105);
            case "Hell" -> ColorAssist.getColor(180, 20, 8);
            case "Twilight" -> ColorAssist.getColor(150, 70, 130);
            case "BloodMoon" -> ColorAssist.getColor(110, 8, 8);
            case "Misty" -> ColorAssist.getColor(155, 162, 170);
            case "Nightmare" -> ColorAssist.getColor(12, 3, 25);
            default -> 0;
        };
    }

    public int getEffectiveCloudColor() {
        if (presetSetting.getSelected().equals("Custom")) return customCloudColor.getColor();
        return switch (presetSetting.getSelected()) {
            case "Overcast" -> ColorAssist.getColor(65, 65, 80);
            case "Hell" -> ColorAssist.getColor(130, 18, 3);
            case "Twilight" -> ColorAssist.getColor(110, 40, 90);
            case "BloodMoon" -> ColorAssist.getColor(80, 8, 8);
            case "Misty" -> ColorAssist.getColor(145, 150, 160);
            case "Nightmare" -> ColorAssist.getColor(8, 2, 15);
            default -> 0;
        };
    }

    public int getEffectiveFogColor() {
        if (presetSetting.getSelected().equals("Custom")) return customFogColor.getColor();
        return switch (presetSetting.getSelected()) {
            case "Overcast" -> ColorAssist.getColor(80, 80, 95);
            case "Hell" -> ColorAssist.getColor(150, 15, 3);
            case "Twilight" -> ColorAssist.getColor(110, 50, 120);
            case "BloodMoon" -> ColorAssist.getColor(90, 6, 6);
            case "Misty" -> ColorAssist.getColor(175, 180, 190);
            case "Nightmare" -> ColorAssist.getColor(6, 2, 12);
            default -> ColorAssist.getColor(180, 190, 210);
        };
    }

    public float getEffectiveStarBrightness() {
        if (presetSetting.getSelected().equals("Custom")) return starBrightness.getValue();
        return switch (presetSetting.getSelected()) {
            case "Overcast" -> 0.0F;
            case "Hell" -> 0.5F;
            case "Twilight" -> 1.8F;
            case "BloodMoon" -> 0.2F;
            case "Misty" -> 0.0F;
            case "Nightmare" -> 3.0F;
            default -> 1.0F;
        };
    }

    static class LightningBolt {
        final List<Vec3d> points = new ArrayList<>();
        int age = 0;
        final int maxAge = 80;

        LightningBolt(Vec3d start, Vec3d end, Random rnd) {
            int segments = 5 + rnd.nextInt(6);
            Vec3d dir = end.subtract(start);
            double segLen = dir.length() / segments;
            points.add(start);
            for (int i = 1; i < segments; i++) {
                float t = (float) i / segments;
                Vec3d base = start.lerp(end, t);
                double spread = segLen * 1.5 * (1 - t * 0.5);
                double ox = (rnd.nextDouble() - 0.5) * spread;
                double oz = (rnd.nextDouble() - 0.5) * spread;
                points.add(base.add(ox, 0, oz));
            }
            points.add(end);
        }
    }

    static class RainDrop {
        double x, y, z;
        double vx, vy, vz;
        boolean dead = false;
        int life;
        float size;

        RainDrop(double x, double y, double z, Random rnd) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vy = -0.8 - rnd.nextDouble() * 0.6;
            this.vx = 0.1 + rnd.nextDouble() * 0.15;
            this.vz = 0.05 + rnd.nextDouble() * 0.1;
            this.life = 80 + rnd.nextInt(40);
            this.size = 0.2f + rnd.nextFloat() * 0.25f;
        }

        void update() {
            x += vx;
            z += vz;
            y += vy;
            vy -= 0.02;
            life--;
            if (life <= 0 || y < 2) {
                dead = true;
            }
        }
    }
}
