package lol.zyrex.common.discord.callbacks;

import com.sun.jna.Callback;
import lol.zyrex.common.discord.utils.DiscordUser;

public interface ReadyCallback extends Callback {
    void apply(DiscordUser var1);
}