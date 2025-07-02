# Tối ưu hóa Special Weapons cho Bot

## Tổng quan

Đã thực hiện tối ưu hóa thuật toán để bot có thể sử dụng special weapons một cách hiệu quả và linh hoạt hơn. Dựa trên phân tích folder `elements`, có 3 loại special weapons chính:

## Các Special Weapons

### 1. ROPE (Dây thừng)
- **Tầm hoạt động**: 6 cell
- **Hiệu ứng**: Kéo hero về phía mình 1 cell + stun 1s, hoặc kéo mình về phía obstacle/NPC
- **Chiến thuật sử dụng**:
  - Kéo enemy từ xa về gần để tấn công
  - Kéo player khác về gần để combat
  - Kéo mình về phía obstacle có thể phá hủy

### 2. BELL (Chuông)
- **Tầm hoạt động**: 7x7 cell
- **Hiệu ứng**: Đảo ngược di chuyển tất cả hero trong vùng 7x7 trong 10s
- **Chiến thuật sử dụng**:
  - Sử dụng khi có nhiều enemy/player trong vùng
  - Tạo cơ hội tấn công khi enemy bị confuse
  - Crowd control hiệu quả

### 3. SAHUR_BAT (Gậy đánh bóng)
- **Tầm hoạt động**: 5 cell
- **Hiệu ứng**: Đẩy lùi hero 3 cell + stun 2s nếu va phải obstacle
- **Chiến thuật sử dụng**:
  - Đẩy enemy ra xa khi bị bao vây
  - Đẩy enemy vào obstacle để gây stun
  - Tạo khoảng cách an toàn

## Các cải tiến đã thực hiện

### 1. Thêm các hàm mới trong StatBotUtils

#### `shouldUseSpecialWeapon()`
- Kiểm tra xem có nên sử dụng special weapon không
- Trả về hướng sử dụng tối ưu

#### `shouldUseRope()`, `shouldUseBell()`, `shouldUseSahurBat()`
- Logic riêng cho từng loại special weapon
- Phân tích tình huống và đưa ra quyết định thông minh

#### `shouldPrioritizeSpecialWeapon()`
- So sánh hiệu quả của special weapon với weapon thường
- Quyết định có nên ưu tiên special weapon không

#### `findOptimalSpecialWeaponDirection()`
- Tìm hướng tối ưu để sử dụng special weapon
- Đánh giá điểm số cho từng hướng

### 2. Cập nhật StatBotMain

#### Xử lý Enemy với Special Weapons
- Kiểm tra sử dụng special weapon trước khi xử lý enemy
- Tấn công enemy bằng special weapon khi có cơ hội
- Sử dụng special weapon để thoát hiểm

#### Combat với Player khác
- Ưu tiên special weapon trong combat
- Sử dụng special weapon cho tấn công từ xa
- Tích hợp special weapon vào logic đánh giá sức mạnh

### 3. Cải thiện hệ thống đánh giá

#### Điểm số vật phẩm
- Tăng điểm ưu tiên cho special weapons:
  - ROPE: +400 điểm (rất hữu ích cho combat)
  - BELL: +350 điểm (hữu ích cho crowd control)
  - SAHUR_BAT: +300 điểm (hữu ích cho escape)

#### Đánh giá sức mạnh
- Thêm điểm cho special weapons trong `evaluateStrength()`:
  - ROPE: +150 điểm
  - BELL: +120 điểm
  - SAHUR_BAT: +100 điểm

#### Inventory tối ưu
- Cập nhật `isInventoryOptimal()` để bao gồm special weapons
- Bot sẽ cố gắng có đủ special weapons trong inventory

## Logic sử dụng chi tiết

### ROPE Logic
```java
// Kéo enemy từ xa (3-6 cell) về gần
if (distance >= 3 && distance <= 6) {
    return directionTo(myPos, enemyPos);
}

// Kéo player khác từ xa
if (distance >= 3 && distance <= 6) {
    return directionTo(myPos, playerPos);
}

// Kéo mình về phía obstacle có thể phá hủy
if (obstacle.getTag().contains("DESTRUCTIBLE")) {
    return directionTo(myPos, obstaclePos);
}
```

### BELL Logic
```java
// Đếm enemy/player trong vùng 7x7
if (enemyCount >= 2 || playerCount >= 1) {
    // Chọn hướng có nhiều enemy nhất
    return bestDirection;
}
```

### SAHUR_BAT Logic
```java
// Đẩy enemy gần (≤5 cell)
if (distance <= 5) {
    // Kiểm tra obstacle phía sau để gây stun
    if (obstacle.getTag().contains("DESTRUCTIBLE")) {
        return direction; // Bonus cho stun
    }
    return direction; // Đẩy ra xa
}
```

## Ưu điểm của cải tiến

1. **Linh hoạt hơn**: Bot có thể sử dụng special weapons trong nhiều tình huống khác nhau
2. **Thông minh hơn**: Phân tích tình huống để chọn special weapon phù hợp
3. **Hiệu quả hơn**: Ưu tiên special weapons khi chúng có lợi thế hơn weapon thường
4. **An toàn hơn**: Sử dụng special weapons để thoát hiểm và tạo khoảng cách

## Kết quả mong đợi

- Bot sẽ sử dụng ROPE để kéo enemy về gần và tấn công
- Bot sẽ sử dụng BELL khi bị bao vây bởi nhiều enemy
- Bot sẽ sử dụng SAHUR_BAT để thoát hiểm và tạo khoảng cách
- Bot sẽ ưu tiên nhặt special weapons hơn các weapon thường
- Bot sẽ đánh giá sức mạnh chính xác hơn khi có special weapons 