package lol.zyrex.features.impl.player;

import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.events.player.PostTickEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.math.time.StopWatch;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public class AutoFish extends Module {

    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "Автоматически переключаться на удочку").setValue(true);
    private final BooleanSetting autoToggle = new BooleanSetting("Auto Toggle", "Автоматически выключаться, если удочка не найдена").setValue(false);

    private final StopWatch castTimer = new StopWatch();
    private boolean wasCaughtFish = false;
    private boolean splashHeard = false;

    public AutoFish() {
        super("AutoFish", "Auto Fish", ModuleCategory.PLAYER);
        setup(autoSwitch, autoToggle);
    }

    @Override
    public void activate() {
        wasCaughtFish = false;
        splashHeard = false;
        castTimer.reset();
    }

    @Override
    public void deactivate() {
        wasCaughtFish = false;
        splashHeard = false;
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.isSend() || PlayerInteractionHelper.nullCheck()) return;

        if (e.getPacket() instanceof PlaySoundS2CPacket soundPacket
                && soundPacket.getSound().value() == SoundEvents.ENTITY_FISHING_BOBBER_SPLASH) {
            splashHeard = true;
        }
    }

    @EventHandler
    public void onPostTick(PostTickEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        if (!rodInHand() && !findRod()) return;

        FishingBobberEntity bobber = findBobber();

        if (bobber == null) {
            wasCaughtFish = false;
            if (castTimer.every(600)) {
                PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
            }
        } else {
            boolean biteDetected = splashHeard;
            splashHeard = false;

            if (bobber.caughtFish) {
                biteDetected = true;
            }

            if (biteDetected && !wasCaughtFish) {
                wasCaughtFish = true;
                PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
            } else if (!bobber.caughtFish) {
                wasCaughtFish = false;
            }
        }
    }

    private FishingBobberEntity findBobber() {
        for (var entity : mc.world.getEntities()) {
            if (entity instanceof FishingBobberEntity fb && fb.getPlayerOwner() == mc.player) {
                return fb;
            }
        }
        return null;
    }

    private boolean rodInHand() {
        return mc.player.getMainHandStack().isOf(Items.FISHING_ROD);
    }

    private boolean findRod() {
        if (!autoSwitch.isValue()) return false;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.FISHING_ROD)) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }

        if (autoToggle.isValue()) setState(false);
        return false;
    }
}
