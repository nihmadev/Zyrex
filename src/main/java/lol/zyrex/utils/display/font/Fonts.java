package lol.zyrex.utils.display.font;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lol.zyrex.Zyrex;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Fonts {

    @SneakyThrows
    public static FontRenderer create(float size, String name) {
        String path = "assets/minecraft/fonts/" + name + ".ttf";

        try (InputStream inputStream = Objects.requireNonNull(Zyrex.class.getClassLoader().getResourceAsStream(path))) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(size / 2f);

            return new FontRenderer(font, size / 2f);
        }
    }

    private static final Map<FontKey, FontRenderer> fontCache = new HashMap<>();

    public static void init() {
        for (Type type : Type.values()) {
            for (int size = 4; size <= 32; size++) {
                fontCache.put(new FontKey(size, type), create(size, type.getType()));
            }
        }
    }



    public static FontRenderer getSize(int size) {
        return getSize(size, Type.INST);
    }

    public static FontRenderer getSize(int size, Type type) {
        return fontCache.computeIfAbsent(new FontKey(size, type), k -> create(size, type.getType()));
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {
        DEFAULT("GoogleSans-Regular"),
        REGULAR("GoogleSans-Regular"),
        SEMI("GoogleSans-Medium"),
        BOLD("GoogleSans-Bold"),
        BOLDED("GoogleSans-Bold"),
        MANROPEEXTRABOLD("GoogleSans-Bold"),
        MANROPEBOLD("GoogleSans-Bold"),
        RICHREGULAR("GoogleSans-Regular"),
        INST("GoogleSans-Regular"),
        ICONS("fa-solid-900"),
        ICONSTYPENEW("fa-solid-900"),
        GUIICONS("fa-solid-900"),
        ICONSCATEGORY("fa-solid-900"),
        ICONRICHREG("fa-solid-900"),
        FA_SOLID("fa-solid-900"),;

        private final String type;
    }

    private record FontKey(int size, Type type) {
    }
}