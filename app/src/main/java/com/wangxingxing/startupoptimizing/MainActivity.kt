package com.wangxingxing.startupoptimizing

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.view.View
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import com.wangxingxing.startupoptimizing.databinding.ActivityMainBinding

/**
 * author : 王星星
 * date : 2021/10/29 18:27
 * email : 1099420259@qq.com
 * description : 
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // 设置回系统主题
        setTheme(R.style.Theme_StartupOptimizing)

//        setContentView(R.layout.activity_main)
        // 把耗时的布局渲染操作放在子线程中，等inflate操作完成后再回调到主线程
        AsyncLayoutInflater(this)
            .inflate(R.layout.activity_main, null) { view, resId, parent ->
                setContentView(view)
            }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Debug.stopMethodTracing()
    }

    fun toIdleActivity(view: View) {
        startActivity(Intent(this, IdleActivity::class.java))
    }
}