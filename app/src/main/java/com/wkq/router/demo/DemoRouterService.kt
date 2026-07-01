package com.wkq.router.demo

import com.wkq.router.annotation.ProvideService

interface DemoRouterService {
    fun summary(): String
}

@ProvideService(DemoRouterService::class)
class DemoRouterServiceImpl : DemoRouterService {
    override fun summary(): String {
        return "DemoRouterServiceImpl 已通过 @ProvideService 注册。"
    }
}
