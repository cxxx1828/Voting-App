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

/**
 * DecideItDbHelper - Glavna klasa za upravljanje SQLite bazom podataka i HTTP komunikacijom
 * Nasleđuje SQLiteOpenHelper što omogućava lakše kreiranje i upravljanje SQLite bazom
 * Kombinuje lokalne database operacije sa sinhronizacijom preko HTTP servera
 * Služi kao "bridge" između aplikacije, lokalne baze i remote servera
 */
public class DecideItDbHelper extends SQLiteOpenHelper {


    private static final String TAG = "DB_DEBUG";

    private HttpHelper httpHelper;



    // Tabela USERS - čuva podatke o korisnicima (studenti i administratori)
    private final String TABLE_USERS = "USERS";
    public static final String COL_NAME = "Name";           // Ime korisnika
    public static final String COL_SURNAME = "Surname";     // Prezime korisnika
    public static final String COL_USERNAME = "Username";   // Jedinstveno korisničko ime
    public static final String COL_INDEX = "IndexNumber";   // Broj indeksa (za studente)
    public static final String COL_PASSWORD = "Password";   // Hashovan password
    public static final String COL_ROLE = "Role";           // Uloga: "student" ili "admin"

    // Tabela SESSIONS - čuva podatke o sesijama za glasanje
    private final String TABLE_SESSIONS = "SESSIONS";
    public static final String COL_DATE = "Date";                    // Datum sesije (dd.MM.yyyy)
    public static final String COL_SESSION_NAME = "SessionName";     // Naziv sesije
    public static final String COL_DESCRIPTION = "Description";      // Opis sesije
    public static final String COL_END_TIME = "EndTime";            // Vreme završetka glasanja
    public static final String COL_SERVER_ID = "ServerId";          // MongoDB _id sa servera

    // Tabela VOTES - čuva rezultate glasanja za svaku sesiju
    private final String TABLE_VOTES = "VOTES";
    public static final String COL_YES = "YesVotes";              // Broj YES glasova
    public static final String COL_NO = "NoVotes";                // Broj NO glasova
    public static final String COL_ABSTAIN = "AbstainVotes";      // Broj ABSTAIN glasova
    public static final String COL_VOTE_SESSION_NAME = "SessionName";     // Naziv sesije
    public static final String COL_VOTE_DATE = "SessionDate";             // Datum sesije
    public static final String COL_VOTE_SERVER_ID = "ServerSessionId";    // MongoDB session _id

    /**
     * Konstruktor DecideItDbHelper klase
     * @param context - Android kontekst (obično Activity ili Fragment)
     * @param name - naziv database fajla
     * @param factory - CursorFactory (obično null)
     * @param version - verzija baze podataka (važno za migracije)
     */
    public DecideItDbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        // Inicijalizuje HttpHelper za komunikaciju sa serverom
        httpHelper = new HttpHelper();
    }

    /**
     * Poziva se kada se baza kreira prvi put
     * Ovde se definišu sve tabele i njihova struktura
     * Ova metoda se izvršava samo jednom - pri prvom pokretanju aplikacije
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating tables");

        // USERS tabela - kreira tabelu za korisnike
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_NAME + " TEXT, " +                    // Ime kao tekst
                COL_SURNAME + " TEXT, " +                 // Prezime kao tekst
                COL_USERNAME + " TEXT UNIQUE, " +         // Username mora biti jedinstven
                COL_INDEX + " TEXT, " +                   // Broj indeksa kao tekst
                COL_PASSWORD + " TEXT, " +                // Hashovan password
                COL_ROLE + " TEXT);");                    // Uloga korisnika
        Log.d(TAG, "Table USERS created");

        // SESSIONS tabela - kreira tabelu za sesije
        // dodana kolona za server ID
        db.execSQL("CREATE TABLE " + TABLE_SESSIONS + " (" +
                COL_DATE + " TEXT, " +                    // Datum sesije
                COL_SESSION_NAME + " TEXT, " +            // Naziv sesije
                COL_DESCRIPTION + " TEXT, " +             // Opis sesije
                COL_END_TIME + " TEXT, " +                // Vreme završetka
                COL_SERVER_ID + " TEXT, " +               // MongoDB ObjectId
                // UNIQUE constraint sprečava duplikate sesija za isti datum i naziv
                "UNIQUE(" + COL_DATE + ", " + COL_SESSION_NAME + "));");
        Log.d(TAG, "Table SESSIONS created");

        // VOTES tabela - kreira tabelu za glasove
        //dodana kolona za server session ID
        db.execSQL("CREATE TABLE " + TABLE_VOTES + " (" +
                COL_YES + " INTEGER, " +                  // Broj DA glasova (brojevi)
                COL_NO + " INTEGER, " +                   // Broj NE glasova
                COL_ABSTAIN + " INTEGER, " +              // Broj UZDRŽAN glasova
                COL_VOTE_SESSION_NAME + " TEXT, " +       // Naziv sesije
                COL_VOTE_DATE + " TEXT, " +               // Datum sesije
                COL_VOTE_SERVER_ID + " TEXT);");          // Server ID sesije
        Log.d(TAG, "Table VOTES created");
    }

    /**
     * Poziva se kada se verzija baze podataka poveća
     * Ovde se dodaju nove kolone ili menjaju postojeće strukture
     * @param oldVersion - stara verzija baze
     * @param newVersion - nova verzija baze
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add ServerId column to SESSIONS if upgrading
        // Pokušava da doda ServerId kolonu u SESSIONS tabelu
        try {
            db.execSQL("ALTER TABLE " + TABLE_SESSIONS + " ADD COLUMN " + COL_SERVER_ID + " TEXT");
            Log.d(TAG, "Added ServerId column to SESSIONS");
        } catch (Exception e) {
            // Hvata grešku ako kolona već postoji - ovo je normalno
            Log.d(TAG, "ServerId column already exists or error adding it");
        }

        // Isto za VOTES tabelu - dodaje ServerSessionId kolonu
        try {
            db.execSQL("ALTER TABLE " + TABLE_VOTES + " ADD COLUMN " + COL_VOTE_SERVER_ID + " TEXT");
            Log.d(TAG, "Added ServerSessionId column to VOTES");
        } catch (Exception e) {
            Log.d(TAG, "ServerSessionId column already exists or error adding it");
        }
    }

    // =================== HTTP  ===================
    // Ove metode kombinuju HTTP zahteve sa lokalnim database operacijama

    /**
     * Sinhronizuje sesije sa servera u lokalnu bazu podataka
     * Šalje GET zahtev serveru, parsira JSON odgovor i ažurira SQLite bazu
     * Syncs sessions from server to local database
     * @return true ako je sinhronizacija uspešna, false ako nije
     */
    public boolean syncSessionsFromServer() {
        try {
            Log.d(TAG, "Syncing sessions from server...");
            // Šalje GET zahtev na /sessions endpoint
            JSONArray sessions = httpHelper.getJSONArrayFromURL(HttpHelper.BASE_URL + "/sessions");

            // Proverava da li je server odgovorio sa podacima
            if (sessions == null) {
                Log.e(TAG, "Failed to get sessions from server");
                return false; // Server nije odgovorio ili je bio problem sa mrežom
            }


            SQLiteDatabase db = getWritableDatabase();

            // Prolazi kroz sve sesije dobijene sa servera
            for (int i = 0; i < sessions.length(); i++) {
                // Parsira svaku sesiju kao JSON objekat
                JSONObject session = sessions.getJSONObject(i);

                // Izvlači podatke iz JSON objekta
                String serverId = session.getString("_id");                    // MongoDB ObjectId
                String sessionName = session.getString("sessionName");        // Naziv sesije
                String description = session.optString("description", "");    // Opis (ili prazan string)
                String dateStr = session.getString("date");                   // datum
                String endTimeStr = session.getString("endOfVotingTime");     // vreme završetka

                // Konvertuje ISO datum (2025-09-20T10:00:00.000Z) u naš format (20.09.2025)
                String formattedDate = formatDateFromISO(dateStr);


                // Proverava da li sesija već postoji u lokalnoj bazi
                Cursor cursor = db.query(TABLE_SESSIONS, null,
                        COL_SERVER_ID + " =?", new String[]{serverId}, null, null, null);

                // Priprema podatke za insert/update
                ContentValues values = new ContentValues();
                values.put(COL_DATE, formattedDate);      // Formatiran datum
                values.put(COL_SESSION_NAME, sessionName); // Naziv
                values.put(COL_DESCRIPTION, description);  // Opis
                values.put(COL_END_TIME, endTimeStr);     // ISO vreme završetka
                values.put(COL_SERVER_ID, serverId);      // Server ID za buduće reference

                // Proverava da li sesija već postoji
                if (cursor.getCount() > 0) {
                    // Update existing - ažurira postojeću sesiju
                    db.update(TABLE_SESSIONS, values, COL_SERVER_ID + " =?", new String[]{serverId});
                    Log.d(TAG, "Updated session: " + sessionName);
                } else {
                    // Insert new - dodaje novu sesiju
                    db.insert(TABLE_SESSIONS, null, values);
                    Log.d(TAG, "Inserted new session: " + sessionName);
                }
                cursor.close();
            }

            db.close();
            Log.d(TAG, "Successfully synced " + sessions.length() + " sessions from server");
            return true; // Uspešna sinhronizacija

        } catch (Exception e) {

            Log.e(TAG, "Error syncing sessions from server", e);
            return false; // Neuspešna sinhronizacija
        }
    }

    /**
     * Kreira novu sesiju na serveru i ažurira lokalnu bazu
     * Šalje POST zahtev serveru, a zatim čuva odgovor u lokalnoj bazi
     * Creates session on server and updates local database
     * @param date - datum sesije u dd.MM.yyyy formatu
     * @param sessionName - naziv sesije
     * @param description - opis sesije
     * @return true ako je kreiranje uspešno, false ako nije
     */
    public boolean createSessionOnServer(String date, String sessionName, String description) {
        try {
            Log.d(TAG, "Creating session on server: " + sessionName);

            // Konvertuje datum iz dd.MM.yyyy u ISO format (2025-09-20T00:00:00.000Z)
            String isoDate = formatDateToISO(date);
            String endTime = formatDateToISO(date, 3); // 3 sata kasnije

            // Kreira JSON objekat sa podacima sesije
            JSONObject sessionData = new JSONObject();
            sessionData.put("date", isoDate);                 // ISO datum
            sessionData.put("sessionName", sessionName);      // Naziv
            sessionData.put("description", description);      // Opis
            sessionData.put("endOfVotingTime", endTime);      // Vreme završetka (3h kasnije)

            // Šalje POST zahtev serveru
            JSONObject response = httpHelper.postJSONObjectFromURL(HttpHelper.BASE_URL + "/session", sessionData);

            // Proverava da li je server uspešno kreirao sesiju
            if (response == null) {
                Log.e(TAG, "Failed to create session on server");
                return false;
            }

            // Dobija podatke o kreiranoj sesiji iz server odgovora
            JSONObject createdSession = response.getJSONObject("session");
            String serverId = createdSession.getString("_id"); // MongoDB ID nove sesije


            // Čuva novu sesiju u lokalnu bazu sa server ID-jem
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_DATE, date);                // Originalnu format datuma
            values.put(COL_SESSION_NAME, sessionName); // Naziv sesije
            values.put(COL_DESCRIPTION, description);  // Opis
            values.put(COL_END_TIME, endTime);        // ISO vreme završetka
            values.put(COL_SERVER_ID, serverId);      // Server ID za sinhronizaciju

            long result = db.insert(TABLE_SESSIONS, null, values);
            db.close();

            Log.d(TAG, "Successfully created session on server with ID: " + serverId);
            return result != -1; // -1 znači da insert nije uspeo

        } catch (Exception e) {
            Log.e(TAG, "Error creating session on server", e);
            return false;
        }
    }

    /**
     * Sinhronizuje glasove za specifičnu sesiju sa servera
     * Dobija najnovije brojke glasova sa servera i ažurira lokalnu bazu
     * Syncs votes for a specific session from server
     * @param sessionServerId - MongoDB ID sesije
     * @return true ako je sinhronizacija uspešna, false ako nije
     */
    public boolean syncVotesFromServer(String sessionServerId) {
        try {
            Log.d(TAG, "Syncing votes from server for session: " + sessionServerId);

            // Šalje GET zahtev sa query parametrom sessionId
            JSONArray votes = httpHelper.getJSONArrayFromURL(HttpHelper.BASE_URL + "/votes?sessionId=" + sessionServerId);

            // Proverava da li postoje glasovi za ovu sesiju
            if (votes == null || votes.length() == 0) {
                Log.d(TAG, "No votes found on server for session: " + sessionServerId);
                return true; // nije greška samo još nema glasova
            }

            //trebao bi biti samo jedan zapis glasova po sesiji
            JSONObject vote = votes.getJSONObject(0);

            // Parsira brojke glasova iz JSON-a
            int yes = vote.getInt("yes");                                    // Broj DA glasova
            int no = vote.getInt("no");                                      // Broj NE glasova
            int abstain = vote.getInt("abstain");                           // Broj UZDRŽAN glasova
            String sessionName = vote.getString("sessionName");             // Naziv sesije
            String sessionDate = formatDateFromISO(vote.getString("sessionDate")); // Konvertuje datum

            SQLiteDatabase db = getWritableDatabase();

            // Check if vote record exists - proverava da li već postoji zapis glasova
            Cursor cursor = db.query(TABLE_VOTES, null,
                    COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId}, null, null, null);

            // Priprema podatke za update/insert
            ContentValues values = new ContentValues();
            values.put(COL_YES, yes);                          // Broj DA glasova
            values.put(COL_NO, no);                            // Broj NE glasova
            values.put(COL_ABSTAIN, abstain);                  // Broj UZDRŽAN glasova
            values.put(COL_VOTE_SESSION_NAME, sessionName);    // Naziv sesije
            values.put(COL_VOTE_DATE, sessionDate);            // Datum sesije
            values.put(COL_VOTE_SERVER_ID, sessionServerId);   // Server ID za referencu

            if (cursor.getCount() > 0) {
                // ažurira postojeći zapis
                db.update(TABLE_VOTES, values, COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId});
                Log.d(TAG, "Updated votes for session: " + sessionName);
            } else {
                // kreira novi zapis glasova
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
     * Šalje glas serveru i ažurira lokalnu bazu
     * Koristi se kada student glasa - šalje POST zahtev i ažurira lokalne brojke
     * Submits vote to server and updates local database
     * @param sessionServerId - MongoDB ID sesije
     * @param sessionName - naziv sesije
     * @param sessionDate - datum sesije
     * @param voteType - tip glasa: 1=yes, 2=no, 3=abstain
     * @return true ako je glasanje uspešno, false ako nije
     */
    public boolean submitVoteToServer(String sessionServerId, String sessionName, String sessionDate, int voteType) {
        try {
            Log.d(TAG, "Submitting vote to server: " + voteType + " for session: " + sessionServerId);

            // Konvertuje numerički tip glasa u string za server
            String voteString;
            switch (voteType) {
                case 1: voteString = "yes"; break;      // DA glas
                case 2: voteString = "no"; break;       // NE glas
                case 3: voteString = "abstain"; break;  // UZDRŽAN glas
                default:
                    Log.e(TAG, "Invalid vote type: " + voteType);
                    return false; // nije okej
            }

            // Kreira JSON objekat sa podacima o glasu
            JSONObject voteData = new JSONObject();
            voteData.put("sessionId", sessionServerId); // MongoDB ID sesije
            voteData.put("vote", voteString);           // String reprezentacija glasa

            // Šalje POST zahtev serveru na /results/vote endpoint
            JSONObject response = httpHelper.postJSONObjectFromURL(HttpHelper.BASE_URL + "/results/vote", voteData);

            // Proverava da li je server uspešno zabeležio glas
            if (response == null) {
                Log.e(TAG, "Failed to submit vote to server");
                return false;
            }

            // Ažurira lokalnu bazu sa novim brojem glasova
            JSONObject votes = response.getJSONObject("votes");
            int yes = votes.getInt("yes");         // Nova brojka DA glasova
            int no = votes.getInt("no");           // Nova brojka NE glasova
            int abstain = votes.getInt("abstain"); // Nova brojka UZDRŽAN glasova

            SQLiteDatabase db = getWritableDatabase();

            //  ažurira ili kreira zapis glasova
            Cursor cursor = db.query(TABLE_VOTES, null,
                    COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId}, null, null, null);

            ContentValues values = new ContentValues();
            values.put(COL_YES, yes);                          // Nove brojke sa servera
            values.put(COL_NO, no);
            values.put(COL_ABSTAIN, abstain);
            values.put(COL_VOTE_SESSION_NAME, sessionName);
            values.put(COL_VOTE_DATE, sessionDate);
            values.put(COL_VOTE_SERVER_ID, sessionServerId);

            if (cursor.getCount() > 0) {
                // Ažurira postojeći zapis
                db.update(TABLE_VOTES, values, COL_VOTE_SERVER_ID + " =?", new String[]{sessionServerId});
            } else {
                // Kreira novi zapis
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
     * Dobija server ID za sesiju na osnovu datuma i naziva
     * Potrebno za mapiranje između lokalne i server baze
     * Gets server ID for a session by date and name
     * @param sessionName - naziv sesije
     * @param sessionDate - datum sesije
     * @return MongoDB ID sesije ili null ako ne postoji
     */
    public String getSessionServerId(String sessionName, String sessionDate) {
        SQLiteDatabase db = getReadableDatabase();
        // Traži samo ServerId kolonu za određenu sesiju
        Cursor cursor = db.query(TABLE_SESSIONS, new String[]{COL_SERVER_ID},
                COL_SESSION_NAME + " =? AND " + COL_DATE + " =?",
                new String[]{sessionName, sessionDate}, null, null, null);

        String serverId = null;
        if (cursor.moveToFirst()) {
            // Dobija server ID ako sesija postoji
            serverId = cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVER_ID));
        }

        cursor.close();
        db.close();
        return serverId; // Vraća MongoDB ID ili null
    }


    // Helper metode za konverziju između naših datuma (dd.MM.yyyy) i ISO formata

    /**
     * Konvertuje ISO datum u naš display format
     * Iz "2025-09-20T10:00:00.000Z" u "20.09.2025"
     * @param isoDate - datum u ISO formatu
     * @return datum u dd.MM.yyyy formatu
     */
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

    /**
     * Konvertuje naš datum u ISO format bez dodavanja sati
     * @param displayDate - datum u dd.MM.yyyy formatu
     * @return datum u ISO formatu
     */
    private String formatDateToISO(String displayDate) {
        return formatDateToISO(displayDate, 0); // Poziva preoptereću metodu bez dodavanja sati
    }

    /**
     * Konvertuje naš datum u ISO format sa opcijom dodavanja sati
     * Iz "20.09.2025" u "2025-09-20T03:00:00.000Z" (ako hoursToAdd = 3)
     * @param displayDate - datum u dd.MM.yyyy formatu
     * @param hoursToAdd - broj sati za dodavanje (za endOfVotingTime)
     * @return datum u ISO formatu
     */
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


    // Postojeće metode koje čuvaju kompatibilnost sa starijim delovima aplikacije

    /**
     * Kreira SHA-256 hash od password-a
     * Koristi se za bezbedno čuvanje password-a u bazi
     * @param password - plaintext password
     * @return heksadecimalni string hash-a
     */
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

    /**
     * Dodaje novog korisnika u bazu podataka
     * @param name - ime korisnika
     * @param surname - prezime korisnika
     * @param username - jedinstveno korisničko ime
     * @param index - broj indeksa
     * @param password - plaintext password (biće hashovan)
     * @param role - uloga korisnika ("student" ili "admin")
     * @return true ako je korisnik uspešno dodat
     */
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

    /**
     * Autentifikuje korisnika proverom username/password kombinacije
     * @param username - korisničko ime
     * @param password - plaintext password
     * @return String array sa [ime, prezime, uloga] ili null ako autentifikacija ne uspe
     */
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

    /**
     * Proverava da li username već postoji u bazi
     * Koristi se pre registracije novog korisnika
     * @param username - korisničko ime za proveru
     * @return true ako username postoji, false ako ne postoji
     */
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

    /**
     * Čita sve studente iz baze podataka
     * Filtrira korisnike po role = "student"
     * @return array Student objekata ili null ako nema studenata
     */
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

    /**
     * Briše studenta iz baze podataka
     * @param username - korisničko ime studenta za brisanje
     */
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

    /**
     * Helper metoda za kreiranje Student objekta iz Cursor-a
     * @param cursor - database cursor pozicioniran na red sa student podacima
     * @return Student objekat kreiran od podataka iz baze
     */
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

    /**
     * Wrapper metoda za kreiranje sesije - koristi novu HTTP metodu
     * Održava kompatibilnost sa starim kodom
     * @param date - datum sesije
     * @param sessionName - naziv sesije
     * @param description - opis sesije
     * @return true ako je kreiranje uspešno
     */
    public boolean insertSession(String date, String sessionName, String description) {
        //koristi novu HTTP metodu
        return createSessionOnServer(date, sessionName, description);
    }

    /**
     * Čita sve sesije iz lokalne baze podataka
     * @return array Session objekata ili null ako nema sesija
     */
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

    /**
     * Dobija sve datume za koje postoje sesije
     * Koristi se u CalendarFragment za proveru dostupnosti
     * @return ArrayList stringova sa datumima u dd.MM.yyyy formatu
     */
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

    /**
     * Dobija sesiju za određeni datum
     * @param date - datum sesije u dd.MM.yyyy formatu
     * @return Session objekat ili null ako sesija ne postoji
     */
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

    /**
     * Helper metoda za kreiranje Session objekta iz Cursor-a
     * Takođe određuje status sesije (UPCOMING vs PAST) na osnovu vremena završetka
     * @param cursor - database cursor pozicioniran na red sa session podacima
     * @return Session objekat sa podacima iz baze
     */
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

    /**
     * Glavna metoda za dodavanje ili ažuriranje glasa
     * Pokušava prvo da pošalje glas serveru, zatim ažurira lokalnu bazu
     * @param sessionName - naziv sesije
     * @param sessionDate - datum sesije
     * @param voteType - tip glasa (1=yes, 2=no, 3=abstain)
     * @return true ako je glasanje uspešno
     */
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


        // Fallback na lokalno ažuriranje ako server zahtev ne uspe
        return insertOrUpdateVoteLocal(sessionName, sessionDate, voteType);
    }

    /**
     * Lokalna metoda za ažuriranje glasova (bez server komunikacije)
     * Koristi se kao fallback kada server nije dostupan
     * @param sessionName - naziv sesije
     * @param sessionDate - datum sesije
     * @param voteType - tip glasa
     * @return true ako je lokalno ažuriranje uspešno
     */
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

    /**
     * Dobija rezultate glasanja za određenu sesiju
     * Prvo pokušava sinhronizaciju sa servera, zatim čita iz lokalne baze
     * @param sessionName - naziv sesije
     * @param sessionDate - datum sesije
     * @return int array sa [yes, no, abstain] brojevima
     */
    public int[] getVoteResults(String sessionName, String sessionDate) {
        // prvo pokušava sinhronizaciju sa servera
        String serverId = getSessionServerId(sessionName, sessionDate);
        if (serverId != null) {
            // Sinhronizuje najnovije glasove sa servera
            syncVotesFromServer(serverId);
        }

        Log.d("DB_DEBUG", "getVoteResults START -> sessionName: " + sessionName +
                ", sessionDate: " + sessionDate);

        // Inicijalizuje rezultate na 0
        int[] results = new int[3]; // [yes, no, abstain]
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = getReadableDatabase();
            // Traži glasove za specifičnu sesiju
            cursor = db.query(TABLE_VOTES, null,
                    COL_VOTE_SESSION_NAME + " =? AND " + COL_VOTE_DATE + " =?",
                    new String[]{sessionName, sessionDate}, null, null, null);

            if (cursor.moveToFirst()) {
                // Čita brojke glasova iz baze
                results[0] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_YES));      // YES glasovi
                results[1] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NO));       // NO glasovi
                results[2] = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ABSTAIN));  // ABSTAIN glasovi

                Log.d("DB_DEBUG", "Vote results -> YES: " + results[0] +
                        ", NO: " + results[1] + ", ABSTAIN: " + results[2]);
            } else {
                Log.d("DB_DEBUG", "No votes found for this session/date");
                // results array ostaje sa default vrednostima [0, 0, 0]
            }
        } catch (Exception e) {
            Log.e("DB_DEBUG", "Error in getVoteResults", e);
        } finally {
            // Zatvaranje resursa
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