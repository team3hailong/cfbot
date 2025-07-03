# Hướng Dẫn Sử Dụng Thuật Toán Dự Đoán Enemy

## Tổng Quan

Thuật toán dự đoán enemy đã được cải thiện để giúp bot tránh nguy hiểm tốt hơn bằng cách:

1. **Dự đoán đường đi của enemy** dựa trên loại enemy và hành vi
2. **Tính toán điểm an toàn** cho các vị trí
3. **Tìm vật phẩm an toàn** với dự đoán enemy
4. **Di chuyển thông minh** để tránh nguy hiểm trong tương lai

## Các Hàm Mới

### 1. Dự Đoán Đường Đi Enemy

#### `predictEnemyNextPosition(Enemy enemy, GameMap map, Node myPos)`
Dự đoán vị trí tiếp theo của enemy dựa trên loại enemy:

- **SPIRIT**: Di chuyển ngẫu nhiên nhưng có xu hướng về phía player khi gần
- **GOLEM/RHINO/ANACONDA**: Enemy hung hăng, luôn di chuyển về phía player
- **LEOPARD/NATIVE**: Enemy săn mồi, có thể "rình" player từ xa
- **GHOST**: Di chuyển hoàn toàn ngẫu nhiên

```java
Node predictedPos = StatBotUtils.predictEnemyNextPosition(enemy, map, myPos);
```

### 2. Kiểm Tra An Toàn

#### `isPositionThreatenedByEnemy(Node pos, GameMap map, Node myPos, int turnsAhead)`
Kiểm tra xem một vị trí có bị enemy đe dọa trong tương lai không:

```java
boolean isThreatened = StatBotUtils.isPositionThreatenedByEnemy(myPos, map, myPos, 2);
```

#### `calculatePositionSafetyScore(Node pos, GameMap map, Node myPos)`
Tính điểm an toàn cho một vị trí (0-150 điểm):

```java
int safetyScore = StatBotUtils.calculatePositionSafetyScore(pos, map, myPos);
if (safetyScore < 50) {
    // Vị trí nguy hiểm
} else if (safetyScore > 100) {
    // Vị trí an toàn
}
```

### 3. Tìm Vật Phẩm An Toàn

#### `findBestItemToPickupWithEnemyPrediction(GameMap map, Inventory inv, Node myPos, Set<Node> excludeItems)`
Tìm vật phẩm tốt nhất với dự đoán enemy:

```java
Node bestItem = StatBotUtils.findBestItemToPickupWithEnemyPrediction(map, inv, myPos, null);
```

**Cải tiến so với hàm cũ:**
- Giảm điểm cho vật phẩm bị enemy đe dọa
- Cộng điểm cho vật phẩm ở vị trí an toàn
- Ưu tiên vật phẩm an toàn khi máu thấp

### 4. Di Chuyển Thông Minh

#### `findSmartDirectionWithEnemyPrediction(Node myPos, GameMap map, Inventory inv)`
Tìm hướng di chuyển tối ưu với dự đoán enemy:

```java
String direction = StatBotUtils.findSmartDirectionWithEnemyPrediction(myPos, map, inv);
```

**Cải tiến so với hàm cũ:**
- Dự đoán enemy trong 1-2 turn tới
- Tránh hướng sẽ bị enemy đe dọa
- Ưu tiên hướng có điểm an toàn cao

## Cách Sử Dụng

### 1. Thay Thế Hàm Cũ

```java
// Thay vì sử dụng:
Node item = StatBotUtils.findBestItemToPickup(map, inv, myPos, null);
String dir = StatBotUtils.findSmartDirection(myPos, map, inv);

// Sử dụng:
Node item = StatBotUtils.findBestItemToPickupWithEnemyPrediction(map, inv, myPos, null);
String dir = StatBotUtils.findSmartDirectionWithEnemyPrediction(myPos, map, inv);
```

### 2. Kiểm Tra An Toàn Trước Khi Di Chuyển

```java
Node nextPos = StatBotUtils.moveTo(myPos, direction);
int safetyScore = StatBotUtils.calculatePositionSafetyScore(nextPos, map, myPos);

if (safetyScore < 50) {
    // Tìm hướng khác an toàn hơn
    direction = StatBotUtils.findSmartDirectionWithEnemyPrediction(myPos, map, inv);
}
```

### 3. Tránh Vật Phẩm Nguy Hiểm

```java
Node item = StatBotUtils.findBestItemToPickupWithEnemyPrediction(map, inv, myPos, null);
if (item != null) {
    boolean isThreatened = StatBotUtils.isPositionThreatenedByEnemy(item, map, myPos, 2);
    if (isThreatened && myHP < 50) {
        // Bỏ qua vật phẩm nguy hiểm khi máu thấp
        return;
    }
}
```

## Demo

Sử dụng `EnemyPredictionDemo.java` để xem cách hoạt động:

```java
EnemyPredictionDemo.runAllDemos(map, inv, myPos);
```

## Lưu Ý Quan Trọng

1. **Hiệu suất**: Các hàm dự đoán có thể chậm hơn một chút do tính toán phức tạp
2. **Độ chính xác**: Dự đoán dựa trên mô hình hành vi, không phải 100% chính xác
3. **Tùy chỉnh**: Có thể điều chỉnh các tham số như phạm vi nguy hiểm, tỉ lệ dự đoán
4. **Tương thích**: Các hàm cũ vẫn hoạt động bình thường

## Ví Dụ Thực Tế

```java
// Trong StatBotMain.java, thay thế logic tìm vật phẩm:
Node bestItem = StatBotUtils.findBestItemToPickupWithEnemyPrediction(map, inv, myPos, excludeItems);
if (bestItem != null) {
    // Kiểm tra thêm độ an toàn
    int safetyScore = StatBotUtils.calculatePositionSafetyScore(bestItem, map, myPos);
    if (safetyScore > 70 || myHP > 80) {
        // Chỉ nhặt vật phẩm nếu an toàn hoặc máu đủ cao
        manageInventoryBeforePickup(map, inv, bestItem, hero);
        hero.pickup(bestItem.x, bestItem.y);
    }
}
```

## Kết Luận

Thuật toán dự đoán enemy giúp bot:
- **Tránh nguy hiểm tốt hơn** bằng cách dự đoán trước
- **Tìm vật phẩm an toàn hơn** thay vì chỉ dựa trên giá trị
- **Di chuyển thông minh hơn** để tránh bẫy enemy
- **Tăng tỉ lệ sống sót** trong các tình huống nguy hiểm 