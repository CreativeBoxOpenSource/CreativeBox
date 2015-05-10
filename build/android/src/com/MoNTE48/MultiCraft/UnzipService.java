package com.MoNTE48.MultiCraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import mobi.MultiCraft.R;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UnzipService extends IntentService {
	public final String TAG = UnzipService.class.getSimpleName();
	public static final String ACTION_MyIntentService = "com.MoNTE48.MultiCraft.RESPONSE";
	public static final String ACTION_MyUpdate = "com.MoNTE48.MultiCraft.UPDATE";
	public static final String EXTRA_KEY_IN_FILE = "file";
	public static final String EXTRA_KEY_IN_LOCATION = "location";
	public static final String EXTRA_KEY_OUT = "EXTRA_OUT";
	public static final String EXTRA_KEY_UPDATE = "EXTRA_UPDATE";

	public UnzipService() {
		super("com.MoNTE48.MultiCraft.UnzipService");
	}

	private void _dirChecker(String dir, String unzipLocation) {
		File f = new File(unzipLocation + dir);

		if (!f.isDirectory()) {
			f.mkdirs();
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification.Builder mBuilder = new Notification.Builder(this);
		mBuilder.setContentTitle(getString(R.string.extract))
				.setContentText(getString(R.string.unpack))
				.setSmallIcon(R.drawable.ic_launcher);
		String file = intent.getStringExtra(EXTRA_KEY_IN_FILE);
		String location = intent.getStringExtra(EXTRA_KEY_IN_LOCATION);
		// Displays the progress bar for the first time.
		mBuilder.setProgress(100, 0, false);
		int id = 1;
		mNotifyManager.notify(id, mBuilder.build());
		int per = 0;
		try {
			ZipFile zipSize = new ZipFile(file);
			try {
				FileInputStream fin = new FileInputStream(file);
				ZipInputStream zin = new ZipInputStream(fin);
				ZipEntry ze;
				while ((ze = zin.getNextEntry()) != null) {
					if (ze.isDirectory()) {
						per++;
						_dirChecker(ze.getName(), location);
					} else {
						per++;
						int progress = 100 * per / zipSize.size();
						// send update
						Intent intentUpdate = new Intent();
						intentUpdate.setAction(ACTION_MyUpdate);
						intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
						intentUpdate.putExtra(EXTRA_KEY_UPDATE, progress);
						sendBroadcast(intentUpdate);
						mBuilder.setProgress(100, progress, false);
						mNotifyManager.notify(id, mBuilder.build());
						FileOutputStream f_out = new FileOutputStream(location
								+ ze.getName());
						byte[] buffer = new byte[8192];
						int len;
						while ((len = zin.read(buffer)) != -1) {
							f_out.write(buffer, 0, len);
						}
						f_out.close();
						zin.closeEntry();
						f_out.close();
					}

				}
				zin.close();
			} catch (FileNotFoundException e) {
				Log.e(TAG, e.getMessage());
			}
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
		// mBuilder.setContentText(getString(R.string.complete));
		// mBuilder.setProgress(0, 0, false);
		// mNotifyManager.notify(id, mBuilder.build());
		mNotifyManager.cancel(id);
		Intent intentResponse = new Intent();
		intentResponse.setAction(ACTION_MyIntentService);
		intentResponse.addCategory(Intent.CATEGORY_DEFAULT);
		intentResponse.putExtra(EXTRA_KEY_OUT, "Success");
		sendBroadcast(intentResponse);
	}
}
