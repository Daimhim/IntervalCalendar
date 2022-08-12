package org.daimhim.demo

import android.app.Activity
import android.os.Bundle
import org.daimhim.widget.ic.IntervalCalendar
import org.daimhim.widget.ic.IntervalCalendarController
import java.util.*

class MainActivity : Activity() {
    private lateinit var ic: IntervalCalendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ic = findViewById(R.id.ic_calendar)

        val config: IntervalCalendarController.Config = IntervalCalendarController.Config()
        val instance = Calendar.getInstance()
        instance.timeInMillis = System.currentTimeMillis()
        instance.add(Calendar.MONTH, 3)
        config.minLimit = System.currentTimeMillis()
        config.maxLimit = instance.timeInMillis
        config.min = 1
        config.max = 3
        config.isCancel = true
        ic.setLifecycle(this, config)
    }


}