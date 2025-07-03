package jsclub.codefest.sdk.statbot;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.armors.Armor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemUtils {

    /**
     * Lấy điểm cơ bản của vật phẩm chỉ dựa trên thông tin vật phẩm (không xét yếu tố khách quan).
     */
    public static int getBasePickupPointsSimple(String itemId) {
        // Ưu tiên lấy theo thứ tự: vũ khí, giáp, hồi máu, đặc biệt
        var weapon = jsclub.codefest.sdk.factory.WeaponFactory.getWeaponById(itemId);
        if (weapon != null) {
            int base = (int)(weapon.getDamage() * 1.5 + weapon.getRange() * 2);
            if (weapon.getEffects() != null) base += weapon.getEffects().size() * 10;
            return base;
        }
        var armor = jsclub.codefest.sdk.factory.ArmorFactory.getArmorById(itemId);
        if (armor != null) {
            int base = (int)(armor.getDamageReduce() * 2 + armor.getHealthPoint() * 1.5);
            return base;
        }
        var heal = jsclub.codefest.sdk.factory.HealingItemFactory.getHealingItemById(itemId);
        if (heal != null) {
            int base = heal.getHealingHP() * 2;
            if (heal.getEffects() != null) base += heal.getEffects().size() * 10;
            return base;
        }
        return 10;
    }

    public static boolean hasCurrentWeapon(Inventory inv, String newItemId) {
        if (inv.getMelee() != null && !inv.getMelee().getId().equals("HAND")) {
            return getBasePickupPointsSimple(inv.getMelee().getId()) >= getBasePickupPointsSimple(newItemId);
        }
        if (inv.getGun() != null) {
            return getBasePickupPointsSimple(inv.getGun().getId()) >= getBasePickupPointsSimple(newItemId);
        }
        if (inv.getSpecial() != null) {
            return getBasePickupPointsSimple(inv.getSpecial().getId()) >= getBasePickupPointsSimple(newItemId);
        }
        return false;
    }

    public static String getItemIdByPosition(GameMap map, Node itemPos) {
        // Kiểm tra weapons (gun, melee, throwable, special)
        for (Weapon weapon : map.getAllGun()) {
            if (weapon.x == itemPos.x && weapon.y == itemPos.y) {
                return weapon.getId();
            }
        }
        for (Weapon weapon : map.getAllMelee()) {
            if (weapon.x == itemPos.x && weapon.y == itemPos.y) {
                return weapon.getId();
            }
        }
        for (Weapon weapon : map.getAllThrowable()) {
            if (weapon.x == itemPos.x && weapon.y == itemPos.y) {
                return weapon.getId();
            }
        }
        for (Weapon weapon : map.getAllSpecial()) {
            if (weapon.x == itemPos.x && weapon.y == itemPos.y) {
                return weapon.getId();
            }
        }
        
        // Kiểm tra armors
        for (Armor armor : map.getListArmors()) {
            if (armor.x == itemPos.x && armor.y == itemPos.y) {
                return armor.getId();
            }
        }
        
        // Kiểm tra healing items
        for (HealingItem healingItem : map.getListHealingItems()) {
            if (healingItem.x == itemPos.x && healingItem.y == itemPos.y) {
                return healingItem.getId();
            }
        }
        
        // Kiểm tra chests và dragon eggs
        for (Obstacle chest : map.getListChests()) {
            if (chest.x == itemPos.x && chest.y == itemPos.y) {
                return chest.getId();
            }
        }
        
        return "UNKNOWN";
    }

    public static String getItemTypeByPosition(GameMap map, Node itemPos) {
        if (map.getAllGun().contains(itemPos) || map.getAllMelee().contains(itemPos) || map.getAllSpecial().contains(itemPos) || map.getAllThrowable().contains(itemPos)) return "weapon";
        if (map.getListArmors().contains(itemPos)) return "armor";
        if (map.getListHealingItems().contains(itemPos)) return "healing";
        if (isChest(map, itemPos) || isDragonEgg(map, itemPos)) return "chest";
        return "unknown";
    }

    public static boolean isChest(GameMap map, Node pos) {
        return map.getListChests().stream().anyMatch(
                chest -> chest.x == pos.x && chest.y == pos.y && "CHEST".equals(chest.getId())
        );
    }

    public static boolean isDragonEgg(GameMap map, Node pos) {
        return map.getListChests().stream().anyMatch(chest -> 
            chest.x == pos.x && chest.y == pos.y && "DRAGON_EGG".equals(chest.getId())
        );
    }

    public static boolean isBetterArmor(Inventory inv, String newId) {
        if (inv.getArmor() == null) return true;
        return getBasePickupPointsSimple(newId) > getBasePickupPointsSimple(inv.getArmor().getId());
    }

    public static boolean isBetterHelmet(Inventory inv, String newId) {
        if (inv.getHelmet() == null) return true;
        return getBasePickupPointsSimple(newId) > getBasePickupPointsSimple(inv.getHelmet().getId());
    }

    public static HealingItem getBestHealingItem(List<HealingItem> healingItems) {
        if (healingItems.isEmpty()) return null;
        for (HealingItem item : healingItems) {
            if ("ELIXIR".equals(item.getId()) || "MAGIC".equals(item.getId())) {
                return item;
            }
        }
        return healingItems.stream()
            .max((a, b) -> Integer.compare(getBasePickupPointsSimple(a.getId()), getBasePickupPointsSimple(b.getId())))
            .orElse(null);
    }

    public static String getGunDirectionToChest(Node myPos, Node chestPos, Weapon gun) {
        if (gun == null) return null;
        int range = gun.getRange();
        if (myPos.x == chestPos.x) {
            int dy = chestPos.y - myPos.y;
            if (dy > 0 && dy <= range) return "u";
            if (dy < 0 && -dy <= range) return "d";
        } else if (myPos.y == chestPos.y) {
            int dx = chestPos.x - myPos.x;
            if (dx > 0 && dx <= range) return "r";
            if (dx < 0 && -dx <= range) return "l";
        }
        return null;
    }

    public static String getMeleeDirectionToChest(Node myPos, Node chestPos, Weapon melee) {
        if (melee == null) return null;
        int width = melee.getRange();
        for (int i = -width/2; i <= width/2; i++) {
            if (myPos.x-1 == chestPos.x && myPos.y+i == chestPos.y) return "l";
            if (myPos.x+1 == chestPos.x && myPos.y+i == chestPos.y) return "r";
            if (myPos.x+i == chestPos.x && myPos.y-1 == chestPos.y) return "d";
            if (myPos.x+i == chestPos.x && myPos.y+1 == chestPos.y) return "u";
        }
        return null;
    }
} 