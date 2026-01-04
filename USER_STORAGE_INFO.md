# User Storage Information

## Current Implementation

### Where User is Stored

The logged-in user is currently stored **in memory only** in `AuthViewModel`:

```kotlin
// AuthViewModel.kt
private val _currentUser = MutableStateFlow<User?>(null)
val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
```

### Storage Details

1. **Location**: `AuthViewModel._currentUser` (MutableStateFlow)
2. **Type**: In-memory only (StateFlow)
3. **Persistence**: ❌ **NOT persisted** - User is lost when app is closed
4. **Scope**: Lives only during app session

### User Data Source

- **User accounts** are stored in **Firestore** (`users` collection)
- **Current logged-in user** is stored in **memory** (AuthViewModel)
- User data is fetched from Firestore during login

## Current Flow

1. **Login**: 
   - User enters email/password
   - `AuthViewModel.login()` queries Firestore
   - If valid, sets `_currentUser.value = user` (in memory)
   
2. **During App Session**:
   - User data is available via `authViewModel.currentUser`
   - All screens can access current user
   
3. **App Restart**:
   - `_currentUser` is reset to `null`
   - User must login again

## Limitations

❌ **User is NOT persisted across app restarts**
- If app is closed, user must login again
- No automatic login/session restoration

## Solution: Add Persistence

To persist user across app restarts, you can:

### Option 1: SharedPreferences (Simple)
```kotlin
// Store user ID after login
sharedPreferences.edit().putString("user_id", user.id).apply()

// On app start, load user from Firestore using stored ID
```

### Option 2: DataStore (Recommended)
```kotlin
// Store user data in DataStore
userPreferencesDataStore.edit { preferences ->
    preferences[USER_ID_KEY] = user.id
    preferences[USER_EMAIL_KEY] = user.email
}

// On app start, restore user from DataStore
```

### Option 3: EncryptedSharedPreferences (Secure)
```kotlin
// Store sensitive user data securely
encryptedSharedPreferences.edit()
    .putString("user_id", user.id)
    .apply()
```

## Current State

✅ **Works during app session**
- User is stored after login
- Available throughout the app
- Profile screen can display user info

❌ **Lost on app restart**
- User must login again
- No automatic session restoration

## Recommendation

For a production app, you should:
1. Store user ID in SharedPreferences/DataStore after login
2. On app start, check if user ID exists
3. If exists, fetch user from Firestore and set `_currentUser`
4. Auto-navigate to appropriate screen (Menu/Admin)

This would provide "Remember Me" functionality.

