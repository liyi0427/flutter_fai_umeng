package com.studyyoun.flutter_fai_umeng;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.flutter.Log;
import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;

/**
 * 创建人： $(USER)
 * 创建时间：$(DATE)
 * 页面说明：
 * 功能性修改记录：
 */
public class MessageChannelItem extends FlutterApplication {
    private String LOGTAG = MessageChannelItem.class.getSimpleName();
    private BasicMessageChannel<Object> mMessageChannel;
    public static MessageChannelItem sMMessageChannelItem = new MessageChannelItem();

    public static MessageChannelItem getInstance() {
        return sMMessageChannelItem;
    }

    private List<String> mPageStartList = new ArrayList<>();
    //获取主线程的Looper对象
//    public Handler mHandler = new Handler(getMainLooper());

    private static String TAG = "| UMPUSH | Flutter | Android | ";
    private static BasicMessageChannel.Reply<Object> mReply;

    /**
     * 设置消息监听
     *
     * @param messenger
     * @param context
     */
    public void messageChannelFunction(final BinaryMessenger messenger, final Context context) {
        //消息接收监听
        //BasicMessageChannel （主要是传递字符串和一些半结构体的数据）
        //创建通
        mMessageChannel = new BasicMessageChannel<Object>(messenger, "flutter_and_native_um_100", StandardMessageCodec.INSTANCE);
        // 接收消息监听
        mMessageChannel.setMessageHandler(new BasicMessageChannel.MessageHandler<Object>() {
            @Override
            public void onMessage(final Object o, final BasicMessageChannel.Reply<Object> reply) {
                mReply = reply;
                controllMessageFunction(context, o, reply);

            }
        });

    }

    /**
     * 消息处理
     *
     * @param context
     * @param o
     * @param reply
     */
    public void controllMessageFunction(Context context, Object o, BasicMessageChannel.Reply<Object> reply) {

        try {
            Map<Object, Object> arguments = (Map<Object, Object>) o;
            //方法名标识
            String lMethod = (String) arguments.get("method");
            switch (lMethod) {
                case "umInit":
                    unInit(context, reply, arguments);
                    break;
                case "umPageStart":
                    umPageStart(arguments);
                    break;
                case "umPageEnd":
                    umPageEnd(arguments);
                    break;
                case "eventClick":
                    eventClick(context, arguments);
                    break;
                case "umError":
                    umError(context, arguments);
                    break;
                case "initPush":
                    init(context, arguments, reply);
                    break;
                case "setSplashMessage":
                    setSplashMessage(context, arguments, reply);
                    break;
            }
        } catch (Exception e) {
            Log.d(LOGTAG, "mMessageChannel 接收到消息异常 " + e.getMessage());
        }
    }

    private void umError(Context context, Map<Object, Object> arguments) {
        String errorMessage = (String) arguments.get("errorMessage");
        Log.d(LOGTAG, "umeng errorMessage【 " + "errorMessage:" + errorMessage + "】");
        //初始化
        UmengUtils.uMengError(context, errorMessage);
    }

    private void eventClick(Context context, Map<Object, Object> arguments) {
        String eventTitle = (String) arguments.get("eventTitle");
        String eventId = null;
        if (arguments.get("eventTitle") != null) {
            eventId = (String) arguments.get("eventId");
        }

        Log.d(LOGTAG, "umeng eventTitle【 " + "eventTitle:" + eventTitle + "】");
        //初始化
        UmengUtils.uMengEventObject(context, eventTitle, eventId);
    }

    private void umPageEnd(Map<Object, Object> arguments) {
        String pageTitle = (String) arguments.get("pageTitle");
        if (mPageStartList.contains(pageTitle)) {
            mPageStartList.remove(pageTitle);
        }
        Log.d(LOGTAG, "umeng umPageEnd【 " + "pageTitle:" + pageTitle + "】");
        //初始化
        UmengUtils.uMengPageEnd(pageTitle);
    }

    private void umPageStart(Map<Object, Object> arguments) {
        String pageTitle = (String) arguments.get("pageTitle");
        if (mPageStartList.contains(pageTitle)) {
            Log.d(LOGTAG, "umeng umPageStart ERR【 " + "pageTitle:" + pageTitle + "】");
        } else {
            mPageStartList.add(pageTitle);
            Log.d(LOGTAG, "umeng umPageStart【 " + "pageTitle:" + pageTitle + "】");
            //初始化
            UmengUtils.uMengPageStart(pageTitle);
        }
    }

    private void unInit(Context context, BasicMessageChannel.Reply<Object> reply, Map<Object, Object> arguments) {
        String appkey = (String) arguments.get("appkey");
        String pushSecret = (String) arguments.get("pushSecret");
        boolean logEnabled = (boolean) arguments.get("logEnabled");
        Log.d(LOGTAG, "umeng 初始化消息【 " + "appkey:" + appkey + " pushSecret:" + pushSecret + " logEnabled:" + logEnabled + "】");
        //初始化
        UmengUtils.uMengInit(context, appkey, pushSecret, logEnabled);
        //获取当前的渠道
        String lChannelName = UmengUtils.getChannelName(context);
        Map<String, String> lMap = new HashMap<>();
        lMap.put("chnnel", lChannelName);
        reply.reply(lMap);
        init(context,arguments,reply);
    }


    /**
     * 初始化UMPush
     * 在此处调用基础组件包提供的初始化函数 相应信息可在应用管理 -> 应用信息 中找到 http://message.umeng.com/list/apps
     * 参数一：当前上下文context；
     * 参数二：应用申请的Appkey（需替换）；
     * 参数三：渠道名称；
     * 参数四：设备类型，必须参数，传参数为UMConfigure.DEVICE_TYPE_PHONE则表示手机；传参数为UMConfigure.DEVICE_TYPE_BOX则表示盒子；默认为手机；
     * 参数五：Push推送业务的secret 填充Umeng Message Secret对应信息（需替换）
     */
    public void init(Context context, Map<Object, Object> map, final BasicMessageChannel.Reply<Object> reply) {
        android.util.Log.d(TAG, "initPush :");
        String umAppkey = (String) map.get("umAppkey");
        String umSecret = (String) map.get("umSecret");
        String miAppId = (String) map.get("miAppId");
        String miAppKey = (String) map.get("miAppKey");
        String mzAppId = (String) map.get("mzAppId");
        String mzAppKey = (String) map.get("mzAppKey");
        String opAppKey = (String) map.get("opAppKey");
        String opAppSecret = (String) map.get("opAppSecret");

        // 前面已初始化
//        UMConfigure.init(this, umAppkey, "Umeng", UMConfigure.DEVICE_TYPE_PHONE, umSecret);

    }

    /**
     * 全屏消息
     * 全屏消息是App首次启动打开进入的页面，以全屏图片的形式展示
     * <p>
     * 1、在主工程的values目录下的styles.xml文件中添加如下代码，
     * 并在drawable目录下放置一张名为umeng\_push\_default\_splash\_bg的默认图片（推荐1920*1080分辨率，也可以根据适配需要引用xml资源）。
     * <style name="Theme_Umeng_Push_Splash" parent="android:Theme.NoTitleBar.Fullscreen">
     * <item name="android:windowBackground">@drawable/umeng_push_default_splash_bg</item>
     * </style>
     * <p>
     * 2、新建一个Activity，继承自UmengSplashMessageActivity，重写onCustomPretreatment方法，并设置全屏消息默认跳转Activity的路径，例如：
     * public class SplashTestActivity extends UmengSplashMessageActivity {
     *
     * @Override public boolean onCustomPretreatment() {
     * InAppMessageManager mInAppMessageManager = InAppMessageManager.getInstance(this);
     * //设置应用内消息为Debug模式
     * mInAppMessageManager.setInAppMsgDebugMode(true);
     * //参数为Activity的完整包路径，下面仅是示例代码，请按实际需求填写
     * mInAppMessageManager.setMainActivityPath("com.umeng.message.example.MainActivity");
     * return super.onCustomPretreatment();
     * }
     * }
     * 说明：onCustomPretreatment方法默认的返回值为false，返回false则会走全屏消息的默认逻辑。
     * 若开发者在全屏消息的Activity里有动态申请权限的需求，则可以在onCustomPretreatment内进行处理，并return true，则全屏消息逻辑不会继续执行
     * <p>
     * 3、在主工程的AndroidManifest.xml中的<application>标签下注册Activity，并将其配置为App首次启动打开的Activity，
     * theme设置为步骤1所写的Theme_Umeng_Push_Splash，例如：
     * <activity
     * android:name="com.umeng.message.example.SplashTestActivity"
     * android:screenOrientation="portrait"
     * android:theme="@style/Theme_Umeng_Push_Splash">
     * <intent-filter>
     * <action android:name="android.intent.action.MAIN" />
     * <p>
     * <category android:name="android.intent.category.LAUNCHER" />
     * </intent-filter>
     * </activity>
     * <p>
     * 说明：
     * <p>
     * 生产模式请求服务器的最小间隔是30分钟，测试模式的最小间隔是1秒。
     * 全屏消息默认的逻辑为显示2s默认图片，若在2s内请求到全屏消息，则展示全屏消息，否则就跳转到开发者设置的页面。
     * 全屏消息的图片会自动缓存，并在有新消息到来时，删除旧消息的缓存。
     */
    public void setSplashMessage(Context context, Object o, BasicMessageChannel.Reply<Object> reply) {
        android.util.Log.d(TAG, "setSplashMessage:");
    }

}
