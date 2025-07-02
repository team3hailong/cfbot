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

public class StatBotUtils {
    public static int dist(Node a, Node b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }
    public static Node moveTo(Node a, String dir) {
        switch (dir) {
            case "u": return new Node(a.x-1, a.y);
            case "d": return new Node(a.x+1, a.y);
            case "l": return new Node(a.x, a.y-1);
            case "r": return new Node(a.x, a.y+1);
        }
        return a;
    }

    public static int getItemPriority(String itemType, String itemId, Inventory currentInv, GameMap map, Node myPos, Node itemPos, boolean isDanger, boolean isChest, double chestGoodRate, float myHP) {
        int basePoints = getBasePickupPoints(itemId, itemType, currentInv, map, myPos, itemPos, isDanger, isChest, chestGoodRate, myHP);
        return basePoints;
    }

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

    /**
     * Tính điểm vật phẩm dựa trên thông tin chi tiết và các yếu tố khách quan.
     * @param itemId id vật phẩm
     * @param itemType loại vật phẩm (weapon, armor, healing, special, ...)
     * @param inv trạng thái inventory hiện tại
     * @param map bản đồ hiện tại
     * @param myPos vị trí người chơi
     * @param itemPos vị trí vật phẩm
     * @param isDanger có kẻ địch gần không
     * @param isChest có phải rương không
     * @param chestGoodRate tỉ lệ ra đồ xịn nếu là rương (0-1)
     * @param myHP máu hiện tại
     * @return điểm số vật phẩm
     */
    public static int getBasePickupPoints(String itemId, String itemType, Inventory inv, GameMap map, Node myPos, Node itemPos, boolean isDanger, boolean isChest, double chestGoodRate, float myHP) {
        int base = 0;
        // 1. Điểm cơ bản theo loại vật phẩm
        try {
            if ("weapon".equals(itemType)) {
                var weapon = jsclub.codefest.sdk.factory.WeaponFactory.getWeaponById(itemId);
                if (weapon != null) {
                    base += weapon.getDamage() * 1.5 + weapon.getRange() * 2;
                    if (weapon.getEffects() != null) base += weapon.getEffects().size() * 10;
                }
            } else if ("armor".equals(itemType) || "helmet".equals(itemType)) {
                var armor = jsclub.codefest.sdk.factory.ArmorFactory.getArmorById(itemId);
                if (armor != null) {
                    base += armor.getDamageReduce() * 2 + armor.getHealthPoint() * 1.5;
                }
            } else if ("healing".equals(itemType)) {
                var heal = jsclub.codefest.sdk.factory.HealingItemFactory.getHealingItemById(itemId);
                if (heal != null) {
                    base += heal.getHealingHP() * 2;
                    if (heal.getEffects() != null) base += heal.getEffects().size() * 10;
                }
            } else if ("special".equals(itemType)) {
                var weapon = jsclub.codefest.sdk.factory.WeaponFactory.getWeaponById(itemId);
                if (weapon != null) {
                    base += weapon.getDamage() * 1.5 + weapon.getRange() * 2;
                    if (weapon.getEffects() != null) base += weapon.getEffects().size() * 10;
                }
            } else {
                base += 10; // fallback cho loại không xác định
            }
        } catch (Exception e) {
            base += 10;
        }
        // 2. Khoảng cách
        int distance = dist(myPos, itemPos);
        base -= distance * 2;
        
        // Nếu vật phẩm ngay gần (distance == 1) và chưa có vật phẩm cùng loại thì cộng 150 điểm
        if (distance == 1) {
            boolean shouldBonus = false;
            if ("weapon".equals(itemType)) {
                // Kiểm tra nếu là gun, melee, special dựa vào id
                var weapon = jsclub.codefest.sdk.factory.WeaponFactory.getWeaponById(itemId);
                if (weapon != null) {
                    var weaponType = weapon.getType();
                    if (weaponType == jsclub.codefest.sdk.model.ElementType.GUN && (inv.getGun() == null || "HAND".equals(inv.getGun().getId()))) shouldBonus = true;
                    else if (weaponType == jsclub.codefest.sdk.model.ElementType.MELEE && (inv.getMelee() == null || "HAND".equals(inv.getMelee().getId()))) shouldBonus = true;
                    else if (weaponType == jsclub.codefest.sdk.model.ElementType.SPECIAL && inv.getSpecial() == null) shouldBonus = true;
                }
            } else if ("armor".equals(itemType)) {
                // Kiểm tra riêng armor và helmet
                if (isBetterArmor(inv, itemId) && (inv.getArmor() == null)) shouldBonus = true;
                if (isBetterHelmet(inv, itemId) && (inv.getHelmet() == null)) shouldBonus = true;
            } else if ("healing".equals(itemType)) {
                if (inv.getListHealingItem() == null || inv.getListHealingItem().isEmpty()) shouldBonus = true;
            } else if ("special".equals(itemType)) {
                if (inv.getSpecial() == null) shouldBonus = true;
            }
            if (shouldBonus){
                System.out.println("shouldBonus: itemId=" + itemId + ", itemType=" + itemType + ", shouldBonus=" + shouldBonus);
                base += 200;
            } 
        }
        // 3. Rương: cộng điểm theo tỉ lệ ra đồ xịn
        if (isChest) {
            base += chestGoodRate * 300;
        }
        // 4. Tình trạng hiện tại
        if ("healing".equals(itemType)) {
            if (inv.getListHealingItem().size() < 2) base += 50;
            if (myHP < 50) base += 40;
            if (myHP < 30) base += 60;
        }
        if ("weapon".equals(itemType) && hasCurrentWeapon(inv, itemId)) {
            base -= 50;
        }
        // 5. Nguy hiểm: ưu tiên vật phẩm sinh tồn
        if (isDanger && ("healing".equals(itemType) || "special".equals(itemType))) {
            base += 50;
        }
        // 6. Số lượng: đã đủ vật phẩm này chưa
        if ("healing".equals(itemType) && inv.getListHealingItem().size() >= 4) {
            base -= 30;
        }
        // 7. Kiểm tra Player khác gần vật phẩm (có thể cướp)
        if (map != null) {
            for (Player otherPlayer : map.getOtherPlayerInfo()) {
                if (otherPlayer.getHealth() != null && otherPlayer.getHealth() >= myHP) {
                    int playerDistance = dist(itemPos, new Node(otherPlayer.x, otherPlayer.y));
                    if (playerDistance <= 3) {
                        // Giảm điểm nếu có Player khác gần vật phẩm
                        base -= (4 - playerDistance) * 30;
                    }
                }
            }
        }
        // 8. Ưu tiên đặc biệt cho một số itemId
        if ("ELIXIR_OF_LIFE".equals(itemId)) base += 1000;
        if ("COMPASS".equals(itemId)) base += 500;
        if ("MAGIC_ARMOR".equals(itemId)) base += 300;
        if ("MAGIC_HELMET".equals(itemId)) base += 200;
        // Ưu tiên DRAGON_EGG cao nhất
        if ("DRAGON_EGG".equals(itemId)) base += 1500;
        // Ưu tiên special weapons cao hơn
        if ("ROPE".equals(itemId)) base += 400; // Rất hữu ích cho combat
        if ("BELL".equals(itemId)) base += 350; // Hữu ích cho crowd control
        if ("SAHUR_BAT".equals(itemId)) base += 300; // Hữu ích cho escape
        if ("SHOTGUN".equals(itemId) || "MACE".equals(itemId)) base += 150;
        if ("AXE".equals(itemId) || "KNIFE".equals(itemId)) base += 100;
        return base;
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

    public static Node findBestItemToPickup(GameMap map, Inventory inv, Node myPos, Set<Node> excludeItems) {
        List<Node> allItems = new ArrayList<>();
        allItems.addAll(map.getAllGun());
        allItems.addAll(map.getAllMelee());
        allItems.addAll(map.getAllThrowable());
        allItems.addAll(map.getAllSpecial());
        allItems.addAll(map.getListArmors());
        allItems.addAll(map.getListHealingItems());
        allItems.addAll(map.getListChests());
        if (allItems.isEmpty()) return null;
        Player me = map.getCurrentPlayer();
        float myHP = (me != null && me.getHealth() != null) ?  me.getHealth() : 100;
        boolean isDanger = false;
        
        // Kiểm tra Enemy gần (chỉ để tránh, không tấn công)
        for (Enemy e : map.getListEnemies()) {
            if (dist(myPos, new Node(e.x, e.y)) <= 1) {
                isDanger = true;
                break;
            }
        }
        
        // Kiểm tra Player khác gần (có thể tấn công)
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                if (dist(myPos, new Node(otherPlayer.x, otherPlayer.y)) <= 2) {
                    isDanger = true;
                    break;
                }
            }
        }
        
        Node best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Node item : allItems) {
            if (excludeItems != null && excludeItems.contains(item)) continue;
            String itemId = getItemIdByPosition(map, item);
            String itemType = getItemTypeByPosition(map, item);
            boolean isChest = isChest(map, item);
            double chestGoodRate = isChest ? 0.8 : 0.0; // Giả định tỉ lệ ra đồ xịn với rương là 80%
            
            // Kiểm tra xem có Player khác gần vật phẩm này không
            boolean itemNearOtherPlayer = false;
            for (Player otherPlayer : map.getOtherPlayerInfo()) {
                if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                    if (dist(item, new Node(otherPlayer.x, otherPlayer.y)) <= 3) {
                        itemNearOtherPlayer = true;
                        break;
                    }
                }
            }
            
            double score = getBasePickupPoints(itemId, itemType, inv, map, myPos, item, isDanger, isChest, chestGoodRate, myHP);
            
            // Giảm điểm nếu vật phẩm gần Player khác (có thể bị cướp)
            if (itemNearOtherPlayer) {
                score -= 100;
            }
            
            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
            System.out.println("DEBUG FROM SDK findBestItemToPickup : " + itemId + " " + itemType + " " + score + " (nearOtherPlayer: " + itemNearOtherPlayer + ")");
        }
        System.out.println("Best score: " + bestScore);
        return best;
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
    public static boolean isInventoryOptimal(Inventory inv) {
        boolean armor = inv.getArmor() != null && inv.getArmor().getId().equals("MAGIC_ARMOR");
        boolean helmet = inv.getHelmet() != null && inv.getHelmet().getId().equals("MAGIC_HELMET");
        boolean gun = inv.getGun() != null && inv.getGun().getId().equals("SHOTGUN");
        boolean melee = inv.getMelee() != null && inv.getMelee().getId().equals("MACE");
        boolean special = inv.getSpecial() != null && (inv.getSpecial().getId().equals("ROPE") || 
                                                       inv.getSpecial().getId().equals("BELL") || 
                                                       inv.getSpecial().getId().equals("SAHUR_BAT"));
        boolean healing = inv.getListHealingItem().stream().anyMatch(h -> h.getId().equals("ELIXIR_OF_LIFE"));
        return armor && helmet && gun && melee && special && healing;
    }
    public static double expectedChestValue(Inventory inv) {
        if (!isInventoryOptimal(inv)) return 120;
        return 30;
    }
    public static void manageInventoryBeforePickup(GameMap map, Inventory inv, Node itemPos, jsclub.codefest.sdk.Hero hero) {
        try {
            for (Obstacle chest : map.getListChests()) {
                if (Math.abs(chest.x - itemPos.x) <= 1 && Math.abs(chest.y - itemPos.y) <= 1) {
                    return;
                }
            }
            if (map.getAllGun().contains(itemPos) && inv.getGun() != null) {
                hero.revokeItem(inv.getGun().getId());
            } else if (map.getAllMelee().contains(itemPos) && inv.getMelee() != null && 
                       !inv.getMelee().getId().equals("HAND")) {
                hero.revokeItem(inv.getMelee().getId());
            } else if (map.getAllSpecial().contains(itemPos) && inv.getSpecial() != null) {
                // Special weapons cũng chỉ được giữ tối đa 1 vật phẩm, cần revoke trước khi nhặt mới
                hero.revokeItem(inv.getSpecial().getId());
            } else if (map.getListArmors().contains(itemPos) && (inv.getArmor() != null || inv.getHelmet() != null)) {
                if (inv.getArmor() != null) {
                    hero.revokeItem(inv.getArmor().getId());
                }
                if (inv.getHelmet() != null) {
                    hero.revokeItem(inv.getHelmet().getId());
                }
            } else if (map.getListHealingItems().contains(itemPos) && inv.getListHealingItem().size() >= 4) {
                HealingItem worstHeal = inv.getListHealingItem().stream()
                    .min((a, b) -> Integer.compare(getBasePickupPointsSimple(a.getId()), getBasePickupPointsSimple(b.getId())))
                    .orElse(null);
                if (worstHeal != null) {
                    hero.useItem(worstHeal.getId());
                }
            }
        } catch (Exception e) {
        }
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
    /**
     * Tìm hướng di chuyển tối ưu dựa trên nhiều yếu tố
     * @param myPos vị trí hiện tại
     * @param map bản đồ game
     * @param inv inventory (có thể null)
     * @param targetEnemy enemy cần tránh (không tấn công, có thể null)
     * @param prioritizeItems có ưu tiên tìm vật phẩm không
     * @return hướng di chuyển tối ưu
     */
    public static String findOptimalDirection(Node myPos, GameMap map, Inventory inv, Node targetEnemy, boolean prioritizeItems) {
        String[] dirs = {"u", "d", "l", "r"};
        List<String> safeDirs = new ArrayList<>();
        List<String> itemDirs = new ArrayList<>();
        List<String> escapeDirs = new ArrayList<>();
        
        for (String dir : dirs) {
            Node nextPos = moveTo(myPos, dir);
            boolean isSafe = true;
            boolean hasItems = false;
            int escapeScore = 0;
            
            // Kiểm tra Enemy (chỉ để tránh, không tấn công)
            for (Enemy enemy : map.getListEnemies()) {
                if (dist(nextPos, new Node(enemy.x, enemy.y)) <= 1) {
                    isSafe = false;
                    break;
                }
            }
            
            // Kiểm tra Player khác (có thể tấn công)
            for (Player otherPlayer : map.getOtherPlayerInfo()) {
                if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                    if (dist(nextPos, new Node(otherPlayer.x, otherPlayer.y)) <= 2) {
                        isSafe = false;
                        break;
                    }
                }
            }
            
            // Kiểm tra bẫy stun
            boolean hasStunTrap = false;
            for (var trap : map.getListTraps()) {
                if (trap.getX() == nextPos.x && trap.getY() == nextPos.y && trap.getTag() != null && 
                    trap.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                    hasStunTrap = true;
                    break;
                }
            }
            
            if (!isSafe || hasStunTrap) continue;
            
            // Tính điểm trốn thoát nếu có enemy target
            if (targetEnemy != null) {
                int d = dist(nextPos, targetEnemy);
                escapeScore = d;
            }
            
            // Kiểm tra vật phẩm trong tầm nhìn (nếu prioritizeItems = true)
            if (prioritizeItems) {
                for (int step = 1; step <= 3; step++) {
                    Node checkPos = nextPos;
                    for (int i = 0; i < step; i++) {
                        checkPos = moveTo(checkPos, dir);
                    }
                    if (map.getAllGun().contains(checkPos) || 
                        map.getAllMelee().contains(checkPos) || 
                        map.getAllThrowable().contains(checkPos) || 
                        map.getAllSpecial().contains(checkPos) || 
                        map.getListArmors().contains(checkPos) || 
                        map.getListHealingItems().contains(checkPos)) {
                        hasItems = true;
                        break;
                    }
                }
            }
            
            safeDirs.add(dir);
            if (hasItems) {
                itemDirs.add(dir);
            }
            if (targetEnemy != null) {
                escapeDirs.add(dir);
            }
        }
        
        // Logic ưu tiên:
        // 1. Nếu có enemy target và cần trốn thoát, ưu tiên hướng xa enemy nhất
        if (targetEnemy != null && !escapeDirs.isEmpty()) {
            String bestEscapeDir = "u";
            int maxDist = -1;
            for (String dir : escapeDirs) {
                Node nextPos = moveTo(myPos, dir);
                int d = dist(nextPos, targetEnemy);
                if (d > maxDist) {
                    maxDist = d;
                    bestEscapeDir = dir;
                }
            }
            return bestEscapeDir;
        }
        
        // 2. Nếu ưu tiên vật phẩm và có hướng có vật phẩm
        if (prioritizeItems && !itemDirs.isEmpty()) {
            return itemDirs.get((int)(Math.random() * itemDirs.size()));
        }
        
        // 3. Chọn hướng an toàn ngẫu nhiên
        if (!safeDirs.isEmpty()) {
            return safeDirs.get((int)(Math.random() * safeDirs.size()));
        }
        
        // 4. Fallback: chọn ngẫu nhiên
        return dirs[(int)(Math.random() * 4)];
    }

    // Giữ lại các hàm cũ để tương thích ngược
    public static String findSmartDirection(Node myPos, GameMap map, Inventory inv) {
        return findOptimalDirection(myPos, map, inv, null, true);
    }
    
    public static String safeDirection(Node me, Node enemy, GameMap map) {
        return findOptimalDirection(me, map, null, enemy, false);
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
    public static int evaluateStrength(Player p, Inventory inv) {
        int score = 0;
        if (p == null) return 0;
        if (p.getHealth() != null) score += p.getHealth();
        if (inv != null) {
            if (inv.getGun() != null) score += getBasePickupPointsSimple(inv.getGun().getId()) + 20;
            if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) score += getBasePickupPointsSimple(inv.getMelee().getId()) + 10;
            if (inv.getArmor() != null) score += getBasePickupPointsSimple(inv.getArmor().getId());
            if (inv.getHelmet() != null) score += getBasePickupPointsSimple(inv.getHelmet().getId());
            // Thêm điểm cho special weapons
            if (inv.getSpecial() != null) {
                String specialId = inv.getSpecial().getId();
                if ("ROPE".equals(specialId)) score += 150; // Rất mạnh cho combat
                else if ("BELL".equals(specialId)) score += 120; // Mạnh cho crowd control
                else if ("SAHUR_BAT".equals(specialId)) score += 100; // Mạnh cho escape
                else score += getBasePickupPointsSimple(specialId);
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
    public static String directionTo(Node a, Node b) {
        if (a.x < b.x) return "r";
        if (a.x > b.x) return "l";
        if (a.y < b.y) return "u";
        if (a.y > b.y) return "d";
        return "";
    }

    /**
     * Kiểm tra xem có nên sử dụng special weapon không và trả về hướng sử dụng
     * @param map bản đồ game
     * @param inv inventory hiện tại
     * @param myPos vị trí hiện tại
     * @return hướng sử dụng special weapon hoặc null nếu không nên dùng
     */
    public static String shouldUseSpecialWeapon(GameMap map, Inventory inv, Node myPos) {
        if (inv.getSpecial() == null) return null;
        
        String specialId = inv.getSpecial().getId();
        
        switch (specialId) {
            case "ROPE":
                return shouldUseRope(map, inv, myPos);
            case "BELL":
                return shouldUseBell(map, inv, myPos);
            case "SAHUR_BAT":
                return shouldUseSahurBat(map, inv, myPos);
            default:
                return null;
        }
    }

    /**
     * Logic sử dụng ROPE (Dây thừng)
     * - Kéo mình về phía obstacle có thể phá hủy
     * - Kéo mình về phía NPC để tấn công
     */
    private static String shouldUseRope(GameMap map, Inventory inv, Node myPos) {
        // 1. Kiểm tra player khác ở xa để kéo về
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                int distance = dist(myPos, playerPos);
                if (distance >= 3 && distance <= 6) {
                    String direction = directionTo(myPos, playerPos);
                    if (direction != null && !direction.isEmpty()) {
                        return direction;
                    }
                }
            }
        }
        
        // 2. Kiểm tra obstacle có thể phá hủy để kéo mình về
        for (var obstacle : map.getListObstacles()) {
            if (obstacle.getTag() != null && obstacle.getTag().contains("DESTRUCTIBLE")) {
                Node obstaclePos = new Node(obstacle.getX(), obstacle.getY());
                int distance = dist(myPos, obstaclePos);
                if (distance >= 3 && distance <= 6) {
                    String direction = directionTo(myPos, obstaclePos);
                    if (direction != null && !direction.isEmpty()) {
                        return direction;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Logic sử dụng BELL (Chuông)
     * - Sử dụng khi có nhiều player trong vùng 7x7
     * - Tạo cơ hội tấn công khi player bị confuse
     */
    private static String shouldUseBell(GameMap map, Inventory inv, Node myPos) {
        int playerCount = 0;
        
        // Đếm player khác trong vùng 7x7
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                int distance = dist(myPos, playerPos);
                if (distance <= 3) {
                    playerCount++;
                }
            }
        }
        
        // Sử dụng BELL nếu có ít nhất 1 player trong vùng
        if (playerCount >= 1) {
            // Chọn hướng có nhiều player nhất
            String bestDirection = "u";
            int maxPlayers = 0;
            
            for (String dir : new String[]{"u", "d", "l", "r"}) {
                Node checkPos = moveTo(myPos, dir);
                int playersInDirection = 0;
                
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = dist(checkPos, playerPos);
                        if (distance <= 3) {
                            playersInDirection++;
                        }
                    }
                }
                
                if (playersInDirection > maxPlayers) {
                    maxPlayers = playersInDirection;
                    bestDirection = dir;
                }
            }
            
            return bestDirection;
        }
        
        return null;
    }

    /**
     * Logic sử dụng SAHUR_BAT (Gậy đánh bóng)
     * - Đẩy player ra xa khi bị bao vây
     * - Đẩy player vào obstacle để gây stun
     * - Tạo khoảng cách an toàn
     */
    private static String shouldUseSahurBat(GameMap map, Inventory inv, Node myPos) {
        // Kiểm tra player khác gần
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                int distance = dist(myPos, playerPos);
                if (distance <= 5) {
                    String direction = directionTo(myPos, playerPos);
                    if (direction != null && !direction.isEmpty()) {
                        // Kiểm tra xem có obstacle phía sau player không để gây stun
                        Node behindPlayer = moveTo(playerPos, direction);
                        for (var obstacle : map.getListObstacles()) {
                            if (obstacle.getX() == behindPlayer.x && obstacle.getY() == behindPlayer.y) {
                                if (obstacle.getTag() != null && 
                                    (obstacle.getTag().contains("DESTRUCTIBLE") || 
                                     obstacle.getTag().contains("CAN_SHOOT_THROUGH"))) {
                                    return direction; // Đẩy vào obstacle để gây stun
                                }
                            }
                        }
                        return direction; // Đẩy ra xa
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Kiểm tra xem có nên ưu tiên sử dụng special weapon hơn weapon thường không
     */
    public static boolean shouldPrioritizeSpecialWeapon(GameMap map, Inventory inv, Node myPos, String targetDirection) {
        if (inv.getSpecial() == null) return false;
        
        String specialId = inv.getSpecial().getId();
        
        switch (specialId) {
            case "ROPE":
                // Ưu tiên ROPE khi player ở xa và có thể kéo về để tấn công
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = dist(myPos, playerPos);
                        if (distance >= 3 && distance <= 6) {
                            String ropeDirection = directionTo(myPos, playerPos);
                            if (ropeDirection != null && ropeDirection.equals(targetDirection)) {
                                return true;
                            }
                        }
                    }
                }
                break;
                
            case "BELL":
                // Ưu tiên BELL khi có nhiều player trong vùng
                int playerCount = 0;
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = dist(myPos, playerPos);
                        if (distance <= 3) {
                            playerCount++;
                        }
                    }
                }
                if (playerCount >= 1) return true;
                break;
                
            case "SAHUR_BAT":
                // Ưu tiên SAHUR_BAT khi bị bao vây hoặc cần tạo khoảng cách
                int nearbyPlayers = 0;
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = dist(myPos, playerPos);
                        if (distance <= 2) {
                            nearbyPlayers++;
                        }
                    }
                }
                if (nearbyPlayers >= 1) return true;
                break;
        }
        
        return false;
    }

    /**
     * Tìm hướng tối ưu để sử dụng special weapon
     */
    public static String findOptimalSpecialWeaponDirection(GameMap map, Inventory inv, Node myPos) {
        if (inv.getSpecial() == null) return null;
        
        String specialId = inv.getSpecial().getId();
        String bestDirection = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (String dir : new String[]{"u", "d", "l", "r"}) {
            double score = evaluateSpecialWeaponDirection(map, inv, myPos, dir, specialId);
            if (score > bestScore) {
                bestScore = score;
                bestDirection = dir;
            }
        }
        
        return bestDirection;
    }

    /**
     * Đánh giá điểm số cho hướng sử dụng special weapon
     */
    private static double evaluateSpecialWeaponDirection(GameMap map, Inventory inv, Node myPos, String direction, String specialId) {
        double score = 0;
        
        switch (specialId) {
            case "ROPE":
                // Điểm cao cho việc kéo player về gần
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = dist(myPos, playerPos);
                        if (distance >= 3 && distance <= 6) {
                            String ropeDirection = directionTo(myPos, playerPos);
                            if (ropeDirection != null && ropeDirection.equals(direction)) {
                                score += 100 - distance * 10; // Càng gần càng tốt
                            }
                        }
                    }
                }
                break;
                
            case "BELL":
                // Điểm cao cho việc confuse nhiều player
                Node checkPos = moveTo(myPos, direction);
                int playerCount = 0;
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = dist(checkPos, playerPos);
                        if (distance <= 3) {
                            playerCount++;
                        }
                    }
                }
                score += playerCount * 50;
                break;
                
            case "SAHUR_BAT":
                // Điểm cao cho việc đẩy player vào obstacle
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = dist(myPos, playerPos);
                        if (distance <= 5) {
                            String batDirection = directionTo(myPos, playerPos);
                            if (batDirection != null && batDirection.equals(direction)) {
                                Node behindPlayer = moveTo(playerPos, direction);
                                for (var obstacle : map.getListObstacles()) {
                                    if (obstacle.getX() == behindPlayer.x && obstacle.getY() == behindPlayer.y) {
                                        if (obstacle.getTag() != null && 
                                            (obstacle.getTag().contains("DESTRUCTIBLE") || 
                                             obstacle.getTag().contains("CAN_SHOOT_THROUGH"))) {
                                            score += 200; // Bonus cho việc gây stun
                                        }
                                    }
                                }
                                score += 100 - distance * 10;
                            }
                        }
                    }
                }
                break;
        }
        
        return score;
    }
} 