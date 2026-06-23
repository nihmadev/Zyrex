package lol.zyrex.features.impl.player;

import lol.zyrex.events.player.TickEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.interactions.inv.InventoryTask;
import lol.zyrex.utils.math.projection.Projection;
import lol.zyrex.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoBoatDrop extends Module {

    private static final List<Item> BOATS = List.of(
            Items.OAK_BOAT, Items.SPRUCE_BOAT, Items.BIRCH_BOAT,
            Items.JUNGLE_BOAT, Items.ACACIA_BOAT, Items.DARK_OAK_BOAT,
            Items.MANGROVE_BOAT, Items.CHERRY_BOAT, Items.BAMBOO_RAFT
    );

    StopWatch cooldown = new StopWatch().setMs(2500);
    boolean shouldMount;
    StopWatch mountTimer = new StopWatch();

    public AutoBoatDrop() {
        super("AutoBoatDrop", "Auto Boat Drop", ModuleCategory.PLAYER);
    }

    @Override
    public void deactivate() {
        shouldMount = false;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (shouldMount) {
            if (mountTimer.finished(2000)) {
                shouldMount = false;
                return;
            }
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof BoatEntity boat && mc.player.distanceTo(boat) < 3F) {
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(boat, false, Hand.MAIN_HAND));
                    shouldMount = false;
                    return;
                }
            }
            return;
        }

        if (!cooldown.finished(2500)) return;
        if (mc.player.isOnGround() || mc.player.fallDistance < 3) return;
        if (mc.player.getVelocity().y >= -0.5) return;

        double distanceToGround = Projection.getDistanceToGround();
        if (distanceToGround > 6 || distanceToGround < 0.5) return;

        Hand hand = null;
        if (BOATS.contains(mc.player.getMainHandStack().getItem())) {
            hand = Hand.MAIN_HAND;
        } else if (BOATS.contains(mc.player.getOffHandStack().getItem())) {
            hand = Hand.OFF_HAND;
        } else {
            int slotId = InventoryTask.getHotbarSlotId(i -> BOATS.contains(mc.player.getInventory().getStack(i).getItem()));
            if (slotId != -1) {
                mc.player.getInventory().selectedSlot = slotId;
                hand = Hand.MAIN_HAND;
            } else {
                Slot slot = InventoryTask.getSlot(s -> BOATS.contains(s.getStack().getItem()));
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
        cooldown.reset();
        shouldMount = true;
        mountTimer.reset();
    }
}
