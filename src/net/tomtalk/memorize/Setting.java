package net.tomtalk.memorize;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Setting {
    public class Flip3dAnimation extends Animation {
	private final float mFromDegrees;
	private final float mToDegrees;
	private final float mCenterX;
	private final float mCenterY;
	private Camera mCamera;

	public Flip3dAnimation(float fromDegrees, float toDegrees, float centerX, float centerY) {
	    mFromDegrees = fromDegrees;
	    mToDegrees = toDegrees;
	    mCenterX = centerX;
	    mCenterY = centerY;
	}

	@Override
	public void initialize(int width, int height, int parentWidth, int parentHeight) {
	    super.initialize(width, height, parentWidth, parentHeight);
	    mCamera = new Camera();
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {
	    final float fromDegrees = mFromDegrees;
	    float degrees = fromDegrees + ((mToDegrees - fromDegrees) * interpolatedTime);

	    final float centerX = mCenterX;
	    final float centerY = mCenterY;
	    final Camera camera = mCamera;

	    final Matrix matrix = t.getMatrix();

	    camera.save();

	    camera.rotateY(degrees);

	    camera.getMatrix(matrix);
	    camera.restore();

	    matrix.preTranslate(-centerX, -centerY);
	    matrix.postTranslate(centerX, centerY);
	}
    }

    public final class DisplayNextView implements Animation.AnimationListener {
	private boolean mCurrentView;
	LinearLayout image1;
	LinearLayout image2;

	public DisplayNextView(boolean currentView, LinearLayout image1, LinearLayout image2) {
	    mCurrentView = currentView;
	    this.image1 = image1;
	    this.image2 = image2;
	}

	public void onAnimationStart(Animation animation) {
	}

	public void onAnimationEnd(Animation animation) {
	    image1.post(new SwapViews(mCurrentView, image1, image2));
	}

	public void onAnimationRepeat(Animation animation) {
	}
    }

    public final class SwapViews implements Runnable {
	private boolean mIsFirstView;
	LinearLayout image1;
	LinearLayout image2;

	public SwapViews(boolean isFirstView, LinearLayout image1, LinearLayout image2) {
	    mIsFirstView = isFirstView;
	    this.image1 = image1;
	    this.image2 = image2;
	}

	public void run() {
	    final float centerX = image1.getWidth() / 2.0f;
	    final float centerY = image1.getHeight() / 2.0f;
	    Flip3dAnimation rotation;

	    if (mIsFirstView) {
		image1.setVisibility(View.GONE);
		image2.setVisibility(View.VISIBLE);
		image2.requestFocus();

		rotation = new Flip3dAnimation(-90, 0, centerX, centerY);
	    } else {
		image2.setVisibility(View.GONE);
		image1.setVisibility(View.VISIBLE);
		image1.requestFocus();

		rotation = new Flip3dAnimation(90, 0, centerX, centerY);
	    }

	    rotation.setDuration(200);
	    rotation.setFillAfter(true);
	    rotation.setInterpolator(new DecelerateInterpolator());

	    if (mIsFirstView) {
		image2.startAnimation(rotation);
	    } else {
		image1.startAnimation(rotation);
	    }
	}
    }

    private MemorizeActivity me;

    private TextView tv_user_name;
    private TextView tv_uid;
    private EditText login_name;
    private EditText login_pwd;
    private EditText reg_name;
    private EditText reg_pwd;

    private Button register_btn;
    private Button login_btn;
    private Button logout_btn;
    private Button return_btn;

    private SharedPreferences setting;

    private LinearLayout login_ly;
    private LinearLayout register_ly;
    private Boolean isLoginLy = false;

    public Setting(MemorizeActivity activity) {
	me = activity;
    }

    public void init() {
	setting = me.getSetting();

	me.openInput();

	// 初始化页面控件变量
	login_ly = (LinearLayout) me.findViewById(R.id.login_form);
	register_ly = (LinearLayout) me.findViewById(R.id.register_form);

	tv_user_name = (TextView) me.findViewById(R.id.account_name);
	tv_uid = (TextView) me.findViewById(R.id.account_id);
	login_name = (EditText) me.findViewById(R.id.login_name);
	login_pwd = (EditText) me.findViewById(R.id.login_pwd);
	reg_name = (EditText) me.findViewById(R.id.reg_name);
	reg_pwd = (EditText) me.findViewById(R.id.reg_pwd);

	set_user_info();

	login_name.setText(setting.getString("name", ""));
	login_pwd.setText(setting.getString("pwd", ""));

	// 按钮
	register_btn = (Button) me.findViewById(R.id.register_btn);
	register_btn.setOnClickListener(onRegister);
	register_btn.setTextColor(0x990088cc);

	login_btn = (Button) me.findViewById(R.id.login_btn);
	login_btn.setOnClickListener(onLogin);

	logout_btn = (Button) me.findViewById(R.id.logout_btn);
	logout_btn.setOnClickListener(onLogout);

	return_btn = (Button) me.findViewById(R.id.setting_return_btn);
	return_btn.setOnClickListener(onReturnList);

	if (setting.getString("uid", "").compareTo("") == 0) {
	    // register or login
	    logout_btn.setVisibility(View.GONE);
	    return_btn.setVisibility(View.GONE);

	    tv_user_name.setVisibility(View.GONE);
	    tv_uid.setVisibility(View.GONE);
	} else {
	    // already login user account
	    login_name.setVisibility(View.GONE);
	    login_pwd.setVisibility(View.GONE);

	    register_btn.setVisibility(View.GONE);
	    login_btn.setVisibility(View.GONE);
	}
    }

    public void showLogin() {
	tv_user_name.setVisibility(View.GONE);
	tv_uid.setVisibility(View.GONE);
	logout_btn.setVisibility(View.GONE);
	return_btn.setVisibility(View.GONE);

	reg_name.setVisibility(View.VISIBLE);
	reg_pwd.setVisibility(View.VISIBLE);
	login_name.setVisibility(View.VISIBLE);
	login_pwd.setVisibility(View.VISIBLE);
	register_btn.setVisibility(View.VISIBLE);
	login_btn.setVisibility(View.VISIBLE);
    }

    public void show_user_info() {
	tv_user_name.setVisibility(View.VISIBLE);
	tv_uid.setVisibility(View.VISIBLE);
	logout_btn.setVisibility(View.VISIBLE);
	return_btn.setVisibility(View.VISIBLE);

	reg_name.setVisibility(View.GONE);
	reg_pwd.setVisibility(View.GONE);
	login_name.setVisibility(View.GONE);
	login_pwd.setVisibility(View.GONE);
	register_btn.setVisibility(View.GONE);
	login_btn.setVisibility(View.GONE);
    }

    public void form_reset() {
	reg_name.setText("");
	reg_pwd.setText("");
	login_name.setText("");
	login_pwd.setText("");
    }

    public void setUid(String result) {
	String[] np = result.split("\\|");
	String name = np[0];
	String uid = np[1];
	String type = np[2];

	if (uid.equals("0")) {
	    if (type.equals("login")) {
		me.toast("用户名或密码错，请重新输入！");
	    } else if (type.equals("register")) {
		me.toast("用户名已被使用，换一个！");
	    }
	} else {
	    Editor editor = setting.edit();
	    editor.putString("uid", uid);
	    editor.putString("name", name);
	    editor.commit();

	    set_user_info();
	    show_user_info();
	}
    }

    public void clearUserInfo() {
	Editor editor = setting.edit();

	editor.putString("uid", "");
	editor.putString("name", "");

	editor.commit();
    }

    public void set_user_info() {
	tv_user_name.setText("用户名：" + setting.getString("name", ""));
	tv_uid.setText("用户ID：" + setting.getString("uid", ""));
    }

    public void applyRotation(float start, float end) {
	// Find the center of image
	final float centerX = login_ly.getWidth() / 2.0f;
	final float centerY = login_ly.getHeight() / 2.0f;

	// Create a new 3D rotation with the supplied parameter
	// The animation listener is used to trigger the next animation
	final Flip3dAnimation rotation = new Flip3dAnimation(start, end, centerX, centerY);
	rotation.setDuration(200);
	rotation.setFillAfter(true);
	rotation.setInterpolator(new AccelerateInterpolator());

	rotation.setAnimationListener(new DisplayNextView(isLoginLy, login_ly, register_ly));

	if (isLoginLy) {
	    login_ly.startAnimation(rotation);
	} else {
	    register_ly.startAnimation(rotation);
	}
    }

    Button.OnClickListener onRegister = new Button.OnClickListener() {
	public void onClick(View v) {
	    register_btn.setTextColor(0xff0088cc);
	    login_btn.setTextColor(0x990088cc);

	    if (isLoginLy) {

		String name = reg_name.getText().toString();
		String pwd = reg_pwd.getText().toString();
		if (name.equals("") || pwd.equals("")) {
		    me.toast("请填写注册名和密码！");
		} else {
		    me.http.getNewUid(name, pwd);
		}
	    } else {
		isLoginLy = true;
		applyRotation(0, 90);
	    }
	}
    };

    Button.OnClickListener onLogin = new Button.OnClickListener() {
	public void onClick(View v) {
	    register_btn.setTextColor(0x990088cc);
	    login_btn.setTextColor(0xff0088cc);

	    if (isLoginLy) {
		isLoginLy = false;
		applyRotation(0, -90);
	    } else {
		String name = login_name.getText().toString();
		String pwd = login_pwd.getText().toString();
		if (name.equals("") || pwd.equals("")) {
		    me.toast("请填写登入名和密码！");
		} else {
		    me.http.getUid(name, pwd);
		}
	    }
	}
    };

    Button.OnClickListener onLogout = new Button.OnClickListener() {
	public void onClick(View v) {
	    clearUserInfo();
	    showLogin();
	    form_reset();
	}
    };

    Button.OnClickListener onReturnList = new Button.OnClickListener() {
	public void onClick(View v) {
	    me.changeViewToList(); // change to list view
	}
    };

}

// end file