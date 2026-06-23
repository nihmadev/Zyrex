package lol.zyrex.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lol.zyrex.utils.client.managers.event.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
