# 多版本移植状态

目标范围是 Minecraft 1.21.x、1.20.x 的正式版本，以及 1.18.2、1.16.5、1.12.2；各目标只列实际存在的 Fabric、Forge、NeoForge 加载器。下面的状态区分已完成的 Linux 构建与仍未完成的游戏内验证。

目前 Linux 构建与 JAR 元数据检查已通过的组合：Fabric 1.20–1.20.6、1.21–1.21.11；NeoForge 1.20.2、1.20.4、1.20.6、1.21、1.21.1、1.21.3、1.21.4、1.21.5、1.21.8、1.21.10、1.21.11；Forge 1.20、1.20.1、1.20.2、1.20.4。Forge 1.20.3 的 Forge 49.0.2 构建被缺失的 `bootstrap-dev:2.0.0` 阻塞。1.20.6 及 1.21.x 的 Forge 版本虽存在，但必要的 Architectury Forge 适配发布版缺失，尚未适配。1.18.2、1.16.5、1.12.2 尚无源码适配。所有组合都还需要 Windows 客户端启动和 IME 行为验证。

| Minecraft | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| 1.21 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | Linux 构建与 JAR 检查成功；待客户端验证 |
| 1.21.1 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | Linux 构建与 JAR 检查成功；待客户端验证 |
| 1.21.2 | Linux 构建与 JAR 检查成功；待客户端验证 | 无稳定版 | 无稳定版 |
| 1.21.3 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | Linux 构建成功；待客户端验证 |
| 1.21.4 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | Linux 构建成功；待客户端验证 |
| 1.21.5 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | Linux 构建成功；待客户端验证 |
| 1.21.6 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | 无稳定版 |
| 1.21.7 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | 无稳定版 |
| 1.21.8 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | Linux 构建与 JAR 检查成功；待客户端验证 |
| 1.21.9 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | 无稳定版 |
| 1.21.10 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | Linux 构建与 JAR 检查成功；待客户端验证 |
| 1.21.11 | Linux 构建与 JAR 检查成功；待客户端验证 | 上游缺少 Architectury Forge 适配发布版 | Linux 构建与 JAR 检查成功；待客户端验证 |
| 1.20 | Linux 构建与 JAR 检查成功；待客户端验证 | Linux 构建成功；待客户端验证 | 无稳定版 |
| 1.20.1 | Linux 构建与 JAR 检查成功；待客户端验证 | Linux 构建成功；待客户端验证 | 无稳定版 |
| 1.20.2 | Linux 构建与 JAR 检查成功；待客户端验证 | Linux 构建成功；待客户端验证 | Linux 构建与 JAR 检查成功；待客户端验证 |
| 1.20.3 | Linux 构建与 JAR 检查成功；待客户端验证 | Forge 49.0.2 所需 `bootstrap-dev:2.0.0` 未发布，当前构建链无法解析 | 无稳定版 |
| 1.20.4 | Linux 构建与 JAR 检查成功；待客户端验证 | Linux 构建成功；待客户端验证 | Linux 构建与 JAR 检查成功；待客户端验证 |
| 1.20.5 | Linux 构建与 JAR 检查成功；待客户端验证 | 无稳定版 | 无稳定版 |
| 1.20.6 | Linux 构建与 JAR 检查成功；待客户端验证 | 缺少 Architectury Forge 适配发布版 | Linux 构建与 JAR 检查成功；待客户端验证 |
| 1.18.2 | 待适配 | 待适配 | 无此官方加载器版本 |
| 1.16.5 | 待适配 | 待适配 | 无此官方加载器版本 |
| 1.12.2 | 无此官方加载器版本 | 待独立移植 | 无此官方加载器版本 |

## 构建入口

使用 JDK 21，在仓库根目录运行：

```sh
python3 tools/build_version.py --list
python3 tools/build_version.py 1.20.1
python3 tools/build_version.py 1.20.1 --loader forge
python3 tools/build_version.py 1.20.6 --loader neoforge
python3 tools/build_version.py 1.21.11 --dry-run
```

Windows 可将 `python3` 替换为 `python`。构建参数来自 `versions/*.properties`；脚本不改写根目录的 `gradle.properties`，也会拒绝未配置的版本或加载器。`--loader all` 只构建该 profile 启用的加载器。CI 只运行构建，不上传或发布构建产物。

当前使用 Gradle 8.13、JDK 21；`java_version` 控制生成字节码和 Mixin 配置，1.20.0–1.20.4 使用 Java 17，1.20.5 及以上使用 Java 21。构建结果位于 `fabric/build/<版本>/libs`、`forge/build/<版本>/libs` 与 `neoforge/build/<版本>/libs`。

`common/src/minecraft/<兼容族>/kotlin` 保存按 Minecraft API 区分的公共适配实现。当前适配族为 1.20.1、1.20.6、1.21.1、1.21.6、1.21.9、1.21.10、1.21.11。Forge 模块对复用 NeoForge 移植的源文件采用 Forge 包名和 Forge 事件入口；1.20.3 没有匹配的 REI Forge 发布版，因此该目标在构建时跳过可选 REI 混入。Forge 目标需安装 MixinExtras 0.4.1；依赖已写入 Forge 模组元数据。

Fabric 与 NeoForge 元数据声明精确的 Minecraft 版本。CI 对当前已配置的 1.20 与 1.21 目标逐项构建；未完成适配的旧版本不会作为成功目标列入矩阵。推送 `v*` 标签时，CI 会汇总并检查全部发行 JAR，发布 GitHub Release，再读取 GitHub Actions secret `MODRINTH_TOKEN` 创建 Modrinth 版本。

## 尚待完成

1. 选择或实现 Forge 1.20.3 的独立构建路径，避免依赖 Forge Maven 未发布的 `bootstrap-dev:2.0.0`。
2. 为 Forge 1.20.6 及 1.21.x 替换未提供 Forge 适配版的 Architectury 及对应界面集成依赖，再逐版本编译。
3. 移植 Fabric 与 Forge 1.18.2、1.16.5 的 GUI、文本输入、Mixin 目标和依赖；这两个版本需要单独的 API 适配族。
4. 为 Forge 1.12.2 建立旧版独立构建配置。它使用 LWJGL2，不能复用当前的 GLFW 窗口句柄获取和按键注入路径。
5. 在 Windows 客户端逐组合检查启动、聊天/告示牌/书本输入、窗口与全屏切换、中日文 IME、候选框位置、热键及设置持久化。

现有 DLL 随仓库提供。Linux 构建可检查 Java/Kotlin 编译和 JAR 内容，但不能验证 Windows IME 行为或原生库重编译；后者需要 Windows、MSVC、Windows SDK、CMake 和 JNI 头文件。
