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

    private static final int SUCCESS = 200;
    private static final String TAG = "HTTP_HELPER";
    public static final String BASE_URL = "http://10.0.2.2:8080/api";

    /*HTTP get zahtev za JSON Array*/
    public JSONArray getJSONArrayFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, "GET Array request to: " + urlString);
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);

            try {
                urlConnection.connect();
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                return null;
            }

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode != SUCCESS) {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String jsonString = sb.toString();
            Log.d(TAG, "JSON Array response: " + jsonString);
            return new JSONArray(jsonString);

        } catch (IOException e) {
            Log.e(TAG, "Network error in getJSONArrayFromURL", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /*HTTP get zahtev za JSON Object*/
    public JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, "GET Object request to: " + urlString);
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

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

            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String jsonString = sb.toString();
            Log.d(TAG, "JSON Object response: " + jsonString);
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

    /*HTTP post zahtev*/
    public JSONObject postJSONObjectFromURL(String urlString, JSONObject jsonObject) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, "POST request to: " + urlString);
            Log.d(TAG, "POST body: " + jsonObject.toString());

            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.connect();

            DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream());
            os.writeBytes(jsonObject.toString());
            os.flush();
            os.close();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "POST Response code: " + responseCode);

            if (responseCode != SUCCESS && responseCode != 201) {
                Log.e(TAG, "HTTP POST Error: " + responseCode);
                return null;
            }

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

    /*HTTP delete zahtev*/
    public boolean httpDelete(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, "DELETE request to: " + urlString);
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "DELETE Response code: " + responseCode);
            return (responseCode == SUCCESS);

        } catch (IOException e) {
            Log.e(TAG, "Network error in httpDelete", e);
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}