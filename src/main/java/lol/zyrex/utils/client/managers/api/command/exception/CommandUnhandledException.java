package lol.zyrex.utils.client.managers.api.command.exception;

import lol.zyrex.utils.client.managers.api.command.ICommand;
import lol.zyrex.utils.client.managers.api.command.argument.ICommandArgument;
import lol.zyrex.utils.display.interfaces.QuickLogger;

import java.util.List;

public class CommandUnhandledException extends RuntimeException implements ICommandException, QuickLogger {

    public CommandUnhandledException(String message) {
        super(message);
    }

    public CommandUnhandledException(Throwable cause) {
        super(cause);
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
    }
}
