package lol.zyrex.utils.client.managers.api.command.datatypes;

import lol.zyrex.Zyrex;
import lol.zyrex.utils.client.managers.api.command.exception.CommandException;
import lol.zyrex.utils.client.managers.api.command.helpers.TabCompleteHelper;
import lol.zyrex.common.repository.macro.Macro;

import java.util.List;
import java.util.stream.Stream;

public enum MacroDataType implements IDatatypeFor<Macro> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<String> macros = getMacro()
                .stream()
                .map(Macro::name);

        String context = datatypeContext
                .getConsumer()
                .getString();

        return new TabCompleteHelper()
                .append(macros)
                .filterPrefix(context)
                .sortAlphabetically()
                .stream();
    }

    @Override
    public Macro get(IDatatypeContext datatypeContext) throws CommandException {
        String username = datatypeContext
                .getConsumer()
                .getString();

        return getMacro().stream()
                .filter(s -> s.name().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    private List<? extends Macro> getMacro() {
        return Zyrex.getInstance().getMacroRepository().macroList;
    }
}
