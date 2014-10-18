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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

/**
 * Utility class for HTTP calls
 * @author ridhishguhan
 */
public class HttpUtils {
	// in case the ISP wants to block us using user agent string, they can't
	public static final String userAgent = "Mozilla/5.0 (Windows NT 6.1)"
			+" AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1468.0"
			+ " Safari/537.36";
	public enum HttpMethod {
		GET, POST, PUT, DELETE, HEAD, INVALID
	}
	private static final Logger logger = LoggerFactory
			.getLogger(HttpUtils.class);

	private Boolean debug = Boolean.FALSE;
	private String version = "";
	private Context context;
	private static HttpUtils instance;

	private HttpUtils(Context context) {
		// private constructor to prevent instantiation
		this.context = context;
		try {
			// get version number to be set as part of user agent string
			version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {}
		if (debug) {
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
			try {
				TrustManager[] trustManagers = new X509TrustManager[1];
				trustManagers[0] = new TrustAllManager();

				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustManagers, null);
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			} catch (Exception ex) {}
		}
		// We don't enable response cache because this scenario requires fresh
		// data every time
		//enableHttpResponseCache();
	}

	private static HttpUtils getInstance(Context context) {
		if (instance == null)
			instance = new HttpUtils(context);
		return instance;
	}

	/**
	 * Used to refer to connection status while executing HTTP transactions
	 */
	public enum ConnectionStatus {
		BOTH_CONNECTED, WIFI_CONNECTED, DATA_CONNECTED, CONNECTING, NO_CONNECTION
	}

	/**
	 * Used for bypass problem with self-signed certificates in SSL connections
	 */
	public static class TrustAllManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1)	throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}


	/**
	 * Uses ConnectivityManager API to check for connectivity
	 * @param context
	 * @return true || false
	 */
	public boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		// test for connection
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			logger.info("Connection available : <", cm.getActiveNetworkInfo().toString() + ">");
			return true;
		} else {
			// no connection
			logger.info("No connectivity.");
			return false;
		}
	}

	private static String getEncodedParameters(Map<String, String> params) {
		StringBuilder s = new StringBuilder();
		synchronized(params) {
			for (String key : params.keySet()) {
				if (s.length() != 0) {
					s.append("&");
				}
				String value = params.get(key).toString();
				String encodedValue = null;
				try {
					encodedValue = URLEncoder.encode(value, "utf8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					logger.warn("Could not encode URL");
				}
				if (!TextUtils.isEmpty(encodedValue)) {
					s.append(key).append("=").append(encodedValue);
				}

				//logger.trace("Encoding from: {}", value);
				//logger.trace("Encoding to  : {}", encodedValue);
			}
		}
		return s.toString();
	}

	/**
	 * Do an HTTP GET / DELETE / POST / PUT to the url with the parameters
	 * @param context the activity or service context. Preferably, the application context
	 * @param method the HttpMethod HttpMethod.GET / .POST/ .PUT / .DELETE
	 * @param url the url without the parameters appended
	 * @param paramz the parameters to be added to the GET/DELETE request
	 * @return <b>response</b> the string response
	 * TODO: throw appropriate exceptions to signal errors
	 */
	public static HttpResponse execute(Context context, HttpMethod method, String url
			, Map<String, String> paramz, List<BasicHeader> headers) {
		HttpUtils instance = getInstance(context);
		return instance.execute(method, url, paramz, headers);
	}

	public static HttpResponse execute(Context context, HttpMethod method, String url
			, Map<String, String> paramz) {
		HttpUtils instance = getInstance(context);
		return instance.execute(method, url, paramz, null);
	}

	private HttpResponse execute(HttpMethod method, String url, Map<String, String> paramz
			, List<BasicHeader> headers ) {
		if (!(method.equals(HttpMethod.GET) || method.equals(HttpMethod.DELETE)
				|| method.equals(HttpMethod.PUT) || method.equals(HttpMethod.POST)
                || method.equals(HttpMethod.HEAD)) || TextUtils.isEmpty(url)) {
			logger.error("Invalid request : {} | {}", method.name(), url);
			return null;
		}
		logger.debug("HTTP {} : {}", method.name(), url);
		String query = paramz == null ? null : getEncodedParameters(paramz);
		logger.trace("Query String : {}", query);
		HttpResponse httpResponse = null;
		try {
				HttpUriRequest req = null;
				switch (method) {
				case GET:
                case HEAD:
				case DELETE:
					url = paramz != null && paramz.size() > 0 ? url + "?"
							+ query : url;
                    if (method.equals(HttpMethod.GET)) {
						req = new HttpGet(url);
					} else if (method.equals(HttpMethod.DELETE)){
						req = new HttpDelete(url);
					} else if (method.equals(HttpMethod.HEAD)) {
                        req = new HttpHead(url);
                    }
					break;
				case POST:
				case PUT:
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					if (paramz != null) {
						for (Entry<String, String> entry : paramz.entrySet()) {
							params.add(new BasicNameValuePair(entry.getKey()
									, entry.getValue()));	
						}
					}
					UrlEncodedFormEntity entity = paramz == null ? null
							: new UrlEncodedFormEntity(params);
//					HttpEntity entity = TextUtils.isEmpty(query) ? null
//							: new StringEntity(query);
					BasicHeader header = new BasicHeader(HTTP.CONTENT_ENCODING
							, "application/x-www-form-urlencoded");
					if (method.equals(HttpMethod.PUT)) {
						HttpPut putr = new HttpPut(url);
						if (entity != null) {
							putr.setHeader(header);
							putr.setEntity(entity);
							req = putr;
						}
					} else if (method.equals(HttpMethod.POST)) {
						HttpPost postr = new HttpPost(url);
						if (entity != null) {
							postr.setHeader(header);
							if (headers != null) {
								for (BasicHeader h : headers) {
									postr.addHeader(h);
								}
							}
							postr.setEntity(entity);
							req = postr;
						}
					}
				}
				httpResponse = HttpManager.execute(req, debug, version);
		} catch (IOException e1) {
			e1.printStackTrace();
			logger.error("HTTP request failed : {}", e1);
		}
		return httpResponse;
	}

	public static String readStreamIntoString(InputStream in) {
		StringBuilder builder = new StringBuilder();
		int buf_size = 50 * 1024; // read in chunks of 50 KB
		ByteArrayBuffer bab = new ByteArrayBuffer(buf_size);
		int read = 0;
		BufferedInputStream bis = new BufferedInputStream(in);
		if (bis != null) {
			byte buffer[] = new byte[buf_size];
			try {
				while ((read = bis.read(buffer, 0, buf_size)) != -1) {
					//builder.append(new String(buffer, "utf-8"));
					bab.append(buffer, 0, read);
				}
				builder.append(new String(bab.toByteArray(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return builder.toString();
	}

	/**
	 * Uses TelephonyManager and WifiManager to check for network connectivity.
	 * Also incorporates CONNECTING state for retry scenarios.
	 * @param context
	 * @return ConnectionStatus
	 */
	public ConnectionStatus isConnectedOLD(Context context) {
		boolean data = false, wifi = false;
		boolean data_connecting = false, wifi_connecting = false;
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		WifiManager wm = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		int ds = tm.getDataState();
		int ws = wm.getWifiState();
		switch (ds) {
		case TelephonyManager.DATA_CONNECTED:
			data = true;
			break;
		case TelephonyManager.DATA_CONNECTING:
			data_connecting = true;
		default:
			data = false;
			data_connecting = false;
		}

		switch (ws) {
		case WifiManager.WIFI_STATE_ENABLING:
			wifi_connecting = true;
		case WifiManager.WIFI_STATE_DISABLING:
		case WifiManager.WIFI_STATE_DISABLED:
		case WifiManager.WIFI_STATE_UNKNOWN:
			wifi = false;
			break;
		case WifiManager.WIFI_STATE_ENABLED:
			WifiInfo wi = wm.getConnectionInfo();
			if (wi != null)
				wifi = true;
			break;
		}

		if (wifi && data) return ConnectionStatus.BOTH_CONNECTED;
		else if (wifi && data_connecting) return ConnectionStatus.WIFI_CONNECTED;
		else if (data && wifi_connecting) return ConnectionStatus.DATA_CONNECTED;
		else if (wifi_connecting || data_connecting) return ConnectionStatus.CONNECTING;
		return ConnectionStatus.NO_CONNECTION;
	}
}
