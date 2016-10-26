package barqsoft.footballscores.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.Constant;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.data.DatabaseContract;
import barqsoft.footballscores.R;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class ScoresFetchService extends IntentService       //renamed from lowercase to uppercase
{
    public static final String LOG_TAG = ScoresFetchService.class.getSimpleName();

    private int[] mLeagueCodes;
    private String mApiKey;
    private ArrayList<ContentValues> mTeams;

    public ScoresFetchService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mLeagueCodes = getResources().getIntArray(R.array.league_codes);
        mApiKey = getResources().getString(R.string.api_key);
        mTeams = new ArrayList<ContentValues>();

        Log.d(LOG_TAG, mApiKey);

        if (Utilies.isNetworkAvailable(getApplicationContext())) {
            getTeamDetails();
            getFixtures("n2");
            getFixtures("p2");
        } else {
            Log.d(LOG_TAG, "no network connection");
        }
    }

    private void getTeamDetails() {

        //loop through selected leagues and fetch their teams with details
        for (int leagueCode : mLeagueCodes) {
            try {
                URL queryUrl = new URL(Uri.parse(Constant.FS_API_URL_BASE)
                        .buildUpon()
                        .appendPath(Constant.FS_API_URL_PATH_SEASONS)
                        .appendPath(Integer.toString(leagueCode))
                        .appendPath(Constant.FS_API_URL_PATH_TEAMS)
                        .build()
                        .toString());

                String teams = queryHelper(queryUrl);

                if (teams != null) {
                    processTeams(teams);
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception in getTeamDetails: " + e.getMessage());
            }
        }
    }

    private void getFixtures(String timeFrame) {
        try {
            URL queryUrl = new URL(Uri.parse(Constant.FS_API_URL_BASE)
                    .buildUpon()
                    .appendPath(Constant.FS_API_URL_PATH_FIXTURES)
                    .appendQueryParameter(Constant.FS_API_URL_PATH_PARAM_TIMEFRAME, timeFrame)
                    .build()
                    .toString());

            // get match data for the next 2 days from api
            String fixtures = queryHelper(queryUrl);

            if (fixtures != null) {
                getApplicationContext().getContentResolver().bulkInsert(
                        DatabaseContract.BASE_CONTENT_URI,
                        processFixtures(fixtures));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception in getFixtures: " + e.getMessage());
        }
    }

    /**
     * Convert loaded json teams data to list of team id and team logo url
     *
     * @param jsonString String
     */
    private void processTeams(String jsonString) {

        final String TEAMS = "teams";
        final String LINKS = "_links";
        final String SELF = "self";
        final String CREST_URL = "crestUrl";

        final String SELF_LINK = Constant.FS_API_URL_BASE + "/" + Constant.FS_API_URL_PATH_TEAMS + "/";

        try {
            JSONArray teams = new JSONObject(jsonString).getJSONArray(TEAMS);

            if (teams.length() == 0) {
                return;
            }

            for (int i = 0; i < teams.length(); i++) {

                JSONObject team = teams.getJSONObject(i);

                // extract the team id from href in links.self
                String teamId = team.getJSONObject(LINKS).getJSONObject(SELF).getString("href");
                teamId = teamId.replace(SELF_LINK, "");

                // get the cresturl
                String teamLogoUrl = team.getString(CREST_URL);

                //convert svg images to pngs via wikipedia special thumb url
                if (teamLogoUrl != null && teamLogoUrl.endsWith(".svg")) {
                    String svgUrl = teamLogoUrl;
                    String filename = svgUrl.substring(svgUrl.lastIndexOf("/") + 1);

                    int wikiPathEndPosition = svgUrl.indexOf("/wikipedia/") + 11;
                    String afterWikipediaPath = svgUrl.substring(wikiPathEndPosition);

                    int thumbInsertPos = wikiPathEndPosition + afterWikipediaPath.indexOf("/") + 1;

                    String afterLanguageCodePath = svgUrl.substring(thumbInsertPos);

                    teamLogoUrl = svgUrl.substring(0, thumbInsertPos);
                    teamLogoUrl += "thumb/" + afterLanguageCodePath;
                    teamLogoUrl += "/200px-" + filename + ".png";
                }

                // create contentvalues object containing the team details
                ContentValues teamValues = new ContentValues();
                teamValues.put(DatabaseContract.ScoreEntry.HOME_ID_COL, Integer.parseInt(teamId));
                teamValues.put(DatabaseContract.ScoreEntry.HOME_LOGO_COL, teamLogoUrl);

                // add team to the vector
                mTeams.add(teamValues);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Convert api fixtures json data to array of matches
     * @param fixturesString String
     */
    private ContentValues[] processFixtures(String fixturesString) {

        ArrayList<ContentValues> fixturesList = new ArrayList<>(0);

        // indicator for real or dummy data
        boolean isReal = true;

        // json element names
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";
        final String HOME_TEAM_ID = "homeTeam";
        final String AWAY_TEAM_ID = "awayTeam";

        final String SEASON_LINK = Constant.FS_API_URL_BASE + "/" +
                Constant.FS_API_URL_PATH_SEASONS + "/";
        final String MATCH_LINK = Constant.FS_API_URL_BASE + "/" +
                Constant.FS_API_URL_PATH_FIXTURES + "/";
        final String TEAM_LINK = Constant.FS_API_URL_BASE + "/" +
                Constant.FS_API_URL_PATH_TEAMS + "/";

        try {
            JSONArray fixtures = new JSONObject(fixturesString).getJSONArray(FIXTURES);

            // load dummy data if no matches found
            if (fixtures.length() == 0) {
                fixturesString = getString(R.string.dummy_data);
                fixtures = new JSONObject(fixturesString).getJSONArray(FIXTURES);
                isReal = false;
            }

            fixturesList = new ArrayList<ContentValues>(fixtures.length());

            for(int i = 0; i < fixtures.length(); i++) {

                JSONObject fixture = fixtures.getJSONObject(i);

                // extract league from href in links.soccerseason
                String leagueValue = fixture.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).getString("href");
                leagueValue = leagueValue.replace(SEASON_LINK, "");
                int league = Integer.parseInt(leagueValue);

                // only include matches from selected leagues
                if (Utilies.contains(mLeagueCodes, league)) {
                    // extract the match id from href in links.self
                    String matchId = fixture.getJSONObject(LINKS).getJSONObject(SELF).getString("href");
                    matchId = matchId.replace(MATCH_LINK, "");

                    // extract the home team id from href in links.homeTeam
                    String homeTeamIdString = fixture.getJSONObject(LINKS).getJSONObject(HOME_TEAM_ID).getString("href");
                    homeTeamIdString = homeTeamIdString.replace(TEAM_LINK, "");
                    int homeTeamId = Integer.parseInt(homeTeamIdString);
                    ContentValues homeTeam = getTeamByIdHelper(homeTeamId);
                    String homeLogo = homeTeam != null ? homeTeam.getAsString(DatabaseContract.ScoreEntry.HOME_LOGO_COL): "";

                    // extract the away team id from href in links.awayTeam
                    String awayTeamIdString = fixture.getJSONObject(LINKS).getJSONObject(AWAY_TEAM_ID).getString("href");
                    awayTeamIdString = awayTeamIdString.replace(TEAM_LINK, "");
                    int awayTeamId = Integer.parseInt(awayTeamIdString);
                    ContentValues awayTeam = getTeamByIdHelper(awayTeamId);
                    String awayLogo = awayTeam != null ? awayTeam.getAsString(DatabaseContract.ScoreEntry.HOME_LOGO_COL): "";

                    // increment the match id of the dummy data (makes it unique)
                    if (!isReal) {
                        matchId = matchId + Integer.toString(i);
                    }

                    // get the date and time from match date field
                    String date = fixture.getString(MATCH_DATE);
                    String time = date.substring(date.indexOf("T") + 1, date.indexOf("Z"));
                    date = date.substring(0, date.indexOf("T"));
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss", Locale.US);
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                    // convert date and time to local datetime and extract date and time again
                    try {
                        Date parsedDate = simpleDateFormat.parse(date + time);
                        SimpleDateFormat newDate = new SimpleDateFormat("yyyy-MM-dd:HH:mm", Locale.US);
                        newDate.setTimeZone(TimeZone.getDefault());
                        date = newDate.format(parsedDate);
                        time = date.substring(date.indexOf(":") + 1);
                        date = date.substring(0, date.indexOf(":"));

                        // change the dummy data's date to match current date range
                        if(!isReal) {
                            Date dummyDate = new Date(System.currentTimeMillis() + ((i-2)*86400000));
                            SimpleDateFormat dummyDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            date = dummyDateFormat.format(dummyDate);
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }

                    // create contentvalues object containing the match details
                    ContentValues fixtureValues = new ContentValues();
                    fixtureValues.put(DatabaseContract.ScoreEntry.MATCH_ID, matchId);
                    fixtureValues.put(DatabaseContract.ScoreEntry.DATE_COL, date);
                    fixtureValues.put(DatabaseContract.ScoreEntry.TIME_COL, time);
                    fixtureValues.put(DatabaseContract.ScoreEntry.HOME_COL, fixture.getString(HOME_TEAM));
                    fixtureValues.put(DatabaseContract.ScoreEntry.HOME_ID_COL, homeTeamId);
                    fixtureValues.put(DatabaseContract.ScoreEntry.HOME_LOGO_COL, homeLogo);
                    fixtureValues.put(DatabaseContract.ScoreEntry.HOME_GOALS_COL, fixture.getJSONObject(RESULT).getString(HOME_GOALS));
                    fixtureValues.put(DatabaseContract.ScoreEntry.AWAY_COL, fixture.getString(AWAY_TEAM));
                    fixtureValues.put(DatabaseContract.ScoreEntry.AWAY_ID_COL, awayTeamId);
                    fixtureValues.put(DatabaseContract.ScoreEntry.AWAY_LOGO_COL, awayLogo);
                    fixtureValues.put(DatabaseContract.ScoreEntry.AWAY_GOALS_COL, fixture.getJSONObject(RESULT).getString(AWAY_GOALS));
                    fixtureValues.put(DatabaseContract.ScoreEntry.LEAGUE_COL, league);
                    fixtureValues.put(DatabaseContract.ScoreEntry.MATCH_DAY, fixture.getString(MATCH_DAY));

                    fixturesList.add(fixtureValues);
                }
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Exception here in processFixtures: " + e.getMessage());
        }

        return fixturesList.toArray(new ContentValues[fixturesList.size()]);
    }

    /**
     * @param apiUrl URL
     */
    private String queryHelper(URL apiUrl) {
        if (apiUrl == null) {
            return null;
        }

        String apiResult = null;

        // check if we have a network connection
        if (!Utilies.isNetworkAvailable(getApplicationContext())) {
            return null;
        }

        HttpURLConnection apiConnection = null;
        BufferedReader apiReader = null;

        try {
            // open connection
            apiConnection = (HttpURLConnection) apiUrl.openConnection();
            apiConnection.setRequestMethod("GET");
            apiConnection.addRequestProperty("X-Auth-Token", mApiKey);
            apiConnection.connect();

            // read the input stream into a string
            InputStream inputStream = apiConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }

            // read the result from the inputstream into a buffer
            apiReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = apiReader.readLine()) != null) {
                buffer.append(line);
            }

            if (buffer.length() > 0) {
                apiResult = buffer.toString();
            } else {
                return null;
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception here: " + e.getMessage());
        } finally {

            // disconnect the api connection and close the reader
            if (apiConnection != null) {
                apiConnection.disconnect();
            }
            if (apiReader != null) {
                try {
                    apiReader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error Closing Stream");
                }
            }
        }

        return apiResult;
    }

    private ContentValues getTeamByIdHelper(int teamId) {

        for (ContentValues team: mTeams) {
            if (team.getAsInteger(DatabaseContract.ScoreEntry.HOME_ID_COL).equals(teamId)) {
                return team;
            }
        }

        return null;
    }
}

