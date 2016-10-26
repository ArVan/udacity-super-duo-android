package barqsoft.footballscores;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import barqsoft.footballscores.data.DatabaseContract;
import barqsoft.footballscores.widget.TodayGamesWidgetProvider;

/**
 * Created by arpy on 12/8/15.
 */
public class PageFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = PageFragment.class.getSimpleName();
    public static final int LOADER_SCORES = 0;
    public ScoresAdapter mAdapter;
    private ListView mScoresList;
    private String mDate;
    public int mSelectedFixtureMatchId = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page, container, false);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.getInt(TodayGamesWidgetProvider.SCORES_MATCH_ID) != 0) {
            mSelectedFixtureMatchId = arguments.getInt(TodayGamesWidgetProvider.SCORES_MATCH_ID);
        } else {

        }

        mAdapter = new ScoresAdapter(getActivity(), null, 0);

        mScoresList = (ListView) rootView.findViewById(R.id.scores_list);

        View emptyView = rootView.findViewById(R.id.messageNoData);

        mScoresList.setEmptyView(emptyView);
        mScoresList.setAdapter(mAdapter);
        mScoresList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                View detailsView = view.findViewById(R.id.match_details);

                // all other detail views should be hidden
                for (int i = 0; i < parent.getCount(); i++) {
                    View listItem = parent.getChildAt(i);
                    if (listItem != null && listItem != view) {
                        listItem.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));
                        listItem.findViewById(R.id.match_details).setVisibility(View.GONE);
                    }
                }

                if (detailsView.getVisibility() != View.VISIBLE) {
                    view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryLight));
                    detailsView.setVisibility(View.VISIBLE);
                    //if last row selected, scroll down to fit
                    if (position == mScoresList.getCount() - 1) {
                        parent.setSelection(position);
                    }
                } else {
                    view.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));
                    detailsView.setVisibility(View.GONE);
                }


            }
        });

        mScoresList.setOnScrollListener(new AbsListView.OnScrollListener() {
            /**
             * @param absListView AbsListView
             * @param i int
             */
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                for (int j = 0; j < absListView.getCount(); j++) {
                    View item = absListView.getChildAt(j);
                    if (item != null) {
                        item.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));
                        View itemDetailsView = item.findViewById(R.id.match_details);
                        itemDetailsView.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        });

        getLoaderManager().initLoader(LOADER_SCORES, null, this);

        return rootView;

    }

    /**
     * @param date
     */
    public void setDate(String date) {
        mDate = date;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                DatabaseContract.ScoreEntry.buildScoreWithDate(),
                null,
                null,
                new String[]{mDate},
                DatabaseContract.ScoreEntry.TIME_COL + " ASC, " + DatabaseContract.ScoreEntry.HOME_COL + " ASC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mAdapter != null) {
            mAdapter.swapCursor(data);
        }

        updateEmptyView();

        // if we recieved a match id from collection widget, find the list position
        int fixturePosition = 0;
        if (mSelectedFixtureMatchId != 0) {
            int position = 0;
            data.moveToFirst();
            while (data.moveToNext()) {
                if (data.getInt(data.getColumnIndex(DatabaseContract.ScoreEntry.MATCH_ID)) == mSelectedFixtureMatchId) {
                    fixturePosition = position;
                    break;
                }
                position++;
            }
        }

        // if we recieved a position from collection widget, select it
        if (fixturePosition != 0) {
            final int selectedFixturePosition = fixturePosition;
            mScoresList.post(new Runnable() {
                @Override
                public void run() {

                    // scroll to it
                    mScoresList.setSelection(selectedFixturePosition);

                    // reset the pre-selection so we only selected on app launch
                    mSelectedFixtureMatchId = 0;
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null) {
            mAdapter.swapCursor(null);
        }
    }

    private void updateEmptyView() {
        if (mAdapter.getCount() == 0) {
            TextView tv = (TextView) getView().findViewById(R.id.messageNoData);
            if (tv != null) {
                int msgId = R.string.message_no_data;
                if (!Utilies.isNetworkAvailable(getContext())) {
                    msgId = R.string.message_no_network;
                }
                tv.setText(msgId);
            }
        }
    }
}
