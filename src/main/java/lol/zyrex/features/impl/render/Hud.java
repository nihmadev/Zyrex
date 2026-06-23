package lol.zyrex.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends Module {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса").value("Watermark", "Hot Keys", "Potions", "Staff List", "Target Hud", "Binds", "Cool Downs", "Player Info", "Notifications", "Hot Keys", "Funtime Events", "Media Player", "Info")
            .selected("Watermark", "Hot Keys", "Potions", "Staff List", "Target Hud", "Binds", "Cool Downs", "Player Info", "Notifications", "Funtime Events", "Media Player", "Info");

    public MultiSelectSetting notificationSettings = new MultiSelectSetting("Уведомления", "Выберите, когда будут появляться уведомления")
            .value("Module Switch", "Staff Join", "Staff Leave", "Item Pick Up", "Auto Armor", "Break Shield").selected("Module Switch", "Item Pick Up", "Auto Armor", "Break Shield").visible(()-> interfaceSettings.isSelected("Notifications"));

    public SliderSettings soundVolumeSetting = new SliderSettings("Sound Volume", "Volume for module switch sounds")
            .range(0.0f, 1.0f)
            .setValue(1.0f)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public float getModuleVolume() {
        return soundVolumeSetting.getValue();
    }

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        setup(interfaceSettings, notificationSettings, soundVolumeSetting);
    }
}