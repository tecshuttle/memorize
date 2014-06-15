package net.tomtalk.memorize;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context; //数据库支持
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase; //数据库支持
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector; //手势
import android.view.GestureDetector.OnGestureListener; //手势
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent; //手势
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast; //简易提示
import android.widget.ViewFlipper;

public class MemorizeActivity extends Activity implements OnGestureListener {
    public SQLiteDatabase db;
    public ArrayList<HashMap<String, Object>> listItem;
    public String current_view = "";
    public String sync_msg = "";
    public ViewFlipper mViewFlipper;
    public MyCustomAdapter listAdapter;

    public Common common = new Common();
    public Http http = new Http(this);
    public Setting setting = new Setting(this);
    public Add add = new Add(this);

    private GestureDetector gesture_detector;
    private Play play = new Play(this);

    private ListView listview;
    private Boolean inputOpen = false;
    private MediaPlayer answer_correct_snd;
    private MediaPlayer answer_incorrect_snd;
    private MediaPlayer save_snd;
    private String db_file_name = "memorize.db";

    private class MyCustomAdapter extends BaseAdapter {
	private MemorizeActivity me;

	private static final int TYPE_BUG = 1;
	private static final int TYPE_TODO = 2;
	private static final int TYPE_MEMO = 3;
	private static final int TYPE_QUESTION = 4;

	private HashMap<String, String[]> item_type = new HashMap<String, String[]>();

	private ArrayList<HashMap<String, Object>> mData = new ArrayList<HashMap<String, Object>>();
	private LayoutInflater mInflater;

	public MyCustomAdapter(MemorizeActivity activity) {
	    me = activity;
	    mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	    String sq = "SELECT * FROM item_type ";
	    Cursor cs = db.rawQuery(sq, new String[] {});
	    while (cs.moveToNext()) {
		String name = cs.getString(cs.getColumnIndex("name"));
		String priority = cs.getString(cs.getColumnIndex("priority"));
		String color = cs.getString(cs.getColumnIndex("color"));

		String date[] = { priority, color };
		item_type.put(name, date);
	    }
	    cs.close();
	}

	public void addItem(HashMap<String, Object> map) {
	    mData.add(map);
	    notifyDataSetChanged();
	}

	// 这个方法主要用于编辑条目后，更新item的显示。
	public void setItem(int idx, HashMap<String, Object> map) {
	    mData.set(idx, map);
	    notifyDataSetChanged();
	}

	public int getItemViewType(HashMap<String, Object> question) {
	    int type = 0;
	    int question_type[] = me.common.get_item_type(question.get("Type").toString());

	    if (question_type[0] == 0 && question_type[1] == 0) {
		type = TYPE_QUESTION;
	    } else if (question_type[0] == 10) {
		type = TYPE_MEMO;
	    } else if (question_type[0] == 50) {
		type = TYPE_TODO;
	    } else if (question_type[0] == 80) {
		type = TYPE_BUG;
	    }

	    return type;
	}

	@Override
	public int getCount() {
	    return mData.size();
	}

	@Override
	public HashMap<String, Object> getItem(int position) {

	    return (HashMap<String, Object>) mData.get(position);
	}

	@Override
	public long getItemId(int position) {
	    return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    ViewHolder holder = new ViewHolder();

	    convertView = mInflater.inflate(R.layout.list_item, null);
	    holder.top_bar = (LinearLayout) convertView.findViewById(R.id.top_bar);
	    holder.sync_state = (TextView) convertView.findViewById(R.id.SyncState);
	    holder.type = (TextView) convertView.findViewById(R.id.list_item_type);
	    holder.familiar = (TextView) convertView.findViewById(R.id.list_item_familiar);
	    holder.date = (TextView) convertView.findViewById(R.id.list_item_date);
	    holder.rec_id = (TextView) convertView.findViewById(R.id.ItemId);
	    holder.title = (TextView) convertView.findViewById(R.id.ItemTitle);
	    holder.text = (TextView) convertView.findViewById(R.id.ItemText);

	    HashMap<String, Object> question = mData.get(position);
	    int type = getItemViewType(question);
	    int color = 0;
	    switch (type) {
	    case TYPE_BUG:
		color = 0xFF000000 + Integer.parseInt(item_type.get("bug")[1], 16);
		holder.top_bar.setVisibility(/* GONE = */8);
		holder.title.setTextColor(color);
		holder.text.setTextColor(color);
		break;
	    case TYPE_TODO:
		color = 0xFF000000 + Integer.parseInt(item_type.get("todo")[1], 16);
		holder.top_bar.setVisibility(/* GONE = */8);
		holder.title.setTextColor(color);
		holder.text.setTextColor(color);
		break;
	    case TYPE_MEMO:
		int mtime = Integer.parseInt(question.get("mtime").toString());
		color = getMemoColor(mtime) + Integer.parseInt(item_type.get("memo")[1], 16);
		holder.top_bar.setVisibility(/* GONE = */8);
		holder.title.setTextColor(color);
		holder.text.setTextColor(color);
		break;
	    case TYPE_QUESTION:
		color = 0xFF000000 + Integer.parseInt(item_type.get("quiz")[1], 16);
		holder.type.setText(question.get("Type").toString());
		holder.familiar.setText("熟悉度 " + question.get("Familiar"));
		holder.date.setText("练习日 " + question.get("PlayDate").toString());

		holder.title.setTextSize(12);
		holder.title.setTextColor(color);

		holder.text.setVisibility(/* GONE = */8); // quiz不显示答案
		break;
	    default:
		me.toast("type error");
	    }

	    String syncState = question.get("SyncState").toString();
	    if (syncState.compareTo("modify") == 0) {
		holder.sync_state.setVisibility(/* VISIBLE = */0);
		holder.sync_state.setBackgroundColor(0xffFF1493);
	    }

	    if (syncState.compareTo("add") == 0) {
		holder.sync_state.setVisibility(/* VISIBLE = */0);
		holder.sync_state.setBackgroundColor(0xff32CD32);
	    }

	    String title = question.get("ItemTitle").toString();
	    String text = question.get("ItemText").toString();

	    holder.rec_id.setText((CharSequence) (question.get("ItemId") + ""));
	    holder.title.setText((CharSequence) title);
	    holder.text.setText((CharSequence) text);

	    if (title.length() == 0)
		holder.title.setVisibility(/* GONE = */8);

	    if (text.length() == 0)
		holder.text.setVisibility(/* GONE = */8);

	    holder.rec_id.setVisibility(/* GONE = */8);

	    return convertView;
	}

	public int getMemoColor(int mtime) {
	    long now = System.currentTimeMillis() / 1000;
	    long before = now - mtime;

	    int color = 0;
	    if (before < 1 * 3600 * 24) {
		color = 0xFF000000;
	    } else if (before < 2 * 3600 * 24) {
		color = 0xEE000000;
	    } else if (before < 3 * 3600 * 24) {
		color = 0xCC000000;
	    } else if (before < 4 * 3600 * 24) {
		color = 0xAA000000;
	    } else if (before < 5 * 3600 * 24) {
		color = 0x88000000;
	    } else if (before < 6 * 3600 * 24) {
		color = 0x66000000;
	    } else if (before < 7 * 3600 * 24) {
		color = 0x44000000;
	    } else if (before < 8 * 3600 * 24) {
		color = 0x22000000;
	    } else if (before < 9 * 3600 * 24) {
		color = 0x00000000;
	    }

	    return color;
	}
    }

    public static class ViewHolder {
	public LinearLayout top_bar;
	public TextView sync_state;
	public TextView type;
	public TextView familiar;
	public TextView date;
	public TextView rec_id;
	public TextView title;
	public TextView text;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	// 隐去状态栏部分(电池等图标和一切修饰部分)
	// this.getWindow().setFlags(
	// WindowManager.LayoutParams.FLAG_FULLSCREEN,
	// WindowManager.LayoutParams.FLAG_FULLSCREEN
	// );

	gesture_detector = new GestureDetector(this, this); // 手势支持

	SoundInit(); // 加载声音文件备用。

	setContentView(R.layout.main); // 加载layout
	mViewFlipper = (ViewFlipper) findViewById(R.id.flipper); // 初始化屏幕切换

	getSetting(); // 初始化帐户信息

	if (getUid().compareTo("") == 0) {
	    changeViewToAccount();
	} else {
	    open_db_file();
	    site_sync();
	    loadAllQuestion(); // 加载题库
	    changeViewToList(); // 默认进入列表页
	}
    }

    public void initDB() {
	// 打开或创建数据库，如果已经有db文件，则跳过。
	File f = this.getDatabasePath(db_file_name);
	String fileName = f.getAbsolutePath();
	File dbFile = new File(fileName);

	if (!dbFile.exists()) {
	    open_db_file();
	    create_db_table();
	    http.init_new_user_db(getUid());
	    toast("waiting while prepare your account data...");
	} else {
	    open_db_file();
	}
    }

    public void open_db_file() {
	db = openOrCreateDatabase(db_file_name, Context.MODE_PRIVATE, null);
    }

    public void create_db_table() {
	db.execSQL("CREATE TABLE questions ( _id INTEGER PRIMARY KEY AUTOINCREMENT, question VARCHAR, answer VARCHAR, explain VARCHAR DEFAULT '', priority INT DEFAULT 0, type CHAR NOT NULL DEFAULT '', is_memo INT NOT NULL DEFAULT 0, next_play_date DATE, familiar INT DEFAULT 0, correct_count INT DEFAULT 0, create_date DATE, sync_state CHAR DEFAULT 'add', mtime INT DEFAULT 0)");
	db.execSQL("CREATE TABLE item_type ( id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR, priority INT DEFAULT 0, color CHAR DEFAULT 'ffffff', fade_out INT DEFAULT 0, sync_state char DEFAULT 'add' ) ");
    }

    public SharedPreferences getSetting() {
	String filename = "memorizeSetting";
	SharedPreferences setting = getSharedPreferences(filename, Context.MODE_PRIVATE);
	return setting;
    }

    public String getUid() {
	SharedPreferences setting = getSetting();
	return setting.getString("uid", "");
    }

    // 绑定触屏事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
	return this.gesture_detector.onTouchEvent(event);
    }

    public void toast(String msg) {
	Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // 这个方法不要暂时不要移到add.java文件里，否则setItem()会报警告。
    public void update_question(String rec_id, String question, String answer) {
	ContentValues values = new ContentValues();

	int question_type[] = common.get_item_type(add.type);

	// 取sync_state状态
	String sq = "SELECT * FROM questions " + "WHERE _id = ?";
	Cursor cs = db.rawQuery(sq, new String[] { rec_id });
	while (cs.moveToNext()) {
	    if (cs.getString(cs.getColumnIndex("sync_state")).compareTo("add") == 0) {
		// none
	    } else {
		values.put("sync_state", "modify");
	    }
	}

	cs.close();

	values.put("question", question);
	values.put("answer", answer);
	values.put("priority", question_type[0]);
	values.put("type", add.type);
	values.put("is_memo", question_type[1]);

	values.put("mtime", (System.currentTimeMillis() / 1000) + "");

	db.update("questions", values, "_id=?", new String[] { rec_id });

	playSound("save");

	changeView(3); // change to list view

	// 更新条目显示
	String sql = "SELECT * FROM questions " + "WHERE _id = ?";
	Cursor c = db.rawQuery(sql, new String[] { rec_id });

	while (c.moveToNext()) {
	    listAdapter.setItem(add.list_position, getListMap(c));
	}

	c.close();
    }

    // 以下6个方法，定义手势动作。
    @Override
    public boolean onDown(MotionEvent e) {
	return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	if (e1.getX() - e2.getX() > 120) {
	    play.flushQuestion(0); // 从右向左
	    return true;
	} else if (e1.getX() - e2.getX() < -120) {
	    play.flushQuestion(1); // 从左向右
	    return true;
	}

	if (e1.getY() - e2.getY() > 120) {
	    play.showExplain();
	    return true;
	} else if (e1.getY() - e2.getY() < -120) {
	    play.showExplain();
	    return true;
	}

	return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
	// none
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
	return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
	// none
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
	return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.add:
	    changeViewToAdd();
	    break;
	case R.id.play:
	    changeViewToPlay();
	    break;
	case R.id.view:
	    changeViewToList();
	    break;
	case R.id.account:
	    changeViewToAccount();
	    break;
	case R.id.help:
	    changeViewToHelp();
	    break;
	}

	return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.main, menu);
	return super.onCreateOptionsMenu(menu);
    }

    public void closeInput() {
	if (!inputOpen) {
	    return;
	}

	inputOpen = false;

	EditText yourEditText = (EditText) findViewById(R.id.user_answer);

	InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
	inputMethodManager.toggleSoftInputFromWindow(yourEditText.getApplicationWindowToken(),
		InputMethodManager.SHOW_IMPLICIT, 0);
    }

    public void openInput() {
	if (inputOpen) {
	    return;
	}

	inputOpen = true;

	EditText yourEditText = (EditText) findViewById(R.id.user_answer);

	InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
	inputMethodManager.toggleSoftInputFromWindow(yourEditText.getApplicationWindowToken(),
		InputMethodManager.SHOW_IMPLICIT, 0);
    }

    public void loadAllQuestion() {
	String sql = "SELECT * FROM questions WHERE is_memo = ? AND correct_count < 3 AND next_play_date <= '"
		+ getToday(0) + "'";
	Cursor c = db.rawQuery(sql, new String[] { "0" });
	push_listItem(c);
    }

    public void loadAllRecord() {
	long mtime = (System.currentTimeMillis() / 1000) - (7 * 24 * 3600);

	String sql = "SELECT * FROM questions " + "WHERE (is_memo = ? AND next_play_date <= '"
		+ getToday(0) + "') " + " OR (is_memo = 1 AND ( priority > 10 OR mtime > " + mtime
		+ ")) ORDER BY priority DESC, mtime DESC";
	Cursor c = db.rawQuery(sql, new String[] { "0" });
	push_listItem(c);
    }

    public String getToday(long offset_ms) {
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	Date date = new Date(System.currentTimeMillis() + offset_ms);
	String today = dateFormat.format(date);

	return today;
    }

    public void push_listItem(Cursor c) {
	listItem = new ArrayList<HashMap<String, Object>>();

	while (c.moveToNext()) {
	    listItem.add(getListMap(c));
	}

	c.close();
    }

    public HashMap<String, Object> getListMap(Cursor c) {
	int id = c.getInt(c.getColumnIndex("_id"));
	String question = c.getString(c.getColumnIndex("question"));
	String answer = c.getString(c.getColumnIndex("answer"));
	String explain = c.getString(c.getColumnIndex("explain"));
	String type = c.getString(c.getColumnIndex("type"));
	String sync_state = c.getString(c.getColumnIndex("sync_state"));
	String next_play_date = c.getString(c.getColumnIndex("next_play_date"));
	int familiar = c.getInt(c.getColumnIndex("familiar"));
	int mtime = c.getInt(c.getColumnIndex("mtime"));
	int correct_count = c.getInt(c.getColumnIndex("correct_count"));

	HashMap<String, Object> map = new HashMap<String, Object>();
	map.put("ItemId", id);
	map.put("ItemTitle", question);
	map.put("ItemText", answer);
	map.put("ItemExplain", explain);
	map.put("Type", type);
	map.put("SyncState", sync_state);
	map.put("PlayDate", next_play_date);
	map.put("Familiar", familiar);
	map.put("mtime", mtime);
	map.put("CorrectCount", correct_count);

	return map;
    }

    public void changeView(int i) {
	if (i == 0) {
	    current_view = "add";
	    add.init();
	    setFullScreen(true);
	}
	if (i == 1) {
	    current_view = "play";
	    setFullScreen(true);
	}
	if (i == 2) {
	    current_view = "help";
	    setFullScreen(false);
	}
	if (i == 3) {
	    closeInput();
	    current_view = "list";
	    setFullScreen(false);
	}
	if (i == 5) {
	    current_view = "account";
	    setting.init();
	    setFullScreen(true);
	}

	mViewFlipper.setDisplayedChild(i);
    }

    public void changeViewToAdd() {
	changeView(0);
    }

    public void changeViewToPlay() {
	changeView(1);
	loadAllQuestion();
	play.init();
	play.flushQuestion(0); // 从右向左
    }

    public void changeViewToList() {
	changeView(3);
	freshList();
    }

    public void freshList() {
	loadAllRecord();

	listAdapter = new MyCustomAdapter(this);

	for (int i = 0; i < listItem.size(); i++) {
	    listAdapter.addItem(listItem.get(i));
	}

	listview = (ListView) findViewById(R.id.question_list);

	listview.setAdapter(listAdapter);

	// 添加点击
	listview.setOnItemClickListener(new OnItemClickListener() {
	    @Override
	    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		add.list_position = position;

		TextView Id = (TextView) arg1.findViewById(R.id.ItemId);
		String rec_id = Id.getText().toString();
		add.edit_question(rec_id);
	    }
	});
    }

    public void changeViewToAccount() {
	changeView(5);
    }

    public void changeViewToHelp() {
	changeView(2);
	WebView mWebView = (WebView) findViewById(R.id.help_webView);
	mWebView.loadUrl("file:///android_asset/help.html");
    }

    public void SoundInit() {
	answer_correct_snd = MediaPlayer.create(this, R.raw.chat_room_request);
	answer_incorrect_snd = MediaPlayer.create(this, R.raw.buddy_invite);
	save_snd = MediaPlayer.create(this, R.raw.openstore);
    }

    public void playSound(String sound_name) {
	if (sound_name == "answer_correct")
	    answer_correct_snd.start();
	if (sound_name == "answer_incorrect")
	    answer_incorrect_snd.start();
	if (sound_name == "save")
	    save_snd.start();
    }

    public void site_sync() {

	String sql = "SELECT * FROM questions WHERE sync_state <> ''";
	Cursor c = db.rawQuery(sql, new String[] {});

	JSONArray rows = new JSONArray();

	while (c.moveToNext()) {
	    JSONObject row = new JSONObject();

	    int id = c.getInt(c.getColumnIndex("_id"));
	    String question = c.getString(c.getColumnIndex("question"));
	    String answer = c.getString(c.getColumnIndex("answer"));
	    String explain = c.getString(c.getColumnIndex("explain"));
	    int priority = c.getInt(c.getColumnIndex("priority"));
	    String type = c.getString(c.getColumnIndex("type"));
	    int is_memo = c.getInt(c.getColumnIndex("is_memo"));
	    String next_play_date = c.getString(c.getColumnIndex("next_play_date"));
	    int familiar = c.getInt(c.getColumnIndex("familiar"));
	    int correct_count = c.getInt(c.getColumnIndex("correct_count"));
	    String create_date = c.getString(c.getColumnIndex("create_date"));
	    String sync_state = c.getString(c.getColumnIndex("sync_state"));
	    int mtime = c.getInt(c.getColumnIndex("mtime"));

	    try {
		row.put("id", id + "");
		row.put("question", question);
		row.put("answer", answer);
		row.put("explain", explain);
		row.put("priority", priority + "");
		row.put("type", type);
		row.put("is_memo", is_memo + "");
		row.put("next_play_date", next_play_date);
		row.put("familiar", familiar + "");
		row.put("correct_count", correct_count + "");
		row.put("create_date", create_date);
		row.put("sync_state", sync_state);
		row.put("mtime", mtime + "");
		rows.put(row);
	    } catch (JSONException e) {
		e.printStackTrace();
	    }
	}

	c.close();

	http.site_sync(rows.toString());
    }

    public void upload_to_site(String result) {
	int total = 0;
	String success = "";

	try {
	    JSONObject json = new JSONObject(result);
	    total = Integer.parseInt(json.getString("total"));
	    success = json.getString("success");
	} catch (JSONException e) {
	    e.printStackTrace();
	}

	if (success.compareTo("ok") == 0) {
	    ContentValues values = new ContentValues();
	    values.put("sync_state", "");
	    db.update("questions", values, "_id>?", new String[] { "0" });
	    freshList();
	}

	if (total == 0) {
	    sync_msg = "";
	} else {
	    sync_msg = "上传：" + total;
	}
    }

    public void update_from_site(String result) {
	int total = 0;

	try {
	    JSONArray json = new JSONArray(result);
	    total = json.length();

	    for (int i = 0; i < total; i++) {// 遍历JSONArray
		JSONObject oj = json.getJSONObject(i);
		String id = oj.getString("_id");

		ContentValues values = new ContentValues();
		values.put("question", oj.getString("question"));
		values.put("answer", oj.getString("answer"));
		values.put("explain", oj.getString("explain"));
		values.put("priority", oj.getString("priority"));
		values.put("type", oj.getString("type"));
		values.put("is_memo", oj.getString("is_memo"));
		values.put("next_play_date", oj.getString("next_play_date"));
		values.put("familiar", oj.getString("familiar"));
		values.put("correct_count", oj.getString("correct_count"));
		values.put("create_date", oj.getString("create_date"));
		values.put("mtime", oj.getString("mtime"));

		if (oj.getString("sync_state").compareTo("add") == 0) {
		    // A
		    values.put("sync_state", "");
		    values.put("_id", id);
		    db.insert("questions", null, values);
		} else {
		    int num = db.update("questions", values, "_id=?", new String[] { id });
		    if (num == 0) {
			// B AB两处代码是一样的，要合并一下。
			values.put("sync_state", "");
			values.put("_id", id);
			db.insert("questions", null, values);
		    }
		}
	    }

	} catch (JSONException e) {
	    e.printStackTrace();
	}

	if (total == 0) {
	    sync_msg += "";
	} else {
	    sync_msg += (sync_msg.compareTo("") == 0 ? "" : "\n") + "下载：" + total;
	    freshList();
	}

	if (sync_msg.compareTo("") > 0) {
	    toast(sync_msg);
	}
    }

    public boolean isFullScreen() {
	return (getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }

    public void setFullScreen(boolean full) {

	// if (full == isFullScreen()) {
	// return;
	// }

	if (Build.VERSION.SDK_INT >= 11) {
	    if (full) {
		getActionBar().hide();
	    } else {
		getActionBar().show();
	    }
	}
    }
}

// end file