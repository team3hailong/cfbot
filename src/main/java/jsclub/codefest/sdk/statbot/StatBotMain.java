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

public class StatBotMain {
    static Set<Node> failedPickupItems = new HashSet<>();
    static int lastInventoryHash = 0;
    static Node targetItemPos = null;
    static String targetItemId = null;

    public static void main(String[] args) {
        // Nhập trực tiếp các thông tin cần thiết
        String secretKey = "sk-_UgcHnbHQACXmdeS-1263w:ZcELGs3FRAvZSBLpHoYD6KN6ylSqmx0yBZB-grJUsZYBQhUNmmtY3E62vFtWOEyRX3JCwXpRy6d4IzqcJ3LCFg"; // <-- Thay bằng secretKey thật
        String gameID = "155375";      // <-- Thay bằng gameID thật
        String playerName = "YOUR_PLAYER_NAME_HERE"; // <-- Thay bằng tên người chơi
        String serverURL = "https://cf25-server.jsclub.dev";   // <-- Thay bằng URL server

        Hero hero = new Hero(gameID, playerName, secretKey);
        // Cài đặt logic bot tối ưu trong callback onMapUpdate
        hero.setOnMapUpdate(args1 -> {
            try {
                System.out.println("=== onMapUpdate callback ===");
                if (args1 == null || args1.length == 0) {
                    System.out.println("args1 null hoặc rỗng, không có dữ liệu map update");
                    return;
                }
                byte[] data = (byte[]) args1[0];
                GameMap map = hero.getGameMap();
                map.updateOnUpdateMap(data);
                Inventory inv = hero.getInventory();
                Player me = map.getCurrentPlayer();
                if (me == null) {
                    System.out.println("Không tìm thấy player của mình trên map");
                    return;
                }
                if (me.getHealth() == null) {
                    System.out.println("Không lấy được máu của player");
                    return;
                }
                if (me.getHealth() <= 0) {
                    System.out.println("Player đã chết, không thực hiện hành động");
                    return;
                }
                // Xử lý effects một cách thông minh
                List<Effect> currentEffects = hero.getEffects();
                System.out.println("Effects hiện tại: " + EffectUtils.getEffectsDescription(currentEffects));
                
                // Kiểm tra và xử lý effects theo mức độ ưu tiên
                int effectPriority = EffectUtils.getEffectPriority(currentEffects);
                if (effectPriority > 0) {
                    System.out.println("Bot đang bị effects với mức ưu tiên: " + effectPriority);
                    
                    // Ưu tiên cao nhất: Stun/Blind/Reverse - cần giải ngay
                    if (EffectUtils.isControlled(currentEffects)) {
                        System.out.println("Bot đang bị khống chế, ưu tiên dùng vật phẩm giải hiệu ứng");
                        for (HealingItem item : inv.getListHealingItem()) {
                            if ("ELIXIR".equals(item.getId())) {
                                System.out.println("Sử dụng ELIXIR để giải khống chế");
                                hero.useItem(item.getId());
                                return;
                            }
                        }
                        // Nếu không có ELIXIR, thử dùng COMPASS để stun area
                        for (HealingItem item : inv.getListHealingItem()) {
                            if ("COMPASS".equals(item.getId())) {
                                System.out.println("Sử dụng COMPASS để stun area");
                                hero.useItem(item.getId());
                                return;
                            }
                        }
                        // Nếu không có vật phẩm giải hiệu ứng, không làm gì cả
                        return;
                    }
                    
                    // Ưu tiên trung bình: Poison/Bleed - cần hồi máu
                    if (EffectUtils.isTakingDamageOverTime(currentEffects)) {
                        System.out.println("Bot đang bị sát thương theo thời gian, ưu tiên hồi máu");
                        if (!inv.getListHealingItem().isEmpty()) {
                            HealingItem bestHeal = StatBotUtils.getBestHealingItem(inv.getListHealingItem());
                            if (bestHeal != null) {
                                System.out.println("Sử dụng healing item: " + bestHeal.getId());
                                hero.useItem(bestHeal.getId());
                                return;
                            }
                        }
                        
                        // Nếu không có healing item, thử dùng MAGIC để tàng hình
                        for (HealingItem item : inv.getListHealingItem()) {
                            if ("MAGIC".equals(item.getId())) {
                                System.out.println("Sử dụng MAGIC để tàng hình thoát khỏi nguy hiểm");
                                hero.useItem(item.getId());
                                return;
                            }
                        }
                    }
                }
                
                // Kiểm tra effects có lợi
                if (EffectUtils.hasBeneficialEffects(currentEffects)) {
                    System.out.println("Bot đang có effects có lợi: " + EffectUtils.getEffectsDescription(currentEffects));
                    // Có thể tận dụng effects có lợi để tấn công mạnh hơn
                }

                Node myPos = new Node(me.x, me.y);
                System.out.println("Vị trí hiện tại: (" + myPos.x + "," + myPos.y + ") HP: " + me.getHealth());

                // Ưu tiên tiếp tục nhặt item mục tiêu nếu còn tồn tại và không phải rương
                if (targetItemPos != null && targetItemId != null) {
                    String currentId = StatBotUtils.getItemIdByPosition(map, targetItemPos);
                    String currentType = StatBotUtils.getItemTypeByPosition(map, targetItemPos);
                    if (targetItemId.equals(currentId) && !"chest".equals(currentType)) {
                        // Item mục tiêu vẫn còn và không phải rương, tiếp tục di chuyển/nhặt nó
                        System.out.println("Tiếp tục nhặt item mục tiêu: (" + targetItemPos.x + "," + targetItemPos.y + ") id=" + targetItemId);
                        // BỔ SUNG: Tránh các node là rương, obstacle và trap gây STUN khi tìm đường
                        List<Node> restrictedNodes = new ArrayList<>();
                        for (var chest : map.getListChests()) {
                            restrictedNodes.add(new Node(chest.x, chest.y));
                        }
                        // Thêm các obstacle gây STUN
                        for (var obs : map.getListObstacles()) {
                            if (obs.getTag() != null && obs.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                                restrictedNodes.add(new Node(obs.getX(), obs.getY()));
                            }
                        }
                        // Thêm các trap gây STUN
                        for (var trap : map.getListTraps()) {
                            if (trap.getTag() != null && trap.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                                restrictedNodes.add(new Node(trap.getX(), trap.getY()));
                            }
                        }
                        // LOG restrictedNodes
                        System.out.println("Restricted nodes (tránh STUN):");
                        for (Node n : restrictedNodes) {
                            System.out.println(" - (" + n.x + "," + n.y + ")");
                        }
                        String path = PathUtils.getShortestPath(map, restrictedNodes, me, targetItemPos, false);
                        System.out.println("Đường đi tới item: " + path);
                        if (path == null) {
                            System.out.println("Không tìm được đường đi tới item");
                            // Nếu đang ở ngoài bo, tìm hướng di chuyển về phía bo
                            int center = map.getMapSize() / 2;
                            int dx = Integer.compare(center, myPos.x);
                            int dy = Integer.compare(center, myPos.y);
                            String moveDir = null;
                            if (Math.abs(dx) > Math.abs(dy)) {
                                moveDir = dx > 0 ? "u" : "d";
                            } else if (dy != 0) {
                                moveDir = dy > 0 ? "r" : "l";
                            } else {
                                moveDir = "u"; // fallback nếu đã ở tâm
                            }
                            System.out.println("BOT OUTSIDE SAFEZONE: fallback move " + moveDir + " to get closer to safezone");
                            hero.move(moveDir);
                            return;
                        } else if (path.isEmpty()) {
                            hero.pickupItem();
                            // Reset sau khi nhặt xong
                            targetItemPos = null;
                            targetItemId = null;
                        } else {
                            if (path.length() == 1) {
                                System.out.println("Sắp nhặt item mục tiêu, kiểm tra/revoke inventory nếu cần");
                                StatBotUtils.manageInventoryBeforePickup(map, inv, targetItemPos, hero);
                            }
                            hero.move(String.valueOf(path.charAt(0)));
                        }
                        return;
                    } else {
                        // Nếu là rương hoặc item mục tiêu đã bị nhặt, reset target để xử lý như cũ
                        System.out.println("Item mục tiêu đã biến mất, bị nhặt hoặc là rương, reset target");
                        targetItemPos = null;
                        targetItemId = null;
                    }
                }

                // 1. Emergency healing khi máu rất thấp (ưu tiên cao nhất)
                if (me.getHealth() < 25 && !inv.getListHealingItem().isEmpty()) {
                    System.out.println("Đang ở chế độ hồi máu khẩn cấp");
                    HealingItem bestHeal = StatBotUtils.getBestHealingItem(inv.getListHealingItem());
                    if (bestHeal != null) {
                        System.out.println("Sử dụng healing item: " + bestHeal.getId());
                        hero.useItem(bestHeal.getId());
                        return;
                    } else {
                        System.out.println("Không tìm thấy healing item phù hợp để dùng");
                    }
                }

                // 4. Combat với player khác và sử dụng special weapons (ưu tiên cao hơn items)
                List<Player> others = map.getOtherPlayerInfo();
                System.out.println("Other players: " + others.size());
                
                // Kiểm tra xem có nên tránh combat do effects không
                if (EffectUtils.shouldAvoidCombat(currentEffects)) {
                    System.out.println("Bot đang bị effects không thuận lợi cho combat, ưu tiên né tránh");
                    // Tìm hướng an toàn để di chuyển
                    String safeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                    if (safeDir != null) {
                        System.out.println("Di chuyển theo hướng an toàn: " + safeDir);
                        hero.move(safeDir);
                        return;
                    }
                }
                
                // 4.1. Kiểm tra sử dụng special weapon trước
                String specialDirection = StatBotUtils.shouldUseSpecialWeapon(map, inv, myPos);
                if (specialDirection != null) {
                    System.out.println("Sử dụng special weapon: " + inv.getSpecial().getId() + " hướng: " + specialDirection);
                    hero.useSpecial(specialDirection);
                    return;
                }
                
                for (Player p : others) {
                    if (p.getHealth() != null && p.getHealth() > 0) {
                        Node opp = new Node(p.x, p.y);
                        int distance = StatBotUtils.dist(myPos, opp);
                        int myStrength = StatBotUtils.evaluateStrengthWithEffects(me, inv, currentEffects);
                        int oppStrength = StatBotUtils.evaluateStrength(p, null); // Không có inventory đối thủ, chỉ dựa vào máu và vũ khí cơ bản
                        System.out.println("So sánh sức mạnh (có xem xét effects): mình=" + myStrength + ", đối thủ=" + oppStrength);
                        
                        if (distance == 1) {
                            // Kiểm tra xem có thể tấn công an toàn không
                            if (!EffectUtils.canAttackSafely(currentEffects)) {
                                System.out.println("Không thể tấn công an toàn do effects, ưu tiên né tránh");
                                String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                                hero.move(safeDir);
                                return;
                            }
                            
                            if (myStrength > oppStrength || p.getHealth() < 20) {
                                System.out.println("Mạnh hơn hoặc đối thủ yếu, sẽ tấn công hoặc bắn");
                                String attackDirection = StatBotUtils.directionTo(myPos, opp);
                                
                                // Kiểm tra xem có nên ưu tiên special weapon không
                                if (inv.getSpecial() != null && StatBotUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, attackDirection)) {
                                    System.out.println("Ưu tiên sử dụng special weapon: " + inv.getSpecial().getId());
                                    hero.useSpecial(attackDirection);
                                    return;
                                }
                                
                                if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                                    System.out.println("Tấn công bằng melee: " + inv.getMelee().getId());
                                    hero.attack(attackDirection);
                                    return;
                                } else if (inv.getGun() != null) {
                                    System.out.println("Bắn bằng súng: " + inv.getGun().getId());
                                    hero.shoot(attackDirection);
                                    return;
                                }
                            } else if (myStrength < oppStrength) {
                                System.out.println("Đối thủ mạnh hơn, ưu tiên né tránh");
                                String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                                hero.move(safeDir);
                                return;
                            } else {
                                System.out.println("Sức mạnh ngang cơ, cân nhắc tấn công nếu máu mình còn nhiều");
                                if (me.getHealth() > 40) {
                                    String attackDirection = StatBotUtils.directionTo(myPos, opp);
                                    
                                    // Kiểm tra special weapon
                                    if (inv.getSpecial() != null && StatBotUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, attackDirection)) {
                                        System.out.println("Sử dụng special weapon trong combat: " + inv.getSpecial().getId());
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
                                    // Nếu máu thấp, ưu tiên hồi máu hoặc né
                                    if (!inv.getListHealingItem().isEmpty()) {
                                        HealingItem bestHeal = StatBotUtils.getBestHealingItem(inv.getListHealingItem());
                                        if (bestHeal != null) {
                                            hero.useItem(bestHeal.getId());
                                            return;
                                        }
                                    }
                                    String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                                    hero.move(safeDir);
                                    return;
                                }
                            }
                        } else if (distance <= 6) {
                            // Kiểm tra xem có thể tấn công từ xa an toàn không
                            if (!EffectUtils.canAttackSafely(currentEffects)) {
                                System.out.println("Không thể tấn công từ xa an toàn do effects, ưu tiên né tránh");
                                String safeDir = StatBotUtils.safeDirection(myPos, opp, map);
                                hero.move(safeDir);
                                return;
                            }
                            
                            String attackDirection = StatBotUtils.directionTo(myPos, opp);
                            
                            // Kiểm tra special weapon cho tấn công từ xa
                            if (inv.getSpecial() != null && StatBotUtils.shouldPrioritizeSpecialWeapon(map, inv, myPos, attackDirection)) {
                                System.out.println("Sử dụng special weapon từ xa: " + inv.getSpecial().getId());
                                hero.useSpecial(attackDirection);
                                return;
                            }
                            
                            if (inv.getGun() != null && (myStrength >= oppStrength || p.getHealth() < 20)) {
                                System.out.println("Bắn player ở xa bằng súng: " + inv.getGun().getId());
                                hero.shoot(attackDirection);
                                return;
                            } else if (inv.getThrowable() != null && (myStrength >= oppStrength || p.getHealth() < 20)) {
                                System.out.println("Ném vật phẩm vào player ở xa: " + inv.getThrowable().getId());
                                hero.throwItem(attackDirection, distance);
                                return;
                            }
                        }
                    }
                }

                // 5. Quản lý inventory thông minh và nhặt items (ưu tiên cao)
                int currentInventoryHash = inv.hashCode();
                if (currentInventoryHash != lastInventoryHash) {
                    failedPickupItems.clear();
                    lastInventoryHash = currentInventoryHash;
                }
                Node bestItem = StatBotUtils.findBestItemToPickup(map, inv, myPos, failedPickupItems);
                if (bestItem == null) {
                    System.out.println("Không tìm thấy item tốt để nhặt");
                } else {
                    System.out.println("Best item để nhặt: (" + bestItem.x + "," + bestItem.y + ")");
                    // Lưu lại item mục tiêu để ưu tiên nhặt ở các bước tiếp theo
                    targetItemPos = bestItem;
                    targetItemId = StatBotUtils.getItemIdByPosition(map, bestItem);
                }
                if (bestItem != null) {
                    boolean isChest = map.getListChests().stream().anyMatch(chest -> chest.x == bestItem.x && chest.y == bestItem.y);
                    if (isChest) {
                        // Nếu máu quá thấp thì mới bỏ phá rương để chạy
                        if (me.getHealth() != null && me.getHealth() < 20) {
                            System.out.println("Máu quá thấp, ưu tiên bỏ chạy thay vì phá rương!");
                            String escapeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                            hero.move(escapeDir);
                            return;
                        }
                        // Ưu tiên bắn rương từ xa nếu có thể
                        if (inv.getGun() != null) {
                            String gunDir = StatBotUtils.getGunDirectionToChest(myPos, bestItem, inv.getGun());
                            if (gunDir != null) {
                                System.out.println("Bắn rương từ xa bằng súng " + inv.getGun().getId() + " hướng: " + gunDir + ", tầm: " + inv.getGun().getRange());
                                hero.shoot(gunDir);
                                return;
                            }
                        }
                        // Ưu tiên tấn công rương từ xa bằng Melee nếu có thể
                        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                            String meleeDir = StatBotUtils.getMeleeDirectionToChest(myPos, bestItem, inv.getMelee());
                            if (meleeDir != null) {
                                System.out.println("Tấn công rương từ xa bằng melee " + inv.getMelee().getId() + " hướng: " + meleeDir + ", độ rộng: " + inv.getMelee().getRange());
                                hero.attack(meleeDir);
                                return;
                            }
                        }
                        // Nếu không bắn/tấn công được từ xa thì xử lý như cũ
                        if (StatBotUtils.dist(myPos, bestItem) == 1) {
                            String dir = StatBotUtils.directionTo(myPos, bestItem);
                            System.out.println("Đứng cạnh rương, sẽ tấn công/chest ở hướng: " + dir);
                            if (inv.getGun() != null) {
                                System.out.println("Bắn rương bằng súng " + inv.getGun().getId() + " có tầm: " + inv.getGun().getRange());
                                hero.shoot(dir);
                            } else if (inv.getMelee() != null) {
                                hero.attack(dir);
                                System.out.println("Tấn công rương bằng melee " + inv.getMelee().getId() + " có tầm: " + inv.getMelee().getRange());
                            }
                            return;
                        } else {
                            // Nếu mục tiêu không phải rương, thêm node rương vào restrictedNodes
                            List<Node> restrictedNodes = new ArrayList<>();
                            for (var chest : map.getListChests()) {
                                restrictedNodes.add(new Node(chest.x, chest.y));
                            }
                            // Thêm các obstacle gây STUN
                            for (var obs : map.getListObstacles()) {
                                if (obs.getTag() != null && obs.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                                    restrictedNodes.add(new Node(obs.getX(), obs.getY()));
                                }
                            }
                            // Thêm các trap gây STUN
                            for (var trap : map.getListTraps()) {
                                if (trap.getTag() != null && trap.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                                    restrictedNodes.add(new Node(trap.getX(), trap.getY()));
                                }
                            }
                            String path = PathUtils.getShortestPath(map, restrictedNodes, me, bestItem, false);
                            System.out.println("Đường đi tới item: " + path);
                            if (path == null) {
                                System.out.println("Không tìm được đường đi tới item");
                            } else {
                                if (path.isEmpty()) {
                                    // Trước khi nhặt, luôn kiểm tra và revoke nếu cần
                                    StatBotUtils.manageInventoryBeforePickup(map, inv, bestItem, hero);
                                    hero.pickupItem();
                                    // Kiểm tra lại nếu inventory vẫn đầy (không nhặt được)
                                    if ((map.getListHealingItems().contains(bestItem) && inv.getListHealingItem().size() >= 4)
                                        || (map.getAllGun().contains(bestItem) && inv.getGun() != null)
                                        || (map.getAllMelee().contains(bestItem) && inv.getMelee() != null && !inv.getMelee().getId().equals("HAND"))
                                        || (map.getListArmors().contains(bestItem) && (inv.getArmor() != null || inv.getHelmet() != null))) {
                                        String escapeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                                        System.out.println("Inventory đầy, không nhặt được item, di chuyển sang hướng: " + escapeDir);
                                        hero.move(escapeDir);
                                        // Đánh dấu node này là đã thử nhặt nhưng thất bại
                                        failedPickupItems.add(bestItem);
                                    }
                                    return;
                                }
                                if (path.length() == 1) {
                                    System.out.println("Sắp nhặt item, kiểm tra/revoke inventory nếu cần");
                                    StatBotUtils.manageInventoryBeforePickup(map, inv, bestItem, hero);
                                }
                                hero.move(String.valueOf(path.charAt(0)));
                                return;
                            }
                        }
                    } else {
                        // Nếu mục tiêu không phải rương, thêm node rương vào restrictedNodes
                        List<Node> restrictedNodes = new ArrayList<>();
                        for (var chest : map.getListChests()) {
                            restrictedNodes.add(new Node(chest.x, chest.y));
                        }
                        // Thêm các obstacle gây STUN
                        for (var obs : map.getListObstacles()) {
                            if (obs.getTag() != null && obs.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                                restrictedNodes.add(new Node(obs.getX(), obs.getY()));
                            }
                        }
                        // Thêm các trap gây STUN
                        for (var trap : map.getListTraps()) {
                            if (trap.getTag() != null && trap.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                                restrictedNodes.add(new Node(trap.getX(), trap.getY()));
                            }
                        }
                        String path = PathUtils.getShortestPath(map, restrictedNodes, me, bestItem, false);
                        System.out.println("Đường đi tới item: " + path);
                        if (path == null) {
                            System.out.println("Không tìm được đường đi tới item");
                        } else {
                            if (path.isEmpty()) {
                                // Trước khi nhặt, luôn kiểm tra và revoke nếu cần
                                StatBotUtils.manageInventoryBeforePickup(map, inv, bestItem, hero);
                                hero.pickupItem();
                                // Kiểm tra lại nếu inventory vẫn đầy (không nhặt được)
                                if ((map.getListHealingItems().contains(bestItem) && inv.getListHealingItem().size() >= 4)
                                    || (map.getAllGun().contains(bestItem) && inv.getGun() != null)
                                    || (map.getAllMelee().contains(bestItem) && inv.getMelee() != null && !inv.getMelee().getId().equals("HAND"))
                                    || (map.getListArmors().contains(bestItem) && (inv.getArmor() != null || inv.getHelmet() != null))) {
                                    String escapeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                                    System.out.println("Inventory đầy, không nhặt được item, di chuyển sang hướng: " + escapeDir);
                                    hero.move(escapeDir);
                                    // Đánh dấu node này là đã thử nhặt nhưng thất bại
                                    failedPickupItems.add(bestItem);
                                }
                                return;
                            }
                            if (path.length() == 1) {
                                System.out.println("Sắp nhặt item, kiểm tra/revoke inventory nếu cần");
                                StatBotUtils.manageInventoryBeforePickup(map, inv, bestItem, hero);
                            }
                            hero.move(String.valueOf(path.charAt(0)));
                            return;
                        }
                    }
                }

                // 7. Healing khi máu thấp và an toàn
                if (me.getHealth() < 50) {
                    List<Node> heals = new ArrayList<>(map.getListHealingItems());
                    Node nearestHeal = heals.stream().min((a, b) ->
                        Integer.compare(StatBotUtils.dist(a, myPos), StatBotUtils.dist(b, myPos))
                    ).orElse(null);
                    if (nearestHeal == null) {
                        System.out.println("Không có healing item nào gần");
                    } else {
                        // BỔ SUNG: Tránh các obstacle và trap gây STUN khi tìm đường tới healing item
                        List<Node> restrictedNodes = new ArrayList<>();
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
                        String path = PathUtils.getShortestPath(map, restrictedNodes, me, nearestHeal, false);
                        System.out.println("Đường đi tới healing item: " + path);
                        if (path == null) {
                            System.out.println("Không tìm được đường đi tới healing item");
                        } else if (path.isEmpty()) {
                            System.out.println("Đã tới healing item, sẽ nhặt");
                        } else {
                            System.out.println("Di chuyển tới healing item, bước đầu: " + path.charAt(0));
                        }
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

                // 8. Di chuyển thông minh - ưu tiên hướng an toàn và có items
                // Kiểm tra xem có thể di chuyển bình thường không
                if (!EffectUtils.canMoveNormally(currentEffects)) {
                    System.out.println("Không thể di chuyển bình thường do effects, không thực hiện hành động");
                    return;
                }
                
                String smartDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                System.out.println("Hướng di chuyển thông minh: " + smartDir);
                hero.move(smartDir);
                System.out.println("Kết thúc callback, đã gọi move với hướng: " + smartDir);
            } catch (Exception e) {
                System.out.println("Lỗi trong callback onMapUpdate: " + e.getMessage());
                e.printStackTrace();
            }
        });
        try {
            hero.start(serverURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 