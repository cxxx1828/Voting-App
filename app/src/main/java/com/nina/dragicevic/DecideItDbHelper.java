package com.nina.dragicevic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;


public class DecideItDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "DB_DEBUG";

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

    // Tabela VOTES
    private final String TABLE_VOTES = "VOTES";
    public static final String COL_YES = "YesVotes";
    public static final String COL_NO = "NoVotes";
    public static final String COL_ABSTAIN = "AbstainVotes";
    public static final String COL_VOTE_SESSION_NAME = "SessionName";
    public static final String COL_VOTE_DATE = "SessionDate";

    public DecideItDbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
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

        // SESSIONS
        db.execSQL("CREATE TABLE " + TABLE_SESSIONS + " (" +
                COL_DATE + " TEXT, " +
                COL_SESSION_NAME + " TEXT, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_END_TIME + " TEXT, " +
                "UNIQUE(" + COL_DATE + ", " + COL_SESSION_NAME + "));");
        Log.d(TAG, "Table SESSIONS created");

        // VOTES
        db.execSQL("CREATE TABLE " + TABLE_VOTES + " (" +
                COL_YES + " INTEGER, " +
                COL_NO + " INTEGER, " +
                COL_ABSTAIN + " INTEGER, " +
                COL_VOTE_SESSION_NAME + " TEXT, " +
                COL_VOTE_DATE + " TEXT);");
        Log.d(TAG, "Table VOTES created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

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


    //login autentifikacija korisnika, u LoginActivity
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

    // SESSION OPERATIONS

    public boolean insertSession(String date, String sessionName, String description) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d(TAG, "Inserting session: " + sessionName + " Date: " + date);
            db = getWritableDatabase();

            // proverim da li već postoji sesija za ovaj datum
            cursor = db.query(TABLE_SESSIONS, null,
                    COL_DATE + " =?", new String[]{date}, null, null, null);

            if (cursor.getCount() > 0) {
                Log.d(TAG, "Session already exists for date: " + date);
                return false; // ako već postoji sesija za ovaj datum false
            }

            // 3 dana unapred
            long endTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3);

            ContentValues values = new ContentValues();
            values.put(COL_DATE, date);
            values.put(COL_SESSION_NAME, sessionName);
            values.put(COL_DESCRIPTION, description);
            values.put(COL_END_TIME, String.valueOf(endTime));

            db.insert(TABLE_SESSIONS, null, values);

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error inserting session", e);
            return false;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
            Log.d(TAG, "Database closed after insertSession");
        }
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
        String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));           // datum
        String sessionName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_NAME)); // naziv
        String endTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_END_TIME));

        long currentTime = System.currentTimeMillis();
        long endTime = Long.parseLong(endTimeStr);
        String status = (endTime > currentTime) ? "UPCOMING" : "PAST";

        Log.d(TAG, "Creating session object -> name: " + sessionName + ", date: " + date + ", status: " + status);

        // PAZI REDOSLED
        return new Session(date, sessionName, status);
    }

    // VOTE OPERATIONS
    public boolean insertOrUpdateVote(String sessionName, String sessionDate, int voteType) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d("DB_DEBUG", "insertOrUpdateVote START -> sessionName: " + sessionName +
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
                // ako postoji, novi ++
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
                return rows > 0; // true ako je bar jedan red promenjen
            } else {
                // novi glasovi, pa inicijalizujem polja
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
            Log.e("DB_DEBUG", "Error in insertOrUpdateVote", e);
            return false;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                Log.d("DB_DEBUG", "Cursor closed in insertOrUpdateVote");
            }
            if (db != null && db.isOpen()) {
                db.close();
                Log.d("DB_DEBUG", "DB closed in insertOrUpdateVote");
            }
        }
    }

    public int[] getVoteResults(String sessionName, String sessionDate) {
        Log.d("DB_DEBUG", "getVoteResults START -> sessionName: " + sessionName +
                ", sessionDate: " + sessionDate);

        int[] results = new int[3]; // [yes, no, abstain]
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