package lol.zyrex.utils.client.managers.api.command.datatypes;

import lol.zyrex.utils.client.managers.api.command.exception.CommandException;
import lol.zyrex.utils.display.interfaces.QuickImports;

import java.util.stream.Stream;

public interface IDatatype extends QuickImports {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
