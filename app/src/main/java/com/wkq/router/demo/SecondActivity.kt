package com.wkq.router.demo

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wkq.router.annotation.Param
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router

@Route(path = "/demo/second")
class SecondActivity : AppCompatActivity() {

    @Param
    @JvmField
    var name: String = ""

    @Param
    @JvmField
    var age: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.inject(this)

        val title = TextView(this).apply {
            text = "路由跳转成功"
            textSize = 22f
            setPadding(32, 48, 32, 24)
        }
        val params = TextView(this).apply {
            text = "注入参数: name=$name, age=$age"
            textSize = 16f
            setPadding(32, 0, 32, 24)
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(title)
                addView(params)
            }
        )
    }
}
