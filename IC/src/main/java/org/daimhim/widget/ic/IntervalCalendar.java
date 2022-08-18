package org.daimhim.widget.ic;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 日期自定义布局
 */
public class IntervalCalendar extends FrameLayout {
    /**
     * 选中开始
     */
    public static final int START_SELECT_STATE = 0;
    /**
     * 选中结束
     */
    public static final int END_SELECT_STATE = 1;
    /**
     * 选中
     */
    public static final int SELECT_STATE = 2;
    /**
     * 未选中状态
     */
    public static final int UNSELECTED_STATE = 3;
    /**
     * 不可选
     */
    public static final int UNSATISFACTORY_STATE = 4;
    private IntervalCalendarAdapter intervalCalendarAdapter;
    /**
     * 上限
     */
    private long upperLimit = -1L;
    /**
     * 下限
     */
    private long lowerLimit = -1L;
    /**
     * 初始
     */
    private long initialTime = -1L;
    /**
     * 最大缓存页数
     */
    private int maxCacheLimit = -1;
    /**
     * 初始时数量
     */
    private int initializationNumber = 5;
    private Lifecycle lifecycle;
    /**
     * 选择区间
     */
    private int maxInterval = -1;

    private LinearLayout ll_weekend_layout;

    private OnSelectTimeChangeListener onSelectTimeChangeListener;
    private RecyclerView recyclerView;

    public IntervalCalendar(@NonNull @NotNull Context context) {
        this(context, null);
    }

    public IntervalCalendar(@NonNull @NotNull Context context,
                            @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IntervalCalendar(@NonNull @NotNull Context context,
                            @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        initListener();
    }

    private void initView(Context context) {
        View inflate = inflate(context, R.layout.interval_calendar_layout, this);
        // 可以设置默认选中时间
        // 可以设置选中时间的区间
        // 是否可以取消选中
        // 绘制开始、绘制结束、绘制选中、绘制未选中、绘制不可选
        recyclerView = inflate.findViewById(R.id.recyclerView);
        ll_weekend_layout = inflate.findViewById(R.id.ll_weekend_layout);
        intervalCalendarAdapter = new IntervalCalendarAdapter(60);

        GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int itemViewType = intervalCalendarAdapter.getItemViewType(position);
                return itemViewType == V2EveryDaySource.TITLE_ITEM_TYPE ? 7 : 1;
            }
        });

        recyclerView.setAdapter(intervalCalendarAdapter);
        DividerItemDecoration decor = new DividerItemDecoration(context, GridLayoutManager.VERTICAL);
        decor.setDrawable(Objects.requireNonNull(ContextCompat
                .getDrawable(context, R.drawable.inset_recyclerview_divider)));
        recyclerView.addItemDecoration(decor);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                Log.i("V2EveryDaySource",String.format("onScrollStateChanged %s",newState));
//                if (newState == RecyclerView.SCROLL_STATE_IDLE)
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int position;
                if (dy < 0){
                    position = layoutManager.findFirstVisibleItemPosition();
                }else {
                    position = layoutManager.findLastVisibleItemPosition();
                }

//                Log.i("V2EveryDaySource",String.format("onScrolled %s",position));
                intervalCalendarAdapter.advance(position);
            }
        });
//        recyclerView.post(new Runnable() {
//            @Override
//            public void run() {
//                int i = intervalCalendarAdapter.recursionBinarySearch(initialTime);
//                recyclerView.scrollToPosition(i);
//            }
//        });
    }

    private void initListener() {
        intervalCalendarAdapter.setOnItemClickListener(new IntervalCalendarAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView.ViewHolder viewHolder, View view, int position) {
                EveryDay itemDay = intervalCalendarAdapter.getItemDay(position);
                if (itemDay == null || !itemDay.enable){
                    return;
                }
                long selectStartTime = intervalCalendarAdapter.getSelectStartTime();
                long selectFinishTime = intervalCalendarAdapter.getSelectFinishTime();
                // 从未选择开始时间
                if (selectStartTime < 0){
                    //刷新
                    intervalCalendarAdapter.setSelectStartTime(itemDay.millis);
                    intervalCalendarAdapter.notifyItemChanged(position,0);
                    if (onSelectTimeChangeListener != null){
                        onSelectTimeChangeListener.onTimeChange(
                                intervalCalendarAdapter.getSelectStartTime(),
                                intervalCalendarAdapter.getSelectFinishTime());
                    }
                    return;
                }

                // 两个日期相差超过限定
                if (maxInterval > 0 && maxInterval < (int) (Math.abs((itemDay.millis - selectStartTime)) / (1000*3600*24))){
                    if (onSelectTimeChangeListener != null) {
                        onSelectTimeChangeListener.chooseExceed();
                    }
                    return;
                }
                //当前选中时间与开始时间
                int startCompare = Long.compare(itemDay.millis, selectStartTime);

                // 从未选择结束时间
                if (selectFinishTime < 0){
                    //&& 当前点击小于开始
                    if (startCompare < 0) {
                        intervalCalendarAdapter.setSelectStartTime(itemDay.millis);
                        intervalCalendarAdapter.setSelectFinishTime(selectStartTime);
                        intervalCalendarAdapter.chooseStatusChange(itemDay.millis, selectStartTime);
                    }else {
                        //&& 当前点击大于或者等于开始
                        intervalCalendarAdapter.setSelectFinishTime(itemDay.millis);
                        intervalCalendarAdapter.chooseStatusChange(selectStartTime,position);
                    }
                    if (onSelectTimeChangeListener != null){
                        onSelectTimeChangeListener.onTimeChange(
                                intervalCalendarAdapter.getSelectStartTime(),
                                intervalCalendarAdapter.getSelectFinishTime());
                    }
                    return;
                }

                // 结束时间
                int endCompare = Long.compare(itemDay.millis, selectFinishTime);

                // 再次点击已经选中的区间内取消选中
                if (startCompare == 0 || endCompare == 0){
                    //刷新
                    intervalCalendarAdapter.setSelectStartTime(-1);
                    intervalCalendarAdapter.setSelectFinishTime(-1);
                    intervalCalendarAdapter.chooseStatusChange(selectStartTime,selectFinishTime);
                    if (onSelectTimeChangeListener != null){
                        onSelectTimeChangeListener.onTimeChange(
                                intervalCalendarAdapter.getSelectStartTime(),
                                intervalCalendarAdapter.getSelectFinishTime());
                    }
                    return;
                }
                intervalCalendarAdapter.chooseStatusChange(selectStartTime,selectFinishTime);
                intervalCalendarAdapter.setSelectStartTime(itemDay.millis);
                intervalCalendarAdapter.setSelectFinishTime(-1);
                intervalCalendarAdapter.chooseStatusChange(position,-1L);
                // 点击
                if (onSelectTimeChangeListener != null){
                    onSelectTimeChangeListener.onTimeChange(
                            intervalCalendarAdapter.getSelectStartTime(),
                            intervalCalendarAdapter.getSelectFinishTime());
                }
            }
        });
    }

    private void initData() {
        intervalCalendarAdapter.setUpperLimit(upperLimit);
        intervalCalendarAdapter.setLowerLimit(lowerLimit);
        V2EveryDaySource v2EveryDaySource = new V2EveryDaySource(
                upperLimit,
                lowerLimit,
                maxCacheLimit,
                initializationNumber
        );
        if (initialTime < 0) {
            initialTime = System.currentTimeMillis();
        }
        v2EveryDaySource.bindAdapter(
                lifecycle,
                initialTime,
                intervalCalendarAdapter
        );
        v2EveryDaySource
                .setInitNotify(new ValueCallback<Integer>() {
                    @Override
                    public void onReceiveValue(Integer value) {
                        if (value == 0){
                            return;
                        }
                        v2EveryDaySource.setInitNotify(null);
                        int i = intervalCalendarAdapter.recursionBinarySearch(initialTime);
                        recyclerView.scrollToPosition(i);
                        Log.i("V2EveryDaySource","scrollToPosition "+ i);
                    }
                });
    }


    public void setOnSelectTimeChangeListener(OnSelectTimeChangeListener onSelectTimeChangeListener) {
        this.onSelectTimeChangeListener = onSelectTimeChangeListener;
    }

    public long getUpperLimit() {
        return upperLimit;
    }

    public void setUpperLimit(long upperLimit) {
        this.upperLimit = upperLimit;
    }

    public long getLowerLimit() {
        return lowerLimit;
    }

    public void setLowerLimit(long lowerLimit) {
        this.lowerLimit = lowerLimit;
    }

    public long getInitialTime() {
        return initialTime;
    }

    public void setInitialTime(long initialTime) {
        this.initialTime = initialTime;
    }

    public int getMaxCacheLimit() {
        return maxCacheLimit;
    }

    public void setMaxCacheLimit(int maxCacheLimit) {
        this.maxCacheLimit = maxCacheLimit;
    }

    public int getInitializationNumber() {
        return initializationNumber;
    }

    public void setInitializationNumber(int initializationNumber) {
        this.initializationNumber = initializationNumber;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setSelectFinishTime(long selectFinishTime) {
        intervalCalendarAdapter.setSelectFinishTime(selectFinishTime);
    }

    public void setSelectStartTime(long selectStartTime) {
        intervalCalendarAdapter.setSelectStartTime(selectStartTime);
    }

    public int getMaxInterval() {
        return maxInterval;
    }

    public void setWeekColor(@ColorInt int color){
        int childCount = ll_weekend_layout.getChildCount();
        TextView childAt = null;
        for (int i = 0; i < childCount; i++) {
            childAt = (TextView) ll_weekend_layout.getChildAt(i);
            childAt.setTextColor(color);
        }
    }

    public void setWeekSize(int unit, float size){
        int childCount = ll_weekend_layout.getChildCount();
        TextView childAt = null;
        for (int i = 0; i < childCount; i++) {
            childAt = (TextView) ll_weekend_layout.getChildAt(i);
            childAt.setTextSize(unit, size);
        }
    }

    public void setWeekSize(float size){
        setWeekSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setWeekPadding(int left, int top, int right, int bottom){
        int childCount = ll_weekend_layout.getChildCount();
        TextView childAt = null;
        for (int i = 0; i < childCount; i++) {
            childAt = (TextView) ll_weekend_layout.getChildAt(i);
            childAt.setPadding(left, top, right, bottom);
        }
    }

    public void setMaxInterval(int maxInterval) {
        this.maxInterval = maxInterval;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        initData();
    }

    public IntervalCalendarAdapter getIntervalCalendarAdapter() {
        return intervalCalendarAdapter;
    }

    public void setEveryDayFactory(IntervalCalendarAdapter.EveryDayFactory everyDayFactory) {
        intervalCalendarAdapter.setEveryDayFactory(everyDayFactory);
    }

    public void clearSelect(){
        long selectStartTime = intervalCalendarAdapter.getSelectStartTime();
        long selectFinishTime = intervalCalendarAdapter.getSelectFinishTime();
        intervalCalendarAdapter.setSelectStartTime(-1);
        intervalCalendarAdapter.setSelectFinishTime(-1);
        intervalCalendarAdapter.chooseStatusChange(selectStartTime,selectFinishTime);
        if (onSelectTimeChangeListener != null){
            onSelectTimeChangeListener.onTimeChange(
                    intervalCalendarAdapter.getSelectStartTime(),
                    intervalCalendarAdapter.getSelectFinishTime());
        }
    }

    public interface OnSelectTimeChangeListener {
        void onTimeChange(long startTime, long endTime);
        void chooseExceed();
    }
}
