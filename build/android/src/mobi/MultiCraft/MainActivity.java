package mobi.MultiCraft;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import static mobi.MultiCraft.PreferencesHelper.DIALOGS;
import static mobi.MultiCraft.PreferencesHelper.HELP_DIALOG;
import static mobi.MultiCraft.PreferencesHelper.MEMORY_DIALOG;
import static mobi.MultiCraft.PreferencesHelper.RATE_DIALOG;
import static mobi.MultiCraft.PreferencesHelper.TAG_SHORTCUT_CREATED;
import static mobi.MultiCraft.PreferencesHelper.VERSION_DIALOG;
import static mobi.MultiCraft.PreferencesHelper.getBitMask;
import static mobi.MultiCraft.PreferencesHelper.isCreateShortcut;
import static mobi.MultiCraft.PreferencesHelper.loadSettings;
import static mobi.MultiCraft.PreferencesHelper.saveSettings;

public class MainActivity extends Activity implements IDialogHelperCallback {
    public final static String TAG = "Error";
    public final static String CREATE_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    public final static String FILES = "Files.zip";
    public final static String NOMEDIA = ".nomedia";
    public final static String STABLE_VER = "1.0.1";
    private ProgressDialog mProgressDialog;
    private TextView mProgressTextView, mWarningTextView;
    private String unzipLocation = Environment.getExternalStorageDirectory() + "/MultiCraft/";
    private DialogHelper dialogHelper;
    private ProgressBar mProgressBar;
    private MyBroadcastReceiver myBroadcastReceiver;
    private MyBroadcastReceiver_Update myBroadcastReceiver_Update;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        RateThisApp.onStart(this);
        loadSettings(this);
        dialogHelper = new DialogHelper(this);
        registerReceivers();
        if (!isTaskRoot()) {
            finish();
            return;
        }
        showDialogTree(getBitMask());

    }

    @Override
    public void showDialogTree(int bitMask) {
        if (bitMask == 0) {
            init();
            return;
        }
        for (int dialog : DIALOGS) {
            Dialog currentDialog = null;
            if ((bitMask & dialog) > 0) {
                bitMask -= dialog;
                switch (dialog) {
                    case MEMORY_DIALOG:
                        currentDialog = dialogHelper.showMemoryDialog(bitMask);
                        break;
                    case RATE_DIALOG:
                        currentDialog = dialogHelper.showRateDialog(bitMask);
                        break;
                    case VERSION_DIALOG:
                        currentDialog = dialogHelper.showVersionDialog(bitMask);
                        break;
                    case HELP_DIALOG:
                        currentDialog = dialogHelper.showHelpDialog(bitMask);
                        break;
                }
                if (currentDialog != null) {
                    currentDialog.show();
                }
                break;
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
        unregisterReceiver(myBroadcastReceiver);
        unregisterReceiver(myBroadcastReceiver_Update);
    }

    private void addShortcut() {
        saveSettings(this, TAG_SHORTCUT_CREATED, false);
        Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_launcher));
        addIntent.setAction(CREATE_SHORTCUT);
        getApplicationContext().sendBroadcast(addIntent);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void init() {
        if (isCreateShortcut())
            addShortcut();
        mProgressTextView = (TextView) findViewById(R.id.progress_textView);
        mProgressBar = (ProgressBar) findViewById(R.id.PB1);
        mWarningTextView = (TextView) findViewById(R.id.warning);
        Drawable draw;
        draw = getResources().getDrawable(R.drawable.custom_progress_bar);
        mProgressBar.setProgressDrawable(draw);
        Utilities util = new Utilities();
        util.createDirAndFiles();
        util.checkVersion();
    }

    @Override
    public void exit() {
        finish();
    }

    private void registerReceivers() {
        myBroadcastReceiver = new MyBroadcastReceiver();
        myBroadcastReceiver_Update = new MyBroadcastReceiver_Update();
        IntentFilter intentFilter = new IntentFilter(UnzipService.ACTION_MyIntentService);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(myBroadcastReceiver, intentFilter);
        IntentFilter intentFilter_update = new IntentFilter(UnzipService.ACTION_MyUpdate);
        intentFilter_update.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(myBroadcastReceiver_Update, intentFilter_update);
    }

    private void showSpinnerDialog(int message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(getString(message));
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void runGame() {
        showSpinnerDialog(R.string.loading);
        new Thread(new Runnable() {
            public void run() {
                Intent intent = new Intent(MainActivity.this, MCNativeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (isFinishing())
                    return;
                dismissProgressDialog();
            }
        }).start();
        File file = new File(Environment.getExternalStorageDirectory(), FILES);
        if (file.exists())
            file.delete();
    }

    private void startUnzipService(String file) throws IOException {
        // Start MyIntentService
        Intent intentMyIntentService = new Intent(this, UnzipService.class);
        intentMyIntentService.putExtra(UnzipService.EXTRA_KEY_IN_FILE, file);
        intentMyIntentService.putExtra(UnzipService.EXTRA_KEY_IN_LOCATION, unzipLocation);
        startService(intentMyIntentService);

    }

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra(UnzipService.EXTRA_KEY_OUT);
            if ("Success".equals(result)) {
                runGame();
            }

        }
    }

    public class MyBroadcastReceiver_Update extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int update = intent.getIntExtra(UnzipService.EXTRA_KEY_UPDATE, 0);
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressTextView.setVisibility(View.VISIBLE);
            mWarningTextView.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(update);
            mProgressTextView.setText(update + "%");
        }
    }

    private class DeleteTask extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showSpinnerDialog(R.string.rm_old);
        }

        @Override
        protected Void doInBackground(String... params) {
            for (String p : params) {
                deleteFiles(p);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (isFinishing())
                return;
            dismissProgressDialog();
            new CopyZip().execute(FILES);
        }

        private void deleteFiles(String path) {
            File file = new File(path);
            if (file.exists()) {
                String deleteCmd = "rm -r " + path;
                Runtime runtime = Runtime.getRuntime();
                try {
                    runtime.exec(deleteCmd);
                } catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        }
    }

    private class CopyZip extends AsyncTask<String, Void, String> {
        String zipName;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            zipName = params[0];
            copyAssets(zipName);
            return "Done";

        }

        @Override
        protected void onPostExecute(String result) {
            if (PhoneInformation.getAvailableSpaceInMB() > 30) {
                try {
                    startUnzipService(Environment.getExternalStorageDirectory() + "/" + zipName);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            } else
                dialogHelper.showNotEnoughSpaceDialog();
        }

        private void copyAssets(String zipName) {
            InputStream in;
            OutputStream out;
            try {
                in = getAssets().open(zipName);
                out = new FileOutputStream(Environment.getExternalStorageDirectory() + "/" + zipName);
                copyFile(in, out);
                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy asset file: " + e.getMessage());
            }
        }

        private void copyFile(InputStream in, OutputStream out) throws IOException {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private class Utilities {

        private void createLangFile() {
            PrintWriter writer;
            try {
                writer = new PrintWriter(unzipLocation + "lang.txt", "UTF-8");
                if ("Russian".equals(Locale.getDefault().getDisplayLanguage())) {
                    writer.println("ru");
                } else {
                    writer.println("en");
                }
                writer.close();
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
            }

        }

        private void createDirAndFiles() {
            try {
                File folder = new File(unzipLocation);
                if (!(folder.exists()))
                    folder.mkdirs();
                File myFile = new File(unzipLocation, NOMEDIA);
                if (!myFile.exists())
                    myFile.createNewFile();
                createLangFile();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        private void checkVersion() {
            File version = new File(unzipLocation + "ver.txt");
            if (version.exists()) {
                if (isCurrent(version))
                    runGame();
                else
                    startDeletion();

            } else {
                startDeletion();
            }
        }

        private void startDeletion() {
            new DeleteTask().execute(unzipLocation + "cache", unzipLocation + "games", unzipLocation + "builtin",
                    unzipLocation + "fonts", unzipLocation + "debug.txt");

        }

        @SuppressWarnings("resource")
        private boolean isCurrent(File file) {
            String line = null;
            try {
                line = new BufferedReader(new FileReader(file)).readLine();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            if (line == null) {
                line = "-999";
            }
            return line.equals(STABLE_VER);
        }
    }
}