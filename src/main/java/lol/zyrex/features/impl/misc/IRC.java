package lol.zyrex.features.impl.misc;

import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.Zyrex;
import lol.zyrex.utils.client.chat.ChatMessage;

public class IRC extends Module {
    public IRC() {
        super("IRC", ModuleCategory.MISC);
    }

    @Override
    public void setState(boolean state) {
        super.setState(state);
        if (state) {
            activate();
        } else {
            deactivate();
        }
    }

    @Override
    public void activate() {
        Zyrex.getInstance().setShowIrcMessages(true);
        Zyrex.getInstance().getIrcManager().connect();
    }

    @Override
    public void deactivate() {
        Zyrex.getInstance().setShowIrcMessages(false);
        Zyrex.getInstance().getIrcManager().disconnect();
    }

    public void sendMessage(String message) {
        if (!isState()) {
            ChatMessage.ircmessageWithRed("Модуль IRC выключен");
            return;
        }
        if (Zyrex.getInstance().getIrcManager().getClient() != null && Zyrex.getInstance().getIrcManager().getClient().isOpen()) {
            Zyrex.getInstance().getIrcManager().getClient().sendMessage(message);
        }
    }
}