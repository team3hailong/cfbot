package jsclub.codefest.sdk.statbot;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import java.util.ArrayList;
import java.util.List;

public class MovementUtils {
    private static List<Node> recentPositions = new ArrayList<>();

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

    public static String directionTo(Node a, Node b) {
        if (a.x < b.x) return "r";
        if (a.x > b.x) return "l";
        if (a.y < b.y) return "u";
        if (a.y > b.y) return "d";
        return "";
    }

    /**
     * Kiểm tra một ô có phải obstacle gây stun không
     */
    public static boolean isStunObstacle(GameMap map, Node pos) {
        for (Obstacle obs : map.getListObstacles()) {
            if (obs.getX() == pos.x && obs.getY() == pos.y && obs.getTag() != null) {
                if (obs.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tìm obstacle phòng thủ (BUSH, POND, obstacle lớn) gần nhất
     */
    public static Node findNearestDefensiveObstacle(GameMap map, Node myPos) {
        List<Node> candidates = new ArrayList<>();
        for (Obstacle obs : map.getListObstacles()) {
            if (obs.getTag() != null && (
                obs.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.CAN_GO_THROUGH) ||
                "BUSH".equals(obs.getId()) ||
                obs.getHp() < 0 // indestructible obstacle
            )) {
                candidates.add(new Node(obs.getX(), obs.getY()));
            }
        }
        Node best = null;
        int minDist = Integer.MAX_VALUE;
        for (Node n : candidates) {
            int d = dist(myPos, n);
            if (d < minDist) {
                minDist = d;
                best = n;
            }
        }
        return best;
    }

    // Giữ lại các hàm cũ để tương thích ngược
    public static String findSmartDirection(Node myPos, GameMap map, Inventory inv) {
        return findOptimalDirection(myPos, map, inv, null, true);
    }
    
    public static String safeDirection(Node me, Node enemy, GameMap map) {
        return findOptimalDirection(me, map, null, enemy, false);
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
        
        // Lấy máu hiện tại
        float myHP = 100;
        if (map.getCurrentPlayer() != null && map.getCurrentPlayer().getHealth() != null) {
            myHP = map.getCurrentPlayer().getHealth();
        }
        
        // Kiểm tra có nhiều player gần không
        boolean isChased = false;
        int nearbyPlayers = 0;
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                if (dist(myPos, new Node(otherPlayer.x, otherPlayer.y)) <= 3) {
                    nearbyPlayers++;
                }
            }
        }
        if (nearbyPlayers >= 2) isChased = true;
        
        // Nếu máu yếu hoặc bị truy đuổi, ưu tiên hướng về obstacle phòng thủ
        if (myHP < 40 || isChased) {
            Node defObs = findNearestDefensiveObstacle(map, myPos);
            if (defObs != null) {
                String dirToObs = directionTo(myPos, defObs);
                Node nextPos = moveTo(myPos, dirToObs);
                boolean isSafe = true;
                for (Enemy enemy : map.getListEnemies()) {
                    if (dist(nextPos, new Node(enemy.x, enemy.y)) <= 2) { // tránh xa 2 ô
                        isSafe = false;
                        break;
                    }
                }
                for (Player otherPlayer : map.getOtherPlayerInfo()) {
                    if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                        if (dist(nextPos, new Node(otherPlayer.x, otherPlayer.y)) <= 2) {
                            isSafe = false;
                            break;
                        }
                    }
                }
                if (isSafe && !isStunObstacle(map, nextPos)) {
                    if (!recentPositions.contains(nextPos)) {
                        recentPositions.add(new Node(nextPos.x, nextPos.y));
                        if (recentPositions.size() > 10) recentPositions.remove(0);
                        return dirToObs;
                    }
                }
            }
        }
        
        // --- BỔ SUNG CHẠY VÒNG BO ---
        // Nếu đang ngoài bo, ưu tiên chạy vào bo
        int safeZone = map.getSafeZone();
        int mapSize = map.getMapSize();
        boolean myPosInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(myPos, safeZone, mapSize);
        int center = mapSize / 2;
        Node centerNode = new Node(center, center);
        boolean centerInSafe = jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(centerNode, safeZone, mapSize);
        
        // Nếu đang ngoài bo, ưu tiên chạy vào bo
        if (!myPosInSafe) {
            // Chỉ gọi getShortestPath nếu đích nằm trong bo
            if (centerInSafe) {
                String pathToBo = jsclub.codefest.sdk.algorithm.PathUtils.getShortestPath(map, new ArrayList<>(), myPos, centerNode, true); // skipDarkArea = true để cho phép vào bo
                if (pathToBo != null && !pathToBo.isEmpty()) {
                    String dir = String.valueOf(pathToBo.charAt(0));
                    Node nextPos = moveTo(myPos, dir);
                    boolean isSafe = true;
                    // Kiểm tra enemy gần
                    for (Enemy enemy : map.getListEnemies()) {
                        if (dist(nextPos, new Node(enemy.x, enemy.y)) <= 2) {
                            isSafe = false;
                            break;
                        }
                    }
                    // Kiểm tra player khác gần
                    for (Player otherPlayer : map.getOtherPlayerInfo()) {
                        if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                            if (dist(nextPos, new Node(otherPlayer.x, otherPlayer.y)) <= 2) {
                                isSafe = false;
                                break;
                            }
                        }
                    }
                    // Kiểm tra bẫy stun
                    if (isStunObstacle(map, nextPos)) isSafe = false;
                    if (isSafe && !recentPositions.contains(nextPos)) {
                        recentPositions.add(new Node(nextPos.x, nextPos.y));
                        if (recentPositions.size() > 10) recentPositions.remove(0);
                        System.out.println("BOT OUTSIDE SAFEZONE: moving " + dir + " to get inside safezone");
                        return dir;
                    }
                } else {
                    System.out.println("BOT OUTSIDE SAFEZONE: getShortestPath trả về null hoặc rỗng!");
                }
            }
            // Nếu không tìm được đường vào bo, chọn hướng bất kỳ vào gần bo nhất
            String bestDir = null;
            int minDist = Integer.MAX_VALUE;
            for (String dir : dirs) {
                Node nextPos = moveTo(myPos, dir);
                int distToSafe = jsclub.codefest.sdk.algorithm.PathUtils.distToSafeZone(nextPos, safeZone, mapSize);
                if (distToSafe < minDist) {
                    minDist = distToSafe;
                    bestDir = dir;
                }
            }
            if (bestDir != null) {
                System.out.println("BOT OUTSIDE SAFEZONE: fallback move " + bestDir + " to get closer to safezone");
                recentPositions.add(moveTo(myPos, bestDir));
                if (recentPositions.size() > 10) recentPositions.remove(0);
                return bestDir;
            }
        }
        
        // Xử lý từng hướng
        for (String dir : dirs) {
            Node nextPos = moveTo(myPos, dir);
            boolean isSafe = true;
            boolean hasItems = false;
            int escapeScore = 0;
            
            // Tránh enemy trong phạm vi 2 ô, nhận diện loại enemy
            for (Enemy enemy : map.getListEnemies()) {
                Node enemyPos = new Node(enemy.x, enemy.y);
                int d = dist(nextPos, enemyPos);
                String enemyId = enemy.getId() != null ? enemy.getId().toUpperCase() : "";
                if (enemyId.equals("SPIRIT")) {
                    // Nếu là SPIRIT và máu thấp, ưu tiên tiếp cận
                    if (myHP < 60 && d == 1) {
                        return dir;
                    }
                } else if (enemyId.equals("GOLEM") || enemyId.equals("RHINO") || enemyId.equals("ANACONDA") || enemyId.equals("LEOPARD") || enemyId.equals("NATIVE") || enemyId.equals("GHOST")) {
                    if (d <= 2) {
                        isSafe = false;
                        break;
                    }
                } else {
                    if (d <= 2) {
                        isSafe = false;
                        break;
                    }
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
            
            // Kiểm tra obstacle gây stun ở ô tiếp theo hoặc các ô liền kề
            if (isStunObstacle(map, nextPos)) {
                isSafe = false;
            } else {
                int[] dx = {0, 0, -1, 1};
                int[] dy = {-1, 1, 0, 0};
                for (int i = 0; i < 4; i++) {
                    Node adj = new Node(nextPos.x + dx[i], nextPos.y + dy[i]);
                    if (isStunObstacle(map, adj)) {
                        isSafe = false;
                        break;
                    }
                }
            }
            
            // Tránh lặp lại vị trí cũ
            if (recentPositions.contains(nextPos)) isSafe = false;
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
            recentPositions.add(moveTo(myPos, bestEscapeDir));
            if (recentPositions.size() > 10) recentPositions.remove(0);
            return bestEscapeDir;
        }
        
        // 2. Nếu ưu tiên vật phẩm và có hướng có vật phẩm
        if (prioritizeItems && !itemDirs.isEmpty()) {
            String dir = itemDirs.get((int)(Math.random() * itemDirs.size()));
            recentPositions.add(moveTo(myPos, dir));
            if (recentPositions.size() > 10) recentPositions.remove(0);
            return dir;
        }
        
        // 3. Chọn hướng an toàn ngẫu nhiên
        if (!safeDirs.isEmpty()) {
            String dir = safeDirs.get((int)(Math.random() * safeDirs.size()));
            recentPositions.add(moveTo(myPos, dir));
            if (recentPositions.size() > 10) recentPositions.remove(0);
            return dir;
        }
        
        // 4. Fallback: chọn hướng ra xa enemy nhất nếu không còn hướng an toàn
        int maxDist = -1;
        String bestDir = "u";
        for (String dir : dirs) {
            Node nextPos = moveTo(myPos, dir);
            // BỔ SUNG: Không chọn hướng đi vào bẫy stun khi fallback
            if (isStunObstacle(map, nextPos)) continue;
            int minDistToEnemy = Integer.MAX_VALUE;
            for (Enemy enemy : map.getListEnemies()) {
                int d = dist(nextPos, new Node(enemy.x, enemy.y));
                if (d < minDistToEnemy) minDistToEnemy = d;
            }
            if (minDistToEnemy > maxDist) {
                maxDist = minDistToEnemy;
                bestDir = dir;
            }
        }
        recentPositions.add(moveTo(myPos, bestDir));
        if (recentPositions.size() > 10) recentPositions.remove(0);
        return bestDir;
    }

    /**
     * Tìm hướng di chuyển thông minh với dự đoán enemy
     * @param myPos vị trí hiện tại
     * @param map bản đồ game
     * @param inv inventory
     * @return hướng di chuyển tối ưu
     */
    public static String findSmartDirectionWithEnemyPrediction(Node myPos, GameMap map, Inventory inv) {
        String[] dirs = {"u", "d", "l", "r"};
        List<String> safeDirs = new ArrayList<>();
        List<String> itemDirs = new ArrayList<>();
        List<String> escapeDirs = new ArrayList<>();
        
        // Lấy máu hiện tại
        float myHP = 100;
        if (map.getCurrentPlayer() != null && map.getCurrentPlayer().getHealth() != null) {
            myHP = map.getCurrentPlayer().getHealth();
        }
        
        // Kiểm tra có nhiều player gần không
        boolean isChased = false;
        int nearbyPlayers = 0;
        for (Player otherPlayer : map.getOtherPlayerInfo()) {
            if (otherPlayer.getHealth() != null && otherPlayer.getHealth() > 0) {
                if (dist(myPos, new Node(otherPlayer.x, otherPlayer.y)) <= 3) {
                    nearbyPlayers++;
                }
            }
        }
        if (nearbyPlayers >= 2) isChased = true;
        
        // Nếu máu yếu hoặc bị truy đuổi, ưu tiên hướng về obstacle phòng thủ
        if (myHP < 40 || isChased) {
            Node defensivePos = findNearestDefensiveObstacle(map, myPos);
            if (defensivePos != null) {
                String dir = directionTo(myPos, defensivePos);
                if (dir != null && !dir.isEmpty()) {
                    Node nextPos = moveTo(myPos, dir);
                    if (!isStunObstacle(map, nextPos)) {
                        return dir;
                    }
                }
            }
        }
        
        // Kiểm tra từng hướng di chuyển
        for (String dir : dirs) {
            Node nextPos = moveTo(myPos, dir);
            boolean isSafe = true;
            boolean hasItems = false;
            int escapeScore = 0;
            
            // === CẢI THIỆN: KIỂM TRA ENEMY HIỆN TẠI VÀ DỰ ĐOÁN ===
            for (Enemy enemy : map.getListEnemies()) {
                Node enemyPos = new Node(enemy.x, enemy.y);
                int d = dist(nextPos, enemyPos);
                String enemyId = enemy.getId() != null ? enemy.getId().toUpperCase() : "";
                
                // Kiểm tra enemy hiện tại
                if (enemyId.equals("SPIRIT")) {
                    if (myHP < 60 && d == 1) {
                        return dir; // Tiếp cận SPIRIT khi máu thấp
                    }
                } else if (enemyId.equals("GOLEM") || enemyId.equals("RHINO") || enemyId.equals("ANACONDA") || enemyId.equals("LEOPARD") || enemyId.equals("NATIVE") || enemyId.equals("GHOST")) {
                    if (d <= 2) {
                        isSafe = false;
                        break;
                    }
                } else {
                    if (d <= 2) {
                        isSafe = false;
                        break;
                    }
                }
                
                // === DỰ ĐOÁN ENEMY TRONG TƯƠNG LAI ===
                Node predictedPos = enemyPos;
                for (int turn = 1; turn <= 2; turn++) {
                    predictedPos = StatBotUtils.predictEnemyNextPosition(enemy, map, myPos);
                    int predictedDistance = dist(nextPos, predictedPos);
                    
                    // Nếu vị trí dự đoán sẽ bị enemy đe dọa, đánh dấu không an toàn
                    int dangerRange = 2;
                    switch (enemyId) {
                        case "SPIRIT":
                            dangerRange = 1;
                            break;
                        case "LEOPARD":
                        case "NATIVE":
                            dangerRange = 3;
                            break;
                    }
                    
                    if (predictedDistance <= dangerRange) {
                        isSafe = false;
                        break;
                    }
                }
                
                if (!isSafe) break;
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
            for (Obstacle obs : map.getListObstacles()) {
                if (obs.getX() == nextPos.x && obs.getY() == nextPos.y && obs.getTag() != null) {
                    if (obs.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                        hasStunTrap = true;
                        break;
                    }
                }
            }
            
            if (hasStunTrap) {
                isSafe = false;
            }
            
            // Kiểm tra có vật phẩm gần không
            if (inv != null) {
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
            
            // Tính điểm an toàn cho hướng này
            int safetyScore = StatBotUtils.calculatePositionSafetyScore(nextPos, map, myPos);
            
            if (isSafe) {
                safeDirs.add(dir);
                if (hasItems) {
                    itemDirs.add(dir);
                }
                if (safetyScore > 80) {
                    escapeDirs.add(dir);
                }
            }
        }
        
        // Logic ưu tiên:
        // 1. Nếu có hướng an toàn cao, ưu tiên hướng đó
        if (!escapeDirs.isEmpty()) {
            String bestEscapeDir = escapeDirs.get(0);
            int maxSafetyScore = 0;
            for (String dir : escapeDirs) {
                Node nextPos = moveTo(myPos, dir);
                int safetyScore = StatBotUtils.calculatePositionSafetyScore(nextPos, map, myPos);
                if (safetyScore > maxSafetyScore) {
                    maxSafetyScore = safetyScore;
                    bestEscapeDir = dir;
                }
            }
            recentPositions.add(moveTo(myPos, bestEscapeDir));
            if (recentPositions.size() > 10) recentPositions.remove(0);
            return bestEscapeDir;
        }
        
        // 2. Nếu ưu tiên vật phẩm và có hướng có vật phẩm
        if (!itemDirs.isEmpty()) {
            String dir = itemDirs.get((int)(Math.random() * itemDirs.size()));
            recentPositions.add(moveTo(myPos, dir));
            if (recentPositions.size() > 10) recentPositions.remove(0);
            return dir;
        }
        
        // 3. Chọn hướng an toàn ngẫu nhiên
        if (!safeDirs.isEmpty()) {
            String dir = safeDirs.get((int)(Math.random() * safeDirs.size()));
            recentPositions.add(moveTo(myPos, dir));
            if (recentPositions.size() > 10) recentPositions.remove(0);
            return dir;
        }
        
        // 4. Fallback: chọn hướng ra xa enemy nhất nếu không còn hướng an toàn
        int maxDist = -1;
        String bestDir = "u";
        for (String dir : dirs) {
            Node nextPos = moveTo(myPos, dir);
            if (isStunObstacle(map, nextPos)) continue;
            
            int minDistToEnemy = Integer.MAX_VALUE;
            for (Enemy enemy : map.getListEnemies()) {
                int d = dist(nextPos, new Node(enemy.x, enemy.y));
                if (d < minDistToEnemy) minDistToEnemy = d;
            }
            
            if (minDistToEnemy > maxDist) {
                maxDist = minDistToEnemy;
                bestDir = dir;
            }
        }
        
        recentPositions.add(moveTo(myPos, bestDir));
        if (recentPositions.size() > 10) recentPositions.remove(0);
        return bestDir;
    }
} 