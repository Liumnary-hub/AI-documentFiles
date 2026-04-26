# 企业文档 AI 助手（Spring AI + Vue3）

> 一个基于 RAG（检索增强生成）的企业级智能问答平台，支持文档管理、异步处理、多租户隔离与混合检索。

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen)
![License](https://img.shields.io/badge/License-MIT-yellow)

## ✨ 核心特性

- 📄 **文档全生命周期管理** － 支持 PDF 上传、状态追踪、版本管理、失败重试与重建索引
- 🤖 **智能 RAG 问答** － 基于 Spring AI + PGVector，实现语义检索与生成式回答
- 🧵 **异步高可靠性** － RabbitMQ 异步入库，超时保护、重试上限、死信队列，确保任务最终一致
- 🔒 **多租户隔离** － 按 workspace 隔离文档与问答，保障数据安全
- 🔍 **混合检索** － 支持 vector / hybrid（向量+BM25）模式切换，显著提升召回率
- 📊 **审计反馈闭环** － 操作日志、失败统计、用户反馈，持续优化问答质量
- 🎨 **现代前端** － Vue3 + TypeScript + Pinia + Element Plus，文档中心分页与状态轮询

## 🛠️ 技术栈

| 类别 | 技术 |
| :--- | :--- |
| 后端框架 | Spring Boot 3, Spring AI |
| 数据库 | MySQL, PGVector (PostgreSQL) |
| 消息队列 | RabbitMQ |
| 前端 | Vue3, TypeScript, Vite, Pinia, Element Plus |
| 构建工具 | Maven, npm |
| 部署 | Docker, Docker Compose |

## 📸 界面预览

*（建议放几张项目截图，例如文档列表、问答对话框、检索效果对比）*

## 🚀 快速开始

### 前置要求
- JDK 17+
- Node.js 18+
- Docker (可选，用于快速启动 RabbitMQ/PostgreSQL)
- Maven

### 1. 克隆项目
```bash
git clone https://github.com/Liumnary-hub/AI-documentFiles.git
cd AI-documentFiles
