package app.philm.in;

import com.crashlytics.android.Crashlytics;
import com.github.johnpersano.supertoasts.SuperCardToast;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import java.util.HashSet;

import app.philm.in.lib.Display;
import app.philm.in.lib.controllers.MainController;
import app.philm.in.lib.util.IntUtils;
import app.philm.in.lib.util.PhilmCollections;
import app.philm.in.view.InsetDrawerLayout;

public abstract class BasePhilmActivity extends FragmentActivity
        implements MainController.HostCallbacks, InsetDrawerLayout.OnInsetsCallback {

    private MainController mMainController;
    private Intent mLaunchIntent;
    private Display mDisplay;

    private ActionBarDrawerToggle mDrawerToggle;

    private HashSet<OnActivityInsetsCallback> mInsetCallbacks;

    private Rect mInsets;

    private View mCardContainer;

    private InsetDrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Crashlytics.start(this);

        // Request Progress Bar in Action Bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(getContentViewLayoutId());

        mCardContainer = findViewById(R.id.card_container);

        mDrawerLayout = (InsetDrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            mDrawerLayout.setOnInsetsCallback(this);

            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer,
                    R.string.drawer_open_content_desc, R.string.drawer_closed_content_desc);

            mDrawerLayout.setDrawerListener(mDrawerToggle);

            ActionBar ab = getActionBar();
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);
        }

        // Record launch intent
        mLaunchIntent = getIntent();

        // Let SuperCardToast restore itself
        SuperCardToast.onRestoreState(savedInstanceState, this);

        mMainController = PhilmApplication.from(this).getMainController();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDisplay = new AndroidDisplay(this, getDrawerToggle(), getDrawerLayout());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mLaunchIntent = intent;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mMainController.attachDisplay(mDisplay);
        mMainController.setHostCallbacks(this);
        mMainController.init();

        if (mLaunchIntent != null) {
            mMainController.handleIntent(mLaunchIntent);
            mLaunchIntent = null;
        }
    }

    @Override
    public void setAccountAuthenticatorResult(String username, String authToken,
            String authTokenType) {
        // NO-OP
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                if (getMainController().onHomeButtonPressed()) {
                    return true;
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    // Call NavUtils for pre-JB functionality
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }
                break;

            case R.id.menu_about:
                if (getMainController().onAboutButtonPressed()) {
                    return true;
                }
                break;

            case R.id.menu_settings:
                if (getMainController().onSettingsButtonPressed()) {
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SuperCardToast.onSaveState(outState);
    }

    @Override
    protected void onPause() {
        mMainController.suspend();
        mMainController.setHostCallbacks(null);
        mMainController.detachDisplay(mDisplay);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisplay = null;
    }

    public void addInsetChangedCallback(OnActivityInsetsCallback callback) {
        if (mInsetCallbacks == null) {
            mInsetCallbacks = new HashSet<>();
        }
        mInsetCallbacks.add(callback);

        if (mInsets != null) {
            callback.onInsetsChanged(mInsets);
        }
    }

    public void removeInsetChangedCallback(OnActivityInsetsCallback callback) {
        if (mInsetCallbacks != null) {
            mInsetCallbacks.remove(callback);
        }
    }

    @Override
    public void onInsetsChanged(Rect insets) {
        mInsets = insets;

        if (mCardContainer != null) {
            mCardContainer.setPadding(0, insets.top, 0, 0);
        }

        if (!PhilmCollections.isEmpty(mInsetCallbacks)) {
            for (OnActivityInsetsCallback callback : mInsetCallbacks) {
                callback.onInsetsChanged(insets);
            }
        }
    }

    public void setInsetTopAlpha(float alpha) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setTopInsetAlpha(IntUtils.anchor(Math.round(alpha * 255), 0, 255));
        }
    }

    public void resetInsets() {
        setInsetTopAlpha(255);
    }

    public static interface OnActivityInsetsCallback {
        public void onInsetsChanged(Rect insets);
    }

    protected ActionBarDrawerToggle getDrawerToggle() {
        return mDrawerToggle;
    }

    protected DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    protected final MainController getMainController() {
        return mMainController;
    }

    protected int getContentViewLayoutId() {
        return R.layout.activity_main;
    }

}
