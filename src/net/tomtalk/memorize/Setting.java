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

	    rotation.setDuration(500);
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

    private TextView tv_uid;
    private EditText et_name;
    private EditText et_pwd;

    private Button register_btn;
    private Button login_btn;
    private Button logout_btn;
    private Button return_btn;

    private SharedPreferences setting;

    private LinearLayout login_layout;
    private LinearLayout register_layout;
    private Boolean isLoginLayout = true;

    public Setting(MemorizeActivity activity) {
	me = activity;
    }

    public void init() {
	setting = me.getSetting();

	me.openInput();

	// 初始化页面控件变量
	tv_uid = (TextView) me.findViewById(R.id.account_id);
	et_name = (EditText) me.findViewById(R.id.account_name);
	et_pwd = (EditText) me.findViewById(R.id.account_pwd);

	tv_uid.setText("uid: " + setting.getString("uid", ""));
	et_name.setText(setting.getString("name", ""));
	et_pwd.setText(setting.getString("pwd", ""));

	// 按钮

	register_btn = (Button) me.findViewById(R.id.register_btn);
	register_btn.setOnClickListener(onRegister);

	login_btn = (Button) me.findViewById(R.id.login_btn);
	login_btn.setOnClickListener(onLogin);

	logout_btn = (Button) me.findViewById(R.id.logout_btn);
	logout_btn.setOnClickListener(onLogout);

	return_btn = (Button) me.findViewById(R.id.setting_return_btn);
	return_btn.setOnClickListener(onReturnList);
    }

    public void setUid(String uid) {
	Editor editor = setting.edit();

	editor.putString("uid", uid);
	editor.putString("name", et_name.getText().toString());
	editor.putString("pwd", et_pwd.getText().toString());

	editor.commit();

	tv_uid.setText("用户ID：" + uid);
    }

    public void applyRotation(float start, float end) {
	// Find the center of image
	final float centerX = login_layout.getWidth() / 2.0f;
	final float centerY = login_layout.getHeight() / 2.0f;

	// Create a new 3D rotation with the supplied parameter
	// The animation listener is used to trigger the next animation
	final Flip3dAnimation rotation = new Flip3dAnimation(start, end, centerX, centerY);
	rotation.setDuration(500);
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

	    // login_layout.setVisibility(View.GONE);

	    isLoginLayout = true;
	    applyRotation(0, 90);
	}
    };

    Button.OnClickListener onLogin = new Button.OnClickListener() {
	public void onClick(View v) {
	    // me.http.getUid(et_name.getText().toString(),
	    // et_pwd.getText().toString());
	    login_layout = (LinearLayout) me.findViewById(R.id.login_form);
	    register_layout = (LinearLayout) me.findViewById(R.id.register_form);

	    // register_layout.setVisibility(View.GONE);

	    isLoginLayout = false;
	    applyRotation(0, -90);
	}
    };

    Button.OnClickListener onLogout = new Button.OnClickListener() {
	public void onClick(View v) {
	    me.toast("logout building ...");
	}
    };

    Button.OnClickListener onReturnList = new Button.OnClickListener() {
	public void onClick(View v) {
	    me.changeViewToList(); // change to list view
	}
    };

}

// end file