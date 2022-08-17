package org.daimhim.widget.ic;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kotlinx.coroutines.Dispatchers;

public class IntervalCalendarAdapter
        extends EarlyAdapter<EveryDay,
        IntervalCalendarAdapter.EveryDayViewHolder> {
    private OnItemClickListener onItemClickListener;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private EveryDayFactory everyDayFactory = new EveryDayFactory() {
    };
    private long selectStartTime = -1;

    private long selectFinishTime = -1;


    private long upperLimit = -1L;
    private long lowerLimit = -1L;

    public IntervalCalendarAdapter(
            int prefetchDistance) {
        super(prefetchDistance, new DiffUtil.ItemCallback<EveryDay>() {
            @Override
            public boolean areItemsTheSame(@NonNull EveryDay oldItem, @NonNull EveryDay newItem) {
                return oldItem.millis == newItem.millis;
            }

            @Override
            public boolean areContentsTheSame(@NonNull EveryDay oldItem, @NonNull EveryDay newItem) {
                if (oldItem.viewType == newItem.viewType) {
                    return true;
                }
                return oldItem.year == newItem.year
                        && oldItem.moon == newItem.moon
                        && oldItem.day == newItem.day
                        && oldItem.hour == newItem.hour
                        && oldItem.minute == newItem.minute
                        && oldItem.second == newItem.second;
            }
        }, Dispatchers.getIO());
    }


    @NonNull
    @Override
    public EveryDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        EveryDayViewHolder everyDayViewHolder = everyDayFactory.create(parent, viewType);
        everyDayViewHolder.setOnItemClickListener(onItemClickListener);
        return everyDayViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull EveryDayViewHolder holder, int position) {
        EveryDay item = getItem(position);
        int selectedState = getSelectedState(item);
        everyDayFactory.bind(holder, item, selectedState);
        holder.bindListener(holder.itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EveryDayViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        EveryDay item = getItem(position);
        int selectedState = getSelectedState(item);
//        Log.i("IntervalCalendarAdapter",String.format("onBindViewHolder %s selectedState %s %s",position,selectedState,payloads.size()));
        everyDayFactory.bind(holder, item, selectedState, payloads);
    }

    /**
     * 动态获取选中状态
     *
     * @param item
     * @return
     */
    private int getSelectedState(EveryDay item) {
        int selectedState = -1;
        if (!item.enable) {
            // 不可选
            selectedState = IntervalCalendar.UNSATISFACTORY_STATE;
        } else if (selectStartTime > 0
                && selectFinishTime > 0
                && item.millis > selectStartTime
                && item.millis < selectFinishTime) {
            // 选中区间
            selectedState = IntervalCalendar.SELECT_STATE;
        } else if (selectStartTime > 0
                && selectStartTime == item.millis) {
            // 选中开始
            selectedState = IntervalCalendar.START_SELECT_STATE;
        } else if (selectFinishTime > 0
                && selectFinishTime == item.millis) {
            // 选中结束
            selectedState = IntervalCalendar.END_SELECT_STATE;
        } else {
            // 未选中状态
            selectedState = IntervalCalendar.UNSELECTED_STATE;
        }
        return selectedState;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void chooseStatusChange(int startPosition, Long endTime) {
        Integer endPosition = -1;
        if (getItemDay(getItemCount() - 1).millis > endTime) {
            endPosition = getItemCount() - 1;
        } else {
            endPosition = recursionBinarySearch(endTime);
        }
//        Log.i("chooseStatusChange", String.format("startPosition %s endPosition %s",
//                startPosition, endPosition));
        if (startPosition > -1 && endPosition.compareTo(-1) > 0) {
            notifyItemRangeChanged(startPosition, endPosition - startPosition + 1, 0);
        } else if (startPosition > -1) {
            notifyItemChanged(startPosition, 0);
        }
    }

    public void chooseStatusChange(Long startTime, int endPosition) {
        Integer startPosition = -1;
        if (getItemDay(0).millis > startTime) {
            startPosition = 0;
        } else {
            startPosition = recursionBinarySearch(startTime);
        }

//        Log.i("chooseStatusChange", String.format("startPosition %s endPosition %s",
//                startPosition, endPosition));
        if (endPosition > -1 && startPosition.compareTo(-1) > 0) {
            notifyItemRangeChanged(startPosition, endPosition - startPosition + 1, 0);
        } else if (endPosition > -1) {
            notifyItemChanged(endPosition, 0);
        }
    }

    public void chooseStatusChange(Long startTime, Long endTime) {
        Future<Integer> startFuture = executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (getItemDay(0).millis > startTime) {
                    return 0;
                }
                return recursionBinarySearch(startTime);
            }
        });
        Future<Integer> endFuture = executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (getItemDay(getItemCount() - 1).millis > endTime) {
                    return getItemCount() - 1;
                }
                return recursionBinarySearch(endTime);
            }
        });

        try {
            Integer startPosition = startFuture.get();
            Integer endPosition = endFuture.get();
//            Log.i("chooseStatusChange", String.format("startPosition %s endPosition %s",
//                    startPosition, endPosition));
            if (startPosition.compareTo(-1) > 0 && endPosition.compareTo(-1) > 0) {
                notifyItemRangeChanged(startPosition, endPosition - startPosition + 1, 0);
            } else if (startPosition.compareTo(-1) > 0) {
                notifyItemChanged(startPosition, 0);
            } else if (endPosition.compareTo(-1) > 0) {
                notifyItemChanged(endPosition, 0);
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int recursionBinarySearch(long key) {
        if (key == -1) {
            return -1;
        }
        int low = 0;
        int high = getItemCount() - 1;
        int mid = 0;
        int sameDayEx;
        EveryDay item = null;
        while (low <= high) {
            low = correction(low, 1);
            high = correction(high, -1);
            if (low > high) {
                return -1;
            } else if (low == high) {
                item = getItem(low);
                if (item == null) {
                    return -1;
                }
                if (item.millis == key) {
                    return low;
                } else {
                    return -1;
                }
            }
            mid = low + (high - low) / 2;
            mid = correction(mid, 1);
            item = getItem(mid);
            if (item == null) {
                return -1;
            }
            sameDayEx = Long.compare(item.millis, key);
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

    private int correction(int tag, int offset) {
        int reference = tag;
        while (!effective(reference)) {
            reference += offset;
        }
        return reference;
    }

    private boolean effective(int tag) {
        EveryDay item = getItem(tag);
        if (item == null) {
            return false;
        }
        return item.enable;
    }


    public EveryDay getItemDay(int position) {
        return getItem(position);
    }

    @Override
    public int getItemViewType(int position) {
//        Log.e("EveryDaySource","getItemViewType " + position);
        return getItemDay(position).viewType;
    }

    public void setEveryDayFactory(EveryDayFactory everyDayFactory) {
        this.everyDayFactory = everyDayFactory;
    }

    public interface EveryDayFactory<VH extends EveryDayViewHolder> {
        default VH create(ViewGroup parent, int viewType) {
            FrameLayout frameLayout = new FrameLayout(parent.getContext());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            frameLayout.setLayoutParams(layoutParams);

            TextView textView = new TextView(parent.getContext());
            layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(layoutParams);
            textView.setTextColor(Color.BLACK);
            textView.setId(R.id.def_date_item);
            textView.setGravity(Gravity.CENTER);

            frameLayout.addView(textView);
            return (VH)new EveryDayViewHolder(frameLayout);
        }

        default void bind(VH holder, EveryDay everyDay, int selectedState) {
            StringBuilder stringBuilder = new StringBuilder();
            switch (everyDay.viewType) {
                case V2EveryDaySource.TITLE_ITEM_TYPE:
                    stringBuilder.append(
                            String.format("%s/%s/%s %s/%s/%s",
                                    everyDay.year, everyDay.moon, everyDay.day,
                                    everyDay.hour, everyDay.minute, everyDay.second)
                    );
                    break;
                case V2EveryDaySource.PLACEHOLDER_ITEM_TYPE:
                    break;
                default:
                    stringBuilder.append(everyDay.day);
                    break;
            }
            switch (selectedState) {
                case IntervalCalendar.START_SELECT_STATE:
                    //选中开始
                    stringBuilder.append("始");
                    break;
                case IntervalCalendar.END_SELECT_STATE:
                    //选中结束
                    stringBuilder.append("末");
                    break;
                case IntervalCalendar.SELECT_STATE:
                    //选中
                    stringBuilder.append("选");
                    break;
                case IntervalCalendar.UNSATISFACTORY_STATE:
                    //不可选
                    stringBuilder.append("禁");
                    break;
                default:
                    //未选中状态
                    stringBuilder.append("待");
                    break;
            }
            TextView textView = holder.itemView.findViewById(R.id.def_date_item);
            textView.setText(stringBuilder);
        }

        default void bind(VH holder, EveryDay everyDay, int selectedState, List<Object> payloads) {
            StringBuilder stringBuilder = new StringBuilder();
            switch (everyDay.viewType) {
                case V2EveryDaySource.TITLE_ITEM_TYPE:
                    stringBuilder.append(
                            String.format("%s/%s/%s %s/%s/%s",
                                    everyDay.year, everyDay.moon, everyDay.day,
                                    everyDay.hour, everyDay.minute, everyDay.second)
                    );
                    break;
                case V2EveryDaySource.PLACEHOLDER_ITEM_TYPE:
                    break;
                default:
                    stringBuilder.append(everyDay.day);
                    break;
            }
            switch (selectedState) {
                case IntervalCalendar.START_SELECT_STATE:
                    //选中开始
                    stringBuilder.append("始");
                    break;
                case IntervalCalendar.END_SELECT_STATE:
                    //选中结束
                    stringBuilder.append("末");
                    break;
                case IntervalCalendar.SELECT_STATE:
                    //选中
                    stringBuilder.append("选");
                    break;
                case IntervalCalendar.UNSATISFACTORY_STATE:
                    //不可选
                    stringBuilder.append("禁");
                    break;
                default:
                    //未选中状态
                    stringBuilder.append("待");
                    break;
            }
            TextView textView = holder.itemView.findViewById(R.id.def_date_item);
            textView.setText(stringBuilder);
        }
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

    public long getSelectStartTime() {
        return selectStartTime;
    }

    public void setSelectStartTime(long selectStartTime) {
        this.selectStartTime = selectStartTime;
    }


    public long getSelectFinishTime() {
        return selectFinishTime;
    }

    public void setSelectFinishTime(long selectFinishTime) {
        this.selectFinishTime = selectFinishTime;
    }

    public static class EveryDayViewHolder extends RecyclerView.ViewHolder {
        private View.OnClickListener onClickListener;
        private SoftReference<OnItemClickListener> onItemClickListener;

        public EveryDayViewHolder(@NonNull @NotNull ViewGroup itemView) {
            super(itemView);
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = new SoftReference<>(onItemClickListener);
        }

        public void bindListener(View view) {
            if (onClickListener == null) {
                onClickListener = new View.OnClickListener() {
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
                                IntervalCalendarAdapter.EveryDayViewHolder.this,
                                v,
                                getBindingAdapterPosition());
                    }
                };
            }
            view.setOnClickListener(onClickListener);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(RecyclerView.ViewHolder viewHolder, View view, int position);
    }

}
