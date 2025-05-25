/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.pages;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.landenlabs.routes.GlobalHolder;
import com.landenlabs.routes.R;
import com.landenlabs.routes.events.EventBase;

public abstract class PageBaseFrag extends Fragment
        implements  MenuProvider, GlobalHolder.EventListener {

   protected PageViewModel viewModel;
   protected View root;

   public void init(@NonNull View root) {
      this.root = root;
      viewModel = new ViewModelProvider(requireActivity()).get(PageViewModel.class);
   }

   @Override
   public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
      menu.clear();
      menuInflater.inflate(R.menu.side_menu_main, menu);
   }

   @Override
   public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
      return false;
   }

   @Override
   public void onPrepareMenu(@NonNull Menu menu) {
      MenuProvider.super.onPrepareMenu(menu);
   }
   @Override
   public void onMenuClosed(@NonNull Menu menu) {
      MenuProvider.super.onMenuClosed(menu);
   }

   @Override
   public void onResume() {
      super.onResume();
      MenuHost menuHost = requireActivity();
      menuHost.addMenuProvider(this);
   }

   @Override
   public void onPause() {
      super.onPause();
      MenuHost menuHost = requireActivity();
      menuHost.removeMenuProvider(this);
   }

   @Override
   public void onEvent(@Nullable EventBase event) {
   }

   GlobalHolder getGlobal() {
      return viewModel.globalHolder;
   }

   protected void navigateTo(@IdRes int id) {
      viewModel.navigateTo(id);
   }
}
