package org.daimhim.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.daimhim.widget.ic.IntervalCalendar
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var ic: IntervalCalendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ic = findViewById(R.id.ic_calendar)


//        val config: IntervalCalendarController.Config = IntervalCalendarController.Config()
//        val instance = Calendar.getInstance()
//        instance.timeInMillis = System.currentTimeMillis()
//        instance.add(Calendar.MONTH, 3)
//        ic.upperLimit = System.currentTimeMillis()
//        ic.lowerLimit = instance.timeInMillis
        ic.maxCacheLimit = 12
        ic.initializationNumber = 5
        ic.maxInterval = 30

        ic.lifecycle = lifecycle
//        config.min = -1
//        config.max = -1
//        config.isCancel = true
//        ic.setLifecycle(this, config)
//        ic.addViewHolderFactory()
    }


}