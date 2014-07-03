package net.tomtalk.memorize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class Add {

    public List<String> list = new ArrayList<String>();
    
    public Spinner mySpinner;
    public ArrayAdapter<String> adapter;
    public int list_position = 0;
    public String type = "";
    public int type_id = 0;
    public SQLiteDatabase db;

    private MemorizeActivity me;

    private Button add_btn;
    private Button cancel_btn;
    private TextView rec_id;
    private EditText question;
    private EditText answer;
    
    private HashMap<String, String> item_type = new HashMap<String, String>();

    public Add(MemorizeActivity activity) {
	me = activity;
    }

    public void init() {
	db = me.db; // ��Ҫ�ŵ����췽�����ʱdb��û��ʼ����

	// ��ʼ��ҳ��ؼ�����
	rec_id = (TextView) me.findViewById(R.id.input_rec_id);
	question = (EditText) me.findViewById(R.id.text_question);
	answer = (EditText) me.findViewById(R.id.text_answer);

	// ��ť
	add_btn = (Button) me.findViewById(R.id.add_btn);
	add_btn.setOnClickListener(onAdd);
	cancel_btn = (Button) me.findViewById(R.id.cancel_btn);
	cancel_btn.setOnClickListener(onCancel);

	addViewReset();
	me.http.syncType(me.getUid());
	spinnerInit();
    }

    public void sync_type(String result) {
	db = me.db; // ע��ʱ�����ݿ�û�г�ʼ�������ԣ��������ٸ�ֵһ�Ρ�

	if (result.compareTo("NA") != 0) {
	    try {
		JSONArray json = new JSONArray(result);
		int total = json.length();

		for (int i = 0; i < total; i++) {// ����JSONArray
		    JSONObject type = json.getJSONObject(i);

		    String id = type.getString("id");

		    ContentValues values = new ContentValues();
		    values.put("name", type.getString("name"));
		    values.put("priority", type.getString("priority"));
		    values.put("color", type.getString("color"));
		    values.put("fade_out", type.getString("fade_out"));

		    if (type.getString("sync_state").compareTo("add") == 0) {
			// A
			values.put("sync_state", "");
			values.put("id", id);
			db.insert("item_type", null, values);
		    } else {
			int num = db.update("item_type", values, "id=?", new String[] { id });
			if (num == 0) {
			    // B AB����������һ���ģ�Ҫ�ϲ�һ�¡�
			    values.put("sync_state", "");
			    values.put("id", id);
			    db.insert("item_type", null, values);
			}
		    }
		}
	    } catch (JSONException e) {
		e.printStackTrace();
	    }
	}

	// ���������Ϊ��ע��ʱ�ܹ���ʼ�����ݡ���ò�Ҫ�����
	me.site_sync();
    }

    public void spinnerInit() {
	// reading item type
	list.clear();
	String sq = "SELECT * FROM item_type ";
	Cursor cs = db.rawQuery(sq, new String[] {});

	item_type.clear();
	
	while (cs.moveToNext()) {
	    list.add(cs.getString(cs.getColumnIndex("name")));
	    item_type.put(cs.getString(cs.getColumnIndex("name")), cs.getString(cs.getColumnIndex("id")));
	}
	cs.close();

	mySpinner = (Spinner) me.findViewById(R.id.spinner1);
	adapter = new ArrayAdapter<String>(me, R.layout.spinner_item, list);

	mySpinner.setAdapter(adapter);

	mySpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
	    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		TextView view = (TextView) arg1;
		view.setTextColor(0xff333333); // ������ɫ
		type = view.getText().toString();
		type_id = Integer.parseInt(item_type.get(type)); 
	    }

	    public void onNothingSelected(AdapterView<?> arg0) {
		// none
	    }
	});
    }

    public void delete_by_id(long id) {
	db.delete("questions", "_id=?", new String[] { id + "" });
    }

    public void content_empty() {
	me.toast("��Ŀ�ʹ𰸣�����ȫΪ��ѽ�� ");
    }

    public void addViewReset() {
	rec_id.setText("0"); // �½�Ϊ0���༭ʱrec_id > 0

	// ��Ŀ���𰸿ؼ����
	question.setText("");
	answer.setText("");
    }

    public void edit_question(String rec_id) {
	me.changeViewToAdd(); // ���л����༭ҳ
	me.openInput();

	TextView input_rec_id = (TextView) me.findViewById(R.id.input_rec_id);
	input_rec_id.setText(rec_id);

	EditText text_question = (EditText) me.findViewById(R.id.text_question);
	EditText text_answer = (EditText) me.findViewById(R.id.text_answer);

	String sql = "SELECT t.name as type, q.* FROM questions as q left join item_type as t on (q.type_id = t.id) WHERE q._id = ?";
	
	Cursor c = db.rawQuery(sql, new String[] { rec_id });

	while (c.moveToNext()) {
	    String type = c.getString(c.getColumnIndex("type"));
	    String question = c.getString(c.getColumnIndex("question"));
	    String answer = c.getString(c.getColumnIndex("answer"));

	    for (int i = 0; i < list.size(); i++) {
		if (type.equals(list.get(i))) {
		    mySpinner.setSelection(i);
		}
	    }

	    text_question.setText(question);
	    text_answer.setText(answer);
	}
    }

    Button.OnClickListener onChangeAccount = new Button.OnClickListener() {
	public void onClick(View v) {
	    me.toast("building...");
	}
    };

    Button.OnClickListener onReturnList = new Button.OnClickListener() {
	public void onClick(View v) {
	    me.changeViewToList(); // change to list view
	}
    };

    Button.OnClickListener onAdd = new Button.OnClickListener() {
	public void onClick(View v) {
	    EditText et_question = (EditText) me.findViewById(R.id.text_question);
	    EditText et_answer = (EditText) me.findViewById(R.id.text_answer);

	    String question = et_question.getText().toString().trim();
	    String answer = et_answer.getText().toString().trim();

	    TextView input_rec_id = (TextView) me.findViewById(R.id.input_rec_id);
	    String rec_id = input_rec_id.getText().toString();

	    if (rec_id != "0") {
		if (question.equals("") && answer.equals("")) {
		    content_empty();
		} else {
		    me.update_question(rec_id, question, answer);
		    // ���º���������
		    // Ŀǰ��ˢ������list����Ļλ�û������б�ͷ������û�кð취ǰ���Ƚ��á�
		    me.site_sync();
		}
		return;
	    }

	    if (question.equals("") && answer.equals("")) {
		content_empty();
		return;
	    }

	    add(type, question, answer);

	    me.playSound("save");
	    me.toast("��Ŀ�ѱ���");

	    addViewReset();

	    me.changeViewToList();

	    me.site_sync(); // ���º���������
	}
    };

    public void add(String item_type, String new_question, String new_answer) {
	ContentValues values = new ContentValues();
	//int question_type[] = me.common.get_item_type(item_type);

	values.put("question", new_question);
	values.put("answer", new_answer);
	values.put("next_play_date", me.getToday(0));
	values.put("create_date", me.getToday(0));
	values.put("type_id", type_id);
	values.put("mtime", (System.currentTimeMillis() / 1000) + "");

	// Ϊ�˲�����վ��������¼��ͻ���ֻ��˽���������id������
	long new_id = 0;
	new_id = me.db.insert("questions", null, values);

	if (new_id % 2 == 0) {
	    delete_by_id(new_id);
	    me.db.insert("questions", null, values);
	}
    }

    Button.OnClickListener onCancel = new Button.OnClickListener() {
	public void onClick(View v) {
	    me.changeView(3); // change to list view
	}
    };

}

// end file
