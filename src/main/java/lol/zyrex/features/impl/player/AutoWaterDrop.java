package lol.zyrex.features.impl.player;

import lol.zyrex.events.player.TickEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.interactions.inv.InventoryTask;
import lol.zyrex.utils.math.projection.Projection;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoWaterDrop extends Module {

    private static final List<net.minecraft.item.Item> BUCKETS = List.of(
            Items.WATER_BUCKET,
            Items.COD_BUCKET,
            Items.SALMON_BUCKET,
            Items.PUFFERFISH_BUCKET,
            Items.TROPICAL_FISH_BUCKET
    );

    public AutoWaterDrop() {
        super("AutoWaterDrop", "Auto Water Drop", ModuleCategory.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isOnGround() || mc.player.fallDistance < 3) return;
        if (mc.player.getVelocity().y >= -0.5) return;

        double distanceToGround = Projection.getDistanceToGround();
        if (distanceToGround > 4 || distanceToGround < 0.5) return;

        Hand hand = null;
        if (BUCKETS.contains(mc.player.getMainHandStack().getItem())) {
            hand = Hand.MAIN_HAND;
        } else if (BUCKETS.contains(mc.player.getOffHandStack().getItem())) {
            hand = Hand.OFF_HAND;
        } else {
            int slotId = InventoryTask.getHotbarSlotId(i -> BUCKETS.contains(mc.player.getInventory().getStack(i).getItem()));
            if (slotId != -1) {
                mc.player.getInventory().selectedSlot = slotId;
                hand = Hand.MAIN_HAND;
            } else {
                Slot slot = InventoryTask.getSlot(s -> BUCKETS.contains(s.getStack().getItem()));
                if (slot == null) return;
                InventoryTask.swapHand(slot, Hand.MAIN_HAND, false);
                hand = Hand.MAIN_HAND;
            }
        }

        if (hand == null) return;

        BlockPos groundPos = mc.player.getBlockPos().down((int) distanceToGround + 1);
        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5),
                Direction.UP, groundPos, false
        );
        mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        mc.player.swingHand(hand);
    }
}
