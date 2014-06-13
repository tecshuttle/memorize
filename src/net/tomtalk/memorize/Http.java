package net.tomtalk.memorize;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;

public class Http {
    public MemorizeActivity me;

    public Http(MemorizeActivity activity) {
	me = activity;
    }

    public void post(String url) {
	new UpLoadTask(me).execute(url);
    }

    public static String POST(String post_rows_json, String uid) {
	InputStream inputStream = null;
	String result = "";

	try {
	    HttpClient client = new DefaultHttpClient();

	    // 设置post参数
	    List<NameValuePair> params = new ArrayList<NameValuePair>();

	    params.add(new BasicNameValuePair("uid", uid));
	    params.add(new BasicNameValuePair("rows_json", post_rows_json));

	    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);

	    HttpPost request = new HttpPost("http://42.121.108.182/method.php");
	    request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
	    request.setEntity(entity);

	    // 发送请求
	    HttpResponse httpResponse = client.execute(request);
	    inputStream = httpResponse.getEntity().getContent();

	    if (inputStream != null)
		result = convertInputStreamToString(inputStream);
	    else
		result = "error";

	} catch (Exception e) {
	    // none
	}

	return result;
    }

    public static String DownLoad(String post_rows_json, String uid) {
	InputStream inputStream = null;
	String result = "";

	try {
	    HttpClient client = new DefaultHttpClient();

	    // 设置post参数
	    List<NameValuePair> params = new ArrayList<NameValuePair>();
	    params.add(new BasicNameValuePair("uid", uid));
	    params.add(new BasicNameValuePair("rows_json", post_rows_json));

	    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);

	    HttpPost request = new HttpPost("http://42.121.108.182/sync_download.php");
	    request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
	    request.setEntity(entity);

	    // 发送请求
	    HttpResponse httpResponse = client.execute(request);
	    inputStream = httpResponse.getEntity().getContent();

	    if (inputStream != null)
		result = convertInputStreamToString(inputStream);
	    else
		result = "error";

	} catch (Exception e) {
	    // none
	}

	return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	String line = "";
	String result = "";
	while ((line = bufferedReader.readLine()) != null)
	    result += line;

	inputStream.close();
	return result;
    }

    private class UpLoadTask extends AsyncTask<String, Void, String> {
	private MemorizeActivity me;

	public UpLoadTask(MemorizeActivity activity) {
	    me = activity;
	}

	@Override
	protected String doInBackground(String... urls) {
	    return POST(urls[0], urls[1]);
	}

	@Override
	protected void onPostExecute(String result) {
	    me.upload_to_site(result);
	}
    }

    private class DownLoadTask extends AsyncTask<String, Void, String> {
	private MemorizeActivity me;

	public DownLoadTask(MemorizeActivity activity) {
	    me = activity;
	}

	@Override
	protected String doInBackground(String... urls) {
	    return DownLoad(urls[0], urls[1]);
	}

	@Override
	protected void onPostExecute(String result) {
	    me.update_from_site(result);
	}
    }

    public boolean isConnected() {
	boolean flag = false;
	ConnectivityManager manager = (ConnectivityManager) me
		.getSystemService(Context.CONNECTIVITY_SERVICE);
	if (manager.getActiveNetworkInfo() != null) {
	    flag = manager.getActiveNetworkInfo().isAvailable();
	}

	return flag;
    }

    public void site_sync(String rows_json) {
	if (!isConnected()) {
	    // me.toast("网络不可用");
	    return;
	}

	new UpLoadTask(me).execute(rows_json, me.getUid());
	new DownLoadTask(me).execute(rows_json, me.getUid());
    }

    // 通过name、pwd取用户id
    private class getUidTask extends AsyncTask<String, Void, String> {
	private MemorizeActivity me;

	public getUidTask(MemorizeActivity activity) {
	    me = activity;
	}

	@Override
	protected String doInBackground(String... urls) {
	    return getUidHttp(urls[0]);
	}

	@Override
	protected void onPostExecute(String result) {
	    me.setting.setUid(result);
	}
    }

    public static String getUidHttp(String name_pwd) {
	InputStream inputStream = null;
	String result = "";

	String[] np = name_pwd.split("\\|");
	String name = np[0];
	String pwd = np[1];

	try {
	    HttpClient client = new DefaultHttpClient();

	    // 设置post参数
	    List<NameValuePair> params = new ArrayList<NameValuePair>();
	    params.add(new BasicNameValuePair("name", name));
	    params.add(new BasicNameValuePair("pwd", pwd));

	    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);

	    HttpPost request = new HttpPost("http://42.121.108.182/get_uid.php");
	    request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
	    request.setEntity(entity);

	    // 发送请求
	    HttpResponse httpResponse = client.execute(request);
	    inputStream = httpResponse.getEntity().getContent();

	    if (inputStream != null)
		result = convertInputStreamToString(inputStream);
	    else
		result = "error";

	} catch (Exception e) {
	    // none
	}

	return result;
    }

    public void getUid(String name, String pwd) {
	if (!isConnected()) {
	    me.toast("网络不可用");
	    return;
	}

	new getUidTask(me).execute(name + "|" + pwd);
    }
}

// end file
