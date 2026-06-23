package lol.zyrex.features.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoDelay extends Module {
    public static NoDelay getInstance() {
        return Instance.get(NoDelay.class);
    }

    public MultiSelectSetting ignoreSetting = new MultiSelectSetting("Тип", "Разрешает выбранные вами действия")
            .value("Jump", "Right Click", "Break CoolDown");

    public NoDelay() {
        super("NoDelay", "No Delay", ModuleCategory.PLAYER);
        setup(ignoreSetting);
    }

    
    @EventHandler
    public void onTick(TickEvent e) {
        if (ignoreSetting.isSelected("Break CoolDown")) mc.interactionManager.blockBreakingCooldown = 0;
        if (ignoreSetting.isSelected("Jump")) mc.player.jumpingCooldown = 0;
        if (ignoreSetting.isSelected("Right Click")) mc.itemUseCooldown = 0;
    }
}