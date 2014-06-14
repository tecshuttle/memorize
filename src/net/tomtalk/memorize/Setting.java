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

    private Button register_btn;
    private Button login_btn;
    private Button logout_btn;
    private Button return_btn;

    private SharedPreferences setting;

    private LinearLayout login_layout;
    private LinearLayout register_layout;
    private Boolean isLoginLayout = false;

    public Setting(MemorizeActivity activity) {
	me = activity;
    }

    public void init() {
	setting = me.getSetting();

	me.openInput();

	// 初始化页面控件变量
	tv_user_name = (TextView) me.findViewById(R.id.account_name);
	tv_uid = (TextView) me.findViewById(R.id.account_id);
	login_name = (EditText) me.findViewById(R.id.login_name);
	login_pwd = (EditText) me.findViewById(R.id.login_pwd);

	set_user_info();

	login_name.setText(setting.getString("name", ""));
	login_pwd.setText(setting.getString("pwd", ""));

	// 按钮
	register_btn = (Button) me.findViewById(R.id.register_btn);
	register_btn.setOnClickListener(onRegister);

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

	login_name.setVisibility(View.GONE);
	login_pwd.setVisibility(View.GONE);
	register_btn.setVisibility(View.GONE);
	login_btn.setVisibility(View.GONE);
    }

    public void form_reset() {
	login_name.setText("");
	login_pwd.setText("");
    }

    public void setUid(String uid) {
	if (uid.equals("0")) {
	    me.toast("用户名或密码错，请重新输入！");
	} else {
	    Editor editor = setting.edit();
	    editor.putString("uid", uid);
	    editor.putString("name", login_name.getText().toString());
	    editor.putString("pwd", login_pwd.getText().toString());
	    editor.commit();

	    set_user_info();
	    show_user_info();
	}
    }

    public void clearUserInfo() {
	Editor editor = setting.edit();

	editor.putString("uid", "");
	editor.putString("name", "");
	editor.putString("pwd", "");

	editor.commit();
    }

    public void set_user_info() {
	tv_user_name.setText("用户名：" + setting.getString("name", ""));
	tv_uid.setText("用户ID：" + setting.getString("uid", ""));
    }

    public void applyRotation(float start, float end) {
	// Find the center of image
	final float centerX = login_layout.getWidth() / 2.0f;
	final float centerY = login_layout.getHeight() / 2.0f;

	// Create a new 3D rotation with the supplied parameter
	// The animation listener is used to trigger the next animation
	final Flip3dAnimation rotation = new Flip3dAnimation(start, end, centerX, centerY);
	rotation.setDuration(200);
	rotation.setFillAfter(true);
	rotation.setInterpolator(new AccelerateInterpolator());

	rotation.setAnimationListener(new DisplayNextView(isLoginLayout, login_layout,
		register_layout));

	if (isLoginLayout) {
	    login_layout.startAnimation(rotation);
	} else {
	    register_layout.startAnimation(rotation);
	}
    }

    Button.OnClickListener onRegister = new Button.OnClickListener() {
	public void onClick(View v) {
	    login_layout = (LinearLayout) me.findViewById(R.id.login_form);
	    register_layout = (LinearLayout) me.findViewById(R.id.register_form);

	    isLoginLayout = true;
	    applyRotation(0, 90);
	}
    };

    Button.OnClickListener onLogin = new Button.OnClickListener() {
	public void onClick(View v) {
	    if (isLoginLayout) {
		isLoginLayout = false;
		login_layout = (LinearLayout) me.findViewById(R.id.login_form);
		register_layout = (LinearLayout) me.findViewById(R.id.register_form);
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