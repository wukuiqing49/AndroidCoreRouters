package com.wkq.router.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wkq.router.annotation.Param
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router

@Route(path = "/demo/result")
class ResultActivity : AppCompatActivity() {

    @Param
    @JvmField
    var request: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.inject(this)

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 48, 32, 24)
                addView(TextView(this@ResultActivity).apply {
                    text = "ActivityResult 目标页"
                    textSize = 22f
                })
                addView(TextView(this@ResultActivity).apply {
                    text = "收到请求: $request"
                    textSize = 16f
                    setPadding(0, 24, 0, 24)
                })
                addView(Button(this@ResultActivity).apply {
                    text = "返回结果"
                    setOnClickListener {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra("result", "ResultActivity 返回成功")
                        )
                        finish()
                    }
                })
            }
        )
    }
}
