package org.daimhim.widget.ic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.daimhim.widget.ic.IntervalCalendarController.*;

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
    private SparseArray<EveryDayViewHolderFactory> factorySparseArray;

    // 可以设置默认选中时间
    // 可以设置选中时间的区间
    // 是否可以取消选中
    // 绘制开始、绘制结束、绘制选中、绘制未选中、绘制不可选
    private RecyclerView recyclerView;
    private IntervalCalendarAdapter intervalCalendarAdapter;

    private IntervalCalendarController intervalCalendarController;

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
    }


    private void initView(Context context) {
        factorySparseArray = new SparseArray<>();
        factorySparseArray.put(ORDINARY_ITEM_TYPE, new EveryDayViewHolderFactoryImpl(ORDINARY_ITEM_TYPE));
        factorySparseArray.put(PLACEHOLDER_ITEM_TYPE, new EveryDayViewHolderFactoryImpl(PLACEHOLDER_ITEM_TYPE));
        factorySparseArray.put(TITLE_ITEM_TYPE, new EveryDayViewHolderFactoryImpl(TITLE_ITEM_TYPE));

        View inflate = inflate(context, R.layout.interval_calendar_layout, null);
        recyclerView = inflate.findViewById(R.id.recyclerView);
        addView(inflate);

        intervalCalendarAdapter = new IntervalCalendarAdapter(factorySparseArray);
        recyclerView.setAdapter(intervalCalendarAdapter);
        DividerItemDecoration decor = new DividerItemDecoration(context, GridLayoutManager.VERTICAL);
        decor.setDrawable(ContextCompat.getDrawable(context,R.drawable.inset_recyclerview_divider));
        recyclerView.addItemDecoration(decor);
        intervalCalendarController = new IntervalCalendarController(intervalCalendarAdapter);
        Log.i("IntervalCalendar tag ",context.getClass().getName());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addViewHolderFactory(int viewType, EveryDayViewHolderFactory factory) {
        factorySparseArray.put(viewType, factory);
        intervalCalendarAdapter.notifyDataSetChanged();
    }

    public static class EveryDay {
        public String text;
        protected int viewType;
        public long millis = -1;
        public int selectedState = 0;
    }

    public static class IntervalCalendarAdapter
            extends PagingDataAdapter<EveryDay, IntervalCalendarAdapter.EveryDayViewHolder> {
        private OnItemClickListener onItemClickListener;
        private SparseArray<EveryDayViewHolderFactory> factorySparseArray;
        private Config config;
        public IntervalCalendarAdapter(SparseArray<EveryDayViewHolderFactory> factorySparseArray) {
            super(new DiffUtil.ItemCallback<EveryDay>() {
                @Override
                public boolean areItemsTheSame(@NonNull @NotNull IntervalCalendar.EveryDay oldItem,
                                               @NonNull @NotNull IntervalCalendar.EveryDay newItem) {
                    return oldItem.millis == newItem.millis;
                }

                @Override
                public boolean areContentsTheSame(@NonNull @NotNull IntervalCalendar.EveryDay oldItem,
                                                  @NonNull @NotNull IntervalCalendar.EveryDay newItem) {
                    return isSameDayEx(oldItem.millis, newItem.millis) == 0;
                }

            });
            this.factorySparseArray = factorySparseArray;
        }

        @NonNull
        @NotNull
        @Override
        public EveryDayViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            Log.d("IntervalCalendarAdapter",String.format("viewType:%s",viewType));
            EveryDayViewHolder crate = factorySparseArray.get(viewType).crate(parent);
            crate.setOnItemClickListener(onItemClickListener);
            return crate;
        }

        @Override
        public int getItemViewType(int position) {
            EveryDay item = getItem(position);
            if (item == null){
                return PLACEHOLDER_ITEM_TYPE;
            }
            return item.viewType;
        }
        public EveryDay getItemDay(int position){
            return getItem(position);
        }
        @Override
        public void onAttachedToRecyclerView(@NonNull @NotNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager){
                ((GridLayoutManager)layoutManager)
                        .setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                            @Override
                            public int getSpanSize(int position) {
                                EveryDay item = getItem(position);
                                if (item != null
                                        && item.viewType == TITLE_ITEM_TYPE){
                                    return 7;
                                }
                                return 1;
                            }
                        });
            }
        }

        @Override
        public void onBindViewHolder(
                @NonNull @NotNull IntervalCalendar.IntervalCalendarAdapter.EveryDayViewHolder holder,
                int position) {
            EveryDay item = getItem(position);
            if (item == null) {
                return;
            }
            Calendar instance = Calendar.getInstance();
            instance.setTimeInMillis(item.millis);
            item.selectedState = itemStatus(config,instance);
            holder.onBindData(item);
            holder.bindListener(holder.itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull IntervalCalendar.IntervalCalendarAdapter.EveryDayViewHolder holder, int position, @NonNull @NotNull List<Object> payloads) {
            if (payloads.isEmpty()){
                super.onBindViewHolder(holder, position, payloads);
            }else {
                EveryDay item = getItem(position);
                if (item == null) {
                    return;
                }
                Calendar instance = Calendar.getInstance();
                instance.setTimeInMillis(item.millis);
                item.selectedState = itemStatus(config,instance);
                holder.onBindData(item);
                holder.bindListener(holder.itemView);
            }
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        public void chooseStatusChange(int startPosition,Long endTime){
            Integer endPosition = recursionBinarySearch(endTime);
            Log.i("chooseStatusChange",String.format("startPosition %s endPosition %s",
                    startPosition,endPosition));
            if (startPosition > -1 && endPosition.compareTo(-1) > 0){
                notifyItemRangeChanged(startPosition,endPosition-startPosition+1,0);
            }else if (startPosition > -1){
                notifyItemChanged(startPosition,0);
            }
        }
        public void chooseStatusChange(Long startTime,int endPosition){
            Integer startPosition = recursionBinarySearch(startTime);
            Log.i("chooseStatusChange",String.format("startPosition %s endPosition %s",
                    startPosition,endPosition));
            if (endPosition > -1 && startPosition.compareTo(-1) > 0){
                notifyItemRangeChanged(startPosition,endPosition-startPosition+1,0);
            }else if (endPosition > -1){
                notifyItemChanged(endPosition,0);
            }
        }
        private final ExecutorService executorService = Executors.newCachedThreadPool();
        public void chooseStatusChange(Long startTime,Long endTime){
            Future<Integer> startFuture = executorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return recursionBinarySearch(startTime);
                }
            });
            Future<Integer> endFuture = executorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return recursionBinarySearch(endTime);
                }
            });

            try {
                Integer startPosition = startFuture.get();
                Integer endPosition = endFuture.get();
                Log.i("chooseStatusChange",String.format("startPosition %s endPosition %s",
                        startPosition,endPosition));
                if (startPosition.compareTo(-1) > 0 && endPosition.compareTo(-1) > 0){
                    notifyItemRangeChanged(startPosition,endPosition-startPosition+1,0);
                }else if (startPosition.compareTo(-1) > 0){
                    notifyItemChanged(startPosition,0);
                }else if (endPosition.compareTo(-1) > 0){
                    notifyItemChanged(endPosition,0);
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        private int recursionBinarySearch(long key){
            if (key == -1){
                return -1;
            }
            int low = 0;
            int high = getItemCount() - 1;
            int mid = 0;
            int sameDayEx;
            EveryDay item = null;
            while (low <= high){
                low = correction(low,1);
                high = correction(high,-1);
                if (low > high){
                    return -1;
                }else if (low == high){
                    item = getItem(low);
                    if (item == null){
                        return -1;
                    }
                     if (isSameDayEx(item.millis, key) == 0){
                         return low;
                    }else{
                         return -1;
                    }
                }
                mid = low + (high - low) / 2;
                mid = correction(mid,1);
                item = getItem(mid);
                if (item == null){
                    return -1;
                }
                sameDayEx = isSameDayEx(item.millis, key);
                item = null;
                if (sameDayEx > 0)
                    high = mid - 1;
                else if (sameDayEx < 0)
                    low = mid + 1;
                else
                    return mid;
            }
            return -1;
        }

        private int correction(int tag,int offset){
            int reference = tag;
            while (!effective(reference)){
                reference += offset;
            }
            return reference;
        }
        private boolean effective(int tag){
            EveryDay item = getItem(tag);
            if (item == null){
                return false;
            }
            return effective(item);
        }

        /**
         * 是否是有效的
         * @param item
         * @return true 有效的
         */
        private boolean effective(EveryDay item){
            return item.viewType != TITLE_ITEM_TYPE
                    && item.selectedState != UNSATISFACTORY_STATE
                    && item.viewType != PLACEHOLDER_ITEM_TYPE;
        }

        public static class EveryDayViewHolder extends RecyclerView.ViewHolder {
            private View.OnClickListener onClickListener;
            private SoftReference<OnItemClickListener> onItemClickListener;

            public EveryDayViewHolder(@NonNull @NotNull View itemView) {
                super(itemView);
            }

            public void onBindData(EveryDay everyDay) {
                TextView textView = (TextView) itemView;
                textView.setText(everyDay.text);
                switch (everyDay.selectedState) {
                    case START_SELECT_STATE:
                        textView.setGravity(Gravity.CENTER);
                        textView.setBackgroundResource(R.drawable.red_round_box_left_4);
                        textView.setTextColor(Color.WHITE);
                        break;
                    case END_SELECT_STATE:
                        textView.setGravity(Gravity.CENTER);
                        textView.setBackgroundResource(R.drawable.red_round_box_right_4);
                        textView.setTextColor(Color.WHITE);
                        break;
                    case UNSELECTED_STATE:
                        textView.setGravity(Gravity.CENTER);
                        textView.setBackgroundColor(Color.WHITE);
                        textView.setTextColor(Color.BLACK);
                        break;
                    case SELECT_STATE:
                        textView.setGravity(Gravity.CENTER);
                        textView.setBackgroundColor(Color.parseColor("#FDEBEA"));
                        textView.setTextColor(Color.BLACK);
                        break;
                    case UNSATISFACTORY_STATE:
                        textView.setGravity(Gravity.CENTER);
                        textView.setBackgroundColor(Color.WHITE);
                        textView.setTextColor(Color.parseColor("#969696"));
                        break;
                }
            }

            public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
                this.onItemClickListener = new SoftReference<>(onItemClickListener);
            }

            public void bindListener(View view) {
                if (onClickListener == null) {
                    onClickListener = new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (onItemClickListener == null) {
                                return;
                            }
                            OnItemClickListener itemClickListener = onItemClickListener.get();
                            if (itemClickListener == null) {
                                return;
                            }
                            itemClickListener.onItemClick(
                                    EveryDayViewHolder.this,
                                    v,
                                    getBindingAdapterPosition());
                        }
                    };
                }
                view.setOnClickListener(onClickListener);
            }
        }
    }
        public static class PlaceholderViewHolder extends IntervalCalendarAdapter.EveryDayViewHolder {

            public PlaceholderViewHolder(@NonNull @NotNull View itemView) {
                super(itemView);
            }

            @Override
            public void onBindData(EveryDay everyDay) {

            }
        }
        public static class TitleEveryDayViewHolder extends IntervalCalendarAdapter.EveryDayViewHolder {
            public TitleEveryDayViewHolder(@NonNull @NotNull View itemView) {
                super(itemView);
            }
            @Override
            public void onBindData(EveryDay everyDay) {
                TextView textView = (TextView) itemView;
                textView.setText(everyDay.text);
            }
        }


    public interface OnItemClickListener {
        void onItemClick(RecyclerView.ViewHolder viewHolder, View view, int position);
    }

    public static interface EveryDayViewHolderFactory {
        IntervalCalendarAdapter.EveryDayViewHolder crate(ViewGroup parent);
    }

    public static class EveryDayViewHolderFactoryImpl implements EveryDayViewHolderFactory {
        private int viewType = 0;

        public EveryDayViewHolderFactoryImpl(int viewType) {
            this.viewType = viewType;
        }

        @Override
        public IntervalCalendarAdapter.EveryDayViewHolder crate(ViewGroup parent) {
            IntervalCalendarAdapter.EveryDayViewHolder viewHolder = null;
            Context context = parent.getContext();
            TextView textView = new TextView(context);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    dp2px(context, 50F));
            textView.setLayoutParams(layoutParams);
            switch (viewType) {
                case PLACEHOLDER_ITEM_TYPE:
                    textView.setGravity(Gravity.CENTER);
                    textView.setBackgroundColor(Color.WHITE);
                    viewHolder = new PlaceholderViewHolder(textView);
                    break;
                case ORDINARY_ITEM_TYPE:
                    textView.setGravity(Gravity.CENTER);
                    textView.setBackgroundColor(Color.WHITE);
                    textView.setTextColor(Color.BLACK);
                    viewHolder = new IntervalCalendarAdapter.EveryDayViewHolder(textView);
                    break;
                case TITLE_ITEM_TYPE: {
                    textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    textView.setBackgroundColor(Color.WHITE);
                    textView.setTextColor(Color.BLACK);
                    viewHolder = new TitleEveryDayViewHolder(textView);
                    break;
                }
            }
            return viewHolder;
        }

        // 方法2
        public int dp2px(Context ctx, float dp) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
        }
    }

    public void setLifecycle(LifecycleOwner lifecycle,Config config) {
        Log.i("IntervalCalendar set","111111setLifecycle111" + lifecycle.toString());
        intervalCalendarController.setLifecycle(lifecycle);
        if (config == null){
            config = new Config();
        }
        intervalCalendarController.setConfig(config);
        intervalCalendarAdapter.config = config;
        intervalCalendarController.init();
    }

    public void setOnSelectTimeChangeListener(OnSelectTimeChangeListener onSelectTimeChangeListener) {
        intervalCalendarController.setOnSelectTimeChangeListener(onSelectTimeChangeListener);
    }

    public long getStartTime(){
        return intervalCalendarController.getConfig().getStartTime();
    }
    public long getFinishTime(){
        return intervalCalendarController.getConfig().getFinishTime();
    }

    public interface OnSelectTimeChangeListener{
        void onTimeChange(long startTime,long endTime);
    }
}
