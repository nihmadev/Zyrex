package lol.zyrex.features.impl.combat;

import lol.zyrex.events.player.PostMotionEvent;
import lol.zyrex.events.player.RotationUpdateEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.events.render.WorldRenderEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.*;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.event.types.EventType;
import lol.zyrex.utils.features.aura.striking.StrikeManager;
import lol.zyrex.utils.features.aura.striking.StrikerConstructor;
import lol.zyrex.utils.features.aura.target.TargetFinder;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;
import lol.zyrex.utils.features.aura.point.MultiPoint;
import lol.zyrex.utils.features.aura.rotations.constructor.RotateConstructor;
import lol.zyrex.utils.features.aura.rotations.constructor.LinearConstructor;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.Zyrex;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class TriggerBot extends Module {

    private static final float RANGE_MARGIN = 0.25F;
    private final TargetFinder targetSelector = new TargetFinder();
    private final MultiPoint pointFinder = new MultiPoint();
    public LivingEntity target;

    public SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует список целей по типу")
            .value("Players", "Mobs", "Animals", "Friends", "Armor Stand")
            .selected("Players", "Friends");

    public MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Параметры атаки")
            .value("Only Critical", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls", "Hit Chance")
            .selected("Only Critical");

    public SliderSettings hitChance = new SliderSettings("Шанс удара в %", "Шанс удара по цели")
            .setValue(100).range(1F, 100F).visible(() -> attackSetting.isSelected("Hit Chance"));

    public SelectSetting sprintReset = new SelectSetting("Сброс спринта", "Выбор сброса спринта перед ударом")
            .value("Legit", "Packet").selected("Legit");

    public BooleanSetting smartCrits = new BooleanSetting("Удары на земле", "Криты только при нажатии пробела")
            .setValue(true).visible(() -> attackSetting.isSelected("Only Critical"));

    public TriggerBot() {
        super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
        setup(attackRange, targetType, attackSetting, hitChance, sprintReset, smartCrits);
    }
    public static TriggerBot getInstance() {
        return Instance.get(TriggerBot.class);
    }
    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackRange.getValue() + RANGE_MARGIN;
        float fov = ProjectileHelper.getInstance().isState() ? ProjectileHelper.getInstance().getFovSetting().getValue() : 90;
        targetSelector.searchTargets(mc.world.getEntities(), range, fov, attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        switch (e.getType()) {
            case EventType.PRE -> target = updateTarget();
            case EventType.POST -> {}
        }
    }

    @EventHandler
    public void onPostMotion(PostMotionEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        if (target != null) {
            Zyrex.getInstance().getAttackPerpetrator().performAttack(getConfig());
        }
    }

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
        var angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));

        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                attackSetting.getSelected(),
                null,
                hitbox
        );
    }

    public RotateConstructor getSmoothMode() {
        return new LinearConstructor();
    }

    @EventHandler
    public void tick(TickEvent e) {}

    @EventHandler
    public void onPacket(PacketEvent e) {}

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {}
}