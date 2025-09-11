# 概述
轻量级应用多开器，实现小型应用的多开

# 原理
使用Inline Hook拦截C库函数，重定向I/O至虚拟目录，实现应用间文件数据隔离。

使用ART Hook拦截Framework层方法，欺骗AMS，实现未注册四大组件生命周期的管理。

通过合并 Resources对象、dexElements结构等方法，实现应用资源、dex的加载。

绕过系统隐藏API调用限制，并拦截系统调用及Framework层方法，集成签名校验绕过功能。
