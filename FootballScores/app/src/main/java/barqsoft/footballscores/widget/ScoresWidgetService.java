package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * Created by DJ on 7/28/2015.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ScoresWidgetService extends RemoteViewsService {
    public final String LOG_TAG = ScoresWidgetService.class.getSimpleName();
    private static final String[] SCORES_COLUMNS = {
            DatabaseContract.scores_table.TIME_COL,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.MATCH_ID
    };

    public static final int INDEX_TIME_COL = 0;
    public static final int INDEX_HOME_COL = 1;
    public static final int INDEX_AWAY_COL = 2;
    public static final int INDEX_HOME_GOALS_COL = 3;
    public static final int INDEX_AWAY_GOALS_COL = 4;
    public static final int INDEX_MATCH_ID = 5;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                data = getApplicationContext().getContentResolver().
                        query(DatabaseContract.BASE_CONTENT_URI, SCORES_COLUMNS, null, null, DatabaseContract.scores_table.TIME_COL + " ASC");
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                data.moveToPosition(position);
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                String homeName,awayName,score,time;
                homeName = data.getString(INDEX_HOME_COL);
                awayName = data.getString(INDEX_AWAY_COL);
                time = data.getString(INDEX_TIME_COL);
                try {
                    score = Utilies.getScores(data.getInt(INDEX_HOME_GOALS_COL), data.getInt(INDEX_AWAY_GOALS_COL));
                }catch (IllegalStateException e){
                    score = "-";
                }


                RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(),
                        R.layout.widget_list_item);

                views.setTextViewText(R.id.home_name, homeName);
                views.setTextViewText(R.id.away_name, awayName);
                views.setTextViewText(R.id.score_textview, score);
                views.setTextViewText(R.id.data_textview, time);

                final Intent fillInIntent = new Intent();
                views.setOnClickFillInIntent(R.id.base_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(data.getColumnIndex(DatabaseContract.scores_table.MATCH_ID));
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
