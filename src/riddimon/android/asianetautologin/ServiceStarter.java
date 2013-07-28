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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class ServiceStarter extends BroadcastReceiver {
	private static final Logger logger = LoggerFactory.getLogger(ServiceStarter.class);
	public static final String ACTION_STATE_CHANGE = "android.net.wifi.STATE_CHANGE";
	private static final String ACTION_SUPPLICANT_CONN_CHANGE
			= "android.net.wifi.supplicant.CONNECTION_CHANGE"; 

	@Override
	public void onReceive(Context context, Intent intent) {
		boolean login = SettingsManager.getBoolean(context, SettingsManager
				.LOG_IN, true);
		boolean stopService = false;
		boolean startService = false;
		logger.info("Service Starter : Login enabled : {}", login);
		if (intent.getAction().equals(ACTION_STATE_CHANGE)) {
			NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager
					.EXTRA_NETWORK_INFO);
			logger.info("WiFi state change : connected = {}", networkInfo.isConnected());
			logger.info("WiFi detailed state : {}", networkInfo.getDetailedState());
			//String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
			//WifiInfo wi = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
			if (!networkInfo.isConnected() && (networkInfo.getDetailedState()
					.equals(NetworkInfo.DetailedState.DISCONNECTED)
					|| networkInfo.getDetailedState().equals(NetworkInfo
					.DetailedState.FAILED))) {
				stopService = true;
			} else if (networkInfo.isConnected()){
				if (login && NetworkUtil.isConnectedToProperNetwork(context)) {
					logger.info("WiFi connected to proper network");
					startService = true;
				} else {
					if (login) logger.info("WiFi NOT connected to proper network");
					else logger.info("Logged out");
					stopService = true;
				}
			}
		} else if (intent.getAction().equals(ACTION_SUPPLICANT_CONN_CHANGE)) {
			/*
			boolean connected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED
					, false);
			if (connected && NetworkUtil.isConnectedToProperNetwork(context)) {
				startService = true;
			} else {
				stopService = true;
			}
			*/
		}

		if (startService) {
			logger.info("Starting service");
			// initiate login, as it would schedule keepAlive reqs
			Intent service = new Intent(context, LoginService.class);
			service.setAction(LoginService.ACTION_LOGIN);
			context.startService(service);
		} else if (stopService) {
			logger.info("Stopping service");
			Intent service = new Intent(context, LoginService.class);
			context.stopService(service);
		}

	}
}