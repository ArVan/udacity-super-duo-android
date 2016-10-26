package barqsoft.footballscores;

import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.View;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by yehya khaled on 3/3/2015.
 */
public class Utilies {
    /**
     * Get the resource id by resource name and type
     *
     * @param context
     * @param aString
     * @return int
     */
    private static int getResourceIdByName(Context context, String aString, String type) {
        return context.getResources().getIdentifier(aString, type, context.getPackageName());
    }

    public static String getLeague(Context context, int league_num) {
        int[] leagueIndexes = context.getResources().getIntArray(R.array.league_indexes);
        int[] leagueCodes = context.getResources().getIntArray(R.array.league_codes);
        String[] leagueLabels = context.getResources().getStringArray(R.array.league_labels);

        // find the position of the league code and we get the league label (same index)
        for (int leagueIndex : leagueIndexes) {
            if (Integer.valueOf(context.getString(getResourceIdByName(context, "league_code_" + leagueIndex, "integer"))) == league_num) {
                return context.getString(getResourceIdByName(context, "league_label_" + leagueIndex, "string"));
            }
        }

        return context.getString(R.string.message_unknown_league);
    }

    public static String getMatchDay(Context context, int match_day, int league_num) {
        if (league_num == Integer.valueOf(context.getString(getResourceIdByName(context, "league_code_" + Integer.valueOf(context.getString(R.integer.league_index_champions_league)), "integer")))) {
            if (match_day <= 6) {
                return context.getString(R.string.group_stage_text) + ", " + context.getString(R.string.matchday_text) + ": " + match_day;
            } else if (match_day == 7 || match_day == 8) {
                return context.getString(R.string.first_knockout_round);
            } else if (match_day == 9 || match_day == 10) {
                return context.getString(R.string.quarter_final);
            } else if (match_day == 11 || match_day == 12) {
                return context.getString(R.string.semi_final);
            } else {
                return context.getString(R.string.final_text);
            }
        } else {
            return context.getString(R.string.matchday_text) +": " + match_day;
        }
    }

    public static String getScores(int home_goals, int awaygoals) {
        if (home_goals < 0 || awaygoals < 0) {
            return " - ";
        } else {
            return String.valueOf(home_goals) + " - " + String.valueOf(awaygoals);
        }
    }

    /**
     * Check if RTL mode is active
     *
     * @return boolean
     */
    public static boolean isRtl(Context context) {
        boolean isRtl = false;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {

            Configuration config = context.getResources().getConfiguration();
            if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                isRtl = true;
            }
        } else {

            // get the device locale and language
            Locale locale = Locale.getDefault();
            String language = locale.getLanguage();

            if (language.equals("ar") ||
                    language.equals("dv") ||
                    language.equals("fa") ||
                    language.equals("ha") ||
                    language.equals("he") ||
                    language.equals("iw") ||
                    language.equals("ji") ||
                    language.equals("ps") ||
                    language.equals("ur") ||
                    language.equals("yi")
                    )
                isRtl = true;
        }

        return isRtl;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean contains(final int[] array, final int key) {

        // loop and return true when found
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }

        return false;
    }

}
