package lol.zyrex.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.display.color.ColorAssist;
import lol.zyrex.events.render.EntityColorEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeeInvisible extends Module {
    SliderSettings alphaSetting = new SliderSettings("Прозрачность", "Прозрачность игрока").setValue(0.5f).range(0.1F, 1);
    public SeeInvisible() {
        super("SeeInvisible", "See Invisible", ModuleCategory.RENDER);
        setup(alphaSetting);
    }

    @EventHandler
    public void onEntityColor(EntityColorEvent e) {
        if (!e.isInvisibleToPlayer()) return;
        e.setColor(ColorAssist.multAlpha(e.getColor(), alphaSetting.getValue()));
        e.cancel();
    }

}
