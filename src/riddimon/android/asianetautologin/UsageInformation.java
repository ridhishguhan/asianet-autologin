package riddimon.android.asianetautologin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicHeader;

import riddimon.android.asianetautologin.HttpUtils.HttpMethod;
import android.content.Context;
import android.util.Log;

public class UsageInformation {
	
	Context mContext;
	String cookieInformation = null;
	final String ACCOUNTINFOURL = "https://myaccount.adlkerala.com/";
	
	UsageInformation(Context context){
		this.mContext = context;
	}
	
	void performLogin(){
		String username = SettingsManager.getString(mContext, SettingsManager.USERNAME, null);
		String password = SettingsManager.getString(mContext, SettingsManager.PASSWORD, null);
		Map<String,String> params = new HashMap<String,String>();
		if(username != null){
			params.put("username", username);
			params.put("pass", password);
		
		
			HttpResponse res = HttpUtils.execute(mContext, HttpMethod.POST, ACCOUNTINFOURL + "login.php", params);
			Header[] headers = res.getHeaders("Set-Cookie");
			for(Header h : headers){

				cookieInformation = h.getValue();
				int end = cookieInformation.indexOf(";");
				cookieInformation = cookieInformation.substring(0, end);
				getPageContent();
			}
		}
	}
	
	String getPageContent(){
		if(cookieInformation == null){
			performLogin();
		}
		List<BasicHeader> headers = new ArrayList<BasicHeader>();
		headers.add(new BasicHeader("Cookie", cookieInformation));
		HttpResponse res = HttpUtils.execute(mContext, HttpMethod.GET, ACCOUNTINFOURL, null, headers);
		String responseString = null;
		try {
			responseString = new BasicResponseHandler().handleResponse(res);
		} catch (HttpResponseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String pattern = "<table.+Package Details.+?Data Transfer.+?class=\"celldata\">([0-9].+?)&nbsp;.+?</table>";
		Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
		Matcher matcher = r.matcher(responseString);
		if(matcher.find()){
			responseString = matcher.group(1);
		}
		
		return responseString;
	}
	
	
	
	

}
