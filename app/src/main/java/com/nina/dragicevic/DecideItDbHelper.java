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



    private final String TABLE_USERS = "USERS";
    public static final String COL_NAME = "Name";           
    public static final String COL_SURNAME = "Surname";     
    public static final String COL_USERNAME = "Username";   
    public static final String COL_INDEX = "IndexNumber";   
    public static final String COL_PASSWORD = "Password";   
    public static final String COL_ROLE = "Role";           

    private final String TABLE_SESSIONS = "SESSIONS";
    public static final String COL_DATE = "Date";                    // Datum sesije (dd.MM.yyyy)
    public static final String COL_SESSION_NAME = "SessionName";     // Naziv sesije
    public static final String COL_DESCRIPTION = "Description";      // Opis sesije
    public static final String COL_END_TIME = "EndTime";            // Vreme završetka glasanja
    public static final String COL_SERVER_ID = "ServerId";          // MongoDB _id sa servera

    private final String TABLE_VOTES = "VOTES";
    public static final String COL_YES = "YesVotes";              // Broj YES glasova
    public static final String COL_NO = "NoVotes";                // Broj NO glasova
    public static final String COL_ABSTAIN = "AbstainVotes";      // Broj ABSTAIN glasova
    public static final String COL_VOTE_SESSION_NAME = "SessionName";     // Naziv sesije
    public static final String COL_VOTE_DATE = "SessionDate";             // Datum sesije
    public static final String COL_VOTE_SERVER_ID = "ServerSessionId";    // MongoDB session _id

   
    public DecideItDbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        httpHelper = new HttpHelper();
    }

  
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating tables");

        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_NAME + " TEXT, " +                    // Ime kao tekst
                COL_SURNAME + " TEXT, " +                 // Prezime kao tekst
                COL_USERNAME + " TEXT UNIQUE, " +         // Username mora biti jedinstven
                COL_INDEX + " TEXT, " +                   // Broj indeksa kao tekst
                COL_PASSWORD + " TEXT, " +                // Hashovan password
                COL_ROLE + " TEXT);");                    // Uloga korisnika
        Log.d(TAG, "Table USERS created");

      
        db.execSQL("CREATE TABLE " + TABLE_SESSIONS + " (" +
                COL_DATE + " TEXT, " +                    // Datum sesije
                COL_SESSION_NAME + " TEXT, " +            // Naziv sesije
                COL_DESCRIPTION + " TEXT, " +             // Opis sesije
                COL_END_TIME + " TEXT, " +                // Vreme završetka
                COL_SERVER_ID + " TEXT, " +               // MongoDB ObjectId
                // UNIQUE constraint sprečava duplikate sesija za isti datum i naziv
                "UNIQUE(" + COL_DATE + ", " + COL_SESSION_NAME + "));");
        Log.d(TAG, "Table SESSIONS created");

        db.execSQL("CREATE TABLE " + TABLE_VOTES + " (" +
                COL_YES + " INTEGER, " +                  // Broj DA glasova (brojevi)
                COL_NO + " INTEGER, " +                   // Broj NE glasova
                COL_ABSTAIN + " INTEGER, " +              // Broj UZDRŽAN glasova
                COL_VOTE_SESSION_NAME + " TEXT, " +       // Naziv sesije
                COL_VOTE_DATE + " TEXT, " +               // Datum sesije
                COL_VOTE_SERVER_ID + " TEXT);");          // Server ID sesije
        Log.d(TAG, "Table VOTES created");
    }

  
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Pokušava da doda ServerId kolonu u SESSIONS tabelu
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

  
    public boolean syncSessionsFromServer() {
        try {
            Log.d(TAG, "Syncing sessions from server...");
            JSONArray sessions = httpHelper.getJSONArrayFromURL(HttpHelper.BASE_URL + "/sessions");

            if (sessions == null) {
                Log.e(TAG, "Failed to get sessions from server");
                return false; // Server nije odgovorio ili je bio problem sa mrežom
            }


            SQLiteDatabase db = getWritableDatabase();

            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.getJSONObject(i);

                String serverId = session.getString("_id");                    // MongoDB ObjectId
                String sessionName = session.getString("sessionName");        // Naziv sesije
                String description = session.optString("description", "");    // Opis (ili prazan string)
                String dateStr = session.getString("date");                   // datum
                String endTimeStr = session.getString("endOfVotingTime");     // vreme završetka

                String formattedDate = formatDateFromISO(dateStr);


                Cursor cursor = db.query(TABLE_SESSIONS, null,
                        COL_SERVER_ID + " =?", new String[]{serverId}, null, null, null);

                ContentValues values = new ContentValues();
                values.put(COL_DATE, formattedDate);      // Formatiran datum
                values.put(COL_SESSION_NAME, sessionName); // Naziv
                values.put(COL_DESCRIPTION, description);  // Opis
                values.put(COL_END_TIME, endTimeStr);     // ISO vreme završetka
                values.put(COL_SERVER_ID, serverId);      // Server ID za buduće reference

                if (cursor.getCount() > 0) {
                    db.update(TABLE_SESSIONS, values, COL_SERVER_ID + " =?", new String[]{serverId});
                    Log.d(TAG, "Updated session: " + sessionName);
                } else {
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

   
    public boolean createSessionOnServer(String date, String sessionName, String description) {
        try {
            Log.d(TAG, "Creating session on server: " + sessionName);

            String isoDate = formatDateToISO(date);
            String endTime = formatDateToISO(date, 3); 

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

            JSONObject createdSession = response.getJSONObject("session");
            String serverId = createdSession.getString("_id"); 


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

  
    public boolean syncVotesFromServer(String sessionServerId) {
        try {
            Log.d(TAG, "Syncing votes from server for session: " + sessionServerId);

            JSONArray votes = httpHelper.getJSONArrayFromURL(HttpHelper.BASE_URL + "/votes?sessionId=" + sessionServerId);

            if (votes == null || votes.length() == 0) {
                Log.d(TAG, "No votes found on server for session: " + sessionServerId);
                return true;
            }

            JSONObject vote = votes.getJSONObject(0);

            int yes = vote.getInt("yes");                                    // Broj DA glasova
            int no = vote.getInt("no");                                      // Broj NE glasova
            int abstain = vote.getInt("abstain");                           // Broj UZDRŽAN glasova
            String sessionName = vote.getString("sessionName");             // Naziv sesije
            String sessionDate = formatDateFromISO(vote.getString("sessionDate")); // Konvertuje datum

            SQLiteDatabase db = getWritableDatabase();

            Cursor cursor = db.query(TABLE_VOTES, null,
                    COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId}, null, null, null);

            ContentValues values = new ContentValues();
            values.put(COL_YES, yes);                          // Broj DA glasova
            values.put(COL_NO, no);                            // Broj NE glasova
            values.put(COL_ABSTAIN, abstain);                  // Broj UZDRŽAN glasova
            values.put(COL_VOTE_SESSION_NAME, sessionName);    // Naziv sesije
            values.put(COL_VOTE_DATE, sessionDate);            // Datum sesije
            values.put(COL_VOTE_SERVER_ID, sessionServerId);   // Server ID za referencu

            if (cursor.getCount() > 0) {
                db.update(TABLE_VOTES, values, COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId});
                Log.d(TAG, "Updated votes for session: " + sessionName);
            } else {
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

   
    public boolean submitVoteToServer(String sessionServerId, String sessionName, String sessionDate, int voteType) {
        try {
            Log.d(TAG, "Submitting vote to server: " + voteType + " for session: " + sessionServerId);

            String voteString;
            switch (voteType) {
                case 1: voteString = "yes"; break;      // DA glas
                case 2: voteString = "no"; break;       // NE glas
                case 3: voteString = "abstain"; break;  // UZDRŽAN glas
                default:
                    Log.e(TAG, "Invalid vote type: " + voteType);
                    return false; // nije okej
            }

            JSONObject voteData = new JSONObject();
            voteData.put("sessionId", sessionServerId); // MongoDB ID sesije
            voteData.put("vote", voteString);           // String reprezentacija glasa

            JSONObject response = httpHelper.postJSONObjectFromURL(HttpHelper.BASE_URL + "/results/vote", voteData);

            if (response == null) {
                Log.e(TAG, "Failed to submit vote to server");
                return false;
            }

            JSONObject votes = response.getJSONObject("votes");
            int yes = votes.getInt("yes");         // Nova brojka DA glasova
            int no = votes.getInt("no");           // Nova brojka NE glasova
            int abstain = votes.getInt("abstain"); // Nova brojka UZDRŽAN glasova

            SQLiteDatabase db = getWritableDatabase();

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


    private String formatDateFromISO(String isoDate) {
        try {
            // parsiraj ISO datum: "2025-09-20T10:00:00.000Z"
            // Parser za ISO format datuma
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            // parser za naš display format
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            // Parsira ISO string u Date objekat
            Date date = isoFormat.parse(isoDate);
            // Konvertuje Date objekat u naš format
            return displayFormat.format(date);
        } catch (Exception e) {
            // Ako parsiranje ne uspe, vraća originalni string
            Log.e(TAG, "Error parsing ISO date: " + isoDate, e);
            return isoDate;
        }
    }

   
    private String formatDateToISO(String displayDate) {
        return formatDateToISO(displayDate, 0); 
    }

   
    private String formatDateToISO(String displayDate, int hoursToAdd) {
        try {
            // parsiraj display date: "20.09.2025"
            // Parser za naš format
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            // Parser za ISO format
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            // Parsira naš string u Date objekat
            Date date = displayFormat.parse(displayDate);

            // dodaje sate ako je specificcano
            if (hoursToAdd > 0) {
                // Dodaje milisekunde (sati * 60 min * 60 sec * 1000 ms)
                date = new Date(date.getTime() + hoursToAdd * 60 * 60 * 1000);
            }

            // Konvertuje Date objekat u ISO string
            return isoFormat.format(date);
        } catch (Exception e) {
            // Ako konverzija ne uspe, vraća originalni string
            Log.e(TAG, "Error formatting date to ISO: " + displayDate, e);
            return displayDate;
        }
    }


    
    private String hashPassword(String password) {
        try {
            // Kreira SHA-256 MessageDigest objekat
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Konvertuje hash bytes u heksadecimalni string
            String hashed = HexFormat.of().formatHex(md.digest(password.getBytes()));
            Log.d(TAG, "Password hashed: " + hashed);
            return hashed;
        } catch (Exception e) {
            Log.e(TAG, "Error hashing password", e);
            // Baca RuntimeException jer password hashing ne sme da ne uspe
            throw new RuntimeException(e);
        }
    }

   
    public boolean insertUser(String name, String surname, String username, String index, String password, String role) {
        SQLiteDatabase db = null;
        try {
            Log.d(TAG, "Inserting user: " + username);
            db = getWritableDatabase();

            // Priprema podatke za insert
            ContentValues values = new ContentValues();
            values.put(COL_NAME, name);
            values.put(COL_SURNAME, surname);
            values.put(COL_USERNAME, username);
            values.put(COL_INDEX, index);
            values.put(COL_PASSWORD, hashPassword(password)); // Hash password pre čuvanja
            values.put(COL_ROLE, role);

            // Dodaje korisnika u tabelu
            db.insert(TABLE_USERS, null, values);
            Log.d(TAG, "User insert result: " + username);

            return true;

        } catch (Exception e) {
            // Hvata greške (duplicate username, database greške, itd.)
            Log.e(TAG, "Error inserting user", e);
            return false;

        }finally {
            // Finally blok osigurava da se database uvek zatvori
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
            // Hash password za poređenje sa onim u bazi
            String hashedPassword = hashPassword(password);

            // Traži korisnika sa matching username i password hash
            cursor = db.query(TABLE_USERS, null,
                    COL_USERNAME + " =? AND " + COL_PASSWORD + " =?",
                    new String[]{username, hashedPassword}, null, null, null);

            String[] userInfo = null;
            if (cursor.moveToFirst()) {
                // Ako je korisnik pronađen, vraća osnovne informacije
                userInfo = new String[3];
                userInfo[0] = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));      // Ime
                userInfo[1] = cursor.getString(cursor.getColumnIndexOrThrow(COL_SURNAME));   // Prezime
                userInfo[2] = cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLE));      // Uloga
                Log.d(TAG, "User authenticated: " + userInfo[0] + " " + userInfo[1] + " Role: " + userInfo[2]);
            } else {
                Log.d(TAG, "Authentication failed for user: " + username);
            }
            return userInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error in authenticateUser", e);
            return null;
        } finally {
            // Zatvaranje resursa u finally bloku
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
            // Traži korisnika sa datim username-om
            cursor = db.query(TABLE_USERS, null, COL_USERNAME + " =?", new String[]{username}, null, null, null);

            // Ako postoji bar jedan red, username je zauzet
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
            // Selektuje samo korisnike sa role = "student"
            cursor = db.query(TABLE_USERS, null, COL_ROLE + " =?", new String[]{"student"}, null, null, null);

            // Proverava da li postoje studenti
            if (cursor.getCount() <= 0) {
                Log.d(TAG, "No students found");
                return null;
            }

            // Kreira array Student objekata
            Student[] students = new Student[cursor.getCount()];
            int i = 0;
            // Prolazi kroz sve redove i kreira Student objekte
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
            // Briše korisnika sa datim username-om
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
        // Čita podatke iz trenutnog reda cursor-a
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
        String surname = cursor.getString(cursor.getColumnIndexOrThrow(COL_SURNAME));
        String index = cursor.getString(cursor.getColumnIndexOrThrow(COL_INDEX));
        String username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME));

        // Defaultna slika za studente - ovde bi mogla biti logika za različite slike
        int imageResId = R.drawable.male_student;

        Log.d(TAG, "Created student from cursor: " + name + " " + surname + " Index: " + index);

        // Kreira i vraća Student objekat
        return new Student(imageResId, name, surname, index, false, username);
    }

  
    public boolean insertSession(String date, String sessionName, String description) {
        //koristi novu HTTP metodu
        return createSessionOnServer(date, sessionName, description);
    }

    
    public Session[] readSessions() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d(TAG, "Reading all sessions");
            db = getReadableDatabase();
            // Selektuje sve sesije iz tabele
            cursor = db.query(TABLE_SESSIONS, null, null, null, null, null, null);

            if (cursor.getCount() <= 0) {
                Log.d(TAG, "No sessions found");
                return null;
            }

            // Kreira array Session objekata
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
            // Selektuje samo Date kolonu iz svih sesija
            cursor = db.query(TABLE_SESSIONS, new String[]{COL_DATE}, null, null, null, null, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                dates.add(date);
                Log.d(TAG, "Found session date: " + date);
            }
            return dates;
        } catch (Exception e) {
            Log.e(TAG, "Error getting session dates", e);
            return dates; // Vraća praznu listu u slučaju greške
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
            // Traži sesiju sa određenim datumom
            cursor = db.query(TABLE_SESSIONS, null, COL_DATE + " =?", new String[]{date}, null, null, null);

            if (cursor.moveToFirst()) {
                // Kreira Session objekat iz pronađenih podataka
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
        // Čita osnovne podatke iz cursor-a
        String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
        String sessionName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_NAME));
        String endTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_END_TIME));

        // Dobija trenutno vreme za poređenje
        long currentTime = System.currentTimeMillis();
        String status;

        try {
            // Try to parse as ISO format first - pokušava da parsira kao ISO format
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date endTime = isoFormat.parse(endTimeStr);
            // Poredi sa trenutnim vremenom da odredi status
            status = (endTime.getTime() > currentTime) ? "UPCOMING" : "PAST";
        } catch (Exception e) {
            // Fallback to old format or default - pokušava stari format ili postavlja default
            try {
                // Pokušava da parsira kao timestamp (stari format)
                long endTime = Long.parseLong(endTimeStr);
                status = (endTime > currentTime) ? "UPCOMING" : "PAST";
            } catch (NumberFormatException ex) {
                status = "UPCOMING"; // Default status ako parsiranje ne uspe
            }
        }

        Log.d(TAG, "Creating session object -> name: " + sessionName + ", date: " + date + ", status: " + status);

        // Kreira i vraća Session objekat
        return new Session(date, sessionName, status);
    }


    public boolean insertOrUpdateVote(String sessionName, String sessionDate, int voteType) {
        // dobija server ID za ovu sesiju
        String serverId = getSessionServerId(sessionName, sessionDate);

        if (serverId != null) {
            // prvo pokušava da pošalje serveru
            boolean serverSuccess = submitVoteToServer(serverId, sessionName, sessionDate, voteType);
            if (serverSuccess) {
                return true;
                // Lokalna baza je već ažurirana u submitVoteToServer metodi
            }
        }


        return insertOrUpdateVoteLocal(sessionName, sessionDate, voteType);
    }

    
    private boolean insertOrUpdateVoteLocal(String sessionName, String sessionDate, int voteType) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Log.d("DB_DEBUG", "insertOrUpdateVoteLocal START -> sessionName: " + sessionName +
                    ", sessionDate: " + sessionDate + ", voteType: " + voteType);

            db = getWritableDatabase();
            // Traži postojeći zapis glasova za ovu sesiju
            cursor = db.query(
                    TABLE_VOTES,
                    new String[]{COL_YES, COL_NO, COL_ABSTAIN}, // Selektuje samo brojke glasova
                    COL_VOTE_SESSION_NAME + "=? AND " + COL_VOTE_DATE + "=?",
                    new String[]{sessionName, sessionDate},
                    null, null, null
            );

            ContentValues values = new ContentValues();
            if (cursor != null && cursor.moveToFirst()) {
                // Postojeći zapis - čita trenutne brojke i uvećava odgovarajuću
                int yes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_YES));
                int no = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NO));
                int abstain = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ABSTAIN));

                Log.d("DB_DEBUG", "Existing vote counts -> YES: " + yes +
                        ", NO: " + no + ", ABSTAIN: " + abstain);

                // Uvećava odgovarajući brojač na osnovu tipa glasa
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

                // Priprema podatke za update
                values.put(COL_YES, yes);
                values.put(COL_NO, no);
                values.put(COL_ABSTAIN, abstain);

                // Ažurira postojeći zapis
                int rows = db.update(TABLE_VOTES, values,
                        COL_VOTE_SESSION_NAME + "=? AND " + COL_VOTE_DATE + "=?",
                        new String[]{sessionName, sessionDate});

                Log.d("DB_DEBUG", "Updated rows: " + rows);
                return rows > 0;
            } else {
                // Novi zapis - postavlja odgovarajući brojač na 1, ostale na 0
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

                // Priprema podatke za insert
                values.put(COL_YES, yes);
                values.put(COL_NO, no);
                values.put(COL_ABSTAIN, abstain);
                values.put(COL_VOTE_SESSION_NAME, sessionName);
                values.put(COL_VOTE_DATE, sessionDate);

                // Dodaje novi zapis glasova
                long id = db.insert(TABLE_VOTES, null, values);
                Log.d("DB_DEBUG", "Inserted new vote row id: " + id);
                return id != -1; // -1 znači da insert nije uspeo
            }
        } catch (Exception e) {
            Log.e("DB_DEBUG", "Error in insertOrUpdateVoteLocal", e);
            return false;
        } finally {
            // Zatvaranje resursa
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
        // prvo pokušava sinhronizaciju sa servera
        String serverId = getSessionServerId(sessionName, sessionDate);
        if (serverId != null) {
            // Sinhronizuje najnovije glasove sa servera
            syncVotesFromServer(serverId);
        }

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
                results[0] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_YES));      // YES glasovi
                results[1] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NO));       // NO glasovi
                results[2] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ABSTAIN));  // ABSTAIN glasovi

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

        return results; // Vraća [yes, no, abstain] brojke
    }
}
