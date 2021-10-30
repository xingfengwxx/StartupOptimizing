package com.wangxingxing.startupoptimizing

import android.app.Application
import android.os.Debug
import android.os.StrictMode
import android.util.Log
import java.io.File

/**
 * author : 王星星
 * date : 2021/10/29 16:22
 * email : 1099420259@qq.com
 * description :
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        //保存性能分析文件到本地
        val file = File(filesDir, "optimizing_example.trace")
        Debug.startMethodTracing(file.absolutePath)
        Log.i(TAG, "onCreate: trace file path:${file.absoluteFile}")

//       openStrictMode()
    }

    /**
     * 开启严格模式
     */
    private fun openStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads() //读、写操作
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects() //Sqlite对象泄露
                    .detectLeakedClosableObjects() //未关闭的Closable对象泄露
                    .penaltyLog() //打印日志
                    .penaltyDeath() //发生违规，应用直接崩溃
                    .build()
            )
        }
    }

    companion object {
        const val TAG = "wxx"
    }
}