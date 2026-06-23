package lol.zyrex.features.impl.render;

import lol.zyrex.events.render.WorldRenderEvent;
import lol.zyrex.features.module.Module;
import lol.zyrex.features.module.ModuleCategory;
import lol.zyrex.features.module.setting.implement.MultiSelectSetting;
import lol.zyrex.features.module.setting.implement.SliderSettings;
import lol.zyrex.utils.client.managers.event.EventHandler;
import lol.zyrex.utils.display.geometry.Render3D;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class XRay extends Module {

    private final Set<BlockPos> orePositions = ConcurrentHashMap.newKeySet();
    private final Set<Block> targetBlocks = new HashSet<>();
    private long lastScanTime = 0;

    private final MultiSelectSetting blockTypeSetting = new MultiSelectSetting("Блоки", "Выбор блоков для XRay")
            .value("Diamond", "Emerald", "Iron", "Gold", "Coal", "Redstone",
                    "Lapis", "Copper", "Ancient Debris", "Netherite", "Quartz", "Amethyst");

    private final SliderSettings radiusFinder = new SliderSettings("Дистанция поиска", "Диапазон поиска")
            .setValue(16f).range(8F, 64F);

    private final SliderSettings scanDelay = new SliderSettings("Задержка сканирования", "Задержка в секундах")
            .setValue(5f).range(1F, 30F);

    public XRay() {
        super("XRay", ModuleCategory.RENDER);
        setup(blockTypeSetting, radiusFinder, scanDelay);
    }

    @Override
    public void activate() {
        updateTargetBlocks();
        scanWorld();
        mc.worldRenderer.reload();
    }

    @Override
    public void deactivate() {
        orePositions.clear();
        mc.worldRenderer.reload();
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent e) {
        if (mc.world == null || mc.player == null) return;

        updateTargetBlocks();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScanTime > scanDelay.getValue() * 1000L) {
            scanWorld();
            lastScanTime = currentTime;
        }

        double maxDistance = radiusFinder.getValue() * radiusFinder.getValue();
        BlockPos playerPos = mc.player.getBlockPos();
        int rendered = 0;

        for (BlockPos pos : orePositions) {
            if (playerPos.getSquaredDistance(pos) > maxDistance) continue;

            BlockState state = mc.world.getBlockState(pos);
            if (targetBlocks.contains(state.getBlock())) {
                int color = getColorByBlock(state.getBlock());
                if (color != -1) {
                    Render3D.drawBox(new Box(pos), color, 1.0f, true, false, false);
                    rendered++;
                }
            }
        }
    }

    private void updateTargetBlocks() {
        targetBlocks.clear();

        if (blockTypeSetting.isSelected("Diamond")) {
            Collections.addAll(targetBlocks, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        }
        if (blockTypeSetting.isSelected("Emerald")) {
            Collections.addAll(targetBlocks, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        }
        if (blockTypeSetting.isSelected("Iron")) {
            Collections.addAll(targetBlocks, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        }
        if (blockTypeSetting.isSelected("Gold")) {
            Collections.addAll(targetBlocks, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        }
        if (blockTypeSetting.isSelected("Coal")) {
            Collections.addAll(targetBlocks, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        }
        if (blockTypeSetting.isSelected("Redstone")) {
            Collections.addAll(targetBlocks, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        }
        if (blockTypeSetting.isSelected("Lapis")) {
            Collections.addAll(targetBlocks, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        }
        if (blockTypeSetting.isSelected("Copper")) {
            Collections.addAll(targetBlocks, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        }
        if (blockTypeSetting.isSelected("Ancient Debris")) {
            targetBlocks.add(Blocks.ANCIENT_DEBRIS);
        }
        if (blockTypeSetting.isSelected("Netherite")) {
            targetBlocks.add(Blocks.ANCIENT_DEBRIS);
        }
        if (blockTypeSetting.isSelected("Quartz")) {
            targetBlocks.add(Blocks.NETHER_QUARTZ_ORE);
        }
        if (blockTypeSetting.isSelected("Amethyst")) {
            Collections.addAll(targetBlocks, Blocks.AMETHYST_CLUSTER, Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD);
        }
    }

    private void scanWorld() {
        orePositions.clear();
        if (mc.world == null) return;

        int radius = radiusFinder.getInt();
        BlockPos playerPos = mc.player.getBlockPos();
        int found = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (!mc.world.isInBuildLimit(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    if (targetBlocks.contains(state.getBlock())) {
                        orePositions.add(pos.toImmutable());
                        found++;
                    }
                }
            }
        }

    }

    private int getColorByBlock(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return 0xFF197B81;
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return 0xFF41871B;
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return 0xFF754C1F;
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) return 0xFFC5B938;
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return 0xFF131313;
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return 0xFF8B0000;
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return 0xFF0D3B8B;
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return 0xFFB4684D;
        if (block == Blocks.ANCIENT_DEBRIS) return 0xFFA67554;
        if (block == Blocks.NETHER_QUARTZ_ORE) return 0xFFE3D4C4;
        if (block == Blocks.AMETHYST_CLUSTER || block == Blocks.LARGE_AMETHYST_BUD || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD) return 0xFF9B59B6;
        return -1;
    }
}