package jsclub.codefest.sdk.statbot;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.effects.Effect;

import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.algorithm.PathUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class StatBotMain {
    static Set<Node> failedPickupItems = new HashSet<>();
    static int lastInventoryHash = 0;
    static Node targetItemPos = null;
    static String targetItemId = null;
    static long timeOutsideSafeZone = 0; // Thời gian ở ngoài bo
    static long lastMapUpdateTime = 0; // Thời gian update map cuối cùng
    static List<String> recentMoves = new ArrayList<>(); // Theo dõi hướng di chuyển gần đây
    static Node lastPickedItemPos = null; // Vị trí item cuối cùng đã nhặt
    static long lastPickupTime = 0; // Thời gian nhặt item cuối cùng
    static Map<String, List<Node>> playerPositions = new HashMap<>(); // Lịch sử vị trí của các player

    // === Các hàm tiện ích tách từ code lặp lại ===
    /**
     * Lấy danh sách các node cần tránh (rương, obstacle, trap gây stun)
     */
    private static List<Node> getRestrictedNodes(GameMap map) {
        List<Node> restrictedNodes = new ArrayList<>();
        for (var chest : map.getListChests()) {
            restrictedNodes.add(new Node(chest.x, chest.y));
        }
        for (var obs : map.getListObstacles()) {
            if (obs.getTag() != null && obs.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                restrictedNodes.add(new Node(obs.getX(), obs.getY()));
            }
        }
        for (var trap : map.getListTraps()) {
            if (trap.getTag() != null && trap.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                restrictedNodes.add(new Node(trap.getX(), trap.getY()));
            }
        }
        return restrictedNodes;
    }

    /**
     * Tìm đường đi an toàn tới một node, trả về path string hoặc null nếu không tìm được
     */
    private static String findSafePath(GameMap map, Player me, Node target) {
        List<Node> restrictedNodes = getRestrictedNodes(map);
        return PathUtils.getShortestPath(map, restrictedNodes, me, target, false);
    }

    /**
     * Xử lý nhặt item thông minh, trả về true nếu đã thực hiện hành động (move hoặc pickup)
     */
    private static boolean handlePickupItem(Hero hero, GameMap map, Inventory inv, Player me, Node myPos, Node itemPos, String itemId) {
        String currentId = StatBotUtils.getItemIdByPosition(map, itemPos);
        String currentType = StatBotUtils.getItemTypeByPosition(map, itemPos);
        if (itemId.equals(currentId) && !"chest".equals(currentType)) {
            String path = findSafePath(map, me, itemPos);
                            if (path == null) {
                    int center = map.getMapSize() / 2;
                    int dx = Integer.compare(center, myPos.x);
                    int dy = Integer.compare(center, myPos.y);
                    String moveDir = null;
                    if (Math.abs(dx) > Math.abs(dy)) {
                        moveDir = dx > 0 ? "u" : "d";
                    } else if (dy != 0) {
                        moveDir = dy > 0 ? "r" : "l";
                    } else {
                        moveDir = "u";
                    }
                    try {
                        addMoveToHistory(moveDir);
                        hero.move(moveDir);
                    } catch (java.io.IOException e) {
                        System.out.println("Lỗi khi gọi hero.move: " + e.getMessage());
                    }
                    return true;
                } else if (path.isEmpty()) {
                    try {
                        hero.pickupItem();
                        // Ghi nhận item đã nhặt
                        lastPickedItemPos = new Node(itemPos.x, itemPos.y);
                        lastPickupTime = System.currentTimeMillis();
                    } catch (java.io.IOException e) {
                        System.out.println("Lỗi khi gọi hero.pickupItem: " + e.getMessage());
                    }
                    targetItemPos = null;
                    targetItemId = null;
                    return true;
                } else {
                    if (path.length() == 1) {
                        StatBotUtils.manageInventoryBeforePickup(map, inv, itemPos, hero);
                    }
                    try {
                        String moveDir = String.valueOf(path.charAt(0));
                        addMoveToHistory(moveDir);
                        hero.move(moveDir);
                    } catch (java.io.IOException e) {
                        System.out.println("Lỗi khi gọi hero.move: " + e.getMessage());
                    }
                    return true;
                }
        }
        return false;
    }

    /**
     * Xử lý hiệu ứng, trả về true nếu đã thực hiện hành động (dùng item giải hiệu ứng, hồi máu, v.v.)
     */
    private static boolean handleEffects(Hero hero, Inventory inv, List<Effect> currentEffects) {
        int effectPriority = EffectUtils.getEffectPriority(currentEffects);
        if (effectPriority > 0) {
            if (EffectUtils.isControlled(currentEffects)) {
                for (HealingItem item : inv.getListHealingItem()) {
                    if ("ELIXIR".equals(item.getId())) {
                        try {
                            hero.useItem(item.getId());
                        } catch (java.io.IOException e) {
                            System.out.println("Lỗi khi gọi hero.useItem (ELIXIR): " + e.getMessage());
                        }
                        return true;
                    }
                }
                for (HealingItem item : inv.getListHealingItem()) {
                    if ("COMPASS".equals(item.getId())) {
                        try {
                            hero.useItem(item.getId());
                        } catch (java.io.IOException e) {
                            System.out.println("Lỗi khi gọi hero.useItem (COMPASS): " + e.getMessage());
                        }
                        return true;
                    }
                }
                return true;
            }
            if (EffectUtils.isTakingDamageOverTime(currentEffects)) {
                if (!inv.getListHealingItem().isEmpty()) {
                    HealingItem bestHeal = StatBotUtils.getBestHealingItem(inv.getListHealingItem());
                    if (bestHeal != null) {
                        try {
                            hero.useItem(bestHeal.getId());
                        } catch (java.io.IOException e) {
                            System.out.println("Lỗi khi gọi hero.useItem (healing): " + e.getMessage());
                        }
                        return true;
                    }
                }
                for (HealingItem item : inv.getListHealingItem()) {
                    if ("MAGIC".equals(item.getId())) {
                        try {
                            hero.useItem(item.getId());
                        } catch (java.io.IOException e) {
                            System.out.println("Lỗi khi gọi hero.useItem (MAGIC): " + e.getMessage());
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem có đang bị mắc kẹt trong vòng lặp di chuyển không
     */
    private static boolean isInMovementLoop() {
        if (recentMoves.size() < 6) return false;
        
        // Kiểm tra pattern lặp lại (ví dụ: u,d,u,d,u,d)
        for (int i = 0; i <= recentMoves.size() - 6; i++) {
            String pattern1 = recentMoves.get(i) + recentMoves.get(i+1);
            String pattern2 = recentMoves.get(i+2) + recentMoves.get(i+3);
            String pattern3 = recentMoves.get(i+4) + recentMoves.get(i+5);
            
            if (pattern1.equals(pattern2) && pattern2.equals(pattern3)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Thêm hướng di chuyển vào lịch sử
     */
    private static void addMoveToHistory(String direction) {
        recentMoves.add(direction);
        if (recentMoves.size() > 10) {
            recentMoves.remove(0);
        }
    }

    public static void main(String[] args) {
        // Nhập trực tiếp các thông tin cần thiết
        String secretKey = "sk-_UgcHnbHQACXmdeS-1263w:ZcELGs3FRAvZSBLpHoYD6KN6ylSqmx0yBZB-grJUsZYBQhUNmmtY3E62vFtWOEyRX3JCwXpRy6d4IzqcJ3LCFg"; // <-- Thay bằng secretKey thật
        String gameID = "174091";      // <-- Thay bằng gameID thật
        String playerName = "YOUR_PLAYER_NAME_HERE"; // <-- Thay bằng tên người chơi
        String serverURL = "https://cf25-server.jsclub.dev";   // <-- Thay bằng URL server

        Hero hero = new Hero(gameID, playerName, secretKey);
        // Cài đặt logic bot tối ưu trong callback onMapUpdate
        hero.setOnMapUpdate(args1 -> {
            try {
                // Chỉ log khi có lỗi hoặc sự kiện bất thường
                if (args1 == null || args1.length == 0) {
                    System.err.println("[ERROR] args1 null hoặc rỗng, không có dữ liệu map update");
                    return;
                }
                byte[] data = (byte[]) args1[0];
                GameMap map = hero.getGameMap();
                map.updateOnUpdateMap(data);
                Inventory inv = hero.getInventory();
                Player me = map.getCurrentPlayer();
                if (me == null || me.getHealth() == null || me.getHealth() <= 0) {
                    System.err.println("[ERROR] Không tìm thấy player hoặc player đã chết");
                    return;
                }
                List<Effect> currentEffects = hero.getEffects();
                // Log hiệu ứng chỉ khi có hiệu ứng bất lợi
                if (EffectUtils.getEffectPriority(currentEffects) > 0) {
                    System.err.println("[WARN] Effects bất lợi: " + EffectUtils.getEffectsDescription(currentEffects));
                }
                Node myPos = new Node(me.x, me.y);
                // Ưu tiên tiếp tục nhặt item mục tiêu nếu còn tồn tại và không phải rương
                if (targetItemPos != null && targetItemId != null) {
                    if (handlePickupItem(hero, map, inv, me, myPos, targetItemPos, targetItemId)) return;
                    else {
                        targetItemPos = null;
                        targetItemId = null;
                    }
                }
                // Emergency healing khi máu rất thấp
                if (me.getHealth() < 25 && !inv.getListHealingItem().isEmpty()) {
                    HealingItem bestHeal = StatBotUtils.getBestHealingItem(inv.getListHealingItem());
                    if (bestHeal != null) {
                        hero.useItem(bestHeal.getId());
                        return;
                    }
                }
                // === CẬP NHẬT LỊCH SỬ VỊ TRÍ CỦA CÁC PLAYER ===
                StatBotUtils.updatePlayerPositions(map, playerPositions);
                
                // Combat và special weapon
                List<Player> others = map.getOtherPlayerInfo();
                if (EffectUtils.shouldAvoidCombat(currentEffects)) {
                    String safeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                    if (safeDir != null) {
                        addMoveToHistory(safeDir);
                        hero.move(safeDir);
                        return;
                    }
                }
                
                // === KIỂM TRA TẤN CÔNG BOT NGƯỜI CHƠI KHÁC ĐỨNG IM ===
                Player vulnerablePlayer = StatBotUtils.findVulnerablePlayerToAttack(map, myPos, playerPositions);
                if (vulnerablePlayer != null) {
                    Node opp = new Node(vulnerablePlayer.x, vulnerablePlayer.y);
                    int distance = StatBotUtils.dist(myPos, opp);
                    int myStrength = StatBotUtils.evaluateStrengthWithEffects(me, inv, currentEffects);
                    int oppStrength = StatBotUtils.evaluateStrength(vulnerablePlayer, null);
                    
                    System.out.println("TẤN CÔNG BOT ĐỨNG IM: " + vulnerablePlayer.getId() + " (HP: " + vulnerablePlayer.getHealth() + ", distance: " + distance + ")");
                    
                    if (distance == 1) {
                        if (!EffectUtils.canAttackSafely(currentEffects)) {
                            String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                            addMoveToHistory(safeDir);
                            hero.move(safeDir);
                            return;
                        }
                        
                        // Tấn công bot đứng im ngay lập tức
                        String attackDirection = StatBotUtils.directionTo(myPos, opp);
                        if (inv.getSpecial() != null && StatBotUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, attackDirection)) {
                            hero.useSpecial(attackDirection);
                            return;
                        }
                        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                            hero.attack(attackDirection);
                            return;
                        } else if (inv.getGun() != null) {
                            hero.shoot(attackDirection);
                            return;
                        }
                    } else if (distance <= 6) {
                        if (!EffectUtils.canAttackSafely(currentEffects)) {
                            String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                            addMoveToHistory(safeDir);
                            hero.move(safeDir);
                            return;
                        }
                        
                        String attackDirection = StatBotUtils.directionTo(myPos, opp);
                        if (inv.getSpecial() != null && StatBotUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, attackDirection)) {
                            hero.useSpecial(attackDirection);
                            return;
                        }
                        if (inv.getGun() != null) {
                            hero.shoot(attackDirection);
                            return;
                        } else if (inv.getThrowable() != null) {
                            hero.throwItem(attackDirection, distance);
                            return;
                        }
                    }
                }
                
                // === LOGIC TẤN CÔNG THÔNG THƯỜNG (CHO CÁC TRƯỜNG HỢP KHÁC) ===
                String specialDirection = StatBotUtils.shouldUseSpecialWeapon(map, inv, myPos);
                if (specialDirection != null) {
                    hero.useSpecial(specialDirection);
                    return;
                }
                
                for (Player p : others) {
                    if (p.getHealth() != null && p.getHealth() > 0) {
                        Node opp = new Node(p.x, p.y);
                        int distance = StatBotUtils.dist(myPos, opp);
                        int myStrength = StatBotUtils.evaluateStrengthWithEffects(me, inv, currentEffects);
                        int oppStrength = StatBotUtils.evaluateStrength(p, null);
                        
                        if (distance == 1) {
                            if (!EffectUtils.canAttackSafely(currentEffects)) {
                                String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                                addMoveToHistory(safeDir);
                                hero.move(safeDir);
                                return;
                            }
                            if (myStrength > oppStrength || p.getHealth() < 20) {
                                String attackDirection = StatBotUtils.directionTo(myPos, opp);
                                if (inv.getSpecial() != null && StatBotUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, attackDirection)) {
                                    hero.useSpecial(attackDirection);
                                    return;
                                }
                                if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                                    hero.attack(attackDirection);
                                    return;
                                } else if (inv.getGun() != null) {
                                    hero.shoot(attackDirection);
                                    return;
                                }
                            } else if (myStrength < oppStrength) {
                                String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                                addMoveToHistory(safeDir);
                                hero.move(safeDir);
                                return;
                            } else {
                                if (me.getHealth() > 40) {
                                    String attackDirection = StatBotUtils.directionTo(myPos, opp);
                                    if (inv.getSpecial() != null && StatBotUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, attackDirection)) {
                                        hero.useSpecial(attackDirection);
                                        return;
                                    }
                                    if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                                        hero.attack(attackDirection);
                                        return;
                                    } else if (inv.getGun() != null) {
                                        hero.shoot(attackDirection);
                                        return;
                                    }
                                } else {
                                    if (!inv.getListHealingItem().isEmpty()) {
                                        HealingItem bestHeal = StatBotUtils.getBestHealingItem(inv.getListHealingItem());
                                        if (bestHeal != null) {
                                            hero.useItem(bestHeal.getId());
                                            return;
                                        }
                                    }
                                    String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                                    addMoveToHistory(safeDir);
                                    hero.move(safeDir);
                                    return;
                                }
                            }
                        } else if (distance <= 6) {
                            if (!EffectUtils.canAttackSafely(currentEffects)) {
                                String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                                addMoveToHistory(safeDir);
                                hero.move(safeDir);
                                return;
                            }
                            String attackDirection = StatBotUtils.directionTo(myPos, opp);
                            if (inv.getSpecial() != null && StatBotUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, attackDirection)) {
                                hero.useSpecial(attackDirection);
                                return;
                            }
                            if (inv.getGun() != null && (myStrength >= oppStrength || p.getHealth() < 20)) {
                                hero.shoot(attackDirection);
                                return;
                            } else if (inv.getThrowable() != null && (myStrength >= oppStrength || p.getHealth() < 20)) {
                                hero.throwItem(attackDirection, distance);
                                return;
                            }
                        }
                    }
                }
                // Quản lý inventory và nhặt items
                int currentInventoryHash = inv.hashCode();
                if (currentInventoryHash != lastInventoryHash) {
                    failedPickupItems.clear();
                    lastInventoryHash = currentInventoryHash;
                }
                
                // === KIỂM TRA VỊ TRÍ HIỆN TẠI SO VỚI BO ===
                int safeZone = map.getSafeZone();
                int mapSize = map.getMapSize();
                boolean myPosInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(myPos, safeZone, mapSize);
                
                // Cập nhật thời gian ở ngoài bo
                long currentTime = System.currentTimeMillis();
                if (lastMapUpdateTime > 0) {
                    if (!myPosInSafe) {
                        timeOutsideSafeZone += (currentTime - lastMapUpdateTime);
                    } else {
                        timeOutsideSafeZone = 0; // Reset khi vào bo
                    }
                }
                lastMapUpdateTime = currentTime;
                
                // Nếu ở ngoài bo quá 10 giây hoặc máu thấp, buộc vào bo
                if (!myPosInSafe && (timeOutsideSafeZone > 10000 || me.getHealth() < 40)) {
                    // === KIỂM TRA ENEMY GẦN ===
                    boolean enemyNear = false;
                    for (Player p : others) {
                        if (p.getHealth() != null && p.getHealth() > 0) {
                            Node opp = new Node(p.x, p.y);
                            int distance = StatBotUtils.dist(myPos, opp);
                            if (distance <= 2) {
                                enemyNear = true;
                                break;
                            }
                        }
                    }
                    if (enemyNear) {
                        // Ưu tiên né ENEMY trước
                        String safeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                        if (safeDir != null) {
                            addMoveToHistory(safeDir);
                            hero.move(safeDir);
                            return;
                        }
                    } else {
                        // Không có ENEMY gần, mới ép vào bo
                        int center = mapSize / 2;
                        Node centerNode = new Node(center, center);
                        String pathToBo = jsclub.codefest.sdk.algorithm.PathUtils.getShortestPath(map, new ArrayList<>(), myPos, centerNode, true);
                        if (pathToBo != null && !pathToBo.isEmpty()) {
                            System.out.println("FORCING INTO SAFEZONE: outside for " + timeOutsideSafeZone + "ms, HP: " + me.getHealth());
                            hero.move(String.valueOf(pathToBo.charAt(0)));
                            return;
                        }
                    }
                }
                
                Node bestItem = StatBotUtils.findBestItemToPickup(map, inv, myPos, failedPickupItems);
                if (bestItem != null) {
                    // === KIỂM TRA VÒNG LẶP GIỮA 2 ITEM GẦN NHAU ===
                    if (lastPickedItemPos != null && lastPickupTime > 0) {
                        int timeSinceLastPickup = (int)(currentTime - lastPickupTime);
                        
                        // Nếu item mới cách item cũ không quá 3 ô và thời gian < 5 giây, có thể bị vòng lặp
                        if (StatBotUtils.dist(lastPickedItemPos, bestItem) <= 3 && timeSinceLastPickup < 5000) {
                            System.out.println("[WARN] PHÁT HIỆN VÒNG LẶP ITEM: last=" + lastPickedItemPos + ", new=" + bestItem + ", time=" + timeSinceLastPickup + "ms");
                            // Bỏ qua item này, di chuyển thông minh thay vào đó
                            String smartDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                            addMoveToHistory(smartDir);
                            hero.move(smartDir);
                            return;
                        }
                    }
                    
                    // Kiểm tra xem item có nằm ngoài bo không
                    boolean itemInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(bestItem, safeZone, mapSize);
                    
                    // Nếu đang ở trong bo và item nằm ngoài bo, chỉ nhặt nếu là item cực kỳ quan trọng
                    if (myPosInSafe && !itemInSafe) {
                        String itemId = StatBotUtils.getItemIdByPosition(map, bestItem);
                        if (!"DRAGON_EGG".equals(itemId) && !"ELIXIR_OF_LIFE".equals(itemId)) {
                            // Bỏ qua item ngoài bo, di chuyển thông minh thay vào đó
                            String smartDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                            addMoveToHistory(smartDir);
                            hero.move(smartDir);
                            return;
                        }
                    }
                    
                    targetItemPos = bestItem;
                    targetItemId = StatBotUtils.getItemIdByPosition(map, bestItem);
                    boolean isChest = map.getListChests().stream().anyMatch(chest -> chest.x == bestItem.x && chest.y == bestItem.y);
                    if (isChest) {
                        if (me.getHealth() != null && me.getHealth() < 20) {
                            String escapeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                            hero.move(escapeDir);
                            return;
                        }
                        if (inv.getGun() != null) {
                            String gunDir = StatBotUtils.getGunDirectionToChest(myPos, bestItem, inv.getGun());
                            if (gunDir != null) {
                                hero.shoot(gunDir);
                                return;
                            }
                        }
                        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                            String meleeDir = StatBotUtils.getMeleeDirectionToChest(myPos, bestItem, inv.getMelee());
                            if (meleeDir != null) {
                                hero.attack(meleeDir);
                                return;
                            }
                        }
                        if (StatBotUtils.dist(myPos, bestItem) == 1) {
                            String dir = StatBotUtils.directionTo(myPos, bestItem);
                            if (inv.getGun() != null) {
                                hero.shoot(dir);
                            } else if (inv.getMelee() != null) {
                                hero.attack(dir);
                            }
                            return;
                        } else {
                            String path = findSafePath(map, me, bestItem);
                            if (path != null) {
                                if (path.isEmpty()) {
                                    StatBotUtils.manageInventoryBeforePickup(map, inv, bestItem, hero);
                                    hero.pickupItem();
                                    // Ghi nhận item đã nhặt
                                    lastPickedItemPos = new Node(bestItem.x, bestItem.y);
                                    lastPickupTime = currentTime;
                                    if ((map.getListHealingItems().contains(bestItem) && inv.getListHealingItem().size() >= 4)
                                        || (map.getAllGun().contains(bestItem) && inv.getGun() != null)
                                        || (map.getAllMelee().contains(bestItem) && inv.getMelee() != null && !inv.getMelee().getId().equals("HAND"))
                                        || (map.getListArmors().contains(bestItem) && (inv.getArmor() != null || inv.getHelmet() != null))) {
                                        String escapeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                                        addMoveToHistory(escapeDir);
                                        hero.move(escapeDir);
                                        failedPickupItems.add(bestItem);
                                    }
                                    return;
                                }
                                if (path.length() == 1) {
                                    StatBotUtils.manageInventoryBeforePickup(map, inv, bestItem, hero);
                                }
                                hero.move(String.valueOf(path.charAt(0)));
                                return;
                            }
                        }
                    } else {
                        String path = findSafePath(map, me, bestItem);
                        if (path != null) {
                            if (path.isEmpty()) {
                                StatBotUtils.manageInventoryBeforePickup(map, inv, bestItem, hero);
                                hero.pickupItem();
                                // Ghi nhận item đã nhặt
                                lastPickedItemPos = new Node(bestItem.x, bestItem.y);
                                lastPickupTime = currentTime;
                                if ((map.getListHealingItems().contains(bestItem) && inv.getListHealingItem().size() >= 4)
                                    || (map.getAllGun().contains(bestItem) && inv.getGun() != null)
                                    || (map.getAllMelee().contains(bestItem) && inv.getMelee() != null && !inv.getMelee().getId().equals("HAND"))
                                    || (map.getListArmors().contains(bestItem) && (inv.getArmor() != null || inv.getHelmet() != null))) {
                                    String escapeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                                    addMoveToHistory(escapeDir);
                                    hero.move(escapeDir);
                                    failedPickupItems.add(bestItem);
                                }
                                return;
                            }
                            if (path.length() == 1) {
                                StatBotUtils.manageInventoryBeforePickup(map, inv, bestItem, hero);
                            }
                            hero.move(String.valueOf(path.charAt(0)));
                            return;
                        }
                    }
                }
                // Healing khi máu thấp và an toàn
                if (me.getHealth() < 50) {
                    List<Node> heals = new ArrayList<>(map.getListHealingItems());
                    Node nearestHeal = heals.stream().min((a, b) ->
                        Integer.compare(StatBotUtils.dist(a, myPos), StatBotUtils.dist(b, myPos))
                    ).orElse(null);
                    if (nearestHeal != null) {
                        String path = findSafePath(map, me, nearestHeal);
                        if (path != null) {
                            if (path.isEmpty()) {
                                hero.pickupItem();
                            } else {
                                hero.move(String.valueOf(path.charAt(0)));
                            }
                            return;
                        }
                    }
                }
                // Di chuyển thông minh
                if (!EffectUtils.canMoveNormally(currentEffects)) {
                    return;
                }
                String smartDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                // Nếu phát hiện vòng lặp, chọn hướng ngẫu nhiên khác
                if (isInMovementLoop()) {
                    System.out.println("[WARN] BOT PHÁT HIỆN VÒNG LẶP DI CHUYỂN, ĐỔI HƯỚNG!");
                    String[] dirs = {"u", "d", "l", "r"};
                    for (String dir : dirs) {
                        if (!recentMoves.isEmpty() && !dir.equals(recentMoves.get(recentMoves.size()-1))) {
                            smartDir = dir;
                            break;
                        }
                    }
                }
                addMoveToHistory(smartDir);
                hero.move(smartDir);
            } catch (Exception e) {
                System.err.println("[EXCEPTION] Lỗi trong callback onMapUpdate: " + e.getMessage());
                e.printStackTrace();
            }
        });
        try {
            hero.start(serverURL);
        } catch (Exception e) {
            System.err.println("[EXCEPTION] Lỗi khi start hero: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 