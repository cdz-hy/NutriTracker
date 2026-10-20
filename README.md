# NutriTracker

简体中文 | [English](README_EN.md)

一款基于 AI 拍照识别与文本速记的智能营养与卡路里追踪 Android 应用，帮助用户管理每日饮食、运动与健康目标。

## 截图

<div align="center">
<img src="images/1%20(1).jpg" alt="首页" width="240">&nbsp;&nbsp;
<img src="images/1%20(2).jpg" alt="日记" width="240">&nbsp;&nbsp;
<img src="images/1%20(3).jpg" alt="统计" width="240">
<br><br>
<img src="images/1%20(4).jpg" alt="个人资料" width="240">&nbsp;&nbsp;
<img src="images/1%20(5).jpg" alt="设置" width="240">
</div>

## 功能特性

**AI 识别与录入**
- 拍照或相册裁剪后识别食物，支持中餐、复合菜品与多图并发分析
- 支持「AI 一句话记餐」文本描述速记，以及识别结果弹窗微调与批量确认
- 支持食物库收藏/归档、多食材自定义食谱配比与整份/快速热量录入

**营养追踪与趋势分析**
- TDEE 每日目标计算（IOM 2005 公式）与目标体重渐近调整
- 四餐分区（早餐/午餐/晚餐/零食）与饮食日记跨日管理
- 宏量营养素及膳食纤维、糖、钠等微量营养素追踪（IOM DRI 参考面板）
- 周/月热量收支柱状图、宏量占比环图、连续打卡天数与近 7 日均值统计

**健康数据、微件与个性化**
- 50+ 种 MET 体力活动消耗追踪、自定义饮水快捷键与明细时间线
- 体重历史趋势折线图与历史日期补录，支持 BMI 双标准（WHO / 中国 WGOC）
- Jetpack Glance 桌面微件（一键 `+250ml` 记水与拍照直达）及 WorkManager 用餐/饮水提醒
- 浅色/深色/跟随系统主题、Material You 动态取色、公英制（lbs/in）与 kcal/kJ 切换
- Android Keystore 加密存储 API Key，支持 ZIP 全量备份导入导出

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM (ViewModel + StateFlow + Flow) |
| 依赖注入 | Hilt 2.53.1 |
| 数据库 | Room 2.6.1 |
| 导航 | Navigation Compose 2.8.5 |
| 相机 | CameraX 1.4.1 |
| 网络与解析 | OkHttp 4.12.0 + Gson 2.11.0 |
| 图片加载 | Coil 2.7.0 |
| 配置与后台 | DataStore 1.1.1 · WorkManager 2.10.0 · Glance 1.1.1 |

## 系统要求

- Android Studio 最新稳定版
- JDK 17+
- Android SDK 35 (`compileSdk`)
- 最低系统版本：Android 8.0 (API 26)

## 构建

```bash
# Debug 版本
./gradlew assembleDebug

# 运行单元测试
./gradlew testDebugUnitTest

# Release 版本（需配置签名）
./gradlew assembleRelease
```

APK 输出格式：`NutriTracker_v{版本号}_c{版本代码}_{构建类型}_{日期}.apk`

版本号在 [`version.properties`](version.properties) 中统一管理，修改此文件即可更新版本。

## AI 配置

应用不内置 AI 服务，需在设置页自行配置：

- **API Key** - 必需（使用 Android Keystore 本地加密存储）
- **Base URL** - 兼容 OpenAI `/chat/completions` 接口格式
- **模型名称** - 需支持多模态图片输入以使用拍照识图

设置页提供连接测试功能，可验证 API 配置与多模态识图能力。

## 项目结构

```text
app/src/main/java/com/example/nutritracker/
├── MainActivity.kt & RootViewModel.kt   # 主入口、全局主题与深度跳转
├── data/
│   ├── AppDatabase.kt                   # Room 数据库（9 张表，含 Schema 导出与迁移）
│   ├── DataExportManager.kt             # ZIP 全量备份与事务导入
│   ├── entity/                          # 数据实体与 MET 运动库
│   ├── dao/                             # 响应式 Flow DAO 接口
│   └── repository/                      # 数据仓库与加密设置管理
├── di/                                  # Hilt 依赖注入模块
├── feature/                             # 业务模块（首页/日记/统计/饮食库/拍照/活动/资料/设置/引导/来源）
├── navigation/                          # 路由与 5 Tab 底部导航
├── ui/
│   ├── components/                      # 图表、卡片、AI 队列横幅与通用弹窗
│   └── theme/                           # MD3 主题、动态配色与动画规范
├── util/                                # BMI/TDEE/DRI 算法、图片处理与加密工具
├── widget/                              # Jetpack Glance 桌面微件
└── worker/                              # WorkManager 用餐与饮水定时提醒
```

## 数据库

| 表名 | 用途 |
|------|------|
| `user` | 用户身体信息与目标设置（单用户模式） |
| `meals` | 食物营养模板、收藏归档状态与子食物项 |
| `intakes` | 饮食摄入记录 |
| `recipes` | 自定义食谱定义 |
| `recipe_ingredients` | 食谱食材配比关联 |
| `tracked_days` | 每日追踪目标快照 |
| `activities` | 体育活动消耗记录 |
| `weight_logs` | 体重历史日志 |
| `water_intakes` | 饮水记录 |

## 权限

- `INTERNET` - AI API 网络请求
- `CAMERA` - 拍照识别食物
- `POST_NOTIFICATIONS` - 用餐与饮水定时提醒通知（Android 13+）
- `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` (API ≤ 32) - 从相册选择图片

## 致谢

本项目的灵感和部分程序思路来源于 [OpenNutriTracker](https://github.com/simonoppowa/OpenNutriTracker)，在此基础上进行了重构与功能扩展，打造了一个轻量化的纯 Android 原生版本。

## 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 开源。
