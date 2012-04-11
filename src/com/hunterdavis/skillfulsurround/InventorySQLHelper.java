package com.hunterdavis.skillfulsurround;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;


public class InventorySQLHelper extends android.database.sqlite.SQLiteOpenHelper {
	private static final String DATABASE_NAME = "skillfulsurround.db";
	private static final int DATABASE_VERSION = 1;

	// Table name
	public static final String TABLE = "skillfulsurround";

	// Columns

	public static final String SCORES = "scores";
	public static final String NAMES = "names";

	public InventorySQLHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table " + TABLE + "( " + BaseColumns._ID
				+ " integer primary key autoincrement, "+ SCORES + " integer not null, " + NAMES + " text not null);";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion >= newVersion)
			return;

		String sql = null;
		if (oldVersion == 1)
			sql = "alter table " + TABLE + " add note text;";
		if (oldVersion == 2)
			sql = "";

		if (sql != null)
			db.execSQL(sql);
	}

}
