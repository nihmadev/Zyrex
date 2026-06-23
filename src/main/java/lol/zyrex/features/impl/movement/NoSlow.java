package lol.zyrex.features.impl.movement;

import lol.zyrex.events.player.TickEvent;
import lol.zyrex.utils.interactions.interact.PlayerInteractionHelper;
import lol.zyrex.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;

import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.event.types.EventType;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SelectSetting;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.math.time.StopWatch;
import lol.zyrex.utils.math.script.Script;
import lol.zyrex.events.item.UsingItemEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoSlow extends Module {
    public static NoSlow getInstance() {
        return Instance.get(NoSlow.class);
    }

    private final StopWatch notifWatch = new StopWatch();
    private final Script script = new Script();
    private boolean finish;

    public final SelectSetting itemMode = new SelectSetting("Режим предмета", "Выберите режим обхода").value("Grim Old", "SpookyTime");

    public NoSlow() {
        super("NoSlow", "No Slow", ModuleCategory.MOVEMENT);
        setup(itemMode);
    }
    private int ticks = 0;

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player.getActiveHand() == Hand.MAIN_HAND ||  mc.player.getActiveHand() == Hand.OFF_HAND) {
            ticks++;
        } else {
            ticks = 0;
        }
    }

    @EventHandler
    public void onUsingItem(UsingItemEvent e) {
        Hand first = mc.player.getActiveHand();
        Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;


        switch (e.getType()) {
            case EventType.ON -> {
                switch (itemMode.getSelected()) {
                    case "Grim Old" -> {
                        if (mc.player.getOffHandStack().getUseAction().equals(UseAction.NONE) || mc.player.getMainHandStack().getUseAction().equals(UseAction.NONE)) {
                            PlayerInteractionHelper.interactItem(first);
                            PlayerInteractionHelper.interactItem(second);
                            e.cancel();
                        }
                    }
                    case "SpookyTime" -> {
                        if (ticks > 1F && mc.player.getItemUseTime() > 1) {
                            e.cancel();
                            ticks = 0;
                        }
                    }
                }
            }
            case EventType.POST -> {
                while (!script.isFinished()) script.update();
            }
        }
    }
}