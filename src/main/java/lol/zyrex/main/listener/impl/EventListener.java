package lol.zyrex.main.listener.impl;

import lol.zyrex.utils.interactions.inv.InventoryFlowManager;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.client.managers.api.draggable.AbstractDraggable;
import lol.zyrex.utils.client.packet.network.Network;
import lol.zyrex.Zyrex;
import lol.zyrex.main.listener.Listener;
import lol.zyrex.events.item.UsingItemEvent;
import lol.zyrex.events.packet.PacketEvent;
import lol.zyrex.events.player.TickEvent;

public class EventListener implements Listener {
    public static boolean serverSprint;
    public static int selectedSlot;

    @EventHandler
    public void onTick(TickEvent e) {
        Network.tick();
        Zyrex.getInstance().getAttackPerpetrator().tick();
        InventoryFlowManager.tick();
        Zyrex.getInstance().getDraggableRepository().draggable().forEach(AbstractDraggable::tick);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case ClientCommandC2SPacket command -> serverSprint = switch (command.getMode()) {
                case ClientCommandC2SPacket.Mode.START_SPRINTING -> true;
                case ClientCommandC2SPacket.Mode.STOP_SPRINTING -> false;
                default -> serverSprint;
            };
            case UpdateSelectedSlotC2SPacket slot -> selectedSlot = slot.getSelectedSlot();
            default -> {}
        }
        Network.packet(e);
        Zyrex.getInstance().getAttackPerpetrator().onPacket(e);
        Zyrex.getInstance().getDraggableRepository().draggable().forEach(drag -> drag.packet(e));
    }

    @EventHandler
    public void onUsingItemEvent(UsingItemEvent e) {
        Zyrex.getInstance().getAttackPerpetrator().onUsingItem(e);
    }
}
