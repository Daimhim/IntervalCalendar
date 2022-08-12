package org.daimhim.widget.ic;

import androidx.paging.PagingSource;
import androidx.paging.PagingState;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntervalCalendarProvider {
    static class EveryDaySource extends PagingSource<String, EveryDay>{

        @Nullable
        @Override
        public String getRefreshKey(@NotNull PagingState<String, EveryDay> pagingState) {
            return null;
        }

        @Nullable
        @Override
        public Object load(@NotNull LoadParams<String> loadParams, @NotNull Continuation<? super LoadResult<String, EveryDay>> continuation) {
            return null;
        }
    }
}
