package lol.zyrex.features.impl.player;

import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.interactions.inv.InventoryFlowManager;
import lol.zyrex.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.utils.interactions.item.ItemToolkit;
import lol.zyrex.utils.math.script.Script;
import lol.zyrex.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoUse extends Module {
    Script script = new Script();

    MultiSelectSetting multiSetting = new MultiSelectSetting("Режим", "Выберите, что будет использоваться").value("Eat", "Invisibility");

    public AutoUse() {
        super("AutoUse", "Auto Use", ModuleCategory.PLAYER);
        setup(multiSetting);
    }

    @Override
    public void deactivate() {
        script.update();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        for (String string : multiSetting.getSelected())
            switch (string) {
                case "Eat" -> {
                    Slot slot = InventoryTask.getFoodMaxSaturationSlot();
                    if (slot != null && mc.player.getHungerManager().isNotFull() && swapAndEat(slot)) {
                        return;
                    }
                }
                case "Invisibility" -> {
                    Slot slot = InventoryTask.getPotion(StatusEffects.INVISIBILITY);
                    if (slot != null && !PlayerInteractionHelper.isPotionActive(StatusEffects.INVISIBILITY) && swapAndEat(slot)) {
                        return;
                    }
                }
            }
        script.update();
    }

    public boolean swapAndEat(Slot slot) {
        ItemStack stack = slot.getStack();
        if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            if (!mc.player.getOffHandStack().equals(stack)) {
                if (InventoryFlowManager.script.isFinished()) {
                    InventoryTask.swapHand(slot, Hand.OFF_HAND, true, true);
                    script.cleanup().addTickStep(0, () -> InventoryTask.swapHand(slot, Hand.OFF_HAND, true, true));
                }
            } else {
                ItemToolkit.INSTANCE.useHand(Hand.OFF_HAND);
            }
            return true;
        }
        return false;
    }
}
