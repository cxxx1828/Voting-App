# DecideIT - Android Voting System

An Android voting app for students and administrators. Students can vote in sessions while admins manage users and create new voting sessions. Uses SQLite for local storage and syncs with a MongoDB backend.

## Overview

The app has two user roles:
- **Students** view a calendar of voting sessions and submit votes
- **Admins** create sessions and manage users

Data is stored locally in SQLite and syncs with a Node.js/MongoDB backend when online. The app works offline and queues changes until connectivity is restored.

## How It Works

```
Login → MainActivity → Student/Admin Views → Voting/Management
```

Students see a calendar with active sessions. Clicking a session opens the voting interface. Admins get a dashboard to create new sessions and manage users.

Background service checks every minute for sessions expiring soon and sends notifications 15 minutes before they end.

## Development Phases

### Phase 1 - Basic UI
Built the core screens using Activities and Fragments. Set up navigation between login, main screen, and different user views. Used LinearLayout and FrameLayout with no margins per requirements.

Main flow:
- LoginActivity for authentication
- MainActivity holds StudentFragment or AdminFragment
- StudentFragment shows calendar, opens VotingFragment
- AdminFragment has user management and session creation

### Phase 2 - Custom Adapters
Added RecyclerView adapters for displaying lists:
- Student list with profile pics and action buttons
- Session list with dates and status indicators

Implemented swipe-to-delete, confirmation dialogs, and real-time list updates.

### Phase 3 - SQLite Database
Replaced hardcoded data with proper database:

```
Users: id, username, password_hash, role, created_at
Sessions: id, title, description, start_date, end_date, created_by
Votes: id, session_id, option_selected, timestamp
```

Used Factory Pattern for database access:
```java
DatabaseFactory.getUserRepository(context)
DatabaseFactory.getSessionRepository(context)
```

Passwords are hashed with SHA-256. Votes are anonymous - no user_id stored.

### Phase 4 - REST API
Built Express.js backend with MongoDB. Created endpoints for:
- GET/POST /api/sessions
- POST /api/votes
- GET /api/users (admin only)
- POST /api/auth/login

Android side uses OkHttp to fetch data from server and sync with local SQLite. If something changes locally, it POSTs to the server. If there's a conflict, server timestamp wins.

Offline mode queues actions and syncs when connection comes back.

### Phase 5 - Background Service
Created a bound service that runs every minute checking for sessions about to expire. If a session ends in 15 minutes or less, it fires a notification. Tapping the notification opens that voting session directly.

Works even when the app is closed. Service is START_STICKY so it restarts if killed.

## Project Structure

```
DecideIT/
├── app/src/main/java/com/decideit/
│   ├── activities/
│   │   ├── LoginActivity.java
│   │   ├── MainActivity.java
│   │   └── VotingActivity.java
│   ├── fragments/
│   │   ├── StudentFragment.java
│   │   ├── AdminFragment.java
│   │   └── VotingFragment.java
│   ├── adapters/
│   │   ├── SessionAdapter.java
│   │   └── StudentAdapter.java
│   ├── database/
│   │   ├── DatabaseHelper.java
│   │   ├── DatabaseFactory.java
│   │   └── repositories/
│   ├── network/
│   │   ├── ApiClient.java
│   │   └── ResponseCallback.java
│   ├── services/
│   │   └── SessionMonitorService.java
│   └── models/
│       ├── User.java
│       ├── Session.java
│       └── Vote.java
├── backend/
│   ├── server.js
│   ├── routes/
│   ├── models/
│   └── package.json
└── README.md
```

## Tech Stack

**Android:**
- Java
- Activities/Fragments for UI
- SQLite with Factory Pattern
- OkHttp for HTTP
- Bound Services for background tasks

**Backend:**
- Node.js + Express.js
- MongoDB
- REST API with JSON

## Setup

Start the backend:
```bash
cd backend/
npm install
node server.js
```

MongoDB should be running on localhost:27017. Update the connection string in `backend/config/database.js` if needed.

Open the Android project in Android Studio. Update the API URL in ApiClient.java:
```java
private static final String BASE_URL = "http://10.0.2.2:3000/api";
```

Run on emulator or device.

Test accounts:
- Admin: admin / admin123
- Student: student / student123

## Features

- Login with password hashing (SHA-256)
- Role-based access (student vs admin)
- Create and manage voting sessions
- Anonymous voting (no user tracking)
- Offline support with sync queue
- Push notifications for expiring sessions
- Calendar view of active sessions
- Admin dashboard for user management

## What Could Be Added

- Vote encryption
- Real-time results with WebSockets
- Fingerprint/face authentication
- Dark mode
- Export results to PDF/CSV
- Multi-language support
- Firebase push notifications instead of local service
- Material Design 3

## Author

Nina Dragićević

## License

MIT
