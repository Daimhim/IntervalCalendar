## 选择区间日历，选择酒店入住
项目中的效果

![image.png]()

#### 常用的参数
+ 设置日期上限，不设置默认无限
  `
  private long upperLimit = -1L;
  `
+ 设置日期下限，不设置默认无限
  `
  private long lowerLimit = -1L;
  `
+ 从哪年哪月那时那分 开始向下向后 居中的日期
  `
  private long initialTime = -1L;
  `
+ 最大缓存页数,无限？不可能真的无限 做了回收 这个参数是用来控制最大缓存数字，超出自动回收，最好大于预加载页数
  `
  private int maxCacheLimit = -1;
  `
+ 初始时数量，这个数量同时代表前后 都会加载这么多，注意第一次加载要覆盖全屏幕
  `
  private int initializationNumber = 5;
  `
+ 选择最大区间，默认无限 通过回收可以实现选取全时间段
  `
  private int maxInterval = -1;
  `
+ 设置用户选中时间的监听 只要点击有效的Item就会回调
  `
  void setOnSelectTimeChangeListener(OnSelectTimeChangeListener onSelectTimeChangeListener)
  `
+ 设置生命周期，这个方法比较特殊 他作为日历控件设置的结束，设置他以后就不能在修改日历控件
  `
  void setLifecycle(Lifecycle lifecycle)
  `
+ 设置每个Item布局，默认布局仅仅是一个代码创建的TextView，用来测试 这个接口是一个简化的Adapter 你只需要创建相应的布局就可以了
  `
  void setEveryDayFactory(IntervalCalendarAdapter.EveryDayFactory<VH> everyDayFactory
  `
#### 使用指南
+ 布局

```
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:orientation="vertical"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <org.daimhim.widget.ic.IntervalCalendar
        android:id="@+id/ic_calendar"
        android:layout_width="match_parent"
        android:layout_height="400dp"
        android:background="#ffffff" />
    <Button
        android:text="clear"
        android:id="@+id/tv_clear"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
</LinearLayout>
```
+ 代码

```
class MainActivity : AppCompatActivity() {
    private lateinit var ic: IntervalCalendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ic = findViewById(R.id.ic_calendar)
        // 创建初始时间 并吧时分秒 归零
        val instance = Calendar.getInstance()
        instance.set(Calendar.HOUR_OF_DAY,0)
        instance.set(Calendar.MINUTE,0)
        instance.set(Calendar.SECOND,0)
        instance.set(Calendar.MILLISECOND,0)
        // 初始时间
        ic.initialTime = instance.timeInMillis
        // 最大缓存时间
        ic.maxCacheLimit = 12
        // 初始加载页数
        ic.initializationNumber = 3
        // 最大选择区间
        ic.maxInterval = 30
        // 设置自定义布局
        ic.setEveryDayFactory(object : IntervalCalendarAdapter.EveryDayFactory<EveryDayViewHolder>{
            /**
             * Create 创建View
             *
             * @param parent
             * @param viewType
             * @return
             */
            override fun create(parent: ViewGroup?, viewType: Int): EveryDayViewHolder {
                return super.create(parent, viewType)
            }

            /**
             * Bind 全量刷新  嗯 就是全部修改
             *
             * @param holder
             * @param everyDay
             * @param selectedState
             */
            override fun bind(
                holder: EveryDayViewHolder?,
                everyDay: EveryDay?,
                selectedState: Int
            ) {
                super.bind(holder, everyDay, selectedState)
            }

            /**
             * Bind 刷新数据的高级应用，如果仅修改UI效果  请使用这个
             *
             * @param holder
             * @param everyDay
             * @param selectedState
             * @param payloads
             */
            override fun bind(
                holder: EveryDayViewHolder?,
                everyDay: EveryDay?,
                selectedState: Int,
                payloads: MutableList<Any>?
            ) {
                super.bind(holder, everyDay, selectedState, payloads)
            }
        })
        // 设置生命周期
        ic.lifecycle = lifecycle
        // 清理选中
        findViewById<View>(R.id.tv_clear)
            .setOnClickListener {
                ic.clearSelect()
            }
    }


}

```
+ 效果

![image.png]()