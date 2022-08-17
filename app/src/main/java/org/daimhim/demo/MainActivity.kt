package org.daimhim.demo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.daimhim.widget.ic.IntervalCalendar
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var ic: IntervalCalendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ic = findViewById(R.id.ic_calendar)
        ic.maxCacheLimit = 12
        ic.initializationNumber = 5
        ic.maxInterval = 30

        ic.lifecycle = lifecycle

        findViewById<View>(R.id.tv_clear)
            .setOnClickListener {
                ic.clearSelect()
            }
    }


}