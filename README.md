# DecideIT - Android Voting System Application


A complete multi-module Android voting system enabling students to participate in voting sessions while administrators manage users and sessions. Features local SQLite persistence, REST API synchronization, and real-time notifications.

---

## System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     Android Application                       │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │     UI      │  │   Business   │  │  Data Persistence│    │
│  │   Layer     │◄─┤    Logic     │◄─┤    (SQLite)      │    │
│  │  (Activities│  │  (Adapters)  │  │  (Factory Pattern)│    │
│  │  Fragments) │  │              │  │                  │    │
│  └─────────────┘  └──────┬───────┘  └──────────────────┘    │
│                           │                                   │
│                    ┌──────▼────────┐                         │
│                    │  HTTP Client  │                         │
│                    │  (REST API)   │                         │
│                    └──────┬────────┘                         │
│                           │                                   │
│                    ┌──────▼────────┐                         │
│                    │ Background    │                         │
│                    │ Service       │                         │
│                    │ (Notifications)│                         │
│                    └───────────────┘                         │
└────────────────────────┬─────────────────────────────────────┘
                         │
                  HTTP/JSON (REST)
                         │
┌────────────────────────▼─────────────────────────────────────┐
│                    Backend Server                             │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────┐        ┌──────────────┐                     │
│  │  Express.js │◄──────►│   MongoDB    │                     │
│  │  REST API   │        │   Database   │                     │
│  │             │        │              │                     │
│  │ - Sessions  │        │ - Users      │                     │
│  │ - Voting    │        │ - Sessions   │                     │
│  │ - Users     │        │ - Votes      │                     │
│  └─────────────┘        └──────────────┘                     │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Development Phases

### Phase 1: UI Foundation

**Goal:** Establish core application structure and navigation

**Implementation:**
- Created multiple Activities and Fragments:
  - Login/Registration screens
  - Calendar view for session selection
  - Voting interface
  - Admin dashboard
- Navigation flow using Intents and Fragment transactions
- Layout system using LinearLayout and FrameLayout
- Strict UI constraints with no margins

**Key Screens:**
```
LoginActivity
    ↓
MainActivity
    ├──► StudentFragment (Calendar view)
    │       └──► VotingFragment
    └──► AdminFragment
            ├──► UserManagementFragment
            └──► SessionCreationFragment
```

---

### Phase 2: Dynamic UI Components

**Goal:** Implement custom adapters and interactive elements

**Features:**
- **Custom RecyclerView Adapters:**
  - Student list adapter (profile images, names, actions)
  - Voting session adapter (date, title, status indicators)
- **Interactive UI updates:**
  - Real-time list modifications
  - Confirmation dialogs for destructive actions
  - Swipe-to-delete functionality
- **Data binding** for efficient view updates

**Example Adapter Structure:**
```java
public class SessionAdapter extends RecyclerView.Adapter<SessionViewHolder> {
    private List<VotingSession> sessions;
    
    @Override
    public void onBindViewHolder(SessionViewHolder holder, int position) {
        VotingSession session = sessions.get(position);
        holder.titleText.setText(session.getTitle());
        holder.dateText.setText(session.getDate());
        holder.statusIcon.setImageResource(session.isActive() 
            ? R.drawable.ic_active 
            : R.drawable.ic_expired);
    }
}
```

---

### Phase 3: Local Persistence with SQLite

**Goal:** Replace hard-coded data with structured database

**Implementation:**
- **Database Schema:**
  ```
  Users Table:
  ├─ id (PRIMARY KEY)
  ├─ username
  ├─ password_hash (SHA-256)
  ├─ role (STUDENT/ADMIN)
  └─ created_at
  
  Sessions Table:
  ├─ id (PRIMARY KEY)
  ├─ title
  ├─ description
  ├─ start_date
  ├─ end_date
  └─ created_by (FOREIGN KEY → Users)
  
  Votes Table:
  ├─ id (PRIMARY KEY)
  ├─ session_id (FOREIGN KEY → Sessions)
  ├─ option_selected
  └─ timestamp (ANONYMOUS - no user_id)
  ```

- **Factory Design Pattern:**
  ```java
  public class DatabaseFactory {
      public static IUserRepository getUserRepository(Context context) {
          return new SQLiteUserRepository(context);
      }
      
      public static ISessionRepository getSessionRepository(Context context) {
          return new SQLiteSessionRepository(context);
      }
  }
  ```

- **Security:**
  - Passwords hashed using SHA-256 before storage
  - Anonymous voting (no user tracking in votes table)
  - SQL injection prevention using parameterized queries

---

### Phase 4: REST API Integration

**Goal:** Synchronize local data with remote MongoDB server

**Backend Stack:**
- **Node.js** with Express.js framework
- **MongoDB** for cloud data storage
- **RESTful API** endpoints

**API Endpoints:**
```
GET  /api/sessions          → Retrieve all voting sessions
POST /api/sessions          → Create new session
GET  /api/sessions/:id      → Get specific session
POST /api/votes             → Submit vote
GET  /api/users             → List users (admin only)
POST /api/auth/login        → User authentication
```

**Android HTTP Client:**
```java
public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:3000/api";
    
    public void getSessions(ResponseCallback callback) {
        Request request = new Request.Builder()
            .url(BASE_URL + "/sessions")
            .get()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                String json = response.body().string();
                List<Session> sessions = parseSessionsFromJson(json);
                syncWithLocalDatabase(sessions);
                callback.onSuccess(sessions);
            }
        });
    }
}
```

**Synchronization Strategy:**
- **Download:** Fetch sessions from server → Update SQLite
- **Upload:** New vote/session created → POST to server → Update local DB
- **Conflict Resolution:** Server timestamp wins
- **Offline Mode:** Queue actions, sync when connection restored

---

### Phase 5: Background Services & Notifications

**Goal:** Real-time session expiration alerts

**Implementation:**

**Bound Service Architecture:**
```java
public class SessionMonitorService extends Service {
    private final IBinder binder = new LocalBinder();
    private Handler handler = new Handler();
    
    private Runnable checkSessionsTask = new Runnable() {
        @Override
        public void run() {
            List<Session> sessions = dbHelper.getActiveSessions();
            
            for (Session session : sessions) {
                long timeUntilExpiry = session.getEndTime() - System.currentTimeMillis();
                
                if (timeUntilExpiry > 0 && timeUntilExpiry <= 15 * 60 * 1000) {
                    showExpiryNotification(session);
                }
            }
            
            handler.postDelayed(this, 60000); // Check every minute
        }
    };
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.post(checkSessionsTask);
        return START_STICKY;
    }
}
```

**Push Notification:**
```java
private void showExpiryNotification(Session session) {
    Intent votingIntent = new Intent(this, VotingActivity.class);
    votingIntent.putExtra("SESSION_ID", session.getId());
    
    PendingIntent pendingIntent = PendingIntent.getActivity(
        this, 0, votingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    
    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Session Ending Soon!")
        .setContentText(session.getTitle() + " expires in 15 minutes")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build();
    
    notificationManager.notify(session.getId(), notification);
}
```

**Features:**
- Periodic background checks (every 60 seconds)
- 15-minute warning threshold
- Tap notification → Direct to voting screen
- Works even when app is closed

---

## Features Overview

| Feature | Description | Technology |
|---------|-------------|------------|
| **User Authentication** | Secure login with hashed passwords | SHA-256, SQLite |
| **Role-Based Access** | Student vs Admin permissions | Custom authorization |
| **Session Management** | Create, view, and manage voting sessions | CRUD operations |
| **Anonymous Voting** | Vote without user tracking | Decoupled vote records |
| **Real-Time Sync** | Local + cloud data consistency | REST API, JSON |
| **Offline Support** | Continue using app without connection | SQLite cache |
| **Push Notifications** | Session expiry warnings | Background Service |
| **Admin Dashboard** | User and session management | Custom UI |

---

## Project Structure

```
DecideIT/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/decideit/
│   │   │   │   ├── activities/
│   │   │   │   │   ├── LoginActivity.java
│   │   │   │   │   ├── MainActivity.java
│   │   │   │   │   └── VotingActivity.java
│   │   │   │   ├── fragments/
│   │   │   │   │   ├── StudentFragment.java
│   │   │   │   │   ├── AdminFragment.java
│   │   │   │   │   └── VotingFragment.java
│   │   │   │   ├── adapters/
│   │   │   │   │   ├── SessionAdapter.java
│   │   │   │   │   └── StudentAdapter.java
│   │   │   │   ├── database/
│   │   │   │   │   ├── DatabaseHelper.java
│   │   │   │   │   ├── DatabaseFactory.java
│   │   │   │   │   └── repositories/
│   │   │   │   │       ├── UserRepository.java
│   │   │   │   │       └── SessionRepository.java
│   │   │   │   ├── network/
│   │   │   │   │   ├── ApiClient.java
│   │   │   │   │   └── ResponseCallback.java
│   │   │   │   ├── services/
│   │   │   │   │   └── SessionMonitorService.java
│   │   │   │   └── models/
│   │   │   │       ├── User.java
│   │   │   │       ├── Session.java
│   │   │   │       └── Vote.java
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       ├── drawable/
│   │   │       └── values/
│   │   └── androidTest/
│   └── build.gradle
├── backend/
│   ├── server.js
│   ├── routes/
│   │   ├── sessions.js
│   │   ├── votes.js
│   │   └── auth.js
│   ├── models/
│   │   ├── User.js
│   │   ├── Session.js
│   │   └── Vote.js
│   └── package.json
└── README.md
```

---

## Technical Stack

### Frontend (Android)
- **Language:** Java
- **IDE:** Android Studio (JetBrains)
- **UI:** Activities, Fragments, RecyclerView
- **Persistence:** SQLite with Factory Pattern
- **Networking:** OkHttp / Volley
- **Background:** Bound Services, Notifications

### Backend (Server)
- **Runtime:** Node.js
- **Framework:** Express.js
- **Database:** MongoDB
- **API:** RESTful JSON endpoints
- **Authentication:** JWT (optional implementation)

---

## Setup Instructions

### 1. Backend Setup
```bash
cd backend/
npm install
node server.js
# Server runs on http://localhost:3000
```

### 2. MongoDB Configuration
```javascript
// backend/config/database.js
const mongoURL = "mongodb://localhost:27017/decideit";
mongoose.connect(mongoURL, { useNewUrlParser: true });
```

### 3. Android Studio
```bash
# Open project in Android Studio
File → Open → Select DecideIT folder

# Update API URL in ApiClient.java
private static final String BASE_URL = "http://10.0.2.2:3000/api";

# Run on emulator or device
Run → Run 'app'
```

### 4. Test Accounts
```
Admin:
Username: admin
Password: admin123

Student:
Username: student
Password: student123
```

---

## Key Accomplishments

- **Complete MVVM-like architecture** with clean separation of concerns
- **Dual persistence strategy:** SQLite for offline, MongoDB for cloud sync
- **Factory Pattern** for database abstraction and scalability
- **Background service** with intelligent notification system
- **Secure authentication** with password hashing
- **Anonymous voting** preserving user privacy
- **Real-time synchronization** between local and remote data
- **Responsive UI** with custom adapters and smooth navigation

---

## Future Enhancements

- [ ] End-to-end encryption for votes
- [ ] Real-time vote tallying using WebSockets
- [ ] Biometric authentication (fingerprint/face)
- [ ] Dark mode theme
- [ ] Export voting results to PDF/CSV
- [ ] Multi-language support (i18n)
- [ ] Firebase Cloud Messaging for push notifications
- [ ] Material Design 3 components

---

## Author

**Nina Dragićević**  


---

## License

MIT License - see [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- JetBrains for Android Studio and development tools
- Android Open Source Project for comprehensive documentation
- MongoDB team for excellent Node.js driver
- Express.js community for REST API best practices
