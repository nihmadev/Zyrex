package lol.zyrex.features.impl.render;

import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.client.Instance;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.*;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.common.repository.friend.FriendUtils;
import lol.zyrex.utils.display.font.FontRenderer;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.events.render.WorldRenderEvent;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.display.geometry.Render3D;
import lol.zyrex.utils.client.packet.network.Network;
import lol.zyrex.utils.math.projection.Projection;
import lol.zyrex.utils.display.geometry.Render2D;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.events.render.DrawEvent;
import lol.zyrex.events.render.WorldLoadEvent;
import lol.zyrex.features.impl.combat.AntiBot;
import lol.zyrex.common.repository.FakePlayerRepository;
import lol.zyrex.utils.features.skeleton.BoneData;
import lol.zyrex.utils.features.skeleton.SkeletonBoneStorage;
import lol.zyrex.utils.math.calc.Calculate;

import java.awt.*;
import java.lang.Math;
import java.util.*;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Esp extends Module {
    public static Esp getInstance() {
        return Instance.get(Esp.class);
    }
    Identifier TEXTURE = Identifier.of("textures/features/esp/container.png");
    List<PlayerEntity> players = new ArrayList<>();
    List<LivingEntity> mobs = new ArrayList<>();
    Map<RegistryKey<Enchantment>, String> encMap = new HashMap<>();

    public MultiSelectSetting entityType = new MultiSelectSetting("Тип сущности", "Сущности, которые будут отображаться")
            .value("Player", "Item", "TNT", "Mob").selected("Player", "Item");
    MultiSelectSetting playerSetting = new MultiSelectSetting("Настройки игрока", "Настройки для игроков")
            .value("Box", "Armor", "NameTags", "Hand Items", "Health").selected("Box", "Armor", "NameTags", "Hand Items", "Health").visible(() -> entityType.isSelected("Player"));
    public BooleanSetting mobHealth = new BooleanSetting("Здоровье мобов", "Отображать полоску здоровья мобов").setValue(true).visible(() -> entityType.isSelected("Mob"));
    public SelectSetting boxType = new SelectSetting("Тип", "Тип")
            .value("Corner", "Full", "3D Box", "Skeleton").selected("3D Box");
    public BooleanSetting flatBoxOutline = new BooleanSetting("Контур", "Контур для плоских боксов").visible(() -> boxType.isSelected("Corner") || boxType.isSelected("Full"));
    public SliderSettings boxAlpha = new SliderSettings("Прозрачность", "Прозрачность бокса")
            .setValue(1.0F).range(0.1F, 1.0F).visible(() -> boxType.isSelected("3D Box"));
    public SliderSettings skeletonWidth = new SliderSettings("Толщина линий", "Толщина линий скелета")
            .setValue(2.5f).range(2.5f, 4.0f).visible(() -> boxType.isSelected("Skeleton"));

    private static final float DISTANCE = 128.0f;

    public Esp() {
        super("Esp", "Esp", ModuleCategory.RENDER);
        setup(entityType, playerSetting, boxType, flatBoxOutline, boxAlpha, skeletonWidth, mobHealth);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
        mobs.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        players.clear();
        mobs.clear();
        if (mc.world != null) {
            mc.world.getPlayers().stream()
                    .filter(player -> player != mc.player)
                    .filter(player -> player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))
                    .filter(player -> {
                        if (mc.getNetworkHandler() == null) return true;
                        if (FakePlayerRepository.isFake(player.getUuid())) return true;
                        return mc.getNetworkHandler().getPlayerList().stream()
                                .anyMatch(entry -> entry.getProfile() != null && entry.getProfile().getId().equals(player.getUuid()));
                    })
                    .forEach(players::add);
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof LivingEntity living && !(entity instanceof PlayerEntity)) {
                    if (entity == mc.player) continue;
                    if (entity instanceof ArmorStandEntity) continue;
                    if (entity.getCustomName() != null && entity.getCustomName().getString().startsWith("Ghost_")) continue;
                    if (living.isInvisible() && living.hasCustomName()) continue;
                    if (living.squaredDistanceTo(mc.player) > 128 * 128) continue;
                    mobs.add(living);
                }
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        if (entityType.isSelected("Player")) {
        for (PlayerEntity player : players) {
            if (player == null) continue;
            if (player == mc.player) continue;
            if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;
            double interpX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
            double interpY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
            double interpZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
            Vec3d interpCenter = new Vec3d(interpX, interpY, interpZ);
            float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(interpCenter);
            if (distance < 1) continue;
            boolean friend = FriendUtils.isFriend(player);
            int baseColor = friend ? ColorAssist.getFriendColor() : ColorAssist.getClientColor();
            int alpha = (int) (boxAlpha.getValue() * 255);
            int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
            int outlineColor = baseColor | 0xFF000000;

            if (boxType.isSelected("3D Box")) {
                Box interpBox = player.getDimensions(player.getPose()).getBoxAt(interpX, interpY, interpZ);
                Render3D.drawBox(interpBox, fillColor, 2, true, true, false);
                Render3D.drawBox(interpBox, outlineColor, 2, true, true, false);
            } else if (boxType.isSelected("Skeleton")) {
                if (distance > DISTANCE) continue;
                renderSkeleton(player, tickDelta, baseColor);
            }
        }
        }
        if (entityType.isSelected("Mob")) {
            int rendered = 0;
            for (LivingEntity mob : mobs) {
                if (mob == null) continue;
                if (rendered > 48) break;
                if (boxType.isSelected("3D Box")) {
                    Box interpBox = mob.getBoundingBox();
                    int mobColor = ColorAssist.getFriendColor();
                    int mobFill = (mobColor & 0x00FFFFFF) | (55 << 24);
                    int mobOutline = mobColor | 0xFF000000;
                    Render3D.drawBox(interpBox, mobFill, 2, true, true, false);
                    Render3D.drawBox(interpBox, mobOutline, 2, true, true, false);
                } else if (boxType.isSelected("Skeleton")) {
                    renderSkeleton(mob, tickDelta, ColorAssist.getFriendColor());
                }
                rendered++;
            }
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.SEMI);
        FontRenderer bigFont = Fonts.getSize(13 + 2, Fonts.Type.SEMI);
        if (entityType.isSelected("Player")) {
            for (PlayerEntity player : players) {
                if (player == null) continue;
                if (player == mc.player) continue;
                if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;
                Vector4d vec4d = Projection.getVector4D(player);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(player.getBoundingBox().getCenter());
                boolean friend = FriendUtils.isFriend(player);
                if (distance < 1) continue;
                if (Projection.cantSee(vec4d)) continue;
                if (playerSetting.isSelected("Box") && !boxType.isSelected("Skeleton")) drawBox(friend, vec4d, player);
                if (playerSetting.isSelected("Armor")) drawArmor(context, player, vec4d, font);
                if (playerSetting.isSelected("Hand Items")) drawHands(matrix, player, font, vec4d);
                MutableText text = getTextPlayer(player, friend);
                if (playerSetting.isSelected("Health")) {
                    float hp = PlayerInteractionHelper.getHealth(player);
                    if (hp >= 0 && hp <= player.getMaxHealth())
                        drawHealthBar(vec4d, hp, player.getMaxHealth());
                }
                if (Network.isAresMine()) {
                    float startX = (float) Projection.centerX(vec4d);
                    float startY = (float) (vec4d.y);
                    float textWidth = mc.textRenderer.getWidth(text);
                    float textHeight = mc.textRenderer.fontHeight;
                    float headSize = textHeight;
                    float gap = 1.5F;
                    float totalWidth = headSize + gap + textWidth;
                    float posX = startX - totalWidth / 2f;
                    float posY = startY - 11F;
                    blur.render(ShapeProperties.create(matrix,
                                    posX - 2f,
                                    posY - 0.75f,
                                    totalWidth + 4f,
                                    textHeight + 1.5f).quality(5).round(4f).softness(1)
                            .round(textHeight / 4f)
                            .color(ColorAssist.HALF_BLACK)
                            .build());
                    Render2D.drawTexture(context, getSkinTexture(player), posX, posY, headSize, 1f, 8, 8, 64, ColorAssist.getRect(1), -1);
                    context.drawText(mc.textRenderer, text, (int)(posX + headSize + gap), (int)posY + 1, ColorAssist.getColor(255), false);
                } else {
                    drawTextWithHead(context, matrix, text, player, Projection.centerX(vec4d), vec4d.y - 2, font);
                }
            }
        }
        List<Entity> entities = PlayerInteractionHelper.streamEntities()
                .filter(ent -> ent instanceof ItemEntity || ent instanceof TntEntity)
                .sorted(Comparator.comparing(ent -> ent instanceof ItemEntity item && item.getStack().getName().getContent().toString().equals("empty")))
                .limit(256)
                .toList();
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity item && entityType.isSelected("Item")) {
                Vector4d vec4d = Projection.getVector4D(entity);
                ItemStack stack = item.getStack();
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
                List<ItemStack> list = compoundTag != null ? compoundTag.stream().toList() : List.of();
                if (Projection.cantSee(vec4d)) continue;
                if (boxType.isSelected("Corner") || boxType.isSelected("Full")) {
                    drawFlatBox(vec4d, ColorAssist.getClientColor());
                }
                Text text = item.getStack().getName();
                if (stack.getCount() > 1) text = text.copy().append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                if (!list.isEmpty()) drawShulkerBox(context, stack, list, vec4d);
                else drawText(matrix, text, Projection.centerX(vec4d), vec4d.y, text.getContent().toString().equals("empty") ? bigFont : font);
            } else if (entity instanceof TntEntity tnt && entityType.isSelected("TNT")) {
                Vector4d vec4d = Projection.getVector4D(entity);
                if (Projection.cantSee(vec4d)) continue;
                if (boxType.isSelected("Corner") || boxType.isSelected("Full")) {
                    drawFlatBox(vec4d, ColorAssist.getClientColor());
                }
                drawText(matrix, tnt.getStyledDisplayName(), Projection.centerX(vec4d), vec4d.y, font);
            }
        }
        if (entityType.isSelected("Mob")) {
            int rendered = 0;
            for (LivingEntity mob : mobs) {
                if (mob == null) continue;
                if (rendered > 48) break;
                Vector4d vec4d = Projection.getVector4D(mob);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(mob.getBoundingBox().getCenter());
                if (distance < 1) continue;
                if (Projection.cantSee(vec4d)) continue;
                if (boxType.isSelected("Corner") || boxType.isSelected("Full")) {
                    drawFlatBox(vec4d, ColorAssist.getFriendColor());
                }
                MutableText text = Text.literal("").append(mob.getDisplayName());
                drawText(matrix, text, Projection.centerX(vec4d), vec4d.y, font);
                if (mobHealth.isValue()) {
                    float hp = PlayerInteractionHelper.getHealth(mob);
                    if (hp >= 0 && hp <= mob.getMaxHealth())
                        drawHealthBar(vec4d, hp, mob.getMaxHealth());
                }
                rendered++;
            }
        }
    }

    private void drawHealthBar(Vector4d vec, float health, float maxHealth) {
        float barWidth = 2.5f;
        float barX = (float) (vec.x - barWidth - 1.5f);
        float barY = (float) vec.y;
        float barHeight = (float) (vec.w - vec.y);

        if (!Float.isFinite(barX) || !Float.isFinite(barY) || !Float.isFinite(barWidth) || !Float.isFinite(barHeight)) return;
        if (barHeight < 1) return;

        float ratio = MathHelper.clamp(health / maxHealth, 0, 1);

        Render2D.drawQuad(barX, barY, barWidth, barHeight, new Color(0, 0, 0, 180).getRGB());

        if (ratio > 0.01f) {
            float fillHeight = barHeight * ratio;
            float fillY = barY + barHeight - fillHeight;
            int barColor = Color.HSBtoRGB(ratio * 0.33f, 0.85f, 1.0f) | 0xFF000000;
            Render2D.drawQuad(barX + 0.5f, fillY + 0.5f, barWidth - 1, fillHeight - 1, barColor);
        }
    }

    private void renderSkeleton(LivingEntity entity, float partialTicks, int color) {
        Map<String, BoneData> bones = SkeletonBoneStorage.get(entity.getUuid());
        if (bones.isEmpty() || !bones.containsKey("right_leg") || !bones.containsKey("left_leg")) {
            renderSkeletonLegacy(entity, partialTicks, color);
            return;
        }

        BoneData headBD = bones.get("head");
        BoneData bodyBD = bones.get("body");
        BoneData rightArmBD = bones.get("right_arm");
        BoneData leftArmBD = bones.get("left_arm");
        BoneData rightLegBD = bones.get("right_leg");
        BoneData leftLegBD = bones.get("left_leg");

        if (headBD == null || bodyBD == null || rightArmBD == null || leftArmBD == null || rightLegBD == null || leftLegBD == null) {
            renderSkeletonLegacy(entity, partialTicks, color);
            return;
        }

        Vec3d head = headBD.position;
        Vec3d body = bodyBD.position;
        Vec3d rightArm = rightArmBD.position;
        Vec3d leftArm = leftArmBD.position;
        Vec3d rightLeg = rightLegBD.position;
        Vec3d leftLeg = leftLegBD.position;

        float width = skeletonWidth.getValue();
        Vec3d entityPos = Calculate.interpolate(entity);

        // Spine: head -> body -> pelvis
        Vec3d pelvis = new Vec3d(
                (rightLeg.x + leftLeg.x) / 2,
                (body.y + (rightLeg.y + leftLeg.y) / 2) / 2,
                (rightLeg.z + leftLeg.z) / 2
        );
        Render3D.drawLine(head, body, color, width, false);
        Render3D.drawLine(body, pelvis, color, width, false);

        // Shoulders: body -> arm pivots
        Render3D.drawLine(body, rightArm, color, width, false);
        Render3D.drawLine(body, leftArm, color, width, false);

        // Arms: shoulder -> hand with real rotation
        float armLen = Math.max(
                (float)(rightArm.y - (rightLeg.y + leftLeg.y) / 2) * 0.7f,
                0.3f
        );
        Vec3d rightHand = applyBoneRotation(rightArm, rightArmBD, armLen);
        Vec3d leftHand = applyBoneRotation(leftArm, leftArmBD, armLen);
        Render3D.drawLine(rightArm, rightHand, color, width, false);
        Render3D.drawLine(leftArm, leftHand, color, width, false);

        // Hips: pelvis -> leg pivots
        Render3D.drawLine(pelvis, rightLeg, color, width, false);
        Render3D.drawLine(pelvis, leftLeg, color, width, false);

        // Legs: hip -> foot with real rotation
        float legLen = Math.max((float)(rightLeg.y - entityPos.y), 0.3f);
        Vec3d rightFoot = applyBoneRotation(rightLeg, rightLegBD, legLen);
        Vec3d leftFoot = applyBoneRotation(leftLeg, leftLegBD, legLen);
        Render3D.drawLine(rightLeg, rightFoot, color, width, false);
        Render3D.drawLine(leftLeg, leftFoot, color, width, false);
    }

    private Vec3d applyBoneRotation(Vec3d pivot, BoneData bone, float length) {
        Vector3f dir = new Vector3f(0, -1, 0);
        Quaternionf quat = new Quaternionf()
                .rotationZ(bone.roll)
                .mul(new Quaternionf().rotationX(bone.pitch))
                .mul(new Quaternionf().rotationY(bone.yaw));
        dir.rotate(quat);
        return new Vec3d(
                pivot.x + dir.x * length,
                pivot.y + dir.y * length,
                pivot.z + dir.z * length
        );
    }

    private void renderSkeletonLegacy(LivingEntity entity, float partialTicks, int color) {
        Vec3d pos = Calculate.interpolate(entity);
        float width = skeletonWidth.getValue();

        float limbSwing = entity.limbAnimator.getPos(partialTicks);
        double dx = entity.getX() - entity.prevX;
        double dz = entity.getZ() - entity.prevZ;
        float movement = MathHelper.sqrt((float)(dx * dx + dz * dz));
        float limbSwingAmount = MathHelper.clamp(movement * 15f, 0f, 1f);
        if (limbSwing == 0 && movement > 0.001f) {
            limbSwing = (entity.age + partialTicks) * 0.1f;
        }

        float bodyYaw = MathHelper.lerpAngleDegrees(partialTicks, entity.prevBodyYaw, entity.bodyYaw);
        float bodyYawRad = (float) Math.toRadians(-bodyYaw + 90);

        boolean isSwimming = entity.isSwimming() || entity.isGliding();
        boolean isSitting = entity.hasVehicle();
        boolean isSneaking = entity.isSneaking();

        float scale = entity.getHeight() / 1.8f;
        if (scale < 0.1f || scale > 5f || !Float.isFinite(scale)) scale = 1f;

        float sneakOffset = isSneaking ? 0.2f * scale : 0f;
        float swimOffset = isSwimming ? 0.6f * scale : 0f;
        float sitOffset = isSitting ? 0.35f * scale : 0f;

        float totalOffset = sneakOffset + swimOffset + sitOffset;

        Vec3d head = pos.add(0, 1.62f * scale - totalOffset, 0);
        Vec3d neck = pos.add(0, 1.4f * scale - totalOffset, 0);
        Vec3d body = pos.add(0, 0.9f * scale - totalOffset, 0);
        Vec3d pelvis = pos.add(0, 0.6f * scale - totalOffset, 0);

        Render3D.drawLine(head, neck, color, width, false);
        Render3D.drawLine(neck, body, color, width, false);
        Render3D.drawLine(body, pelvis, color, width, false);

        float sinYaw = (float) Math.sin(bodyYawRad);
        float cosYaw = (float) Math.cos(bodyYawRad);

        Vec3d rightShoulder = neck.add(sinYaw * 0.3 * scale, -0.1 * scale, cosYaw * 0.3 * scale);
        Vec3d leftShoulder = neck.add(-sinYaw * 0.3 * scale, -0.1 * scale, -cosYaw * 0.3 * scale);

        if (isSitting) {
            Vec3d rightElbow = rightShoulder.add(0, -0.3 * scale, 0);
            Vec3d rightHand = rightElbow.add(0, -0.25 * scale, 0);
            Vec3d leftElbow = leftShoulder.add(0, -0.3 * scale, 0);
            Vec3d leftHand = leftElbow.add(0, -0.25 * scale, 0);

            Render3D.drawLine(rightShoulder, rightElbow, color, width, false);
            Render3D.drawLine(rightElbow, rightHand, color, width, false);
            Render3D.drawLine(leftShoulder, leftElbow, color, width, false);
            Render3D.drawLine(leftElbow, leftHand, color, width, false);

            Vec3d rightHip = pelvis.add(sinYaw * 0.15 * scale, 0, cosYaw * 0.15 * scale);
            Vec3d leftHip = pelvis.add(-sinYaw * 0.15 * scale, 0, -cosYaw * 0.15 * scale);

            Vec3d rightKnee = rightHip.add(-cosYaw * 0.2 * scale, -0.1 * scale, sinYaw * 0.2 * scale);
            Vec3d rightFoot = rightKnee.add(0, -0.3 * scale, 0);
            Vec3d leftKnee = leftHip.add(-cosYaw * 0.2 * scale, -0.1 * scale, sinYaw * 0.2 * scale);
            Vec3d leftFoot = leftKnee.add(0, -0.3 * scale, 0);

            Render3D.drawLine(rightHip, rightKnee, color, width, false);
            Render3D.drawLine(rightKnee, rightFoot, color, width, false);
            Render3D.drawLine(leftHip, leftKnee, color, width, false);
            Render3D.drawLine(leftKnee, leftFoot, color, width, false);
        } else {
            float rightArmSwing = MathHelper.cos(limbSwing * 0.6662f) * limbSwingAmount * 0.5f;
            float leftArmSwing = MathHelper.cos(limbSwing * 0.6662f + (float)Math.PI) * limbSwingAmount * 0.5f;
            float rightLegSwing = MathHelper.cos(limbSwing * 0.6662f + (float)Math.PI) * limbSwingAmount * 0.7f;
            float leftLegSwing = MathHelper.cos(limbSwing * 0.6662f) * limbSwingAmount * 0.7f;

            Vec3d rightElbow = rightShoulder.add(
                    sinYaw * 0.05 * scale + Math.sin(bodyYawRad + Math.PI/2) * rightArmSwing * 0.15 * scale,
                    -0.25 * scale - Math.abs(rightArmSwing) * 0.1 * scale,
                    cosYaw * 0.05 * scale + Math.cos(bodyYawRad + Math.PI/2) * rightArmSwing * 0.15 * scale
            );

            Vec3d rightHand = rightElbow.add(
                    Math.sin(bodyYawRad + Math.PI/2) * rightArmSwing * 0.1 * scale,
                    -0.25 * scale - Math.abs(rightArmSwing) * 0.05 * scale,
                    Math.cos(bodyYawRad + Math.PI/2) * rightArmSwing * 0.1 * scale
            );

            Render3D.drawLine(rightShoulder, rightElbow, color, width, false);
            Render3D.drawLine(rightElbow, rightHand, color, width, false);

            Vec3d leftElbow = leftShoulder.add(
                    -sinYaw * 0.05 * scale + Math.sin(bodyYawRad + Math.PI/2) * leftArmSwing * 0.15 * scale,
                    -0.25 * scale - Math.abs(leftArmSwing) * 0.1 * scale,
                    -cosYaw * 0.05 * scale + Math.cos(bodyYawRad + Math.PI/2) * leftArmSwing * 0.15 * scale
            );

            Vec3d leftHand = leftElbow.add(
                    Math.sin(bodyYawRad + Math.PI/2) * leftArmSwing * 0.1 * scale,
                    -0.25 * scale - Math.abs(leftArmSwing) * 0.05 * scale,
                    Math.cos(bodyYawRad + Math.PI/2) * leftArmSwing * 0.1 * scale
            );

            Render3D.drawLine(leftShoulder, leftElbow, color, width, false);
            Render3D.drawLine(leftElbow, leftHand, color, width, false);

            Vec3d rightHip = pelvis.add(sinYaw * 0.15 * scale, 0, cosYaw * 0.15 * scale);

            Vec3d rightKnee = rightHip.add(
                    Math.sin(bodyYawRad + Math.PI/2) * rightLegSwing * 0.1 * scale,
                    -0.35 * scale + Math.max(0, rightLegSwing) * 0.05 * scale,
                    Math.cos(bodyYawRad + Math.PI/2) * rightLegSwing * 0.1 * scale
            );

            Vec3d rightFoot = rightKnee.add(
                    Math.sin(bodyYawRad + Math.PI/2) * rightLegSwing * 0.08 * scale,
                    -0.35 * scale - Math.max(0, -rightLegSwing) * 0.05 * scale,
                    Math.cos(bodyYawRad + Math.PI/2) * rightLegSwing * 0.08 * scale
            );

            Render3D.drawLine(rightHip, rightKnee, color, width, false);
            Render3D.drawLine(rightKnee, rightFoot, color, width, false);

            Vec3d leftHip = pelvis.add(-sinYaw * 0.15 * scale, 0, -cosYaw * 0.15 * scale);

            Vec3d leftKnee = leftHip.add(
                    Math.sin(bodyYawRad + Math.PI/2) * leftLegSwing * 0.1 * scale,
                    -0.35 * scale + Math.max(0, leftLegSwing) * 0.05 * scale,
                    Math.cos(bodyYawRad + Math.PI/2) * leftLegSwing * 0.1 * scale
            );

            Vec3d leftFoot = leftKnee.add(
                    Math.sin(bodyYawRad + Math.PI/2) * leftLegSwing * 0.08 * scale,
                    -0.35 * scale - Math.max(0, -leftLegSwing) * 0.05 * scale,
                    Math.cos(bodyYawRad + Math.PI/2) * leftLegSwing * 0.08 * scale
            );

            Render3D.drawLine(leftHip, leftKnee, color, width, false);
            Render3D.drawLine(leftKnee, leftFoot, color, width, false);
        }

        Render3D.drawLine(rightShoulder, leftShoulder, color, width, false);
        Render3D.drawLine(
                pelvis.add(sinYaw * 0.15 * scale, 0, cosYaw * 0.15 * scale),
                pelvis.add(-sinYaw * 0.15 * scale, 0, -cosYaw * 0.15 * scale),
                color, width, false
        );
    }

    private void drawBox(boolean friend, Vector4d vec, PlayerEntity player) {
        if (!boxType.isSelected("Corner") && !boxType.isSelected("Full")) {
            return;
        }
        int client = friend ? ColorAssist.getFriendColor() : ColorAssist.getClientColor();
        drawFlatBox(vec, client);
    }

    private void drawFlatBox(Vector4d vec, int color) {
        int black = ColorAssist.HALF_BLACK;
        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;

        if (!Float.isFinite(posX) || !Float.isFinite(posY) || !Float.isFinite(endPosX) || !Float.isFinite(endPosY)) return;
        float boxWidth = endPosX - posX;
        float boxHeight = endPosY - posY;
        if (boxWidth > 2000 || boxHeight > 2000) return;
        if (boxWidth < 1 || boxHeight < 1) return;

        float size = (endPosX - posX) / 3;
        if (boxType.isSelected("Corner")) {
            Render2D.drawQuad(posX - 0.5F, posY - 0.5F, size, 0.5F, color);
            Render2D.drawQuad(posX - 0.5F, posY, 0.5F, size + 0.5F, color);
            Render2D.drawQuad(posX - 0.5F, endPosY - size - 0.5F, 0.5F, size, color);
            Render2D.drawQuad(posX - 0.5F, endPosY - 0.5F, size, 0.5F, color);
            Render2D.drawQuad(endPosX - size + 1, posY - 0.5F, size, 0.5F, color);
            Render2D.drawQuad(endPosX + 0.5F, posY, 0.5F, size + 0.5F, color);
            Render2D.drawQuad(endPosX + 0.5F, endPosY - size - 0.5F, 0.5F, size, color);
            Render2D.drawQuad(endPosX - size + 1, endPosY - 0.5F, size, 0.5F, color);
            if (flatBoxOutline.isValue()) {
                Render2D.drawQuad(posX - 1F, posY - 1, size + 1, 1.5F, black);
                Render2D.drawQuad(posX - 1F, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2D.drawQuad(posX - 1F, endPosY - size - 1, 1.5F, size, black);
                Render2D.drawQuad(posX - 1F, endPosY - 1, size + 1, 1.5F, black);
                Render2D.drawQuad(endPosX - size + 0.5F, posY - 1, size + 1, 1.5F, black);
                Render2D.drawQuad(endPosX, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2D.drawQuad(endPosX, endPosY - size - 1, 1.5F, size, black);
                Render2D.drawQuad(endPosX - size + 0.5F, endPosY - 1, size + 1, 1.5F, black);
            }
        } else if (boxType.isSelected("Full")) {
            if (flatBoxOutline.isValue()) {
                Render2D.drawQuad(posX - 1F, posY - 1F, endPosX - posX + 2F, 1.5F, black);
                Render2D.drawQuad(posX - 1F, posY - 1F, 1.5F, endPosY - posY + 2F, black);
                Render2D.drawQuad(posX - 1F, endPosY - 1F, endPosX - posX + 2F, 1.5F, black);
                Render2D.drawQuad(endPosX - 0.5F, posY - 1F, 1.5F, endPosY - posY + 2F, black);
            }
            Render2D.drawQuad(posX - 0.5F, posY - 0.5F, endPosX - posX + 1F, 0.5F, color);
            Render2D.drawQuad(posX - 0.5F, posY - 0.5F, 0.5F, endPosY - posY + 1F, color);
            Render2D.drawQuad(posX - 0.5F, endPosY - 0.5F, endPosX - posX + 1F, 0.5F, color);
            Render2D.drawQuad(endPosX, posY - 0.5F, 0.5F, endPosY - posY + 1F, color);
        }
    }

    private void drawArmor(DrawContext context, PlayerEntity player, Vector4d vec, FontRenderer font) {
        MatrixStack matrix = context.getMatrices();
        List<ItemStack> items = new ArrayList<>();
        player.getEquippedItems().forEach(s -> {if (!s.isEmpty()) items.add(s);});
        float posX = (float) (Projection.centerX(vec) - items.size() * 5.5);
        float posY = (float) (vec.y - 13 / 1.5 - 15);
        float padding = 0.5F;
        float offset = -11;
        if (!items.isEmpty()) {
            matrix.push();
            matrix.translate(posX, posY, 0);
            for (ItemStack stack : items) {
                offset += 11;
                Render2D.defaultDrawStack(context, stack, offset, 0, false, false, 0.5F);
            }
            matrix.pop();
        }
    }

    private void drawHands(MatrixStack matrix, PlayerEntity player, FontRenderer font, Vector4d vec) {
        double posY = vec.w;
        for (ItemStack stack : player.getHandItems()) {
            if (stack.isEmpty()) continue;
            MutableText text = Text.empty().append(stack.getName());
            if (stack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
            posY += font.getStringHeight(text) / 2 + 3;
            drawText(matrix, text, Projection.centerX(vec), posY, font);
        }
    }

    private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, Vector4d vec) {
        MatrixStack matrix = context.getMatrices();
        int width = 176;
        int height = 67;
        int color = ColorAssist.multBright(ColorAssist.replAlpha(((BlockItem) itemStack.getItem()).getBlock().getDefaultMapColor().color, 1F), 1);
        matrix.push();
        matrix.translate(Projection.centerX(vec) - (double) width / 4, vec.w + 2, -200 + Math.cos(vec.x));
        matrix.scale(0.5F, 0.5F, 1);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, 0, 0, width, height, width, height, color);
        int posX = 7;
        int posY = 6;
        for (ItemStack stack : stacks.stream().toList()) {
            Render2D.defaultDrawStack(context, stack, posX, posY, false, true, 1);
            posX += 18;
            if (posX >= 165) {
                posY += 18;
                posX = 7;
            }
        }
        matrix.pop();
    }

    private void drawText(MatrixStack matrix, Text text, double startX, double startY, FontRenderer font) {
        int paddingX = 2;
        float paddingY = 0.75F;
        float height = font.getFont().getSize() / 1.5F;
        float width = font.getStringWidth(text);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;
        rectangle.render(ShapeProperties.create(matrix, posX - paddingX, posY - paddingY, width + paddingX * 2, height + paddingY * 2)
                .round(2f)
                .outlineColor(new Color(33, 33, 33, 0).getRGB())
                .color(ColorAssist.getRect(0.65f))
                .build());
        font.drawText(matrix, text, posX, posY + 3);
    }

    private MutableText getTextPlayer(PlayerEntity player, boolean friend) {
        MutableText text = Text.empty();
        if (friend) text.append("[" + Formatting.GREEN + "F" + Formatting.RESET + "] ");
        if (AntiBot.getInstance().isBot(player)) text.append("[" + Formatting.DARK_RED + "BOT" + Formatting.RESET + "] ");
        if (playerSetting.isSelected("NameTags")) text.append(player.getDisplayName()); else text.append(player.getName());
        if (player.getOffHandStack().getItem().equals(Items.PLAYER_HEAD) || player.getOffHandStack().getItem().equals(Items.TOTEM_OF_UNDYING))
            text.append(Formatting.RESET + getSphere(player.getOffHandStack()));
        return text;
    }

    private String getSphere(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (Network.isFunTime() && component != null) {
            NbtCompound compound = component.copyNbt();
            if (compound.getInt("tslevel") != 0) {
                return " [" + Formatting.GOLD + compound.getString("don-item").replace("sphere-", "").toUpperCase() + Formatting.RESET + "]";
            }
        }
        return "";
    }

    private Identifier getSkinTexture(PlayerEntity player) {
        try {
            var profile = player.getGameProfile();
            if (profile == null) return Identifier.of("minecraft", "textures/entity/steve.png");
            var textures = mc.getSkinProvider().getSkinTextures(profile);
            if (textures != null) {
                Identifier texture = textures.texture();
                if (texture != null) return texture;
            }
        } catch (Exception ignored) {}
        return Identifier.of("minecraft", "textures/entity/steve.png");
    }

    private void drawTextWithHead(DrawContext context, MatrixStack matrix, Text text, PlayerEntity player, double startX, double startY, FontRenderer font) {
        Identifier skinTexture = getSkinTexture(player);

        int paddingX = 2;
        float paddingY = 0.75F;
        float height = font.getFont().getSize() / 1.5F;
        float width = font.getStringWidth(text);
        float headSize = height;
        float gap = 1.5F;
        float totalWidth = headSize + gap + width;

        float posX = (float) (startX - totalWidth / 2);
        float posY = (float) startY - height;

        rectangle.render(ShapeProperties.create(matrix, posX - paddingX, posY - paddingY,
                        totalWidth + paddingX * 2, height + paddingY * 2)
                .round(2f)
                .outlineColor(new Color(33, 33, 33, 0).getRGB())
                .color(ColorAssist.getRect(0.65f))
                .build());

        Render2D.drawTexture(context, skinTexture, posX, posY, headSize, 1f, 8, 8, 64, ColorAssist.getRect(1), -1);
        font.drawText(matrix, text, posX + headSize + gap, posY + 3);
    }
}