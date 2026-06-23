package lol.zyrex.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lol.zyrex.features.impl.combat.TriggerBot;
import lol.zyrex.features.module.setting.implement.ColorSetting;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.event.types.EventType;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.utils.features.aura.striking.StrikeManager;
import lol.zyrex.common.animation.Animation;
import lol.zyrex.common.animation.Direction;
import lol.zyrex.common.animation.implement.Decelerate;
import lol.zyrex.events.player.RotationUpdateEvent;
import lol.zyrex.events.render.WorldRenderEvent;
import lol.zyrex.Zyrex;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.math.calc.CalcVector;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.math.time.StopWatch;
import lol.zyrex.utils.display.geometry.Render3D;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import net.minecraft.util.Identifier;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetESP extends Module {

    public static TargetESP getInstance() {
        return Instance.get(TargetESP.class);
    }

    Animation esp_anim = new Decelerate().setMs(400).setValue(1);
    SelectSetting targetEspType = new SelectSetting("Отображения таргета", "Выбирает тип цели esp")
            .value("Cube", "Circle", "Ghosts", "Crystals", "Runes")
            .selected("Circle");
    SelectSetting cubeType = new SelectSetting("Картинка для куба", "Выбирает тип куба")
            .value("1", "2", "3", "4", "5")
            .visible(() -> targetEspType.isSelected("Cube"));
    public ColorSetting colorSetting = new ColorSetting("Цвет", "Выберите цвет для esp")
            .setColor(new Color(255, 101, 57, 255).getRGB());

    public TargetESP() {
        super("TargetEsp", "Target Esp", ModuleCategory.RENDER);
        setup(targetEspType, cubeType, colorSetting);
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.POST) {
            Render3D.updateTargetEsp();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        StrikeManager attackHandler = Zyrex.getInstance().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();

        LivingEntity currentTarget = null;
        LivingEntity lastTarget = null;

        if (Aura.getInstance().isState()) {
            currentTarget = Aura.getInstance().getTarget();
            lastTarget = Aura.getInstance().getLastTarget();
        } else if (TriggerBot.getInstance().isState()) {
            currentTarget = TriggerBot.getInstance().target;
            lastTarget = TriggerBot.getInstance().target;
        }

        esp_anim.setDirection(currentTarget != null ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = esp_anim.getOutput().floatValue();

        if (lastTarget != null && !esp_anim.isFinished(Direction.BACKWARDS)) {
            float red = MathHelper.clamp((lastTarget.hurtTime - tickCounter.getTickDelta(false)) / 20, 0, 1);
            switch (targetEspType.getSelected()) {
                case "Cube" -> Render3D.drawCube(lastTarget, anim, red, cubeType.getSelected());
                case "Circle" -> Render3D.drawCircle(e.getStack(), lastTarget, anim, red);
                case "Ghosts" -> Render3D.drawGhosts(lastTarget, anim, red, 0.62F);
                case "Crystals" -> {
                    if (crystalList.isEmpty() || lastTarget != lastRenderedTarget) {
                        createCrystals(lastTarget);
                        lastRenderedTarget = lastTarget;
                    }
                    renderCrystals(e.getStack(), lastTarget, anim, red);
                }
                case "Runes" -> {
                    if (runeList.isEmpty() || lastTarget != lastRenderedRuneTarget) {
                        createRunes(lastTarget);
                        lastRenderedRuneTarget = lastTarget;
                    }
                    renderRunes(e.getStack(), lastTarget, anim, red);
                }
            }
        }
    }
    private Entity lastRenderedTarget = null;
    private final List<Crystal> crystalList = new ArrayList<>();
    private float rotationAngle = 0;
    private Entity lastRenderedRuneTarget = null;
    private final List<Rune> runeList = new ArrayList<>();
    private float runeRotationAngle = 0;
    private static class Crystal {
        private final Entity entity;
        private final Vec3d position;
        private final Vec3d rotation;
        private final float size;
        private final float rotationSpeed;

        public Crystal(Entity entity, Vec3d position, Vec3d rotation) {
            this.entity = entity;
            this.position = position;
            this.rotation = rotation;
            this.size = 0.05f;
            this.rotationSpeed = 0.5f + (float)(Math.random() * 1.5f);
        }

        public void render(MatrixStack ms, float anim, float red, Camera camera) {
            ms.push();
            ms.translate(position.x, position.y, position.z);
            float pulsation = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.1f);
            ms.scale(pulsation, pulsation, pulsation);
            float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            int baseColor = ColorAssist.interpolateColor(TargetESP.getInstance().colorSetting.getColor(), new Color(255, 0, 0).getRGB(), red);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, baseColor, 0.2f, true, anim);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, baseColor, 0.3f, true, anim);
            drawCrystal(ms, baseColor, 0.8f, false, anim);
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            ms.push();
            ms.scale(1.2f, 1.2f, 1.2f);
            drawCrystal(ms, baseColor, 0.3f, true, anim);
            ms.pop();
            drawBloomSphere(ms, baseColor, anim, camera);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        }

        private void drawBloomSphere(MatrixStack ms, int baseColor, float anim, Camera camera) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, Identifier.of("textures/features/particles/bloom.png"));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.depthMask(false);
            int bloomColor = ColorAssist.setAlpha(baseColor, (int) (0.4f * 25 * anim));
            float bloomSize = size * 13.0f;
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            int segments = 6;
            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }
            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }
            RenderSystem.depthMask(true);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled, float anim) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
                    filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );
            float s = size;
            float h_prism = size * 1f;
            float h_pyramid = size * 1.5f;
            int numSides = 8;
            List<Vec3d> topVertices = new ArrayList<>();
            List<Vec3d> bottomVertices = new ArrayList<>();
            for (int i = 0; i < numSides; i++) {
                float angle = (float) (2 * Math.PI * i / numSides);
                float x = (float) (s * Math.cos(angle));
                float z = (float) (s * Math.sin(angle));
                topVertices.add(new Vec3d(x, h_prism / 2, z));
                bottomVertices.add(new Vec3d(x, -h_prism / 2, z));
            }
            Vec3d vTop = new Vec3d(0, h_prism / 2 + h_pyramid, 0);
            Vec3d vBottom = new Vec3d(0, -h_prism / 2 - h_pyramid, 0);
            int finalColor = ColorAssist.setAlpha(baseColor, (int) (alpha * 255 * anim));
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                Vec3d v3 = topVertices.get((i + 1) % numSides);
                Vec3d v4 = topVertices.get(i);
                drawQuad(ms, bufferBuilder, v1, v2, v3, v4, finalColor, filled);
            }
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = topVertices.get(i);
                Vec3d v2 = topVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vTop, v1, v2, finalColor, filled);
            }
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vBottom, v2, v1, finalColor, filled);
            }
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        private void drawTriangle(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
            if (filled) {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            }
        }

        private void drawQuad(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color, boolean filled) {
            if (filled) {
                drawTriangle(ms, bb, v1, v2, v3, color, true);
                drawTriangle(ms, bb, v1, v3, v4, color, true);
            } else {
                bb.vertex(ms.peek().getPositionMatrix(), 100, 100, (float)v1.z).color(color);
            }
        }
    }

    private record RuneSymbol(String name, float[][] points, int[][] lines) {}

    private static final RuneSymbol HEXAGRAM = new RuneSymbol("Hexagram",
        new float[][]{{0, 1}, {0.866f, 0.5f}, {0.866f, -0.5f}, {0, -1}, {-0.866f, -0.5f}, {-0.866f, 0.5f}},
        new int[][]{{0, 2}, {2, 4}, {4, 0}, {1, 3}, {3, 5}, {5, 1}}
    );
    private static final RuneSymbol PENTAGRAM = new RuneSymbol("Pentagram",
        new float[][]{{0, 1}, {0.951f, 0.309f}, {0.588f, -0.809f}, {-0.588f, -0.809f}, {-0.951f, 0.309f}},
        new int[][]{{0, 2}, {2, 4}, {4, 1}, {1, 3}, {3, 0}}
    );
    private static final RuneSymbol OCTAGRAM = new RuneSymbol("Octagram",
        new float[][]{{0, 1}, {0.707f, 0.707f}, {1, 0}, {0.707f, -0.707f}, {0, -1}, {-0.707f, -0.707f}, {-1, 0}, {-0.707f, 0.707f}},
        new int[][]{{0, 3}, {3, 6}, {6, 1}, {1, 4}, {4, 7}, {7, 2}, {2, 5}, {5, 0}}
    );
    private static final RuneSymbol DIAMOND = new RuneSymbol("Diamond",
        new float[][]{{0, 1}, {0.707f, 0}, {0, -1}, {-0.707f, 0}},
        new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {0, 2}, {1, 3}}
    );
    private static final RuneSymbol[] RUNE_SYMBOLS = {HEXAGRAM, PENTAGRAM, OCTAGRAM, DIAMOND};

    private static class Rune {
        private final Entity entity;
        private final RuneSymbol symbol;
        private final Vec3d position;
        private final float size;
        private final float rotationSpeed;
        private final float bobSpeed;
        private final float bobOffset;

        public Rune(Entity entity, RuneSymbol symbol, Vec3d position) {
            this.entity = entity;
            this.symbol = symbol;
            this.position = position;
            this.size = 0.17f;
            this.rotationSpeed = 0.3f + (float)(Math.random() * 0.6f);
            this.bobSpeed = 0.6f + (float)(Math.random() * 0.4f);
            this.bobOffset = (float)(Math.random() * Math.PI * 2);
        }

        public void render(MatrixStack ms, float anim, float red, Camera camera) {
            ms.push();
            float bob = (float) (Math.sin(System.currentTimeMillis() / 600.0 * bobSpeed + bobOffset) * 0.08f);
            ms.translate(position.x, position.y + bob, position.z);

            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(selfRotation));

            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

            int baseColor = ColorAssist.interpolateColor(TargetESP.getInstance().colorSetting.getColor(), new Color(255, 0, 0).getRGB(), red);
            float glowPulse = 0.85f + (float) (Math.sin(System.currentTimeMillis() / 400.0 + bobOffset) * 0.15f);
            int lineColor = ColorAssist.setAlpha(baseColor, (int) (0.8f * 255 * anim * glowPulse));

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

            Matrix4f mat = ms.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int[] line : symbol.lines()) {
                float x1 = symbol.points()[line[0]][0] * size;
                float y1 = symbol.points()[line[0]][1] * size;
                float x2 = symbol.points()[line[1]][0] * size;
                float y2 = symbol.points()[line[1]][1] * size;
                buffer.vertex(mat, x1, y1, 0).color(lineColor);
                buffer.vertex(mat, x2, y2, 0).color(lineColor);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, Identifier.of("textures/features/particles/bloom.png"));
            int bloomColor = ColorAssist.setAlpha(baseColor, (int) (0.4f * 25 * anim * glowPulse));
            float bloomSize = size * 0.9f;
            Matrix4f bloomMat = ms.peek().getPositionMatrix();
            BufferBuilder bloomBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bloomBuffer.vertex(bloomMat, -bloomSize, -bloomSize, 0).texture(0, 1).color(bloomColor);
            bloomBuffer.vertex(bloomMat, bloomSize, -bloomSize, 0).texture(1, 1).color(bloomColor);
            bloomBuffer.vertex(bloomMat, bloomSize, bloomSize, 0).texture(1, 0).color(bloomColor);
            bloomBuffer.vertex(bloomMat, -bloomSize, bloomSize, 0).texture(0, 0).color(bloomColor);
            BufferRenderer.drawWithGlobalProgram(bloomBuffer.end());

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        }
    }

    private void createCrystals(Entity target) {
        crystalList.clear();
        crystalList.add(new Crystal(target, new Vec3d(0, 0.85, 0.8), new Vec3d(-49, 0, 40)));
        crystalList.add(new Crystal(target, new Vec3d(0.2, 0.85, -0.675), new Vec3d(35, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.6, 1.35, 0.6), new Vec3d(-30, 0, 35)));
        crystalList.add(new Crystal(target, new Vec3d(-0.74, 1.05, 0.4), new Vec3d(-25, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.74, 0.95, -0.4), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.475, 0.85, -0.375), new Vec3d(30, 0, -25)));
        crystalList.add(new Crystal(target, new Vec3d(0, 1.35, -0.6), new Vec3d(45, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(0.85, 0.7, 0.1), new Vec3d(-30, 0, 30)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 1.35, -0.3), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.3, 1.35, 0.55), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 0.75, 0), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.2, 0.65, -0.7), new Vec3d(0, 0, 0)));
    }

    private void renderCrystals(MatrixStack ms, Entity target, float anim, float red) {
        if (target == null || crystalList.isEmpty()) {
            return;
        }
        RenderSystem.enableDepthTest();
        Vec3d targetPos = CalcVector.lerpPosition(target);
        rotationAngle = (rotationAngle + 0.5f) % 360;
        ms.push();
        ms.translate(targetPos.x, targetPos.y, targetPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));
        Camera camera = mc.gameRenderer.getCamera();
        for (Crystal crystal : crystalList) {
            crystal.render(ms, anim, red, camera);
        }
        ms.pop();
        RenderSystem.enableDepthTest();
    }

    private void createRunes(Entity target) {
        runeList.clear();
        float[] heights = {0.3f, 0.85f, 1.4f};
        float[] radii = {0.7f, 0.8f, 0.7f};
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 3; i++) {
                float angle = (float) (2 * Math.PI * i / 3 + ring * 0.5f);
                float x = (float) Math.cos(angle) * radii[ring];
                float z = (float) Math.sin(angle) * radii[ring];
                RuneSymbol symbol = RUNE_SYMBOLS[(int) (Math.random() * RUNE_SYMBOLS.length)];
                runeList.add(new Rune(target, symbol, new Vec3d(x, heights[ring], z)));
            }
        }
    }

    private void renderRunes(MatrixStack ms, Entity target, float anim, float red) {
        if (target == null || runeList.isEmpty()) return;
        RenderSystem.enableDepthTest();
        Vec3d targetPos = CalcVector.lerpPosition(target);
        runeRotationAngle = (runeRotationAngle + 0.4f) % 360;
        ms.push();
        ms.translate(targetPos.x, targetPos.y, targetPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(runeRotationAngle));
        Camera camera = mc.gameRenderer.getCamera();
        for (Rune rune : runeList) {
            rune.render(ms, anim, red, camera);
        }
        ms.pop();
        RenderSystem.enableDepthTest();
    }

}