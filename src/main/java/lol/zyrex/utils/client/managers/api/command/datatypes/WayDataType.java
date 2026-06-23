package lol.zyrex.utils.client.managers.api.command.datatypes;

import lol.zyrex.utils.client.managers.api.command.exception.CommandException;
import lol.zyrex.utils.client.managers.api.command.helpers.TabCompleteHelper;
import lol.zyrex.common.repository.way.Way;
import lol.zyrex.Zyrex;

import java.util.List;
import java.util.stream.Stream;

public enum WayDataType implements IDatatypeFor<Way> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<String> ways = getWay().stream().map(Way::name);
        String context = datatypeContext.getConsumer().getString();
        return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
    }

    @Override
    public Way get(IDatatypeContext datatypeContext) throws CommandException {
        String text = datatypeContext.getConsumer().getString();
        return getWay().stream().filter(s -> s.name().equalsIgnoreCase(text)).findFirst().orElse(null);
    }

    private List<? extends Way> getWay() {
        return Zyrex.getInstance().getWayRepository().wayList;
    }
}
