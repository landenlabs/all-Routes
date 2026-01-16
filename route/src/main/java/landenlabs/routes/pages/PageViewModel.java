/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.pages;

import static landenlabs.routes.utils.SysUtils.hasRef;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;

import landenlabs.routes.GlobalHolder;
import landenlabs.routes.R;

import java.lang.ref.WeakReference;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * ViewModel used to communicate/share data between page fragment.
 */
@SuppressWarnings("UnusedReturnValue")
public class PageViewModel extends ViewModel {

    public GlobalHolder globalHolder;
    public NavController navController;
    public WeakReference<Toolbar> toolBarRef;
    public WeakReference<ViewGroup> navBarRef;

    public PageViewModel setGlobal(GlobalHolder globalHolder) {
        this.globalHolder = globalHolder;
        return this;
    }
    public PageViewModel setToolBar(Toolbar toolBar) {
        this.toolBarRef = new WeakReference<>(toolBar);
        return this;
    }
    public PageViewModel setNavBar(ViewGroup navBar) {
        this.navBarRef = new WeakReference<>(navBar);
        return this;
    }
    public PageViewModel setNav(NavController navController) {
        this.navController = navController;
        return this;
    }

    public void navigateTo(@IdRes int pageFragId) {
        ALog.d.tagMsg(this, "Navigate to ",  globalHolder.contextRef.get().getResources().getResourceName(pageFragId));
        NavDestination navDestination = navController.getCurrentDestination();
        // https ://stackoverflow.com/questions/50514758/how-to-clear-navigation-stack-after-navigating-to-another-fragment-in-android
        if (false) {
            NavOptions navOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.page_summary, false, false)
                    .build();
            Bundle args = null;
            navController.navigate(pageFragId, args, navOptions);
        } else {
            navController.popBackStack(R.id.page_summary, false);
            navController.navigate(pageFragId);
        }
    }

    protected void popBackPage() {
        // navController.popBackStack();
        navController.navigate(R.id.page_summary);
    }

    public void showToolBar(boolean enable) {
        if (hasRef(toolBarRef))
            toolBarRef.get().setVisibility(enable ? View.VISIBLE : View.GONE);
    }
    public void showNavBar(boolean enable) {
        if (hasRef(navBarRef))
            navBarRef.get().setVisibility(enable ? View.VISIBLE : View.GONE);
    }

}
