package org.daimhim.demo

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import org.daimhim.widget.ic.EveryDay
import org.daimhim.widget.ic.IntervalCalendar
import org.daimhim.widget.ic.IntervalCalendarAdapter
import org.daimhim.widget.ic.IntervalCalendarAdapter.EveryDayViewHolder
import java.util.*

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