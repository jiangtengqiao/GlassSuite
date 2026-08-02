// 国内镜像开关：本地默认开启（gradle.properties mirror=true），CI 传 -Pmirror=false 走官方源
// 注：Kotlin DSL 的 lambda 闭包无法访问脚本顶层 val，故直接内联判断
pluginManagement {
    repositories {
        if (providers.gradleProperty("mirror").getOrElse("true").toBoolean()) {
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (providers.gradleProperty("mirror").getOrElse("true").toBoolean()) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "GlassSuite"
include(":app")
