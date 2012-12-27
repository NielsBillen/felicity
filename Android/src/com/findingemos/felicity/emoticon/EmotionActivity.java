package com.findingemos.felicity.emoticon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.findingemos.felicity.R;
import com.findingemos.felicity.backend.EmotionDatabase;
import com.findingemos.felicity.doing.DoingActivity;
import com.findingemos.felicity.emoticonselector.EmotionSelectorActivity;
import com.findingemos.felicity.general.ActivityIndicator;
import com.findingemos.felicity.general.ActivitySwitchListener;
import com.findingemos.felicity.settings.SettingsActivity;
import com.findingemos.felicity.twitter.Constants;
import com.findingemos.felicity.util.SimpleSwipeListener;
import com.findingemos.felicity.util.SlideActivity;
import com.findingemos.felicity.util.Swipeable;
import com.findingemos.felicity.visualization.VisualizationActivity;

/**
 * This is the main activity. This screen show the current emotion of the user.
 * Furthermore, it has a scroll view that allows the user to select an emotion.<br>
 * 
 * 
 * @author Niels
 * @version 0.1
 */
@SuppressLint("NewApi")
public class EmotionActivity extends SlideActivity implements Swipeable,
		EmotionSelectionListener {
	
	// Boolean die er voorzorgt dat een emotie maximaal eenmaal geteld kan worden.
	public static boolean doingStarted = false;

	// Final boolean variable to check whether drag and drop is enabled.
	public static final boolean DRAG_AND_DROP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	// The database to call
	public static EmotionDatabase DATABASE;

	// Code for requesting an emotion.
	public static final int EMOTION_REQUEST_CODE = 1;
	// Code for requesting extra information (activity)
	public static final int EXTRA_INFORMATION_CODE = 2;
	
	public static final int LOCATION_SETTINGS = 3;
	
	public static final String UNKNOWN_LOCATION = "Location not kwown yet";

	// Variable to indicate the current city the user is in.
	private String currentCity = UNKNOWN_LOCATION;
	// Variable to indicate the current country the user is in.
	private String currentCountry = UNKNOWN_LOCATION;
	// Variable to store the emoticon the user selected.
	private static Emotion currentEmotion;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// This method is called when the activity is first created.
		super.onCreate(savedInstanceState);

		// Deze hack mag hier gebruikt worden, aangezien de main thread juist
		// geblokt moet worden als we twitter tokens willen toevoegen.
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		Log.i("Activity", "EmotionActivity started");

		// Create the database
		DATABASE = new EmotionDatabase(this);
		DATABASE.open();
		DATABASE.readEmotionCount();
		DATABASE.readEmotionDatabase();

		// Print the android version
		Log.e("Android version", "" + Build.VERSION.SDK_INT);

		/*
		 * These two method calls are suggested by a site to avoid the blocks
		 * when drawing gradients. These lines should make the transition
		 * between the colors more smoothly.
		 */
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

		/*
		 * This method sets how this view should be constructed. These details
		 * are specified in "emotion.xml". To access that xml file, you use the
		 * variables in the autogenerated "R" class.
		 */
		setContentView(R.layout.emotion_activity);

		// This method will fill the HorizontalScroller (see xml) with
		// emoticons.
		fillEmoticonSelection();

		// Add a touch listener.
		EmotionDrawer drawer = (EmotionDrawer) findViewById(R.id.emoticonDrawer);
		drawer.setOnTouchListener(new SimpleSwipeListener(this));

		// Add a view change listener
		ActivityIndicator indicator = (ActivityIndicator) findViewById(R.id.activityIndicator);
		indicator.addListener(new ActivitySwitchListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see com.findingemos.felicity.general.ActivitySwitchListener#
			 * activitySelected(int)
			 */
			@Override
			public void activitySelected(int index) {
				if (index == 1)
					switchToVisualization();
			}
		});

		EmotionExpander expander = (EmotionExpander) findViewById(R.id.emotionExpander);
		expander.setOnTouchListener(new View.OnTouchListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnTouchListener#onTouch(android.view.View,
			 * android.view.MotionEvent)
			 */
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() != MotionEvent.ACTION_DOWN)
					return false;
				switchToEmotionSelection();
				return true;
			}
		});

		overridePendingTransition(R.anim.lefttoright_emotion,
				R.anim.righttoleft_emotion);

		// Initialize the location look-up.
		initCurrentLocation();

	}

	/**
	 * Start de visualisatie activiteit.
	 */
	private void switchToVisualization() {
		Intent intent = new Intent(this, VisualizationActivity.class);
		startActivity(intent);
	}

	/**
	 * Start de emotiongallery activiteit.
	 */
	private void switchToEmotionSelection() {
		Intent intent = new Intent(this, EmotionSelectorActivity.class);
		overridePendingTransition(R.anim.uptodown_emotion,
				R.anim.downtoup_emotion);
		startActivityForResult(intent, EMOTION_REQUEST_CODE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "Settings");
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);

			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	/**
	 * This method fills the horizontal scroller with all the emoticons.<br>
	 * <br>
	 * The available emoticons are defined in Emoticon.java.<br>
	 * <br>
	 * The emoticons are put in a LinearLayout that is placed inside a
	 * HorizontalScroller. The HorizontalScroller allows you to scroll through a
	 * view that is much bigger than the scroller itself. Therefore we add all
	 * the emoticons after each other in the horizontal scroller;
	 * 
	 */
	private void fillEmoticonSelection() {
		// Get the linear layout to put the emoticon views in.
		LinearLayout layout = (LinearLayout) findViewById(R.id.horizontalEmoticonScrollerLayout);

		// Find the EmoticonDrawer view.
		EmotionDrawer drawer = (EmotionDrawer) findViewById(R.id.emoticonDrawer);

		Emotion[] sorted = Emotion.values();
		Arrays.sort(sorted, Emotion.getComparator());

		for (int i = 0; i < sorted.length; ++i) {
			// Create the layout paramters
			LayoutParams layoutParameters = new LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.MATCH_PARENT);
			EmotionView view = new EmotionView(this, sorted[i]);
			view.setMinimumWidth(80);

			// IMPORTANT! this line makes sure the name of the icon is drawn!
			view.addListener(drawer);
			view.addListener(this);
			layout.addView(view, layoutParameters);
		}

	}

	// ////////////////////////////////////////////////////
	// / Activity Life Cycle ///
	// ////////////////////////////////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		DATABASE.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		DATABASE.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		DATABASE.close();
	}

	// ////////////////////////////////////////////////////
	// / Activity Result ///
	// ////////////////////////////////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i("Result", "Got a result!");
		if (resultCode != RESULT_OK) {
			return;
		}

		if (requestCode == EMOTION_REQUEST_CODE) {
			emotionGalleryReturned(data);
		} else if (requestCode == EXTRA_INFORMATION_CODE) {
			extraInformationReturned(data);
		} else if (requestCode == LOCATION_SETTINGS) {
			initCurrentLocation();
		}
	}

	/**
	 * Methode die de terugkeer van de emotionGallery afhandelt.
	 * 
	 * @param data
	 *            De bijgevoegde data.
	 */
	private void emotionGalleryReturned(Intent data) {
		Log.i("Result", "Got the result!");
		int uniqueEmotionId = data.getIntExtra("emotion", 0);
		Emotion emotion = Emotion.getEmoticonByUniqueId(uniqueEmotionId);
		userSelectedEmoticon(emotion);
	}

	/**
	 * Methode die de terugkeer van de extraInformation afhandelt.
	 * 
	 * @param data
	 *            De bijgevoegde data.
	 */
	private void extraInformationReturned(Intent data) {

		String activity = data.getStringExtra("activity");
		ArrayList<String> friends = data.getStringArrayListExtra("friends");

		DATABASE.open();
		DATABASE.createEmotionEntry(Calendar.getInstance(), currentCountry,
				currentCity, activity, friends, currentEmotion);

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		boolean twitterEnabled = settings.getBoolean("twitter enabled", false);
		if (twitterEnabled) {
			String tweet = makeTweet(activity, friends);
			updateStatus(tweet);
		}

		Log.i("Emotion", currentEmotion.toString());
		Log.i("Date & Time", Calendar.getInstance().getTime() + "");
		Log.i("Epoch", Calendar.getInstance().getTimeInMillis() + "");
		Log.i("Country", currentCountry);
		Log.i("City", currentCity);
	}

	/**
	 * @param activity
	 * @param friends
	 * @return
	 */
	private String makeTweet(String activity, ArrayList<String> friends) {
		String tweet = "I am #" + currentEmotion.getName().replaceAll(" ", "");
		// Indien de locatie niet gekend is wordt deze ook niet getweet.
		if(!currentCity.equals(UNKNOWN_LOCATION)) {
			tweet += " in " + currentCity;
		}
		tweet += " during #" + activity.replaceAll(" ", "");
		if (friends != null) {
			// indien geen vrienden geselecteerd worden, dan is de lijst leeg
			if (friends.size() > 0) {
				tweet += " with " + friends.get(0);
				if(friends.size() == 2) {
					tweet += " and 1 other. #Felicity";
				} else if (friends.size() > 2) {
					tweet += " and " + (friends.size()-1) + " others. #Felicity";
				} else {
					tweet += ". #Felicity";
				}
			} else {
				tweet += " all by myself. #Felicity";
			}
		}

		tweet = tweetLimitCheck(tweet);

		return tweet;
	}

	/**
	 * @param tweet
	 * @return
	 */
	private String tweetLimitCheck(String tweet) {
		if (tweet.length() > Constants.TWEET_LIMIT) {
			System.out.println("Tweet te lang!");
			int i = Constants.TWEET_LIMIT - 1;
			while (i > 0) {
				tweet = tweet.substring(0, i);
				if (tweet.charAt(tweet.length() - 1) == ' ') {
					break;
				}
				i--;
			}
		}
		return tweet;
	}

	/**
	 * Deze methode dient te worden aangeroepen wanneer de gebruiker zijn
	 * (nieuwe) emotie aangaf. Deze methode update de currentEmotion variable en
	 * tekent de desbetreffende emotie op het scherm. Bovendien start ze de
	 * DoingActivity op om na te gaan wat te gebruiker aan het doen is en bij
	 * wie hij is.
	 * 
	 * @param emoticon
	 *            De nieuwe emotie van de gebruiker.
	 */
	private void userSelectedEmoticon(Emotion emoticon) {
		currentEmotion = emoticon;
		drawEmoticion(currentEmotion);

		if (!doingStarted) {
			doingStarted = true;
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					currentEmotion.incrementSelectionCount();
					Intent intent = new Intent(EmotionActivity.this,
							DoingActivity.class);
					startActivityForResult(intent, EXTRA_INFORMATION_CODE);
				}
			}, 1250);
		}
	}

	public static void decrementSelectionCountOfCurrentEmotion() {
		currentEmotion.decrementSelectionCount();
	}

	/**
	 * Methode die de meegegeven emoticon tekent op het scherm.
	 * 
	 * @param emoticon
	 *            De emoticon die getekend dient te worden op het scherm.
	 */
	private void drawEmoticion(Emotion emoticon) {
		EmotionDrawer drawer = (EmotionDrawer) findViewById(R.id.emoticonDrawer);
		drawer.onEmotionSelected(emoticon);
		drawer.onEmotionDoubleTapped(emoticon);
	}

	// ////////////////////////////////////////////////////
	// / Locatie ///
	// ////////////////////////////////////////////////////

	/**
	 * Deze methode initialiseert het opzoeken van de locatie.
	 * 
	 * De primaire methode is het bepalen van de locatie via Netwerkgegevens.
	 * Als dit niet lukt, wordt er gebruikgemaakt van de GPS (als deze
	 * aanstaat). Is er geen internetverbinding en staat de GPS af, dan wordt de
	 * locatie niet opgezocht.
	 */
	private void initCurrentLocation() {
		LocationManager locationManager;
		String provider;
		try {
			String svcName = Context.LOCATION_SERVICE;
			locationManager = (LocationManager) getSystemService(svcName);

			provider = null;
			boolean gpsEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);
			boolean internetEnabled = isNetworkConnected();

			if (internetEnabled) {
				Log.i("Location Method", "Internet");
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				criteria.setPowerRequirement(Criteria.POWER_LOW);
				criteria.setAltitudeRequired(false);
				criteria.setBearingRequired(false);
				criteria.setSpeedRequired(false);
				criteria.setCostAllowed(true);
				provider = locationManager.getBestProvider(criteria, true);
				locationManager.requestSingleUpdate(provider, locationListener,
						null);
			} else if (gpsEnabled) {
				Log.i("Location Method", "GPS");
				provider = LocationManager.GPS_PROVIDER;
				locationManager.requestSingleUpdate(provider, locationListener,
						null);
			} else {
				Log.i("Location Method", "none");
			}
		} catch (Exception e) {
			
			Log.i("Location Method", "locationServices disabled");
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						Toast.makeText(EmotionActivity.this, "Please enable location services", Toast.LENGTH_LONG).show();
			            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			            EmotionActivity.this.startActivityForResult(myIntent,LOCATION_SETTINGS);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						// No button clicked
						break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(EmotionActivity.this);
			builder.setMessage("Do you want to enable location services?")
					.setPositiveButton("Yes", dialogClickListener)
					.setNegativeButton("No", dialogClickListener).show();

		}
	}

	/**
	 * Methode die nagaat of de gebruiker met internet verbonden is.
	 * 
	 * @return True als de gebruiker met internet verbonden is.
	 */
	private boolean isNetworkConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null) {
			// Geen actieve netwerken
			return true;
		}

		return false;
	}

	/**
	 * Methode die de currentCity en currentCountry variable invult, gebaseerd
	 * op het meegegeven Location object.
	 * 
	 * @param currentLocation
	 *            De huidige locatie.
	 */
	private void getCityOfLocation(Location currentLocation) {
		double lat = currentLocation.getLatitude();
		double lng = currentLocation.getLongitude();

		Geocoder geocoder = new Geocoder(getApplicationContext(),
				Locale.getDefault());
		List<Address> addresses;
		try {
			if (isNetworkConnected()) {
				addresses = geocoder.getFromLocation(lat, lng, 1);
				Address first = addresses.get(0);
				currentCity = first.getLocality();
				currentCountry = first.getCountryName();
			}
		} catch (IOException e) {
			e.printStackTrace();
			currentCity = "No location Found";
			currentCountry = "No location Found";
		}
	}

	/**
	 * Variabele die ervoor zorgt dat de huidige locatie wordt geupdated indien
	 * nodig. Dit is wanneer de huidige locatie verandert of wanneer de
	 * gebruiker verbinding maakt met het internet.
	 */
	private final LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			getCityOfLocation(location);
			Log.i("Location", "Updated location");
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.i("Provider", provider);
			initCurrentLocation();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	// ////////////////////////////////////////////////////
	// / EmotionSelectionListener ///
	// ////////////////////////////////////////////////////

	// Aangeroepen wanneer de gebruiker een emotie opneemt (Android 4.0).
	@Override
	public void onEmotionSelected(Emotion emoticon) {
		// doe niets.
	}

	// Aangeroepen wanneer de gebruiker dubbelklikt op een emotie (Android 2.3).
	@Override
	public void onEmotionDoubleTapped(Emotion emoticon) {
		userSelectedEmoticon(emoticon);
	}

	// Aangeroepen wanneer de gebruiker een emotie dropt op de grote (lege)
	// emoticon (Android 4.0).
	@Override
	public void onEmotionDeselected(Emotion emoticon) {
		// userSelectedEmoticon(emoticon);
	}

	// ////////////////////////////////////////////////////
	// / Swipeable ///
	// ////////////////////////////////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.findingemos.felicity.general.SimpleSwipeListener#onSwipeLeft()
	 */
	@Override
	public void onSwipeLeft() {
		Log.d("--scrolled--", "scrolled to the right!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.findingemos.felicity.general.SimpleSwipeListener#onSwipeRight()
	 */
	@Override
	public void onSwipeRight() {
		Log.d("--scrolled--", "scrolled to the left!");
		switchToVisualization();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.findingemos.felicity.util.Swipeable#onSwipeUp()
	 */
	@Override
	public void onSwipeUp() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.findingemos.felicity.util.Swipeable#onSwipeDown()
	 */
	@Override
	public void onSwipeDown() {
	}

	/**
	 * Post een tweet op twitter. Deze tweet moet korter of gelijk aan het
	 * twitter limiet zijn.
	 * 
	 * @param tweet
	 */
	private void updateStatus(String tweet) {
		// Veiligheid check om niet te crashen
		if (tweet.length() > Constants.TWEET_LIMIT) {
			// zou niet mogen optreden.
			System.out.println("Tweet te lang!");
			return;
		}

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		String accessToken = settings.getString("twitter_access_token", null);
		String accessTokenSecret = settings.getString(
				"twitter_access_token_secret", null);
		if (haveNetworkConnection(this)) {
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(Constants.CONSUMER_KEY);
			builder.setOAuthConsumerSecret(Constants.CONSUMER_SECRET);
			builder.setOAuthAccessToken(accessToken);
			builder.setOAuthAccessTokenSecret(accessTokenSecret);
			Configuration conf = builder.build();
			Twitter t = new TwitterFactory(conf).getInstance();

			try {
				t.updateStatus(tweet);

			} catch (TwitterException e) {
				Log.e("EmotionActivity", "Het posten van de tweet is mislukt");
			}
		} else {
			Toast.makeText(this, "No access to Internet..please try again",
					Toast.LENGTH_LONG).show();
		}
	}

	public static boolean haveNetworkConnection(Context context) {
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					haveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					haveConnectedMobile = true;
		}
		return haveConnectedWifi || haveConnectedMobile;
	}
}
