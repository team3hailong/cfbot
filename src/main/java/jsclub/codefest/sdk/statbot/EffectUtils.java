package jsclub.codefest.sdk.statbot;

import jsclub.codefest.sdk.model.effects.Effect;
import java.util.List;

public class EffectUtils {
    public static boolean isStunned(List<Effect> effects) {
        for (Effect e : effects) {
            if (e.id != null && e.id.toUpperCase().contains("STUN")) return true;
        }
        return false;
    }
} 