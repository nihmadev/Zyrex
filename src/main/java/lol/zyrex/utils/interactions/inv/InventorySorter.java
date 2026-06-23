package lol.zyrex.utils.interactions.inv;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;
import lol.zyrex.utils.display.interfaces.QuickImports;

import java.util.*;

public class InventorySorter implements QuickImports {

    private enum SlotCat {
        MELEE, TOOL, FOOD, POTION, RANGED, TOTEM, PEARL, BUCKET, SHIELD
    }

    public static void sortInventory() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (InventoryTask.isServerScreen()) return;

        Map<ItemStack, Integer> itemToSlot = new IdentityHashMap<>();
        Map<Integer, ItemStack> slotToItem = new HashMap<>();

        for (int slot = 9; slot <= 44; slot++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(slot).getStack();
            slotToItem.put(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            if (!stack.isEmpty()) {
                itemToSlot.put(stack, slot);
            }
        }

        List<ItemStack> all = new ArrayList<>(itemToSlot.keySet());
        all.sort((a, b) -> {
            int pa = prio(cat(a)), pb = prio(cat(b));
            if (pa != pb) return Integer.compare(pb, pa);
            return Double.compare(score(b), score(a));
        });

        List<Integer> hotbarSlots = new ArrayList<>();
        for (int i = 36; i <= 44; i++) hotbarSlots.add(i);
        List<Integer> invSlots = new ArrayList<>();
        for (int i = 9; i <= 35; i++) invSlots.add(i);

        Map<ItemStack, Integer> targetMap = new IdentityHashMap<>();
        EnumSet<SlotCat> placed = EnumSet.noneOf(SlotCat.class);
        int h = 0, i = 0;

        for (ItemStack item : all) {
            if (h < hotbarSlots.size()) {
                SlotCat c = cat(item);
                if (c != null && !placed.contains(c)) {
                    placed.add(c);
                    targetMap.put(item, hotbarSlots.get(h++));
                    continue;
                }
            }
            if (i < invSlots.size()) {
                targetMap.put(item, invSlots.get(i++));
            } else if (h < hotbarSlots.size()) {
                targetMap.put(item, hotbarSlots.get(h++));
            }
        }

        List<Runnable> moves = new ArrayList<>();
        for (Map.Entry<ItemStack, Integer> entry : targetMap.entrySet()) {
            ItemStack want = entry.getKey();
            int target = entry.getValue();
            int current = itemToSlot.get(want);
            if (current == target) continue;

            int fc = current, ft = target;
            moves.add(() -> InventoryTask.moveItem(fc, ft, false));

            ItemStack displaced = slotToItem.get(target);
            itemToSlot.put(want, target);
            slotToItem.put(target, want);
            if (displaced != null && !displaced.isEmpty()) {
                itemToSlot.put(displaced, current);
                slotToItem.put(current, displaced);
            }
        }

        if (!moves.isEmpty()) {
            moves.forEach(Runnable::run);
            InventoryTask.updateSlots();
        }
    }

    private static SlotCat cat(ItemStack stack) {
        Item i = stack.getItem();
        if (i instanceof SwordItem || i instanceof AxeItem || i instanceof TridentItem) return SlotCat.MELEE;
        if (i instanceof BowItem || i instanceof CrossbowItem) return SlotCat.RANGED;
        if (i instanceof MiningToolItem && !(i instanceof AxeItem)) return SlotCat.TOOL;
        if (stack.get(DataComponentTypes.FOOD) != null) return SlotCat.FOOD;
        if (stack.get(DataComponentTypes.POTION_CONTENTS) != null) return SlotCat.POTION;
        if (i == Items.ENDER_PEARL) return SlotCat.PEARL;
        if (i == Items.WATER_BUCKET || i == Items.LAVA_BUCKET || i == Items.MILK_BUCKET) return SlotCat.BUCKET;
        if (i == Items.SHIELD) return SlotCat.SHIELD;
        if (i == Items.TOTEM_OF_UNDYING) return SlotCat.TOTEM;
        return null;
    }

    private static int prio(SlotCat c) {
        if (c == null) return 0;
        return switch (c) {
            case MELEE -> 9;
            case TOOL -> 8;
            case FOOD -> 7;
            case POTION -> 6;
            case RANGED -> 5;
            case TOTEM -> 4;
            case PEARL -> 3;
            case BUCKET -> 2;
            case SHIELD -> 1;
        };
    }

    private static double score(ItemStack stack) {
        if (isWeapon(stack)) return 1000 + weaponScore(stack);
        if (isTool(stack)) return 900 + toolScore(stack);
        if (isFood(stack)) return 800 + foodScore(stack);
        if (isPotion(stack)) return 700 + rarityScore(stack);
        if (isProjectile(stack)) return 600 + rarityScore(stack);
        if (isBlock(stack)) return 500 + rarityScore(stack);
        if (isArmor(stack)) return 400 + armorScore(stack);
        return rarityScore(stack);
    }

    private static boolean isPotion(ItemStack stack) {
        return stack.get(DataComponentTypes.POTION_CONTENTS) != null;
    }

    private static boolean isWeapon(ItemStack stack) {
        Item i = stack.getItem();
        return i instanceof SwordItem || i instanceof AxeItem ||
               i instanceof TridentItem || i instanceof BowItem ||
               i instanceof CrossbowItem;
    }

    private static boolean isFood(ItemStack stack) {
        return stack.get(DataComponentTypes.FOOD) != null;
    }

    private static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof MiningToolItem && !(stack.getItem() instanceof AxeItem);
    }

    private static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    private static boolean isBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    private static boolean isProjectile(ItemStack stack) {
        Item i = stack.getItem();
        return i instanceof ArrowItem || i == Items.ENDER_PEARL ||
               i == Items.SNOWBALL || i == Items.EGG ||
               i == Items.FIREWORK_ROCKET || i == Items.EXPERIENCE_BOTTLE;
    }

    private static double weaponScore(ItemStack stack) {
        double d = attackDamage(stack);
        Registry<Enchantment> r = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        d += EnchantmentHelper.getLevel(r.getEntry(Enchantments.SHARPNESS.getValue()).orElseThrow(), stack) * 1.25;
        d += EnchantmentHelper.getLevel(r.getEntry(Enchantments.SMITE.getValue()).orElseThrow(), stack) * 0.5;
        d += EnchantmentHelper.getLevel(r.getEntry(Enchantments.BANE_OF_ARTHROPODS.getValue()).orElseThrow(), stack) * 0.5;
        if (EnchantmentHelper.getLevel(r.getEntry(Enchantments.FIRE_ASPECT.getValue()).orElseThrow(), stack) > 0) d += 2.0;

        if (stack.getItem() instanceof SwordItem) d += 1.0;
        return d;
    }

    private static double attackDamage(ItemStack stack) {
        AttributeModifiersComponent m = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (m == null) return 1.0;
        double d = 1.0;
        for (AttributeModifiersComponent.Entry e : m.modifiers()) {
            if (e.attribute().equals(EntityAttributes.ATTACK_DAMAGE) &&
                e.modifier().operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                d += e.modifier().value();
            }
        }
        return d;
    }

    private static double foodScore(ItemStack stack) {
        FoodComponent f = stack.get(DataComponentTypes.FOOD);
        return f == null ? 0 : f.saturation() * 2.0 + f.nutrition();
    }

    private static double toolScore(ItemStack stack) {
        return rarityScore(stack) + enchScore(stack);
    }

    private static double armorScore(ItemStack stack) {
        Registry<Enchantment> r = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        double s = rarityScore(stack) * 5;
        s += EnchantmentHelper.getLevel(r.getEntry(Enchantments.PROTECTION.getValue()).orElseThrow(), stack) * 2.0;
        s += EnchantmentHelper.getLevel(r.getEntry(Enchantments.UNBREAKING.getValue()).orElseThrow(), stack) * 0.5;
        s += EnchantmentHelper.getLevel(r.getEntry(Enchantments.MENDING.getValue()).orElseThrow(), stack) * 1.0;
        return s;
    }

    private static double rarityScore(ItemStack stack) {
        Rarity r = stack.get(DataComponentTypes.RARITY);
        if (r == null) return 1;
        return switch (r) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
        };
    }

    private static double enchScore(ItemStack stack) {
        Registry<Enchantment> r = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        double s = 0;
        s += EnchantmentHelper.getLevel(r.getEntry(Enchantments.UNBREAKING.getValue()).orElseThrow(), stack) * 0.5;
        s += EnchantmentHelper.getLevel(r.getEntry(Enchantments.MENDING.getValue()).orElseThrow(), stack) * 1.0;
        s += EnchantmentHelper.getLevel(r.getEntry(Enchantments.EFFICIENCY.getValue()).orElseThrow(), stack) * 2.0;
        return s;
    }
}
