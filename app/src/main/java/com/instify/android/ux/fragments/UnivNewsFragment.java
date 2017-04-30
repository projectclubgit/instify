package com.instify.android.ux.fragments;

/**
 * Created by Abhish3k on 3/06/2016. Thanks to Ravi
 */

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.instify.android.R;
import com.instify.android.app.AppController;
import com.instify.android.helpers.RetrofitBuilder;
import com.instify.android.interfaces.RetrofitInterface;
import com.instify.android.models.NewsItemModel;
import com.instify.android.models.NewsItemModelList;
import com.thefinestartist.finestwebview.FinestWebView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class UnivNewsFragment extends Fragment {
    private String TAG = UnivNewsFragment.class.getSimpleName();

    public UnivNewsFragment() {
    }

    public static UnivNewsFragment newInstance() {
        UnivNewsFragment frag = new UnivNewsFragment();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private static final String endpoint = "https://hashbird.com/gogrit.in/workspace/srm-api/univ-news.php";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SimpleStringRecyclerViewAdapter mAdapter;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_university_news, container, false);
        // Taking control of the menu options
        setHasOptionsMenu(true);
        // Initialize SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.red_primary, R.color.black, R.color.google_blue_900);

        // Setting up recycle view
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_university);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // Make it look like something is happening
        showRefreshing();

        // Make the request!
        makeJSONRequestRetrofit();

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            showRefreshing();
            makeJSONRequestRetrofit();
        });
        return rootView;
    }

    public void makeJSONRequestRetrofit() {
        RetrofitInterface client = RetrofitBuilder.createService(RetrofitInterface.class);
        Call<NewsItemModelList> call = client.GetUnivNews();
        call.enqueue(new Callback<NewsItemModelList>() {
            @Override
            public void onResponse(Call<NewsItemModelList> call, Response<NewsItemModelList> response) {
                if (response.isSuccessful()) {

                    List<Object> news = new ArrayList<>();
                    news.addAll(response.body().getNewsItems());
                    for (int i = 0; i < news.size(); i += 5) {
                        final NativeExpressAdView n = new NativeExpressAdView(getContext());

                        news.add(i, n);

                    }
                    loadNativeExpressAd(0, news);

                    // Set adapter
                    mAdapter = new SimpleStringRecyclerViewAdapter(getContext(), news);

                    // UI
                    hideRefreshing();
                    mAdapter.notifyDataSetChanged();
                    // Setting the adapter
                    recyclerView.setAdapter(mAdapter);
                } else {
                    Toast.makeText(getContext(), "Error Receiving University News", Toast.LENGTH_LONG).show();
                    hideRefreshing();
                }
            }

            @Override
            public void onFailure(Call<NewsItemModelList> call, Throwable t) {
                Toast.makeText(getContext(), "Failed to Receive University News", Toast.LENGTH_LONG).show();
                hideRefreshing();
            }
        });
    }

    private void loadNativeExpressAd(final int index, List<Object> mRecyclerViewItems) {

        if (index >= mRecyclerViewItems.size()) {
            return;
        }

        Object item = mRecyclerViewItems.get(index);
        if (!(item instanceof NativeExpressAdView)) {
            throw new ClassCastException("Expected item at index " + index + " to be a Native"
                    + " Express ad.");
        }

        final NativeExpressAdView adView = (NativeExpressAdView) item;

        // Set an AdListener on the NativeExpressAdView to wait for the previous Native Express ad
        // to finish loading before loading the next ad in the items list.
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // The previous Native Express ad loaded successfully, call this method again to
                // load the next ad in the items list.
                loadNativeExpressAd(index + 5, mRecyclerViewItems);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // The previous Native Express ad failed to load. Call this method again to load
                // the next ad in the items list.
                Log.e("MainActivity", "The previous Native Express ad failed to load. Attempting to"
                        + " load the next Native Express ad in the items list.");
                loadNativeExpressAd(index + 5, mRecyclerViewItems);
            }
        });

        // Load the Native Express ad.
        adView.setAdUnitId(getString(R.string.native_express_ad_unit_id));
        adView.setAdSize(new AdSize(300, 150));
        adView.loadAd(new AdRequest.Builder().build());
    }

    public void makeJSONRequest() {
        JsonObjectRequest req = new JsonObjectRequest(endpoint, null,
                response -> {
                    Timber.d(TAG, response.toString());
                    try {
                        JSONArray newsItems = response.getJSONArray("newsItems");

//                        mAdapter = new SimpleStringRecyclerViewAdapter(getContext(), newsItems);

                        // UI
                        hideRefreshing();
                        // Setting the adapter
                        recyclerView.setAdapter(mAdapter);
                    } catch (JSONException e) {
                        Log.e(TAG, "Json parsing error: " + e.getMessage());
                        Toast.makeText(getContext(), "JSON Parsing error", Toast.LENGTH_LONG).show();
                    }
                    mAdapter.notifyDataSetChanged();
                }, VolleyError -> {
            Log.e(TAG, "Error: " + VolleyError.getMessage());
            Toast.makeText(getContext(), "Error Receiving University News", Toast.LENGTH_LONG).show();
            hideRefreshing();
        });

        int socketTimeout = 10000;  // 10 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        req.setRetryPolicy(policy);
        AppController.getInstance().addToRequestQueue(req);
    }

    public static class SimpleStringRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int AD_TYPE = 1;
        private static final int CONTENT_TYPE = 0;
        private Context mContext;
        private List<Object> newsArray;
        AdRequest request;

        // Constructor
        private SimpleStringRecyclerViewAdapter(Context context, List<Object> newsArray) {
            mContext = context;
            this.newsArray = newsArray;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder viewHolder;
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == AD_TYPE) {
                View v = inflater.inflate(R.layout.card_view_with_ad, parent, false);
                viewHolder = new AdViewHolder(v);
            } else {
                View v = inflater.inflate(R.layout.card_view_univ, parent, false);
                viewHolder = new ViewHolder(v);
            }
            return viewHolder;
        }

        @Override
        public int getItemViewType(int position) {
            if (position % 5 == 0)
                return AD_TYPE;
            return CONTENT_TYPE;

        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            switch (viewType) {
                case CONTENT_TYPE:
                    ViewHolder viewHolder = (ViewHolder) holder;
                    NewsItemModel m = (NewsItemModel) newsArray.get(position);
                    viewHolder.mTextViewTitle.setText(m.getTitle());
                    viewHolder.mTextViewSnip.setText(m.getSnip());
                    viewHolder.mView.setOnClickListener(v -> {

                        new FinestWebView.Builder(v.getContext()).theme(R.style.FinestWebViewTheme)
                                .titleDefault("News Update")
                                .showUrl(false)
                                .statusBarColorRes(R.color.colorPrimaryDark)
                                .toolbarColorRes(R.color.colorPrimary)
                                .titleColorRes(R.color.finestWhite)
                                .urlColorRes(R.color.colorPrimaryLight)
                                .iconDefaultColorRes(R.color.finestWhite)
                                .progressBarColorRes(R.color.finestWhite)
                                .stringResCopiedToClipboard(R.string.copied_to_clipboard)
                                .stringResCopiedToClipboard(R.string.copied_to_clipboard)
                                .stringResCopiedToClipboard(R.string.copied_to_clipboard)
                                .updateTitleFromHtml(true)
                                .swipeRefreshColorRes(R.color.colorPrimaryDark)
                                .menuSelector(R.drawable.selector_light_theme)
                                .menuTextGravity(Gravity.CENTER)
                                .menuTextPaddingRightRes(R.dimen.defaultMenuTextPaddingLeft)
                                .dividerHeight(0)
                                .gradientDivider(false)
                                .setCustomAnimations(R.anim.slide_up, R.anim.hold, R.anim.hold, R.anim.slide_down)
                                .show(m.getLink());
                    });
                    break;
                case AD_TYPE:
                    // Fall Through
                default:
                    AdViewHolder nativeExpressHolder =
                            (AdViewHolder) holder;
                    NativeExpressAdView adView =
                            (NativeExpressAdView) newsArray.get(position);
                    ViewGroup adCardView = (ViewGroup) nativeExpressHolder.itemView;
                    // The NativeExpressAdViewHolder recycled by the RecyclerView may be a different
                    // instance than the one used previously for this position. Clear the
                    // NativeExpressAdViewHolder of any subviews in case it has a different
                    // AdView associated with it, and make sure the AdView for this position doesn't
                    // already have a parent of a different recycled NativeExpressAdViewHolder.
                    if (adCardView.getChildCount() > 0) {
                        adCardView.removeAllViews();
                    }
                    if (adView.getParent() != null) {
                        ((ViewGroup) adView.getParent()).removeView(adView);
                    }

                    // Add the Native Express ad to the native express ad view.
                    adCardView.addView(adView);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return newsArray.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final View mView;
            private final TextView mTextViewTitle, mTextViewSnip;

            private ViewHolder(View view) {
                super(view);
                mView = view;
                mTextViewTitle = (TextView) view.findViewById(R.id.univ_news_title);
                mTextViewSnip = (TextView) view.findViewById(R.id.univ_news_snip);
            }
        }

        public static class AdViewHolder extends RecyclerView.ViewHolder {


            private AdViewHolder(View view) {
                super(view);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.removeGroup(R.id.main_menu_group);
        super.onPrepareOptionsMenu(menu);
    }

    private void showRefreshing() {
        if (!mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(true);
    }

    private void hideRefreshing() {
        if (mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);
    }
}
