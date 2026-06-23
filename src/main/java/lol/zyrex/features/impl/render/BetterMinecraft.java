package lol.zyrex.features.impl.render;

import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.BooleanSetting;
import lol.zyrex.utils.client.Instance;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetterMinecraft extends Module {

    public static BetterMinecraft getInstance() {
        return Instance.get(BetterMinecraft.class);
    }

    BooleanSetting betterButton = new BooleanSetting("Кастомные кнопки", "язаипалсяэтопаститьспасите")
            .setValue(true);
    BooleanSetting tabVanishButton = new BooleanSetting("Спектаторы в табе", "язаипалсяэтопаститьспасите")
            .setValue(true);

    public BetterMinecraft() {
        super("BetterMinecraft", "Better Minecraft", ModuleCategory.RENDER);
        setup(betterButton, tabVanishButton);
    }

}
