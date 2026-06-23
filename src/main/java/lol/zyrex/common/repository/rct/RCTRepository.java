package lol.zyrex.common.repository.rct;

import lol.zyrex.utils.interactions.inv.InventoryTask;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.event.EventManager;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.utils.display.interfaces.QuickLogger;
import lol.zyrex.utils.client.packet.network.Network;
import lol.zyrex.utils.math.time.StopWatch;
import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.events.player.TickEvent;
import lol.zyrex.display.hud.Notifications;
import org.apache.commons.lang3.StringUtils;

public class RCTRepository implements QuickImports, QuickLogger {
    private final StopWatch stopWatch = new StopWatch();
    private final StopWatch funtimeStopWatch = new StopWatch();
    private final StopWatch holyStopWatch = new StopWatch();
    
    private boolean lobby;
    private int anarchy;
    
    // FunTime RCT
    private boolean isFunTimeRct = false;
    private String funtimeAnarchy = "";
    private int funtimeStep = 0;
    
    // HolyWorld RCT
    private boolean isHolyWorldRct = false;
    private int holyStep = 0;
    private String holyLiteType = ""; // СолоЛайт, ДуоЛайт, ТриоЛайт, КланЛайт
    private int holyAnarchyNumber = 0;

    public RCTRepository(EventManager eventManager) {eventManager.register(this);}

    @EventHandler
    public void onPacket(PacketEvent e) {
        if ((anarchy != 0 || isHolyWorldRct || isFunTimeRct) && e.getPacket() instanceof GameMessageS2CPacket message) {
            String text = message.content().getString().toLowerCase();
            if (!text.contains("хаб") && text.contains("не удалось")) {
                Notifications.getInstance().addList("[RCT] На данную анархию " + Formatting.RED + "нельзя" + Formatting.RESET + " зайти", 3000);
                resetAll();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        
        // FunTime RCT
        if (isFunTimeRct && Network.isFunTime()) {
            handleFunTimeRct();
            return;
        }
        
        // HolyWorld RCT (новая логика)
        if (isHolyWorldRct && isHolyWorldServer()) {
            handleHolyWorldRctNew();
            return;
        }
        
        if (anarchy == 0) return;

        // Старая HolyWorld RCT логика (для совместимости)
        if (Network.isHolyWorld()) {
            handleHolyWorldRct();
        }
    }
    
    /**
     * Проверка что сервер HolyWorld по IP
     */
    private boolean isHolyWorldServer() {
        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) return false;
        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        return serverIp.contains("holyworld") || serverIp.contains("hollyworld");
    }
    
    /**
     * Новая логика HolyWorld RCT через скорборд
     */
    private void handleHolyWorldRctNew() {
        switch (holyStep) {
            case 0 -> {
                // Шаг 0: Читаем скорборд и сохраняем данные
                if (readHolyWorldScoreboard()) {
                    mc.player.networkHandler.sendChatCommand("hub");
                    holyStep = 1;
                    holyStopWatch.reset();
                    Notifications.getInstance().addList("[RCT] Тип: " + holyLiteType + ", Анархия: #" + holyAnarchyNumber, 2000);
                } else {
                    Notifications.getInstance().addList("[RCT] Не удалось прочитать скорборд", 3000);
                    resetAll();
                }
            }
            case 1 -> {
                // Шаг 1: Ждём появления компаса в 1 слоте
                if (mc.currentScreen != null) return; // Ждём пока закроются все меню
                
                ItemStack firstSlot = mc.player.getInventory().getStack(0);
                // Проверяем что в первом слоте компас
                if (firstSlot.getItem() == Items.COMPASS || firstSlot.getItem() == Items.RECOVERY_COMPASS) {
                    if (mc.player.getInventory().selectedSlot != 0) {
                        mc.player.getInventory().selectedSlot = 0;
                    }
                    holyStep = 2;
                    holyStopWatch.reset();
                }
                // Таймаут на случай если компас не появился
                else if (holyStopWatch.finished(5000)) {
                    Notifications.getInstance().addList("[RCT] Компас не найден", 3000);
                    resetAll();
                }
            }
            case 2 -> {
                // Шаг 2: Небольшая задержка и кликаем ПКМ
                if (holyStopWatch.finished(100)) {
                    if (mc.currentScreen != null) return;
                    
                    mc.options.useKey.setPressed(true);
                    holyStep = 3;
                    holyStopWatch.reset();
                }
            }
            case 3 -> {
                // Шаг 3: Отпускаем ПКМ
                if (holyStopWatch.finished(50)) {
                    mc.options.useKey.setPressed(false);
                    holyStep = 4;
                    holyStopWatch.reset();
                }
            }
            case 4 -> {
                // Шаг 4: Ждём открытия меню и кликаем на предмет "Лайт" с "1.16"
                if (holyStopWatch.finished(300)) {
                    if (mc.currentScreen instanceof GenericContainerScreen screen) {
                        int slot = findLiteSlot(screen);
                        if (slot != -1) {
                            InventoryTask.clickSlot(slot, 0, SlotActionType.PICKUP, false);
                            holyStep = 5;
                            holyStopWatch.reset();
                        } else {
                            Notifications.getInstance().addList("[RCT] Не найден слот Лайт 1.16", 3000);
                            resetAll();
                        }
                    }
                }
            }
            case 5 -> {
                // Шаг 5: Кликаем на нужную стойку по типу лайта
                if (holyStopWatch.finished(200)) {
                    if (mc.currentScreen instanceof GenericContainerScreen screen) {
                        int slot = getLiteTypeSlot();
                        if (slot != -1) {
                            InventoryTask.clickSlot(slot, 0, SlotActionType.PICKUP, false);
                        }
                        holyStep = 6;
                        holyStopWatch.reset();
                    }
                }
            }
            case 6 -> {
                // Шаг 6: Кликаем на предмет с нужным количеством (номер анархии)
                if (holyStopWatch.finished(200)) {
                    if (mc.currentScreen instanceof GenericContainerScreen screen) {
                        int slot = findSlotByItemCount(screen, holyAnarchyNumber);
                        if (slot != -1) {
                            InventoryTask.clickSlot(slot, 0, SlotActionType.PICKUP, false);
                            Notifications.getInstance().addList("[RCT] Переподключение на #" + holyAnarchyNumber, 2000);
                        } else {
                            Notifications.getInstance().addList("[RCT] Не найден слот для анархии #" + holyAnarchyNumber, 3000);
                        }
                        resetAll();
                    }
                }
            }
        }
    }
    
    /**
     * Читает скорборд HolyWorld и извлекает тип лайта и номер анархии
     */
    private boolean readHolyWorldScoreboard() {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return false;
        
        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            String text = Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name()).getString();
            if (text.isEmpty()) continue;
            
            // Ищем тип лайта
            if (text.contains("СолоЛайт")) holyLiteType = "СолоЛайт";
            else if (text.contains("ДуоЛайт")) holyLiteType = "ДуоЛайт";
            else if (text.contains("ТриоЛайт")) holyLiteType = "ТриоЛайт";
            else if (text.contains("КланЛайт")) holyLiteType = "КланЛайт";
            
            // Ищем номер анархии (#21, #32 и т.д.)
            String anarchyStr = StringUtils.substringBetween(text, "#", " ");
            if (anarchyStr == null) anarchyStr = StringUtils.substringAfter(text, "#");
            if (anarchyStr != null && !anarchyStr.isEmpty()) {
                try {
                    // Убираем лишние символы
                    anarchyStr = anarchyStr.replaceAll("[^0-9]", "");
                    if (!anarchyStr.isEmpty()) {
                        holyAnarchyNumber = Integer.parseInt(anarchyStr);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        return holyAnarchyNumber > 0;
    }
    
    private boolean isLiteType() {
        return holyLiteType.equals("СолоЛайт") || holyLiteType.equals("ДуоЛайт") || 
               holyLiteType.equals("ТриоЛайт") || holyLiteType.equals("КланЛайт");
    }
    
    /**
     * Находит слот с предметом "Лайт" и "1.16" в названии или описании
     */
    private int findLiteSlot(GenericContainerScreen screen) {
        var inventory = screen.getScreenHandler().getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            
            // Проверяем название предмета
            String name = stack.getName().getString().toLowerCase();
            boolean hasLite = name.contains("лайт") || name.contains("lite");
            boolean has116 = name.contains("1.16");
            
            // Проверяем lore через компоненты
            var loreComponent = stack.get(net.minecraft.component.DataComponentTypes.LORE);
            if (loreComponent != null) {
                for (var line : loreComponent.lines()) {
                    String lineStr = line.getString().toLowerCase();
                    if (lineStr.contains("лайт") || lineStr.contains("lite")) hasLite = true;
                    if (lineStr.contains("1.16")) has116 = true;
                }
            }
            
            if (hasLite && has116) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Возвращает слот для клика по типу лайта (количество стоек)
     */
    private int getLiteTypeSlot() {
        return switch (holyLiteType) {
            case "СолоЛайт" -> findSlotByItemCount(null, 1);   // 1 стойка
            case "ДуоЛайт" -> findSlotByItemCount(null, 2);    // 2 стойки
            case "ТриоЛайт" -> findSlotByItemCount(null, 3);   // 3 стойки
            case "КланЛайт" -> findSlotByItemCount(null, 16);  // 16 стоек
            default -> -1;
        };
    }
    
    /**
     * Находит слот с предметом указанного количества
     */
    private int findSlotByItemCount(GenericContainerScreen screen, int count) {
        if (screen == null && mc.currentScreen instanceof GenericContainerScreen s) {
            screen = s;
        }
        if (screen == null) return -1;
        
        var inventory = screen.getScreenHandler().getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getCount() == count) {
                return i;
            }
        }
        return -1;
    }
    
    private void resetAll() {
        anarchy = 0;
        isFunTimeRct = false;
        funtimeStep = 0;
        funtimeAnarchy = "";
        isHolyWorldRct = false;
        holyStep = 0;
        holyLiteType = "";
        holyAnarchyNumber = 0;
    }
    
    private void handleFunTimeRct() {
        if (funtimeStep == 0) {
            for (ScoreboardObjective team : mc.world.getScoreboard().getObjectives()) {
                String an = team.getDisplayName().getString();
                if (an.contains("Анархия-")) {
                    funtimeAnarchy = an.split("Анархия-")[1];
                    mc.player.networkHandler.sendChatCommand("hub");
                    funtimeStep = 1;
                    funtimeStopWatch.reset();
                    break;
                }
            }
            if (funtimeAnarchy.isEmpty()) {
                Notifications.getInstance().addList("[RCT] Не удалось определить анархию", 3000);
                isFunTimeRct = false;
            }
            return;
        }
        
        if (funtimeStep == 1) {
            if (funtimeStopWatch.finished(1250)) {
                mc.player.networkHandler.sendChatCommand("an" + funtimeAnarchy);
                Notifications.getInstance().addList("[RCT] Переподключение на an" + funtimeAnarchy, 2000);
                funtimeStep = 0;
                isFunTimeRct = false;
                funtimeAnarchy = "";
            }
        }
    }
    
    private void handleHolyWorldRct() {
        if (!Network.isHolyWorld()) {
            anarchy = 0;
            return;
        }

        int currentAnarchy = Network.getAnarchy();
        if (lobby) {
            if (currentAnarchy == -1) lobby = false;
            else mc.player.networkHandler.sendChatCommand("hub");
            return;
        }

        if (currentAnarchy == anarchy) {
            anarchy = 0;
            return;
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen && screen.getTitle().getString().equals("Выбор Лайт анархии:"))  {
            boolean secondScreen = screen.getScreenHandler().getInventory().size() < 10;
            int[] slots = anarchy < 15 ? new int[]{0, 0} : anarchy < 33 ? new int[]{1, 14} : anarchy < 48 ? new int[]{2, 32} : new int[]{3, 47};
            if (secondScreen) InventoryTask.clickSlot(slots[0], 0, SlotActionType.PICKUP, false);
            else InventoryTask.clickSlot(17 + anarchy - slots[1], 0, SlotActionType.PICKUP, false);
            return;
        }

        if (stopWatch.every(500)) mc.player.networkHandler.sendChatCommand("lite");
    }

    public void reconnect(int anarchy) {
        if (Network.isFunTime()) {
            isFunTimeRct = true;
            funtimeStep = 0;
            funtimeAnarchy = "";
            Notifications.getInstance().addList("[RCT] Запуск FunTime RCT...", 2000);
        } else if (isHolyWorldServer()) {
            // Новая HolyWorld RCT логика
            isHolyWorldRct = true;
            holyStep = 0;
            holyLiteType = "";
            holyAnarchyNumber = 0;
            Notifications.getInstance().addList("[RCT] Запуск HolyWorld RCT...", 2000);
        } else if (Network.isHolyWorld()) {
            // Старая логика для совместимости
            if (anarchy > 0 && anarchy < 64) {
                this.anarchy = anarchy;
                this.lobby = true;
            } else {
                Notifications.getInstance().addList("[RCT] Не верный " + Formatting.RED + "лайт", 3000);
            }
        }
    }
}
