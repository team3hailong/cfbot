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
import java.util.Map;
import java.util.HashSet;

public class StatBotUtils {
    // Delegate to MovementUtils
    public static int dist(Node a, Node b) {
        return MovementUtils.dist(a, b);
    }
    public static Node moveTo(Node a, String dir) {
        return MovementUtils.moveTo(a, dir);
    }
    
    public static String directionTo(Node a, Node b) {
        return MovementUtils.directionTo(a, b);
    }
    public static String findSmartDirection(Node myPos, GameMap map, Inventory inv) {
        return MovementUtils.findSmartDirection(myPos, map, inv);
    }
    
    public static String findSmartDirectionWithEnemyPrediction(Node myPos, GameMap map, Inventory inv) {
        return MovementUtils.findSmartDirectionWithEnemyPrediction(myPos, map, inv);
    }
    public static String safeDirection(Node me, Node enemy, GameMap map) {
        return MovementUtils.safeDirection(me, enemy, map);
    }
    public static boolean isStunObstacle(GameMap map, Node pos) {
        return MovementUtils.isStunObstacle(map, pos);
    }
    public static Node findNearestDefensiveObstacle(GameMap map, Node myPos) {
        return MovementUtils.findNearestDefensiveObstacle(map, myPos);
    }
    // Delegate to ItemUtils
    public static int getBasePickupPointsSimple(String itemId) {
        return ItemUtils.getBasePickupPointsSimple(itemId);
    }
    public static boolean hasCurrentWeapon(Inventory inv, String newItemId) {
        return ItemUtils.hasCurrentWeapon(inv, newItemId);
    }
    public static String getItemIdByPosition(GameMap map, Node itemPos) {
        return ItemUtils.getItemIdByPosition(map, itemPos);
    }
    public static String getItemTypeByPosition(GameMap map, Node itemPos) {
        return ItemUtils.getItemTypeByPosition(map, itemPos);
    }
    public static boolean isChest(GameMap map, Node pos) {
        return ItemUtils.isChest(map, pos);
    }
    public static boolean isDragonEgg(GameMap map, Node pos) {
        return ItemUtils.isDragonEgg(map, pos);
    }
    public static boolean isBetterArmor(Inventory inv, String newId) {
        return ItemUtils.isBetterArmor(inv, newId);
    }
    public static boolean isBetterHelmet(Inventory inv, String newId) {
        return ItemUtils.isBetterHelmet(inv, newId);
    }
    public static HealingItem getBestHealingItem(List<HealingItem> healingItems) {
        return ItemUtils.getBestHealingItem(healingItems);
    }

    public static void manageInventoryBeforePickup(GameMap map, Inventory inv, Node itemPos, jsclub.codefest.sdk.Hero hero) {
        try {
            for (Obstacle chest : map.getListChests()) {
                if (Math.abs(chest.x - itemPos.x) <= 1 && Math.abs(chest.y - itemPos.y) <= 1) {
                    return;
                }
            }
            if (map.getAllGun().stream().anyMatch(g -> g.x == itemPos.x && g.y == itemPos.y) && inv.getGun() != null) {
                hero.revokeItem(inv.getGun().getId());
            } else if (map.getAllMelee().stream().anyMatch(m -> m.x == itemPos.x && m.y == itemPos.y) && inv.getMelee() != null && 
                       !inv.getMelee().getId().equals("HAND")) {
                hero.revokeItem(inv.getMelee().getId());
            } else if (map.getAllSpecial().stream().anyMatch(s -> s.x == itemPos.x && s.y == itemPos.y) && inv.getSpecial() != null) {
                hero.revokeItem(inv.getSpecial().getId());
            } else if (map.getListArmors().stream().anyMatch(a -> a.x == itemPos.x && a.y == itemPos.y)) {
                // Xác định loại item (armor hay helmet)
                String itemId = getItemIdByPosition(map, itemPos);
                String itemType = "unknown";
                for (var a : map.getListArmors()) {
                    if (a.x == itemPos.x && a.y == itemPos.y) {
                        if (a.getType() != null && a.getType().toString().equals("ARMOR")) {
                            itemType = "armor";
                        } else if (a.getType() != null && a.getType().toString().equals("HELMET")) {
                            itemType = "helmet";
                        }
                        break;
                    }
                }
                if (itemType.equals("armor") && inv.getArmor() != null) {
                    // Chỉ bỏ armor nếu cái mới tốt hơn
                    if (isBetterArmor(inv, itemId)) {
                        hero.revokeItem(inv.getArmor().getId());
                    }
                } else if (itemType.equals("helmet") && inv.getHelmet() != null) {
                    // Chỉ bỏ helmet nếu cái mới tốt hơn
                    if (isBetterHelmet(inv, itemId)) {
                        hero.revokeItem(inv.getHelmet().getId());
                    }
                }
            } else if (map.getListHealingItems().stream().anyMatch(h -> h.x == itemPos.x && h.y == itemPos.y) && inv.getListHealingItem().size() >= 4) {
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
    // ... giữ lại các hàm khác nếu cần ...

    public static int getItemPriority(String itemType, String itemId, Inventory currentInv, GameMap map, Node myPos, Node itemPos, boolean isDanger, boolean isChest, double chestGoodRate, float myHP) {
        int basePoints = getBasePickupPoints(itemId, itemType, currentInv, map, myPos, itemPos, isDanger, isChest, chestGoodRate, myHP);
        return basePoints;
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
            Object modelObj = null;
            if ("weapon".equals(itemType)) {
                modelObj = jsclub.codefest.sdk.factory.WeaponFactory.getWeaponById(itemId);
                // Nếu chưa có vũ khí (gun, melee, special đều null hoặc HAND), cộng thêm 50 điểm
                boolean hasWeapon = (inv.getGun() != null) ||
                                   (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) ||
                                   (inv.getSpecial() != null);
                if (!hasWeapon) {
                    base += 50;
                }
                if (modelObj != null) {
                    Weapon weapon = (Weapon) modelObj;
                    base += weapon.getDamage() * 1.5 + weapon.getRange() * 2;
                    if (weapon.getEffects() != null) base += weapon.getEffects().size() * 10;
                }
            } else if ("armor".equals(itemType) || "helmet".equals(itemType)) {
                modelObj = jsclub.codefest.sdk.factory.ArmorFactory.getArmorById(itemId);
                if (modelObj != null) {
                    Armor armor = (Armor) modelObj;
                    base += armor.getDamageReduce() * 2 + armor.getHealthPoint() * 1.5;
                }
            } else if ("healing".equals(itemType)) {
                modelObj = jsclub.codefest.sdk.factory.HealingItemFactory.getHealingItemById(itemId);
                if (modelObj != null) {
                    HealingItem heal = (HealingItem) modelObj;
                    base += heal.getHealingHP() * 2;
                    if (heal.getEffects() != null) base += heal.getEffects().size() * 10;
                }
            } else if ("special".equals(itemType)) {
                modelObj = jsclub.codefest.sdk.factory.WeaponFactory.getWeaponById(itemId);
                if (modelObj != null) {
                    Weapon weapon = (Weapon) modelObj;
                    base += weapon.getDamage() * 1.5 + weapon.getRange() * 2;
                    if (weapon.getEffects() != null) base += weapon.getEffects().size() * 10;
                }
            } else {
                base += 10; // fallback cho loại không xác định
            }
            // Nếu object model có hàm getPickupPoints, cộng điểm này vào base
            if (modelObj != null) {
                try {
                    java.lang.reflect.Method method = modelObj.getClass().getMethod("getPickupPoints");
                    Object pickupPoints = method.invoke(modelObj);
                    if (pickupPoints instanceof Integer) {
                        System.out.println("pickupPoints: " + pickupPoints);
                        base += (Integer) pickupPoints*2;
                    }
                } catch (NoSuchMethodException nsme) {
                    // Không có hàm getPickupPoints, bỏ qua
                } catch (Exception e) {
                    // Lỗi khác khi gọi, bỏ qua
                }
            }
        } catch (Exception e) {
            base += 10;
        }
        // 2. Khoảng cách
        int distance = dist(myPos, itemPos);
        base -= distance * 2;
        
        // Nếu vật phẩm ngay gần (distance < 4) và chưa có vật phẩm cùng loại thì cộng 250 điểm
        if (distance < 4) {
            boolean shouldBonus = false;
            if ("weapon".equals(itemType)) {
                // Kiểm tra nếu là gun, melee, special dựa vào id
                var weapon = jsclub.codefest.sdk.factory.WeaponFactory.getWeaponById(itemId);
                if (weapon != null) {
                    var weaponType = weapon.getType();
                    if (weaponType == jsclub.codefest.sdk.model.ElementType.GUN && (inv.getGun() == null)) shouldBonus = true;
                    else if (weaponType == jsclub.codefest.sdk.model.ElementType.MELEE && (inv.getMelee() == null || "HAND".equals(inv.getMelee().getId()))) shouldBonus = true;
                    else if (weaponType == jsclub.codefest.sdk.model.ElementType.SPECIAL && inv.getSpecial() == null) shouldBonus = true;
                }
            } else if ("armor".equals(itemType)) {
                // Kiểm tra riêng armor và helmet
                if (isBetterArmor(inv, itemId) && (inv.getArmor() == null)) shouldBonus = true;
                if (isBetterHelmet(inv, itemId) && (inv.getHelmet() == null)) shouldBonus = true;
            } else if ("healing".equals(itemType)) {
                if (inv.getListHealingItem().isEmpty() || inv.getListHealingItem().size() < 3) shouldBonus = true;
            } else if ("special".equals(itemType)) {
                if (inv.getSpecial() == null) shouldBonus = true;
            }
            if (shouldBonus){
                System.out.println("shouldBonus: itemId=" + itemId + ", itemType=" + itemType + ", shouldBonus=" + shouldBonus);
                base += 250;
            } 
        }
        // 3. Rương: cộng điểm theo tỉ lệ ra đồ xịn
        if (isChest) {
            base += chestGoodRate * 247;
        }
        // 4. Tình trạng hiện tại
        if ("healing".equals(itemType)) {
            if (myHP < 50) base += 40;
            if (myHP < 30) base += 60;
        }
        if ("weapon".equals(itemType) && hasCurrentWeapon(inv, itemId)) {
            base -= 50;
        }
        // === SỬA LOGIC ARMOR: KHÔNG NHẶT ARMOR KHI ĐÃ CÓ ===
        if ("armor".equals(itemType)) {
            // Nếu đã có armor và item này không tốt hơn, giảm điểm mạnh
            if (inv.getArmor() != null) {
                if (!isBetterArmor(inv, itemId)) {
                    base -= 500; // Giảm điểm mạnh để không nhặt armor kém hơn
                } else {
                    base += 50; // Cộng điểm nếu armor tốt hơn
                }
            }
        }
        if ("helmet".equals(itemType)) {
            // Nếu đã có helmet và item này không tốt hơn, giảm điểm mạnh
            if (inv.getHelmet() != null) {
                if (!isBetterHelmet(inv, itemId)) {
                    base -= 500; // Giảm điểm mạnh để không nhặt helmet kém hơn
                } else {
                    base += 50; // Cộng điểm nếu helmet tốt hơn
                }
            }
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
        if ("SHOTGUN".equals(itemId) || "MACE".equals(itemId)) base += 100;
        if ("AXE".equals(itemId) || "KNIFE".equals(itemId)) base += 100;
        return base;
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
        int safeZone = map.getSafeZone();
        int mapSize = map.getMapSize();
        boolean myPosInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(myPos, safeZone, mapSize);
        
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
            
            // === SỬA LOGIC: KIỂM TRA VỊ TRÍ ITEM SO VỚI BO ===
            boolean itemInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(item, safeZone, mapSize);
            
            // Nếu đang ở trong bo, chỉ nhặt item trong bo (trừ khi là DRAGON_EGG hoặc item cực kỳ quan trọng)
            if (myPosInSafe && !itemInSafe) {
                // Chỉ cho phép nhặt item ngoài bo nếu là DRAGON_EGG hoặc item cực kỳ quan trọng
                if (!"DRAGON_EGG".equals(itemId) && !"ELIXIR_OF_LIFE".equals(itemId)) {
                    continue; // Bỏ qua item ngoài bo
                }
                // Nếu là item quan trọng ngoài bo, giảm điểm đáng kể
                double score = getBasePickupPoints(itemId, itemType, inv, map, myPos, item, isDanger, isChest, chestGoodRate, myHP);
                score -= 500; // Giảm điểm mạnh cho item ngoài bo
                if (itemNearOtherPlayer) {
                    score -= 100;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = item;
                }
                continue;
            }
            
            // Nếu đang ở ngoài bo, ưu tiên item trong bo
            if (!myPosInSafe && itemInSafe) {
                double score = getBasePickupPoints(itemId, itemType, inv, map, myPos, item, isDanger, isChest, chestGoodRate, myHP);
                score += 300; // Cộng điểm cho item trong bo
                if (itemNearOtherPlayer) {
                    score -= 100;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = item;
                }
                continue;
            }
            
            // Bổ sung: Nếu là rương ngoài bo, chỉ cho phép lấy nếu cầm HAND, máu đủ lớn, không có địch gần
            if (isChest && !itemInSafe) {
                boolean hasHand = (inv.getMelee() != null && "HAND".equals(inv.getMelee().getId()));
                boolean safeHP = myHP > 50; // Có thể điều chỉnh ngưỡng máu này
                boolean noEnemyNear = true;
                for (Enemy e : map.getListEnemies()) {
                    if (dist(item, new Node(e.x, e.y)) <= 2) {
                        noEnemyNear = false;
                        break;
                    }
                }
                if (!(hasHand && safeHP && noEnemyNear)) {
                    continue; // Bỏ qua rương ngoài bo nếu không đủ điều kiện an toàn
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
            System.out.println("DEBUG FROM SDK findBestItemToPickup : " + itemId + " " + itemType + " " + score + " (nearOtherPlayer: " + itemNearOtherPlayer + ", itemInSafe: " + itemInSafe + ", myPosInSafe: " + myPosInSafe + ")");
        }
        System.out.println("Best score: " + bestScore);
        return best;
    }

    public static String getGunDirectionToChest(Node myPos, Node chestPos, Weapon gun) {
        return ItemUtils.getGunDirectionToChest(myPos, chestPos, gun);
    }

    public static String getMeleeDirectionToChest(Node myPos, Node chestPos, Weapon melee) {
        return ItemUtils.getMeleeDirectionToChest(myPos, chestPos, melee);
    }

    public static int evaluateStrength(Player p, Inventory inv) {
        return CombatUtils.evaluateStrength(p, inv);
    }

    public static int evaluateStrengthWithEffects(Player p, Inventory inv, List<jsclub.codefest.sdk.model.effects.Effect> effects) {
        return CombatUtils.evaluateStrengthWithEffects(p, inv, effects);
    }

    public static String shouldUseSpecialWeapon(GameMap map, Inventory inv, Node myPos) {
        return SpecialWeaponUtils.shouldUseSpecialWeapon(map, inv, myPos);
    }

    public static String findBatStunDirection(GameMap map, Node myPos) {
        return SpecialWeaponUtils.findBatStunDirection(map, myPos);
    }

    public static boolean shouldPrioritizeSpecialWeapon(GameMap map, Inventory inv, Node myPos, String targetDirection) {
        return SpecialWeaponUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, targetDirection);
    }

    public static String findOptimalSpecialWeaponDirection(GameMap map, Inventory inv, Node myPos) {
        return SpecialWeaponUtils.findOptimalSpecialWeaponDirection(map, inv, myPos);
    }

    // === CÁC HÀM DỰ ĐOÁN ĐƯỜNG ĐI CỦA ENEMY ===
    
    /**
     * Dự đoán vị trí tiếp theo của enemy dựa trên loại enemy và hành vi
     */
    public static Node predictEnemyNextPosition(Enemy enemy, GameMap map, Node myPos) {
        Node enemyPos = new Node(enemy.x, enemy.y);
        String enemyId = enemy.getId() != null ? enemy.getId().toUpperCase() : "";
        
        // Dự đoán dựa trên loại enemy
        switch (enemyId) {
            case "SPIRIT":
                return predictSpiritMovement(enemyPos, myPos, map);
            case "GOLEM":
            case "RHINO":
            case "ANACONDA":
                return predictAggressiveEnemyMovement(enemyPos, myPos, map);
            case "LEOPARD":
            case "NATIVE":
                return predictHunterEnemyMovement(enemyPos, myPos, map);
            case "GHOST":
                return predictGhostMovement(enemyPos, myPos, map);
            default:
                return predictGenericEnemyMovement(enemyPos, myPos, map);
        }
    }
    
    /**
     * Dự đoán đường đi của SPIRIT (thường di chuyển ngẫu nhiên nhưng có xu hướng về phía player)
     */
    private static Node predictSpiritMovement(Node enemyPos, Node myPos, GameMap map) {
        int distance = dist(enemyPos, myPos);
        
        // Nếu SPIRIT gần player (distance <= 3), có 70% khả năng di chuyển về phía player
        if (distance <= 3 && Math.random() < 0.7) {
            String dir = directionTo(enemyPos, myPos);
            return moveTo(enemyPos, dir);
        }
        
        // Nếu xa player, di chuyển ngẫu nhiên nhưng có xu hướng về trung tâm
        int centerX = map.getMapSize() / 2;
        int centerY = map.getMapSize() / 2;
        Node center = new Node(centerX, centerY);
        
        if (Math.random() < 0.6) {
            String dir = directionTo(enemyPos, center);
            return moveTo(enemyPos, dir);
        } else {
            // Di chuyển ngẫu nhiên
            String[] dirs = {"u", "d", "l", "r"};
            String randomDir = dirs[(int)(Math.random() * dirs.length)];
            return moveTo(enemyPos, randomDir);
        }
    }
    
    /**
     * Dự đoán đường đi của enemy hung hăng (GOLEM, RHINO, ANACONDA)
     */
    private static Node predictAggressiveEnemyMovement(Node enemyPos, Node myPos, GameMap map) {
        int distance = dist(enemyPos, myPos);
        
        // Enemy hung hăng luôn di chuyển về phía player nếu trong tầm nhìn
        if (distance <= 5) {
            String dir = directionTo(enemyPos, myPos);
            return moveTo(enemyPos, dir);
        }
        
        // Nếu xa player, di chuyển về trung tâm để tìm player
        int centerX = map.getMapSize() / 2;
        int centerY = map.getMapSize() / 2;
        Node center = new Node(centerX, centerY);
        String dir = directionTo(enemyPos, center);
        return moveTo(enemyPos, dir);
    }
    
    /**
     * Dự đoán đường đi của enemy săn mồi (LEOPARD, NATIVE)
     */
    private static Node predictHunterEnemyMovement(Node enemyPos, Node myPos, GameMap map) {
        int distance = dist(enemyPos, myPos);
        
        // Hunter enemy có thể "rình" player từ xa
        if (distance <= 4) {
            // 80% khả năng di chuyển về phía player
            if (Math.random() < 0.8) {
                String dir = directionTo(enemyPos, myPos);
                return moveTo(enemyPos, dir);
            } else {
                // 20% khả năng di chuyển ngang để "rình"
                if (Math.random() < 0.5) {
                    return moveTo(enemyPos, "l");
                } else {
                    return moveTo(enemyPos, "r");
                }
            }
        }
        
        // Nếu xa player, di chuyển về trung tâm
        int centerX = map.getMapSize() / 2;
        int centerY = map.getMapSize() / 2;
        Node center = new Node(centerX, centerY);
        String dir = directionTo(enemyPos, center);
        return moveTo(enemyPos, dir);
    }
    
    /**
     * Dự đoán đường đi của GHOST (di chuyển ngẫu nhiên)
     */
    private static Node predictGhostMovement(Node enemyPos, Node myPos, GameMap map) {
        // GHOST di chuyển hoàn toàn ngẫu nhiên
        String[] dirs = {"u", "d", "l", "r"};
        String randomDir = dirs[(int)(Math.random() * dirs.length)];
        return moveTo(enemyPos, randomDir);
    }
    
    /**
     * Dự đoán đường đi của enemy chung
     */
    private static Node predictGenericEnemyMovement(Node enemyPos, Node myPos, GameMap map) {
        int distance = dist(enemyPos, myPos);
        
        // 60% khả năng di chuyển về phía player nếu gần
        if (distance <= 4 && Math.random() < 0.6) {
            String dir = directionTo(enemyPos, myPos);
            return moveTo(enemyPos, dir);
        }
        
        // Di chuyển ngẫu nhiên
        String[] dirs = {"u", "d", "l", "r"};
        String randomDir = dirs[(int)(Math.random() * dirs.length)];
        return moveTo(enemyPos, randomDir);
    }
    
    /**
     * Kiểm tra xem một vị trí có bị enemy đe dọa trong tương lai không
     */
    public static boolean isPositionThreatenedByEnemy(Node pos, GameMap map, Node myPos, int turnsAhead) {
        for (Enemy enemy : map.getListEnemies()) {
            Node enemyPos = new Node(enemy.x, enemy.y);
            
            // Dự đoán vị trí enemy trong tương lai
            Node predictedPos = enemyPos;
            for (int i = 0; i < turnsAhead; i++) {
                predictedPos = predictEnemyNextPosition(enemy, map, myPos);
            }
            
            // Kiểm tra khoảng cách từ vị trí dự đoán đến pos
            int distance = dist(predictedPos, pos);
            String enemyId = enemy.getId() != null ? enemy.getId().toUpperCase() : "";
            
            // Định nghĩa phạm vi nguy hiểm cho từng loại enemy
            int dangerRange = 2; // Mặc định
            switch (enemyId) {
                case "SPIRIT":
                    dangerRange = 1; // SPIRIT chỉ nguy hiểm khi ở cạnh
                    break;
                case "GOLEM":
                case "RHINO":
                case "ANACONDA":
                    dangerRange = 2; // Enemy hung hăng có phạm vi 2 ô
                    break;
                case "LEOPARD":
                case "NATIVE":
                    dangerRange = 3; // Hunter có thể tấn công từ xa
                    break;
                case "GHOST":
                    dangerRange = 2;
                    break;
            }
            
            if (distance <= dangerRange) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Tính điểm an toàn cho một vị trí dựa trên dự đoán enemy
     */
    public static int calculatePositionSafetyScore(Node pos, GameMap map, Node myPos) {
        int safetyScore = 100; // Điểm cơ bản
        
        // Kiểm tra enemy hiện tại
        for (Enemy enemy : map.getListEnemies()) {
            Node enemyPos = new Node(enemy.x, enemy.y);
            int distance = dist(pos, enemyPos);
            String enemyId = enemy.getId() != null ? enemy.getId().toUpperCase() : "";
            
            // Giảm điểm dựa trên khoảng cách và loại enemy
            int dangerRange = 2;
            switch (enemyId) {
                case "SPIRIT":
                    dangerRange = 1;
                    if (distance <= dangerRange) safetyScore -= 50;
                    else if (distance <= 3) safetyScore -= 20;
                    break;
                case "GOLEM":
                case "RHINO":
                case "ANACONDA":
                    dangerRange = 2;
                    if (distance <= dangerRange) safetyScore -= 80;
                    else if (distance <= 4) safetyScore -= 40;
                    break;
                case "LEOPARD":
                case "NATIVE":
                    dangerRange = 3;
                    if (distance <= dangerRange) safetyScore -= 60;
                    else if (distance <= 5) safetyScore -= 30;
                    break;
                case "GHOST":
                    dangerRange = 2;
                    if (distance <= dangerRange) safetyScore -= 40;
                    else if (distance <= 4) safetyScore -= 20;
                    break;
            }
        }
        
        // Kiểm tra dự đoán enemy trong tương lai (1-2 turn)
        for (int turn = 1; turn <= 2; turn++) {
            for (Enemy enemy : map.getListEnemies()) {
                Node enemyPos = new Node(enemy.x, enemy.y);
                Node predictedPos = enemyPos;
                for (int i = 0; i < turn; i++) {
                    predictedPos = predictEnemyNextPosition(enemy, map, myPos);
                }
                
                int distance = dist(pos, predictedPos);
                if (distance <= 2) {
                    safetyScore -= (3 - turn) * 30; // Giảm điểm nhiều hơn cho turn gần hơn
                }
            }
        }
        
        // Cộng điểm nếu ở trong safe zone
        int safeZone = map.getSafeZone();
        int mapSize = map.getMapSize();
        if (jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(pos, safeZone, mapSize)) {
            safetyScore += 50;
        }
        
        return Math.max(0, safetyScore);
    }
    
    /**
     * Cải thiện hàm findBestItemToPickup với dự đoán enemy
     */
    public static Node findBestItemToPickupWithEnemyPrediction(GameMap map, Inventory inv, Node myPos, Set<Node> excludeItems) {
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
        float myHP = (me != null && me.getHealth() != null) ? me.getHealth() : 100;
        boolean isDanger = false;
        
        // Kiểm tra Enemy gần
        for (Enemy e : map.getListEnemies()) {
            if (dist(myPos, new Node(e.x, e.y)) <= 1) {
                isDanger = true;
                break;
            }
        }
        
        // Kiểm tra Player khác gần
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
        int safeZone = map.getSafeZone();
        int mapSize = map.getMapSize();
        boolean myPosInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(myPos, safeZone, mapSize);
        
        for (Node item : allItems) {
            if (excludeItems != null && excludeItems.contains(item)) continue;
            
            String itemId = getItemIdByPosition(map, item);
            String itemType = getItemTypeByPosition(map, item);
            boolean isChest = isChest(map, item);
            double chestGoodRate = isChest ? 0.8 : 0.0;
            
            // Kiểm tra Player khác gần vật phẩm
            boolean itemNearOtherPlayer = false;
            for (Player otherPlayer : map.getOtherPlayerInfo()) {
                if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                    if (dist(item, new Node(otherPlayer.x, otherPlayer.y)) <= 3) {
                        itemNearOtherPlayer = true;
                        break;
                    }
                }
            }
            
            // === CẢI THIỆN: KIỂM TRA AN TOÀN DỰA TRÊN DỰ ĐOÁN ENEMY ===
            boolean itemInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(item, safeZone, mapSize);
            
            // Tính điểm an toàn cho vị trí item
            int itemSafetyScore = calculatePositionSafetyScore(item, map, myPos);
            
            // Kiểm tra xem item có bị enemy đe dọa trong tương lai không
            boolean itemThreatenedByEnemy = isPositionThreatenedByEnemy(item, map, myPos, 2);
            
            // Nếu đang ở trong bo, chỉ nhặt item trong bo (trừ khi là DRAGON_EGG hoặc item cực kỳ quan trọng)
            if (myPosInSafe && !itemInSafe) {
                if (!"DRAGON_EGG".equals(itemId) && !"ELIXIR_OF_LIFE".equals(itemId)) {
                    continue;
                }
                double score = getBasePickupPoints(itemId, itemType, inv, map, myPos, item, isDanger, isChest, chestGoodRate, myHP);
                score -= 500;
                if (itemNearOtherPlayer) score -= 100;
                if (itemThreatenedByEnemy) score -= 200; // Giảm điểm nếu item bị đe dọa
                if (score > bestScore) {
                    bestScore = score;
                    best = item;
                }
                continue;
            }
            
            // Nếu đang ở ngoài bo, ưu tiên item trong bo
            if (!myPosInSafe && itemInSafe) {
                double score = getBasePickupPoints(itemId, itemType, inv, map, myPos, item, isDanger, isChest, chestGoodRate, myHP);
                score += 300;
                if (itemNearOtherPlayer) score -= 100;
                if (itemThreatenedByEnemy) score -= 150; // Giảm điểm nếu item bị đe dọa
                if (score > bestScore) {
                    bestScore = score;
                    best = item;
                }
                continue;
            }
            
            // Bổ sung: Nếu là rương ngoài bo, chỉ cho phép lấy nếu cầm HAND, máu đủ lớn, không có địch gần
            if (isChest && !itemInSafe) {
                boolean hasHand = (inv.getMelee() != null && "HAND".equals(inv.getMelee().getId()));
                boolean safeHP = myHP > 50;
                boolean noEnemyNear = true;
                for (Enemy e : map.getListEnemies()) {
                    if (dist(item, new Node(e.x, e.y)) <= 2) {
                        noEnemyNear = false;
                        break;
                    }
                }
                if (!(hasHand && safeHP && noEnemyNear)) {
                    continue;
                }
            }
            
            double score = getBasePickupPoints(itemId, itemType, inv, map, myPos, item, isDanger, isChest, chestGoodRate, myHP);
            
            // === CẢI THIỆN: ĐIỀU CHỈNH ĐIỂM DỰA TRÊN DỰ ĐOÁN ENEMY ===
            if (itemNearOtherPlayer) {
                score -= 100;
            }
            if (itemThreatenedByEnemy) {
                score -= 200; // Giảm điểm mạnh nếu item bị đe dọa bởi enemy
            }
            
            // Cộng điểm dựa trên độ an toàn của vị trí item
            score += itemSafetyScore * 0.5;
            
            // Nếu máu thấp, ưu tiên item an toàn hơn
            if (myHP < 50) {
                if (itemSafetyScore < 50) {
                    score -= 300; // Giảm điểm mạnh cho item không an toàn khi máu thấp
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
            
            System.out.println("DEBUG FROM SDK findBestItemToPickupWithEnemyPrediction : " + itemId + " " + itemType + " " + score + " (nearOtherPlayer: " + itemNearOtherPlayer + ", itemInSafe: " + itemInSafe + ", myPosInSafe: " + myPosInSafe + ", itemThreatenedByEnemy: " + itemThreatenedByEnemy + ", itemSafetyScore: " + itemSafetyScore + ")");
        }
        
        System.out.println("Best score with enemy prediction: " + bestScore);
        return best;
    }

    /**
     * Kiểm tra xem bot người chơi khác có đang đứng im và xung quanh đủ an toàn để tấn công không
     * @param player Bot người chơi khác cần kiểm tra
     * @param map GameMap hiện tại
     * @param myPos Vị trí của bot mình
     * @param playerPositions Lịch sử vị trí của các player (để kiểm tra đứng im)
     * @return true nếu player đứng im và xung quanh an toàn
     */
    public static boolean isPlayerStandingStillAndSafe(Player player, GameMap map, Node myPos, Map<String, List<Node>> playerPositions) {
        if (player.getHealth() == null || player.getHealth() <= 0) {
            return false;
        }
        
        Node playerPos = new Node(player.x, player.y);
        String playerId = player.getId();
        
        // Kiểm tra xem player có đứng im không (trong 3 turn gần nhất)
        if (playerPositions.containsKey(playerId)) {
            List<Node> positions = playerPositions.get(playerId);
            if (positions.size() >= 3) {
                Node pos1 = positions.get(positions.size() - 1);
                Node pos2 = positions.get(positions.size() - 2);
                Node pos3 = positions.get(positions.size() - 3);
                
                // Nếu vị trí không thay đổi trong 3 turn, coi như đứng im
                if (!pos1.equals(pos2) || !pos2.equals(pos3)) {
                    return false;
                }
            } else {
                return false; // Chưa đủ dữ liệu để xác định
            }
        } else {
            return false; // Chưa có dữ liệu về player này
        }
        
        // Kiểm tra xem xung quanh player có an toàn không
        int safetyScore = calculatePositionSafetyScore(playerPos, map, myPos);
        
        // Kiểm tra có enemy gần player không
        boolean hasEnemyNearby = false;
        for (Enemy enemy : map.getListEnemies()) {
            Node enemyPos = new Node(enemy.x, enemy.y);
            int distance = dist(playerPos, enemyPos);
            if (distance <= 2) {
                hasEnemyNearby = true;
                break;
            }
        }
        
        // Kiểm tra có player khác gần không (tránh tấn công khi có nhiều player)
        int nearbyPlayers = 0;
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0 && !otherPlayer.getId().equals(playerId)) {
                Node otherPos = new Node(otherPlayer.x, otherPlayer.y);
                int distance = dist(playerPos, otherPos);
                if (distance <= 3) {
                    nearbyPlayers++;
                }
            }
        }
        
        // Điều kiện để tấn công:
        // 1. Player đứng im
        // 2. Xung quanh an toàn (safetyScore > 70)
        // 3. Không có enemy gần
        // 4. Không có quá nhiều player khác gần (tối đa 1 player khác)
        return safetyScore > 70 && !hasEnemyNearby && nearbyPlayers <= 1;
    }
    
    /**
     * Tìm bot người chơi khác phù hợp để tấn công (đứng im và xung quanh an toàn)
     * @param map GameMap hiện tại
     * @param myPos Vị trí của bot mình
     * @param playerPositions Lịch sử vị trí của các player
     * @return Player phù hợp để tấn công, hoặc null nếu không có
     */
    public static Player findVulnerablePlayerToAttack(GameMap map, Node myPos, Map<String, List<Node>> playerPositions) {
        Player bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (Player player : map.getOtherPlayerInfo()) {
            if (player.getHealth() == null || player.getHealth() <= 0) {
                continue;
            }
            
            Node playerPos = new Node(player.x, player.y);
            int distance = dist(myPos, playerPos);
            
            // Chỉ tấn công player trong phạm vi hợp lý (1-6 ô)
            if (distance < 1 || distance > 6) {
                continue;
            }
            
            // Kiểm tra xem player có đứng im và an toàn không
            if (!isPlayerStandingStillAndSafe(player, map, myPos, playerPositions)) {
                continue;
            }
            
            // Tính điểm ưu tiên cho player này
            double score = 0;
            
            // Ưu tiên player có máu thấp
            score += (100 - player.getHealth()) * 2;
            
            // Ưu tiên player gần hơn
            score += (7 - distance) * 10;
            
            // Ưu tiên player trong safe zone (dễ tấn công hơn)
            int safeZone = map.getSafeZone();
            int mapSize = map.getMapSize();
            boolean playerInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(playerPos, safeZone, mapSize);
            if (playerInSafe) {
                score += 50;
            }
            
            // Kiểm tra xem có thể tấn công an toàn không
            boolean canAttackSafely = true;
            for (Enemy enemy : map.getListEnemies()) {
                Node enemyPos = new Node(enemy.x, enemy.y);
                int enemyDistance = dist(myPos, enemyPos);
                if (enemyDistance <= 2) {
                    canAttackSafely = false;
                    break;
                }
            }
            
            if (!canAttackSafely) {
                score -= 200; // Giảm điểm nếu không thể tấn công an toàn
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestTarget = player;
            }
        }
        
        return bestTarget;
    }
    
    /**
     * Cập nhật lịch sử vị trí của các player
     * @param map GameMap hiện tại
     * @param playerPositions Map lưu lịch sử vị trí
     */
    public static void updatePlayerPositions(GameMap map, Map<String, List<Node>> playerPositions) {
        // Cập nhật vị trí của tất cả player
        for (Player player : map.getOtherPlayerInfo()) {
            if (player.getHealth() != null && player.getHealth() > 0) {
                String playerId = player.getId();
                Node currentPos = new Node(player.x, player.y);
                
                if (!playerPositions.containsKey(playerId)) {
                    playerPositions.put(playerId, new ArrayList<>());
                }
                
                List<Node> positions = playerPositions.get(playerId);
                positions.add(currentPos);
                
                // Giữ tối đa 5 vị trí gần nhất
                if (positions.size() > 5) {
                    positions.remove(0);
                }
            }
        }
        
        // Xóa dữ liệu của player đã chết
        Set<String> activePlayerIds = new HashSet<>();
        for (Player player : map.getOtherPlayerInfo()) {
            if (player.getHealth() != null && player.getHealth() > 0) {
                activePlayerIds.add(player.getId());
            }
        }
        
        playerPositions.entrySet().removeIf(entry -> !activePlayerIds.contains(entry.getKey()));
    }
} 