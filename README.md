# 外部依赖
项目使用了[rustfft](https://docs.rs/rustfft/latest/rustfft/)加速fft和ifft，
使用[rust_fft_wrapper](https://gitee.com/bieyuanxi/rust_fft_wrapper.git)简要包装并编译依赖库
## 使用简单包装的rustfft库
可直接下载已经编译好的库：
1. 创建文件夹：`app/jniLibs`和`app/native-libs`(后者用于本地测试用例)
2. 将[压缩文件](https://pan.baidu.com/s/1jw0eY33CJlsTDmH_7J28Sw?pwd=mfxf)解压到jniLibs目录下
3. 库文件按照如下表示存放：
    ```shell
    ├───jniLibs
    │   ├───arm64-v8a
    │   │       librust_fft_wrapper.so
    │   │
    │   ├───armeabi-v7a
    │   │       librust_fft_wrapper.so
    │   │
    │   └───x86_64
    │           librust_fft_wrapper.so
    ```
4. 编译Android项目时应该不会出现编译错误了



# Q
## gradle bin和src下载慢
修改 `项目根目录\gradle\wrapper\gradle-wrapper.properties` 内的`distributionUrl`为国内源，
使用`distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.9-all.zip`，注意后缀为`all`

## 代码更新后使用华为设备，程序未更新
在Run处的位置EditConfigruations, 勾选Always install with package manager。

## maven下载包很慢
修改`settings.gradle.kts`，使用国内镜像