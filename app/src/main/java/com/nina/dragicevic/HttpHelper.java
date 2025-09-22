package com.nina.dragicevic;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HttpHelper klasa za HTTP komunikaciju sa serverom
 * Implementira osnovne HTTP operacije: GET, POST, DELETE
 */
public class HttpHelper {

    // HTTP status kod za uspešan zahtev
    private static final int SUCCESS = 200;
    // Tag za Android Log sistem - za lakše praćenje u logovima
    private static final String TAG = "HTTP_HELPER";
    // Osnovna URL adresa servera - 10.0.2.2 predstavlja localhost na Android emulatoru
    public static final String BASE_URL = "http://10.0.2.2:8080/api"; // 10.0.2.2 for emulator, localhost for device

    /**
     * HTTP GET zahtev koji vraća JSON Array
     * Koristi se za dobavljanje liste objekata (npr. lista sesija)
     * @param urlString - puna URL adresa za zahtev
     * @return JSONArray sa podacima ili null ako je zahtev neuspešan
     */
    /*HTTP get zahtev da dobijemo listu*/
    public JSONArray getJSONArrayFromURL(String urlString) throws IOException, JSONException {
        // Referenca na HTTP konekciju - inicijalizujemo sa null
        HttpURLConnection urlConnection = null;
        try {
            // Logujemo URL na koji šaljemo zahtev - za debug potrebe
            Log.d(TAG, "GET Array request to: " + urlString);
            // Kreiramo URL objekat od string-a
            java.net.URL url = new URL(urlString);
            // Otvaramo HTTP konekciju prema serveru
            urlConnection = (HttpURLConnection) url.openConnection();

            /*header fields*/
            // Postavljamo HTTP metodu na GET
            urlConnection.setRequestMethod("GET");
            // Specificiramo da očekujemo JSON odgovor
            urlConnection.setRequestProperty("Accept", "application/json");
            // Maksimalno vreme čekanja na odgovor - 10 sekundi
            urlConnection.setReadTimeout(10000 /* milliseconds */ );
            // Maksimalno vreme za uspostavljanje konekcije - 15 sekundi
            urlConnection.setConnectTimeout(15000 /* milliseconds */ );

            // Uspostavljamo konekciju sa serverom
            urlConnection.connect();

            // Dobijamo HTTP status kod odgovora (200, 404, 500, itd.)
            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            // Proveravamo da li je zahtev uspešan (status kod 200)
            if (responseCode != SUCCESS) {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return null; // Vraćamo null ako server vraća grešku
            }

            // Kreiramo BufferedReader za čitanje odgovora od servera
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            // StringBuilder za efikasno spajanje stringova
            StringBuilder sb = new StringBuilder();
            String line;
            // Čitamo odgovor liniju po liniju
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            // Zatvaramo BufferedReader da oslobodimo resurse
            br.close();

            // Konvertujemo StringBuilder u String
            String jsonString = sb.toString();
            // Logujemo ceo JSON odgovor za debug
            Log.d(TAG, "JSON Array response: " + jsonString);

            // Parsiramo JSON string u JSONArray objekat i vraćamo ga
            return new JSONArray(jsonString);

        } catch (IOException e) {
            // Hvatamo mrežne greške (nema interneta, server nedostupan, itd.)
            Log.e(TAG, "Network error in getJSONArrayFromURL", e);
            return null; // Vraćamo null umesto da aplikacija krahira
        } finally {
            // Finally blok se UVEK izvršava, bez obzira na to da li je bilo greške
            if (urlConnection != null) {
                // Zatvaramo konekciju da oslobodimo mrežne resurse
                urlConnection.disconnect();
            }
        }
    }

    /**
     * HTTP GET zahtev koji vraća JSON Object
     * Koristi se za dobavljanje pojedinačnog objekta
     * @param urlString - puna URL adresa za zahtev
     * @return JSONObject sa podacima ili null ako je zahtev neuspešan
     */
    /*HTTP get json object*/
    public JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            // Logujemo GET zahtev za objekat
            Log.d(TAG, "GET Object request to: " + urlString);
            java.net.URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

            /*header fields*/
            // Identične postavke kao za Array metodu
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setReadTimeout(10000 /* milliseconds */ );
            urlConnection.setConnectTimeout(15000 /* milliseconds */ );

            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode != SUCCESS) {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return null;
            }

            // Isti proces čitanja kao u prethodnoj metodi
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String jsonString = sb.toString();
            Log.d(TAG, "JSON Object response: " + jsonString);

            // Razlika: parsiramo kao JSONObject umesto JSONArray
            return new JSONObject(jsonString);

        } catch (IOException e) {
            Log.e(TAG, "Network error in getJSONObjectFromURL", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * HTTP POST zahtev koji šalje JSON podatke na server
     * Koristi se za kreiranje novih objekata (sesije, glasovi)
     * @param urlString - puna URL adresa za zahtev
     * @param jsonObject - JSON podaci koje šaljemo serveru
     * @return JSONObject odgovor od servera ili null ako je zahtev neuspešan
     */
    /*HTTP post*/
    public JSONObject postJSONObjectFromURL(String urlString, JSONObject jsonObject) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            // Logujemo POST zahtev i podatke koje šaljemo
            Log.d(TAG, "POST request to: " + urlString);
            Log.d(TAG, "POST body: " + jsonObject.toString());

            java.net.URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            // HTTP metoda je POST umesto GET
            urlConnection.setRequestMethod("POST");
            // Specificiramo da šaljemo JSON podatke u UTF-8 kodiranju
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            // I dalje očekujemo JSON odgovor
            urlConnection.setRequestProperty("Accept","application/json");

            /*needed when used POST or PUT methods*/
            // Omogućavamo slanje podataka serveru (obavezno za POST)
            urlConnection.setDoOutput(true);
            // Omogućavamo čitanje odgovora od servera
            urlConnection.setDoInput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);

            // Uspostavljamo konekciju
            urlConnection.connect();

            // Kreiramo DataOutputStream za slanje JSON podataka
            DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream());
            /*write json object*/
            // Konvertujemo JSON objekat u string i šaljemo serveru
            os.writeBytes(jsonObject.toString());
            // Prisiljavamo slanje svih podataka (čišćenje buffer-a)
            os.flush();
            // Zatvaramo OutputStream
            os.close();

            // Dobijamo status kod odgovora
            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "POST Response code: " + responseCode);

            // POST zahtevi mogu vratiti 200 (OK) ili 201 (Created)
            if (responseCode != SUCCESS && responseCode != 201) {
                Log.e(TAG, "HTTP POST Error: " + responseCode);
                return null;
            }

            // Read response
            // Čitamo odgovor od servera (identično kao u GET metodama)
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String jsonString = sb.toString();
            Log.d(TAG, "POST Response: " + jsonString);

            // Vraćamo JSON odgovor od servera
            return new JSONObject(jsonString);

        } catch (IOException e) {
            Log.e(TAG, "Network error in postJSONObjectFromURL", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * HTTP DELETE zahtev za brisanje objekata na serveru
     * @param urlString - puna URL adresa resursa koji se briše
     * @return true ako je brisanje uspešno, false ako nije
     */
    /*HTTP delete*/
    public boolean httpDelete(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, "DELETE request to: " + urlString);
            java.net.URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            // HTTP metoda je DELETE
            urlConnection.setRequestMethod("DELETE");
            // Postavljamo zaglavlja (mada DELETE često ne šalje podatke)
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept","application/json");

            // Uspostavljamo konekciju
            urlConnection.connect();

            // Dobijamo status kod
            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "DELETE Response code: " + responseCode);

            // Vraćamo true samo ako je status kod 200 (uspešno obrisano)
            return (responseCode == SUCCESS);

        } catch (IOException e) {
            Log.e(TAG, "Network error in httpDelete", e);
            // Vraćamo false ako je bilo greške
            return false;
        } finally {
            if (urlConnection != null) {
                // Obavezno zatvaramo konekciju
                urlConnection.disconnect();
            }
        }
    }
}