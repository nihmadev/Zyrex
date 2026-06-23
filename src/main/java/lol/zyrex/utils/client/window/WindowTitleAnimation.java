package lol.zyrex.utils.client.window;

import antidaunleak.api.UserProfile;
import lol.zyrex.features.impl.misc.SelfDestruct;

public class WindowTitleAnimation {
    private static final WindowTitleAnimation INSTANCE = new WindowTitleAnimation();
    private static final String DISPLAY_USERNAME = "Boolean User";
    private String currentTitle = "<User: " + DISPLAY_USERNAME + ">";
    private int animationTick = 0;
    private boolean isRemoving = true;
    private boolean isUserPhase = true;
    private int pauseTicks = 0;
    private final int delayTicks = 1;
    private final int pauseDuration = 100;

    private WindowTitleAnimation() {}

    public static WindowTitleAnimation getInstance() {
        return INSTANCE;
    }

    public void updateTitle() {
        if (pauseTicks > 0) {
            pauseTicks--;
            return;
        }
        if (animationTick >= delayTicks) {
            String newTitle;
            String fullText = isUserPhase ? "<User: " + DISPLAY_USERNAME + ">" : "<Uid: " + UserProfile.getInstance().profile("uid") + ">";
            if (isRemoving) {
                if (currentTitle.length() > 1) {
                    newTitle = currentTitle.substring(0, currentTitle.length() - 1);
                } else {
                    newTitle = "<";
                    isRemoving = false;
                }
            } else {
                if (currentTitle.length() < fullText.length()) {
                    newTitle = fullText.substring(0, currentTitle.length() + 1);
                } else {
                    newTitle = fullText;
                    isRemoving = true;
                    isUserPhase = !isUserPhase;
                    pauseTicks = pauseDuration;
                }
            }
            currentTitle = newTitle;
            animationTick = 0;
        }
        animationTick++;
    }

    public String getCurrentTitle() {
        return "Zyrex 1.21.4 " + currentTitle;
    }
}