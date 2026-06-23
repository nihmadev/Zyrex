package lol.zyrex.commands.defaults;

import lol.zyrex.Zyrex;
import lol.zyrex.utils.client.managers.api.command.ICommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultCommands {
    public static List<ICommand> createAll() {
        Zyrex main = Zyrex.getInstance();
        List<ICommand> commands = new ArrayList<>(Arrays.asList(
                new ConfigCommand(main),
                new MacroCommand(main),
                new HelpCommand(main),
                new BindCommand(main),
                new WayCommand(main),
                new RCTCommand(main),
                new FriendCommand(),
                new IRCCommand(),
                new PrefixCommand(),
                new TargetCommand(),
                new StaffCommand(),
                new BlockESPCommand(),
                new TabParserCommand(),
                new FakePlayerCommand(),
                new NeuroCommand()
        ));
        return Collections.unmodifiableList(commands);
    }
}