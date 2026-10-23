# MOTO NAV 桌面导航原型

面向 Android 的 Nothing 风格 `3×1` 导航组件原型。Doto 用于英文和数字，Zpix 用于中文。模拟模式验证下一步动作、距离、道路、速度、剩余时间、路线进度和 DEMO 城市夹具；高德 LIVE 模式只显示通知实际提供的数据，不提供或伪造真实路线指引。

视觉与信息架构参考 [MOTO GPS · Waveshare Edition](https://github.com/mx3353672833-debug/moto-gps-waveshare)，当前项目未复制其 iOS、ESP32 或 LVGL 源码。后续若复用上游代码，需遵守 PolyForm Noncommercial 1.0.0、NOTICE 和第三方许可。

## 构建

```sh
sh build.sh
adb install -r build/moto-nav-widget.apk
```

安装后先打开应用切换模拟状态，再将 `3×1`“MOTO 导航组件”添加到桌面。点击卡片主体打开控制页，点击右侧热区切换下一条导航指令。

真实场景按钮只在用户主动操作后读取一次位置，并通过 OpenStreetMap 标准小范围地图接口载入约 240m 内的道路和建筑轮廓。请求最长等待约 18 秒，重复点击不会产生排队任务；结果仅缓存在本机，不由本项目服务器收集。地图数据 © OpenStreetMap contributors，ODbL。

AMAP LIVE 布局将导航仪表、通知信息与 OSM 场景分为三个独立区域。真实道路和建筑只绘制在右侧 `OSM SCENE` 窗口，不会穿过主标题或导航状态文字。

