package lol.zyrex.utils.features.autobuy;

import lol.zyrex.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import lol.zyrex.display.screens.clickgui.components.implement.autobuy.manager.AutoBuyManager;
import lol.zyrex.display.screens.clickgui.components.implement.autobuy.autobuyui.PurchaseHistoryWindow;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PurchaseHandler {
    private static final Pattern PURCHASE_PATTERN = Pattern.compile("Вы успешно купили (.+?) за \\$([\\d,]+)!");

    public static void handlePurchaseMessage(String message, AutoBuyManager autoBuyManager) {
        Matcher matcher = PURCHASE_PATTERN.matcher(message);
        if (matcher.find()) {
            String itemName = matcher.group(1);
            String priceStr = matcher.group(2).replace(",", "");
            try {
                int price = Integer.parseInt(priceStr);
                AutoBuyableItem purchasedItem = autoBuyManager.getAllItems().stream()
                        .filter(item -> item.getDisplayName().equals(itemName))
                        .findFirst()
                        .orElse(null);
                if (purchasedItem != null) {
                    PurchaseHistoryWindow.addPurchase(purchasedItem, price);
                } else {
                    PurchaseHistoryWindow.addPurchase(itemName, price);
                }
            } catch (NumberFormatException ignored) {}
        }
    }
}