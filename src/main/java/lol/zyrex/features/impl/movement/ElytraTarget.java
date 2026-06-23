package lol.zyrex.features.impl.movement;

import com.google.common.eventbus.Subscribe;
import lol.zyrex.display.hud.Notifications;
import lol.zyrex.events.keyboard.KeyEvent;
import lol.zyrex.features.impl.render.Hud;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.BindSetting;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.sound.SoundManager;
import net.minecraft.util.Formatting;

public class ElytraTarget extends Module {

    public static ElytraTarget getInstance() {
        return Instance.get(ElytraTarget.class);
    }

    public SliderSettings elytraFindRange = new SliderSettings("Дистанция наводки", "Дальность поиска цели во время полета на элитре")
            .setValue(32).range(6F, 64F);

    public SliderSettings elytraForward = new SliderSettings("Значение перегона", "заебался")
            .setValue(3).range(0F, 6F);

    final BindSetting forward = new BindSetting("Кнопка вкл/выкл перегона", "ужас");

    public static boolean shouldElytraTarget = false;

    public ElytraTarget() {
        super("ElytraTarget", "Elytra Target", ModuleCategory.MOVEMENT);
        setup(elytraFindRange, elytraForward, forward);
    }

    @EventHandler
    private void onEventKey(KeyEvent e) {
        if (e.isKeyDown(forward.getKey())) {
            float volume = Hud.getInstance().getModuleVolume();
            shouldElytraTarget = !shouldElytraTarget;
            Notifications.getInstance().addList("Elytra Forward " + (shouldElytraTarget ? "enabled!" : "disabled"), 1500, null);
            SoundManager.playSound(shouldElytraTarget ? SoundManager.ENABLE_MODULE : SoundManager.DISABLE_MODULE, volume, 1.0f);
        }
    }

}