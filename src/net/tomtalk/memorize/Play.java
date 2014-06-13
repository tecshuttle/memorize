package net.tomtalk.memorize;

import android.content.ContentValues;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Play {
    private MemorizeActivity me;

    private TextView tv_item_type;
    private TextView tv_question_type;
    private TextView tv_view_count;

    private TextView tv_today_rest_count;
    private TextView tv_question;
    private EditText et_user_answer;
    private TextView tv_correct_answer;
    private TextView tv_explain;

    private Button next_question_btn;
    private Button return_btn;

    private Button choice_btn1;
    private Button choice_btn2;
    private Button choice_btn3;
    private Button choice_btn4;
    private Button choice_btn5;

    private Boolean explainToggle = false;
    private int familiar = 0;
    private int play_cur = 0;
    private int correct_count = 0;
    private String play_record_id = "0";

    public Play(MemorizeActivity activity) {
	me = activity;
    }

    public void init() {
	// 初始化页面控件变量
	tv_item_type = (TextView) me.findViewById(R.id.item_type);
	tv_question_type = (TextView) me.findViewById(R.id.question_type);
	tv_view_count = (TextView) me.findViewById(R.id.view_count);

	tv_today_rest_count = (TextView) me.findViewById(R.id.today_rest_count);
	tv_question = (TextView) me.findViewById(R.id.tv_question);
	et_user_answer = (EditText) me.findViewById(R.id.user_answer);
	tv_correct_answer = (TextView) me.findViewById(R.id.correct_answer);
	tv_explain = (TextView) me.findViewById(R.id.explain);

	return_btn = (Button) me.findViewById(R.id.return_list);
	return_btn.setOnClickListener(onReturn);
	next_question_btn = (Button) me.findViewById(R.id.next_question);
	next_question_btn.setOnClickListener(onNextQuestion);

	choice_btn1 = (Button) me.findViewById(R.id.choice_btn_1);
	choice_btn2 = (Button) me.findViewById(R.id.choice_btn_2);
	choice_btn3 = (Button) me.findViewById(R.id.choice_btn_3);
	choice_btn4 = (Button) me.findViewById(R.id.choice_btn_4);
	choice_btn5 = (Button) me.findViewById(R.id.choice_btn_5);

	// 绑定事件
	choice_btn1.setOnClickListener(choice_btn1_click);
	choice_btn2.setOnClickListener(choice_btn2_click);
	choice_btn3.setOnClickListener(choice_btn3_click);
	choice_btn4.setOnClickListener(choice_btn4_click);
	choice_btn5.setOnClickListener(choice_btn5_click);

	// 监听答案文本框
	et_user_answer.addTextChangedListener(new TextWatcher() {
	    private MemorizeActivity _me = me;

	    @Override
	    public void onTextChanged(CharSequence s, int start, int before, int count) {
		if (_me.current_view == "play") {
		    String user_answer = et_user_answer.getText().toString();
		    String correct_answer = tv_correct_answer.getText().toString();

		    if (user_answer.compareTo(correct_answer) == 0) {
			answer_ok();
		    }
		}
	    }

	    @Override
	    public void afterTextChanged(Editable s) {
		// none
	    }

	    @Override
	    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// none
	    }
	});
    }

    public void play_view_reset() {
	et_user_answer.setVisibility(/* VISIBLE = */0);
	tv_today_rest_count.setText("还剩 " + me.listItem.size() + " 道题");

	explainToggle = false;
	showExplain();
    }

    public void answer_ok() {
	// me.toast("正确 ");
	me.playSound("answer_correct");

	ContentValues values = new ContentValues();
	int[] ebbing = { 1, 2, 4, 7, 15, 30, 50, 70, 100, 150, 200, 150, 200, 300, 400 };

	if (correct_count < 2) {
	    correct_count += 1;
	} else {
	    familiar += 1;
	    correct_count = 0;
	    values.put("next_play_date", me.getToday((long) ebbing[familiar] * 3600 * 24 * 1000));
	    values.put("familiar", familiar);
	}

	values.put("correct_count", correct_count);
	values.put("sync_state", "modify");

	me.db.update("questions", values, "_id=?", new String[] { play_record_id });
	me.loadAllQuestion();
	flushQuestion(0); // 从右向左
    }

    public void answer_error() {
	me.toast("错误");
	me.playSound("answer_incorrect");

	explainToggle = true;
	showExplain();

	familiar -= (familiar > 0 ? 1 : 0); // 熟悉度减1
    }

    public void showExplain() {
	String explain = tv_explain.getText().toString();

	explainToggle = explainToggle ? false : true;

	tv_explain.setText(explain.length() == 0 ? "本题无题解" : explain);

	tv_explain.setVisibility(explainToggle ? 8 : 0);
    }

    public void hideOption() {
	choice_btn1.setVisibility(/* GONE = */8);
	choice_btn2.setVisibility(/* GONE = */8);
	choice_btn3.setVisibility(/* GONE = */8);
	choice_btn4.setVisibility(/* GONE = */8);
	choice_btn5.setVisibility(/* GONE = */8);
    }

    public void next_animator(int direction) {
	DisplayMetrics metric = new DisplayMetrics();
	me.getWindowManager().getDefaultDisplay().getMetrics(metric);
	int width = metric.widthPixels; // 屏幕宽度（像素）

	Animation tAnim = new TranslateAnimation((direction == 0 ? width : -width), 0, 0, 0);
	tAnim.setDuration(300);

	LinearLayout ly = (LinearLayout) me.findViewById(R.id.ly_question);
	ly.startAnimation(tAnim);
    }

    public void flushQuestion(int direction) {
	if (me.listItem.isEmpty()) {
	    me.mViewFlipper.setDisplayedChild(4);
	    return;
	}

	play_view_reset();

	if (play_cur > me.listItem.size() - 1) {
	    play_cur = 0;
	}

	int rec_id = (Integer) me.listItem.get(play_cur).get("ItemId");
	play_record_id = rec_id + ""; // int类型转换String
	familiar = (Integer) me.listItem.get(play_cur).get("Familiar");
	correct_count = (Integer) me.listItem.get(play_cur).get("CorrectCount");
	tv_explain.setText((String) me.listItem.get(play_cur).get("ItemExplain"));

	tv_item_type.setText((String) me.listItem.get(play_cur).get("Type"));
	tv_view_count.setText("第 " + (correct_count + 1) + "/3 次");

	String question = (String) me.listItem.get(play_cur).get("ItemTitle");
	String answer = (String) me.listItem.get(play_cur).get("ItemText");

	play_cur++;

	if (answer.indexOf("|") > 0) {
	    flushChoiceQuestion(question, answer);
	    me.closeInput();
	} else {
	    flushBlankQuestion(question, answer);
	    me.openInput();
	    et_user_answer.requestFocus(); // 文本框设定输入焦点
	}

	next_animator(direction); // 动画切换放在最后调用，否则可能造成填空题切题没有动画效果。
    }

    public void flushBlankQuestion(String question, String answer) {
	hideOption();

	tv_question_type.setText("问答题");

	// 设置正确答案
	tv_question.setText(question);
	tv_correct_answer.setText(answer);

	// 清空用户上次题的输入
	et_user_answer.setVisibility(/* VISIBLE = */0);
	et_user_answer.setText("");

	// 显示答题文本输入框、答题按钮
	et_user_answer.setVisibility(/* VISIBLE = */0);
    }

    public void flushChoiceQuestion(String question, String answer) {
	// 复位答题界面
	flushBlankQuestion(question, answer);

	tv_question_type.setText("选择题");
	// 隐藏答题文本输入框、答题按钮
	et_user_answer.setVisibility(/* GONE = */8);
	// answer_btn.setVisibility(/*INVISIBILITY = */4);

	// 显示回答按钮
	String[] answerArray = answer.split("\\|");

	choice_btn1.setVisibility(/* VISIBLE = */0);
	choice_btn1.setText(answerArray[0]);

	choice_btn2.setVisibility(/* VISIBLE = */0);
	choice_btn2.setText(answerArray[1]);

	if (answerArray.length > 3) {
	    choice_btn3.setVisibility(/* VISIBLE = */0);
	    choice_btn3.setText(answerArray[2]);
	}

	if (answerArray.length > 4) {
	    choice_btn4.setVisibility(/* VISIBLE = */0);
	    choice_btn4.setText(answerArray[3]);
	}

	if (answerArray.length > 5) {
	    choice_btn5.setVisibility(/* VISIBLE = */0);
	    choice_btn5.setText(answerArray[4]);
	}

	tv_correct_answer.setText(answerArray[answerArray.length - 1]);
    }

    public void choose(int iOption) {
	String correct_answer = tv_correct_answer.getText().toString();

	if (correct_answer.compareTo(iOption + "") == 0) {
	    answer_ok();
	} else {
	    answer_error();
	}
    }

    Button.OnClickListener onReturn = new Button.OnClickListener() {
	public void onClick(View v) {
	    me.changeViewToList(); // change to list view
	}
    };

    Button.OnClickListener onNextQuestion = new Button.OnClickListener() {
	public void onClick(View v) {
	    flushQuestion(0);
	}
    };

    Button.OnClickListener choice_btn1_click = new Button.OnClickListener() {
	public void onClick(View v) {
	    choose(1);
	}
    };

    Button.OnClickListener choice_btn2_click = new Button.OnClickListener() {
	public void onClick(View v) {
	    choose(2);
	}
    };

    Button.OnClickListener choice_btn3_click = new Button.OnClickListener() {
	public void onClick(View v) {
	    choose(3);
	}
    };

    Button.OnClickListener choice_btn4_click = new Button.OnClickListener() {
	public void onClick(View v) {
	    choose(4);
	}
    };

    Button.OnClickListener choice_btn5_click = new Button.OnClickListener() {
	public void onClick(View v) {
	    choose(5);
	}
    };

}

// end file
