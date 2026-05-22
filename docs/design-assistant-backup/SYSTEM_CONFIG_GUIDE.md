# 系统配置管理功能使用指南

## 功能概述

濮院毛衫 AI 平台新增了系统配置管理功能，允许管理员在后台动态配置多个 AI 模型提供商和对象存储（OSS），支持自动切换和故障转移。

## 主要特性

1. **多 Key 轮换/故障转移**：支持配置多个 API Key，自动按优先级尝试
2. **加密存储**：所有敏感配置（API Key、Access Key）均使用 AES-GCM 加密存储
3. **多提供商支持**：可同时配置 OpenAI、阿里云、腾讯云等多个提供商
4. **OSS 多配置**：支持多个 OSS 配置，自动切换
5. **配置测试**：支持测试配置是否可用
6. **管理后台**：提供 Web 界面进行配置管理

## 启动步骤

### 1. 初始化数据库

```bash
mysql -u root -p puyuan_ai_mvp < backend/sql/system_config_migration.sql
```

### 2. 配置加密密钥（可选）

在 `application-dev.yml` 或环境变量中配置加密密钥：

```yaml
app:
  crypto:
    secret-key: ${CRYPTO_SECRET_KEY:puyuan-maoshan-default-secret-key-32bytes}
```

或通过环境变量：

```bash
export CRYPTO_SECRET_KEY=your-secret-key-here
```

### 3. 启动后端服务

```bash
cd backend/java-spring
mvn clean package
java -jar target/platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 4. 启动前端服务

```bash
cd frontend/admin-web
npm run dev
```

## 使用说明

### 1. 登录管理后台

访问 `http://localhost:5173/admin/login`，使用管理员账号登录。

### 2. 访问系统配置页面

登录后，点击左侧菜单的「系统配置」，进入配置管理页面。

### 3. 添加 AI 图片生成配置

- 选择「AI 图片生成」标签页
- 点击「添加配置」
- 填写以下信息：
  - 提供商名称：如 `OpenAI`
  - 模型名称：如 `dall-e-3`
  - API Key：你的 OpenAI API Key（以 `sk-` 开头）
  - API 端点：`https://api.openai.com/v1`
  - 优先级：1-10，数字越小优先级越高
  - 启用：勾选
- 点击「保存」

### 4. 添加 AI 文本生成配置

- 选择「AI 文本生成」标签页
- 点击「添加配置」
- 填写以下信息：
  - 提供商名称：如 `OpenAI`
  - 模型名称：如 `gpt-4o`
  - API Key：你的 OpenAI API Key
  - API 端点：`https://api.openai.com/v1`
  - 优先级：1-10
  - 启用：勾选
- 点击「保存」

### 5. 添加 AI 翻译配置

- 选择「AI 翻译」标签页
- 点击「添加配置」
- 填写以下信息：
  - 提供商名称：如 `OpenAI`
  - 模型名称：如 `gpt-4o-mini`
  - API Key：你的 OpenAI API Key
  - API 端点：`https://api.openai.com/v1`
  - 优先级：1-10
  - 启用：勾选
- 点击「保存」

### 6. 添加 OSS 配置

- 选择「对象存储」标签页
- 点击「添加配置」
- 填写以下信息：
  - 提供商名称：如 `Aliyun`
  - Access Key ID：你的阿里云 Access Key ID
  - Access Key Secret：你的阿里云 Access Key Secret
  - 端点：如 `oss-cn-hangzhou.aliyuncs.com`
  - Bucket 名称：你的 Bucket 名称
  - 区域：如 `cn-hangzhou`
  - 优先级：1-10
  - 启用：勾选
- 点击「保存」

## 多 Key 轮换测试

### 测试场景 1：主 Key 失效，自动切换到备 Key

1. 添加两个 OpenAI 配置：
   - 配置 1：优先级 1，使用有效的 API Key
   - 配置 2：优先级 2，使用另一个有效的 API Key

2. 调用 AI 服务，系统会自动使用配置 1

3. 如果配置 1 失效（如配额用完、Key 过期），系统会自动切换到配置 2

### 测试场景 2：故意使用失效 Key 测试切换

1. 添加两个配置：
   - 配置 1：优先级 1，使用**失效的** API Key
   - 配置 2：优先级 2，使用有效的 API Key

2. 调用 AI 服务，系统会先尝试配置 1，失败后自动切换到配置 2

3. 查看后端日志，可以看到切换过程

### 测试场景 3：测试配置功能

1. 在配置列表中，点击「测试」按钮

2. 系统会尝试连接该配置并返回测试结果

3. 如果配置无效，会显示错误信息

## 配置优先级规则

- 优先级数字越小，优先级越高
- 系统按优先级升序尝试配置
- 只有 `enabled=true` 的配置才会被使用
- 可以通过修改 `sort_order` 字段调整优先级

## 加密存储说明

- 所有 `api_key`、`access_key_secret` 等敏感字段在存储到数据库前会自动加密
- 前端显示时自动脱敏（如 `sk-****abcd`）
- 加密使用 AES-GCM 算法，密钥从 `CRYPTO_SECRET_KEY` 环境变量读取
- 如果未设置加密密钥，使用默认密钥（**生产环境请务必修改**）

## 降级机制

当数据库中没有配置或配置加载失败时，系统会自动降级到以下行为：

- AI 服务：使用 Mock 模式（返回占位数据）
- OSS 服务：返回 Mock URL

## API 接口说明

### 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/admin/system-config/groups | GET | 获取所有配置分组 |
| /api/v1/admin/system-config/list | GET | 获取指定分组的所有配置 |
| /api/v1/admin/system-config/ai | GET | 获取 AI 配置列表 |
| /api/v1/admin/system-config/oss | GET | 获取 OSS 配置列表 |
| /api/v1/admin/system-config/save | POST | 保存或更新配置 |
| /api/v1/admin/system-config/{id} | DELETE | 删除配置 |
| /api/v1/admin/system-config/test | POST | 测试配置 |

## 前端文件说明

- `frontend/admin-web/src/api/systemConfig.ts`：API 封装
- `frontend/admin-web/src/pages/AdminSystemConfigPage.vue`：系统配置页面
- `frontend/admin-web/src/router/routes.ts`：路由配置

## 后端文件说明

- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/entity/SystemConfig.java`：实体类
- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/mapper/SystemConfigMapper.java`：Mapper
- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/SystemConfigService.java`：Service 接口
- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/impl/SystemConfigServiceImpl.java`：Service 实现
- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/controller/AdminSystemConfigController.java`：Controller
- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/util/CryptoUtil.java`：加密工具类
- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/dto/SystemConfigDtos.java`：DTO 类
- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/StorageService.java`：OSS 服务接口
- `backend/java-spring/src/main/java/com/puyuanmaoshan/platform/service/impl/StorageServiceImpl.java`：OSS 服务实现

## 已修改的 AI Service

以下 Service 已更新为从 ConfigService 获取配置：

- `AiImageServiceImpl`：AI 图片生成
- `AiScriptServiceImpl`：AI 脚本生成
- `AiTranslateServiceImpl`：AI 翻译

## 注意事项

1. **加密密钥**：生产环境请务必修改 `CRYPTO_SECRET_KEY`，不要使用默认密钥
2. **配置备份**：建议定期备份数据库中的 `system_config` 表
3. **测试配置**：添加新配置后，建议先测试确认可用
4. **优先级设置**：确保至少有一个高优先级的可用配置
5. **降级机制**：当所有配置都失败时，系统会降级到 Mock 模式，请监控日志
