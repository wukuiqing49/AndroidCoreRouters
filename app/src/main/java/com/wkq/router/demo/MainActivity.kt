package com.wkq.router.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router

@Route(path = "/demo/main")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "AndroidCoreRouters Demo"
            textSize = 22f
            setPadding(32, 48, 32, 24)
        }
        val description = TextView(this).apply {
            text = "点击按钮通过 Router 跳转到 /demo/second，并注入 name 与 age 参数。"
            textSize = 16f
            setPadding(32, 0, 32, 24)
        }
        val button = Button(this).apply {
            text = "打开路由页面"
            setOnClickListener {
                Router.build("/demo/second")
                    .withString("name", "Router Demo")
                    .withInt("age", 18)
                    .navigation(this@MainActivity)
            }
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(title)
                addView(description)
                addView(button, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        )
    }
}
