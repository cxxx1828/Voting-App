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

public class HttpHelper {


    private static final int SUCCESS = 200;              // HTTP 200 OK
    private static final String TAG = "HTTP_HELPER";     // Tag za Android log

    // Base URL servera - 10.0.2.2 je specijalna IP adresa koja u Android emulatoru
    // predstavlja localhost (127.0.0.1) računara na kome se pokreće emulator
    public static final String BASE_URL = "http://10.0.2.2:8080/api";

    /**
     * HTTP GET zahtev za dobijanje JSON Array-a
     *GET – dobavljanje podataka; parametri se šalju unutar linije zahteva"
     * Koristi se za dobijanje lista podataka (npr. lista sesija, lista glasova)
     */
    public JSONArray getJSONArrayFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;            // HttpURLConnection - klasa koja se koristi za uspostavljanje HTTP konekcije ka serveru

        try {
            Log.d(TAG, "GET Array request to: " + urlString);

            // Kreiranje URL objekta i otvaranje konekcije
            URL url = new URL(urlString);

            // HttpURLConnection - klasa koja se koristi za uspostavljanje HTTP konekcije ka serveru
            //"Jedna instanca ove klase odgovara jednom zahtevu poslatom ka serveru"
            urlConnection = (HttpURLConnection) url.openConnection();

            // Konfiguracija HTTP zahteva
            urlConnection.setRequestMethod("GET");    // HTTP metoda - GET za dobavljanje podataka

            // HTTP zaglavlje - iz PDF-a: "Accept – tipovi koji su prihvatljivi kao odgovor"
            urlConnection.setRequestProperty("Accept", "application/json");

            // Timeout vrednosti - važno za sprečavanje blokiranja aplikacije
            urlConnection.setReadTimeout(10000);      // 10 sekundi za čitanje odgovora
            urlConnection.setConnectTimeout(15000);   // 15 sekundi za uspostavljanje konekcije

            try {
                // Uspostavljanje konekcije sa serverom
                urlConnection.connect();
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                return null; // Vraća null ako konekcija ne uspe (offline mode)
            }


            int responseCode = urlConnection.getResponseCode();//200  dobra
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode != SUCCESS) {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return null; // Vraća null za bilo koji status kod koji nije 200
            }

            // Čitanje odgovora sa servera koristeći BufferedReader
            // BufferedReader – klasa koja služi za čitanje iz input stream-a
            // url.openStream()  dobavlja stream iz koga se čitaju podaci
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder sb = new StringBuilder();
            String line;

            // Čitanje odgovora liniju po liniju
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            // Konverzija string-a u JSONArray objekat
            String jsonString = sb.toString();
            Log.d(TAG, "JSON Array response: " + jsonString);
            return new JSONArray(jsonString); // Parsiranje JSON-a u Java objekat

        } catch (IOException e) {
            Log.e(TAG, "Network error in getJSONArrayFromURL", e);
            return null; // Graceful degradation - aplikacija nastavi da radi offline
        } finally {
            // Zatvaranje konekcije da se oslobode resursi
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * HTTP GET zahtev za dobijanje pojedinačnog JSON objekta
     * Identičan kao prethodna metoda, razlika je samo u parsiranju odgovora
     */
    public JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, "GET Object request to: " + urlString);
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

            // Ista konfiguracija kao za Array verziju
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode != SUCCESS) {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return null;
            }

            // Čitanje odgovora koristeći getInputStream() umesto openStream()
            // za bolju kontrolu nad stream-om
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String jsonString = sb.toString();
            Log.d(TAG, "JSON Object response: " + jsonString);
            return new JSONObject(jsonString); // Parsiranje kao JSONObject umesto JSONArray

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
     * HTTP POST zahtev za slanje podataka serveru
     * Iz PDF-a: "POST – slanje podataka serveru; podaci se šalju u telu zahteva"
     * Koristi se za kreiranje novih resursa (sesije, glasovi, korisnici)
     */
    public JSONObject postJSONObjectFromURL(String urlString, JSONObject jsonObject) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, "POST request to: " + urlString);
            Log.d(TAG, "POST body: " + jsonObject.toString());

            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

            // Konfiguracija specifična za POST zahtev
            urlConnection.setRequestMethod("POST"); // HTTP metoda POST

            // HTTP zaglavlja -Content-Type – tip tela zahteva
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");

            // Ključne POST konfiguracije
            urlConnection.setDoOutput(true);  // Omogućava pisanje u output stream (slanje podataka), bez ovoga ne moze
            urlConnection.setDoInput(true);   // Omogućava čitanje odgovora

            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.connect();

            // Slanje JSON podataka u telu zahteva
            // DataOutputStream – upis u output stream
            DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream());
            os.writeBytes(jsonObject.toString()); // Konverzija JSON-a u bytes i slanje
            os.flush(); // Osigurava da se svi podaci pošalju
            os.close();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "POST Response code: " + responseCode);

            // POST može vratiti 200 (OK) ili 201 (Created) - oba su uspešni kodovi
            if (responseCode != SUCCESS && responseCode != 201) {
                Log.e(TAG, "HTTP POST Error: " + responseCode);
                return null;
            }

            // Čitanje odgovora sa servera nakon POST operacije
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String jsonString = sb.toString();
            Log.d(TAG, "POST Response: " + jsonString);
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
     * HTTP DELETE zahtev za brisanje resursa
     * Iz PDF-a: "DELETE – brisanje određenog resursa sa servera"
     * Vraća boolean jer obično ne očekujemo sadržaj u odgovoru
     */
    public boolean httpDelete(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, "DELETE request to: " + urlString);
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

            // Konfiguracija za DELETE zahtev
            urlConnection.setRequestMethod("DELETE");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "DELETE Response code: " + responseCode);

            // Vraća true samo ako je status kod 200 (uspešno obrisano)
            return (responseCode == SUCCESS);

        } catch (IOException e) {
            Log.e(TAG, "Network error in httpDelete", e);
            return false; // Vraća false ako operacija ne uspe
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}