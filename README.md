# AndroidCoreRouters

AndroidCoreRouters 是一个基于 Kotlin + KSP 的 Android 路由库，支持页面跳转、参数注入、拦截器、服务发现、降级处理和多模块自动注册。

## 模块说明

- `core_router_api`：运行时 API，负责路由表、跳转、参数注入、拦截器、服务查找和降级处理。
- `core_router_annotation`：注解模块，提供 `@Route`、`@Param`、`@Interceptor`、`@ProvideService`。
- `core_router_processor`：KSP 处理器，编译期生成路由注册代码。
- `app`：示例工程，只用于验证和演示，不作为库发布。

## 接入方式

```gradle
plugins {
    id "com.google.devtools.ksp"
}

dependencies {
    implementation "com.github.wukuiqing49:core_router_api:v1.0.0"
    ksp "com.github.wukuiqing49:core_router_processor:v1.0.0"
}

ksp {
    arg("moduleName", "app")
}
```

`core_router_api` 会传递暴露 `core_router_annotation`，所以业务模块通常不需要单独依赖 `core_router_annotation`。

每个声明了 `@Route`、`@Param`、`@Interceptor` 或 `@ProvideService` 的模块都需要配置 `ksp { arg("moduleName", "...") }`，并且不同模块建议使用不同的 `moduleName`，例如 `app`、`feature_user`、`feature_pay`。

## 初始化

建议在 `Application.onCreate()` 中初始化：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Router.setDebug(BuildConfig.DEBUG)
        val result = Router.init(this)
        check(result.success) { "Router init failed: $result" }
    }
}
```

`Router.init(context)` 是幂等的，多次调用不会重复注册，后续调用会返回上一次初始化结果。

常用运行时配置：

```kotlin
Router.setAutoInitWhenNavigate(true)
Router.setThrowExceptionWhenRouteNotFound(BuildConfig.DEBUG)
Router.setDegradationService(object : IDegradationService {
    override fun onLost(context: Context, postcard: Postcard) {
        // 路由丢失时打开兜底页、上报日志或提示用户。
    }
})
```

## 页面路由

声明路由：

```kotlin
@Route(path = "/demo/second")
class SecondActivity : AppCompatActivity() {
    @Param
    @JvmField
    var name: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.inject(this)
    }
}
```

发起跳转：

```kotlin
Router.build("/demo/second")
    .withString("name", "Router Demo")
    .navigation(context)
```

快捷跳转：

```kotlin
Router.navigate(context, "/demo/second")
```

## 拦截器

```kotlin
@Interceptor(priority = 10)
class LoginInterceptor : IInterceptor {
    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        if (needLogin(postcard)) {
            callback.onInterrupt(IllegalStateException("Login required"))
        } else {
            callback.onContinue(postcard)
        }
    }
}
```

拦截器优先级越高越先执行。调用 `callback.onInterrupt(...)` 后会进入统一错误和降级流程。

## 服务发现

声明服务接口与实现：

```kotlin
interface AccountService {
    fun userId(): String?
}

@ProvideService(AccountService::class)
class AccountServiceImpl : AccountService {
    override fun userId(): String? = "10001"
}
```

获取服务：

```kotlin
val service = Router.getService(AccountService::class)
```

## 稳定性说明

- 库内置 `consumer-rules.pro`，会保留生成路由类、`ServiceLoader` 资源、`IRouteInit`、`IRouteGroup`、`ISyringe` 和参数注入相关类。
- 如果业务启用了混淆和资源压缩，建议在 release 包里验证一次页面跳转、参数注入、拦截器和服务发现。
- 使用 `Router.getFragment()` 或 `Router.getView()` 时，目标类需要保留可反射调用的构造方法。
- `Router.resetForTest()` 仅建议在单元测试、Demo 环境切换或调试工具中使用。
- Debug 阶段可以打开 `Router.setThrowExceptionWhenRouteNotFound(true)`，让路由缺失问题更早暴露。

## 发布产物

本库发布为三个 artifact，版本号统一来自根目录的 `version.properties`。

- `com.github.wukuiqing49:core_router_api:v1.0.0`
- `com.github.wukuiqing49:core_router_annotation:v1.0.0`
- `com.github.wukuiqing49:core_router_processor:v1.0.0`

使用方通常只需要显式依赖：

```gradle
implementation "com.github.wukuiqing49:core_router_api:v1.0.0"
ksp "com.github.wukuiqing49:core_router_processor:v1.0.0"
```

## 本地发布

```bash
./gradlew publishRouterToMavenLocal
```

可以通过 Gradle 属性或环境变量覆盖发布坐标：

```properties
POM_GROUP_ID=com.your.company
POM_VERSION=v1.0.1
GITHUB_REPOSITORY=owner/repository
```

## 发版

指定版本发版：

```powershell
.\scripts\release-router.ps1 -Version 1.0.0
```

自动递增版本：

```powershell
.\scripts\release-router.ps1 -Bump patch
```

如果当前工作区存在尚未提交、但确认要一起进入本次发版的改动，可以加 `-AllowDirty`：

```powershell
.\scripts\release-router.ps1 -Bump patch -AllowDirty
```

脚本会更新 `version.properties`，验证 `:app:assembleDebug`，统一发布三个 artifact 到 Maven Local，创建发版提交，并创建 `vX.Y.Z` tag。

GitHub Actions 中的 `Release router` workflow 使用同一套版本号，并统一上传三个 artifact 到 GitHub Packages。

JitPack 会执行：

```bash
./gradlew publishRouterToMavenLocal -x test
```
