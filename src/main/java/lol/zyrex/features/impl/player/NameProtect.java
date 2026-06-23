package lol.zyrex.features.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.features.module.setting.implement.TextSetting;
import lol.zyrex.common.repository.friend.FriendUtils;
import lol.zyrex.events.render.TextFactoryEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NameProtect extends Module {
    TextSetting nameSetting = new TextSetting("Имя", "Никнейм, который будет заменен на ваш").setText("Protected").setMax(32);
    BooleanSetting friendsSetting = new BooleanSetting("Друзья", "Скрывает никнеймы друзей").setValue(true);

    public NameProtect() {
        super("NameProtect","Name Protect", ModuleCategory.RENDER);
        setup(friendsSetting);
    }

    @EventHandler
    public void onTextFactory(TextFactoryEvent e) {
        e.replaceText(mc.getSession().getUsername(), nameSetting.getText());
        if (friendsSetting.isValue()) FriendUtils.getFriends().forEach(friend -> e.replaceText(friend.getName(), nameSetting.getText()));
    }
}
