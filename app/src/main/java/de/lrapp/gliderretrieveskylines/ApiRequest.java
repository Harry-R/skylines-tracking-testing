package de.lrapp.gliderretrieveskylines;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

public class ApiRequest {


    /**
     *Coordinates the api request
     * @param pilotId the pilot's id
     * @param apiCallback the callback to call after fetching api data
     */
    private static void liveTrackData(int pilotId, ApiCallback apiCallback) {
        try {
            URL url = new URL("https://skylines.aero/api/tracking");
            new RequestTask(url, apiCallback, pilotId).execute();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Establishes a http connection, calls readStream
     * @return see read stream
     * @param url url to connect to
     * @throws IOException if url connect not successful
     */
    static String connect(URL url) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Accept", "application/json");
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            return readStream(in);
        } finally {
            urlConnection.disconnect();
        }
    }

    /**
     * Reads an InputStream and converts it to a String.
     * @param stream the InputStream
     * @return string from the stream
     * @throws IOException if reading stream fails
     */
    private static String readStream(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        Log.i("readStream:", total.toString());
        return total.toString();
    }


    /**
     * Async task for handling API HTTP request
     */
    private static class RequestTask extends AsyncTask<Void, Void, String> {
        private URL url;
        private ApiCallback apiCallback;
        private int pilotID;
        /**
         * Constructor
         * @param url url to connect to
         */
        private RequestTask(URL url, ApiCallback apiCallback, int pilotID) {
            this.url = url;
            this.apiCallback = apiCallback;
            this.pilotID = pilotID;

        }

        /**
         * Do HTTP request in Background
         * @param voids void
         * @return result or null
         */
        @Override
        protected String doInBackground(Void... voids) {

            try {
                return connect(url);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * displays the results of the AsyncTask.
         * @param result API HTTP request result
         */
        @Override
        protected void onPostExecute(String result) {
            Log.i("result: ", result);
            if (result != null) {
                JSONObject jsonResult = filterResult(result, pilotID);
               apiCallback.callback(jsonResult);

            }
        }
    }

    /**
     * converts result to json and filters for pilot ID
     * @param result http request result to filter
     * @param pilotID pilot id to filter for
     * @return pilot's track data as JSONObject
     */
    private static JSONObject filterResult(String result, int pilotID) {
        try {
            JSONObject jsonResult = new JSONObject(result);
            JSONObject trackData;
            //get JSONTrack array
            JSONArray trackArray = jsonResult.getJSONArray("tracks");
            for(int i = 0; i < trackArray.length(); i++) {
                if (trackArray.getJSONObject(i).getJSONObject("pilot").getInt("id") == pilotID) {
                    trackData = trackArray.getJSONObject(i);
                    return trackData;
                }
            }
            return null;
            // JSONObject jObj = jsonResult.getJSONArray("tracks").getJSONObject(0).getJSONObject("pilot");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * update every 5 seconds by calling liveTrackData function
     * @param pilotId the pilot's id
     * @param apiCallback the callback to call after fetching api data
     */
    static void startUpdater(final int pilotId, final ApiCallback apiCallback) {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            liveTrackData(pilotId, apiCallback);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 5000);
    }

}
