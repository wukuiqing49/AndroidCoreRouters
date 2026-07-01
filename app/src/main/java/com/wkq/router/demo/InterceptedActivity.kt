package com.wkq.router.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wkq.router.annotation.Route

@Route(path = "/demo/intercepted")
class InterceptedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "如果看到这个页面，说明拦截器没有中断。"
            textSize = 18f
            setPadding(32, 64, 32, 32)
        })
    }
}
