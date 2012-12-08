package com.findingemos.felicity.visualization;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.findingemos.felicity.R;
import com.findingemos.felicity.emoticon.Emotion;
import com.findingemos.felicity.emoticon.EmotionActivity;

public class FilterActivity extends FragmentActivity {

	private String TIME;
	private String LOCATION;
	private String WHO;
	private String DOING;

	private String[] currentFilter = new String[4];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filter_activity);

		TIME = getApplicationContext().getResources().getString(
				R.string.visualizations_time);
		LOCATION = getApplicationContext().getResources().getString(
				R.string.visualizations_location);
		WHO = getApplicationContext().getResources().getString(
				R.string.visualizations_who);
		DOING = getApplicationContext().getResources().getString(
				R.string.visualizations_doing);

		Button filterButton = (Button) findViewById(R.id.filterButton);
		filterButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				List<VisualizationResult> results = EmotionActivity.DATABASE.readWithFilters(currentFilter[0],
						currentFilter[1], currentFilter[2], currentFilter[3],
						getApplicationContext());
				transformToSelectionCount(results);
				Intent intent =  new Intent(getApplicationContext(),VisualizationActivity.class);
				startActivity(intent);
				
			}
		});

		ArrowButton leftTime = (ArrowButton) findViewById(R.id.timeArrowLeft);
		leftTime.setArrowDirection(ArrowButton.DIRECTION_LEFT);
		ArrowButton rightTime = (ArrowButton) findViewById(R.id.timeArrowRight);
		rightTime.setArrowDirection(ArrowButton.DIRECTION_RIGHT);
		OptionSpinner timeSpinner = (OptionSpinner) findViewById(R.id.timeSpinner);
		timeSpinner.setLeftButton(leftTime);
		timeSpinner.setRightButton(rightTime);
		timeSpinner.setOptions(TIME, "Today", "Week");
		timeSpinner.addListener(new SpinnerListener() {

			@Override
			public void optionChanged(int index, String name) {
				if (name != TIME)
					currentFilter[0] = name;
			}
		});

		ArrowButton leftLocation = (ArrowButton) findViewById(R.id.locationArrowLeft);
		leftLocation.setArrowDirection(ArrowButton.DIRECTION_LEFT);
		ArrowButton rightLocation = (ArrowButton) findViewById(R.id.locationArrowRight);
		rightLocation.setArrowDirection(ArrowButton.DIRECTION_RIGHT);
		OptionSpinner locationSpinner = (OptionSpinner) findViewById(R.id.locationSpinner);
		locationSpinner.setLeftButton(leftLocation);
		locationSpinner.setRightButton(rightLocation);
		locationSpinner.setOptions(LOCATION, "Lanaken");
		locationSpinner.addListener(new SpinnerListener() {

			@Override
			public void optionChanged(int index, String name) {
				if (name != LOCATION)
					currentFilter[1] = name;
			}
		});

		ArrowButton leftWho = (ArrowButton) findViewById(R.id.whoArrowLeft);
		leftWho.setArrowDirection(ArrowButton.DIRECTION_LEFT);
		ArrowButton rightWho = (ArrowButton) findViewById(R.id.whoArrowRight);
		rightWho.setArrowDirection(ArrowButton.DIRECTION_RIGHT);
		OptionSpinner whoSpinner = (OptionSpinner) findViewById(R.id.whoSpinner);
		whoSpinner.setLeftButton(leftWho);
		whoSpinner.setRightButton(rightWho);
		whoSpinner.setOptions(WHO, "Robin");
		locationSpinner.addListener(new SpinnerListener() {

			@Override
			public void optionChanged(int index, String name) {
				if (name != WHO)
					currentFilter[2] = name;
			}
		});

		ArrowButton leftDoing = (ArrowButton) findViewById(R.id.doingArrowLeft);
		leftDoing.setArrowDirection(ArrowButton.DIRECTION_LEFT);
		ArrowButton rightDoing = (ArrowButton) findViewById(R.id.doingArrowRight);
		rightDoing.setArrowDirection(ArrowButton.DIRECTION_RIGHT);
		OptionSpinner doingSpinner = (OptionSpinner) findViewById(R.id.doingSpinner);
		doingSpinner.setLeftButton(leftDoing);
		doingSpinner.setRightButton(rightDoing);
		doingSpinner.setOptions(EmotionActivity.DATABASE.readActivities());
		doingSpinner.addListener(new SpinnerListener() {

			@Override
			public void optionChanged(int index, String name) {
				if (name != DOING)
					currentFilter[3] = name;
			}
		});
	}

	protected void transformToSelectionCount(List<VisualizationResult> results) {
		
		int[] counts = new int[Emotion.values().length];
		for(VisualizationResult result : results) {
			int id = result.getEmotionId();
			int oldCount = counts[id];
			counts[id] = oldCount + 1;
		}
		
		Emotion[] resultSet  = Emotion.values();
		for(Emotion emotion : resultSet) {
			emotion.setSelectionCount(counts[emotion.getUniqueId()]);
		}
		
		VisualizationActivity.setCurrentData(resultSet);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}
