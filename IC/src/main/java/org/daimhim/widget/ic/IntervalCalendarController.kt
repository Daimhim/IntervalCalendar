package org.daimhim.widget.ic

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.paging.*
import java.text.SimpleDateFormat
import java.util.*

class IntervalCalendarController(private val intervalCalendarAdapter: IntervalCalendar.IntervalCalendarAdapter) {
    companion object {
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

        @JvmStatic
        fun isSameDay(current: Calendar, time: Calendar): Int {
            var compareTo = current.get(Calendar.YEAR)
                .compareTo(time.get(Calendar.YEAR))
            if (compareTo != 0) {
                return compareTo
            }
            compareTo = current.get(Calendar.MONTH)
                .compareTo(time.get(Calendar.MONTH))
            if (compareTo != 0) {
                return compareTo
            }
            return current.get(Calendar.DAY_OF_MONTH)
                .compareTo(time.get(Calendar.DAY_OF_MONTH))
        }

        @JvmStatic
        fun itemStatus(config: Config, gc: Calendar): Int {
            val startTime = config.startTimeCache!!
            val finishTime = config.finishTimeCache!!
            return when {
                startTime.timeInMillis > -1
                        && isSameDay(gc, startTime) == 0 -> {
                    // 选中开始
                    IntervalCalendar.START_SELECT_STATE
                }
                finishTime.timeInMillis > -1
                        && isSameDay(gc, finishTime) == 0 -> {
                    // 选中结束
                    IntervalCalendar.END_SELECT_STATE
                }
                startTime.timeInMillis > -1
                        && finishTime.timeInMillis > -1
                        && isSameDay(gc, startTime) > 0
                        && isSameDay(gc, finishTime) < 0 -> {
                    // 选中区间
                    IntervalCalendar.SELECT_STATE
                }
                config.minLimitCache!!.timeInMillis > -1
                        && isSameDay(gc, config.minLimitCache!!) < 0 -> {
                    //禁止选择
                    IntervalCalendar.UNSATISFACTORY_STATE
                }
                config.maxLimitCache!!.timeInMillis > -1
                        && isSameDay(gc, config.maxLimitCache!!) > 0 -> {
                    //禁止选择
                    IntervalCalendar.UNSATISFACTORY_STATE
                }
                else -> {
                    IntervalCalendar.UNSELECTED_STATE
                }
            }
        }

        @JvmStatic
        fun isSameDayEx(start: Long, tag: Long): Int {
            val startTime = Calendar.getInstance()
            startTime.timeInMillis = start

            val tagTime = Calendar.getInstance()
            tagTime.timeInMillis = tag

            return isSameDay(startTime, tagTime)
        }
    }

    lateinit var config: Config
    var onSelectTimeChangeListener: IntervalCalendar.OnSelectTimeChangeListener? = null
    fun init() {
        val instance = Calendar.getInstance()
        // 重置 计算日历最小时间
        instance.timeInMillis = config.timeCenter
        instance.add(Calendar.MONTH, -config.min)
        config.minTime = instance.timeInMillis

        // 重置计算日历最大时间
        instance.timeInMillis = config.timeCenter
        instance.add(Calendar.MONTH, config.max)
        config.maxTime = instance.timeInMillis

        // 重置 干其他事
        instance.timeInMillis = config.timeCenter
        if (config.startTime < 0 && !config.isCancel) {
            config.startTime = config.timeCenter
        }
        if (config.finishTime < 0 && !config.isCancel) {
            instance.add(Calendar.DAY_OF_MONTH, 1)
            config.finishTime = instance.timeInMillis
        }

        // 重置 干其他事
        instance.timeInMillis = config.timeCenter
        Log.i("EveryDaySource", String.format("%s %s", instance.get(Calendar.YEAR), instance.get(Calendar.MONTH)))
        val pager = Pager(
            config = PagingConfig(
                pageSize = 6
            ),
            initialKey = String.format(
                "%s/%s", instance.get(Calendar.YEAR),
                instance.get(Calendar.MONTH) + 1
            ),
            pagingSourceFactory = {
                EveryDaySource(config)
            }
        )
        pager
            .liveData
            .observeForever {

                intervalCalendarAdapter.submitData(pagingData = it)
            }
        intervalCalendarAdapter.setOnItemClickListener { viewHolder, view, position ->
            Log.i("chooseStatusChange", String.format("position %s", position))
            val itemDay = intervalCalendarAdapter.getItemDay(position)
            //排除标题、占位、不可选
            if (itemDay.viewType == TITLE_ITEM_TYPE
                || itemDay.viewType == PLACEHOLDER_ITEM_TYPE
                || itemDay.selectedState == IntervalCalendar.UNSATISFACTORY_STATE
            ) {
                return@setOnItemClickListener
            }
            // 当前时间
            val itemTime = Calendar.getInstance()
            itemTime.timeInMillis = itemDay.millis
            // 从未选择开始时间
            if (config.startTime < 0) {
                //刷新
                intervalCalendarAdapter.notifyItemChanged(position, 0)
                config.startTime = itemDay.millis
                onSelectTimeChangeListener?.onTimeChange(config.startTime, config.finishTime)
                return@setOnItemClickListener
            }
            //开始时间
            val startTime = Calendar.getInstance()
            startTime.timeInMillis = config.startTime


            // 点击了开始 并且开启了不可取消 则直接返回
            val startSameDay = isSameDay(itemTime, startTime)
            if (startSameDay == 0) {
                if (config.isCancel) {
                    // 取消
                    intervalCalendarAdapter.chooseStatusChange(position, config.finishTime)
                    config.startTime = config.finishTime
                    config.finishTime = -1
                    onSelectTimeChangeListener?.onTimeChange(config.startTime, config.finishTime)
                }
                return@setOnItemClickListener
            }
            // 当前点击小于开始
            if (startSameDay < 0) {
                //刷新
                intervalCalendarAdapter.chooseStatusChange(position, config.startTime)
                config.startTime = itemDay.millis
                onSelectTimeChangeListener?.onTimeChange(config.startTime, config.finishTime)
                return@setOnItemClickListener
            }
            // 从未选择结束时间
            if (config.finishTime < 0) {
                //刷新
                intervalCalendarAdapter.chooseStatusChange(config.startTime, itemDay.millis)
                config.finishTime = itemDay.millis
                onSelectTimeChangeListener?.onTimeChange(config.startTime, config.finishTime)
                return@setOnItemClickListener
            }
            // 结束时间
            val finishTime = Calendar.getInstance()
            finishTime.timeInMillis = config.finishTime
            val endSameDay = isSameDay(itemTime, finishTime)
            if (endSameDay == 0) {
                if (config.isCancel) {
                    // 取消
                    intervalCalendarAdapter.chooseStatusChange(config.startTime, position)
                    config.finishTime = -1
                    onSelectTimeChangeListener?.onTimeChange(config.startTime, config.finishTime)
                }
                return@setOnItemClickListener
            }
            // 当前点击大于结束
            if (endSameDay > 0) {
                //刷新
                intervalCalendarAdapter.chooseStatusChange(config.finishTime, position)
                config.finishTime = itemDay.millis
                onSelectTimeChangeListener?.onTimeChange(config.startTime, config.finishTime)
                return@setOnItemClickListener
            }
            // 本次点击距离开始近  还是结束近
            val b = (itemDay.millis - startTime.timeInMillis).compareTo(finishTime.timeInMillis - itemDay.millis)
            if (b < 0) {
                intervalCalendarAdapter.chooseStatusChange(config.startTime, position)
                config.startTime = itemDay.millis
            } else {
                //刷新
                intervalCalendarAdapter.chooseStatusChange(position, config.finishTime)
                config.finishTime = itemDay.millis
            }
            // 点击
            onSelectTimeChangeListener?.onTimeChange(config.startTime, config.finishTime)
        }
    }

    class EveryDaySource(val config: Config) : PagingSource<String, EveryDay>() {
        private val ZERO_TIME = "/01 00:00"
        private val FORMAT_YEAR_MOON_DAY = "yyyy/MM/dd HH:mm"
        private val simpleDateFormat = SimpleDateFormat(
            FORMAT_YEAR_MOON_DAY,
            Locale.CHINA
        )

        override fun getRefreshKey(state: PagingState<String, EveryDay>): String? {
            val anchorPosition = state.anchorPosition ?: return null
            var preOrNext = false
            var key = state.closestPageToPosition(anchorPosition)?.prevKey
            if (key.isNullOrEmpty()) {
                key = state.closestPageToPosition(anchorPosition)?.nextKey
                preOrNext = true
            }
            if (key.isNullOrEmpty()) {
                return null
            }
            val format = String.format("%s%s", key, ZERO_TIME)

            val parse = simpleDateFormat.parse(format)

            val instance = Calendar.getInstance()
            instance.timeInMillis = parse.time

            instance.add(Calendar.MONTH, if (!preOrNext) 1 else -1)

            val format1 = String.format(
                "%s/%s", instance.get(Calendar.YEAR),
                instance.get(Calendar.MONTH) + 1
            )
            Log.i("EveryDaySource", String.format("getRefreshKey:%s", format1))
            return format1
        }

        override suspend fun load(params: LoadParams<String>): LoadResult<String, EveryDay> {
            val key = params.key!!
            val format = String.format("%s%s", key, ZERO_TIME)

            val parse = simpleDateFormat.parse(format)

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = parse.time
            calendar.add(Calendar.MONTH, 1)
            val nextKey = if (calendar.timeInMillis > config.maxTime) {
                null
            } else {
                String.format("%s/%s", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
            }

            calendar.timeInMillis = parse.time
            calendar.add(Calendar.MONTH, -1)
            val prevKey = if (calendar.timeInMillis < config.minTime) {
                null
            } else {
                String.format("%s/%s", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
            }


            val data = getMonth(parse.time)

            Log.i(
                "EveryDaySource",
                String.format(
                    "prevKey:%s key:%s nextKey:%s  data:%s",
                    prevKey, key, nextKey, data.size
                )
            )

            return LoadResult.Page(
                data = data,
                nextKey = nextKey,
                prevKey = prevKey,
            )
        }

        /**
         *  time = 2022/04
         *  time = 2022/04/01 00:00
         */
        private fun getMonth(time: Long): MutableList<EveryDay> {
            val mutableListOf = mutableListOf<EveryDay>()

            val gc = GregorianCalendar()
            gc.timeInMillis = time

            // 设置月份的起始日期为1
            gc.set(Calendar.DAY_OF_MONTH, 1)

            println(simpleDateFormat.format(time))
            // 标题
            mutableListOf.add(EveryDay().also {
                it.viewType = TITLE_ITEM_TYPE
                it.millis = gc.timeInMillis
                it.text = simpleDateFormat.format(time)
            })
            // 获取一周中的第几天 表示从周几开始本月的第一天
            val weekday = gc.get(Calendar.DAY_OF_WEEK)
            for (i in 1 until weekday) {
                mutableListOf.add(EveryDay().also {
                    it.viewType = PLACEHOLDER_ITEM_TYPE
                })
            }
            val startTime = Calendar.getInstance()
            startTime.timeInMillis = config.startTime

            val finishTime = Calendar.getInstance()
            finishTime.timeInMillis = config.finishTime

            val minLimit = Calendar.getInstance()
            minLimit.timeInMillis = config.minLimit

            val maxLimit = Calendar.getInstance()
            maxLimit.timeInMillis = config.maxLimit


            // 现在属于第几个月
            val month = gc.get(Calendar.MONTH)
            do {
                // 获取当天在本月中 在第几天
                val day = gc.get(Calendar.DAY_OF_MONTH)
                mutableListOf.add(EveryDay().also {
                    it.text = day.toString()
                    it.millis = gc.timeInMillis
                    it.viewType = ORDINARY_ITEM_TYPE
                    it.selectedState = itemStatus(config, gc)
                })
                println(day)
                gc.add(Calendar.DAY_OF_MONTH, 1)// 天数加1
            } while (gc.get(Calendar.MONTH) == month)
            return mutableListOf
        }

        /**
         *
         */

    }

    public class Config {
        // 中心时间 一般是本月
        var timeCenter: Long = System.currentTimeMillis()

        // 日历最大最小 间隔月
        var min: Int = 3
        var max: Int = 3

        // 选中的开始时间
        var startTime: Long = -1
            set(value) {
                field = value
                (startTimeCache ?: Calendar.getInstance()).timeInMillis = value
            }
        var finishTime: Long = -1
            set(value) {
                field = value
                (finishTimeCache ?: Calendar.getInstance()).timeInMillis = value
            }

        // 最小 最大预加载时间 按月计算
        var minTime: Long = 0
        var maxTime: Long = 0

        // 不可选边界
        var minLimit: Long = -1
        var maxLimit: Long = -1

        // 重复点击是否可以取消 true 是
        var isCancel = false
        var startPosition: Int = -1
        var finishPosition: Int = -1

        var startTimeCache: Calendar? = null
            get() {
                return (field ?: Calendar.getInstance()).also {
                    it.timeInMillis = startTime
                }
            }

        var finishTimeCache: Calendar? = null
            get() {
                return (field ?: Calendar.getInstance()).also {
                    it.timeInMillis = finishTime
                }
            }
        var minLimitCache: Calendar? = null
            get() {
                return (field ?: Calendar.getInstance()).also {
                    it.timeInMillis = minLimit
                }
            }

        var maxLimitCache: Calendar? = null
            get() {
                return (field ?: Calendar.getInstance()).also {
                    it.timeInMillis = maxLimit
                }
            }
    }


}
