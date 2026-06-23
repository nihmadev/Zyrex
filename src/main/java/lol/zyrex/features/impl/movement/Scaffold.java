package lol.zyrex.features.impl.movement;

import lol.zyrex.events.player.InputEvent;
import lol.zyrex.events.player.RotationUpdateEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.event.types.EventType;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lol.zyrex.utils.features.aura.warp.TurnsConfig;
import lol.zyrex.utils.features.aura.warp.TurnsConnection;
import lol.zyrex.utils.interactions.inv.InventoryTask;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.math.task.TaskPriority;
import lol.zyrex.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.IntPredicate;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Scaffold extends Module {

    SelectSetting mode = new SelectSetting("Режим", "Режим Scaffold")
            .value("Normal", "Speed Bridge", "Breezily")
            .selected("Normal");

    BooleanSetting rotate = new BooleanSetting("Поворот", "Автоматический поворот к блоку").setValue(true);

    @NonFinal
    StopWatch strafeTimer = new StopWatch();

    @NonFinal
    boolean strafeLeft = true;

    public Scaffold() {
        super("Scaffold", "Scaffold", ModuleCategory.MOVEMENT);
        setup(mode, rotate);
    }

    @Override
    public void deactivate() {
        mc.options.sneakKey.setPressed(false);
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() != EventType.PRE) return;
        if (PlayerInteractionHelper.nullCheck()) return;
        if (!rotate.isValue()) return;

        BlockPos targetPos = mc.player.getBlockPos().down();
        if (!PlayerInteractionHelper.isAir(mc.world.getBlockState(targetPos))) return;

        BlockPos supportPos = findSupport(targetPos);
        if (supportPos == null) return;

        Direction face = Direction.getFacing(
                targetPos.getX() - supportPos.getX(),
                targetPos.getY() - supportPos.getY(),
                targetPos.getZ() - supportPos.getZ()
        );

        Vec3d lookPos = Vec3d.ofCenter(supportPos).add(
                new Vec3d(
                        face.getVector().getX() * 0.5,
                        face.getVector().getY() * 0.5,
                        face.getVector().getZ() * 0.5
                )
        );

        TurnsConnection.INSTANCE.rotateTo(
                MathAngle.calculateAngle(lookPos),
                TurnsConfig.DEFAULT,
                TaskPriority.HIGH_IMPORTANCE_1,
                this
        );
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;

        if (mode.isSelected("Speed Bridge")) {
            handleSpeedBridge();
        } else {
            mc.options.sneakKey.setPressed(false);
        }

        BlockPos targetPos = mc.player.getBlockPos().down();
        if (!PlayerInteractionHelper.isAir(mc.world.getBlockState(targetPos))) return;

        BlockPos supportPos = findSupport(targetPos);
        if (supportPos == null) return;

        Hand hand = findBlockHand();
        if (hand == null) return;

        Direction face = Direction.getFacing(
                targetPos.getX() - supportPos.getX(),
                targetPos.getY() - supportPos.getY(),
                targetPos.getZ() - supportPos.getZ()
        );

        Vec3d hitPos = Vec3d.ofCenter(supportPos).add(
                new Vec3d(
                        face.getVector().getX() * 0.5,
                        face.getVector().getY() * 0.5,
                        face.getVector().getZ() * 0.5
                )
        );

        mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(hitPos, face, supportPos, false));
        mc.player.swingHand(hand);
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (!mode.isSelected("Breezily")) return;

        if (strafeTimer.finished(400)) {
            strafeLeft = !strafeLeft;
            strafeTimer.reset();
        }

        e.setDirectional(e.getInput().forward(), e.getInput().backward(), strafeLeft, !strafeLeft);
    }

    private void handleSpeedBridge() {
        double dx = Math.abs((mc.player.getX() % 1 + 1) % 1 - 0.5);
        double dz = Math.abs((mc.player.getZ() % 1 + 1) % 1 - 0.5);
        double maxOffset = Math.max(dx, dz);

        mc.options.sneakKey.setPressed(maxOffset > 0.35);
    }

    private Hand findBlockHand() {
        if (mc.player.getOffHandStack().getItem() instanceof BlockItem) {
            return Hand.OFF_HAND;
        }
        IntPredicate blockPred = i -> mc.player.getInventory().getStack(i).getItem() instanceof BlockItem;
        int slot = InventoryTask.getHotbarSlotId(blockPred);
        if (slot != -1) {
            mc.player.getInventory().selectedSlot = slot;
            return Hand.MAIN_HAND;
        }
        return null;
    }

    private BlockPos findSupport(BlockPos target) {
        BlockPos below = target.down();
        if (!PlayerInteractionHelper.isAir(mc.world.getBlockState(below))) return below;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = target.offset(dir);
            if (!PlayerInteractionHelper.isAir(mc.world.getBlockState(neighbor))) return neighbor;
        }

        return null;
    }
}
