package com.wkq.router.demo

import android.os.Bundle
import android.widget.Button
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

    @Param
    @JvmField
    var vip: Boolean = false

    @Param
    @JvmField
    var tags: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.inject(this)

        val title = TextView(this).apply {
            text = "路由跳转成功"
            textSize = 22f
            setPadding(32, 48, 32, 24)
        }
        val params = TextView(this).apply {
            text = "注入参数:\nname=$name\nage=$age\nvip=$vip\ntags=${tags?.joinToString()}"
            textSize = 16f
            setPadding(32, 0, 32, 24)
        }
        val back = Button(this).apply {
            text = "返回测试面板"
            setOnClickListener { finish() }
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(title)
                addView(params)
                addView(back, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        )
    }
}
