package com.idormy.sms.forwarder;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idormy.sms.forwarder.adapter.AppAdapter;
import com.idormy.sms.forwarder.model.AppInfo;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class AppListActivity extends AppCompatActivity {

    public static final int APP_LIST = 0x9731991;
    private final String TAG = "AppListActivity";
    private List<AppInfo> appInfoList = new ArrayList<>();
    private ListView listView;
    private String currentType = "user";

    //消息处理者,创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == NOTIFY) {
                Toast.makeText(AppListActivity.this, msg.getData().getString("DATA"), Toast.LENGTH_LONG).show();
            } else if (msg.what == APP_LIST) {
                AppAdapter adapter = new AppAdapter(AppListActivity.this, R.layout.item_app, appInfoList);
                listView.setAdapter(adapter);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applist);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        //是否关闭页面提示
        TextView help_tip = findViewById(R.id.help_tip);
        help_tip.setVisibility(MyApplication.showHelpTip ? View.VISIBLE : View.GONE);

        //获取应用列表
        getAppList();

        //切换日志类别
        int typeCheckId = "user".equals(currentType) ? R.id.btnTypeUser : R.id.btnTypeSys;
        final RadioGroup radioGroupTypeCheck = findViewById(R.id.radioGroupTypeCheck);
        radioGroupTypeCheck.check(typeCheckId);
        radioGroupTypeCheck.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            currentType = (String) rb.getTag();
            getAppList();
        });

        listView = findViewById(R.id.list_view_app);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppInfo appInfo = appInfoList.get(position);
            Log.d(TAG, "onItemClick: " + appInfo.toString());
            //复制到剪贴板
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("pkgName", appInfo.getPkgName());
            cm.setPrimaryClip(mClipData);

            Toast.makeText(AppListActivity.this, "已复制包名：" + appInfo.getPkgName(), Toast.LENGTH_LONG).show();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            AppInfo appInfo = appInfoList.get(position);
            Log.d(TAG, "onItemClick: " + appInfo.toString());
            //启动应用
            Intent intent;
            intent = getPackageManager().getLaunchIntentForPackage(appInfo.getPkgName());
            startActivity(intent);

            return true;
        });
    }

    //获取应用列表
    private void getAppList() {
        new Thread(() -> {
            Message msg = new Message();
            msg.what = NOTIFY;
            Bundle bundle = new Bundle();
            bundle.putString("DATA", "user".equals(currentType) ? "正在加载用户应用，请稍候..." : "正在加载系统应用，请稍候...");
            msg.setData(bundle);
            handler.sendMessage(msg);

            appInfoList = new ArrayList<>();
            PackageManager pm = getApplication().getPackageManager();
            try {
                List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
                for (PackageInfo packageInfo : packages) {
                    //只取用户应用
                    if ("user".equals(currentType) && isSystemApp(packageInfo)) continue;
                    //只取系统应用
                    if ("sys".equals(currentType) && !isSystemApp(packageInfo)) continue;

                    String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
                    String packageName = packageInfo.packageName;
                    Drawable drawable = packageInfo.applicationInfo.loadIcon(pm);
                    String verName = packageInfo.versionName;
                    int verCode = packageInfo.versionCode;
                    AppInfo appInfo = new AppInfo(appName, packageName, drawable, verName, verCode);
                    appInfoList.add(appInfo);
                    Log.d(TAG, appInfo.toString());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            Message message = new Message();
            message.what = APP_LIST;
            message.obj = appInfoList;
            handler.sendMessage(message);
        }).start();
    }

    // 通过packName得到PackageInfo，作为参数传入即可
    private boolean isSystemApp(PackageInfo pi) {
        boolean isSysApp = (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1;
        boolean isSysUpd = (pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1;
        return isSysApp || isSysUpd;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}
