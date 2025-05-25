/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes;

import static com.landenlabs.routes.utils.DataUtils.getString1x;
import static com.landenlabs.routes.utils.SysUtils.getAppVersion;
import static com.landenlabs.routes.utils.SysUtils.getNavController;
import static com.landenlabs.routes.utils.UtilSpan.SSBold;
import static com.landenlabs.routes.utils.UtilSpan.SSJoin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.perf.metrics.Trace;
import com.landenlabs.routes.databinding.ActivityMainBinding;
import com.landenlabs.routes.logger.Externals;
import com.landenlabs.routes.pages.PageViewModel;
import com.landenlabs.routes.utils.CatchAppExceptions;
import com.wsi.wxdata.WxData;
import com.wsi.wxdata.WxDataInitializationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import landenlabs.wx_lib_data.logger.ALog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GlobalHolder globalHolder;
    private PageViewModel pageViewModel;
    private CatchAppExceptions catchAppExceptions;
    String intentAction = "";

    // Must register before onCreate
    public ActivityResultCallback<ActivityResult> onResultCallback;
    public ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (onResultCallback != null) {
                    onResultCallback.onActivityResult(result);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Trace trace = Externals.init(this, launcher);

        globalHolder = GlobalHolder.getInstance(this);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class).setGlobal(globalHolder);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        /*
        binding.appBarMain.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
         */
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.page_summary, R.id.page_routes, R.id.page_record, R.id.page_wx)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = getNavController(this, R.id.nav_host_fragment_content_main);
        // NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        /*
        // https ://stackoverflow.com/questions/50514758/how-to-clear-navigation-stack-after-navigating-to-another-fragment-in-android
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                @SuppressLint("RestrictedApi")
                List<NavBackStackEntry> backStackEntries = navController.getCurrentBackStack().getValue();
                int backStackSize = backStackEntries.size();
                ALog.d.tagMsg(this,  "BackStack depth=", backStackSize, " navTo=", navDestination.getDisplayName());
            }
        });
         */

        // Share data between fragments.
        pageViewModel.setNav(navController);

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        setupBars();
        initSDKs();
        catchAppExceptions = new CatchAppExceptions(this);
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS}, 0);

        // Setup shortcuts for Bottom Nav items.
        binding.getRoot().post(this::addOrExecuteShortcut);
        intentAction = getIntent() != null ? getIntent().getAction() : null;
        ALog.d.tagMsg(this,  "indent action=", intentAction);

        trace.stop();   // Firebase performance monitor
    }

    private void initSDKs() {
        ALog.d.tagMsg(this, "onWxDataInitialization");
        String key = getString1x(this, "app_name");
        String sun = getString1x(this, "job1");

        Map<String, String> overrides = new HashMap<>();
        overrides.put( WxData.OVER_CLIENT, "TWC Auto Widget Demo");
        overrides.put( WxData.OVER_SUN, sun);
        overrides.put( WxData.OVER_MAP, key);
        WxData.enableLogging(ALog.isDebugApp());
        ALog.logPrintLn("ALog Init WxData logging=" + ALog.isDebugApp());

        WxData.getInstance().initialize(this, key, new com.wsi.wxdata.WxData.InitializationListener() {
            @Override
            public void onWxDataInitializationSucceeded() {
                WxData.enableLogging(ALog.isDebugApp());
                globalHolder.wx = WxData.getInstance();
                ALog.d.tagMsg(this, "onWxDataInitializationSucceeded");
            }

            @Override
            public void onWxDataInitializationFailed(WxDataInitializationException error) {
                ALog.e.tagMsg(this, "onWxDataInitializationFailed", error);
            }
        }, overrides);
    }

    @Nullable
    public static Fragment getCurrentFragment(@NonNull FragmentActivity context) {
        FragmentManager fragManager = context.getSupportFragmentManager();
        int count = context.getSupportFragmentManager().getBackStackEntryCount();
        return (count > 0) ? fragManager.getFragments().get(count - 1) : null;
    }

    private void setupBars() {
        Toolbar toolBar = findViewById(R.id.toolbar);
        toolBar.setTitle(SSJoin(SSBold(getString(R.string.app_name)), " v", getAppVersion(this)));
        pageViewModel.setToolBar(toolBar);

        ViewGroup navBar = findViewById(R.id.page_nav_bar);
        pageViewModel.setNavBar(navBar);
        if (navBar != null) {
            for (int idx = 0; idx < navBar.getChildCount(); idx++)
                navBar.getChildAt(idx).setOnClickListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.side_menu_main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onClick(View view) {
        @IdRes int id = view.getId();
        if (view instanceof RadioButton) {
            pageViewModel.navigateTo(id);
        }
    }

    /**
     * Add or execute short cut
     */
    @SuppressWarnings("ConstantConditions")
    private void addOrExecuteShortcut() {
        Map<Integer, Integer> icons = new HashMap<>();
        icons.put(R.id.page_summary, R.drawable.ic_menu_summary);
        icons.put(R.id.page_routes, R.drawable.ic_menu_routes);
        icons.put(R.id.page_record, R.drawable.ic_menu_record);
        icons.put(R.id.page_wx, R.drawable.ic_menu_wx);
        icons.put(R.id.page_dev, R.drawable.ic_menu_dev);

        ShortcutManager shortcutManager = this.getSystemService(ShortcutManager.class);
        NavController navController  = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        Iterator<NavDestination> navIT =  navController.getGraph().iterator();
        List<ShortcutInfo> shortcutList = new ArrayList<>();
        while (navIT.hasNext()) {
            NavDestination navDestination = navIT.next();
            Intent newTaskIntent = new Intent(this, MainActivity.class);
            newTaskIntent.setAction(navDestination.getLabel().toString());
            newTaskIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            Integer iconNum = icons.get(navDestination.getId());
            @DrawableRes int iconRes = iconNum != null ? iconNum : R.drawable.ic_map_marker;
            ShortcutInfo postShortcut
                    = new ShortcutInfo.Builder(this, navDestination.getLabel().toString())
                    .setShortLabel(navDestination.getLabel())
                    .setLongLabel(navDestination.getLabel())
                    .setIcon(Icon.createWithResource(this, iconRes))
                    .setIntent(newTaskIntent)
                    .build();
            shortcutList.add(postShortcut);

            if (navDestination.getLabel().equals(intentAction)) {
                // Execute shortcut
                navController.navigate(navDestination.getId());
            }
        }

        shortcutManager.addDynamicShortcuts(shortcutList);
    }
}