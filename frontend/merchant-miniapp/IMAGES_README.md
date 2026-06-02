# Tab Bar Icons

请将以下图标文件放置到 `src/images/` 目录：

## 需要的图标文件

### Tab Bar 图标 (32x32px, PNG 格式)

1. **tab-home.png** - 工作台图标（未选中）
2. **tab-home-active.png** - 工作台图标（选中）
3. **tab-plugin.png** - 插件图标（未选中）
4. **tab-plugin-active.png** - 插件图标（选中）
5. **tab-account.png** - 账户图标（未选中）
6. **tab-account-active.png** - 账户图标（选中）

## 图标设计建议

- 使用简单的线条图标
- 未选中状态使用灰色 (#999999)
- 选中状态使用主题色 (#667eea)
- 保持风格统一

## 临时解决方案

在正式图标制作完成前，可以使用以下方式：

1. 从图标库下载免费图标（如 iconfont、Flaticon）
2. 使用在线工具生成简单图标
3. 暂时移除 `app.config.ts` 中的 `iconPath` 配置，使用纯文本 TabBar
