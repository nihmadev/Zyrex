package lol.zyrex.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.events.render.AspectRatioEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AspectRatio extends Module {

    SliderSettings ratioSetting = new SliderSettings("Соотношение", "Настройка значения соотношения сторон")
            .setValue(1.0F).range(0.1F, 2.0F);


    public AspectRatio() {
        super("AspectRatio", "Aspect Ratio", ModuleCategory.RENDER);
        setup(ratioSetting);
    }

    @EventHandler
    public void onAspectRatio(AspectRatioEvent e) {
            e.setRatio(ratioSetting.getValue());
            e.cancel();
    }
}