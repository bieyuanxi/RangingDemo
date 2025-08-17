pluginManagement {
    repositories {
        // 优先使用国内镜像下载插件（如Android Gradle Plugin、Kotlin插件）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 官方仓库作为后备
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 优先使用国内镜像下载项目依赖（包括gradle-src等资源）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven(url = "https://jitpack.io")
        // 官方仓库作为后备
        google()
        mavenCentral()
    }
}

rootProject.name = "RangingDemo"
include(":app")
 