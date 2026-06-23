package lol.zyrex.mixins.player.inventory;

import lol.zyrex.features.impl.misc.SelfDestruct;
import lol.zyrex.utils.interactions.inv.InventorySorter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void addDropAllButton(CallbackInfo ci) {
        if (SelfDestruct.unhooked) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;

        int guiTop = (screen.height - screen.backgroundHeight) / 2;
        int totalWidth = 164;
        int btnX = (screen.width - totalWidth) / 2;
        int btnY = guiTop - 22;

        ButtonWidget dropAllButton = ButtonWidget.builder(
                Text.of("Выкинуть всё"),
                button -> dropAllItems(mc)
        ).position(btnX, btnY).size(80, 20).build();

        screen.addDrawableChild(dropAllButton);

        ButtonWidget sortButton = ButtonWidget.builder(
                Text.of("Сортировать"),
                button -> InventorySorter.sortInventory()
        ).position(btnX + 84, btnY).size(80, 20).build();

        screen.addDrawableChild(sortButton);
    }

    private void dropAllItems(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || player.currentScreenHandler == null) return;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        i,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        i + 36,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }
    }
}