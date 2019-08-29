package com.zane.androidupnpdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.zane.androidupnpdemo.ui.TvScreenPlayAc;
import com.zane.test.R;

public class MainActivity extends Activity {
    private String playUrl="http://jx.cx77m1.cn/?url=http://www.gaoya123.cn/2019/1564753701598.m3u8";

    @Override
    protected void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void connected(View view) {
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
}
