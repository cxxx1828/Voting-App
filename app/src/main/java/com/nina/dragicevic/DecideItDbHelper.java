package com.nina.dragicevic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class DecideItDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "DB_DEBUG";
    private HttpHelper httpHelper;

    // Tabela USERS
    private final String TABLE_USERS = "USERS";
    public static final String COL_NAME = "Name";
    public static final String COL_SURNAME = "Surname";
    public static final String COL_USERNAME = "Username";
    public static final String COL_INDEX = "IndexNumber";
    public static final String COL_PASSWORD = "Password";
    public static final String COL_ROLE = "Role";

    // Tabela SESSIONS
    private final String TABLE_SESSIONS = "SESSIONS";
    public static final String COL_DATE = "Date";
    public static final String COL_SESSION_NAME = "SessionName";
    public static final String COL_DESCRIPTION = "Description";
    public static final String COL_END_TIME = "EndTime";
    public static final String COL_SERVER_ID = "ServerId"; // MongoDB _id

    // Tabela VOTES
    private final String TABLE_VOTES = "VOTES";
    public static final String COL_YES = "YesVotes";
    public static final String COL_NO = "NoVotes";
    public static final String COL_ABSTAIN = "AbstainVotes";
    public static final String COL_VOTE_SESSION_NAME = "SessionName";
    public static final String COL_VOTE_DATE = "SessionDate";
    public static final String COL_VOTE_SERVER_ID = "ServerSessionId"; // MongoDB session _id

    public DecideItDbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        httpHelper = new HttpHelper();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating tables");

        // USERS
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_NAME + " TEXT, " +
                COL_SURNAME + " TEXT, " +
                COL_USERNAME + " TEXT UNIQUE, " +
                COL_INDEX + " TEXT, " +
                COL_PASSWORD + " TEXT, " +
                COL_ROLE + " TEXT);");
        Log.d(TAG, "Table USERS created");

        // SESSIONS - added ServerId column
        db.execSQL("CREATE TABLE " + TABLE_SESSIONS + " (" +
                COL_DATE + " TEXT, " +
                COL_SESSION_NAME + " TEXT, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_END_TIME + " TEXT, " +
                COL_SERVER_ID + " TEXT, " +
                "UNIQUE(" + COL_DATE + ", " + COL_SESSION_NAME + "));");
        Log.d(TAG, "Table SESSIONS created");

        // VOTES - added ServerSessionId column
        db.execSQL("CREATE TABLE " + TABLE_VOTES + " (" +
                COL_YES + " INTEGER, " +
                COL_NO + " INTEGER, " +
                COL_ABSTAIN + " INTEGER, " +
                COL_VOTE_SESSION_NAME + " TEXT, " +
                COL_VOTE_DATE + " TEXT, " +
                COL_VOTE_SERVER_ID + " TEXT);");
        Log.d(TAG, "Table VOTES created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add ServerId column to SESSIONS if upgrading
        try {
            db.execSQL("ALTER TABLE " + TABLE_SESSIONS + " ADD COLUMN " + COL_SERVER_ID + " TEXT");
            Log.d(TAG, "Added ServerId column to SESSIONS");
        } catch (Exception e) {
            Log.d(TAG, "ServerId column already exists or error adding it");
        }

        try {
            db.execSQL("ALTER TABLE " + TABLE_VOTES + " ADD COLUMN " + COL_VOTE_SERVER_ID + " TEXT");
            Log.d(TAG, "Added ServerSessionId column to VOTES");
        } catch (Exception e) {
            Log.d(TAG, "ServerSessionId column already exists or error adding it");
        }
    }

    // =================== HTTP SYNC METHODS ===================

    /**
     * Syncs sessions from server to local database
     */
    public boolean syncSessionsFromServer() {
        try {
            Log.d(TAG, "Syncing sessions from server...");
            JSONArray sessions = httpHelper.getJSONArrayFromURL(HttpHelper.BASE_URL + "/sessions");

            if (sessions == null) {
                Log.e(TAG, "Failed to get sessions from server");
                return false;
            }

            SQLiteDatabase db = getWritableDatabase();

            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.getJSONObject(i);

                String serverId = session.getString("_id");
                String sessionName = session.getString("sessionName");
                String description = session.optString("description", "");
                String dateStr = session.getString("date");
                String endTimeStr = session.getString("endOfVotingTime");

                // Convert ISO date to our format
                String formattedDate = formatDateFromISO(dateStr);

                // Check if session already exists
                Cursor cursor = db.query(TABLE_SESSIONS, null,
                        COL_SERVER_ID + " =?", new String[]{serverId}, null, null, null);

                ContentValues values = new ContentValues();
                values.put(COL_DATE, formattedDate);
                values.put(COL_SESSION_NAME, sessionName);
                values.put(COL_DESCRIPTION, description);
                values.put(COL_END_TIME, endTimeStr);
                values.put(COL_SERVER_ID, serverId);

                if (cursor.getCount() > 0) {
                    // Update existing
                    db.update(TABLE_SESSIONS, values, COL_SERVER_ID + " =?", new String[]{serverId});
                    Log.d(TAG, "Updated session: " + sessionName);
                } else {
                    // Insert new
                    db.insert(TABLE_SESSIONS, null, values);
                    Log.d(TAG, "Inserted new session: " + sessionName);
                }
                cursor.close();
            }

            db.close();
            Log.d(TAG, "Successfully synced " + sessions.length() + " sessions from server");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error syncing sessions from server", e);
            return false;
        }
    }

    /**
     * Creates session on server and updates local database
     */
    public boolean createSessionOnServer(String date, String sessionName, String description) {
        try {
            Log.d(TAG, "Creating session on server: " + sessionName);

            // Convert date to ISO format
            String isoDate = formatDateToISO(date);
            String endTime = formatDateToISO(date, 3); // 3 hours later

            JSONObject sessionData = new JSONObject();
            sessionData.put("date", isoDate);
            sessionData.put("sessionName", sessionName);
            sessionData.put("description", description);
            sessionData.put("endOfVotingTime", endTime);

            JSONObject response = httpHelper.postJSONObjectFromURL(HttpHelper.BASE_URL + "/session", sessionData);

            if (response == null) {
                Log.e(TAG, "Failed to create session on server");
                return false;
            }

            // Get the created session data
            JSONObject createdSession = response.getJSONObject("session");
            String serverId = createdSession.getString("_id");

            // Insert/update in local database with server ID
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_DATE, date);
            values.put(COL_SESSION_NAME, sessionName);
            values.put(COL_DESCRIPTION, description);
            values.put(COL_END_TIME, endTime);
            values.put(COL_SERVER_ID, serverId);

            long result = db.insert(TABLE_SESSIONS, null, values);
            db.close();

            Log.d(TAG, "Successfully created session on server with ID: " + serverId);
            return result != -1;

        } catch (Exception e) {
            Log.e(TAG, "Error creating session on server", e);
            return false;
        }
    }

    /**
     * Syncs votes for a specific session from server
     */
    public boolean syncVotesFromServer(String sessionServerId) {
        try {
            Log.d(TAG, "Syncing votes from server for session: " + sessionServerId);

            JSONArray votes = httpHelper.getJSONArrayFromURL(HttpHelper.BASE_URL + "/votes?sessionId=" + sessionServerId);

            if (votes == null || votes.length() == 0) {
                Log.d(TAG, "No votes found on server for session: " + sessionServerId);
                return true; // Not an error, just no votes yet
            }

            JSONObject vote = votes.getJSONObject(0); // Should be only one vote record per session

            int yes = vote.getInt("yes");
            int no = vote.getInt("no");
            int abstain = vote.getInt("abstain");
            String sessionName = vote.getString("sessionName");
            String sessionDate = formatDateFromISO(vote.getString("sessionDate"));

            SQLiteDatabase db = getWritableDatabase();

            // Check if vote record exists
            Cursor cursor = db.query(TABLE_VOTES, null,
                    COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId}, null, null, null);

            ContentValues values = new ContentValues();
            values.put(COL_YES, yes);
            values.put(COL_NO, no);
            values.put(COL_ABSTAIN, abstain);
            values.put(COL_VOTE_SESSION_NAME, sessionName);
            values.put(COL_VOTE_DATE, sessionDate);
            values.put(COL_VOTE_SERVER_ID, sessionServerId);

            if (cursor.getCount() > 0) {
                // Update existing
                db.update(TABLE_VOTES, values, COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId});
                Log.d(TAG, "Updated votes for session: " + sessionName);
            } else {
                // Insert new
                db.insert(TABLE_VOTES, null, values);
                Log.d(TAG, "Inserted new votes for session: " + sessionName);
            }

            cursor.close();
            db.close();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error syncing votes from server", e);
            return false;
        }
    }

    /**
     * Submits vote to server and updates local database
     */
    public boolean submitVoteToServer(String sessionServerId, String sessionName, String sessionDate, int voteType) {
        try {
            Log.d(TAG, "Submitting vote to server: " + voteType + " for session: " + sessionServerId);

            String voteString;
            switch (voteType) {
                case 1: voteString = "yes"; break;
                case 2: voteString = "no"; break;
                case 3: voteString = "abstain"; break;
                default:
                    Log.e(TAG, "Invalid vote type: " + voteType);
                    return false;
            }

            JSONObject voteData = new JSONObject();
            voteData.put("sessionId", sessionServerId);
            voteData.put("vote", voteString);

            JSONObject response = httpHelper.postJSONObjectFromURL(HttpHelper.BASE_URL + "/results/vote", voteData);

            if (response == null) {
                Log.e(TAG, "Failed to submit vote to server");
                return false;
            }

            // Update local database with new vote counts
            JSONObject votes = response.getJSONObject("votes");
            int yes = votes.getInt("yes");
            int no = votes.getInt("no");
            int abstain = votes.getInt("abstain");

            SQLiteDatabase db = getWritableDatabase();

            // Update or insert vote record
            Cursor cursor = db.query(TABLE_VOTES, null,
                    COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId}, null, null, null);

            ContentValues values = new ContentValues();
            values.put(COL_YES, yes);
            values.put(COL_NO, no);
            values.put(COL_ABSTAIN, abstain);
            values.put(COL_VOTE_SESSION_NAME, sessionName);
            values.put(COL_VOTE_DATE, sessionDate);
            values.put(COL_VOTE_SERVER_ID, sessionServerId);

            if (cursor.getCount() > 0) {
                db.update(TABLE_VOTES, values, COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId});
            } else {
                db.insert(TABLE_VOTES, null, values);
            }

            cursor.close();
            db.close();

            Log.d(TAG, "Successfully submitted vote and updated local database");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error submitting vote to server", e);
            return false;
        }
    }

    /**
     * Gets server ID for a session by date and name
     */
    public String getSessionServerId(String sessionName, String sessionDate) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SESSIONS, new String[]{COL_SERVER_ID},
                COL_SESSION_NAME + " =? AND " + COL_DATE + " =?",
                new String[]{sessionName, sessionDate}, null, null, null);

        String serverId = null;
        if (cursor.moveToFirst()) {
            serverId = cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVER_ID));
        }

        cursor.close();
        db.close();
        return serverId;
    }

    // =================== DATE FORMATTING HELPERS ===================

    private String formatDateFromISO(String isoDate) {
        try {
            // Parse ISO date: "2025-09-20T10:00:00.000Z"
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Date date = isoFormat.parse(isoDate);
            return displayFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ISO date: " + isoDate, e);
            return isoDate;
        }
    }

    private String formatDateToISO(String displayDate) {
        return formatDateToISO(displayDate, 0);
    }

    private String formatDateToISO(String displayDate, int hoursToAdd) {
        try {
            // Parse display date: "20.09.2025"
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = displayFormat.parse(displayDate);

            // Add hours if specified
            if (hoursToAdd > 0) {
                date = new Date(date.getTime() + hoursToAdd * 60 * 60 * 1000);
            }

            return isoFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date to ISO: " + displayDate, e);
            return displayDate;
        }
    }

    // =================== EXISTING METHODS (keeping compatibility) ===================

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String hashed = HexFormat.of().formatHex(md.digest(password.getBytes()));
            Log.d(TAG, "Password hashed: " + hashed);
            return hashed;
        } catch (Exception e) {
            Log.e(TAG, "Error hashing password", e);
            throw new RuntimeException(e);
        }
    }

    public boolean insertUser(String name, String surname, String username, String index, String password, String role) {
        SQLiteDatabase db = null;
        try {
            Log.d(TAG, "Inserting user: " + username);
            db = getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(COL_NAME, name);
            values.put(COL_SURNAME, surname);
            values.put(COL_USERNAME, username);
            values.put(COL_INDEX, index);
            values.put(COL_PASSWORD, hashPassword(password));
            values.put(COL_ROLE, role);

            db.insert(TABLE_USERS, null, values);
            Log.d(TAG, "User insert result: " + username);

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error inserting user", e);
            return false;

        }finally {
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after insertUser");
        }
    }

    public String[] authenticateUser(String username, String password) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d(TAG, "Authenticating user: " + username);
            db = getReadableDatabase();
            String hashedPassword = hashPassword(password);

            cursor = db.query(TABLE_USERS, null,
                    COL_USERNAME + " =? AND " + COL_PASSWORD + " =?",
                    new String[]{username, hashedPassword}, null, null, null);

            String[] userInfo = null;
            if (cursor.moveToFirst()) {
                userInfo = new String[3];
                userInfo[0] = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                userInfo[1] = cursor.getString(cursor.getColumnIndexOrThrow(COL_SURNAME));
                userInfo[2] = cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLE));
                Log.d(TAG, "User authenticated: " + userInfo[0] + " " + userInfo[1] + " Role: " + userInfo[2]);
            } else {
                Log.d(TAG, "Authentication failed for user: " + username);
            }
            return userInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error in authenticateUser", e);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after authenticateUser");
        }
    }

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d(TAG, "Checking if username exists: " + username);
            db = getReadableDatabase();
            cursor = db.query(TABLE_USERS, null, COL_USERNAME + " =?", new String[]{username}, null, null, null);

            boolean exists = cursor.getCount() > 0;
            Log.d(TAG, "Username exists: " + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Error checking username", e);
            return false;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after isUsernameExists");
        }
    }

    public Student[] readStudents() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d(TAG, "Reading all students");
            db = getReadableDatabase();
            cursor = db.query(TABLE_USERS, null, COL_ROLE + " =?", new String[]{"student"}, null, null, null);

            if (cursor.getCount() <= 0) {
                Log.d(TAG, "No students found");
                return null;
            }

            Student[] students = new Student[cursor.getCount()];
            int i = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                students[i++] = createStudentFromCursor(cursor);
            }

            Log.d(TAG, "Total students read: " + students.length);
            return students;
        } catch (Exception e) {
            Log.e(TAG, "Error reading students", e);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after readStudents");
        }
    }

    public void deleteStudent(String username) {
        SQLiteDatabase db = null;
        try {
            Log.d(TAG, "Deleting student: " + username);
            db = getWritableDatabase();
            int rows = db.delete(TABLE_USERS, COL_USERNAME + " =?", new String[]{username});
            Log.d(TAG, "Rows deleted: " + rows);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting student", e);
        } finally {
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after deleteStudent");
        }
    }

    private Student createStudentFromCursor(Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
        String surname = cursor.getString(cursor.getColumnIndexOrThrow(COL_SURNAME));
        String index = cursor.getString(cursor.getColumnIndexOrThrow(COL_INDEX));
        String username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME));

        int imageResId = R.drawable.male_student;

        Log.d(TAG, "Created student from cursor: " + name + " " + surname + " Index: " + index);

        return new Student(imageResId, name, surname, index, false, username);
    }

    public boolean insertSession(String date, String sessionName, String description) {
        // Use the new HTTP method instead
        return createSessionOnServer(date, sessionName, description);
    }

    public Session[] readSessions() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d(TAG, "Reading all sessions");
            db = getReadableDatabase();
            cursor = db.query(TABLE_SESSIONS, null, null, null, null, null, null);

            if (cursor.getCount() <= 0) {
                Log.d(TAG, "No sessions found");
                return null;
            }

            Session[] sessions = new Session[cursor.getCount()];
            int i = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                sessions[i++] = createSessionFromCursor(cursor);
            }

            Log.d(TAG, "Total sessions read: " + sessions.length);
            return sessions;
        } catch (Exception e) {
            Log.e(TAG, "Error reading sessions", e);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after readSessions");
        }
    }

    public ArrayList<String> getSessionDates() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        ArrayList<String> dates = new ArrayList<>();
        try {
            Log.d(TAG, "Getting all session dates");
            db = getReadableDatabase();
            cursor = db.query(TABLE_SESSIONS, new String[]{COL_DATE}, null, null, null, null, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                dates.add(date);
                Log.d(TAG, "Found session date: " + date);
            }
            return dates;
        } catch (Exception e) {
            Log.e(TAG, "Error getting session dates", e);
            return dates;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after getSessionDates");
        }
    }

    public Session getSessionByDate(String date) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d(TAG, "Getting session by date: " + date);
            db = getReadableDatabase();
            cursor = db.query(TABLE_SESSIONS, null, COL_DATE + " =?", new String[]{date}, null, null, null);

            if (cursor.moveToFirst()) {
                Session session = createSessionFromCursor(cursor);
                Log.d(TAG, "Session found: " + session.getNaziv() + " Status: " + session.getAtribut());
                return session;
            } else {
                Log.d(TAG, "No session found for date: " + date);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting session by date", e);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after getSessionByDate");
        }
    }

    private Session createSessionFromCursor(Cursor cursor) {
        String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
        String sessionName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_NAME));
        String endTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_END_TIME));

        long currentTime = System.currentTimeMillis();
        String status;

        try {
            // Try to parse as ISO format first
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date endTime = isoFormat.parse(endTimeStr);
            status = (endTime.getTime() > currentTime) ? "UPCOMING" : "PAST";
        } catch (Exception e) {
            // Fallback to old format or default
            try {
                long endTime = Long.parseLong(endTimeStr);
                status = (endTime > currentTime) ? "UPCOMING" : "PAST";
            } catch (NumberFormatException ex) {
                status = "UPCOMING"; // Default
            }
        }

        Log.d(TAG, "Creating session object -> name: " + sessionName + ", date: " + date + ", status: " + status);

        return new Session(date, sessionName, status);
    }

    public boolean insertOrUpdateVote(String sessionName, String sessionDate, int voteType) {
        // Get server ID for this session
        String serverId = getSessionServerId(sessionName, sessionDate);

        if (serverId != null) {
            // Submit to server first
            boolean serverSuccess = submitVoteToServer(serverId, sessionName, sessionDate, voteType);
            if (serverSuccess) {
                return true; // Local database is already updated in submitVoteToServer
            }
        }

        // Fallback to local-only update if server fails
        return insertOrUpdateVoteLocal(sessionName, sessionDate, voteType);
    }

    private boolean insertOrUpdateVoteLocal(String sessionName, String sessionDate, int voteType) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d("DB_DEBUG", "insertOrUpdateVoteLocal START -> sessionName: " + sessionName +
                    ", sessionDate: " + sessionDate + ", voteType: " + voteType);

            db = getWritableDatabase();
            cursor = db.query(
                    TABLE_VOTES,
                    new String[]{COL_YES, COL_NO, COL_ABSTAIN},
                    COL_VOTE_SESSION_NAME + "=? AND " + COL_VOTE_DATE + "=?",
                    new String[]{sessionName, sessionDate},
                    null, null, null
            );

            ContentValues values = new ContentValues();
            if (cursor != null && cursor.moveToFirst()) {
                int yes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_YES));
                int no = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NO));
                int abstain = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ABSTAIN));

                Log.d("DB_DEBUG", "Existing vote counts -> YES: " + yes +
                        ", NO: " + no + ", ABSTAIN: " + abstain);

                if (voteType == 1) {
                    yes++;
                    Log.d("DB_DEBUG", "Incremented YES");
                } else if (voteType == 2) {
                    no++;
                    Log.d("DB_DEBUG", "Incremented NO");
                } else if (voteType == 3) {
                    abstain++;
                    Log.d("DB_DEBUG", "Incremented ABSTAIN");
                } else {
                    Log.e("DB_DEBUG", "Invalid vote type: " + voteType);
                    return false;
                }

                values.put(COL_YES, yes);
                values.put(COL_NO, no);
                values.put(COL_ABSTAIN, abstain);

                int rows = db.update(TABLE_VOTES, values,
                        COL_VOTE_SESSION_NAME + "=? AND " + COL_VOTE_DATE + "=?",
                        new String[]{sessionName, sessionDate});

                Log.d("DB_DEBUG", "Updated rows: " + rows);
                return rows > 0;
            } else {
                int yes = 0, no = 0, abstain = 0;
                if (voteType == 1) {
                    yes = 1;
                    Log.d("DB_DEBUG", "Creating new vote record with YES = 1");
                } else if (voteType == 2) {
                    no = 1;
                    Log.d("DB_DEBUG", "Creating new vote record with NO = 1");
                } else if (voteType == 3) {
                    abstain = 1;
                    Log.d("DB_DEBUG", "Creating new vote record with ABSTAIN = 1");
                } else {
                    Log.e("DB_DEBUG", "Invalid vote type: " + voteType);
                    return false;
                }

                values.put(COL_YES, yes);
                values.put(COL_NO, no);
                values.put(COL_ABSTAIN, abstain);
                values.put(COL_VOTE_SESSION_NAME, sessionName);
                values.put(COL_VOTE_DATE, sessionDate);

                long id = db.insert(TABLE_VOTES, null, values);
                Log.d("DB_DEBUG", "Inserted new vote row id: " + id);
                return id != -1;
            }
        } catch (Exception e) {
            Log.e("DB_DEBUG", "Error in insertOrUpdateVoteLocal", e);
            return false;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                Log.d("DB_DEBUG", "Cursor closed in insertOrUpdateVoteLocal");
            }
            if (db != null && db.isOpen()) {
                db.close();
                Log.d("DB_DEBUG", "DB closed in insertOrUpdateVoteLocal");
            }
        }
    }

    public int[] getVoteResults(String sessionName, String sessionDate) {
        // First try to sync from server
        String serverId = getSessionServerId(sessionName, sessionDate);
        if (serverId != null) {
            syncVotesFromServer(serverId);
        }

        Log.d("DB_DEBUG", "getVoteResults START -> sessionName: " + sessionName +
                ", sessionDate: " + sessionDate);

        int[] results = new int[3];
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = getReadableDatabase();
            cursor = db.query(TABLE_VOTES, null,
                    COL_VOTE_SESSION_NAME + " =? AND " + COL_VOTE_DATE + " =?",
                    new String[]{sessionName, sessionDate}, null, null, null);

            if (cursor.moveToFirst()) {
                results[0] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_YES));
                results[1] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NO));
                results[2] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ABSTAIN));

                Log.d("DB_DEBUG", "Vote results -> YES: " + results[0] +
                        ", NO: " + results[1] + ", ABSTAIN: " + results[2]);
            } else {
                Log.d("DB_DEBUG", "No votes found for this session/date");
            }
        } catch (Exception e) {
            Log.e("DB_DEBUG", "Error in getVoteResults", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                Log.d("DB_DEBUG", "Cursor closed");
            }
            if (db != null && db.isOpen()) {
                db.close();
                Log.d("DB_DEBUG", "Database closed");
            }
            Log.d("DB_DEBUG", "getVoteResults END");
        }

        return results;
    }
}