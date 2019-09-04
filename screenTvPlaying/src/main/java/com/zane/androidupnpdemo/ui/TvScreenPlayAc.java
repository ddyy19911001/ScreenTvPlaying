package com.zane.androidupnpdemo.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.kongzue.dialog.listener.InputDialogOkButtonClickListener;
import com.kongzue.dialog.v2.DialogSettings;
import com.kongzue.dialog.v2.InputDialog;
import com.kongzue.dialog.v2.MessageDialog;
import com.kongzue.dialog.v2.SelectDialog;
import com.zane.androidupnpdemo.Intents;
import com.zane.androidupnpdemo.control.ClingPlayControl;
import com.zane.androidupnpdemo.control.callback.ControlCallback;
import com.zane.androidupnpdemo.control.callback.ControlReceiveCallback;
import com.zane.androidupnpdemo.entity.ClingDevice;
import com.zane.androidupnpdemo.entity.ClingDeviceList;
import com.zane.androidupnpdemo.entity.DLANPlayState;
import com.zane.androidupnpdemo.entity.IDevice;
import com.zane.androidupnpdemo.entity.IResponse;
import com.zane.androidupnpdemo.listener.BrowseRegistryListener;
import com.zane.androidupnpdemo.listener.DeviceListChangedListener;
import com.zane.androidupnpdemo.service.ClingUpnpService;
import com.zane.androidupnpdemo.service.manager.ClingManager;
import com.zane.androidupnpdemo.service.manager.DeviceManager;
import com.zane.androidupnpdemo.util.Utils;
import com.zane.clinglibrary.R;

import org.fourthline.cling.model.meta.Device;

import java.util.Collection;

import yin.deng.superbase.activity.LogUtils;
import yin.deng.superbase.activity.ScreenUtils;
import yin.deng.superbase.activity.SuperBaseActivity;

public class TvScreenPlayAc extends SuperBaseActivity implements SwipeRefreshLayout.OnRefreshListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = TvScreenPlayAc.class.getSimpleName();
    /** 连接设备状态: 播放状态 */
    public static final int PLAY_ACTION = 0xa1;
    /** 连接设备状态: 暂停状态 */
    public static final int PAUSE_ACTION = 0xa2;
    /** 连接设备状态: 停止状态 */
    public static final int STOP_ACTION = 0xa3;
    /** 连接设备状态: 转菊花状态 */
    public static final int TRANSITIONING_ACTION = 0xa4;
    /** 获取进度 */
    public static final int GET_POSITION_INFO_ACTION = 0xa5;
    /** 投放失败 */
    public static final int ERROR_ACTION = 0xa5;

    private Context mContext;
    private Handler mHandler = new InnerHandler();

    private ListView mDeviceList;
    private SwipeRefreshLayout mRefreshLayout;
    private TextView mTVSelected;
    private SeekBar mSeekProgress;
    private SeekBar mSeekVolume;
    private Switch mSwitchMute;
    private String playUrl;
    private TextView tvEmpty;
    private ImageView ivBack;
    private TextView tvJd;
    private TextView tvOpenThirdApp;

    private BroadcastReceiver mTransportStateBroadcastReceiver;
    private ArrayAdapter<ClingDevice> mDevicesAdapter;
    /**
     * 投屏控制器
     */
    private ClingPlayControl mClingPlayControl = new ClingPlayControl();

    /** 用于监听发现设备 */
    private BrowseRegistryListener mBrowseRegistryListener = new BrowseRegistryListener();

    private ServiceConnection mUpnpServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.e(TAG, "mUpnpServiceConnection onServiceConnected");

            ClingUpnpService.LocalBinder binder = (ClingUpnpService.LocalBinder) service;
            ClingUpnpService beyondUpnpService = binder.getService();

            ClingManager clingUpnpServiceManager = ClingManager.getInstance();
            clingUpnpServiceManager.setUpnpService(beyondUpnpService);
            clingUpnpServiceManager.setDeviceManager(new DeviceManager());

            clingUpnpServiceManager.getRegistry().addListener(mBrowseRegistryListener);
            //Search on service created.
            clingUpnpServiceManager.searchDevices();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "mUpnpServiceConnection onServiceDisconnected");

            ClingManager.getInstance().setUpnpService(null);
        }
    };


    public void showLinearBar(){
        TextView linerBar = (TextView)findViewById(R.id.tv_linear_bar);
        if(linerBar!=null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) linerBar.getLayoutParams();
            params.height = ScreenUtils.getStatusHeight(this);
            linerBar.setLayoutParams(params);
        }
    }

    @Override
    public int setLayout() {
        return R.layout.tv_screen_play_ac;
    }

    @Override
    protected void initFirst() {
        DialogSettings.style = DialogSettings.STYLE_IOS;
        showLinearBar();
        playUrl=getIntent().getStringExtra("url");
        mContext = this;

        if(TextUtils.isEmpty(playUrl)){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setTitle("系统提示")
                    .setMessage("播放地址有误，无法投屏！")
                    .setPositiveButton("返回上页", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            finish();
                        }
                    }).setCancelable(false).create().show();
            return;
        }
        initView();
        initListeners();
        bindServices();
        registerReceivers();
        tvOpenThirdApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectDialog.show(TvScreenPlayAc.this, "系统提示", "是否使用专业投屏软件《快点投屏App》投放本视频？", "打开快点投屏", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        kuaiDianTp(playUrl);
                    }
                }, "暂时不用", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            }
        });
        tvJd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetJd();
            }
        });
    }

    public void onSetJd(){
        InputDialog.build(TvScreenPlayAc.this, "输入本影片总分钟数", "请输入当前投屏的影片的总时长（总分钟数），便于为您提供更精确的进度控制。", "提交", new InputDialogOkButtonClickListener() {
            @Override
            public void onClick(Dialog dialog, String inputText) {
                dialog.dismiss();
                if(TextUtils.isEmpty(inputText)){
                    MessageDialog.show(TvScreenPlayAc.this, "系统提示", "请输入视频总分钟数再提交", "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            onSetJd();
                        }
                    });
                    return;
                }
                if(inputText.length()>3){
                    MessageDialog.show(TvScreenPlayAc.this,"系统提示","总分钟数不能超过3位数哦", "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            onSetJd();
                        }
                    });
                    return;
                }
                if(inputText.equals("0")){
                    MessageDialog.show(TvScreenPlayAc.this,"系统提示","请输入合理的视频总时长（分钟数）", "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            onSetJd();
                        }
                    });
                    return;
                }
                dialog.dismiss();
                int s=120;
                try {
                    s=Integer.parseInt(inputText);
                    mSeekProgress.setMax(60*s);
                    showTs("设置成功，请点击播放重新加载");
                }catch (Exception e){
                    e.printStackTrace();
                    MessageDialog.show(TvScreenPlayAc.this,"系统提示","请不要输入中文或特殊字符", "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            onSetJd();
                        }
                    });
                }

            }
        }, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).setCanCancel(true).showDialog();
    }


    //使用快点投屏com.wukongtv.wkcast
    private void kuaiDianTp(String realUrl) {
        boolean isInstallApp=openThirdApp(this,realUrl,"com.wukongtv.wkcast","com.wukongtv.wkcast.main.MainActivity");
        if(!isInstallApp){
            SelectDialog.show(this, "系统提示", "未安装该应用，请先下载安装《快点投屏app》后回到莲银影视重新投屏。", "去下载", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    openBrowser(TvScreenPlayAc.this, "https://static2.wukongtv.com/quickdownload/dist/down?c=share");
                    dialogInterface.dismiss();
                }
            }, "暂时不用", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

        }
    }


    /**
     * 调用第三方浏览器打开
     * @param context
     * @param url 要浏览的资源地址
     */
    public static  boolean openThirdApp(Context context, String url,String packageName,String className){
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        //包名、要打开的activity
        intent.setClassName(packageName, className);
        try {
            context.startActivity(intent);
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 调用第三方浏览器打开
     * @param context
     * @param url 要浏览的资源地址
     */
    public void openBrowser(final Context context, final String url){
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        //包名、要打开的activity
        intent.setClassName("com.tencent.mtt", "com.tencent.mtt.MainActivity");
        try {
            context.startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
            intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
            // 注意此处的判断intent.resolveActivity()可以返回显示该Intent的Activity对应的组件名
            // 官方解释 : Name of the component implementing an activity that can display the intent
            try {
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    final ComponentName componentName = intent.resolveActivity(context.getPackageManager());
                    // 打印Log   ComponentName到底是什么
                    LogUtils.d("componentName = " + componentName.getClassName());
                    context.startActivity(Intent.createChooser(intent, "请选择打开方式"));
                } else {
                    SelectDialog.build(context, "系统提示", "无法使用此方式打开，请点击下方复制按钮，在浏览器中粘贴进行打开！", "复制链接", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            copy(TvScreenPlayAc.this,url);
                            showTs("复制成功，请打开浏览器粘贴");
                        }
                    }, "取消操作", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).showDialog();
                }
            }catch (Exception ex){
                ex.printStackTrace();
                SelectDialog.build(context, "系统提示", "无法使用此方式打开，请点击下方复制按钮，在浏览器中粘贴进行打开！", "复制链接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        copy(TvScreenPlayAc.this,url);
                        showTs("复制成功，请打开浏览器粘贴");
                    }
                }, "取消操作", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).showDialog();
            }
        }
    }

    public static void copy(Context context,String url) {
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("莲银", url);
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
    }


    /**
     * 设置状态栏背景及字体颜色
     */
    public void setStatusStyle(int statusColorRes) {
        statusColorRes=getResources().getColor(R.color.black);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 设置状态栏底色白色
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(statusColorRes);
            // 设置状态栏字体黑色
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }


    //    private ServiceConnection mSystemServiceConnection = new ServiceConnection() {
    //        @Override
    //        public void onServiceConnected(ComponentName className, IBinder service) {
    //            Log.e(TAG, "mSystemServiceConnection onServiceConnected");
    //
    //            SystemService.LocalBinder systemServiceBinder = (SystemService.LocalBinder) service;
    //            //Set binder to SystemManager
    //            ClingManager clingUpnpServiceManager = ClingManager.getInstance();
    ////            clingUpnpServiceManager.setSystemService(systemServiceBinder.getService());
    //        }
    //
    //        @Override
    //        public void onServiceDisconnected(ComponentName className) {
    //            Log.e(TAG, "mSystemServiceConnection onServiceDisconnected");
    //
    //            ClingUpnpServiceManager.getInstance().setSystemService(null);
    //        }
    //    };



    private void registerReceivers() {
        //Register play status broadcast
        mTransportStateBroadcastReceiver = new TransportStateBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_PLAYING);
        filter.addAction(Intents.ACTION_PAUSED_PLAYBACK);
        filter.addAction(Intents.ACTION_STOPPED);
        filter.addAction(Intents.ACTION_TRANSITIONING);
        registerReceiver(mTransportStateBroadcastReceiver, filter);
    }


    private void bindServices() {
        // Bind UPnP service
        Intent upnpServiceIntent = new Intent(TvScreenPlayAc.this, ClingUpnpService.class);
        bindService(upnpServiceIntent, mUpnpServiceConnection, Context.BIND_AUTO_CREATE);
        // Bind System service
        //        Intent systemServiceIntent = new Intent(TvScreenPlayAc.this, SystemService.class);
        //        bindService(systemServiceIntent, mSystemServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        // Unbind UPnP service
        unbindService(mUpnpServiceConnection);
        // Unbind System service
        //        unbindService(mSystemServiceConnection);
        // UnRegister Receiver
        unregisterReceiver(mTransportStateBroadcastReceiver);

        ClingManager.getInstance().destroy();
        ClingDeviceList.getInstance().destroy();
    }



    private void initView() {
        tvJd = (TextView) findViewById(R.id.tv_jd);
        tvOpenThirdApp = (TextView) findViewById(R.id.open_third_app);
        mDeviceList = (ListView)findViewById(R.id.lv_devices);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_refresh);
        mTVSelected = (TextView) findViewById(R.id.tv_selected);
        mSeekProgress = (SeekBar) findViewById(R.id.seekbar_progress);
        mSeekVolume = (SeekBar) findViewById(R.id.seekbar_volume);
        mSwitchMute = (Switch) findViewById(R.id.sw_mute);
        tvEmpty=findViewById(R.id.tv_empty);
        mDevicesAdapter = new DevicesAdapter(mContext);
        mDeviceList.setAdapter(mDevicesAdapter);
        ivBack=findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if(mDevicesAdapter.getCount()==0){
            tvEmpty.setVisibility(View.VISIBLE);
        }else{
            tvEmpty.setVisibility(View.GONE);
        }
        /** 这里为了模拟 seek 效果(假设视频时间为 15s)，拖住 seekbar 同步视频时间，
         * 在实际中 使用的是片源的时间 */
        mSeekProgress.setMax(60*120);

        // 最大音量就是 100，不要问我为什么
        mSeekVolume.setMax(100);
    }




    private void initListeners() {
        mRefreshLayout.setOnRefreshListener(this);

        mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 选择连接设备
                ClingDevice item = mDevicesAdapter.getItem(position);
                if (Utils.isNull(item)) {
                    Toast.makeText(TvScreenPlayAc.this,"该设备无法连接，请重新选择",Toast.LENGTH_SHORT).show();
                    return;
                }

                ClingManager.getInstance().setSelectedDevice(item);

                Device device = item.getDevice();
                if (Utils.isNull(device)) {
                    Toast.makeText(TvScreenPlayAc.this,"该设备无法连接，请重新选择",Toast.LENGTH_SHORT).show();
                    return;
                }

                String selectedDeviceName = String.format(getString(R.string.selectedText), device.getDetails().getFriendlyName());
                mTVSelected.setText(selectedDeviceName);
            }
        });

        // 设置发现设备监听
        mBrowseRegistryListener.setOnDeviceListChangedListener(new DeviceListChangedListener() {
            @Override
            public void onDeviceAdded(final IDevice device) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mDevicesAdapter.add((ClingDevice) device);
                        if(mDevicesAdapter.getCount()==0){
                            tvEmpty.setVisibility(View.VISIBLE);
                        }else{
                            tvEmpty.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onDeviceRemoved(final IDevice device) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mDevicesAdapter.remove((ClingDevice) device);
                        if(mDevicesAdapter.getCount()==0){
                            tvEmpty.setVisibility(View.VISIBLE);
                        }else{
                            tvEmpty.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        // 静音开关
        mSwitchMute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mClingPlayControl.setMute(isChecked, new ControlCallback() {
                    @Override
                    public void success(IResponse response) {
                        Log.e(TAG, "setMute success");
                        Toast.makeText(TvScreenPlayAc.this,"已静音",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void fail(IResponse response) {
                        Log.e(TAG, "setMute fail");
                        Toast.makeText(TvScreenPlayAc.this,"关闭静音",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        mSeekProgress.setOnSeekBarChangeListener(this);
        mSeekVolume.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onRefresh() {
        mRefreshLayout.setRefreshing(true);
        mDeviceList.setEnabled(false);

        mRefreshLayout.setRefreshing(false);
        refreshDeviceList();
        mDeviceList.setEnabled(true);
    }

    /**
     * 刷新设备
     */
    private void refreshDeviceList() {
        Collection<ClingDevice> devices = ClingManager.getInstance().getDmrDevices();
        ClingDeviceList.getInstance().setClingDeviceList(devices);
        if (devices != null) {
            mDevicesAdapter.clear();
            mDevicesAdapter.addAll(devices);
        }
        if(mDevicesAdapter.getCount()==0){
            tvEmpty.setVisibility(View.VISIBLE);
        }else{
            tvEmpty.setVisibility(View.GONE);
        }

    }




    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.bt_play) {
            play();
        } else if (id == R.id.bt_pause) {
            pause();
        } else if (id == R.id.bt_stop) {
            stop();
        }
    }

    /**
     * 停止
     */
    private void stop() {
        mClingPlayControl.stop(new ControlCallback() {
            @Override
            public void success(IResponse response) {
                Log.e(TAG, "stop success");
            }

            @Override
            public void fail(IResponse response) {
                Log.e(TAG, "stop fail");
            }
        });
    }

    /**
     * 暂停
     */
    private void pause() {
        mClingPlayControl.pause(new ControlCallback() {
            @Override
            public void success(IResponse response) {
                Log.e(TAG, "pause success");
            }

            @Override
            public void fail(IResponse response) {
                Log.e(TAG, "pause fail");
            }
        });
    }

    public void getPositionInfo() {
        mClingPlayControl.getPositionInfo(new ControlReceiveCallback() {
            @Override
            public void receive(IResponse response) {

            }

            @Override
            public void success(IResponse response) {

            }

            @Override
            public void fail(IResponse response) {

            }
        });
    }

    /**
     * 播放视频
     */
    private void play() {
        @DLANPlayState.DLANPlayStates int currentState = mClingPlayControl.getCurrentState();

        /**
         * 通过判断状态 来决定 是继续播放 还是重新播放
         */

        if (currentState == DLANPlayState.STOP) {
            mClingPlayControl.playNew(playUrl, new ControlCallback() {

                @Override
                public void success(IResponse response) {
                    Log.e(TAG, "play success");
                    //                    ClingUpnpServiceManager.getInstance().subscribeMediaRender();
                    //                    getPositionInfo();
                    // TODO: 17/7/21 play success
                    ClingManager.getInstance().registerAVTransport(mContext);
                    ClingManager.getInstance().registerRenderingControl(mContext);
                }

                @Override
                public void fail(IResponse response) {
                    Log.e(TAG, "play fail");
                    mHandler.sendEmptyMessage(ERROR_ACTION);
                }
            });
        } else {
            mClingPlayControl.play(new ControlCallback() {
                @Override
                public void success(IResponse response) {
                    Log.e(TAG, "play success");
                }

                @Override
                public void fail(IResponse response) {
                    Log.e(TAG, "play fail");
                    mHandler.sendEmptyMessage(ERROR_ACTION);
                }
            });
        }
    }

    /******************* start progress changed listener *************************/

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.e(TAG, "Start Seek");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.e(TAG, "Stop Seek");
        int id = seekBar.getId();
        // 进度
        // 音量
        if (id == R.id.seekbar_progress) {
            int currentProgress = seekBar.getProgress() * 1000; // 转为毫秒
            mClingPlayControl.seek(currentProgress, new ControlCallback() {
                @Override
                public void success(IResponse response) {
                    Log.e(TAG, "seek success");
                }

                @Override
                public void fail(IResponse response) {
                    Log.e(TAG, "seek fail");
                }
            });
        } else if (id == R.id.seekbar_volume) {
            int currentVolume = seekBar.getProgress();
            mClingPlayControl.setVolume(currentVolume, new ControlCallback() {
                @Override
                public void success(IResponse response) {
                    Log.e(TAG, "volume success");
                }

                @Override
                public void fail(IResponse response) {
                    Log.e(TAG, "volume fail");
                }
            });
        }
    }

    /******************* end progress changed listener *************************/

    private final class InnerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case PLAY_ACTION:
                    Log.i(TAG, "Execute PLAY_ACTION");
                    Toast.makeText(mContext, "正在投放", Toast.LENGTH_SHORT).show();
                    mClingPlayControl.setCurrentState(DLANPlayState.PLAY);

                    break;
                case PAUSE_ACTION:
                    Log.i(TAG, "Execute PAUSE_ACTION");
                    mClingPlayControl.setCurrentState(DLANPlayState.PAUSE);
                    Toast.makeText(TvScreenPlayAc.this,"暂停投放",Toast.LENGTH_SHORT).show();
                    break;
                case STOP_ACTION:
                    Log.i(TAG, "Execute STOP_ACTION");
                    mClingPlayControl.setCurrentState(DLANPlayState.STOP);

                    break;
                case TRANSITIONING_ACTION:
                    Log.i(TAG, "Execute TRANSITIONING_ACTION");
                    Toast.makeText(mContext, "正在连接", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR_ACTION:
                    Log.e(TAG, "Execute ERROR_ACTION");
                    Toast.makeText(mContext, "投放失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    /**
     * 接收状态改变信息
     */
    private class TransportStateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e(TAG, "Receive playback intent:" + action);
            if (Intents.ACTION_PLAYING.equals(action)) {
                mHandler.sendEmptyMessage(PLAY_ACTION);

            } else if (Intents.ACTION_PAUSED_PLAYBACK.equals(action)) {
                mHandler.sendEmptyMessage(PAUSE_ACTION);

            } else if (Intents.ACTION_STOPPED.equals(action)) {
                mHandler.sendEmptyMessage(STOP_ACTION);

            } else if (Intents.ACTION_TRANSITIONING.equals(action)) {
                mHandler.sendEmptyMessage(TRANSITIONING_ACTION);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.bottom_in, R.anim.bottom_out);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.top_in, R.anim.top_out);
    }

}