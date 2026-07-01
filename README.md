# AndroidCoreRouters

AndroidCoreRouters 是一个基于 Kotlin + KSP 的 Android 路由库，支持页面跳转、参数注入、拦截器、服务发现、降级处理和多模块自动注册。

## 模块说明

- `core_router_api`：运行时 API，负责路由表、跳转、参数注入、拦截器、服务查找和降级处理。
- `core_router_annotation`：注解模块，提供 `@Route`、`@Param`、`@Interceptor`、`@ProvideService`。
- `core_router_processor`：KSP 处理器，编译期生成路由注册代码。
- `core_router_gradle_plugin`：可选 Gradle 插件，自动配置 API、KSP processor 和 `moduleName`。
- `app`：示例工程，只用于验证和演示，不作为库发布。
- `feature_user`、`feature_pay`：示例验证模块，只用于验证多业务模块同时生成和注册路由表，不作为库发布。

## 接入方式

在根工程 `settings.gradle` 中添加 JitPack 仓库。使用 Gradle 插件接入时，`pluginManagement` 和 `dependencyResolutionManagement` 都需要能解析 JitPack：

```gradle
pluginManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

普通依赖解析仓库：

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
    }
}
```

推荐使用 Gradle 插件一键接入。业务模块只需要应用插件，不需要再手动添加 `core_router_api`、`core_router_processor` 或 KSP `moduleName`：

```gradle
plugins {
    id "com.android.application" // 或 com.android.library
    id "org.jetbrains.kotlin.android"
    id "com.wkq.router" version "v1.0.4"
}
```

插件会自动：

- 应用 `com.google.devtools.ksp`
- 添加 `core_router_api`
- 添加 `core_router_processor`
- 根据 Gradle module path 生成 `moduleName`

如果需要覆盖默认值：

```gradle
wkqRouter {
    moduleName = "feature_user"
    version = "v1.0.4"
    group = "com.github.wukuiqing49.AndroidCoreRouters"
    addApiDependency = true
    addProcessorDependency = true
}
```

如果在根工程 `plugins` 里统一声明版本，子模块可以只写：

```gradle
plugins {
    id "com.wkq.router"
}
```

传统手动接入方式也继续支持：

```gradle
plugins {
    id "com.google.devtools.ksp"
}

dependencies {
    implementation "com.github.wukuiqing49.AndroidCoreRouters:core_router_api:v1.0.4"
    ksp "com.github.wukuiqing49.AndroidCoreRouters:core_router_processor:v1.0.4"
}

ksp {
    arg("moduleName", "app")
}
```

`core_router_api` 会传递暴露 `core_router_annotation`，所以业务模块通常不需要单独依赖 `core_router_annotation`。

每个声明了 `@Route`、`@Param`、`@Interceptor` 或 `@ProvideService` 的模块都需要配置 `ksp { arg("moduleName", "...") }`，并且不同模块建议使用不同的 `moduleName`，例如 `app`、`feature_user`、`feature_pay`。

本仓库的 demo 默认使用源码模块依赖，便于本地开发。如果要像外部项目一样验证远程依赖，可以执行：

```bash
./gradlew :app:assembleDebug -PusePublishedRouter=true
```

也可以执行封装好的验证任务：

```bash
./gradlew verifyRouterPublishedSample
```

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
Router.setInterceptorTimeout(3000)
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

URI / Deeplink 跳转：

```kotlin
Router.navigate(context, Uri.parse("wkq://router/demo/second?name=RouterDemo"))
// 或
Router.navigateUri(context, "wkq://router/demo/second?name=RouterDemo")
```

编译器会为每个模块生成路由常量，减少手写 path：

```kotlin
import com.wkq.router.generated.RouterPaths_app

Router.navigate(context, RouterPaths_app.DEMO_SECOND)
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

服务默认懒加载：编译期只注册 provider，首次 `Router.getService()` 时创建实现并缓存。

预加载指定路由或路由组：

```kotlin
Router.preload("/user/center")
Router.preloadGroup("pay")
```

## 稳定性说明

- 库内置 `consumer-rules.pro`，会保留生成路由类、`IRouteInit`、`IRouteGroup`、`ISyringe` 和参数注入相关类。
- 如果业务启用了混淆和资源压缩，建议在 release 包里验证一次页面跳转、参数注入、拦截器和服务发现。
- 本仓库提供 `verifyRouterReleaseMinify` 任务，会构建开启混淆和资源压缩的 demo release 包，并检查 APK 内是否保留路由初始化元数据。
- 本仓库提供 `verifyRouterPublishedSample` 任务，会强制 demo 使用 `-PusePublishedRouter=true` 构建，用来模拟外部项目接入已发布库。
- 本仓库提供 `:core_router_processor:test`，使用 compile-testing 验证 KSP 生成结果、缺少 `moduleName` 的错误提示、重复路由 path 的编译期报错。
- 本仓库提供 `verifyRouterConnectedSmoke` 任务，有设备或模拟器时可执行运行级 smoke test，覆盖初始化、多模块路由、Fragment/View、服务发现、URI 解析和预加载。
- 使用 `Router.getFragment()` 或 `Router.getView()` 时，目标类需要保留可反射调用的构造方法。
- `Router.resetForTest()` 仅建议在单元测试、Demo 环境切换或调试工具中使用。
- Debug 阶段可以打开 `Router.setThrowExceptionWhenRouteNotFound(true)`，让路由缺失问题更早暴露。

## 常见错误排查

### 找不到 `moduleName`

每个使用路由注解的模块都必须配置：

```gradle
ksp {
    arg("moduleName", "feature_user")
}
```

`moduleName` 必须匹配 `[A-Za-z][A-Za-z0-9_]*`，不同模块建议使用不同名称。

### JitPack 坐标解析失败

多模块 JitPack 坐标需要包含仓库名：

```gradle
implementation "com.github.wukuiqing49.AndroidCoreRouters:core_router_api:v1.0.4"
ksp "com.github.wukuiqing49.AndroidCoreRouters:core_router_processor:v1.0.4"
```

不要写成 `com.github.wukuiqing49:core_router_api:v1.0.4`。

### 路由不存在

- 检查 path 是否符合 `/group/page` 格式。
- 检查目标模块是否配置了 KSP。
- 检查 app 是否依赖了对应 feature 模块。
- Debug 阶段可开启 `Router.setThrowExceptionWhenRouteNotFound(true)`。

### 参数没有注入

- Activity/Fragment 中需要调用 `Router.inject(this)`。
- Kotlin 属性建议使用 `@JvmField var`。
- 当前支持基础类型、基础类型数组、`String`、`Array<String>`、`Bundle`、`Parcelable`、`Serializable`、`ArrayList<String>` 和 `ArrayList<Int>`。
- 不支持的 `@Param` 类型会在 KSP 编译阶段直接报错，避免运行时静默注入失败。

### 混淆后异常

- 确认使用方依赖的是 `core_router_api`，其 consumer rules 会自动合并。
- 如果使用 `Router.getFragment()` 或 `Router.getView()`，目标类需要保留可反射构造方法。
- 可在库工程执行 `./gradlew verifyRouterReleaseMinify` 复现 release 混淆构建。

## 发布产物

本库发布为三个核心 artifact 和一个可选 Gradle 插件 artifact，版本号统一来自根目录的 `version.properties`。对使用方来说，通常只需要选择“插件接入”或“手动接入”其中一种方式。

- `com.github.wukuiqing49.AndroidCoreRouters:core_router_api:v1.0.4`
- `com.github.wukuiqing49.AndroidCoreRouters:core_router_annotation:v1.0.4`
- `com.github.wukuiqing49.AndroidCoreRouters:core_router_processor:v1.0.4`
- `com.github.wukuiqing49.AndroidCoreRouters:core_router_gradle_plugin:v1.0.4`

`app`、`feature_user`、`feature_pay` 只用于 sample 和 CI 验证，不发布到 Maven/JitPack。

手动接入时，使用方通常只需要显式依赖：

```gradle
implementation "com.github.wukuiqing49.AndroidCoreRouters:core_router_api:v1.0.4"
ksp "com.github.wukuiqing49.AndroidCoreRouters:core_router_processor:v1.0.4"
```

## 本地发布

```bash
./gradlew publishRouterToMavenLocal
```

验证 release 混淆构建：

```bash
./gradlew verifyRouterReleaseMinify
```

该任务不仅构建 release APK，还会检查 `META-INF/services/com.wkq.router.api.IRouteInit` 是否包含 sample 的全部路由初始化类。

验证 KSP 处理器生成逻辑和错误提示：

```bash
./gradlew :core_router_processor:test
```

验证 sample 强制引用已发布路由库：

```bash
./gradlew verifyRouterPublishedSample
```

一次性执行 processor 单测、Gradle 插件单测、sample 源码构建、release 混淆构建和 published 依赖构建：

```bash
./gradlew verifyRouterSample
```

验证本地发布产物链路：

```bash
./gradlew verifyRouterPublishLocal
```

有设备或模拟器时执行运行级 smoke test：

```bash
./gradlew verifyRouterConnectedSmoke
```

可以通过 Gradle 属性或环境变量覆盖发布坐标：

```properties
POM_GROUP_ID=com.your.company
POM_VERSION=v1.0.5
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

该参数会在发版提交前执行 `git add -A`，把当前工作区的改动一起纳入本次发版。

脚本会更新 `version.properties`，验证 processor 单测、Gradle 插件单测、`:app:assembleDebug`、release 混淆和 Maven Local published sample，统一发布三个核心 artifact 和 Gradle 插件 marker 到 Maven Local，创建发版提交，并创建 `vX.Y.Z` tag。

GitHub Actions 中的 `Release router` workflow 使用同一套版本号，并统一上传三个核心 artifact 和 Gradle 插件 marker 到 GitHub Packages。

JitPack 会执行：

```bash
./gradlew publishRouterToMavenLocal -x test
```

