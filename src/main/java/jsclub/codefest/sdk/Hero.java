package jsclub.codefest.sdk;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.factory.HealingItemFactory;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.effects.Effect;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.socket.EventName;
import jsclub.codefest.sdk.socket.SocketClient;
import jsclub.codefest.sdk.socket.data.emit_data.*;
import jsclub.codefest.sdk.util.MsgPackUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.weapon.Weapon;

public class Hero {
    private String playerName = "";
    private String gameID = "";
    private String secretKey = "";
    private final SocketClient socketClient;
    private final GameMap gameMap;
    private final Inventory inventory;
    private final List<Effect> effects;
    private Emitter.Listener onMapUpdate;

    public Hero(String gameID, String playerName, String secretKey) {
        this.playerName = playerName;
        this.gameID = gameID;
        this.secretKey = secretKey;
        this.inventory = new Inventory();
        this.effects = new ArrayList<>();
        this.gameMap = new GameMap(this.getInventory(), this.getEffects());
        this.socketClient = new SocketClient(this.inventory, this.effects, this.gameMap);
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public void start(String serverURL) throws IOException {
        if (this.onMapUpdate == null) {
            throw new RuntimeException("onMapUpdate is not set");
        }

        if (this.playerName.isEmpty()) {
            throw new RuntimeException("playerName is not set");
        }

        if (this.gameID.isEmpty()) {
            throw new RuntimeException("gameID is not set");
        }

        socketClient.connectToServer(serverURL, playerName, secretKey, onMapUpdate)
                .thenRun(this::joinGame)
                .exceptionally(ex -> {
                    System.err.println("Failed to connect and join game: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    public String getPlayerID() {
        return playerName;
    }

    public String getGameID() {
        return gameID;
    }

    public void joinGame() {
        Socket socket = socketClient.getSocket();

        if (socket != null) {
            try {
                PlayerJoinGameAction joinGame = new PlayerJoinGameAction(this.gameID);
                byte[] bytes = MsgPackUtil.encodeFromObject(joinGame);
                socket.emit(EventName.EMIT_JOIN_GAME, (Object) bytes);
            } catch (IOException e) {
                e.printStackTrace(); // or handle more gracefully
            }
        }
    }


    private boolean invalidDirection(String direction) {
        if (direction == null) {
            return true;
        }

        int dirLength = direction.length();
        if (dirLength > 0) {
            for (int i = 0; i < direction.length(); i++) {
                char ch = direction.charAt(i);
                if (ch != 'u' && ch != 'd' && ch != 'l' && ch != 'r') {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Moves the player in the specified direction ('l', 'r', 'u', 'd').
     *
     * @param direction the direction in which to move the player
     * @throws IOException if an I/O error occurs
     */
    public void move(String direction) throws IOException {
        Socket socket = socketClient.getSocket();

        if (invalidDirection(direction)) {
            System.out.println("DEBUG FROM SDK move ERROR : Invalid direction");
            return;
        }

        if (socket != null) {
            PlayerMoveAction botMove = new PlayerMoveAction(direction);
            byte[] bytes = MsgPackUtil.encodeFromObject(botMove);
            socket.emit(EventName.EMIT_MOVE, (Object) bytes);
        } else {
            System.out.println("DEBUG FROM SDK move ERROR : Socket is null");
        }
    }


    /**
     * Shoots a projectile in the specified direction ('l', 'r', 'u', 'd').
     *
     * @param direction the direction in which to shoot
     * @throws IOException if an I/O error occurs
     */
    public void useSpecial(String direction) throws IOException{
        Socket socket = socketClient.getSocket();

        if (direction.isEmpty()) {
            System.out.println("DEBUG FROM SDK shoot ERROR : direction is null or empty");
            return;
        }

        if (direction.length() != 1) {
            System.out.println("DEBUG FROM SDK shoot ERROR : direction string length must be 1");
            return;
        }

        if (invalidDirection(direction)) {
            System.out.println("DEBUG FROM SDK useSpecial ERROR : Invalid direction");
            return;
        }

        if (socket == null) {
            System.out.println("DEBUG FROM SDK useSpecial ERROR : Socket is null or inventory does not have special weapons");
            return;
        }

        PlayerUseSpecialAction botUseSpecial = new PlayerUseSpecialAction(direction);
        byte[] bytes = MsgPackUtil.encodeFromObject(botUseSpecial);
        socket.emit(EventName.EMIT_USE_SPECIAL, (Object)bytes);

    }

    public void shoot(String direction) throws IOException {
        Socket socket = socketClient.getSocket();

        if (direction.isEmpty() || direction == null) {
            System.out.println("DEBUG FROM SDK shoot ERROR : direction is null or empty");
            return;
        }

        if (direction.length() != 1) {
            System.out.println("DEBUG FROM SDK shoot ERROR : direction string length must be 1");
            return;
        }

        if (invalidDirection(direction)) {
            System.out.println("DEBUG FROM SDK shoot ERROR : Invalid direction");
            return;
        }

        if (socket == null || getInventory().getGun() == null) {
            System.out.println("DEBUG FROM SDK shoot ERROR : Socket is null or inventory does not have gun");
            return;
        }

        PlayerShootAction botShoot = new PlayerShootAction(direction);
        byte[] bytes = MsgPackUtil.encodeFromObject(botShoot);
        socket.emit(EventName.EMIT_SHOOT, (Object) bytes);

    }


    /**
     * Performs a melee attack in the specified direction ('l', 'r', 'u', 'd').
     *
     * @param direction the direction in which to attack
     * @throws IOException if an I/O error occurs
     */
    public void attack(String direction) throws IOException {
        Socket socket = socketClient.getSocket();

        if (direction.isEmpty() || direction == null) {
            System.out.println("DEBUG FROM SDK attack ERROR : direction is null or empty");
            return;
        }

        if (direction.length() != 1) {
            System.out.println("DEBUG FROM SDK attack ERROR : direction string length must be 1");
            return;
        }

        if (invalidDirection(direction)) {
            System.out.println("DEBUG FROM SDK attack ERROR : Invalid direction");
            return;
        }

        if (socket == null) {
            System.out.println("DEBUG FROM SDK shoot ERROR : Socket is null");
            return;
        }

        PlayerAttackAction botAttack = new PlayerAttackAction(direction);
        byte[] bytes = MsgPackUtil.encodeFromObject(botAttack);
        socket.emit(EventName.EMIT_ATTACK, (Object) bytes);
    }

    /**
     * Throws a throwable object in the specified direction ('l', 'r', 'u', 'd').
     *
     * @param direction the direction in which to throw the object
     * @throws IOException if an I/O error occurs
     */
    public void throwItem(String direction, int distance) throws IOException {
        Socket socket = socketClient.getSocket();

        if (direction.isEmpty() || direction == null) {
            System.out.println("DEBUG FROM SDK throwItem ERROR : direction is null or empty");
            return;
        }
        if (direction.length() != 1) {
            System.out.println("DEBUG FROM SDK throwItem ERROR : direction string length must be 1");
            return;
        }
        if (invalidDirection(direction)) {
            System.out.println("DEBUG FROM SDK throwItem ERROR : Invalid direction");
            return;
        }

        if (socket == null || getInventory().getThrowable() == null) {
            System.out.println("DEBUG FROM SDK throwItem ERROR : Socket is null or inventory does not have throwable");
            return;
        }

        PlayerThrowItemAction botThrow = new PlayerThrowItemAction(direction,distance);
        byte[] bytes = MsgPackUtil.encodeFromObject(botThrow);
        socket.emit(EventName.EMIT_THROW, (Object) bytes);
    }


    /**
     * Picks up an item at the player's current position.
     *
     * @throws IOException if an I/O error occurs
     */
    public void pickupItem() throws IOException {
        Socket socket = socketClient.getSocket();

        Node currentPos = new Node(getGameMap().getCurrentPlayer().x, getGameMap().getCurrentPlayer().y);
        boolean hasItem = hasItem(currentPos.x, currentPos.y);

        System.out.println("hasItem:"+hasItem);

        if (socket == null || !hasItem) {
            System.out.println("DEBUG FROM SDK pickupItem ERROR : Socket is null or current position does not have item");
            return;
        }

        String data = "{}";
        byte[] bytes = MsgPackUtil.encodeFromObject(data);
        socket.emit(EventName.EMIT_PICKUP_ITEM, (Object) bytes);
    }

    private boolean hasItem(int x, int y) {
        List<Node> listItem = new ArrayList<>();
        listItem.addAll(getGameMap().getListHealingItems());
        listItem.addAll(getGameMap().getAllGun());
        listItem.addAll(getGameMap().getAllMelee());
        listItem.addAll(getGameMap().getAllThrowable());
        listItem.addAll(getGameMap().getAllSpecial());
        listItem.addAll(getGameMap().getListArmors());
        listItem.addAll(getGameMap().getAllSpecial());

        boolean hasItem = false;

        for (Node item : listItem) {
            if (item.x == x && item.y == y) {
                hasItem = true;
                break;
            }
        }
        return hasItem;
    }

    /**
     * Uses an item with the specified ID.
     *
     * @param itemId the ID of the item to use
     * @throws IOException if an I/O error occurs
     */


    public void useItem(String itemId) throws IOException {
        Socket socket = socketClient.getSocket();
        HealingItem item = HealingItemFactory.getHealingItemById(itemId);
        int indexOfItem = getInventory().getListHealingItem().indexOf(item);

        if (itemId.isEmpty() || itemId == null) {
            System.out.println("DEBUG FROM SDK useItem ERROR : itemId is null or empty");
            return;
        }
        if (indexOfItem == -1) {
            System.out.println("DEBUG FROM SDK useItem ERROR : Inventory does not have " + item.getId());
            return;
        }

        if (socket == null || getInventory().getListHealingItem().get(indexOfItem) == null) {
            System.out.println("DEBUG FROM SDK useItem ERROR : Socket is null or cannot get item");
            return;
        }

        List<HealingItem> inventAfter = inventory.getListHealingItem();
        inventAfter.remove(indexOfItem);
        inventory.setListHealingItem(inventAfter);
        PlayerUseItemAction botUseItem = new PlayerUseItemAction(itemId);

        byte[] bytes = MsgPackUtil.encodeFromObject(botUseItem);
        socket.emit(EventName.EMIT_USE_ITEM, (Object) bytes);
    }

    /**
     * Revokes an item with the specified ID.
     *
     * @param itemId the ID of the item to revoke
     * @throws IOException if an I/O error occurs
     */
    public void revokeItem(String itemId) throws IOException {
        Socket socket = socketClient.getSocket();

        if (itemId.isEmpty() || itemId == null) {
            System.out.println("DEBUG FROM SDK revokeItem ERROR : itemId is null or empty");
            return;
        }

        if(socket == null){
            System.out.println("DEBUG FROM SDK revokeItem ERROR : Socket is null");
            return;
        }

        PlayerRevokeItemAction botRevokeItem = new PlayerRevokeItemAction(itemId);
        byte[] bytes = MsgPackUtil.encodeFromObject(botRevokeItem);
        socket.emit(EventName.EMIT_REVOKE_ITEM, (Object) bytes);
    }

    public String getPlayerName() {
        return playerName;
    }

    /**
     * Retrieves the current game map information.
     *
     * @return the current game map
     */
    public GameMap getGameMap() {
        return gameMap;
    }

    public void setOnMapUpdate(Emitter.Listener onMapUpdate) {
        this.onMapUpdate = onMapUpdate;
    }

    /**
     * Retrieves the player's inventory information.
     *
     * @return the player's inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    public static void main(String[] args) {
        // Nhập trực tiếp các thông tin cần thiết
        String secretKey = "sk-_UgcHnbHQACXmdeS-1263w:ZcELGs3FRAvZSBLpHoYD6KN6ylSqmx0yBZB-grJUsZYBQhUNmmtY3E62vFtWOEyRX3JCwXpRy6d4IzqcJ3LCFg"; // <-- Thay bằng secretKey thật
        String gameID = "178994";      // <-- Thay bằng gameID thật
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
                if (hero.isStunned()) {
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
                    HealingItem bestHeal = getBestHealingItem(inv.getListHealingItem());
                    if (bestHeal != null) {
                        System.out.println("Sử dụng healing item: " + bestHeal.getId());
                        hero.useItem(bestHeal.getId());
                        return;
                    } else {
                        System.out.println("Không tìm thấy healing item phù hợp để dùng");
                    }
                }

                // 2. Xử lý ENEMY thông minh - chỉ né khi thực sự nguy hiểm
                List<Enemy> enemies = map.getListEnemies();
                System.out.println("Enemy list: " + enemies.size());
                boolean isInDanger = false;
                for (Enemy e : enemies) {
                    Node enemyPos = new Node(e.x, e.y);
                    int enemyDist = dist(myPos, enemyPos);
                    if (enemyDist <= 1 && me.getHealth() < 25) {
                        isInDanger = true;
                        break;
                    }
                }
                if (isInDanger) {
                    System.out.println("Bot đang gặp nguy hiểm, sẽ né tránh");
                    for (Enemy e : enemies) {
                        Node enemyPos = new Node(e.x, e.y);
                        if (dist(myPos, enemyPos) <= 1) {
                            String safeDir = safeDirection(myPos, enemyPos, map);
                            System.out.println("Di chuyển né enemy về hướng: " + safeDir);
                            hero.move(safeDir);
                            return;
                        }
                    }
                }

                // 4. Combat với player khác (ưu tiên cao hơn items)
                List<Player> others = map.getOtherPlayerInfo();
                System.out.println("Other players: " + others.size());
                for (Player p : others) {
                    if (p.getHealth() != null && p.getHealth() > 0) {
                        Node opp = new Node(p.x, p.y);
                        int distance = dist(myPos, opp);
                        int myStrength = evaluateStrength(me, inv);
                        int oppStrength = evaluateStrength(p, null); // Không có inventory đối thủ, chỉ dựa vào máu và vũ khí cơ bản
                        System.out.println("So sánh sức mạnh: mình=" + myStrength + ", đối thủ=" + oppStrength);
                        if (distance == 1) {
                            if (myStrength > oppStrength || p.getHealth() < 20) {
                                System.out.println("Mạnh hơn hoặc đối thủ yếu, sẽ tấn công hoặc bắn");
                                if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                                    System.out.println("Tấn công bằng melee: " + inv.getMelee().getId());
                                    hero.attack(directionTo(myPos, opp));
                                    return;
                                } else if (inv.getGun() != null) {
                                    System.out.println("Bắn bằng súng: " + inv.getGun().getId());
                                    hero.shoot(directionTo(myPos, opp));
                                    return;
                                }
                            } else if (myStrength < oppStrength) {
                                System.out.println("Đối thủ mạnh hơn, ưu tiên né tránh");
                                String safeDir = safeDirection(myPos, opp, map);
                                hero.move(safeDir);
                                return;
                            } else {
                                System.out.println("Sức mạnh ngang cơ, cân nhắc tấn công nếu máu mình còn nhiều");
                                if (me.getHealth() > 40) {
                                    if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                                        hero.attack(directionTo(myPos, opp));
                                        return;
                                    } else if (inv.getGun() != null) {
                                        hero.shoot(directionTo(myPos, opp));
                                        return;
                                    }
                                } else {
                                    // Nếu máu thấp, ưu tiên hồi máu hoặc né
                                    if (!inv.getListHealingItem().isEmpty()) {
                                        HealingItem bestHeal = getBestHealingItem(inv.getListHealingItem());
                                        if (bestHeal != null) {
                                            hero.useItem(bestHeal.getId());
                                            return;
                                        }
                                    }
                                    String safeDir = safeDirection(myPos, opp, map);
                                    hero.move(safeDir);
                                    return;
                                }
                            }
                        } else if (distance <= 6 && inv.getGun() != null) {
                            if (myStrength >= oppStrength || p.getHealth() < 20) {
                                System.out.println("Bắn player ở xa bằng súng: " + inv.getGun().getId());
                                hero.shoot(directionTo(myPos, opp));
                                return;
                            }
                        } else if (distance <= 6 && inv.getThrowable() != null) {
                            if (myStrength >= oppStrength || p.getHealth() < 20) {
                                System.out.println("Ném vật phẩm vào player ở xa: " + inv.getThrowable().getId());
                                hero.throwItem(directionTo(myPos, opp), distance);
                                return;
                            }
                        }
                    }
                }

                // 5. Quản lý inventory thông minh và nhặt items (ưu tiên cao)
                Node bestItem = findBestItemToPickup(map, inv, myPos);
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
                            String gunDir = getGunDirectionToChest(myPos, bestItem, inv.getGun());
                            if (gunDir != null) {
                                System.out.println("Bắn rương từ xa bằng súng " + inv.getGun().getId() + " hướng: " + gunDir + ", tầm: " + inv.getGun().getRange());
                                hero.shoot(gunDir);
                                return;
                            }
                        }
                        // Ưu tiên tấn công rương từ xa bằng Melee nếu có thể
                        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                            String meleeDir = getMeleeDirectionToChest(myPos, bestItem, inv.getMelee());
                            if (meleeDir != null) {
                                System.out.println("Tấn công rương từ xa bằng melee " + inv.getMelee().getId() + " hướng: " + meleeDir + ", độ rộng: " + inv.getMelee().getRange());
                                hero.attack(meleeDir);
                                return;
                            }
                        }
                        // Nếu không bắn/tấn công được từ xa thì xử lý như cũ
                        if (dist(myPos, bestItem) == 1) {
                            String dir = directionTo(myPos, bestItem);
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
                                    String escapeDir = findSmartDirection(myPos, map, inv);
                                    System.out.println("Inventory đầy, không nhặt được item, di chuyển sang hướng: " + escapeDir);
                                    hero.move(escapeDir);
                                }
                                return;
                            }
                            if (path.length() == 1) {
                                System.out.println("Sắp nhặt item, kiểm tra/revoke inventory nếu cần");
                                manageInventoryBeforePickup(map, inv, bestItem, hero);
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
                        Integer.compare(dist(a, myPos), dist(b, myPos))
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
                String smartDir = findSmartDirection(myPos, map, inv);
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

    // Hàm tính khoảng cách Manhattan
    private static int dist(Node a, Node b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }
    // Hàm xác định hướng di chuyển từ a đến b
    private static String directionTo(Node a, Node b) {
        if (a.x < b.x) return "r";
        if (a.x > b.x) return "l";
        if (a.y < b.y) return "u";
        if (a.y > b.y) return "d";
        return "";
    }
    // Hàm chọn hướng an toàn tránh enemy
    private static String safeDirection(Node me, Node enemy, GameMap map) {
        // Ưu tiên di chuyển xa enemy nhất
        String[] dirs = {"u", "d", "l", "r"};
        int maxDist = -1;
        String bestDir = "u";
        for (String dir : dirs) {
            Node nextPos = moveTo(me, dir);
            int d = dist(nextPos, enemy);
            if (d > maxDist) {
                maxDist = d;
                bestDir = dir;
            }
        }
        return bestDir;
    }
    // Hàm trả về node mới sau khi di chuyển 1 bước
    private static Node moveTo(Node a, String dir) {
        switch (dir) {
            case "u": return new Node(a.x-1, a.y);
            case "d": return new Node(a.x+1, a.y);
            case "l": return new Node(a.x, a.y-1);
            case "r": return new Node(a.x, a.y+1);
        }
        return a;
    }
    
    // Helper methods cho việc quản lý items và inventory
    
    // Tính điểm ưu tiên cho các items dựa trên pickup points và tính hữu ích
    private static int getItemPriority(String itemType, String itemId, Inventory currentInv) {
        // Điểm pickup cơ bản từ game design
        int basePoints = getBasePickupPoints(itemId);
        
        // Bonus cho items đặc biệt quan trọng
        if ("ELIXIR_OF_LIFE".equals(itemId)) return basePoints + 1000; // Vật phẩm hồi sinh
        if ("COMPASS".equals(itemId)) return basePoints + 500; // La bàn stun AoE
        if ("MAGIC_ARMOR".equals(itemId)) return basePoints + 300; // Giáp tốt nhất
        if ("MAGIC_HELMET".equals(itemId)) return basePoints + 200; // Mũ tốt nhất
        
        // Bonus cho vũ khí mạnh
        if ("SAHUR_BAT".equals(itemId) || "BELL".equals(itemId) || "ROPE".equals(itemId)) {
            return basePoints + 200; // Special weapons
        }
        if ("SHOTGUN".equals(itemId) || "MACE".equals(itemId)) return basePoints + 150;
        if ("AXE".equals(itemId) || "KNIFE".equals(itemId)) return basePoints + 100;
        
        // Penalty nếu đã có item loại này tốt hơn
        if ("weapon".equals(itemType) && hasCurrentWeapon(currentInv, itemId)) {
            return basePoints - 50;
        }
        
        return basePoints;
    }
    
    private static int getBasePickupPoints(String itemId) {
        // Mapping từ game design document
        switch (itemId) {
            case "KNIFE": return 55;
            case "TREE_BRANCH": return 35;
            case "HAND": return 0;
            case "BONE": return 45;
            case "AXE": return 45;
            case "SCEPTER": return 40;
            case "CROSSBOW": return 35;
            case "RUBBER_GUN": return 50;
            case "SHOTGUN": return 20;
            case "BANANA": return 30;
            case "SMOKE": return 50;
            case "METEORITE_FRAGMENT": return 25;
            case "CRYSTAL": return 40;
            case "SEED": return 55;
            case "MACE": return 60;
            case "ROPE": return 80;
            case "BELL": return 70;
            case "SAHUR_BAT": return 70;
            case "GOD_LEAF": return 5;
            case "SPIRIT_TEAR": return 15;
            case "MERMAID_TAIL": return 20;
            case "PHOENIX_FEATHERS": return 25;
            case "UNICORN_BLOOD": return 30;
            case "ELIXIR": return 30;
            case "MAGIC": return 30;
            case "ELIXIR_OF_LIFE": return 30;
            case "COMPASS": return 60;
            case "WOODEN_HELMET": return 40;
            case "ARMOR": return 50;
            case "MAGIC_HELMET": return 60;
            case "MAGIC_ARMOR": return 80;
            default: return 10;
        }
    }
    
    private static boolean hasCurrentWeapon(Inventory inv, String newItemId) {
        if (inv.getMelee() != null && !inv.getMelee().getId().equals("HAND")) {
            return getBasePickupPoints(inv.getMelee().getId()) >= getBasePickupPoints(newItemId);
        }
        if (inv.getGun() != null) {
            return getBasePickupPoints(inv.getGun().getId()) >= getBasePickupPoints(newItemId);
        }
        return false;
    }
    
    private static Node findBestItemToPickup(GameMap map, Inventory inv, Node myPos) {
        List<Node> allItems = new ArrayList<>();
        allItems.addAll(map.getAllGun());
        allItems.addAll(map.getAllMelee());
        allItems.addAll(map.getAllThrowable());
        allItems.addAll(map.getAllSpecial());
        allItems.addAll(map.getListArmors());
        allItems.addAll(map.getListHealingItems());
        allItems.addAll(map.getListChests()); // Giả sử rương cũng được coi là item

        if (allItems.isEmpty()) return null;

        Player me = map.getCurrentPlayer();
        float myHP = (me != null && me.getHealth() != null) ?  me.getHealth() : 100;
        boolean isDanger = false;
        for (Enemy e : map.getListEnemies()) {
            if (dist(myPos, new Node(e.x, e.y)) <= 1) {
                isDanger = true;
                break;
            }
        }

        Node best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Node item : allItems) {
            double score = 0;
            String itemId = getItemIdByPosition(map, item);
            String itemType = getItemTypeByPosition(map, item);
            int basePriority = getItemPriority(itemType, itemId, inv);
            int distance = dist(myPos, item);

            // 1. Ưu tiên vật phẩm cơ bản
            score += basePriority;
            // 2. Trừ điểm theo khoảng cách
            score -= distance * 2;
            // 3. Bonus nếu vật phẩm tốt hơn cái đang giữ
            if (itemType.equals("weapon")) {
                if (!hasCurrentWeapon(inv, itemId)) score += 30;
                else score -= 50;
            }
            if (itemType.equals("armor")) {
                if (isBetterArmor(inv, itemId)) score += 40;
                else score -= 40;
            }
            if (itemType.equals("helmet")) {
                if (isBetterHelmet(inv, itemId)) score += 30;
                else score -= 30;
            }
            // 4. Bonus nếu inventory còn slot trống
            if (itemType.equals("healing")){
                if(inv.getListHealingItem().size() < 4) score += 20;
                if(inv.getListHealingItem().size() < 2) score += 80;
                if(myHP < 50) score += 40;
                if(myHP < 30) score += 60;
            } 
            // 6. Nếu đang nguy hiểm, ưu tiên utility (stun, tàng hình, hồi máu)
            if (isDanger && (itemId.equals("COMPASS") || itemId.equals("ELIXIR") || itemId.equals("MAGIC") || itemType.equals("healing"))) score += 50;
            // 7. Nếu là rương, tính xác suất ra đồ xịn và inventory chưa tối ưu
            if (isChest(map, item)) {
                double chestValue = expectedChestValue(inv);
                score += chestValue;
                // Nếu có thể tấn công rương ngay (có vũ khí mạnh)
                if (canAttackChest(inv)) score += 30;
                // Nếu inventory đã tối ưu, giảm điểm ưu tiên rương
                if (isInventoryOptimal(inv)) score -= 50;
            }
            // 8. Nếu item là DRAGON_EGG, bonus lớn hơn CHEST
            if (isDragonEgg(map, item)) score += 80;
            // 9. Nếu inventory đã có vật phẩm tốt nhất loại đó, trừ điểm mạnh cho các vật phẩm cùng loại yếu hơn
            if (isInventoryOptimal(inv) && !isChest(map, item)) score -= 30;

            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
        }
        return best;
    }

    // Helper xác định loại và id vật phẩm từ vị trí
    private static String getItemIdByPosition(GameMap map, Node itemPos) {
        if (map.getAllGun().contains(itemPos)) return "SHOTGUN";
        if (map.getAllMelee().contains(itemPos)) return "MACE";
        if (map.getAllSpecial().contains(itemPos)) return "BELL";
        if (map.getListArmors().contains(itemPos)) return "MAGIC_ARMOR";
        if (map.getListHealingItems().contains(itemPos)) return "UNICORN_BLOOD";
        if (isDragonEgg(map, itemPos)) return "DRAGON_EGG";
        if (isChest(map, itemPos)) return "CHEST";
        return "UNKNOWN";
    }
    private static String getItemTypeByPosition(GameMap map, Node itemPos) {
        if (map.getAllGun().contains(itemPos) || map.getAllMelee().contains(itemPos) || map.getAllSpecial().contains(itemPos) || map.getAllThrowable().contains(itemPos)) return "weapon";
        if (map.getListArmors().contains(itemPos)) return "armor";
        if (map.getListHealingItems().contains(itemPos)) return "healing";
        if (isChest(map, itemPos) || isDragonEgg(map, itemPos)) return "chest";
        return "unknown";
    }
    private static boolean isChest(GameMap map, Node pos) {
        return map.getListChests().stream().anyMatch(chest -> chest.x == pos.x && chest.y == pos.y);
    }
    private static boolean isDragonEgg(GameMap map, Node pos) {
        // Giả sử DRAGON_EGG là rương đặc biệt, có thể cần cập nhật lại nếu có field riêng
        return false; // Cập nhật nếu có field riêng cho DRAGON_EGG
    }
    private static boolean isBetterArmor(Inventory inv, String newId) {
        if (inv.getArmor() == null) return true;
        return getBasePickupPoints(newId) > getBasePickupPoints(inv.getArmor().getId());
    }
    private static boolean isBetterHelmet(Inventory inv, String newId) {
        if (inv.getHelmet() == null) return true;
        return getBasePickupPoints(newId) > getBasePickupPoints(inv.getHelmet().getId());
    }
    private static boolean canAttackChest(Inventory inv) {
        // Có vũ khí mạnh (gun hoặc melee tốt)
        if (inv.getGun() != null || (inv.getMelee() != null && !inv.getMelee().getId().equals("HAND"))) return true;
        return false;
    }
    private static boolean isInventoryOptimal(Inventory inv) {
        // Đã có các vật phẩm tốt nhất (giáp, mũ, vũ khí, healing xịn)
        boolean armor = inv.getArmor() != null && inv.getArmor().getId().equals("MAGIC_ARMOR");
        boolean helmet = inv.getHelmet() != null && inv.getHelmet().getId().equals("MAGIC_HELMET");
        boolean gun = inv.getGun() != null && inv.getGun().getId().equals("SHOTGUN");
        boolean melee = inv.getMelee() != null && inv.getMelee().getId().equals("MACE");
        boolean healing = inv.getListHealingItem().stream().anyMatch(h -> h.getId().equals("ELIXIR_OF_LIFE"));
        return armor && helmet && gun && melee && healing;
    }
    private static double expectedChestValue(Inventory inv) {
        // Ước lượng giá trị kỳ vọng của rương dựa trên inventory hiện tại
        // Nếu inventory còn yếu, giá trị cao, nếu đã tối ưu thì thấp
        if (!isInventoryOptimal(inv)) return 120;
        return 30;
    }
    
    private static void manageInventoryBeforePickup(GameMap map, Inventory inv, Node itemPos, Hero hero) {
        try {
            // Skip revoke for items spawned from breaking a chest (adjacent to any chest)
            for (Obstacle chest : map.getListChests()) {
                if (Math.abs(chest.x - itemPos.x) <= 1 && Math.abs(chest.y - itemPos.y) <= 1) {
                    return;
                }
            }
            // Check if we need to revoke something to make space
            if (map.getAllGun().contains(itemPos) && inv.getGun() != null) {
                // Revoke current gun if new one might be better
                hero.revokeItem(inv.getGun().getId());
            } else if (map.getAllMelee().contains(itemPos) && inv.getMelee() != null && 
                       !inv.getMelee().getId().equals("HAND")) {
                // Don't revoke HAND as it's default
                hero.revokeItem(inv.getMelee().getId());
            } else if (map.getListArmors().contains(itemPos) && (inv.getArmor() != null || inv.getHelmet() != null)) {
                // Revoke current armor and/or helmet to make space for new one
                if (inv.getArmor() != null) {
                    hero.revokeItem(inv.getArmor().getId());
                }
                if (inv.getHelmet() != null) {
                    hero.revokeItem(inv.getHelmet().getId());
                }
            } else if (map.getListHealingItems().contains(itemPos) && inv.getListHealingItem().size() >= 4) {
                // Revoke lowest priority healing item
                HealingItem worstHeal = inv.getListHealingItem().stream()
                    .min((a, b) -> Integer.compare(getBasePickupPoints(a.getId()), getBasePickupPoints(b.getId())))
                    .orElse(null);
                if (worstHeal != null) {
                    hero.useItem(worstHeal.getId());
                }
            }
        } catch (Exception e) {
            // Continue even if revoke fails
        }
    }
    
    private static HealingItem getBestHealingItem(List<HealingItem> healingItems) {
        if (healingItems.isEmpty()) return null;
        
        // Ưu tiên items đặc biệt trước
        for (HealingItem item : healingItems) {
            if ("ELIXIR".equals(item.getId()) || "MAGIC".equals(item.getId())) {
                return item; // Special effects
            }
        }
        
        // Sau đó ưu tiên hồi máu nhiều nhất
        return healingItems.stream()
            .max((a, b) -> Integer.compare(getBasePickupPoints(a.getId()), getBasePickupPoints(b.getId())))
            .orElse(null);
    }
    
    private static String findSmartDirection(Node myPos, GameMap map, Inventory inv) {
        String[] dirs = {"u", "d", "l", "r"};
        List<String> safeDirs = new ArrayList<>();
        List<String> itemDirs = new ArrayList<>();
        
        for (String dir : dirs) {
            Node nextPos = moveTo(myPos, dir);
            boolean isSafe = true;
            boolean hasItems = false;
            
            // Check for enemies nearby
            for (Enemy enemy : map.getListEnemies()) {
                if (dist(nextPos, new Node(enemy.x, enemy.y)) <= 1) {
                    isSafe = false;
                    break;
                }
            }
            
            // Check for STUN trap ở vị trí tiếp theo
            boolean hasStunTrap = false;
            for (var trap : map.getListTraps()) {
                if (trap.getX() == nextPos.x && trap.getY() == nextPos.y && trap.getTag() != null && trap.getTag().contains(jsclub.codefest.sdk.model.obstacles.ObstacleTag.HERO_HIT_BY_BAT_WILL_BE_STUNNED)) {
                    hasStunTrap = true;
                    break;
                }
            }
            if (!isSafe || hasStunTrap) continue;
            
            // Check for items in this direction
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
            
            safeDirs.add(dir);
            if (hasItems) {
                itemDirs.add(dir);
            }
        }
        
        // Ưu tiên hướng có items
        if (!itemDirs.isEmpty()) {
            return itemDirs.get((int)(Math.random() * itemDirs.size()));
        }
        
        // Fallback to safe directions
        if (!safeDirs.isEmpty()) {
            return safeDirs.get((int)(Math.random() * safeDirs.size()));
        }
        
        // Last resort: random direction
        return dirs[(int)(Math.random() * 4)];
    }

    // Kiểm tra rương có nằm trong vùng bắn của súng không
    private static String getGunDirectionToChest(Node myPos, Node chestPos, Weapon gun) {
        if (gun == null) return null;
        int range = gun.getRange(); // chiều dài
        // Tạm thời assume rộng = 1 (bắn thẳng hàng)
        // Nếu cần mở rộng, có thể lấy thêm thông tin từ AttackRange hoặc bổ sung trường riêng
        if (myPos.x == chestPos.x) {
            // Cùng hàng ngang
            int dy = chestPos.y - myPos.y;
            if (dy > 0 && dy <= range) return "u";
            if (dy < 0 && -dy <= range) return "d";
        } else if (myPos.y == chestPos.y) {
            // Cùng cột
            int dx = chestPos.x - myPos.x;
            if (dx > 0 && dx <= range) return "r";
            if (dx < 0 && -dx <= range) return "l";
        }
        return null;
    }

    // Kiểm tra rương có nằm trong vùng tấn công của Melee không (dựa vào độ rộng getRange)
    private static String getMeleeDirectionToChest(Node myPos, Node chestPos, Weapon melee) {
        if (melee == null) return null;
        int width = melee.getRange(); // độ rộng (số ô vuông góc với hướng đánh)
        // Melee chỉ đánh 1 ô phía trước mặt, nhưng có thể rộng nhiều ô
        // Kiểm tra 4 hướng
        // Hướng lên (u): các ô (myPos.x, myPos.y - i), i từ -(width/2) đến width/2, và (myPos.x-1, myPos.y - i)
        for (int i = -width/2; i <= width/2; i++) {
            if (myPos.x-1 == chestPos.x && myPos.y+i == chestPos.y) return "r";
            if (myPos.x+1 == chestPos.x && myPos.y+i == chestPos.y) return "l";
            if (myPos.x+i == chestPos.x && myPos.y-1 == chestPos.y) return "d";
            if (myPos.x+i == chestPos.x && myPos.y+1 == chestPos.y) return "u";
        }
        return null;
    }

    // Hàm kiểm tra bot có bị STUN không
    private boolean isStunned() {
        for (Effect e : effects) {
            if (e.id != null && e.id.toUpperCase().contains("STUN")) return true;
        }
        return false;
    }

    // Thêm hàm đánh giá sức mạnh tổng thể của player
    // Điểm càng cao càng mạnh
    private static int evaluateStrength(Player p, Inventory inv) {
        int score = 0;
        if (p == null) return 0;
        // Máu
        if (p.getHealth() != null) score += p.getHealth();
        // Vũ khí
        if (inv != null) {
            if (inv.getGun() != null) score += getBasePickupPoints(inv.getGun().getId()) + 20;
            if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) score += getBasePickupPoints(inv.getMelee().getId()) + 10;
            if (inv.getArmor() != null) score += getBasePickupPoints(inv.getArmor().getId());
            if (inv.getHelmet() != null) score += getBasePickupPoints(inv.getHelmet().getId());
            score += inv.getListHealingItem().size() * 5;
        } else {
            // Đối thủ: chỉ biết máu, tạm cộng điểm nếu máu cao
            if (p.getHealth() != null && p.getHealth() > 80) score += 10;
        }
        return score;
    }
}

