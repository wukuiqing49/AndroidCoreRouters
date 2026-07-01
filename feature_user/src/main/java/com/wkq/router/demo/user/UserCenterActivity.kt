package com.wkq.router.demo.user

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wkq.router.annotation.Param
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router

@Route(path = "/user/center")
class UserCenterActivity : AppCompatActivity() {

    @Param
    @JvmField
    var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.inject(this)

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 56, 32, 32)
                addView(TextView(this@UserCenterActivity).apply {
                    text = "Feature User"
                    textSize = 24f
                })
                addView(TextView(this@UserCenterActivity).apply {
                    text = "userId=$userId"
                    textSize = 16f
                    setPadding(0, 20, 0, 0)
                })
            }
        )
    }
}
