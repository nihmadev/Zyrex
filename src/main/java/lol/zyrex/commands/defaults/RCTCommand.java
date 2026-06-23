package lol.zyrex.commands.defaults;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Formatting;

import lol.zyrex.utils.client.managers.api.command.Command;
import lol.zyrex.utils.client.managers.api.command.argument.IArgConsumer;
import lol.zyrex.utils.client.managers.api.command.exception.CommandException;
import lol.zyrex.common.repository.rct.RCTRepository;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.utils.client.packet.network.Network;
import lol.zyrex.Zyrex;
import lol.zyrex.display.hud.Notifications;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RCTCommand extends Command implements QuickImports {
    private final RCTRepository repository;

    protected RCTCommand(Zyrex main) {
        super("rct");
        repository = main.getRCTRepository();
    }

    
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        // Работает на FunTime и HolyWorld
        if (!Network.isHolyWorld() && !Network.isFunTime()) {
            Notifications.getInstance().addList("[RCT] Не работает на этом " + Formatting.RED + "сервере", 3000);
            return;
        }

        if (Network.isPvp()) {
            Notifications.getInstance().addList("[RCT] Вы находитесь в режиме " + Formatting.RED + "пвп", 3000);
            return;
        }

        if (args.hasAny()) {
            args.requireMin(1);
            int anarchy = args.getArgs().getFirst().getAs(Integer.class);
            repository.reconnect(anarchy);
        } else repository.reconnect(Network.getAnarchy());
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }


    @Override
    public String getShortDesc() {
        return "Перезаходит на анархию";
    }


    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Перезаходит на анархию",
                "",
                "Использование:",
                "> rct <anarchy> - Заходит на <anarchy>",
                "> rct - Перезаходит на анархию где вы только что были"
        );
    }
}