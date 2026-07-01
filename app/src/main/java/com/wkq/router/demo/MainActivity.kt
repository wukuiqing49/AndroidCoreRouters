package com.wkq.router.demo

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.wkq.router.annotation.Route
import com.wkq.router.api.IDegradationService
import com.wkq.router.api.Postcard
import com.wkq.router.api.Router

@Route(path = "/demo/main")
class MainActivity : AppCompatActivity() {

    private lateinit var content: LinearLayout
    private lateinit var statusCard: MaterialCardView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.setDegradationService(object : IDegradationService {
            override fun onLost(context: Context, postcard: Postcard) {
                showMessage("已进入降级处理: ${postcard.path}")
            }
        })
        buildPage()
    }

    private fun buildPage() {
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }

        val root = ScrollView(this).apply {
            setBackgroundColor(color(R.color.demo_bg))
            addView(content)
        }

        addHeader()
        addStatusCard()
        addSection("核心链路")
        addAction(
            iconRes = R.drawable.ic_md_send,
            title = "页面跳转与参数注入",
            route = "/demo/second",
            desc = "验证 @Route、Postcard 参数传递、Router.inject 和集合参数注入。"
        ) {
            Router.build("/demo/second")
                .withString("name", "Router Demo")
                .withInt("age", 18)
                .withBoolean("vip", true)
                .withStringArrayList("tags", arrayListOf("route", "param", "ksp"))
                .navigation(this)
        }
        addAction(
            iconRes = R.drawable.ic_md_reply,
            title = "ActivityResult",
            route = "/demo/result",
            desc = "验证通过代理 Fragment 启动目标页并接收返回结果。"
        ) {
            Router.build("/demo/result")
                .withString("request", "MainActivity 请求数据")
                .navigation(this) { result ->
                    val value = result.data?.getStringExtra("result") ?: "无返回内容"
                    showMessage("ActivityResult: code=${result.resultCode}, data=$value")
                }
        }
        addAction(
            iconRes = R.drawable.ic_md_route,
            title = "URI / Deeplink",
            route = "wkq://router/demo/second?name=UriDemo",
            desc = "验证 Router.build(Uri) 解析 path 和 query 参数。"
        ) {
            navigateUriCompat("wkq://router/demo/second?name=UriDemo")
        }

        addSection("多模块路由")
        addAction(
            iconRes = R.drawable.ic_md_route,
            title = "用户模块页面",
            route = "/user/center",
            desc = "验证 feature_user 独立模块生成并注册路由表。"
        ) {
            Router.build("/user/center")
                .withString("userId", "U-10086")
                .navigation(this)
        }
        addAction(
            iconRes = R.drawable.ic_md_tune,
            title = "支付模块页面",
            route = "/pay/checkout",
            desc = "验证 feature_pay 独立模块生成并注册路由表。"
        ) {
            Router.build("/pay/checkout")
                .withString("orderId", "ORDER-20260701")
                .withDouble("amount", 199.0)
                .navigation(this)
        }

        addSection("组件发现")
        addAction(
            iconRes = R.drawable.ic_md_extension,
            title = "Fragment 路由",
            route = "/demo/fragment",
            desc = "验证 Router.getFragment 创建被 @Route 标记的 Fragment。"
        ) {
            val fragment = Router.getFragment(
                "/demo/fragment",
                Bundle().apply { putString("title", "Fragment 参数注入成功") }
            )
            showDialog("Fragment 路由", fragment?.arguments?.getString("title") ?: "创建失败")
        }
        addAction(
            iconRes = R.drawable.ic_md_widgets,
            title = "View 路由",
            route = "/demo/view",
            desc = "验证 Router.getView 创建自定义 View。"
        ) {
            val view = Router.getView("/demo/view", this)
            showDialog("View 路由", if (view != null) "创建成功: ${view.javaClass.simpleName}" else "创建失败")
        }
        addAction(
            iconRes = R.drawable.ic_md_verified,
            title = "服务发现",
            route = "@ProvideService",
            desc = "验证接口实现自动注册，并通过 Router.getService 获取。"
        ) {
            val service = Router.getService(DemoRouterService::class)
            showDialog("服务发现", service?.summary() ?: "服务未找到")
        }

        addSection("稳定性")
        addAction(
            iconRes = R.drawable.ic_md_block,
            title = "拦截器中断",
            route = "/demo/intercepted",
            desc = "验证 @Interceptor 优先级、中断和降级流程。"
        ) {
            Router.build("/demo/intercepted")
                .withBoolean("requireLogin", true)
                .navigation(this)
        }
        addAction(
            iconRes = R.drawable.ic_md_warning,
            title = "路由丢失降级",
            route = "/missing/page",
            desc = "验证路由不存在时的统一兜底处理。"
        ) {
            Router.navigate(this, "/missing/page")
        }
        addAction(
            iconRes = R.drawable.ic_md_verified,
            title = "路由组预加载",
            route = "preload user/pay",
            desc = "验证 Router.preloadGroup 提前加载多模块路由表。"
        ) {
            val userReady = preloadGroupCompat("user")
            val payReady = preloadGroupCompat("pay")
            showDialog("路由组预加载", "user=$userReady, pay=$payReady")
        }
        addAction(
            iconRes = R.drawable.ic_md_info,
            title = "初始化状态",
            route = "Router.init",
            desc = "查看初始化状态、注册结果和运行时配置。"
        ) {
            refreshStatus()
            showDialog("初始化状态", statusText())
        }

        setContentView(root)
    }

    private fun addHeader() {
        content.addView(TextView(this).apply {
            text = "Router Lab"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.demo_text_primary))
        })
        content.addView(TextView(this).apply {
            text = "覆盖 AndroidCoreRouters 的跳转、注入、发现、拦截和降级能力。"
            textSize = 14f
            setTextColor(color(R.color.demo_text_secondary))
            setPadding(0, dp(6), 0, dp(16))
        })
    }

    private fun addStatusCard() {
        statusText = TextView(this).apply {
            text = statusText()
            textSize = 14f
            setLineSpacing(dp(2).toFloat(), 1f)
            setTextColor(color(R.color.demo_text_primary))
        }

        val statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(TextView(this@MainActivity).apply {
                text = "运行状态"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.demo_primary))
            })
            addView(statusText)
        }

        statusCard = MaterialCardView(this).apply {
            radius = dp(8).toFloat()
            cardElevation = 0f
            strokeWidth = 1
            strokeColor = 0xFFD8E4E1.toInt()
            setCardBackgroundColor(0xFFEFFAF7.toInt())
            addView(statusLayout)
        }
        content.addView(statusCard, matchWrap().apply {
            bottomMargin = dp(10)
        })
    }

    private fun addSection(title: String) {
        content.addView(TextView(this).apply {
            text = title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.demo_text_secondary))
            setPadding(0, dp(18), 0, dp(8))
        })
    }

    private fun addAction(iconRes: Int, title: String, route: String, desc: String, action: () -> Unit) {
        val icon = FrameLayout(this).apply {
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_demo_icon)
            addView(ImageView(this@MainActivity).apply {
                setImageResource(iconRes)
                setColorFilter(color(R.color.demo_primary))
            }, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.demo_text_primary))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(Chip(this@MainActivity).apply {
                text = route
                textSize = 11f
                isClickable = false
                isCheckable = false
                chipMinHeight = dp(28).toFloat()
                setTextColor(color(R.color.demo_primary))
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFFE7F5F2.toInt())
            })
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleRow)
            addView(TextView(this@MainActivity).apply {
                text = desc
                textSize = 13f
                setTextColor(color(R.color.demo_text_secondary))
                setPadding(0, dp(5), 0, 0)
            })
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                rightMargin = dp(13)
            })
            addView(textColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(TextView(this@MainActivity).apply {
                text = "›"
                textSize = 28f
                setTextColor(0xFF94A3B8.toInt())
                setPadding(dp(8), 0, 0, 0)
            })
        }

        val card = MaterialCardView(this).apply {
            radius = dp(8).toFloat()
            cardElevation = dp(1).toFloat()
            strokeWidth = 1
            strokeColor = 0xFFE2E8F0.toInt()
            setCardBackgroundColor(color(R.color.demo_surface))
            isClickable = true
            isFocusable = true
            foreground = selectableItemBackground()
            setOnClickListener { action() }
            addView(row)
        }

        content.addView(card, matchWrap().apply {
            bottomMargin = dp(10)
        })
    }

    private fun refreshStatus() {
        statusText.text = statusText()
    }

    private fun statusText(): String {
        val result = Router.getLastInitResult()
        return "initialized=${Router.isInitialized()}\ninitResult=${result ?: "暂无"}"
    }

    private fun showMessage(message: String) {
        Snackbar.make(content, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun navigateUriCompat(uri: String) {
        val method = Router::class.java.methods.firstOrNull {
            it.name == "navigateUri" && it.parameterTypes.contentEquals(arrayOf(Context::class.java, String::class.java))
        }
        if (method != null) {
            method.invoke(Router, this, uri)
        } else {
            Router.build("/demo/second")
                .withString("name", "UriDemo")
                .navigation(this)
        }
    }

    private fun preloadGroupCompat(group: String): Boolean {
        val method = Router::class.java.methods.firstOrNull {
            it.name == "preloadGroup" && it.parameterTypes.contentEquals(arrayOf(String::class.java))
        }
        return if (method != null) {
            method.invoke(Router, group) as? Boolean ?: false
        } else {
            false
        }
    }

    private fun selectableItemBackground(): android.graphics.drawable.Drawable? {
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = obtainStyledAttributes(attrs)
        return typedArray.getDrawable(0).also { typedArray.recycle() }
    }

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
}
