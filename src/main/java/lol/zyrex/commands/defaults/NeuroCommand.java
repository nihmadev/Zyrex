package lol.zyrex.commands.defaults;

import lol.zyrex.utils.client.managers.api.command.Command;
import lol.zyrex.utils.client.managers.api.command.argument.IArgConsumer;
import lol.zyrex.utils.client.managers.api.command.exception.CommandException;
import lol.zyrex.utils.client.managers.api.command.helpers.TabCompleteHelper;
import lol.zyrex.utils.features.aura.neuro.NeuroProfile;
import lol.zyrex.utils.features.aura.neuro.NeuroRecorder;
import lol.zyrex.utils.features.aura.neuro.NeuroRepository;
import lol.zyrex.Zyrex;
import net.minecraft.util.Formatting;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * .neuro record <name> — начать запись
 * .neuro stop          — остановить запись и сохранить
 * .neuro list          — список всех профилей
 * .neuro set <name>    — установить активный профиль для режима Neuro
 */
public class NeuroCommand extends Command {

    public NeuroCommand() {
        super("neuro");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            printUsage();
            return;
        }

        String flag = args.getString().toLowerCase(Locale.US);

        switch (flag) {

            case "record" -> {
                if (!args.hasAny()) {
                    logDirect("Укажи имя профиля: .neuro record <name>", Formatting.RED);
                    return;
                }
                String name = args.getString();
                if (NeuroRecorder.getInstance().isRecording()) {
                    logDirect("Запись уже идёт! Сначала останови: .neuro stop", Formatting.YELLOW);
                    return;
                }
                NeuroProfile profile = new NeuroProfile(name);
                // Регистрируем рекордер в EventManager
                Zyrex.getInstance().getEventManager().register(NeuroRecorder.getInstance());
                NeuroRecorder.getInstance().startRecording(profile);
                logDirect("Запись профиля «" + name + "» начата. Бейся как обычно, потом .neuro stop", Formatting.GREEN);
            }

            case "stop" -> {
                if (!NeuroRecorder.getInstance().isRecording()) {
                    logDirect("Запись не ведётся.", Formatting.RED);
                    return;
                }
                NeuroProfile finished = NeuroRecorder.getInstance().stopRecording();
                Zyrex.getInstance().getEventManager().unregister(NeuroRecorder.getInstance());
                if (finished == null || finished.getFrames().isEmpty()) {
                    logDirect("Профиль пустой — данные не сохранены.", Formatting.RED);
                    return;
                }
                NeuroRepository.getInstance().save(finished);
                logDirect(String.format(
                        "Профиль «%s» сохранён. Фреймов: %d | Задержка: %.0f мс | Промахи: %.0f%%",
                        finished.getName(),
                        finished.getFrames().size(),
                        finished.getAvgAttackDelay(),
                        finished.getMissRate() * 100
                ), Formatting.GREEN);
            }

            case "list" -> {
                List<NeuroProfile> profiles = NeuroRepository.getInstance().getProfiles();
                if (profiles.isEmpty()) {
                    logDirect("Нет сохранённых профилей. Запиши первый: .neuro record <name>", Formatting.YELLOW);
                    return;
                }
                NeuroProfile active = NeuroRepository.getInstance().getActiveProfile();
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm");
                logDirect("── Neuro профили ──────────────────", Formatting.AQUA);
                for (int i = 0; i < profiles.size(); i++) {
                    NeuroProfile p = profiles.get(i);
                    boolean isActive = active != null && active.getName().equals(p.getName());
                    String marker = isActive ? " §a[активный]§r" : "";
                    logDirect(String.format(
                            "%d. §e%s§r%s — %d фр | delay=%.0fмс | miss=%.0f%% | dist=%.1f | %s",
                            i + 1,
                            p.getName(),
                            marker,
                            p.getFrames().size(),
                            p.getAvgAttackDelay(),
                            p.getMissRate() * 100,
                            p.getAvgDistance(),
                            sdf.format(new Date(p.getCreatedAt()))
                    ));
                }
            }

            case "set" -> {
                if (!args.hasAny()) {
                    logDirect("Укажи имя профиля: .neuro set <name>", Formatting.RED);
                    return;
                }
                String name = args.getString();
                NeuroProfile profile = NeuroRepository.getInstance().getByName(name);
                if (profile == null) {
                    logDirect("Профиль «" + name + "» не найден. Посмотри список: .neuro list", Formatting.RED);
                    return;
                }
                NeuroRepository.getInstance().setActiveProfile(profile);
                logDirect("Активный профиль установлен: §e" + profile.getName() + "§r. Выбери режим Neuro в ауре.", Formatting.GREEN);
            }

            default -> printUsage();
        }
    }

    private void printUsage() {
        logDirect("── .neuro ──────────────────────────", Formatting.AQUA);
        logDirect(".neuro record <name> — начать запись стиля игры");
        logDirect(".neuro stop          — остановить запись и сохранить");
        logDirect(".neuro list          — список всех профилей");
        logDirect(".neuro set <name>    — установить профиль для режима Neuro");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) return Stream.empty();
        String first = args.getString();
        if (!args.hasAny()) {
            return new TabCompleteHelper()
                    .prepend("record", "stop", "list", "set")
                    .filterPrefix(first)
                    .stream();
        }
        // Второй аргумент для set — имена профилей
        if (first.equalsIgnoreCase("set")) {
            String prefix = args.getString();
            return NeuroRepository.getInstance().getProfiles().stream()
                    .map(NeuroProfile::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()));
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление Neuro-профилями ауры";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Neuro — режим ауры, обучающийся твоему стилю игры.",
                "",
                "Использование:",
                "> .neuro record <name> — начать запись поведения",
                "> .neuro stop          — остановить запись и сохранить профиль",
                "> .neuro list          — список всех сохранённых профилей",
                "> .neuro set <name>    — выбрать профиль для режима Neuro в ауре"
        );
    }
}
