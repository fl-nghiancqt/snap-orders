# Firestore Security Rules Setup

## Problem
You're getting `PERMISSION_DENIED` errors because Firestore security rules are blocking read/write access.

## Solution
Configure Firestore security rules in the Firebase Console to allow access for testing.

## Steps to Fix

### 1. Go to Firebase Console
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **naporders-23cd3**
3. Click on **Firestore Database** in the left sidebar
4. Click on the **Rules** tab

### 2. Replace the Rules
Copy and paste the following rules into the Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // USERS collection: Allow read/write for testing
    match /users/{userId} {
      allow read, write: if true;
    }
    
    // MENUS collection: Allow read/write for testing
    match /menus/{menuId} {
      allow read, write: if true;
    }
    
    // ORDERS collection: Allow read/write for testing
    match /orders/{orderId} {
      allow read, write: if true;
    }
    
    // Allow all other collections for testing (optional)
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

### 3. Publish the Rules
1. Click **Publish** button
2. Wait for confirmation that rules are published

### 4. Test Again
After publishing, try the app again. The permission errors should be resolved.

## ⚠️ Important Notes

**These rules are for DEVELOPMENT/TESTING ONLY!**

For production, you should implement proper authentication and security rules, for example:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can only read their own data
    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Menus: Anyone can read, only admins can write
    match /menus/{menuId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                     get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
    }
    
    // Orders: Users can create/read their own orders
    match /orders/{orderId} {
      allow read: if request.auth != null && 
                    resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && 
                      request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null && 
                      resource.data.userId == request.auth.uid;
    }
  }
}
```

## Quick Fix (Temporary - 30 days)
If you need a quick temporary fix, Firebase also offers a "Test mode" that allows read/write for 30 days, but this is not recommended for production.


