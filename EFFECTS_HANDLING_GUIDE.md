# Hướng Dẫn Xử Lý Effects trong Bot

## Tổng Quan

Bot đã được cải thiện để xử lý tất cả các loại effects một cách thông minh, bao gồm:
- **Effects khống chế**: STUN, BLIND, REVERSE
- **Effects gây sát thương**: POISON, BLEED  
- **Effects có lợi**: INVISIBLE, UNDEAD, CONTROL_IMMUNITY, REVIVAL
- **Effects tức thì**: PULL, KNOCKBACK

## Các Loại Effects và Cách Xử Lý

### 1. Effects Khống Chế (Ưu tiên cao nhất)

#### STUN
- **Mô tả**: Làm choáng, không thể thực hiện bất kỳ hành động nào
- **Xử lý**: 
  - Ưu tiên dùng ELIXIR để giải ngay
  - Nếu không có ELIXIR, dùng COMPASS để stun area
  - Nếu không có vật phẩm giải, không làm gì cả

#### BLIND  
- **Mô tả**: Không nhìn thấy gì khi ở trong vùng bị mù
- **Xử lý**:
  - Ưu tiên dùng ELIXIR để giải
  - Tránh combat và di chuyển cẩn thận
  - Không thể tấn công an toàn

#### REVERSE
- **Mô tả**: Đảo ngược di chuyển (muốn đi phải thì đi trái)
- **Xử lý**:
  - Ưu tiên dùng ELIXIR để giải
  - Có thể di chuyển nhưng khó khăn
  - Tránh combat phức tạp

### 2. Effects Gây Sát Thương (Ưu tiên trung bình)

#### POISON
- **Mô tả**: Gây sát thương 5/s theo thời gian
- **Xử lý**:
  - Ưu tiên hồi máu bằng healing items
  - Nếu không có healing, dùng MAGIC để tàng hình thoát
  - Giảm điểm sức mạnh khi đánh giá combat

#### BLEED
- **Mô tả**: Gây sát thương 5/s và giảm 50% khả năng hồi HP
- **Xử lý**:
  - Ưu tiên cao hơn POISON do nguy hiểm hơn
  - Dùng healing items ngay lập tức
  - Nếu không có healing, dùng MAGIC để tàng hình
  - Giảm điểm sức mạnh nhiều hơn POISON

### 3. Effects Có Lợi (Tận dụng)

#### INVISIBLE
- **Mô tả**: Tàng hình, hero khác không nhìn thấy
- **Xử lý**:
  - Tận dụng để tấn công bất ngờ
  - Tăng điểm sức mạnh khi đánh giá combat
  - Có thể thoát khỏi tình huống nguy hiểm

#### UNDEAD
- **Mô tả**: Bất tử trong 2s, không chịu sát thương
- **Xử lý**:
  - Tấn công mạnh mẽ mà không sợ bị đánh
  - Tăng điểm sức mạnh đáng kể
  - Tận dụng thời gian bất tử để combat

#### CONTROL_IMMUNITY
- **Mô tả**: Miễn khống chế trong 7s
- **Xử lý**:
  - Tự tin tấn công mà không sợ bị stun/blind
  - Tăng điểm sức mạnh
  - Có thể chủ động tấn công

#### REVIVAL
- **Mô tả**: Hồi sinh ngay lập tức khi chết
- **Xử lý**:
  - Tự tin combat mạnh mẽ
  - Tăng điểm sức mạnh rất cao
  - Không sợ chết

### 4. Effects Tức Thì

#### PULL
- **Mô tả**: Bị kéo về vị trí khác ngay lập tức
- **Xử lý**: Không cần xử lý đặc biệt, chỉ là thay đổi vị trí

#### KNOCKBACK
- **Mô tả**: Bị đẩy lùi 3 cell
- **Xử lý**: Không cần xử lý đặc biệt, chỉ là thay đổi vị trí

## Logic Xử Lý Effects

### 1. Kiểm Tra Mức Độ Ưu Tiên
```java
int effectPriority = EffectUtils.getEffectPriority(currentEffects);
```

**Thang điểm ưu tiên:**
- STUN: 100 điểm
- BLIND: 80 điểm  
- REVERSE: 60 điểm
- BLEED: 40 điểm
- POISON: 30 điểm

### 2. Quyết Định Hành Động

#### Khi Bị Khống Chế (STUN/BLIND/REVERSE)
1. Thử dùng ELIXIR trước
2. Nếu không có ELIXIR, thử dùng COMPASS
3. Nếu không có gì, không làm gì cả

#### Khi Bị Sát Thương Theo Thời Gian (POISON/BLEED)
1. Ưu tiên dùng healing items
2. Nếu không có healing, dùng MAGIC để tàng hình
3. Tránh combat nếu có thể

#### Khi Có Effects Có Lợi
1. Tận dụng để tấn công mạnh hơn
2. Tăng điểm sức mạnh khi đánh giá combat
3. Có thể chủ động tấn công

### 3. Điều Chỉnh Chiến Thuật Combat

#### Kiểm Tra Khả Năng Tấn Công
```java
if (!EffectUtils.canAttackSafely(currentEffects)) {
    // Né tránh thay vì tấn công
}
```

#### Kiểm Tra Khả Năng Di Chuyển
```java
if (!EffectUtils.canMoveNormally(currentEffects)) {
    // Không thực hiện hành động
}
```

#### Đánh Giá Sức Mạnh Có Xem Xét Effects
```java
int myStrength = StatBotUtils.evaluateStrengthWithEffects(me, inv, currentEffects);
```

## Các Vật Phẩm Giải Effects

### ELIXIR (Thần dược)
- **Tác dụng**: Giải tất cả effects khống chế
- **Ưu tiên**: Cao nhất khi bị STUN/BLIND/REVERSE
- **Thời gian**: 7s miễn khống chế

### MAGIC (Gậy thần)
- **Tác dụng**: Tàng hình 5s
- **Ưu tiên**: Khi bị sát thương theo thời gian hoặc cần thoát
- **Lưu ý**: Gây sát thương sẽ hiện nguyên hình

### COMPASS (La bàn)
- **Tác dụng**: Stun area 9x9 trong 7s
- **Ưu tiên**: Khi bị bao vây hoặc cần thoát khỏi tình huống nguy hiểm
- **Lưu ý**: Stun tất cả hero trong vùng trừ bản thân

## Cải Tiến So Với Phiên Bản Cũ

1. **Xử lý toàn diện**: Không chỉ STUN mà tất cả effects
2. **Ưu tiên thông minh**: Dựa trên mức độ nguy hiểm của effects
3. **Tận dụng effects có lợi**: Không chỉ né tránh effects có hại
4. **Đánh giá sức mạnh chính xác**: Có xem xét effects khi quyết định combat
5. **Chiến thuật linh hoạt**: Thay đổi hành vi dựa trên effects hiện tại

## Debug và Monitoring

Bot sẽ in ra thông tin effects hiện tại:
```
Effects hiện tại: STUN(5s), POISON(10s)
Bot đang bị effects với mức ưu tiên: 130
Bot đang bị khống chế, ưu tiên dùng vật phẩm giải hiệu ứng
Sử dụng ELIXIR để giải khống chế
```

Điều này giúp theo dõi và debug việc xử lý effects của bot. 