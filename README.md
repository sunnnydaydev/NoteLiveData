# LiveData

###### 1、简介

LiveData 是一种可观察的数据存储器类。与常规的可观察类不同，LiveData 具有生命周期感知能力。当应用组件处于活跃状态时LiveData
才会更新数据给对应的观察者。因此LiveData的使用可极大避免内存泄漏~

###### 3、基本使用

LiveData一般结合ViewModel进行使用，接下来搞一个简单的栗子~

```kotlin
/**
 * Create by SunnyDay /08/19 11:41:49
 */
class MainViewModel : ViewModel() {
    private var count = 0

    companion object {
        private const val DEFAULT = "defaultName"
    }

    val mutableLiveData: MutableLiveData<String> = MutableLiveData()

    init {
        mutableLiveData.value = DEFAULT
    }

    fun changeName(name: String) {
        mutableLiveData.value = "$name:${++count}"
    }

    fun cleanName() {
        mutableLiveData.value = DEFAULT
        count = 0
    }

    fun changeNameByTimes(name: String){
        object : CountDownTimer(1000*60,1000) {
            override fun onTick(millisUntilFinished: Long) {
                  mutableLiveData.value = "$name:${millisUntilFinished/1000}"
            }
            override fun onFinish() {
               cleanName()
            }
        }.start()
    }
    
}
```

```kotlin

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // the data of ViewModel changed, here is update
        viewModel.mutableLiveData.observe(this, object : Observer<String> {
            override fun onChanged(t: String?) {
                tvText.text = t
            }
        })

        btnChange.setOnClickListener {
            viewModel.changeName("SunnyDay")
        }

        btnReset.setOnClickListener {
            viewModel.cleanName()
        }

        btnCountDown.setOnClickListener {
            viewModel.changeNameByTimes("Tom")
        }

    }

    //注意：手机back键不会立即走这个方法。显示调用finish 或者杀进程才立即走这个。
    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity","onDestroy")
    }

}
```

（1）了解下基本使用：如上代码activity的业务逻辑抽取到了ViewModel中，ViewModel的获取采用了委托的方式，委托的方法是一个Activity扩展方法。

LiveData为ViewModel的内部类，他的value属性就是我们最终要操作的字段。除了value字段外LiveData还提供了observe方法来观察字段值的变化。

（2）验证下数据的更新：这个通过上述点击事件可以验证，点击时会更新ViewModel数据，同时在UI层也能够进行同步刷新。

（3）验证下数据始终保持最新状态：摁了返回键或者home键后进入后台，此时上述的changeNameByTimes方法内继续工作，当activity再次resume时
数据始终是最新的。

（4）趁机了解下如何避免内存泄漏：观察者会绑定到 Lifecycle 对象，并在其关联的生命周期遭到销毁后进行自我清理。因此当activity finish后
LiveData观察者也会被自动移除，避免了内存泄漏的case。

###### 4、LiveDate#setValue与LiveDate#PostValue的区别

我们知道一般情况下子线程是不允许更新UI的，这也是LiveDate设计PostValue的原因。先上结论吧：setValue只能执行在安卓main线程,
而PostValue可以跑在子线程中。

栗子~ 加入我们在activity的onCreate种执行如下代码：
```kotlin
        thread {
            //setValue
            viewModel.mutableLiveData.value = "Test"
            //postValue
            viewModel.mutableLiveData.postValue("Test")
        }
```
setValue直接crash了：java.lang.IllegalStateException: Cannot invoke setValue on a background thread

还是浅看下源码吧~

```java
public class MutableLiveData<T> extends LiveData<T> {

    /**
     * Creates a MutableLiveData initialized with the given {@code value}.
     *
     * @param value initial value
     */
    public MutableLiveData(T value) {
        super(value);
    }

    /**
     * Creates a MutableLiveData with no value assigned to it.
     */
    public MutableLiveData() {
        super();
    }
    
    @Override
    public void postValue(T value) {
        super.postValue(value);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }
}
```
（1）setValue

```java
public abstract class LiveData<T> {
    @MainThread
    protected void setValue(T value) {
        assertMainThread("setValue");
        mVersion++;
        mData = value;
        dispatchingValue(null);
    }
    static void assertMainThread(String methodName) {
        if (!ArchTaskExecutor.getInstance().isMainThread()) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a background"
                    + " thread");
        }
    }
}
```
代码很简单，首先是一个注解@MainThread说明方法需要运行在主线程，并且assertMainThread方法还做了线程检测。

（2）postValue

```java
public abstract class LiveData<T> {
    static final Object NOT_SET = new Object();
    volatile Object mPendingData = NOT_SET;
    
    protected void postValue(T value) {
        boolean postTask;
        synchronized (mDataLock) {
            postTask = mPendingData == NOT_SET;
            mPendingData = value; //1、 value 赋值给Object
        }
        if (!postTask) {
            return;
        }
        //2、通过线程池执行一个Runnable
        ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
    }
    private final Runnable mPostValueRunnable = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Object newValue;
            synchronized (mDataLock) {
                newValue = mPendingData; //3、runnable中传递value
                mPendingData = NOT_SET;
            }
            setValue((T) newValue); //4、最终调用的setValue方法
        }
    };
}
```

分析完毕，比较简单。






