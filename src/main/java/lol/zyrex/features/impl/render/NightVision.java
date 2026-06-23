package lol.zyrex.features.impl.render;

import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.client.Instance;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NightVision extends Module {
    public static NightVision getInstance() {
        return Instance.get(NightVision.class);
    }

    public final SliderSettings brightness = new SliderSettings("Яркость", "Уровень яркости ночного зрения")
            .setValue(10.0F).range(0.0F, 10.0F);

    public NightVision() {
        super("NightVision", "Night Vision", ModuleCategory.RENDER);
        setup(brightness);
    }
}
