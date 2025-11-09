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

基于 Xposed，Hook 系统框架的进程创建函数，修改进程的 MaxOomAdj 实现应用进程保活。只能保活进程，对 Activity/Service 无效。

理论上支持 Android 8+，Hook 位置：[Hook.kt](./app/src/main/java/com/h3110w0r1d/phoenix/Hook.kt)，仅在 Android 15 上测试可用，其他版本谨慎使用。

## 下载模块

   - [Latest Release](https://github.com/h3110w0r1d-y/Phoenix/releases/latest)

## 使用说明

- 进入 LSPosed 启用模块，勾选`系统框架`，重启系统使模块生效
- 打开模块，勾选需要保活的应用（实时生效，无需重启系统，勾选后将目标应用重启即可）

## 画饼

- [ ] 允许修改默认MaxAdj
- [ ] 为每个应用配置不同的MaxAdj

这两个功能用处不大，随缘更新，欢迎提 PR

如有建议或问题欢迎提交 Issue 反馈。
