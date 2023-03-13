package com.raredev.vcspace.ui.editor.manager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ViewFlipper;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.tabs.TabLayout;
import com.raredev.common.Indexer;
import com.raredev.common.util.DialogUtils;
import com.raredev.common.util.FileUtil;
import com.raredev.vcspace.R;
import com.raredev.vcspace.databinding.ActivityMainBinding;
import com.raredev.vcspace.ui.editor.CodeEditorView;
import com.raredev.vcspace.ui.editor.EditorViewModel;
import com.raredev.vcspace.util.PreferencesUtils;
import java.io.File;

public class EditorManager {
  private static final String LAST_FILES_KEY = "lastOpenedFiles";

  private DrawerLayout drawerLayout;
  private ViewFlipper container;
  private TabLayout tabLayout;

  private Context context;

  private EditorViewModel viewModel;
  private Indexer indexer;

  public EditorManager(Context context, ActivityMainBinding binding, EditorViewModel viewModel) {
    this.context = context;

    this.drawerLayout = binding.drawerLayout;
    this.container = binding.container;
    this.tabLayout = binding.tabLayout;
    this.viewModel = viewModel;

    indexer = new Indexer(context.getExternalFilesDir("editor") + "/openedFiles.json");
  }

  public EditorViewModel getViewModel() {
    return viewModel;
  }

  public void undo() {
    getCurrentEditor().undo();
  }

  public void redo() {
    getCurrentEditor().redo();
  }

  public void tryOpenFileFromIntent(Intent it) {
    try {
      Uri uri = it.getData();
      if (uri != null) {
        DocumentFile pickedFile = DocumentFile.fromSingleUri(context, uri);
        File file = FileUtil.getFileFromUri(context, pickedFile.getUri());

        openFile(file);
      }
    } catch (Exception e) {
      DialogUtils.newErrorDialog(
          context,
          context.getString(R.string.error),
          context.getString(R.string.error_editor_opening_recent_files) + "\n\n" + e.toString());
    }
  }

  public void tryOpenRecentOpenedFiles() {
    if (PreferencesUtils.useOpenRecentsAutomatically()) {
      try {
        viewModel.clear();
        for (File file : indexer.getList(LAST_FILES_KEY)) {
          openFile(file, false);
        }
      } catch (Throwable e) {
        DialogUtils.newErrorDialog(
            context,
            context.getString(R.string.error),
            context.getString(R.string.error_editor_opening_recent_files) + "\n\n" + e.toString());
      }
    }
  }

  public void openFile(File file) {
    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.close();
    }
    openFile(file, true);
  }

  public void openFile(File file, boolean setCurrent) {
    if (file == null) {
      return;
    }
    if (!file.isFile() && !file.exists()) {
      return;
    }

    if (viewModel.contains(file)) {
      if (setCurrent) setCurrentPosition(viewModel.indexOf(file));
      return;
    }

    container.addView(new CodeEditorView(context, file));
    tabLayout.addTab(tabLayout.newTab().setText(file.getName()));

    viewModel.openFile(file);
    if (setCurrent) setCurrentPosition(viewModel.indexOf(file));
    saveOpenedFiles();
  }

  public void closeFile(int index) {
    if (index >= 0 && index < viewModel.getFiles().getValue().size()) {
      CodeEditorView editor = getEditorAtIndex(index);
      if (editor != null) {
        editor.release();
      }

      viewModel.removeFile(index);
      tabLayout.removeTabAt(index);
      container.removeViewAt(index);
    }
    tabLayout.requestLayout();
  }

  public void closeOthers() {
    File currentFile = viewModel.getCurrentFile();
    CodeEditorView currentEditor = getCurrentEditor();

    closeAllFiles(true);

    currentEditor.getEditor().requestFocus();
    container.addView(currentEditor);
    
    tabLayout.addTab(tabLayout.newTab().setText(currentFile.getName()));

    viewModel.openFile(currentFile);
  }

  public void closeAllFiles(boolean ignoreCurrent) {
    if (!viewModel.getFiles().getValue().isEmpty()) {
      for (int i = 0; i < viewModel.getFiles().getValue().size(); i++) {
        CodeEditorView editor = getEditorAtIndex(i);
        // If ignoreCurrent is true then closeAll will
        // ignore and not trigger the release for the current editor 
        if (ignoreCurrent) {
          if (i != viewModel.getCurrentPosition())
            editor.release();
        } else {
          editor.release();
        }
      }
      viewModel.clear();
      tabLayout.removeAllTabs();
      tabLayout.requestLayout();
      container.removeAllViews();
    }
  }

  public void saveAll(boolean showMsg) {
    saveAllFiles(showMsg);
    saveOpenedFiles();
  }

  public void saveAllFiles(boolean showMsg) {
    if (!viewModel.getFiles().getValue().isEmpty()) {
      for (int i = 0; i < viewModel.getFiles().getValue().size(); i++) {
        getEditorAtIndex(i).save();
      }

      if (showMsg) {
        ToastUtils.showShort(R.string.saved_files);
      }
    }
  }

  public void saveOpenedFiles() {
    if (PreferencesUtils.useOpenRecentsAutomatically()) {
      try {
        indexer.put(LAST_FILES_KEY, viewModel.getFiles().getValue()).flush();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void onFileDeleted() {
    for (int i = 0; i < viewModel.getFiles().getValue().size(); i++) {
      File file = viewModel.getFiles().getValue().get(i);
      if (!file.exists()) {
        closeFile(i);
      }
    }
  }

  public CodeEditorView getEditorAtIndex(int index) {
    return (CodeEditorView) container.getChildAt(index);
  }

  public CodeEditorView getCurrentEditor() {
    return (CodeEditorView) container.getChildAt(viewModel.getCurrentPosition());
  }

  public void setCurrentPosition(int index) {
    final var tab = tabLayout.getTabAt(index);
    if (tab != null && index >= 0 && !tab.isSelected()) {
      tab.select();
    }
    viewModel.setCurrentPosition(index);
  }
}
