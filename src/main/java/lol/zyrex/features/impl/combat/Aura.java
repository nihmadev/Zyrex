package lol.zyrex.features.impl.combat;

import lol.zyrex.events.player.MotionEvent;
import lol.zyrex.events.player.PostMotionEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.events.render.DrawEvent;
import lol.zyrex.events.render.WorldRenderEvent;
import lol.zyrex.features.impl.movement.ElytraTarget;
import lol.zyrex.features.impl.movement.Strafe;
import lol.zyrex.features.impl.movement.TargetStrafe;
import lol.zyrex.utils.client.chat.ChatMessage;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.utils.display.font.Fonts;
import lol.zyrex.utils.display.geometry.Render3D;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.utils.display.shape.ShapeProperties;
import lol.zyrex.utils.display.shape.implement.Arc;
import lol.zyrex.utils.features.aura.point.MultiPoint;
import lol.zyrex.utils.features.aura.rotations.constructor.LinearConstructor;
import lol.zyrex.utils.features.aura.rotations.constructor.RotateConstructor;
import lol.zyrex.utils.features.aura.rotations.impl.*;
import lol.zyrex.utils.features.aura.rotations.impl.SlothAngle;
import lol.zyrex.utils.features.aura.neuro.NeuroAngle;
import lol.zyrex.utils.features.aura.neuro.NeuroProfile;
import lol.zyrex.utils.features.aura.neuro.NeuroRecorder;
import lol.zyrex.utils.features.aura.neuro.NeuroRepository;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.features.aura.warp.TurnsConfig;
import lol.zyrex.utils.features.aura.warp.Turns;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.interactions.simulate.PlayerSimulation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.event.types.EventType;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.*;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.math.task.TaskPriority;
import lol.zyrex.Zyrex;
import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.events.player.RotationUpdateEvent;
import lol.zyrex.display.hud.Notifications;
import lol.zyrex.utils.features.aura.striking.StrikeManager;
import lol.zyrex.utils.features.aura.striking.StrikerConstructor;
import lol.zyrex.utils.features.aura.target.TargetFinder;
import lol.zyrex.features.impl.render.Hud;
import lol.zyrex.utils.math.calc.Calculate;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Aura extends Module {

    private static final float RANGE_MARGIN = 0.25F;

    private static final Map<String, Integer> LEGIT_SPRINT_MAP = Map.of(
            "FunTime", 1,
            "Matrix", 1,
            "Snap", 1,
            "SpookyTime", 2,
            "HolyWorld", 2
    );

    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    TargetFinder targetSelector = new TargetFinder();
    MultiPoint pointFinder = new MultiPoint();

    @NonFinal
    LivingEntity target, lastTarget;

    @NonFinal
    long shiftTapEndTime = 0;

    public static boolean fakeRotate;

    @NonFinal
    @Getter
    public static float legitSprintNeed;

    @NonFinal
    private FovCircleRenderer fovCircleRenderer;

    SelectSetting aimMode = new SelectSetting("Наводка", "Выберите тип наводки")
            .value("FunTime", "Legit Snap", "ReallyWorld", "HolyWorld", "SpookyTime", "CakeWorld", "Sloth", "Neuro")
            .selected("Legit Snap");

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует весь список целей по типу")
            .value("Players", "Mobs", "Animals", "Friends", "Armor Stand")
            .selected("Players", "Friends");

    SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    SliderSettings lookRange = new SliderSettings("Дополнительная дистанция поиска", "Диапазон поиска до цели")
            .setValue(0.0f).range(0F, 2F);

    MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Позволяет настроить работу функции")
            .value("Only Critical", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls", "Fake Lag", "Hit Chance")
            .selected("Only Critical");

    SliderSettings hitChance = new SliderSettings("Шанс удара в %", "Шанс удара по цели")
            .setValue(100).range(1F, 100F).visible(() -> attackSetting.isSelected("Hit Chance"));

    SelectSetting correctionType = new SelectSetting("Коррекции движения", "Выбор коррекции движения игрока")
            .value("Free", "Focused", "Преследование", "Not visible").selected("Free");

    SelectSetting sprintReset = new SelectSetting("Сброс спринта", "Выбор сброса спринта перед ударом")
            .value("Legit", "Packet").selected("Legit");

    BooleanSetting smartCrits = new BooleanSetting("Удары на земле", "Криты только при нажатии пробела")
            .setValue(true).visible(() -> attackSetting.isSelected("Only Critical"));

    public SliderSettings slothSpeed = new SliderSettings("Скорость Sloth", "Скорость наведения для режима Sloth")
            .setValue(1.0f).range(0.5f, 2.0f).visible(() -> aimMode.isSelected("Sloth"));

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        setup(
                aimMode,
                correctionType,
                sprintReset,

                targetType,

                attackRange,
                lookRange,
                hitChance,
                slothSpeed,

                attackSetting,
                smartCrits
        );
        fovCircleRenderer = new FovCircleRenderer();
        Zyrex.getInstance().getEventManager().register(fovCircleRenderer);
    }

    @Override
    public void deactivate() {
        targetSelector.releaseTarget();
        target = null;
        lastTarget = null;
        packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
        packets.clear();
        
        // Сбрасываем ротации чтобы голова не крутилась
        TurnsConnection controller = TurnsConnection.INSTANCE;
        controller.clear();
        controller.setRotation(null);
        controller.setFakeRotation(null);
        
        super.deactivate();
    }

    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private Box box;
    public static int tickStop = -1;

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
            Entity entity = status.getEntity(mc.world);
            if (entity != null && entity.equals(target) && Hud.getInstance().notificationSettings.isSelected("Break Shield")) {
                Notifications.getInstance().addList(Text.literal("Сломали щит игроку - ").append(entity.getDisplayName()), 5000);
            }
        }

        if (attackSetting.isSelected("Fake Lag") && target !=null) {
            if (PlayerInteractionHelper.nullCheck()) return;
            switch (e.getPacket()) {
                case PlayerRespawnS2CPacket respawn -> setState(false);
                case GameJoinS2CPacket join -> setState(false);
                case ClientStatusC2SPacket status when status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) ->
                        setState(false);
                default -> {
                    if (e.isSend() && tickStop < 0) {
                        packets.add(e.getPacket());
                        e.cancel();
                    }
                }
            }
        }
    }

    public class FovCircleRenderer implements QuickImports {
        private float currentScale = 1.0f;
        private float targetScale = 1.0f;
        private float cachedDynamicFov = 33;
        private float lastFinalRadius = 0;

        @EventHandler
        public void drawEvent(DrawEvent e){
            if (mc.player == null || !aimMode.isSelected("Legit Snap") || !isState()) return;

            if (mc.options.getPerspective().isFirstPerson()) {
                MatrixStack matrix = e.getDrawContext().getMatrices();
                float middleW = mc.getWindow().getScaledWidth() / 2f;
                float middleH = mc.getWindow().getScaledHeight() / 2f;

                double fov = mc.options.getFov().getValue();
                fov = MathHelper.clamp(fov, 30, 110);

                float baseRadius = (float) MathHelper.lerp((fov - 30.0) / 80.0, 106.5, 65.0);
                float fovScale = (float) (450.0 / fov);
                float dynamicRadius = baseRadius * fovScale;

                targetScale = mc.player.isSprinting() ? 0.9f : 1f;
                currentScale = Calculate.interpolateSmooth(2.5, currentScale, targetScale);

                float finalRadius = dynamicRadius * currentScale;
                lastFinalRadius = finalRadius;

                float baseThickness = (float) MathHelper.lerp((fov - 30.0) / 80.0, 0.003, 0.015);

                arc.render(ShapeProperties.create(matrix, middleW - finalRadius / 2f, middleH - finalRadius / 2f, finalRadius, finalRadius)
                        .round(0.3F)
                        .thickness(baseThickness)
                        .end(360)
                        .color(ColorAssist.getColor(255, 255, 255, 255))
                        .build());

                cachedDynamicFov = calculateDynamicFov();
            }
        }

        private float calculateDynamicFov() {
            if (mc.player == null || mc.getWindow() == null) return 33;

            double fov = mc.options.getFov().getValue();
            fov = MathHelper.clamp(fov, 30, 110);

            float screenWidth = mc.getWindow().getScaledWidth();
            float screenHeight = mc.getWindow().getScaledHeight();

            float circleRadiusInPixels = lastFinalRadius / 2f;

            double horizontalFovRadians = Math.toRadians(fov);

            double pixelsPerRadian = screenWidth / horizontalFovRadians;

            double circleFovRadians = circleRadiusInPixels / pixelsPerRadian;

            float circleFovDegrees = (float) Math.toDegrees(circleFovRadians);

            circleFovDegrees *= 2.0f;

            return MathHelper.clamp(circleFovDegrees, 36, 360);
        }

        public float getCachedDynamicFov() {
            return cachedDynamicFov;
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (box != null && attackSetting.isSelected("Fake Lag") && target !=null) {
            Render3D.drawBox(box, ColorAssist.getClientColor(), 1);
        }
    }

    @EventHandler
    public void tick(TickEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        if (target == null) return;

        tickStop--;
        if (tickStop >= 0 && !packets.isEmpty() && attackSetting.isSelected("Fake Lag")) {
            box = mc.player.getBoundingBox();
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
        }
        if (mc.player.distanceTo(target) > attackRange.getValue() && attackSetting.isSelected("Fake Lag")) {
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        try {
            if (aimMode.isSelected("FunTime") && Zyrex.getInstance().getFtCheckClient() != null) {
                Zyrex.getInstance().getFtCheckClient().checkAndWarnFunTime();
            }
        } catch (Exception ex) {
        }

        switch (e.getType()) {
            case EventType.PRE -> {
                target = updateTarget();
                if (target != null) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
            }
            case EventType.POST -> {
                // Атака перенесена в PostMotionEvent — после отправки пакета движения с ротацией
            }
        }
    }

    @EventHandler
    public void onPostMotion(PostMotionEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        if (target != null) {
            Zyrex.getInstance().getAttackPerpetrator().performAttack(getConfig());
        }
    }

    public static boolean shouldRotate;

    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackRange.getValue() + RANGE_MARGIN + (mc.player.isGliding() && ElytraTarget.getInstance().isState() ? ElytraTarget.getInstance().elytraFindRange.getValue() : lookRange.getValue());

        float dynamicFov = 360;
        if (aimMode.isSelected("Legit Snap") && fovCircleRenderer != null) {
            dynamicFov = fovCircleRenderer.getCachedDynamicFov();
        }

        targetSelector.searchTargets(mc.world.getEntities(), range, dynamicFov, attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        StrikeManager attackHandler = Zyrex.getInstance().getAttackPerpetrator().getAttackHandler();
        TurnsConnection controller = TurnsConnection.INSTANCE;
        Turns.VecRotation rotation = new Turns.VecRotation(config.getAngle(), config.getAngle().toVector());
        TurnsConfig rotationConfig = getRotationConfig();

        boolean elytraMode = mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities");

        if (fakeRotate && target != null) {
            FakeAngle fake = new FakeAngle();
            Turns fakeRot = fake.limitAngleChange(controller.getRotation(), rotation.getAngle(), rotation.getVec(), target);
            controller.setFakeRotation(fakeRot);
        }
        fakeRotate = false;

        int resetTicks = 999;

        switch (aimMode.getSelected()) {


            case "FunTime" -> {
                controller.clear();
                controller.rotateTo(rotation, target, resetTicks, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

            case "HolyWorld" -> {
                controller.rotateTo(rotation, target, resetTicks, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

            case "Legit Snap" -> {
                controller.rotateTo(rotation, target, resetTicks, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

            case "ReallyWorld", "SpookyTime", "CakeWorld" -> {
                controller.rotateTo(rotation, target, resetTicks, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

            case "Sloth" -> {
                controller.rotateTo(rotation, target, resetTicks, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

            case "Neuro" -> {
                controller.rotateTo(rotation, target, resetTicks, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

        };

        if (elytraMode) {
            controller.rotateTo(rotation, target, resetTicks, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }
    }

    @NonFinal
    public boolean elytraStateForward = false;
    private boolean wasForwardPressed = false;

    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float baseRange = attackRange.getValue() + RANGE_MARGIN;

        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                TurnsConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                attackSetting.isSelected("Ignore The Walls")
        );

        Vec3d computedPoint = pointData.getLeft();
        Box hitbox = pointData.getRight();

        if (mc.player.isGliding() && target.isGliding()) {
            Vec3d targetVelocity = target.getVelocity();
            double targetSpeed = targetVelocity.horizontalLength();

            float leadTicks = 0;
            if (ElytraTarget.shouldElytraTarget) {
                leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
            }

            if (targetSpeed > 0.35) {

                Vec3d predictedPos = target.getPos().add(targetVelocity.multiply(leadTicks));
                computedPoint = predictedPos.add(0, target.getHeight() / 2, 0);

                hitbox = new Box(
                        predictedPos.x - target.getWidth() / 2,
                        predictedPos.y,
                        predictedPos.z - target.getWidth() / 2,
                        predictedPos.x + target.getWidth() / 2,
                        predictedPos.y + target.getHeight(),
                        predictedPos.z + target.getWidth() / 2
                );
            }
        }

        Turns angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));

        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                attackSetting.getSelected(),
                aimMode,
                hitbox
        );
    }

    public TurnsConfig getRotationConfig() {
        boolean visibleCorrection = !correctionType.isSelected("Not visible");
        boolean freeCorrection = !aimMode.isSelected("Legit") && correctionType.isSelected("Free");
        if (TargetStrafe.getInstance().isState() && TargetStrafe.getInstance().mode.isSelected("Grim") && target !=null) { freeCorrection = false; }
        return new TurnsConfig(getSmoothMode(), visibleCorrection, freeCorrection);
    }

    @EventHandler
    public void onmotion(MotionEvent event) {
        // Пусто - логика преследования теперь в onInput
    }
    
    // Переменная для точки обхода (как в TargetStrafe Grim)
    private int pursuitPointIndex = 0;

    // ── Neuro movement state ──────────────────────────────────────────────
    private final Random neuroRng = new java.security.SecureRandom();
    private int neuroMoveDir = -1;
    private int neuroMoveTicks = 0;
    
    @EventHandler
    public void onInput(lol.zyrex.events.player.InputEvent event) {
        if (target == null || mc.player == null) return;

        // Neuro — воспроизведение движения из профиля
        if (aimMode.isSelected("Neuro")) {
            NeuroProfile profile = NeuroRepository.getInstance().getActiveProfile();
            if (profile != null && profile.getTotalFrames() >= 10) {
                neuroMoveTicks++;

                // Выбираем направление движения через матрицу переходов
                if (neuroMoveDir < 0 || neuroMoveTicks > Math.max(3, neuroRng.nextInt(8) + 3)) {
                    float[][] matrix = profile.getMovementTransitionMatrix();
                    if (neuroMoveDir < 0) {
                        float roll = neuroRng.nextFloat();
                        float f = profile.getForwardRatio();
                        float b = profile.getBackwardRatio();
                        float l = profile.getLeftRatio();
                        float r = profile.getRightRatio();
                        float sum = f + b + l + r;
                        if (sum > 0) {
                            float p = roll * sum;
                            if (p < f) neuroMoveDir = 0;
                            else if (p < f + b) neuroMoveDir = 1;
                            else if (p < f + b + l) neuroMoveDir = 2;
                            else neuroMoveDir = 3;
                        } else {
                            neuroMoveDir = 0;
                        }
                    } else {
                        float roll = neuroRng.nextFloat();
                        float cumulative = 0;
                        int nextDir = -1;
                        for (int j = 0; j < 4; j++) {
                            cumulative += matrix[neuroMoveDir][j];
                            if (roll <= cumulative) { nextDir = j; break; }
                        }
                        neuroMoveDir = nextDir >= 0 ? nextDir : neuroRng.nextInt(4);
                    }
                    neuroMoveTicks = 0;
                }

                // Движение относительно цели: approach/strafe/retreat
                Vec3d toTarget = target.getEyePos().subtract(mc.player.getEyePos()).normalize();
                float targetAngle = (float) Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90F;
                float yaw = TurnsConnection.INSTANCE.getRotation() != null
                    ? TurnsConnection.INSTANCE.getRotation().getYaw()
                    : mc.player.getYaw();
                float angleDiff = MathHelper.wrapDegrees(targetAngle - yaw);

                // Смещение угла движения в зависимости от режима
                float moveAngle = angleDiff;
                switch (neuroMoveDir) {
                    case 0 -> {}                          // approach
                    case 1 -> moveAngle += 180;           // retreat
                    case 2 -> moveAngle -= 90;            // strafe left
                    case 3 -> moveAngle += 90;            // strafe right
                }
                moveAngle = MathHelper.wrapDegrees(moveAngle);

                boolean forward = false, back = false, left = false, right = false;
                if (moveAngle >= -22.5 && moveAngle < 22.5) {
                    forward = true;
                } else if (moveAngle >= 22.5 && moveAngle < 67.5) {
                    forward = true; right = true;
                } else if (moveAngle >= 67.5 && moveAngle < 112.5) {
                    right = true;
                } else if (moveAngle >= 112.5 && moveAngle < 157.5) {
                    back = true; right = true;
                } else if (moveAngle >= -67.5 && moveAngle < -22.5) {
                    forward = true; left = true;
                } else if (moveAngle >= -112.5 && moveAngle < -67.5) {
                    left = true;
                } else if (moveAngle >= -157.5 && moveAngle < -112.5) {
                    back = true; left = true;
                } else {
                    back = true;
                }

                boolean jumping = neuroRng.nextFloat() < profile.getJumpRatio() && mc.player.isOnGround();
                boolean sprinting = neuroRng.nextFloat() < profile.getSprintRatio();
                boolean sneaking = neuroRng.nextFloat() < profile.getSneakRatio();

                event.setAll(forward, back, left, right, jumping, sneaking, sprinting);
                return;
            }
        }

        // Режим преследования - как TargetStrafe Grim с минимальной дистанцией
        if (!correctionType.isSelected("Преследование")) return;
        
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        
        // Радиус обхода вокруг цели
        float radius = target.getWidth() + 0.15f;
        
        // Точки обхода вокруг таргета (как Grim Cube)
        Vec3d[] points = new Vec3d[]{
            new Vec3d(targetPos.x - radius, playerPos.y, targetPos.z - radius),
            new Vec3d(targetPos.x - radius, playerPos.y, targetPos.z + radius),
            new Vec3d(targetPos.x + radius, playerPos.y, targetPos.z + radius),
            new Vec3d(targetPos.x + radius, playerPos.y, targetPos.z - radius)
        };
        
        // Если достигли точки - переходим к следующей
        if (playerPos.distanceTo(points[pursuitPointIndex]) < 0.5) {
            pursuitPointIndex = (pursuitPointIndex + 1) % points.length;
        }
        
        Vec3d nextPoint = points[pursuitPointIndex];
        Vec3d direction = nextPoint.subtract(playerPos).normalize();
        
        // Угол движения относительно взгляда
        float yaw = TurnsConnection.INSTANCE.getRotation() != null 
            ? TurnsConnection.INSTANCE.getRotation().getYaw() 
            : mc.player.getYaw();
        float movementAngle = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
        float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);
        
        // Определяем клавиши как в TargetStrafe Grim
        boolean forward = false, back = false, left = false, right = false;
        
        if (angleDiff >= -22.5 && angleDiff < 22.5) {
            forward = true;
        } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
            forward = true; right = true;
        } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
            right = true;
        } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
            back = true; right = true;
        } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
            forward = true; left = true;
        } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
            left = true;
        } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
            back = true; left = true;
        } else {
            back = true;
        }
        
        event.setDirectional(forward, back, left, right);
        
        // Авто прыжок в режиме преследования
        if (mc.player.isOnGround() && (forward || left || right)) {
            event.setJumping(true);
        }
    }

    public RotateConstructor getSmoothMode() {
        if (mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities") && !aimMode.isSelected("Trigger Bot")) {
            return new LinearConstructor();
        }
        return switch (aimMode.getSelected()) {
            case "FunTime" -> new FTAngle();
            case "HolyWorld" -> new HWAngle();
            case "HvH" -> new HAngle();
            case "CakeWorld" -> new LGAngle();
            case "SpookyTime" -> new SPAngle();
            case "ReallyWorld" -> new RWAngle();
            case "Snap" -> new SnapAngle();
            case "Legit Snap" -> new SnapAngle();
            case "Matrix" -> new MatrixAngle();
            case "Sloth" -> new SlothAngle();
            case "Neuro" -> new NeuroAngle();
            default -> new LinearConstructor();
        };
    }
}