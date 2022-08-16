package org.daimhim.widget.ic

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

abstract class EarlyAdapter<T : Any, VH : RecyclerView.ViewHolder> : ListAdapter<T, VH> {

    private var prefetchDistance: Int = 5
    private var workerDispatcher: CoroutineDispatcher
    var dataSource : DataSource<T>? = null

    constructor(
        prefetchDistance: Int,
        diffCallback: DiffUtil.ItemCallback<T>,
        workerDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : super(diffCallback) {
        this.prefetchDistance = prefetchDistance
        this.workerDispatcher = workerDispatcher
    }

    constructor(
        prefetchDistance: Int,
        asyncDifferConfig: AsyncDifferConfig<T>,
        workerDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : super(asyncDifferConfig) {
        this.prefetchDistance = prefetchDistance
        this.workerDispatcher = workerDispatcher
    }

    private var isLoading = false
    @OptIn(DelicateCoroutinesApi::class)
    fun advance(position: Int) {
//        Log.i("V2EveryDaySource",String.format("advance %s",position))
        if (isLoading){
            return
        }
        if (position < prefetchDistance) {
            isLoading = true
            //向前
            GlobalScope
                .launch(context = workerDispatcher){
                    dataSource?.load(DataSource.Prepend,getItem(0))
                    isLoading = false
                }
        }else if (itemCount - position < prefetchDistance){
            isLoading = true
            GlobalScope
                .launch(context = workerDispatcher){
                    dataSource?.load(DataSource.Append,getItem(itemCount-1))
                    isLoading = false
                }
        }
    }

    interface DataSource<Key : Any> {
        companion object{
            val Refresh = 0
            val Append = 1
            val Prepend = 2
        }
        fun getRefresh(key : Key)
        suspend fun load(loadParams:Int, key : Key)
    }
}