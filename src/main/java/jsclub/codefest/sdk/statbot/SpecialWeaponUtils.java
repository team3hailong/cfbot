package jsclub.codefest.sdk.statbot;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.obstacles.Obstacle;

public class SpecialWeaponUtils {

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
                int distance = MovementUtils.dist(myPos, playerPos);
                if (distance >= 3 && distance <= 6) {
                    String direction = MovementUtils.directionTo(myPos, playerPos);
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
                int distance = MovementUtils.dist(myPos, obstaclePos);
                if (distance >= 3 && distance <= 6) {
                    String direction = MovementUtils.directionTo(myPos, obstaclePos);
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
                int distance = MovementUtils.dist(myPos, playerPos);
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
                Node checkPos = MovementUtils.moveTo(myPos, dir);
                int playersInDirection = 0;
                
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = MovementUtils.dist(checkPos, playerPos);
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
     * Tìm hướng dùng SAHUR_BAT để đánh đối thủ vào obstacle gây stun gần nhất
     */
    public static String findBatStunDirection(GameMap map, Node myPos) {
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                int distance = MovementUtils.dist(myPos, playerPos);
                if (distance <= 5) {
                    String direction = MovementUtils.directionTo(myPos, playerPos);
                    if (direction != null && !direction.isEmpty()) {
                        // Kiểm tra obstacle gây stun phía sau player
                        Node behindPlayer = MovementUtils.moveTo(playerPos, direction);
                        if (MovementUtils.isStunObstacle(map, behindPlayer)) {
                            return direction;
                        }
                    }
                }
            }
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
        // Ưu tiên đánh đối thủ vào obstacle gây stun
        String stunDir = findBatStunDirection(map, myPos);
        if (stunDir != null) return stunDir;
        
        // Kiểm tra player khác gần
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                int distance = MovementUtils.dist(myPos, playerPos);
                if (distance <= 5) {
                    String direction = MovementUtils.directionTo(myPos, playerPos);
                    if (direction != null && !direction.isEmpty()) {
                        // Kiểm tra xem có obstacle phía sau player không để gây stun
                        Node behindPlayer = MovementUtils.moveTo(playerPos, direction);
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
                        int distance = MovementUtils.dist(myPos, playerPos);
                        if (distance >= 3 && distance <= 6) {
                            String ropeDirection = MovementUtils.directionTo(myPos, playerPos);
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
                        int distance = MovementUtils.dist(myPos, playerPos);
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
                        int distance = MovementUtils.dist(myPos, playerPos);
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
                        int distance = MovementUtils.dist(myPos, playerPos);
                        if (distance >= 3 && distance <= 6) {
                            String ropeDirection = MovementUtils.directionTo(myPos, playerPos);
                            if (ropeDirection != null && ropeDirection.equals(direction)) {
                                score += 100 - distance * 10; // Càng gần càng tốt
                            }
                        }
                    }
                }
                break;
                
            case "BELL":
                // Điểm cao cho việc confuse nhiều player
                Node checkPos = MovementUtils.moveTo(myPos, direction);
                int playerCount = 0;
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        Node playerPos = new Node(otherPlayer.x, otherPlayer.y);
                        int distance = MovementUtils.dist(checkPos, playerPos);
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
                        int distance = MovementUtils.dist(myPos, playerPos);
                        if (distance <= 5) {
                            String batDirection = MovementUtils.directionTo(myPos, playerPos);
                            if (batDirection != null && batDirection.equals(direction)) {
                                Node behindPlayer = MovementUtils.moveTo(playerPos, direction);
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