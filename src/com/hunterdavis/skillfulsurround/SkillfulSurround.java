package com.hunterdavis.skillfulsurround;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdView;

public class SkillfulSurround extends Activity {

	InventorySQLHelper scoreData = new InventorySQLHelper(this);
	ArrayAdapter<String> m_adapterForHighScores;
	Panel mypanel = null;
	int currentScore = 0;
	String lastHighScoreName = "";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mypanel = (Panel) findViewById(R.id.SurfaceView01);
		m_adapterForHighScores = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line);

		// set an adapter for our spinner
		// m_adapterForSpinner
		// .setDropDownViewResource(android.R.layout.);
		
		// set an adapter for our difficult level
		// no onclick or onselect necessary
		Spinner spinner = (Spinner) findViewById(R.id.difficultyspin);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.difficulty, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setOnItemSelectedListener(new MyUnitsOnItemSelectedListener());
		spinner.setAdapter(adapter);
		spinner.setSelection(0);
		
	
		

		// Create an anonymous implementation of OnClickListener
		OnClickListener highscoresListner = new OnClickListener() {
			public void onClick(View v) {
				// do something when the button is clicked
				m_adapterForHighScores.clear();

				Cursor cursor = getScoresCursor();
				if (cursor.getCount() > 0) {
					while (cursor.moveToNext()) {
						String highscore = cursor.getString(2) + " - "
								+ cursor.getInt(1);
						m_adapterForHighScores.add(highscore);
					}
				} else {
					m_adapterForHighScores.add("Hunter - 100");
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(
						v.getContext());
				builder.setTitle("High Scores");
				builder.setAdapter(m_adapterForHighScores, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								// Do something with the selection
								dialog.dismiss();
							}
						});

				
				AlertDialog alert = builder.create();
				alert.show();

			}
		};

		// Create an anonymous implementation of OnClickListener
		OnClickListener saveButtonListner = new OnClickListener() {
			public void onClick(View v) {
				// do something when the button is clicked
				// Boolean didWeSave = saveImage(v.getContext());
				// TODO GET NAME AND SAVE TO HIGH SCORES
				currentScore = mypanel.getScore();
				AlertDialog.Builder alert = new AlertDialog.Builder(
						v.getContext());
				mypanel.setMutex(true);

				alert.setTitle("Your Name?");
				alert.setMessage("Please Enter Your Name For the High Score List");

				// Set an EditText view to get user input
				final EditText input = new EditText(v.getContext());
				input.setText(lastHighScoreName);
				alert.setView(input);

				alert.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								String tempName = input.getText().toString()
										.trim();

								if (tempName.length() < 1) {
									tempName = "Unnamed Player";
								}
								lastHighScoreName = tempName;
								int score = currentScore;
								SQLiteDatabase db = scoreData
										.getWritableDatabase();
								ContentValues values = new ContentValues();
								values.put(InventorySQLHelper.NAMES, tempName);
								values.put(InventorySQLHelper.SCORES, score);
								long latestRowId = db.insert(
										InventorySQLHelper.TABLE, null, values);
								db.close();
								mypanel.enableKillMutex();
								mypanel.reset();
							}

						});

				alert.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Canceled.
								mypanel.enableKillMutex();
								mypanel.reset();
							}
						});

				alert.show();
				

			}
		};

		// Create an anonymous implementation of OnClickListener
		OnClickListener resetButtonListner = new OnClickListener() {
			public void onClick(View v) {
				// do something when the button is clicked
				// Boolean didWeSave = saveImage(v.getContext());
				mypanel.reset();
			}
		};

		// Create an anonymous implementation of OnClickListener
		OnClickListener screenshotButtonListner = new OnClickListener() {
			public void onClick(View v) {
				// do something when the button is clicked
				// Boolean didWeSave = saveImage(v.getContext());
				mypanel.saveImage(v.getContext(), v);
			}
		};

		Button highscoresButton = (Button) findViewById(R.id.highscoresButton);
		highscoresButton.setOnClickListener(highscoresListner);

		Button saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(saveButtonListner);
		
		Button resetButton = (Button) findViewById(R.id.resetButton);
		resetButton.setOnClickListener(resetButtonListner);

		ImageButton ssButton = (ImageButton) findViewById(R.id.screenshotButton);
		ssButton.setOnClickListener(screenshotButtonListner);

		Toast.makeText(getBaseContext(), "Draw a Line Around Everything Without Touching",
				Toast.LENGTH_LONG).show();

		// Look up the AdView as a resource and load a request.
		AdView adView = (AdView) this.findViewById(R.id.adView);
		adView.loadAd(new AdRequest());

	} // end of oncreate



	protected void onPause() {
		super.onPause();
		mypanel.terminateThread();
		System.gc();
	}

	protected void onResume() {
		super.onResume();
		if (mypanel.surfaceCreated == true) {
			mypanel.createThread(mypanel.getHolder());
		}
	}
	
	// set up the listener class for spinner
	class MyUnitsOnItemSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			mypanel.setDifficulty(pos);
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	}

	// this is called when the screen rotates.
	// (onCreate is no longer called when screen rotates due to manifest, see:
	// android:configChanges)
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// setContentView(R.layout.main);

		// InitializeUI();
	}

	private Cursor getScoresCursor() {
		SQLiteDatabase db = scoreData.getReadableDatabase();
		Cursor cursor = db.query(InventorySQLHelper.TABLE, null, null, null,
				null, null, InventorySQLHelper.SCORES + " desc");
		startManagingCursor(cursor);
		return cursor;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {

		}
	}

}