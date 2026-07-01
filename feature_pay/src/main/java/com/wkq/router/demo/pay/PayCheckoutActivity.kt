package com.wkq.router.demo.pay

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wkq.router.annotation.Param
import com.wkq.router.annotation.Route
import com.wkq.router.api.Router

@Route(path = "/pay/checkout")
class PayCheckoutActivity : AppCompatActivity() {

    @Param
    @JvmField
    var orderId: String = ""

    @Param
    @JvmField
    var amount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.inject(this)

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 56, 32, 32)
                addView(TextView(this@PayCheckoutActivity).apply {
                    text = "Feature Pay"
                    textSize = 24f
                })
                addView(TextView(this@PayCheckoutActivity).apply {
                    text = "orderId=$orderId\namount=$amount"
                    textSize = 16f
                    setPadding(0, 20, 0, 0)
                })
            }
        )
    }
}
