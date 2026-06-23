package lol.zyrex.features.impl.combat;

import lol.zyrex.common.repository.friend.FriendUtils;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.interactions.inv.InventoryTask;
import lol.zyrex.utils.math.script.Script;
import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.events.player.EntitySpawnEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoCrystal extends Module {
    private final Script script = new Script();
    private BlockPos obsPosition;
    private int obsPositionTicks;

    private final MultiSelectSetting protections = new MultiSelectSetting("Защита", "Что не взрывать")
            .value("Себя", "Друзей", "Ресурсы")
            .selected("Себя", "Друзей", "Ресурсы");

    private final SliderSettings itemRange = new SliderSettings("Дистанция до ресурсов", "Минимальное расстояние до ресурсов для защиты")
            .range(1.0f, 12.0f)
            .setValue(6.0f);

    private final BooleanSetting anchorSupport = new BooleanSetting("Якоря", "Авто зарядка и активация якорей")
            .setValue(true);

    private final BooleanSetting autoCrystalHand = new BooleanSetting("Кристал в руку", "Автоматически брать кристал энда в руку")
            .setValue(true);

    private final BooleanSetting autoGlowstoneHand = new BooleanSetting("Светокамень в руку", "Автоматически брать светокамень в руку")
            .setValue(true);

    private BlockPos anchorFillTarget;
    private int anchorFillTimer;
    private int anchorFillAttempts;
    private boolean moduleInteracting;

    public AutoCrystal() {
        super("AutoCrystal", "Auto Crystal", ModuleCategory.COMBAT);
        setup(protections, itemRange, anchorSupport, autoCrystalHand, autoGlowstoneHand);
    }

    @Override
    public void activate() {
        obsPosition = null;
        obsPositionTicks = 0;
        anchorFillTarget = null;
        anchorFillTimer = 0;
        anchorFillAttempts = 0;
        super.activate();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!e.isSend()) return;
        if (!(e.getPacket() instanceof PlayerInteractBlockC2SPacket interact)) return;

        BlockPos interactPos = interact.getBlockHitResult().getBlockPos();
        BlockPos spawnPos = interactPos.offset(interact.getBlockHitResult().getSide());

        Block spawnBlock = mc.world.getBlockState(spawnPos).getBlock();
        Block interactBlock = mc.world.getBlockState(interactPos).getBlock();
        ItemStack handItem = mc.player.getStackInHand(Hand.MAIN_HAND);
        int sequence = interact.getSequence();

        if (spawnBlock == Blocks.OBSIDIAN || spawnBlock == Blocks.BEDROCK) {
            e.setCancelled(true);
            placeCrystalOn(spawnPos, sequence);
            return;
        }
        if (interactBlock == Blocks.OBSIDIAN || interactBlock == Blocks.BEDROCK) {
            e.setCancelled(true);
            placeCrystalOn(interactPos, sequence);
            return;
        }

        if (!anchorSupport.isValue()) return;

        if (interactBlock == Blocks.RESPAWN_ANCHOR) {
            e.setCancelled(true);
            interactWithAnchor(interactPos, handItem, sequence);
            return;
        }

        if (handItem.getItem() == Items.OBSIDIAN) {
            scheduleDelayedCrystal(spawnPos);
            return;
        }

        if (handItem.getItem() == Items.RESPAWN_ANCHOR) {
            startAnchorFill(spawnPos);
        }
    }

    @EventHandler
    public void onEntitySpawnEvent(EntitySpawnEvent e) {
        if (e.getEntity() instanceof EndCrystalEntity crystal && obsPosition != null && obsPosition.equals(crystal.getBlockPos().down())) {
            if (isSafePosition(crystal.getBlockPos().down())) {
                mc.interactionManager.attackEntity(mc.player, crystal);
            }
            obsPosition = null;
            obsPositionTicks = 0;
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();
        updateAnchorFill();
        if (obsPosition != null && ++obsPositionTicks > 10) {
            obsPosition = null;
            obsPositionTicks = 0;
        }
    }

    private int nextSequence() {
        try (PendingUpdateManager mgr = mc.world.getPendingUpdateManager()) {
            return mgr.incrementSequence().getSequence();
        }
    }

    private void placeCrystalOn(BlockPos blockPos, int sequence) {
        if (moduleInteracting) return;
        Slot crystal = InventoryTask.getSlot(Items.END_CRYSTAL);
        if (crystal == null || !isSafePosition(blockPos)) return;

        script.addTickStep(0, () -> {
            obsPosition = blockPos;
            obsPositionTicks = 0;

            if (autoCrystalHand.isValue()) {
                InventoryTask.swapHand(crystal, Hand.MAIN_HAND, false);
            }
            moduleInteracting = true;
            try {
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(blockPos.toCenterPos(), Direction.UP, blockPos, false), sequence));
            } finally {
                moduleInteracting = false;
            }
            if (autoCrystalHand.isValue()) {
                InventoryTask.swapHand(crystal, Hand.MAIN_HAND, false, true);
            }
        });
    }

    private void scheduleDelayedCrystal(BlockPos blockPos) {
        if (moduleInteracting) return;
        Slot crystal = InventoryTask.getSlot(Items.END_CRYSTAL);
        if (crystal == null || !isSafePosition(blockPos)) return;

        script.addTickStep(6, () -> {
            obsPosition = blockPos;
            obsPositionTicks = 0;

            if (autoCrystalHand.isValue()) {
                InventoryTask.swapHand(crystal, Hand.MAIN_HAND, false);
            }
            moduleInteracting = true;
            try {
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(blockPos.toCenterPos(), Direction.UP, blockPos, false), nextSequence()));
            } finally {
                moduleInteracting = false;
            }
            if (autoCrystalHand.isValue()) {
                InventoryTask.swapHand(crystal, Hand.MAIN_HAND, false, true);
            }
        });
    }

    private void interactWithAnchor(BlockPos anchorPos, ItemStack stack, int sequence) {
        if (moduleInteracting) return;
        if (!isSafePosition(anchorPos)) return;

        boolean userHasGlowstone = stack.getItem() == Items.GLOWSTONE;
        boolean userHasCrystal = stack.getItem() == Items.END_CRYSTAL;

        if (userHasGlowstone || (autoGlowstoneHand.isValue() && !userHasCrystal)) {
            Slot glowstone = InventoryTask.getSlot(Items.GLOWSTONE);
            if (glowstone != null) {
                script.addTickStep(0, () -> doAnchorInteract(anchorPos, glowstone, Items.GLOWSTONE, sequence));
                return;
            }
        }

        if (userHasCrystal || autoCrystalHand.isValue()) {
            Slot crystal = InventoryTask.getSlot(Items.END_CRYSTAL);
            if (crystal != null) {
                script.addTickStep(0, () -> doAnchorInteract(anchorPos, crystal, Items.END_CRYSTAL, sequence));
            }
        }
    }

    private void doAnchorInteract(BlockPos pos, Slot slot, Item item) {
        doAnchorInteract(pos, slot, item, nextSequence());
    }

    private void doAnchorInteract(BlockPos pos, Slot slot, Item item, int sequence) {
        if (moduleInteracting) return;
        boolean shouldSwap = item == Items.END_CRYSTAL ? autoCrystalHand.isValue() : autoGlowstoneHand.isValue();

        if (shouldSwap) {
            InventoryTask.swapHand(slot, Hand.MAIN_HAND, false);
        }
        moduleInteracting = true;
        try {
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false), sequence));
        } finally {
            moduleInteracting = false;
        }
        if (shouldSwap) {
            InventoryTask.swapHand(slot, Hand.MAIN_HAND, false, true);
        }
    }

    private void startAnchorFill(BlockPos pos) {
        anchorFillTarget = pos;
        anchorFillTimer = 0;
        anchorFillAttempts = 0;
    }

    private void updateAnchorFill() {
        if (anchorFillTarget == null) return;
        if (anchorFillAttempts >= 8) {
            anchorFillTarget = null;
            return;
        }

        if (++anchorFillTimer >= 4) {
            anchorFillTimer = 0;
            BlockPos target = anchorFillTarget;
            Slot glowstone = InventoryTask.getSlot(Items.GLOWSTONE);
            if (glowstone == null) {
                anchorFillTarget = null;
                return;
            }
            script.addTickStep(0, () -> doAnchorInteract(target, glowstone, Items.GLOWSTONE));
            anchorFillAttempts++;
        }
    }

    private boolean isSafePosition(BlockPos pos) {
        if (protections.isSelected("Себя")) {
            if (mc.player.getY() > pos.getY()) {
                return false;
            }
        }

        if (protections.isSelected("Друзей")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (FriendUtils.isFriend(player) && player.getY() > pos.getY()) {
                    return false;
                }
            }
        }

        if (protections.isSelected("Ресурсы")) {
            Vec3d crystalPos = pos.up().toCenterPos();
            double range = itemRange.getValue();
            Box box = new Box(crystalPos.x - range, crystalPos.y - range, crystalPos.z - range, crystalPos.x + range, crystalPos.y + range, crystalPos.z + range);
            List<Entity> entities = mc.world.getOtherEntities(mc.player, box);
            for (Entity entity : entities) {
                if (entity instanceof ItemEntity) {
                    return false;
                }
            }
        }

        return true;
    }
}
