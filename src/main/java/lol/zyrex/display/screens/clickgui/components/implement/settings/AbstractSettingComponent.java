package lol.zyrex.display.screens.clickgui.components.implement.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lol.zyrex.features.module.setting.Setting;
import lol.zyrex.display.screens.clickgui.components.AbstractComponent;

@Getter
@RequiredArgsConstructor
public abstract class AbstractSettingComponent extends AbstractComponent {
    private final Setting setting;
}
