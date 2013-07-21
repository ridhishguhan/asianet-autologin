/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package riddimon.android.asianetautologin;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
	private static final String SP_FILE = "asianet_auto_login_preferences.xml";
	public static final String URL = "url";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String KEEP_ALIVE = "keep_alive";
	public static final String SHOW_NOTIF = "show_notif";
	public static final String SSID = "ssid";
	public static final String LOG_IN = "log_in";
	public static final String LOG_IN_TIME = "log_in_time";

	public static String getString(Context context, String name, String def) {
		SharedPreferences sp = context.getSharedPreferences(SP_FILE, 0);
		return sp.getString(name, def);
	}

	public static void putString(Context context, String name, String value) {
		SharedPreferences.Editor spe = context.getSharedPreferences(SP_FILE, 0)
				.edit();
		spe.putString(name, value).commit();
	}

	public static Integer getInt(Context context, String name, int def) {
		SharedPreferences sp = context.getSharedPreferences(SP_FILE, 0);
		return sp.getInt(name, def);
	}

	public static void putInt(Context context, String name, int value) {
		SharedPreferences.Editor spe = context.getSharedPreferences(SP_FILE, 0)
				.edit();
		spe.putInt(name, value).commit();
	}

	public static Boolean getBoolean(Context context, String name
			, Boolean def) {
		SharedPreferences sp = context.getSharedPreferences(SP_FILE, 0);
		return sp.getBoolean(name, def);
	}

	public static void putBoolean(Context context, String name, boolean value) {
		SharedPreferences.Editor spe = context.getSharedPreferences(SP_FILE, 0)
				.edit();
		spe.putBoolean(name, value).commit();
	}

	public static Long getLong(Context context, String name, long def) {
		SharedPreferences sp = context.getSharedPreferences(SP_FILE, 0);
		return sp.getLong(name, def);
	}

	public static void putLong(Context context, String name, long value) {
		SharedPreferences.Editor spe = context.getSharedPreferences(SP_FILE, 0)
				.edit();
		spe.putLong(name, value).commit();
	}

}
