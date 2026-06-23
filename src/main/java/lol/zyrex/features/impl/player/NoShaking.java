package lol.zyrex.features.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.utils.client.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoShaking extends Module {
    public MultiSelectSetting modeSetting = new MultiSelectSetting("Режим", "Выберите эффекты для отключения")
            .value("Hurt Tilt", "View Bob")
            .selected("Hurt Tilt", "View Bob");

    public NoShaking() {
        super("NoShaking", "No Shaking", ModuleCategory.RENDER);
        setup(modeSetting);
    }

    public static NoShaking getInstance() {
        return Instance.get(NoShaking.class);
    }
}
