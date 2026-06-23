package lol.zyrex.features.module;

import lol.zyrex.features.impl.combat.*;
import lol.zyrex.features.impl.misc.*;
import lol.zyrex.features.impl.movement.*;
import lol.zyrex.features.impl.player.*;
import lol.zyrex.features.impl.render.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;


import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<Module> modules = new ArrayList<>();

    public void setup() {
        register(
                // Combat
                new Aura(),
                new Velocity(),
                new AutoTotem(),
                new TriggerBot(),
                new AutoCrystal(),
                new HitBoxModule(),
                new AntiBot(),
                new AutoSwap(),
                new TargetPearl(),
                new ProjectileHelper(),
                new ShiftTap(),
                new NoFriendDamage(),
                new NoInteract(),

                // Movement
                new Speed(),
                new Scaffold(),
                new Fly(),
                new AutoSprint(),
                new NoSlow(),
                new InventoryMove(),
                new NoFallDamage(),
                new Strafe(),
                new TargetStrafe(),
                new Jesus(),
                new ElytraMotion(),
                new ElytraTarget(),
                new LongJump(),
                new Spider(),
                new NoWeb(),
                new WaterSpeed(),
                new Blink(),
                new NoClip(),
                new AirStuck(),
                new SuperFireWork(),

                // Render
                new Hud(),
                new Esp(),
                new Chams(),
                new TargetESP(),
                new CrossHair(),
                new NoRender(),
                new ViewModel(),
                new ProjectilePrediction(),
                new BlockESP(),
                new XRay(),
                new BlockOverlay(),
                new FreeLook(),
                new CameraSettings(),
                new SwingAnimation(),
                new ChinaHat(),
                new BetterMinecraft(),
                new WorldTweaks(),
                new WorldParticles(),
                new NightVision(),
                new KillEffect(),
                new JumpCircle(),
                new AspectRatio(),
                new Arrows(),
                new SeeInvisible(),
                new NameProtect(),
                new NoFire(),
                new NoShaking(),
                new AuctionHelper(),
                new Atmosphere(),
//                new HitEffect(),

                // Player
                new FastBreak(),
                new AutoTool(),
                new AutoArmor(),
                new Nuker(),
                new NoPush(),
                new NoDelay(),
                new AutoRespawn(),
                new AutoFish(),
                new AntiAFK(),
                new AutoUse(),
                new AutoPilot(),
                new NoEntityTrace(),
                new AutoWaterDrop(),
                new AutoBoatDrop(),
                new ItemScroller(),
                new ItemFixSwap(),
                new EnderChestPlus(),

                // Misc
                new ChestStealer(),
                new FreeCam(),
                new SelfDestruct(),
                new AutoLeave(),
                new AutoTpAccept(),
                new ClickPearl(),
                new ClickFriend(),
                new AutoBuy(),
                new IRC(),
                new ServerHelper(),
                new TabParser(),
                new JoinerHelper(),
                new ElytraHelper(),
                new ServerRPSpoofer(),
                new WindJump()
        );
    }

    
    public void register(Module... module) {
        modules.addAll(List.of(module));
    }

    public List<Module> modules() {
        return modules;
    }
}
