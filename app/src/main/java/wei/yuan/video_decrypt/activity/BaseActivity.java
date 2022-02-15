package wei.yuan.video_decrypt.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import wei.yuan.video_decrypt.R;
import wei.yuan.video_decrypt.util.CommonUtil;
import wei.yuan.video_decrypt.view.SwordLoadingView;

public class BaseActivity extends Activity {

    private AlertDialog alertDialog;
    private AlertDialog swordLoadingDialog;

    public void showLoadingDialog() {
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable());
        alertDialog.setCancelable(false);
        alertDialog.show();
        alertDialog.setContentView(R.layout.loading_dialog);
        alertDialog.setCanceledOnTouchOutside(false);
    }

    public void dismissLoadingDialog() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
            alertDialog = null;
        }
    }

    public void showSwordLoadingDialog() {
        swordLoadingDialog = new AlertDialog.Builder(this).create();
        swordLoadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable());
        swordLoadingDialog.setCancelable(true);
        swordLoadingDialog.show();
        Window window = swordLoadingDialog.getWindow();
        swordLoadingDialog.getWindow().setLayout(CommonUtil.dip2px(this, 100),
                CommonUtil.dip2px(this, 100));
        swordLoadingDialog.setContentView(swordLoadingView());
    }

    public void dismissSwordLoadingDialog() {
        if (swordLoadingDialog != null && swordLoadingDialog.isShowing()) {
            swordLoadingDialog.dismiss();
            swordLoadingDialog = null;
        }
    }

    public void showToastMsg(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private View swordLoadingView() {
        SwordLoadingView view = new SwordLoadingView(this);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
                CommonUtil.px2dip(this, 200),
                CommonUtil.px2dip(this, 200));
        fl.gravity = Gravity.CENTER;
        view.setLayoutParams(fl);
        view.getAnimator().start();
        return view;
    }
}
