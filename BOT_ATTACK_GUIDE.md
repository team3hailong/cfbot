# Hướng Dẫn Logic Tấn Công Bot Người Chơi Khác Đứng Im

## Tổng Quan

Bot đã được cải thiện để có thể phát hiện và tấn công bot người chơi khác khi chúng đứng im và xung quanh đủ an toàn. Logic này giúp bot tận dụng cơ hội tấn công khi đối thủ không di chuyển.

## Các Hàm Mới

### 1. Kiểm Tra Bot Đứng Im

#### `isPlayerStandingStillAndSafe(Player player, GameMap map, Node myPos, Map<String, List<Node>> playerPositions)`
Kiểm tra xem bot người chơi khác có đang đứng im và xung quanh đủ an toàn để tấn công không:

**Điều kiện để tấn công:**
- Player đứng im trong 3 turn gần nhất
- Xung quanh an toàn (safetyScore > 70)
- Không có enemy gần (trong phạm vi 2 ô)
- Không có quá nhiều player khác gần (tối đa 1 player khác)

```java
boolean canAttack = StatBotUtils.isPlayerStandingStillAndSafe(player, map, myPos, playerPositions);
```

### 2. Tìm Bot Phù Hợp Để Tấn Công

#### `findVulnerablePlayerToAttack(GameMap map, Node myPos, Map<String, List<Node>> playerPositions)`
Tìm bot người chơi khác phù hợp nhất để tấn công:

**Tiêu chí ưu tiên:**
- Player có máu thấp (điểm cao hơn)
- Player gần hơn (điểm cao hơn)
- Player trong safe zone (dễ tấn công hơn)
- Có thể tấn công an toàn (không có enemy gần)

```java
Player target = StatBotUtils.findVulnerablePlayerToAttack(map, myPos, playerPositions);
if (target != null) {
    // Tấn công target
}
```

### 3. Cập Nhật Lịch Sử Vị Trí

#### `updatePlayerPositions(GameMap map, Map<String, List<Node>> playerPositions)`
Cập nhật lịch sử vị trí của tất cả player để theo dõi chuyển động:

```java
StatBotUtils.updatePlayerPositions(map, playerPositions);
```

## Logic Hoạt Động

### 1. Theo Dõi Vị Trí
- Bot lưu trữ vị trí của tất cả player trong 5 turn gần nhất
- Xóa dữ liệu của player đã chết
- Cập nhật mỗi turn

### 2. Phát Hiện Bot Đứng Im
- Kiểm tra xem player có ở cùng vị trí trong 3 turn gần nhất không
- Nếu có, coi như đang đứng im

### 3. Đánh Giá An Toàn
- Tính điểm an toàn cho vị trí của player
- Kiểm tra có enemy gần không
- Kiểm tra có player khác gần không

### 4. Tấn Công
- Ưu tiên tấn công bot đứng im trước logic tấn công thông thường
- Sử dụng special weapon nếu phù hợp
- Tấn công từ xa nếu có gun/throwable

## Cách Sử Dụng

### 1. Trong StatBotMain.java
Logic đã được tích hợp sẵn trong main loop:

```java
// Cập nhật lịch sử vị trí
StatBotUtils.updatePlayerPositions(map, playerPositions);

// Tìm bot đứng im để tấn công
Player vulnerablePlayer = StatBotUtils.findVulnerablePlayerToAttack(map, myPos, playerPositions);
if (vulnerablePlayer != null) {
    // Tấn công ngay lập tức
    // ...
}
```

### 2. Biến Cần Thiết
Thêm biến để lưu lịch sử vị trí:

```java
static Map<String, List<Node>> playerPositions = new HashMap<>();
```

## Ví Dụ Thực Tế

### Tình Huống 1: Bot Đứng Im Trong Safe Zone
```
Turn 1: Player A ở (10, 10)
Turn 2: Player A ở (10, 10) 
Turn 3: Player A ở (10, 10)
→ Bot phát hiện Player A đứng im
→ Kiểm tra xung quanh an toàn
→ Tấn công ngay lập tức
```

### Tình Huống 2: Bot Đứng Im Ngoài Safe Zone
```
Turn 1: Player B ở (5, 5)
Turn 2: Player B ở (5, 5)
Turn 3: Player B ở (5, 5)
→ Bot phát hiện Player B đứng im
→ Kiểm tra có enemy gần không
→ Nếu an toàn, tấn công từ xa
```

### Tình Huống 3: Bot Di Chuyển
```
Turn 1: Player C ở (15, 15)
Turn 2: Player C ở (16, 15)
Turn 3: Player C ở (16, 16)
→ Bot không tấn công vì Player C đang di chuyển
```

## Lưu Ý Quan Trọng

### 1. Phân Biệt ENEMY và Bot Người Chơi
- **ENEMY**: Các quái vật trong game (SPIRIT, GOLEM, RHINO, v.v.)
- **Bot người chơi khác**: Các player khác trong game
- Logic tấn công chỉ áp dụng cho bot người chơi khác, không áp dụng cho ENEMY

### 2. Điều Kiện An Toàn
- Chỉ tấn công khi xung quanh đủ an toàn
- Tránh tấn công khi có enemy gần
- Tránh tấn công khi có nhiều player khác gần

### 3. Ưu Tiên Tấn Công
- Bot đứng im được ưu tiên tấn công trước
- Sau đó mới áp dụng logic tấn công thông thường
- Đảm bảo không bỏ lỡ cơ hội tấn công

### 4. Debug và Logging
- Bot sẽ in log khi tấn công bot đứng im
- Có thể theo dõi quá trình phát hiện và tấn công

## Kết Luận

Logic tấn công bot đứng im giúp bot:
- **Tận dụng cơ hội** khi đối thủ không di chuyển
- **Tấn công thông minh** dựa trên tình huống
- **Tránh nguy hiểm** bằng cách kiểm tra an toàn
- **Tăng hiệu quả** trong việc loại bỏ đối thủ

Logic này hoạt động song song với logic tấn công thông thường và không ảnh hưởng đến các hành vi khác của bot. 