package lol.zyrex.commands;

import lol.zyrex.utils.client.managers.api.command.ICommandSystem;
import lol.zyrex.utils.client.managers.api.command.argparser.IArgParserManager;
import lol.zyrex.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
