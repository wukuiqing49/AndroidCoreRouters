package com.wkq.router.demo

import android.app.Application
import com.wkq.router.api.Router

class RouterDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Router.setDebug(BuildConfig.DEBUG)
        Router.init(this)
    }
}
