package lol.zyrex.utils.client;

import lombok.experimental.UtilityClass;
import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.features.module.Module;
import lol.zyrex.Zyrex;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@UtilityClass
public class Instance {
    private final ConcurrentMap<Class<? extends Module>, Module> instanceModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends AbstractDraggable>, AbstractDraggable> instanceDraggables = new ConcurrentHashMap<>();

    public <T extends Module> T get(Class<T> clazz) {
        return clazz.cast(instanceModules.computeIfAbsent(clazz, instance -> Zyrex.getInstance().getModuleProvider().get(instance)));
    }

    public <T extends Module> T get(String module) {
        return Zyrex.getInstance().getModuleProvider().get(module);
    }

    public <T extends AbstractDraggable> T getDraggable(Class<T> clazz) {
        return clazz.cast(instanceDraggables.computeIfAbsent(clazz, instance -> Zyrex.getInstance().getDraggableRepository().get(instance)));
    }

    public <T extends AbstractDraggable> T getDraggable(String draggable) {
        return Zyrex.getInstance().getDraggableRepository().get(draggable);
    }
}
