# 概述
轻量级应用多开器，实现小型应用的多开

# 原理
通过AndroidManifest.xml预埋stub的手段欺骗AMS，实现未注册组件生命周期的管理

使用Inline Hook拦截C库函数，重定向I/O至虚拟目录，实现应用间文件数据隔离

使用ART Hook拦截Java Framework层方法，替换参数，实现应用Activity、Service的正常启动

通过合并 Resources对象，修改AAPT产物，有效避免资源冲突，实现应用资源的加载

通过合并dexElements结构、修改ClassLoader双亲委派模型，实现应用dex的加载

绕过系统隐藏API调用限制，并拦截系统调用，集成签名校验绕过功能。
