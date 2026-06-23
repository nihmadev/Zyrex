package lol.zyrex.features.impl.misc;

import antidaunleak.api.UserProfile;
import lol.zyrex.Zyrex;
import lol.zyrex.common.discord.DiscordManager;
import lol.zyrex.events.chat.ChatEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.chat.ChatMessage;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.file.FileRepository;
import lol.zyrex.utils.client.managers.file.exception.FileLoadException;
import lol.zyrex.utils.client.managers.file.impl.PrefixFile;
import lol.zyrex.utils.math.calc.Calculate;
import lol.zyrex.utils.math.time.StopWatch;
import lombok.experimental.NonFinal;
import org.apache.logging.log4j.core.appender.rolling.action.IfAll;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MathUtil;

public class SelfDestruct extends Module {
    public static boolean unhooked;

    public SelfDestruct() {
        super("SelfDestruct", "Self Destruct", ModuleCategory.MISC);
    }
    @NonFinal
    StopWatch timer = new StopWatch();

    @Override
    public void activate() {
        unhooked = true;

        Zyrex.getInstance().getDiscordManager().stopRPC();

        for (Module module : Zyrex.getInstance().getModuleProvider().getModules()) {
            if (module != this && module.isState()) {
                module.setState(false);
            }
        }

        ChatMessage.brandmessage("Для возвращения чита впишите в чат ваш username в чите");
        ChatMessage.brandmessage("Сообщение удалится через пол секунды");
        if (timer.every(500)) {
            mc.inGameHud.getChatHud().clear(true);
        }

        for (Module module : Zyrex.getInstance().getModuleProvider().getModules()) {
            module.setKey(GLFW.GLFW_KEY_UNKNOWN);
        }

        Zyrex.getInstance().getCommandDispatcher().prefix = "" + Calculate.getRandom(0, 9999999);

        super.activate();
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase(UserProfile.getInstance().profile("username"))) {
            unhooked = false;
            Zyrex.getInstance().getDiscordManager().setRunning(true);
            state = false;
            Zyrex.getInstance().getCommandDispatcher().prefix = ".";
            ChatMessage.brandmessage("Unhook reset to FALSE");
            event.setCancelled(true);
        }
    }

    @Override
    public void deactivate() {
        unhooked = false;
        super.deactivate();
    }
}
