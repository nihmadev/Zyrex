package lol.zyrex.features.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.events.block.PushEvent;
import lol.zyrex.events.player.PlayerCollisionEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoPush extends Module {
    MultiSelectSetting ignoreSetting = new MultiSelectSetting("Игнорировать", "Разрешает выбранные вами действия")
            .value("Water", "Block", "Entity Collision", "Powder Snow", "Berry");
    public NoPush() {
        super("NoPush", "No Push", ModuleCategory.PLAYER);
        setup(ignoreSetting);
    }

    @EventHandler
    public void onPush(PushEvent e) {
        switch (e.getType()) {
            case PushEvent.Type.COLLISION -> e.setCancelled(ignoreSetting.isSelected("Entity Collision"));
            case PushEvent.Type.WATER -> e.setCancelled(ignoreSetting.isSelected("Water"));
            case PushEvent.Type.BLOCK -> e.setCancelled(ignoreSetting.isSelected("Block"));
        }
    }

    @EventHandler
    public void onPlayerCollision(PlayerCollisionEvent e) {
        Block block = e.getBlock();
        if (block.equals(Blocks.POWDER_SNOW)) e.setCancelled(ignoreSetting.isSelected("Powder Snow"));
        else if (block.equals(Blocks.SWEET_BERRY_BUSH)) e.setCancelled(ignoreSetting.isSelected("Berry"));
    }
}
