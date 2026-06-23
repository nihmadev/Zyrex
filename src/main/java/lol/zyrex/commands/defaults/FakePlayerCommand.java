package lol.zyrex.commands.defaults;

import com.mojang.authlib.GameProfile;
import lol.zyrex.common.repository.FakePlayerRepository;
import lol.zyrex.utils.client.managers.api.command.Command;
import lol.zyrex.utils.client.managers.api.command.argument.IArgConsumer;
import lol.zyrex.utils.client.managers.api.command.exception.CommandException;
import lol.zyrex.utils.client.managers.api.command.helpers.TabCompleteHelper;
import lol.zyrex.utils.display.interfaces.QuickImports;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

public class FakePlayerCommand extends Command implements QuickImports {

    private OtherClientPlayerEntity fakePlayer;
    private static final String FAKE_NAME = "FakePlayer";
    private static final UUID FAKE_UUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + FAKE_NAME).getBytes());

    public FakePlayerCommand() {
        super("fakeplayer", "fp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String action = args.peekString().toLowerCase(Locale.US);
            if (action.equals("remove") || action.equals("clear")) {
                args.get();
                removeFakePlayer();
                return;
            }
        }

        if (fakePlayer != null && mc.world != null && mc.world.getEntityById(fakePlayer.getId()) != null) {
            removeFakePlayer();
            logDirect("FakePlayer §7удалён.", Formatting.GREEN);
            return;
        }

        if (mc.world == null || mc.player == null) {
            logDirect("§cОшибка: мир или игрок равен null.", Formatting.RED);
            return;
        }

        spawnFakePlayer();
    }

    private void spawnFakePlayer() {
        GameProfile profile = new GameProfile(FAKE_UUID, FAKE_NAME);
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);

        fakePlayer.setPosition(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        fakePlayer.setYaw(mc.player.getYaw());
        fakePlayer.setPitch(mc.player.getPitch());
        fakePlayer.headYaw = mc.player.getYaw();
        fakePlayer.bodyYaw = mc.player.getYaw();

        fakePlayer.setInvulnerable(true);
        fakePlayer.setHealth(Float.MAX_VALUE);
        fakePlayer.setCustomNameVisible(false);
        fakePlayer.setCustomName(Text.literal(FAKE_NAME));

        FakePlayerRepository.add(FAKE_UUID);
        mc.world.addEntity(fakePlayer);
        logDirect("FakePlayer §7создан на вашей позиции. §f" + Formatting.GRAY + "Используйте .fp remove для удаления.", Formatting.GREEN);
    }

    private void removeFakePlayer() {
        if (fakePlayer != null) {
            Entity entity = mc.world.getEntityById(fakePlayer.getId());
            if (entity != null) {
                mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            }
            FakePlayerRepository.remove(FAKE_UUID);
            fakePlayer = null;
            logDirect("FakePlayer §7удалён.", Formatting.GREEN);
        } else {
            logDirect("§cНет активного FakePlayer.", Formatting.RED);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().prepend("remove", "clear").filterPrefix(args.peekString()).stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Создать/удалить фейкового игрока.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Создаёт бессмертного фейкового игрока на вашей позиции.",
                "Он стоит на месте и ничего не делает.",
                "Виден на ESP, Aura и TriggerBot.",
                "",
                "Использование:",
                "> fakeplayer — создать фейкового игрока",
                "> fakeplayer remove — удалить",
                "> fakeplayer clear — удалить"
        );
    }
}
