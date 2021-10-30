package com.wangxingxing.startupoptimizing

import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.wangxingxing.startupoptimizing.databinding.ActivityIdleBinding

/**
 * author : 王星星
 * date : 2021/10/29 18:28
 * email : 1099420259@qq.com
 * description :
 */
class IdleActivity : AppCompatActivity() {

    private val mBinding: ActivityIdleBinding by lazy {
        ActivityIdleBinding.inflate(layoutInflater)
    }

    private lateinit var mHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        init()
    }

    private fun init() {
        mHandler = MyHandler(Looper.myLooper()!!)

        mBinding.button.setOnClickListener {
            Log.i(App.TAG, "init: 点击了Button...")
            for (i in 1..4 ) {
                mHandler.sendEmptyMessage(i)
            }
        }

        Looper.myQueue().addIdleHandler(RunOnceHandler())
        Looper.myQueue().addIdleHandler(KeepAliveHandler())
    }

    class MyHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.i(App.TAG, "handleMessage: msg:${msg.what}")
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    class KeepAliveHandler : MessageQueue.IdleHandler {
        /**
         * 返回值为true，则保持此IdleHandler在消息队列的mIdleHandlers列表当中
         */
        override fun queueIdle(): Boolean {
            idleHandlerRunCount++
            Log.i(App.TAG, "KeepAliveHandler.queueIdle...第${idleHandlerRunCount}次运行")
            return true
        }
    }

    class RunOnceHandler : MessageQueue.IdleHandler {

        override fun queueIdle(): Boolean {
            Log.i(App.TAG, "RunOnceHandler.queueIdle()...只运行一次")
            return false
        }

    }

    companion object {
        private var idleHandlerRunCount = 0
    }
}