package com.wkq.router.demo

import com.wkq.router.annotation.Interceptor
import com.wkq.router.api.IInterceptor
import com.wkq.router.api.InterceptorCallback
import com.wkq.router.api.Postcard

@Interceptor(priority = 10)
class DemoLoginInterceptor : IInterceptor {
    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        if (postcard.path == "/demo/intercepted" && postcard.getExtras().getBoolean("requireLogin")) {
            callback.onInterrupt(IllegalStateException("Demo 拦截器模拟未登录"))
        } else {
            callback.onContinue(postcard)
        }
    }
}
