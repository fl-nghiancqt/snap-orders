# Order Data Structure Comparison

## Debug Test Order vs Real Order Flow

This document compares the data structures between the debug test order creation and the real order creation flow to identify any differences.

---

## 1. Debug Test Order (CartViewModel.createTestOrderDirectly)

### OrderItem Creation:
```kotlin
OrderItem(
    menuItemId = "test_item_1",        // String
    menuItemName = "Test Item 1",      // String
    price = 30000.0,                   // Double (hardcoded)
    quantity = 2                        // Int
)
```

### Order Creation:
```kotlin
Order(
    id = "",                           // String (empty, set by Firestore)
    tableNumber = 99,                  // Int
    status = OrderStatus.CREATED,      // OrderStatus enum
    items = testItems,                 // List<OrderItem>
    totalPrice = totalPrice,           // Double (subtotal + serviceFee)
    createdAt = Timestamp.now(),       // Timestamp
    userId = userId                    // String
)
```

### Firestore Map Structure (via Order.toMap()):
```kotlin
{
    "tableNumber": 99,                 // Int
    "items": [
        {
            "menuItemId": "test_item_1",    // String
            "menuItemName": "Test Item 1",  // String
            "price": 30000.0,               // Double
            "quantity": 2                    // Int
        }
    ],
    "totalPrice": 95000.0,             // Double
    "status": "CREATED",               // String (enum.name)
    "createdAt": Timestamp,            // Timestamp
    "userId": "test_user"              // String
}
```

---

## 2. Real Order Flow (OrderBusinessLogic.createNewOrder)

### CartItem → OrderItem Conversion:
```kotlin
// Input: CartItem
CartItem(
    id = "...",                        // String
    menuItemId = "...",                // String
    name = "...",                      // String
    price = 30000,                     // Int (from MenuItem)
    quantity = 2                        // Int
)

// Conversion:
OrderItem(
    menuItemId = cartItem.menuItemId,  // String
    menuItemName = cartItem.name,      // String
    price = cartItem.price.toDouble(), // Double (converted from Int)
    quantity = cartItem.quantity       // Int
)
```

### Order Creation:
```kotlin
Order(
    id = "",                           // String (default, empty)
    tableNumber = tableNumber,          // Int (from user input)
    status = OrderStatus.CREATED,      // OrderStatus enum
    items = orderItems,                // List<OrderItem>
    totalPrice = totalPrice,           // Double (subtotal + serviceFee)
    createdAt = Timestamp.now(),       // Timestamp
    userId = userId                     // String
)
```

### Firestore Map Structure (via Order.toMap()):
```kotlin
{
    "tableNumber": 5,                  // Int
    "items": [
        {
            "menuItemId": "...",            // String
            "menuItemName": "...",          // String
            "price": 30000.0,              // Double (converted from Int)
            "quantity": 2                   // Int
        }
    ],
    "totalPrice": 95000.0,             // Double
    "status": "CREATED",               // String (enum.name)
    "createdAt": Timestamp,            // Timestamp
    "userId": "user_id"                // String
}
```

---

## 3. Key Differences

### ✅ Same Structure
Both flows use the **exact same**:
- `Order` model
- `OrderItem` model
- `Order.toMap()` conversion function
- `OrderItem.toMap()` conversion function
- Firestore collection and document structure

### ⚠️ Potential Type Conversion
- **Debug**: `price` is directly `Double` (30000.0)
- **Real**: `price` is converted from `Int` to `Double` via `cartItem.price.toDouble()`

**This should be fine** - both result in `Double` type in Firestore.

---

## 4. Data Flow Comparison

### Debug Test Flow:
```
CartViewModel.createTestOrderDirectly()
  → Creates OrderItem (price: Double)
  → Creates Order
  → orderRepository.createOrder(order)
  → Order.toMap()
  → Firestore
```

### Real Order Flow:
```
CartViewModel.onPlaceOrderClick()
  → orderBusinessLogic.processOrderPlacement()
    → createNewOrder()
      → Convert CartItem (price: Int) → OrderItem (price: Double)
      → Creates Order
      → orderRepository.createOrder(order)
      → Order.toMap()
      → Firestore
```

---

## 5. Logging Added

Enhanced logging now shows:
1. **CartViewModel**: CartItem data types and values
2. **OrderBusinessLogic**: CartItem → OrderItem conversion with types
3. **OrderRepository**: Complete Firestore map structure with data types

---

## 6. How to Compare

1. **Run Debug Test**:
   - Click "🔧 DEBUG: Create Test Order" button
   - Check logs for "DEBUG: TEST ORDER DATA STRUCTURE"

2. **Run Real Order**:
   - Add items to cart
   - Enter table number
   - Click "Place Order"
   - Check logs for "REAL ORDER DATA STRUCTURE"

3. **Compare Logs**:
   ```bash
   adb logcat -s CartViewModel OrderBusinessLogic OrderRepository | grep -E "(DATA STRUCTURE|type:|price:|quantity:)"
   ```

4. **Look for differences in**:
   - Data types (Int vs Double)
   - Missing fields
   - Null values
   - Empty strings

---

## 7. Expected Issues to Check

1. **Empty menuItemId**: If `cartItem.menuItemId` is empty, the order item won't have a valid reference
2. **Price conversion**: Ensure `Int.toDouble()` doesn't cause precision issues
3. **Empty cart items**: If cart is empty, order won't be created
4. **Table number validation**: If table number is invalid, order won't be created
5. **Existing order check**: If table has open order, it tries to update instead of create

---

## Next Steps

1. Run both flows and compare the logs
2. Check if any fields are missing or have wrong types
3. Verify Firestore document structure matches expectations
4. Check for any exceptions during conversion

