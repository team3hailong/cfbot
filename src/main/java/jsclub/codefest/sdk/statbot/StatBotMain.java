package jsclub.codefest.sdk.statbot;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.healing_items.HealingItem;

import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.algorithm.PathUtils;
import java.util.ArrayList;
import java.util.List;

public class StatBotMain {
    public static void main(String[] args) {
        // Nhập trực tiếp các thông tin cần thiết
        String secretKey = "sk-_UgcHnbHQACXmdeS-1263w:ZcELGs3FRAvZSBLpHoYD6KN6ylSqmx0yBZB-grJUsZYBQhUNmmtY3E62vFtWOEyRX3JCwXpRy6d4IzqcJ3LCFg"; // <-- Thay bằng secretKey thật
        String gameID = "146559";      // <-- Thay bằng gameID thật
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
                // Nếu bị STUN, ưu tiên dùng vật phẩm giải hiệu ứng nếu có
                if (EffectUtils.isStunned(hero.getEffects())) {
                    System.out.println("Bot đang bị STUN, ưu tiên dùng vật phẩm giải hiệu ứng nếu có");
                    for (HealingItem item : inv.getListHealingItem()) {
                        if ("ELIXIR".equals(item.getId()) || "MAGIC".equals(item.getId()) || "COMPASS".equals(item.getId())) {
                            hero.useItem(item.getId());
                            return;
                        }
                    }
                    // Nếu không có vật phẩm giải hiệu ứng, không làm gì cả
                    return;
                }

                Node myPos = new Node(me.x, me.y);
                System.out.println("Vị trí hiện tại: (" + myPos.x + "," + myPos.y + ") HP: " + me.getHealth());

                // 1. Emergency healing khi máu rất thấp (ưu tiên cao nhất)
                if (me.getHealth() < 20 && !inv.getListHealingItem().isEmpty()) {
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
                        int myStrength = StatBotUtils.evaluateStrength(me, inv);
                        int oppStrength = StatBotUtils.evaluateStrength(p, null); // Không có inventory đối thủ, chỉ dựa vào máu và vũ khí cơ bản
                        System.out.println("So sánh sức mạnh: mình=" + myStrength + ", đối thủ=" + oppStrength);
                        
                        if (distance == 1) {
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
                Node bestItem = StatBotUtils.findBestItemToPickup(map, inv, myPos);
                if (bestItem == null) {
                    System.out.println("Không tìm thấy item tốt để nhặt");
                } else {
                    System.out.println("Best item để nhặt: (" + bestItem.x + "," + bestItem.y + ")");
                }
                if (bestItem != null) {
                    boolean isChest = map.getListChests().stream().anyMatch(chest -> chest.x == bestItem.x && chest.y == bestItem.y);
                    if (isChest) {
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
                            String path = PathUtils.getShortestPath(map, new ArrayList<>(), me, bestItem, true);
                            if (path == null) {
                                System.out.println("Không tìm được đường đi tới rương");
                            } else if (path.isEmpty()) {
                                System.out.println("Đã tới cạnh rương, sẽ nhặt hoặc tấn công");
                            } else {
                                System.out.println("Di chuyển tới rương, bước đầu: " + path.charAt(0));
                            }
                            if (path != null && !path.isEmpty()) {
                                hero.move(String.valueOf(path.charAt(0)));
                                return;
                            }
                        }
                    } else {
                        String path = PathUtils.getShortestPath(map, new ArrayList<>(), me, bestItem, false);
                        if (path == null) {
                            System.out.println("Không tìm được đường đi tới item");
                        } else if (path.isEmpty()) {
                            System.out.println("Đã tới nơi, sẽ nhặt item");
                        } else {
                            System.out.println("Di chuyển tới item, bước đầu: " + path.charAt(0));
                        }
                        if (path != null) {
                            if (path.isEmpty()) {
                                hero.pickupItem();
                                if ((map.getListHealingItems().contains(bestItem) && inv.getListHealingItem().size() >= 4)
                                    || (map.getAllGun().contains(bestItem) && inv.getGun() != null)
                                    || (map.getAllMelee().contains(bestItem) && inv.getMelee() != null && !inv.getMelee().getId().equals("HAND"))
                                    || (map.getListArmors().contains(bestItem) && (inv.getArmor() != null || inv.getHelmet() != null))) {
                                    String escapeDir = StatBotUtils.findSmartDirection(myPos, map, inv);
                                    System.out.println("Inventory đầy, không nhặt được item, di chuyển sang hướng: " + escapeDir);
                                    hero.move(escapeDir);
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
                        String path = PathUtils.getShortestPath(map, new ArrayList<>(), me, nearestHeal, false);
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