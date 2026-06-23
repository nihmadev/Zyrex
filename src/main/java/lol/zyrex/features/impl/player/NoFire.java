package lol.zyrex.features.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFire extends Module {
    public NoFire() {
        super("NoFire", "No Fire", ModuleCategory.RENDER);
    }

    public static NoFire getInstance() {
        return Instance.get(NoFire.class);
    }
}
