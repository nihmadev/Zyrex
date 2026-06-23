package lol.zyrex.utils.features.aura.warp;

import lol.zyrex.features.impl.combat.Aura;
import lol.zyrex.features.impl.movement.Strafe;
import lol.zyrex.features.impl.movement.TargetStrafe;
import lol.zyrex.features.impl.player.AutoPilot;
import lol.zyrex.utils.features.aura.utils.MathAngle;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import lol.zyrex.utils.display.interfaces.QuickImports;
import lol.zyrex.utils.features.aura.rotations.constructor.RotateConstructor;

@Setter
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TurnsConstructor implements QuickImports {
    Turns angle;
    Vec3d vec3d;
    Entity entity;
    RotateConstructor angleSmooth;
    int ticksUntilReset;
    float resetThreshold;
    public boolean moveCorrection;
    @Getter(AccessLevel.PUBLIC)
    public boolean freeCorrection;
    public boolean changeLook = (AutoPilot.getInstance().isState() && AutoPilot.getInstance().target !=null) ? true : (Aura.getInstance().isState() && Aura.getInstance().getTarget() !=null && Aura.getInstance().getCorrectionType().isSelected("Change Look")) ? true : false;


    public Turns nextRotation(Turns fromAngle, boolean isResetting) {
        if (isResetting) {
            return angleSmooth.limitAngleChange(fromAngle, MathAngle.fromVec2f(mc.player.getRotationClient()));
        }
        return angleSmooth.limitAngleChange(fromAngle, angle, vec3d, entity);
    }
}