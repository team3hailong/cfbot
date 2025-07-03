package jsclub.codefest.sdk.statbot;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.Inventory;
import java.util.List;

public class CombatUtils {

    public static int evaluateStrength(Player p, Inventory inv) {
        int score = 0;
        if (p == null) return 0;
        if (p.getHealth() != null) score += p.getHealth();
        if (inv != null) {
            if (inv.getGun() != null) score += ItemUtils.getBasePickupPointsSimple(inv.getGun().getId()) + 20;
            if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) score += ItemUtils.getBasePickupPointsSimple(inv.getMelee().getId()) + 10;
            if (inv.getArmor() != null) score += ItemUtils.getBasePickupPointsSimple(inv.getArmor().getId());
            if (inv.getHelmet() != null) score += ItemUtils.getBasePickupPointsSimple(inv.getHelmet().getId());
            // Thêm điểm cho special weapons
            if (inv.getSpecial() != null) {
                String specialId = inv.getSpecial().getId();
                if ("ROPE".equals(specialId)) score += 150; // Rất mạnh cho combat
                else if ("BELL".equals(specialId)) score += 120; // Mạnh cho crowd control
                else if ("SAHUR_BAT".equals(specialId)) score += 100; // Mạnh cho escape
                else score += ItemUtils.getBasePickupPointsSimple(specialId);
            }
            score += inv.getListHealingItem().size() * 5;
        } else {
            if (p.getHealth() != null && p.getHealth() > 80) score += 10;
        }
        return score;
    }
    
    /**
     * Đánh giá sức mạnh có xem xét effects
     * @param p Player
     * @param inv Inventory
     * @param effects Danh sách effects hiện tại
     * @return Điểm sức mạnh đã điều chỉnh theo effects
     */
    public static int evaluateStrengthWithEffects(Player p, Inventory inv, List<jsclub.codefest.sdk.model.effects.Effect> effects) {
        int baseScore = evaluateStrength(p, inv);
        
        if (effects == null || effects.isEmpty()) {
            return baseScore;
        }
        
        // Điều chỉnh điểm theo effects
        for (jsclub.codefest.sdk.model.effects.Effect effect : effects) {
            if (effect.id == null) continue;
            
            String effectId = effect.id.toUpperCase();
            
            // Effects có lợi - tăng điểm
            if (effectId.contains("INVISIBLE")) {
                baseScore += 50; // Tàng hình giúp tấn công bất ngờ
            } else if (effectId.contains("UNDEAD")) {
                baseScore += 100; // Bất tử rất mạnh
            } else if (effectId.contains("CONTROL_IMMUNITY")) {
                baseScore += 80; // Miễn khống chế rất hữu ích
            } else if (effectId.contains("REVIVAL")) {
                baseScore += 200; // Hồi sinh cực kỳ mạnh
            }
            
            // Effects có hại - giảm điểm
            else if (effectId.contains("STUN")) {
                baseScore -= 200; // Stun làm mất khả năng hành động
            } else if (effectId.contains("BLIND")) {
                baseScore -= 150; // Blind làm mất khả năng nhìn
            } else if (effectId.contains("REVERSE")) {
                baseScore -= 100; // Reverse làm khó di chuyển
            } else if (effectId.contains("POISON")) {
                baseScore -= 30; // Poison gây sát thương theo thời gian
            } else if (effectId.contains("BLEED")) {
                baseScore -= 50; // Bleed nguy hiểm hơn Poison
            }
        }
        
        return Math.max(0, baseScore); // Không để điểm âm
    }
} 