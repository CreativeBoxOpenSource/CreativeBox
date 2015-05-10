package com.MoNTE48.RateME;

import java.util.Date;

import mobi.MultiCraft.R;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;
import android.widget.RatingBar;
import android.widget.Toast;

/**
 * RateThisApp<br>
 * A library to show the app rate dialog
 *
 * @author Keisuke Kobayashi <k.kobayashi.122@gmail.com>
 */
public class RateThisApp {
	private static final String TAG = RateThisApp.class.getSimpleName();
	private static final String GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=";
	private static final String PREF_NAME = "RateThisApp";
	private static final String KEY_INSTALL_DATE = "rta_install_date";
	private static final String KEY_LAUNCH_TIMES = "rta_launch_times";
	private static final String KEY_OPT_OUT = "rta_opt_out";

	private static Date mInstallDate = new Date();
	private static int mLaunchTimes = 0;
	private static boolean mOptOut = false;

	/**
	 * Days after installation until showing rate dialog
	 */
	public static final int INSTALL_DAYS = 3;
	/**
	 * App launching times until showing rate dialog
	 */
	public static final int LAUNCH_TIMES = 3;

	/**
	 * If true, print LogCat
	 */
	public static final boolean DEBUG = false;

	/**
	 * Call this API when the launcher activity is launched.<br>
	 * It is better to call this API in onStart() of the launcher activity.
	 */
	public static void onStart(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		// If it is the first launch, save the date in shared preference.
		if (pref.getLong(KEY_INSTALL_DATE, 0) == 0L) {
			Date now = new Date();
			editor.putLong(KEY_INSTALL_DATE, now.getTime());
			log("First install: " + now.toString());
		}
		// Increment launch times
		int launchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
		launchTimes++;
		editor.putInt(KEY_LAUNCH_TIMES, launchTimes);
		log("Launch times; " + launchTimes);

		editor.apply();

		mInstallDate = new Date(pref.getLong(KEY_INSTALL_DATE, 0));
		mLaunchTimes = pref.getInt(KEY_LAUNCH_TIMES, 0);
		mOptOut = pref.getBoolean(KEY_OPT_OUT, false);

		printStatus(context);
	}

	public static void showRateDialogIfNeeded(final Context context) {
		if (shouldShowRateDialog()) {
			showRateDialog(context);
		}
	}

	public static boolean shouldShowRateDialog() {
		if (mOptOut) {
			return false;
		} else {
			if (mLaunchTimes >= LAUNCH_TIMES) {
				return true;
			}
			long threshold = INSTALL_DAYS * 24 * 60 * 60 * 1000L; // msec
			return new Date().getTime() - mInstallDate.getTime() >= threshold;
		}
	}

	public static void showRateDialog(final Context context) {
		// custom dialog
		final Dialog dialog = new Dialog(context);
		dialog.setContentView(R.layout.rate_layout);
		dialog.setTitle(R.string.rta_dialog_title);

		RatingBar ratingBar = (RatingBar) dialog.findViewById(R.id.ratingBar);
		ratingBar
				.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
					@Override
					public void onRatingChanged(RatingBar ratingBar,
							float rating, boolean fromUser) {
						if (rating > 3) {
							dialog.dismiss();
							String appPackage = context.getPackageName();
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri
									.parse(GOOGLE_PLAY + appPackage));
							context.startActivity(intent);
							setOptOut(context, true);
						} else {
							Toast.makeText(context, R.string.sad,
									Toast.LENGTH_LONG).show();
							dialog.dismiss();
							clearSharedPreferences(context);
						}
					}
				});
		dialog.show();
	}

	private static void clearSharedPreferences(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(KEY_INSTALL_DATE);
		editor.remove(KEY_LAUNCH_TIMES);
		editor.apply();
	}

	private static void setOptOut(final Context context, boolean optOut) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putBoolean(KEY_OPT_OUT, optOut);
		editor.apply();
	}

	private static void printStatus(final Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		log("*** RateThisApp Status ***");
		log("Install Date: " + new Date(pref.getLong(KEY_INSTALL_DATE, 0)));
		log("Launch Times: " + pref.getInt(KEY_LAUNCH_TIMES, 0));
		log("Opt out: " + pref.getBoolean(KEY_OPT_OUT, false));
	}

	private static void log(String message) {
		if (DEBUG) {
			Log.v(TAG, message);
		}
	}
}
