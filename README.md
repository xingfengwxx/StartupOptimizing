# 启动优化

## 应用启动流程

- 点击桌面App图标，Launcher进程采用Binder IPC向system_server进程发起startActivity请求；
- system_server进程接收到请求后，向zygote进程发送创建进程的请求；
- Zygote进程fork出新的子进程，即App进程；
- App进程，通过Binder IPC向sytem_server进程发起attachApplication请求；
- system_server进程在收到请求后，进行一系列准备工作后，再通过binder IPC向App进程发送scheduleLaunchActivity请求；
- App进程的binder线程（ApplicationThread）在收到请求后，通过handler向主线程发送LAUNCH_ACTIVITY消息；
- 主线程在收到Message后，通过反射机制创建目标Activity，并回调Activity.onCreate()等方法。
- 到此，App便正式启动，开始进入Activity生命周期，执行完onCreate/onStart/onResume方法，UI渲染结束后便可以看到App的主界面。

![应用启动流程](https://gitee.com/xingfengwxx/blogImage/raw/master/img/20211029171137.png)

## 启动状态

应用有三种启动状态，每种状态都会影响应用向用户显示所需的时间：冷启动、温启动与热启动。

- 冷启动：

冷启动是指应用从头开始启动：系统进程在冷启动后才创建应用进程。发生冷启动的情况包括应用自设备启动后或系统终止应用后首次启动。 

- 热启动：

在热启动中，系统的所有工作就是将 Activity 带到前台。只要应用的所有 Activity 仍驻留在内存中，应用就不必重复执行对象初始化、布局加载和绘制。

- 温启动：

温启动包含了在冷启动期间发生的部分操作；同时，它的开销要比热启动高。有许多潜在状态可视为温启动。例如：

- [x] 用户在退出应用后又重新启动应用。进程可能未被销毁，继续运行，但应用需要执行 onCreate() 从头开始重新创建 Activity。

- [x] 系统将应用从内存中释放，然后用户又重新启动它。进程和 Activity 需要重启，但传递到 onCreate() 的已保存的实例savedInstanceState对于完成此任务有一定助益。


## 启动耗时统计

### 系统日志统计

在 Android 4.4（API 级别 19）及更高版本中，logcat 包含一个输出行，其中包含名为 Displayed 的值。此值代表从启动进程到在屏幕上完成对应 Activity 的绘制所用的时间。 

![系统日志统计](https://gitee.com/xingfengwxx/blogImage/raw/master/img/20211029172001.png)

### adb命令统计

adb shell am start -S -W [packageName]/.[activityName]

例如：adb shell am start -S -W com.dongnaoedu.optimizingexample/.MainActivity

![adb命令统计](https://gitee.com/xingfengwxx/blogImage/raw/master/img/20211029172149.png)

- [x] WaitTime：包括前一个应用Activity pause的时间和新应用启动的时间；
- [x] ThisTime：表示一连串启动Activity的最后一个Activity的启动耗时；
- [x] TotalTime：表示新应用启动的耗时，包括新进程的启动和Activity的启动，但不包括前一个应用Activity pause的耗时。

![启动耗时统计](https://gitee.com/xingfengwxx/blogImage/raw/master/img/20211029172523.png)

## 冷启动耗时统计

在性能测试中存在启动时间2-5-8原则：

- 当用户能够在2秒以内得到响应时，会感觉系统的响应很快；
- 当用户在2-5秒之间得到响应时，会感觉系统的响应速度还可以；
- 当用户在5-8秒以内得到响应时，会感觉系统的响应速度很慢，但是还可以接受；
- 而当用户在超过8秒后仍然无法得到响应时，会感觉系统糟透了，或者认为系统已经失去响应。

而Google也提出一项计划：Android Vitals 。该计划旨在改善 Android 设备的稳定性和性能。当选择启用了该计划的用户运行您的应用时，其 Android 设备会记录各种指标，包括应用稳定性、应用启动时间、电池使用情况、呈现时间和权限遭拒等方面的数据。Google Play 管理中心 会汇总这些数据，并将其显示在 Android Vitals 信息中心内。

当应用启动时间过长时，Android Vitals 可以通过 Play管理中心提醒您，从而帮助提升应用性能。Android Vitals 在您的应用出现以下情况时将其启动时间视为过长：

- 冷启动用了 5 秒或更长时间。
- 温启动用了 2 秒或更长时间。
- 热启动用了 1.5 秒或更长时间。

实际上不同的应用因为启动时需要初始化的数据不同，启动时间自然也会不同。相同的应用也会因为在不同的设
备，因为设备性能影响启动速度不同。所以实际上启动时间并没有绝对统一的标准，我们之所以需要进行启动耗时
的统计的，可能在于产品对我们应用启动时间提出具体的要求。

## CPU Profile

## 应用启动之后收集方法执行

- Run Configuration 配置

![CPU Profile配置](https://gitee.com/xingfengwxx/blogImage/raw/master/img/20211029172709.png)

- Profile app 运行

![Profile 运行](https://gitee.com/xingfengwxx/blogImage/raw/master/img/20211029172838.png)

| 类型           | 作用                                                         |
| -------------- | ------------------------------------------------------------ |
| Call Chart     | 根据时间线查看调用栈，便于观察每次调用是何时发生的           |
| Flame Chart    | 根据耗时百分比查看调用栈，便于发现总耗时很长的调用链         |
| Top Down Tree  | 查看记录数据中所有方法调用栈，便于观察其中每一步所消耗的精确时间 |
| Bottom Up Tree | 相对于Top Down Tree，能够更方便查看耗时方法如何被调用        |

## 通过应用插桩生成跟踪日志

如需生成应用执行的方法跟踪，您可以使用 Debug 类进行应用插桩。通过这种方式检测我们的应用，可让我们更精确地控制设备何时开始和停止记录跟踪信息。此外，设备还能使用我们指定的名称保存跟踪日志，便于我们日后轻松识别各个日志文件。我们随后可以使用 Android Studio 的 CPU Profile 查看各个跟踪日志。

- Debug.startMethodTracing("optimizing_example.trace");
- Debug.stopMethodTracing();

在调用中，可以指定 .trace 文件的名称，系统会将它保存到一个特定于软件包的目录中。该目录专门用于保存目标设备上的永久性应用数据，与 getExternalFilesDir() 返回的目录相同，在大多数设备上都位于 ~/sdcard/ 目录中。此文件包含二进制方法跟踪数据，以及一个包含线程和方法名称的映射表。如需停止跟踪，请调用 stopMethodTracing()。

## 优化布局加载

- 避免布局嵌套过深
- 把耗时的布局渲染操作放在子线程中，等inflate操作完成后再回调到主线程

### AsyncLayoutInflater

依赖：

```gradle
implementation "androidx.asynclayoutinflater:asynclayoutinflater:1.0.0"
```

Java 写法：

```java
new AsyncLayoutInflater(this).inflate(R.layout.activity_main, null,
    new AsyncLayoutInflater.OnInflateFinishedListener() {
        @Override
        public void onInflateFinished(@NonNull View view, int resid, @Nullable ViewGroup parent) {
            setContentView(view);
        }
    });
```

Kotlin 写法：

```kotlin
AsyncLayoutInflater(this).inflate(
    R.layout.activity_main, null
) { view, resid, parent -> setContentView(view) }
```


## 黑白屏问题

### 问题由来

当系统加载并启动 App 时，需要耗费相应的时间，即使时间不到 1s,用户也会感觉到当点击 App 图标时会有 “延迟” 现象，为了解决这一问题，Google 的做法是在 App 创建的过程中，先展示一个空白页面，让用户体会到点击图标之后立马就有响应；而这个空白页面的颜色是根据我们在 AndroiMainfest 文件中配置的主题背景颜色来决定的，现在一般默认是白色。

### 解决方案

- 修改AppTheme

在应用默认的 AppTheme 中，设置系统 “取消预览（空白窗体）” 为 true，或者设置空白窗体为透明。

```xml
<style name="AppTheme" parent="Theme.AppCompat.Light.NoActionBar">
         Customize your theme here. 
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
        <item name="colorAccent">@color/colorAccent</item>
        
        <!--设置系统取消预览（空白窗口）-->
        <item name="android:windowDisablePreview">true</item>
        
         <!--设置背景透明-->
        <item name="android:windowIsTranslucent">true</item>
</style>
```

- 自定义AppTheme

```xml
// styles文件中自定义启动页主题theme
 <style name="AppTheme.LaunchTheme">
       <item name="android:windowBackground">@drawable/launch_layout</item>
        <item name="windowNoTitle">true</item>
        <item name="android:windowFullscreen">true</item>
 </style>
```

将启动的 Activity 的 theme 设置为自定义主题 ：

```xml
// AndroidManifest.xml 文件中
 <activity android:name=".MainActivity" android:theme="@style/AppTheme.LaunchTheme">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />

        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

- Activity 中重新设置为系统主题

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 设置为系统主题
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //....
    }
    
}
```


## StrictMode

StrictMode是一个开发人员工具，它可以检测出我们可能无意中做的事情，并提醒我们注意，以便我们能够修复它们。

StrictMode最常用于捕获应用程序主线程上的意外磁盘或网络访问。帮助我们让磁盘和网络操作远离主线程，可以使应用程序更加平滑、响应更快。

```java
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            //线程检测策略
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()   //读、写操作
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()   //Sqlite对象泄露
                    .detectLeakedClosableObjects()  //未关闭的Closable对象泄露
                    .penaltyLog()  //违规打印日志
                    .penaltyDeath() //违规崩溃
                    .build());
        }
    }
}
```

## IdleHandler

### 介绍

IdleHandler 是 MessageQueue 内定义的一个接口，一般可用于做性能优化。当消息队列内没有需要立即执行的 message 时，会主动触发 IdleHandler 的 queueIdle 方法。返回值为 false，即只会执行一次；返回值为 true，即每次当消息队列内没有需要立即执行的消息时，都会触发该方法。

简单总结，IdleHandler可用于监听主线程是否为空闲状态（无事可干）。

```java
public final class MessageQueue {
    public static interface IdleHandler {
        boolean queueIdle();
    }
}
```

### 使用方式

通过获取 looper 对应的 MessageQueue 队列注册监听。

```java
Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
    @Override
    public boolean queueIdle() {
        // doSomething()
        return false;
    }
});
```

### 源码解析

```java
Message next() {
    // 隐藏无关代码...
    int pendingIdleHandlerCount = -1; // -1 only during first iteration
    int nextPollTimeoutMillis = 0;
    for (; ; ) {
        // 隐藏无关代码...
        // If first time idle, then get the number of idlers to run.
        // Idle handles only run if the queue is empty or if the first message
        // in the queue (possibly a barrier) is due to be handled in the future.
        if (pendingIdleHandlerCount < 0
                && (mMessages == null || now < mMessages.when)) {
            pendingIdleHandlerCount = mIdleHandlers.size();
        }
        if (pendingIdleHandlerCount <= 0) {
            // No idle handlers to run.  Loop and wait some more.
            mBlocked = true;
            continue;
        }
        if (mPendingIdleHandlers == null) {
            mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
        }
        mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
    }
    // Run the idle handlers.
    // We only ever reach this code block during the first iteration.
    for (int i = 0; i < pendingIdleHandlerCount; i++) {
        final IdleHandler idler = mPendingIdleHandlers[i];
        mPendingIdleHandlers[i] = null; // release the reference to the handler
        boolean keep = false;
        try {
            keep = idler.queueIdle();
        } catch (Throwable t) {
            Log.wtf(TAG, "IdleHandler threw exception", t);
        }
        if (!keep) {
            synchronized (this) {
                mIdleHandlers.remove(idler);
            }
        }
    }
    // Reset the idle handler count to 0 so we do not run them again.
    pendingIdleHandlerCount = 0;
    // While calling an idle handler, a new message could have been delivered
    // so go back and look again for a pending message without waiting.
    nextPollTimeoutMillis = 0;
}
```

- 在 MessageQueue 里 next 方法的 for 死循环内，获取 mIdleHandlers 的数量 pendingIdleHandlerCount；
- 通过 mMessages == null || now < mMessages.when 判断当前消息队列为空或者目前没有需要执行的消息时，给 pendingIdleHandlerCount 赋值；
- 当数量大于 0，遍历取出数组内的 IdleHandler，执行 queueIdle() ；
- 返回值为 false 时，主动移除监听 mIdleHandlers.remove(idler) ；

### 使用场景

- 如果启动的 Activity、Fragment、Dialog 内含有大量数据和视图的加载，导致首次打开时动画切换卡顿或者一瞬间白屏，可将部分加载逻辑放到 queueIdle() 内处理。例如引导图的加载和弹窗提示等；
- 系统源码中 ActivityThread 的 GcIdler，在某些场景等待消息队列暂时空闲时会尝试执行 GC 操作；
- 系统源码中  ActivityThread 的 Idler，在 handleResumeActivity() 方法内会注册 Idler()，等待 handleResumeActivity 后视图绘制完成，消息队列暂时空闲时再调用 AMS 的 activityIdle 方法，检查页面的生命周期状态，触发 activity 的 stop 生命周期等。这也是为什么我们 BActivity 跳转 CActivity 时，BActivity 生命周期的 onStop() 会在 CActivity 的 onResume() 后。
- 一些第三方框架 Glide 和 LeakCanary 等也使用到 IdleHandler；