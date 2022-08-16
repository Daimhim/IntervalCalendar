package org.daimhim.widget.ic

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import kotlin.Comparator

class V2EveryDaySource(
    /**
     * 上限
     * -1 无限
     */
    private val upperLimit: Long = -1,
    /**
     * 下限
     *  -1 无限
     */
    private val lowerLimit: Long = -1,
    /**
     * 最大月份限制
     */
    private val maxLimit: Long = -1,
    /**
     * 初始化页数 上下
     */
    private val initializationNumber: Int = 6,
) : EarlyAdapter.DataSource<EveryDay>{
    companion object{
        /**
         * 普通
         */
        const val ORDINARY_ITEM_TYPE = 0

        /**
         * 标题
         */
        const val TITLE_ITEM_TYPE = 1

        /**
         * 占位
         */
        const val PLACEHOLDER_ITEM_TYPE = 2
    }
    private val flow = MutableStateFlow<List<EveryDay>>(mutableListOf())

    private val pager : TreeMap<Long,MutableList<EveryDay>> = TreeMap(object : Comparator<Long>{
        override fun compare(o1: Long, o2: Long): Int {
            return o1.compareTo(o2)
        }
    })

    private var loading = false
    fun bindAdapter(
        lifecycle: Lifecycle,
        initialTime:Long,
        pagingDataAdapter: IntervalCalendarAdapter
    ){
        pagingDataAdapter.dataSource = this
        getRefresh(EveryDay().apply {
            millis = initialTime
        })
        lifecycle
            .coroutineScope
            .launch(Dispatchers.Main) {
                flow.collectLatest {
                    pagingDataAdapter.submitList(it)
                    loading = false
                }
            }
    }

    override fun getRefresh(key: EveryDay) {
        //向前
        val forward = Calendar.getInstance()

        forward.timeInMillis = key.millis
        //向后
        val backward = Calendar.getInstance()
        backward.timeInMillis = key.millis

        var month = getMonth(key.millis)
        pager.put(key.millis,month)

        for (i in 0 until initializationNumber){

            backward.add(Calendar.MONTH,-1)
            month = getMonth(backward.timeInMillis)
            pager.put(backward.timeInMillis,month)

            forward.add(Calendar.MONTH,1)
            month = getMonth(forward.timeInMillis)
            pager.put(forward.timeInMillis,month)


//            Log.i("V2EveryDaySource",
//                String.format("getRefresh forward %s backward %s",
//                    DataU.millisToData(forward.timeInMillis),
//                    DataU.millisToData(backward.timeInMillis)))
        }
        val mutableListOf = mutableListOf<EveryDay>()
        pager.forEach {
            mutableListOf.addAll(it.value)
        }
        flow.value = mutableListOf
    }

    override suspend fun load(loadParams: Int, key: EveryDay) {
        if (loading){
            return
        }
//        Log.i("V2EveryDaySource", String.format("load %s %s",loadParams,DataU.millisToData(key.millis)))
        val previous = Calendar.getInstance()

        val instance = Calendar.getInstance()
        instance.timeInMillis = key.millis

        when(loadParams){
            EarlyAdapter.DataSource.Append->{
                instance.add(Calendar.MONTH,1)
                if (upperLimit > 0 && upperLimit < instance.timeInMillis){
                    return
                }
                previous.timeInMillis = pager.lastKey()
                if (previous.get(Calendar.YEAR) == instance.get(Calendar.YEAR)
                    && previous.get(Calendar.MONTH) == instance.get(Calendar.MONTH)
                    && previous.get(Calendar.DAY_OF_MONTH) == instance.get(Calendar.DAY_OF_MONTH)){
                    return
                }
                if (maxLimit > 0 && maxLimit < pager.size) {
                    pager.pollFirstEntry()
                }
            }
            EarlyAdapter.DataSource.Prepend->{ //2
                instance.add(Calendar.MONTH,-1)
                if (lowerLimit > 0 && lowerLimit > instance.timeInMillis){
                    return
                }
                previous.timeInMillis = pager.firstKey()
                if (previous.get(Calendar.YEAR) == instance.get(Calendar.YEAR)
                    && previous.get(Calendar.MONTH) == instance.get(Calendar.MONTH)
                    && previous.get(Calendar.DAY_OF_MONTH) == instance.get(Calendar.DAY_OF_MONTH)){
                    return
                }
                if (maxLimit > 0 && maxLimit < pager.size) {
                    pager.pollLastEntry()
                }
            }
        }
        loading = true
        val month = getMonth(instance.timeInMillis)
        pager.put(instance.timeInMillis,month)
        val mutableListOf = mutableListOf<EveryDay>()
        pager.forEach {
            mutableListOf.addAll(it.value)
        }
        flow.value = mutableListOf
    }


    private fun getMonth(time: Long): MutableList<EveryDay> {
        val mutableListOf = mutableListOf<EveryDay>()

        val gc = GregorianCalendar()
        gc.timeInMillis = time

        // 设置月份的起始日期为1
        gc.set(Calendar.DAY_OF_MONTH, 1)

        // 标题
        mutableListOf.add(EveryDay().also {
            it.viewType = TITLE_ITEM_TYPE
            it.millis = gc.timeInMillis
            it.year = gc.get(Calendar.YEAR)
            it.moon = gc.get(Calendar.MONTH)
            it.day = gc.get(Calendar.DAY_OF_MONTH)
            it.hour = gc.get(Calendar.HOUR)
            it.minute = gc.get(Calendar.MINUTE)
            it.second = gc.get(Calendar.SECOND)
            it.enable = false
        })

        // 获取一周中的第几天 表示从周几开始本月的第一天
        val weekday = gc.get(Calendar.DAY_OF_WEEK)
        for (i in 1 until weekday){
            mutableListOf.add(EveryDay().also {
                it.viewType = PLACEHOLDER_ITEM_TYPE
                it.millis = gc.timeInMillis
                it.year = gc.get(Calendar.YEAR)
                it.moon = gc.get(Calendar.MONTH)
                it.day = gc.get(Calendar.DAY_OF_MONTH)
                it.hour = gc.get(Calendar.HOUR)
                it.minute = gc.get(Calendar.MINUTE)
                it.second = gc.get(Calendar.SECOND)
                it.enable = false
            })
        }
        // 现在属于第几个月
        val month = gc.get(Calendar.MONTH)
        do {
            // 获取当天在本月中 在第几天
            val day = gc.get(Calendar.DAY_OF_MONTH)
            mutableListOf.add(EveryDay().also {
                it.viewType = ORDINARY_ITEM_TYPE
                it.millis = gc.timeInMillis
                it.year = gc.get(Calendar.YEAR)
                it.moon = gc.get(Calendar.MONTH)
                it.day = gc.get(Calendar.DAY_OF_MONTH)
                it.hour = gc.get(Calendar.HOUR)
                it.minute = gc.get(Calendar.MINUTE)
                it.second = gc.get(Calendar.SECOND)
                it.enable = it.millis > lowerLimit || it.millis > upperLimit
            })
            println(day)
            gc.add(Calendar.DAY_OF_MONTH, 1)// 天数加1
        }while (gc.get(Calendar.MONTH) == month)
        return mutableListOf
    }

}