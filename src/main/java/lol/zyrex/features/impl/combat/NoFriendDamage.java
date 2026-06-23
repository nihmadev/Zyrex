package lol.zyrex.features.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.common.repository.friend.FriendUtils;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.events.player.AttackEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFriendDamage extends Module {
    public NoFriendDamage() {
        super("NoFriendDamage", "No Friend Damage", ModuleCategory.COMBAT);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        e.setCancelled(FriendUtils.isFriend(e.getEntity()));
    }
}

