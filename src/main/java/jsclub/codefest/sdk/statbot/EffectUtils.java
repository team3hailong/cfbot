package jsclub.codefest.sdk.statbot;

import jsclub.codefest.sdk.model.effects.Effect;
import java.util.List;

public class EffectUtils {
    
    // Kiểm tra các effects khống chế (không thể hành động)
    public static boolean isStunned(List<Effect> effects) {
        return hasEffect(effects, "STUN");
    }
    
    public static boolean isBlinded(List<Effect> effects) {
        return hasEffect(effects, "BLIND");
    }
    
    public static boolean isReversed(List<Effect> effects) {
        return hasEffect(effects, "REVERSE");
    }
    
    // Kiểm tra effects gây sát thương theo thời gian
    public static boolean isPoisoned(List<Effect> effects) {
        return hasEffect(effects, "POISON");
    }
    
    public static boolean isBleeding(List<Effect> effects) {
        return hasEffect(effects, "BLEED");
    }
    
    // Kiểm tra effects có lợi
    public static boolean isInvisible(List<Effect> effects) {
        return hasEffect(effects, "INVISIBLE");
    }
    
    public static boolean isUndead(List<Effect> effects) {
        return hasEffect(effects, "UNDEAD");
    }
    
    public static boolean hasControlImmunity(List<Effect> effects) {
        return hasEffect(effects, "CONTROL_IMMUNITY");
    }
    
    public static boolean hasRevival(List<Effect> effects) {
        return hasEffect(effects, "REVIVAL");
    }
    
    // Kiểm tra effects tức thì
    public static boolean isPulled(List<Effect> effects) {
        return hasEffect(effects, "PULL");
    }
    
    public static boolean isKnockedBack(List<Effect> effects) {
        return hasEffect(effects, "KNOCKBACK");
    }
    
    // Phương thức tổng quát để kiểm tra effect
    public static boolean hasEffect(List<Effect> effects, String effectId) {
        if (effects == null) return false;
        for (Effect e : effects) {
            if (e.id != null && e.id.toUpperCase().contains(effectId.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
    
    // Kiểm tra xem có bị khống chế không (không thể hành động bình thường)
    public static boolean isControlled(List<Effect> effects) {
        return isStunned(effects) || isBlinded(effects) || isReversed(effects);
    }
    
    // Kiểm tra xem có bị sát thương theo thời gian không
    public static boolean isTakingDamageOverTime(List<Effect> effects) {
        return isPoisoned(effects) || isBleeding(effects);
    }
    
    // Kiểm tra xem có effects có lợi không
    public static boolean hasBeneficialEffects(List<Effect> effects) {
        return isInvisible(effects) || isUndead(effects) || hasControlImmunity(effects) || hasRevival(effects);
    }
    
    // Kiểm tra xem có cần ưu tiên hồi máu không (do effects gây sát thương)
    public static boolean needsHealingPriority(List<Effect> effects) {
        return isBleeding(effects); // Bleed giảm 50% khả năng hồi HP
    }
    
    // Kiểm tra xem có nên tránh combat không
    public static boolean shouldAvoidCombat(List<Effect> effects) {
        return isControlled(effects) || isTakingDamageOverTime(effects);
    }
    
    // Kiểm tra xem có thể tấn công an toàn không
    public static boolean canAttackSafely(List<Effect> effects) {
        return !isControlled(effects) && !isBlinded(effects);
    }
    
    // Kiểm tra xem có thể di chuyển bình thường không
    public static boolean canMoveNormally(List<Effect> effects) {
        return !isStunned(effects) && !isBlinded(effects);
    }
    
    // Lấy mức độ ưu tiên giải effects (số càng cao càng ưu tiên)
    public static int getEffectPriority(List<Effect> effects) {
        int priority = 0;
        
        // Ưu tiên cao nhất: Stun (không thể làm gì)
        if (isStunned(effects)) priority += 100;
        
        // Ưu tiên cao: Blind (không nhìn thấy gì)
        if (isBlinded(effects)) priority += 80;
        
        // Ưu tiên trung bình: Reverse (di chuyển ngược)
        if (isReversed(effects)) priority += 60;
        
        // Ưu tiên thấp: Poison/Bleed (sát thương theo thời gian)
        if (isPoisoned(effects)) priority += 30;
        if (isBleeding(effects)) priority += 40; // Bleed nguy hiểm hơn Poison
        
        return priority;
    }
    
    // Kiểm tra xem có cần dùng ELIXIR không
    public static boolean needsElixir(List<Effect> effects) {
        return isControlled(effects) && !hasControlImmunity(effects);
    }
    
    // Kiểm tra xem có cần dùng MAGIC (tàng hình) không
    public static boolean needsInvisibility(List<Effect> effects) {
        // Tàng hình khi bị đuổi hoặc máu thấp
        return isTakingDamageOverTime(effects) || isControlled(effects);
    }
    
    // Kiểm tra xem có cần dùng COMPASS (stun area) không
    public static boolean needsAreaStun(List<Effect> effects) {
        // Dùng khi bị bao vây hoặc cần thoát khỏi tình huống nguy hiểm
        return isControlled(effects) || isTakingDamageOverTime(effects);
    }
    
    // Lấy danh sách effects hiện tại dưới dạng string để debug
    public static String getEffectsDescription(List<Effect> effects) {
        if (effects == null || effects.isEmpty()) return "Không có effects";
        
        StringBuilder sb = new StringBuilder();
        for (Effect e : effects) {
            if (e.id != null) {
                sb.append(e.id);
                if (e.duration != null) {
                    sb.append("(").append(e.duration).append("s)");
                }
                sb.append(", ");
            }
        }
        
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2); // Xóa ", " cuối
        }
        
        return sb.toString();
    }
} 