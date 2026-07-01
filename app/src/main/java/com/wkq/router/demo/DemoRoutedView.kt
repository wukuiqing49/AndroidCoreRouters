package com.wkq.router.demo

import android.content.Context
import android.widget.TextView
import com.wkq.router.annotation.Route

@Route(path = "/demo/view")
class DemoRoutedView(context: Context) : TextView(context) {
    init {
        text = "DemoRoutedView"
        textSize = 18f
        setPadding(24, 24, 24, 24)
    }
}
