# 本地生活服务平台

## 项目简介
一个类似大众点评的本地生活服务平台，包含商铺查询、优惠券秒杀、
用户签到、达人探店、好友关注等功能，并扩展了 AI 智能客服模块。

## 技术栈
- Spring Boot、MyBatis-Plus
- MySQL、Redis、RabbitMQ
- DeepSeek（AI 客服）

## 核心功能
- 短信登录（Redis + Token）
- 商铺缓存（逻辑过期 + 异步重建）
- 优惠券秒杀（Lua + RabbitMQ）
- AI 智能客服（DeepSeek + Prompt 约束）

## 运行方式
1. 启动 MySQL、Redis、RabbitMQ
2. 参考 `application-example.yaml` 配置 `application.yaml`
3. 运行 LocalLifePlatformApplication