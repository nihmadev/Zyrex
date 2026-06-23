package lol.zyrex.features.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import lol.zyrex.common.animation.Animation;
import lol.zyrex.common.animation.Direction;
import lol.zyrex.common.animation.implement.Decelerate;
import lol.zyrex.utils.client.sound.SoundManager;
import lol.zyrex.Zyrex;
import lol.zyrex.features.module.setting.SettingRepository;
import lol.zyrex.features.module.setting.implement.BindSetting;
import lol.zyrex.utils.client.managers.event.EventManager;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.display.hud.Notifications;
import lol.zyrex.features.impl.render.Hud;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Module extends SettingRepository implements QuickImports {
    String name;
    String visibleName;
    ModuleCategory category;
    Animation animation = new Decelerate().setMs(175).setValue(1);

    public Module(String name, ModuleCategory category) {
        this.name = name;
        this.category = category;
        this.visibleName = name;
        initBindSetting();
    }

    public Module(String name, String visibleName, ModuleCategory category) {
        this.name = name;
        this.visibleName = visibleName;
        this.category = category;
        initBindSetting();
    }

    @NonFinal
    int key = GLFW.GLFW_KEY_UNKNOWN, type = 1;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @NonFinal
    BindSetting bindSetting;

    @NonFinal
    public boolean state;

    public void switchState() {
        setState(!state);
    }

    public void setState(boolean state) {
        animation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
        if (state != this.state) {
            this.state = state;
            handleStateChange();
        }
    }

    private void handleStateChange() {
        MinecraftClient mc = MinecraftClient.getInstance();
        float volume = Hud.getInstance().getModuleVolume();

        if (mc.player != null && mc.world != null) {
            if (state) {
                if (Hud.getInstance().notificationSettings.isSelected("Module Switch")) {
                    Notifications.getInstance().addList("Feature " + Formatting.GRAY + visibleName + Formatting.RESET + " - enabled!", 2000, null);
                    SoundManager.playSound(SoundManager.ENABLE_MODULE, volume, 1.0f);
                }
                activate();
            } else {
                if (Hud.getInstance().notificationSettings.isSelected("Module Switch")) {
                    Notifications.getInstance().addList("Feature " + Formatting.GRAY + visibleName + Formatting.RESET + " - disabled!", 2000, null);
                    SoundManager.playSound(SoundManager.DISABLE_MODULE, volume, 1.0f);
                }
                deactivate();
            }
        }
        toggleSilent(state);
    }

    private void toggleSilent(boolean activate) {
        EventManager eventManager = Zyrex.getInstance().getEventManager();
        if (activate) {
            eventManager.register(this);
        } else {
            eventManager.unregister(this);
        }
    }

    public void activate() {
    }

    public void deactivate() {
    }

    private void initBindSetting() {
        bindSetting = new BindSetting("Keybind", "Module keybind") {
            @Override
            public BindSetting setKey(int key) {
                Module.this.key = key;
                return super.setKey(key);
            }

            @Override
            public BindSetting setType(int type) {
                Module.this.type = type;
                return super.setType(type);
            }
        };
        bindSetting.setKey(key);
        bindSetting.setType(type);
        setup(bindSetting);
    }

    public void setKey(int key) {
        this.key = key;
        if (bindSetting != null) bindSetting.setKey(key);
    }

    public void setType(int type) {
        this.type = type;
        if (bindSetting != null) bindSetting.setType(type);
    }
}
