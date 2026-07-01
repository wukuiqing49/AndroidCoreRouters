package com.wkq.router.demo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wkq.router.api.Router
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouterSmokeTest {

    @Before
    fun setUp() {
        Router.resetForTest()
        Router.setThrowExceptionWhenRouteNotFound(true)
        Router.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun routerRuntimeFeaturesAreAvailable() {
        assertTrue(Router.preload("/demo/second"))
        assertTrue(Router.preloadGroup("user"))
        assertTrue(Router.preloadGroup("pay"))

        val fragment = Router.getFragment("/demo/fragment")
        assertNotNull(fragment)

        val view = Router.getView("/demo/view", ApplicationProvider.getApplicationContext())
        assertNotNull(view)

        val service = Router.getService(DemoRouterService::class)
        assertNotNull(service)

        val postcard = Router.buildUri("wkq://router/demo/second?name=Smoke")
        assertTrue(postcard.path == "/demo/second")
        assertTrue(postcard.getExtras().getString("name") == "Smoke")
    }
}
