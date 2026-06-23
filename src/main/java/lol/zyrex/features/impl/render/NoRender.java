package lol.zyrex.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.utils.client.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoRender extends Module {
    public static NoRender getInstance() {
        return Instance.get(NoRender.class);
    }

    public MultiSelectSetting modeSetting = new MultiSelectSetting("Элементы", "Выберите элементы для игнорирования")
            .value("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage")
            .selected("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage");

    public NoRender() {
        super("NoRender", "No Render", ModuleCategory.RENDER);
        setup(modeSetting);
    }
}