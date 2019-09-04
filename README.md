# ScreenTvPlaying

## 使用方法
  
# 项目的build.gradle中加

    maven { url 'https://jitpack.io' }
        mavenCentral()
        maven {
            url 'http://4thline.org/m2'
        }



# app的build.gradle 添加
   
    packagingOptions {
        exclude 'META-INF/LICENSE.txt'
        exclude 'META-INF/beans.xml'
    }
    
     dependencies {
	        implementation 'com.github.ddyy19911001:ScreenTvPlaying:1.0.2'
		implementation 'com.github.ddyy19911001:DySuperBase:1.0.3'
	  }
    
# manifast中添加
    
        <uses-permission android:name="android.permission.INTERNET" />
        <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
        <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
        <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
        <uses-permission android:name="android.permission.WAKE_LOCK" /> 
        
        ........
    
      <activity android:name="com.zane.androidupnpdemo.ui.TvScreenPlayAc"
            >
        <!-- 这行代码便可以隐藏ActionBar -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
            </intent-filter>
      </activity>
        
      <service
            android:name="com.zane.androidupnpdemo.service.ClingUpnpService"
            android:exported="false" />

      <service
            android:name="com.zane.androidupnpdemo.service.SystemService"
            android:exported="false" />
            
 # 调用的地方使用
   
    public void screenPlay(String playUrl) {
        if(playUrl!=null) {
            if(playUrl.contains("url=")){
                String s=playUrl.substring(playUrl.indexOf("url=")+4);
                if(s.startsWith("http")){
                    playUrl=s;
                }
            }
            Log.i("要投屏的地址：",playUrl);
            Intent intent = new Intent(MainActivity.this, TvScreenPlayAc.class);
            intent.putExtra("url", playUrl);
            startActivity(intent);
        }else{
            Toast.makeText(this,"视频地址无法播放",Toast.LENGTH_SHORT).show();
        }
    }

### 主要功能

发现设备
- 发现设备
- 发现设备监听

操作功能：
- 播放
- 暂停
- 停止
- 进度拖拽
- 音量调节
- 设置静音
# ScreenTvPlaying
