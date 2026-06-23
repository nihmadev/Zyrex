package lol.zyrex.features.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.utils.client.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoInteract extends Module {
    public static NoInteract getInstance() {
        return Instance.get(NoInteract.class);
    }

    public NoInteract() {
        super("NoInteract", "No Interact", ModuleCategory.COMBAT);
    }
}
