package lol.zyrex.features.impl.player;

import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.math.time.StopWatch;
import lol.zyrex.events.container.HandledScreenEvent;
import lol.zyrex.events.item.ClickSlotEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemScroller extends Module {
    StopWatch stopWatch = new StopWatch();

    SliderSettings scrollerSetting = new SliderSettings("Задержка прокрутки предметов", "Выберите задержку прокрутки предметов").setValue(50).range(0, 200);

    public ItemScroller() {
        super("ItemScroller","Item Scroller", ModuleCategory.PLAYER);
        setup(scrollerSetting);
    }

    @EventHandler
    public void onHandledScreen(HandledScreenEvent e) {
        Slot hoverSlot = e.getSlotHover();
        SlotActionType actionType = PlayerInteractionHelper.isKey(mc.options.dropKey) ? SlotActionType.THROW : PlayerInteractionHelper.isKey(mc.options.attackKey) ? SlotActionType.QUICK_MOVE : null;

        if (PlayerInteractionHelper.isKey(mc.options.sneakKey) && !PlayerInteractionHelper.isKey(mc.options.sprintKey) && hoverSlot != null && hoverSlot.hasStack() && actionType != null && stopWatch.every(scrollerSetting.getValue())) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hoverSlot.id, actionType.equals(SlotActionType.THROW) ? 1 : 0, actionType, mc.player);
        }
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        int slotId = e.getSlotId();
        if (slotId < 0 || slotId > mc.player.currentScreenHandler.slots.size()) return;
        Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
        Item item = slot.getStack().getItem();

        if (item != null && PlayerInteractionHelper.isKey(mc.options.sneakKey) && PlayerInteractionHelper.isKey(mc.options.sprintKey) && stopWatch.every(50)) {
            InventoryTask.slots().filter(s -> s.getStack().getItem().equals(item) && s.inventory.equals(slot.inventory))
                        .forEach(s -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, s.id, 1, e.getActionType(), mc.player));
        }
    }
}

