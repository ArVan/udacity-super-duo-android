package barqsoft.footballscores;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.text.format.Time;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import barqsoft.footballscores.service.ScoresFetchService;
import barqsoft.footballscores.widget.LatestGameWidgetProvider;

/**
 * Created by arpy on 12/8/15.
 */
public class MainFragment extends Fragment {

    private static final String LOG_TAG = MainFragment.class.getSimpleName();

    public static final int NUM_PAGES = 5;
    public ViewPager mViewPager;
    private PageFragment[] viewFragments = new PageFragment[NUM_PAGES];
    private Toast mToast;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        if(savedInstanceState == null) {
            updateAllScores();
        }

        mViewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        mViewPager.setAdapter(new PagerAdapter(getChildFragmentManager()));
        mViewPager.setCurrentItem(MainActivity.mCurrentFragment);

        for (int i = 0; i < NUM_PAGES; i++) {
            Date fragmentDate = new Date(System.currentTimeMillis() + ((i - 2) * 86400000));
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            PageFragment iterPage = new PageFragment();
            iterPage.setDate(simpleDateFormat.format(fragmentDate));

            viewFragments[i] = iterPage;

            viewFragments[i].setArguments(getArguments());

            Bundle arguments = getArguments();
            if (arguments != null && arguments.getString(LatestGameWidgetProvider.SCORES_DATE) != null) {
                if (simpleDateFormat.format(fragmentDate).equals(arguments.getString(LatestGameWidgetProvider.SCORES_DATE))) {
                    mViewPager.setCurrentItem(i);
                }
            }
        }

        // if device in rtl mode, reverse the order of the page fragments
        if (Utilies.isRtl(getContext())) {
            Collections.reverse(Arrays.asList(viewFragments));
        }

        return rootView;
    }

    public void updateAllScores() {
        if (Utilies.isNetworkAvailable(getActivity())) {
            Intent serviceIntent = new Intent(getActivity(), ScoresFetchService.class);
            getActivity().startService(serviceIntent);
        } else {
            mToast = Toast.makeText(getActivity(), R.string.message_not_connected, Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {

        /**
         * Constructor
         * @param fragmentManager FragmentManager
         */
        public PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        /**
         * Get the fragment for given pager position
         * @param position int
         * @return PageFragment
         */
        @Override
        public Fragment getItem(int position) {
            return viewFragments[position];
        }

        /**
         * Get the amount of pages in pager
         * @return int
         */
        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        /**
         * Return the page title for given pager position
         */
        @Override
        public CharSequence getPageTitle(int position) {

            long dateInMillis = 0;
            String title = "";

            // get the time of given day position -xdays in milliseconds (or +xdays when in rtl mode)
            if (Utilies.isRtl(getContext())) {
                dateInMillis = System.currentTimeMillis() - ((position - 2) * 86400000);
            } else {
                dateInMillis = System.currentTimeMillis() + ((position - 2) * 86400000);
            }

            Time t = new Time();
            t.setToNow();
            int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
            int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);

            if (julianDay == currentJulianDay) {
                // today
                title = getActivity().getString(R.string.today);
            } else if (julianDay == currentJulianDay + 1) {
                // tomorrow
                title = getActivity().getString(R.string.tomorrow);
            } else if (julianDay == currentJulianDay - 1) {
                // yesterday
                title = getActivity().getString(R.string.yesterday);
            } else {
                // weekday
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
                title = dayFormat.format(dateInMillis);
            }

            return title.toUpperCase();
        }
    }
}
