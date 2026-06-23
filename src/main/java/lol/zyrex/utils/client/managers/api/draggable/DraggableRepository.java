package lol.zyrex.utils.client.managers.api.draggable;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import lol.zyrex.display.hud.*;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DraggableRepository {
    List<AbstractDraggable> draggable = new ArrayList<>();

    public void setup() {
        register(
                new TargetHud(),
                new Potions(),
                new HotKeys(),
                new Watermark(),
                new CoolDowns(),
                new StaffList(),
                new Binds(),
                new Notifications(),
                new PlayerInfo(),
                new FuntimeEvents(),
                new MediaPlayer(),
                new Info()
        );
    }

    public void register(AbstractDraggable... module) {
        draggable.addAll(List.of(module));
    }

    public List<AbstractDraggable> draggable() {
        return draggable;
    }

    public <T extends AbstractDraggable> T get(String name) {
        return draggable.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .map(module -> (T) module)
                .findFirst()
                .orElse(null);
    }

    public <T extends AbstractDraggable> T get(Class<T> clazz) {
        return draggable.stream()
                .filter(module -> clazz.isAssignableFrom(module.getClass()))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}
