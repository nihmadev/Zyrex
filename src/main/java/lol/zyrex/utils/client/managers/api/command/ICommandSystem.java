package lol.zyrex.utils.client.managers.api.command;

import lol.zyrex.utils.client.managers.api.command.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
