package lol.zyrex.commands.defaults;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import lol.zyrex.utils.client.managers.file.FileRepository;
import lol.zyrex.Zyrex;
import lol.zyrex.main.client.ClientInfoProvider;
import lol.zyrex.utils.client.managers.api.command.Command;
import lol.zyrex.utils.client.managers.api.command.argument.IArgConsumer;
import lol.zyrex.utils.client.managers.api.command.datatypes.ConfigFileDataType;
import lol.zyrex.utils.client.managers.api.command.exception.CommandException;
import lol.zyrex.utils.client.managers.api.command.helpers.Paginator;
import lol.zyrex.utils.client.managers.api.command.helpers.TabCompleteHelper;
import lol.zyrex.utils.client.managers.file.FileController;
import lol.zyrex.utils.client.managers.file.exception.FileProcessingException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static lol.zyrex.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigCommand extends Command {
    FileController fileController;
    ClientInfoProvider clientInfoProvider;

    protected ConfigCommand(Zyrex main) {
        super("config", "cfg");
        this.fileController = main.getFileController();
        this.clientInfoProvider = main.getClientInfoProvider();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        args.requireMax(1);
        if (arg.contains("load")) {
            String name = args.getString();
            File customDir = new File(clientInfoProvider.clientDir(), "Custom");
            if (new File(customDir, name + ".json").exists()) {
                try {
                    var fileRepository = new FileRepository();
                    fileRepository.setup(Zyrex.getInstance());
                    var fileController = new FileController(fileRepository.getClientFiles(), clientInfoProvider.filesDir());
                    fileController.loadFile(name + ".json");
                    logDirect(String.format("Конфигурация %s загружена!", name));
                } catch (FileProcessingException e) {
                    logDirect(String.format("Ошибка при загрузке конфига! Детали: %s", e.getCause().getMessage()), Formatting.RED);
                }
            } else {
                logDirect(String.format("Конфигурация %s не найдена!", name));
            }
        }
        if (arg.contains("save")) {

            String name = args.getString();

            try {
                fileController.saveFile(name + ".json");
                logDirect(String.format("Конфигурация %s сохранена!", name));
                System.out.println("loaded");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.printf("error %s%n", e.getCause().getMessage());
                logDirect(String.format("Ошибка при сохранении конфига! Детали: %s", e.getCause().getMessage()), Formatting.RED);
            }
        }
        if (arg.contains("list")) {
            Paginator.paginate(
                    args, new Paginator<>(
                            getConfigs()),
                    () -> logDirect("Список конфигов:"),
                    config -> {
                        MutableText namesComponent = Text.literal(config);
                        namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
                        return namesComponent;
                    },
                    FORCE_COMMAND_PREFIX + label
            );
        }
        if (arg.contains("dir")) {
            try {
                File customDir = new File(clientInfoProvider.clientDir(), "Custom");
                Runtime.getRuntime().exec("explorer " + customDir.getAbsolutePath());
            } catch (IOException e) {
                logDirect("Папка с конфигурациями не найдена!" + e.getMessage());
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (args.hasExactlyOne()) {
                if (arg.equalsIgnoreCase("load")) {
                    return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
                } else if (arg.equalsIgnoreCase("save")) {
                    return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
                }
            } else {
                return new TabCompleteHelper()
                        .sortAlphabetically()
                        .prepend("load", "save", "list", "dir")
                        .filterPrefix(arg)
                        .stream();
            }
        }
        return Stream.empty();
    }


    @Override
    public String getShortDesc() {
        return "Позволяет взаимодействовать с конфигами в чите";
    }


    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "С помощью этой команды можно загружать/сохранять конфиги",
                "",
                "Использование:",
                "> config load <name> - Загружает конфиг.",
                "> config save <name> - Сохраняет конфиг.",
                "> config list - Возвращает список конфигов",
                "> config dir - Открывает папку с конфигами."
        );
    }

    public List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        File customDir = new File(Zyrex.getInstance().getClientInfoProvider().clientDir(), "Custom");
        File[] configFiles = customDir.listFiles();

        if (configFiles != null) {
            for (File configFile : configFiles) {
                if (configFile.isFile() && configFile.getName().endsWith(".json")) {
                    String configName = configFile.getName().replace(".json", "");
                    configs.add(configName);
                }
            }
        }

        return configs;
    }
}