package lol.zyrex.events.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lol.zyrex.utils.client.managers.event.events.Event;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkyEvent implements Event {
    int skyColor;
    int cloudColor;
    float starBrightness;
    float rainStrength;
    float thunderStrength;
}
