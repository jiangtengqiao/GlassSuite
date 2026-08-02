pluginManagement {
    // 镜像开关：本地默认开启（gradle.properties mirror=true），CI 传 -Pmirror=false 走官方源
    val useMirror = providers.gradleProperty("mirror").getOrElse("true").toBoolean()
    repositories {
        if (useMirror) {
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
        if (useMirror) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "GlassSuite"
include(":app")
