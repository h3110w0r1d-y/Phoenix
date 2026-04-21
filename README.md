<div align="center">
  <img src="./app/src/main/ic_launcher-round.png" alt="Phoenix" width="128" height="128">
  <h1>不死鸟 (Phoenix)</h1>

[![GitHub release](https://img.shields.io/github/v/release/h3110w0r1d-y/Phoenix)](https://github.com/h3110w0r1d-y/Phoenix/releases)
[![CI/CD](https://github.com/h3110w0r1d-y/Phoenix/actions/workflows/release.yml/badge.svg)](https://github.com/h3110w0r1d-y/Phoenix/actions/workflows/release.yml)
![Android](https://img.shields.io/badge/Android-8.0%2B-blue)
![License](https://img.shields.io/github/license/h3110w0r1d-y/Phoenix)
![GitHub stars](https://img.shields.io/github/stars/h3110w0r1d-y/Phoenix?style=social)
</div>

---

## 项目简介

基于 Xposed，Hook 系统框架的进程创建函数，修改进程的 MaxOomAdj 实现应用进程保活。

理论上支持 Android 8+，Hook 位置：[Hook.kt](./app/src/main/java/com/h3110w0r1d/phoenix/Hook.kt)，仅在 Android 15~16 上测试可用，其他版本谨慎使用。

## 下载模块

  - [Latest Release](https://github.com/h3110w0r1d-y/Phoenix/releases/latest)

## 使用说明

  - 进入 LSPosed 启用模块，仅勾选`系统框架`，重启系统使模块生效
  - 打开模块，勾选需要保活的应用（Android 10+ 实时生效，无需重启系统/应用; Android 10 以下重启应用生效）

## 应用截图

<details>
    <summary>点击展开截图</summary>
    <img src="img/home.png" width="300">
    <img src="img/app.png" width="300">
    <img src="img/setting.png" width="300">
</details>

## ❤️赞助者

| [<img src="https://github.com/Jie0746.png" width="64px;"/><br>@Jie0746<br>(ByChum)](https://github.com/Jie0746) |
| :--: |

## 画饼

  - [x] 配置默认MaxAdj
  - [x] 为应用配置不同MaxAdj
  - [x] 支持开启Persistent
  - [x] 保活Activity (Android 11+)
  - [ ] 保活Service

如有建议或问题欢迎提交 Issue 反馈或加群交流1083682874。

## 打赏

<details>
    <summary>点击展开二维码</summary>
    <img src="img/wechat.png" width="300">
    <img src="img/alipay.jpg" width="300">
</details>
