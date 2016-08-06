package com.project.zfy.zhihu.activity;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.loopj.android.http.TextHttpResponseHandler;
import com.project.zfy.zhihu.R;
import com.project.zfy.zhihu.db.WebCacheDbHelper;
import com.project.zfy.zhihu.global.Constant;
import com.project.zfy.zhihu.moudle.StoriesEntity;
import com.project.zfy.zhihu.utils.HttpUtils;
import com.project.zfy.zhihu.utils.LogUtils;
import com.project.zfy.zhihu.utils.UIUtils;
import com.project.zfy.zhihu.view.RevealBackgroundView;

import org.apache.http.Header;

/**
 * 新闻详情页面的基类
 * Created by zfy on 2016/8/6.
 */
public abstract class BaseContentActivity extends AppCompatActivity implements RevealBackgroundView.OnStateChangeListener {

    public WebCacheDbHelper mDbHelper;
    public int[] mStartingLocation;
    public StoriesEntity mEntity;
    public RevealBackgroundView mBackgroundView;
    public WebView mWebView;
    private Toolbar mToolbar;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(loadContentView());
        initView();
        initData();
        initAnimation(savedInstanceState);

    }

    /**
     * 初始化Activity跳转动画的方法
     *
     * @author zfy
     * @created at 2016/8/6 12:22
     */
    public void initAnimation(Bundle bundle) {
        setupRevealBackground(bundle);
        setStatusBarColor(UIUtils.getColor(R.color.light_toolbar));

    }


    public void setupRevealBackground(Bundle bundle) {
        mBackgroundView.setOnStateChangeListener(this);
        if (bundle == null) {

            //view树完成测量并且分配空间而绘制过程还没有开始的时候播放动画
            mBackgroundView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mBackgroundView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mBackgroundView.startFromLocation(mStartingLocation);
                    return true;
                }
            });


        } else {
            mBackgroundView.setToFinishedFrame();
        }

    }


    /**
     * 初始化数据的方法,由子类根据其业务逻辑,加载不同的数据
     *
     * @author zfy
     * @created at 2016/8/6 12:21
     */
    public void initData() {
        if (HttpUtils.isNetworkConnected(this)) {
            HttpUtils.get(Constant.CONTENT + mEntity.getId(), new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    //请求数据成功，缓存到数据库
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();
//                    responseString = responseString.replaceAll("'", "''");

                    LogUtils.d("responseString--->" + responseString);
                    db.execSQL("replace into Cache(newsId,json) values(" + mEntity.getId() + ",'" + responseString + "')");
                    db.close();
                    parseJsonData(responseString);
                }
            });

        } else {

            //没有网络，则从数据库中拿数据
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("select * from Cache where newsId = " + mEntity.getId(), null);
            if (cursor.moveToFirst()) {
                String json = cursor.getString(cursor.getColumnIndex("json"));
                parseJsonData(json);
            }
            cursor.close();
            db.close();

        }
    }


    /**
     * 初始化控件的方法,由子类根据其布局文件,加载各自的控件
     *
     * @author zfy
     * @created at 2016/8/6 12:20
     */
    public void initView() {



        mDbHelper = new WebCacheDbHelper(this, 1);
        mStartingLocation = getIntent().getIntArrayExtra(Constant.START_LOCATION);
        mEntity = (StoriesEntity) getIntent().getSerializableExtra("entity");
        mBackgroundView = (RevealBackgroundView) findViewById(R.id.rbv_view);

        mToolbar = (Toolbar) findViewById(R.id.tb_bar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        //对左上角的返回键做监听
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        mWebView = (WebView) findViewById(R.id.wv_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        // 开启DOM storage API 功能
        mWebView.getSettings().setDomStorageEnabled(true);
        // 开启database storage API功能
        mWebView.getSettings().setDatabaseEnabled(true);
        // 开启Application Cache功能
        mWebView.getSettings().setAppCacheEnabled(true);

    }




    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, R.anim.slide_out_to_left);
    }

    @Override
    public void onStateChange(int state) {

        stateChangeShowView(state);

    }



    /**
     * 解析服务器返回的JSON类型数据的方法,由具体的子类去实现
     *
     * @param responseString Json类型的数据
     * @return void
     * @author zfy
     * @created at 2016/8/6 12:42
     */
    public abstract void parseJsonData(String responseString);

    /**
     * 用于加载布局文件的方法,由子类去实现加载具体的布局文件
     *
     * @author zfy
     * @created at 2016/8/6 12:19
     */
    public abstract int loadContentView();

    /**
     * 状态改变后应该显示出来的View,具体由子类去实现
     *
     * @author zfy
     * @created at 2016/8/6 12:46
     */
    public abstract void stateChangeShowView(int state);


    @TargetApi(21)
    public void setStatusBarColor(int statusBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // If both system bars are black, we can remove these from our layout,
            // removing or shrinking the SurfaceFlinger overlay required for our views.
            Window window = this.getWindow();
            if (statusBarColor == Color.BLACK && window.getNavigationBarColor() == Color.BLACK) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
            window.setStatusBarColor(statusBarColor);
        }
    }

}
