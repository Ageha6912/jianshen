# 交接文档:健身打卡 App(Windows + Android 模拟器全流程)

> 写给零上下文的新会话。读完这份文档你应该能立刻接着干活,不需要问任何背景问题。

---

## 一、我们在做什么

在用户的 Windows 11 家庭中文版(24H2,**精简版系统**)上,从零搭建 Android 开发环境,并用 Kotlin + Jetpack Compose 开发一个中文健身记录 App「**健身打卡**」(包名 `com.jianshen.fitness`)。工作方式是 grilling 会话:所有产品决策都经用户逐轮确认后才执行。

### 产品规格(已冻结,全部经用户确认)

- **v1(已交付验收)**:底部 2 tab(训练日志/动作库);动作库 43 个动作(来自 GitHub 开源数据集 exercises-dataset,含中文名/中文分步说明/缩略图/GIF);训练日志 = 弹窗表单打卡(日期+动作+组数×次数+重量kg可选),Room 持久化;Material 3 默认风格。
- **v2(已交付验收)**:应用户要求追加。① **Notion 简约风设计语言**(纸感白底 `#FFFFFF`/暗色 `#191919`、暖黑文字 `#37352F`、零阴影卡片用 1px 描边、圆角 4-6dp、黑底白字 CTA、emoji 图标、底部导航无色块);② **Session 逐组记录模型**(开始训练→添加动作→逐组勾选记录,session 持久化可恢复);③ **组间休息计时器**(勾组触发,页内黑色横幅 + 系统通知,默认 90s 长按横幅改 60/90/120);④ **PR + Epley 估算 1RM**(历史 tab 内「日志/个人纪录」双页签);⑤ **导出**(设置页:CSV 带 BOM + JSON 全量备份,SAF 零权限);⑥ 深色主题跟随系统;⑦ DB v1→v2 **摧毁式重建**(旧数据清空属预期)。
- **v3(2026-08-30 已交付验收):白金风设计重皮,向 LibreFit 看齐**。用户主动重开已冻结的 Notion 风决策,经 grill-me 会话三轮拷问定案:**香槟铂金单强调色**(深色主色 `#E8E1D5`/暖黑底 `#141210`;浅色反转为炭黑主色 `#2A2724`/暖白底 `#FAF9F7`,白金容器 `#EFE9DE`;完成/选中全用白金,红色仅错误/删除,绿色彻底退出)+ **M3 大圆角**(卡片 20/弹层对话框 28/控件 12-16dp,弃描边靠表面色阶)+ **Roboto Flex 宽体数字**(wdth=125,数字拉丁生效、中文回退系统;字体子集 103KB 在 `res/font/roboto_flex.ttf`)+ **底部导航 pill 高亮**(res/drawable 自持矢量图标)+ **逐组表格卡**(列头 `#/重量(kg)/次数`,行尾圆形✓=完成态(点按删除),草稿录入行自动带入上组数值,动作缩略图)+ **动作选择器改 ModalBottomSheet**(保持打开便于连加,已选项标「已添加」)+ **休息横幅重皮**(表面高阶层 + 进度条,新增 `RestTimer.total` 流;通知 `setColor` 白金染色)+ **主题三档**沿用 + **应用图标对勾改白金**(gen_icon.py 已更新)。
- **v4(2026-08-31 已交付验收):功能补强**,grilling 定案(数据洞察 A + 数据安全 D + 轻量计划 B + 有氧/计时动作 C1)。① 底部导航 **5 tab**(训练/历史/统计/计划/动作库);② **统计 tab**:本周训练次数 / 连续不空训练周(周维度)/ 总容量 + **6 个月训练热力图**(自动滚到当前周,点天看当天摘要)+ 动作走势入口;③ **每动作历史页**:自绘 Canvas 折线图(最高重量 / 估算 1RM 切换)+ 全部记录,入口 = 统计页 / PR 条目 / 动作库详情;④ **计划模板(轻量)**:模板 = 名称 + 动作(目标组数 + 次数区间 + 可选目标重量),从模板开始预填动作、训练页显示「目标 N 组 × X-Y 次 · 已完成 M/N」,**预置推/拉/腿 3 个**(首启种子,可删);⑤ **数据安全**:JSON 覆盖式导入(红色确认,导入前自动再备份)+ 应用内自动备份(每 7 天,私有目录轮换 4 份),设置页「备份」区;⑥ **有氧/计时动作**:assets type="timed",预置 8 个(跑步/快走/动感单车/跳绳计时/划船机/椭圆机/拉伸-腿/拉伸-肩背,图标占位),记录 = 时长(分)+ 可选距离(km),不进 1RM;⑦ 休息倒计时自然结束**响铃 + 震动**(VIBRATE 权限);⑧ **DB v3→v4 真 Migration**(首次,ALTER+CREATE,移除 fallbackToDestructiveMigration),模拟器实测旧数据逐行完好。
- **明确不做**(第三批候选):动作库扩容(数据集还剩 ~1280 个动作可用)、逐组「上次数据」增强、RPE/组备注、体重记录、周期日程排期、多语言、云同步、社交、Wear OS、小组件。

---

## 二、已完成

### 环境(全部就绪,重启后已验证)

| 组件 | 位置/状态 |
|---|---|
| JDK 17 (Adoptium Temurin 17.0.20.101) | `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`,用户级 `JAVA_HOME` 已持久化 |
| Gradle 8.9 | `E:\Android\tools\gradle-8.9`(winget 装不上,手动 zip) |
| Android SDK 全套(API 35/build-tools 35.0.0/platform-tools/emulator) | `E:\Android\Sdk`;用户级 `ANDROID_HOME`/`ANDROID_SDK_ROOT` 已持久化 |
| junction | `C:\Users\31198\AppData\Local\Android\Sdk` → `E:\Android\Sdk`(别删,插件默认找这个路径) |
| WHPX 虚拟化加速 | **已启用且已重启生效**,`emulator -accel-check` 返回 accel: 0 |
| AVD | `medium_phone`(API 35, default/x86_64),分辨率 1080x2400 密度 420 |
| 项目 Gradle wrapper | 已生成,`gradle-wrapper.properties` 的 distributionUrl 指向腾讯镜像 |

### App

- **v1 已在模拟器全流程验收通过**(空态/打卡/列表/动作库/GIF 详情/零崩溃),截图:`E:\jianshen\_dataset\shot_01~07_*.png`。
- **v2 已构建并全流程验收通过(2026-08-30)**:`E:\jianshen\FitnessApp\app\build\outputs\apk\debug\app-debug.apk`。修复过徒手组 bug(DB v3)。
- **v3 白金风已构建并全流程验收通过(2026-08-30,见下方 v3 小节)**:versionName 2.0 / versionCode 2,debug 包 **14.6MB**(v1.3 时 21MB 的 icon 包胀回已解决),已装模拟器。截图 `E:\jianshen\_dataset\v3_01~14_*.png`。
- **v4 功能补强已构建并全流程验收通过(2026-08-31)**:versionName 4.0 / versionCode 3,release 包 ~12MB。截图 `E:\jianshen\_dataset4_01~05_*.png`。git 仓库 github.com/Ageha6912/jianshen(公开),Release v2.0 / v4.0 已发布。

### 代码结构(`E:\jianshen\FitnessApp`)

- `data/Database.kt`:Room v2,三表 `sessions`/`session_exercises`/`set_entries`,PR 查询(SQLite bare-column MAX + Epley `weightKg*(1.0+reps/30.0)`)、导出查询。
- `data/RestTimer.kt`:进程内倒计时单例(StateFlow `remaining`) + 通知工具函数 + 90s 偏好存 SharedPreferences。
- `data/Exercise.kt`:动作模型 + assets 加载器 + `CATEGORY_EMOJI` 映射 + `Float.fmtKg()`。
- `ui/`:Theme.kt(Notion 双色系+小圆角 shapes)、AppRoot(3 tab:🏋️训练/📅历史/📚动作库)、TrainScreen(session 编辑器+计时器横幅+通知权限请求)、HistoryScreen(日志/PR 双页签)、SettingsScreen(SAF 导出+关于署名)、LibraryScreen、ExerciseDetail、Common.kt(`Modifier.combinedClickableNoRipple`)。
- `assets/exercises.json`(43 动作)+ `assets/media/images|videos`(88 文件 4.5MB)。
- 数据集工作区:`E:\jianshen\_dataset\`(subset.json 是动作子集源,exercises.json 17MB 全量也在这)。

---

## 三、v2 验收结果(2026-08-30 全部完成,零阻塞)

v2 十步验收流程全部通过,截图在 `E:\jianshen\_dataset\v2_01~15_*.png`:

1. ✅ 动作选择器按部位分组(v2_03)
2. ✅ 动作卡片出现;DB 重建后首次无「上次」行符合预期(v2_04)
3. ✅ 勾组三验证:组行 50kg×10次 / 黑色横幅 90s 倒数(87→)/ 通知「组间休息·休息90秒」(v2_05、v2_06)
4. ✅ 俯卧撑徒手组 15次×2 组(修复后,v2_09)
5. ✅ 杀进程恢复:force-stop 后重启 session 完整恢复,计时器横幅随进程消失属预期(v2_10)
6. ✅ 历史日志页签 session 分组明细正确、徒手显示「15次」无脏数据(v2_11);PR 页签 杠铃卧推 最佳 50kg×10次 **估算1RM≈67**(Epley 50×(1+10/30)=66.7;旧文档写的 ≈55 是笔误)(v2_12);全徒手动作不出现在 PR(查询已过滤)
7. ✅ CSV 导出(SAF→Download):UTF-8 BOM 正确,表头 `日期,动作,组序,重量(kg),次数`,徒手组重量导出为空串(v2_13,导出文件已拉回 `_dataset/csv_export.csv`)
8. ✅ 深色模式:#191919 底 + CTA 反转白底黑字(v2_14、v2_15)
9. ✅ `logcat -d | grep -E "FATAL|AndroidRuntime.*com.jianshen"` 为空

### 验收中发现并修复的 bug:徒手动作无法记组

原实现 `TrainScreen.kt` 勾组按钮 `enabled` 要求重量非空,导致俯卧撑/引体向上等徒手动作记不了组,与冻结规格「重量可选」冲突。已修复:**weightKg 全链路改可空**——`SetEntry.weightKg: Float?`、`ExportRow.weightKg: Float?`、✓ 按钮 enabled 只看次数、组行/日志显示 `(weightKg?.let { "${it.fmtKg()}kg × " } ?: "") + "${reps}次"`、`Float?.fmtKgOrNull()` 新增于 Exercise.kt、PR 查询加 `WHERE weightKg IS NOT NULL`(徒手不参与 Epley 1RM)、CSV 空串/JSON `JSONObject.NULL`。**DB version 2→3,摧毁式重建**(开发期可接受;交付有真实数据后改 Migration,见坑 17)。改动文件:Database.kt、Exercise.kt、TrainScreen.kt、HistoryScreen.kt、SettingsScreen.kt。

### 后续候选(用户未拍板,别擅自动工)

进度图表、GitHub 式热力图、动作库扩容(数据集剩 ~1280 个)、训练计划体系。

### App 图标(2026-08-30 已接入)

暖黑底(#37352F)+ 白色哑铃 + 绿色对勾徽章,Notion 风。自适应图标全密度资源在 `res/mipmap-*`(`ic_launcher.png`/`ic_launcher_round.png`/`ic_launcher_foreground.png` + `mipmap-anydpi-v26/*.xml` + `values/ic_launcher_background.xml`),manifest 已声明 icon/roundIcon,含 monochrome 层(Android 13 主题图标)。**生成脚本:`E:\jianshen\_dataset\gen_icon.py`**(PIL,改色/改形后重跑即可再生成);预览 `_dataset/icon_preview.png`,抽屉实拍 `v2_16_drawer.png`。

### 外观设置 v1.2(2026-08-30 已交付)

设置页新增「外观」三选一:🌗跟随系统 / ☀️浅色 / 🌙深色,默认跟随系统。实现:`data/ThemePrefs.kt`(object + `mutableStateOf` + SharedPreferences 键 `theme_mode`,与 rest_seconds 同文件 `fitness_prefs`)→ MainActivity 读偏好算 dark 传 `FitnessTheme(darkTheme=)`(Theme.kt 已加参数,默认仍 `isSystemInDarkTheme()`)。已验证:深↔浅↔跟随系统即时切换、杀进程持久化、深色下状态栏白色图标。相关截图 `v2_17~27_*.png`。

### 去 emoji 图标化 v1.3(2026-08-30 已交付)

App 内所有 emoji 全部移除,改用 `material-icons-extended`(BOM 2024.12.01 自带版本,经阿里云镜像可拉取)。微信式风格:底部导航选中实心(Filled)/未选中描边(Outlined),灰色/墨色;训练=FitnessCenter、历史=DateRange、动作库=AutoMirrored MenuBook。其余替换:训练页设置入口=Outlined.Settings、设置页返回=AutoMirrored ArrowBack、导出行=Description/Backup(灰色小图标)、分类头/筛选 chip/PR 页/详情页/器械行=纯文本。`CATEGORY_EMOJI` 映射已整体删除。**debug APK 从 14.4MB 涨到 ~21MB**(extended 全量图标未裁剪;出 release 时 R8 minify 会裁掉未用图标,属预期)。截图 `v2_28~34_*.png`。**【v3 已推翻此方案】**:icons-extended 依赖已整体移除、改自持矢量 drawable(见下节),APK 回落到 14.6MB。

### v3 白金风(2026-08-30 已交付,grilling 共识全按推荐落地)

设计 token(写死在 `ui/Theme.kt`,实现时微调 ± 已含):深色 底`#141210`/主色`#E8E1D5`/onPrimary`#1F1B16`/容器五级`#0E0C0A→#2C2926`;浅色 底`#FAF9F7`/主色`#2A2724`/容器`#EFE9DE` 系;错误=M3 标准红。形状:extraSmall 8/small 12/medium 16/large 20/extraLarge 28dp + `PillShape`(RoundedCornerShape(50))供按钮/页签/chip。字体:`PlatinumFontFamily` = Roboto Flex 可变字体(wght 100-1000 + wdth 25-151 双轴,其余 11 轴已用 fontTools instancer 钉死),`FontVariation.width(125f)`,全 Typography 挂它——数字拉丁自动宽体,中文回退系统字体,零额外处理。

改动清单:`ui/Theme.kt`(整套重写)、`MainActivity.kt`(**根部包了一层 Surface 设 contentColor**——修深色下绕过 Scaffold 的页面黑字不可读的 bug,别删)、`ui/AppRoot.kt`(NavigationBar indicator=secondaryContainer pill,去分隔线)、`ui/TrainScreen.kt`(ExerciseSetCard 表格化+缩略图+圆勾行、RestBanner 重皮+进度条、ExercisePickerSheet=ModalBottomSheet)、`ui/HistoryScreen.kt`(HistoryPill 页签、PR 卡片大号 1RM)、`ui/LibraryScreen.kt`(pill 分类 chip、圆角缩略图)、`ui/ExerciseDetail.kt`(返回箭头图标)、`ui/SettingsScreen.kt`(✓改 primary、版本 v2.0)、`data/RestTimer.kt`(新增 `total` StateFlow + 通知 setColor)、`res/font/roboto_flex.ttf`(103KB)、`res/drawable/ic_*.xml`(9 个 Material 圆角图标:fitness_center/calendar_month/menu_book/settings/close/check/arrow_back/description/backup)、`app/build.gradle.kts`(**删了 `libs.androidx.compose.icons.extended`**,versionCode 2/2.0)、`_dataset/gen_icon.py`(GREEN→PLATINUM,对勾描边 WHITE→WARM_BLACK)。

**图标自持模式(以后加图标照此办)**:从 google/material-design-icons 仓库按路径 `android/<类别>/<图标名>/materialiconsround/black/res/drawable/round_<名>_24.xml` 下载(raw.githubusercontent 可达),去掉 XML 里的 `android:tint="?attr/colorControlNormal"` 一行,存 `res/drawable/ic_<名>.xml`,代码里 `Icon(painterResource(R.drawable.ic_x))`。类别探测:settings/description/backup/calendar_month/check=action,close/arrow_back/check=navigation,fitness_center=places,menu_book=maps。

v3 验收结果:浅色全屏(空态/选择器 sheet/表格记组/横幅+进度条/历史/PR≈67kg/动作库/详情)✓;深色同套(白金 CTA、白金 1RM 数字、pill 导航)✓;徒手组回归(俯卧撑 15 次无重量,表格列显「—」)✓;杀进程恢复 ✓;CSV(BOM/表头/徒手空串)✓、JSON(null 重量保留)✓;通知白金染色 ✓;抽屉图标白金对勾 ✓;`logcat` 零 FATAL ✓;APK 14.6MB ✓。截图 `_dataset/v3_01~14_*.png`。

### 操作备忘

- **坐标换算**:adb 截图是 900x2000,实机 1080x2400,**实机坐标 = 截图坐标 × 1.2**。
- 底部 tab 实机坐标约:训练 (144,2216) / 历史 (539,2216) / 动作库 (933,2216);训练页 CTA 约 (539,1146);「+ 添加动作」约 (539,497)。
- App 冷启动有 3-5 秒闪屏,截图前 `sleep 3`。
- `android_ui_resolve` 语义查找经常匹配不到中文文本,直接用坐标 tap。
- MCP 工具调用有 **30 秒硬超时**:`android_build_app`/`android_start_emulator` 必超时,**不要用**;改用 bash。短操作(`android_launch_app`/`android_logs`/`android_screenshot`)可用。启动模拟器的可行方式:`(emulator.exe -avd medium_phone -no-snapshot-save > log 2>&1 &)`,然后轮询 `adb shell getprop sys.boot_completed` 直到 `1`(WHPX 生效后约 10-30 秒)。
- 构建命令:`export JAVA_HOME=$(cygpath -w "$(ls -d "/c/Program Files/Eclipse Adoptium/"jdk-17* | head -1)") && cd /e/jianshen/FitnessApp && cmd.exe /c "gradlew.bat assembleDebug --console=plain"`(bash 超时给 600000)。
- **`adb exec-out screencap -p` 会返回陈旧缓存帧**(尤其键盘弹出/收起、界面快速变化后,同一命令反复出同一张旧图)——**视觉验证一律用 MCP `android_screenshot`**(走不同管线,实时)。判断"到底点没点上"用 sqlite(`run-as com.jianshen.fitness sqlite3 ...`)查 DB 最可靠,别信截图。
- **键盘(IME)会移动布局,坐标会飘**:输入框聚焦时整个窗口上移 ~105px,screencap/dump/实况三者时序还不一致。铁律:①先 `adb shell input keyevent 4` 关键盘并 `dumpsys input_method | grep mInputShown` 确认 false;②坐标从**当下**的 uitap dump 取,别复用旧坐标;③带 contentDescription 的按钮(记一组/删除该组)直接语义 tap,uitap.py 已支持 content-desc 解析,且 ADB 路径已写死 `E:\Android\Sdk\platform-tools\adb.exe`(不再依赖 PATH)。
- uitap.py 匹配「同名多节点」时按出现顺序取第 N 个(`python uitap.py "记一组" 1` = 第 2 个命中)——多动作卡都有「记一组」时务必带索引,否则记到第一个动作头上。

---

## 四、踩过的坑(绝对不要再踩)

### 这台精简版系统的坑

1. **winget 源残缺**:搜不到 Gradle.Gradle 等常见包(源更新也没用)。装东西优先官方 zip/镜像,别浪费时间反复试 winget。
2. **pnputil.exe 不存在**(连 System32 里都没有),任何依赖它的驱动安装路线直接死。
3. **AEHD 驱动装不上,别再试**:sdkmanager 能下载 AEHD 2.2.0,`sc create` 服务能建成,但内核拒绝加载(SCM 报"找不到文件"——文件明明存在且签名有效)。官方 `silent_install.bat` 会**假成功**(rundll32 不回传错误、脚本吞掉 sc start 失败,退出码 0 但服务没装)。已定局:用 WHPX,已启用,已重启,`accel-check` 通过。AEHD 残留已清理。
4. **VulnerableDriverBlocklistEnable=1**(微软脆弱驱动拦截列表开启),疑似与坑 3 相关。
5. **网络 TLS 拦截**:`services.gradle.org`(Gradle 发行包)、`repo.maven.apache.org`(Maven Central)握手被掐;`dl.google.com`、`raw.githubusercontent.com`、`mirrors.cloud.tencent.com`、`maven.aliyun.com` 是通的。curl 失败 exit 35 时加 `-k`。

### 构建配置的坑(已固化在项目里,别删)

6. `gradle/wrapper/gradle-wrapper.properties` 的 distributionUrl **必须保持腾讯镜像** `https://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip`——改回官方 URL 构建直接挂(可配合 `--gradle-distribution-url` 生成)。
7. `settings.gradle.kts` 仓库列表**阿里云镜像在前**(`maven.aliyun.com/repository/{gradle-plugin,google,public}`),google()/mavenCentral() 做兜底——顺序反了会去撞被拦截的源。
8. `app/build.gradle.kts` 里 **`buildToolsVersion = "35.0.0"` 必须保留**:AGP 8.7 默认要 34,会触发联网自动下载然后被网络掐死。
9. 同文件里 **`compileOptions`/`kotlinOptions` 的 JVM 17 必须保留**,否则 Java(默认1.8)与 Kotlin/KSP(17)目标不一致直接编译失败。
10. Kotlin 2.0 + Compose:Coil 解码器注册写法是 `add(ImageDecoderDecoder.Factory())` / `add(GifDecoder.Factory())`(**带括号实例化**,不传 Context)。

### Git Bash / 命令行的坑

11. bash heredoc 里的 `'\''`/反斜杠转义会把 python 脚本写坏(出现过 SyntaxError)。python 里处理路径用 `os.path.join(*rel.split('/'))`,不写反斜杠字面量。
12. `reg.exe query` 的键路径带反斜杠会被 Git Bash 路径转换搞坏,前缀 `MSYS_NO_PATHCONV=1`。
13. PowerShell 内联命令用**单引号包整体**(内部 `$` 不会被 bash 展开),且 `2>$null` 这类写在双引号里会被 bash 吃掉。
14. 下载大文件的容错模式:`curl -sL ... || curl -skL ...`(第二条跳过证书校验)。

### App/数据层的坑(已处理,改动时留意)

15. Android 13+ 通知权限必须运行时请求——TrainScreen 里已有 `rememberLauncherForActivityResult`,别移除。
16. Room 查询 `reps / 30.0` 必须带 `.0`(SQLite 整数除法会得 0);PR 的 `MAX(...)` 裸列取行是 SQLite 特性,依赖它。
17. DB 版本升到 3+ 时:当前用 `fallbackToDestructiveMigration()`,用户已有真实训练数据后**必须改成写 Migration**。
18. 模拟器截图(900x2000)与实机(1080x2400)坐标差 1.2 倍,tap 前先 `wm size` 确认。**但 `adb exec-out screencap -p` 出的是 1080x2400 原生图**(900x2000 只是 MCP android_screenshot 的缩放),adb tap 直接用截图坐标。
19. 启动模拟器别用 `android_start_emulator`(MCP 30s 超时),用 bash 后台方式(见操作备忘)。
20. **adb 参数过 Git Bash 会被路径转换**:`adb pull /sdcard/ui.xml` 里的 `/sdcard/...` 被转成 `C:/Program Files (x86)/Git/sdcard/...`,整个会话 `export MSYS_NO_PATHCONV=1` 一了百了(坑 12 的扩展)。
21. **Compose 界面别按截图目测坐标盲点**:文本绘制位置≠点击热区(整行 item 的 bounds 中心在 x=540)。用 `E:\jianshen\_dataset\uitap.py`(uiautomator dump + 按文本 tap,`python uitap.py "文本"` / `--list`)稳得多;`android_ui_resolve` 对中文经常失灵,这个脚本不挑语言。
22. **设置页(以及任何不走 Scaffold 的新页面)必须自己铺背景**:设置页 Column 已加 `.background(MaterialTheme.colorScheme.background)`。因为 AppTheme 继承 `Theme.Material.Light.NoActionBar`,窗口背景写死浅色;没铺底色的页面在深色模式下透出白底,症状像「主题没切换」,极易误判(本次排查走了大弯路:先用日志证明重组与 FitnessTheme 参数都正常,最后靠像素取色 + 训练页正常这一对照锁定)。
23. **手动切换外观时系统栏样式要跟着走**:MainActivity 里用 `DisposableEffect(dark) { enableEdgeToEdge(statusBarStyle/navigationBarStyle = if (dark) SystemBarStyle.dark(...) else SystemBarStyle.light(...)) }`。系统 uiMode 没变,enableEdgeToEdge 的 auto 样式不会自己切,深底会出现深色图标。
24. **状态读取和 `isSystemInDarkTheme()` 这类 composable 调用要放在 when 分支之前**(MainActivity 已如此写),分支里只做纯计算——避免组合位置变化带来的重组歧义。
25. **绕过 Scaffold 的页面拿不到 contentColor**:`LocalContentColor` 默认纯黑,设置页等无色 Text 在深色模式黑字不可读。已在 MainActivity `FitnessTheme` 内根部包 `Surface(color=background, contentColor=onBackground)` 全局修掉——这个 Surface 别删,新页面也不必再自己兜底。
26. **debug 包增量打包会留陈旧条目**:删大依赖后 `ls` 看体积可能不减(磁盘 21.4MB 但 zip 实际内容 14.5MB,本地条目区有孤儿数据)。`gradlew clean assembleDebug` 后才是真实体积。
27. **material-icons-extended 已移除,别再加回**:全量图标 debug 包 +7MB。要新图标走 v3 小节的「图标自持模式」(drawable 自持,~10KB/个)。
28. **Roboto Flex 子集套路**:google/fonts 的 VF 全量 1.8MB;先用 `fontTools.varLib.instancer` 把不用的轴钉死默认值(只留 wght+wdth),再 `fontTools.subset` 裁字符集(Basic Latin + Latin-1 + 常用标点),1.8MB→103KB。fonttools 本机已有(4.51.0)。Compose 侧注意 `FontVariation.weight()` 参数是 **Int**;`Font(resId, variationSettings=...)` 是 ExperimentalTextApi,要 @OptIn。
29. **Room Migration:INTEGER PRIMARY KEY AUTOINCREMENT 必须显式写 NOT NULL**——SQLite 整型主键默认 nullable,Room 按非空 Kotlin 字段校验 schema,缺 NOT NULL 直接 "Migration didn't properly handle" 崩溃(踩过)。写完迁移先在带数据的机器上验:user_version、旧行数、新表存在性。
30. **K2 推断级联毒化**:`var x by remember { mutableStateOf(复杂内联表达式) }` 可能让整个函数的类型解析坏掉,症状是毫不相关的报错(setValue 缺失、forEachIndexed 解析不了、连 Int.compareTo 都报 "operator modifier required")。解法:把复杂初始化拆成 `val initial: List<T> = ...` 再 `var x by remember { mutableStateOf(initial) }`。
31. **DrawScope 里用 nativeCanvas 要写全接收者**:`drawContext.canvas.nativeCanvas.drawText(...)`,光写 `nativeCanvas` 报 receiver mismatch。
32. **IME 开着时 swipe/滚动手势全部被吃**(表现为"页面不滚动"的假象;`keyevent 111` 关不掉 IME,必须用 4):做滚动/拖拽验收前先 `input keyevent 4`,必要时 `dumpsys input_method | grep mInputShown` 确认。

---

## 五、关键决策记录(用户已确认,不要重开讨论)

- 轻量命令行方案装 SDK(无 Android Studio);SDK 放 `E:\Android\Sdk`。
- 数据集:exercises-dataset(GitHub, MIT)筛 43 个经典动作,我翻译的中文名;**媒体(图/GIF)是 © Gym visual 版权,App 内必须保留署名**(详情页 + 设置页"关于"都有,别删)。
- grilling 全按推荐落地:Session 模型、计时器(横幅+通知+90s 默认)、PR 用 Epley、CSV+JSON 导出、深色跟随系统、DB 摧毁式迁移。
- **设计语言:2026-08-30 起为「白金风」(v3),Notion 风(含 1px 描边/小圆角/黑 CTA)已废弃**。这是用户主动重开的已冻结决策,经 grill-me 三轮确认,全部按推荐落地,唯一用户自选点是品牌色=**白金色**(否掉了绿色提案)。合规边界:LibreFit 只参考 token 数值与布局思路、不复制其 GPL 源码。
- v1 的旧"弹窗表单打卡"已整体被 v2 Session 模型**取代并删除**(TrainingLog.kt/LogScreen.kt 已删),历史页由 sessions+set_entries 聚合。
- v4 功能集经 grilling 定案(用户自述痛点「没有图表反馈/没有计划模板/只有力量动作没有跑步拉伸」直接决定范围):数据洞察 + 数据安全 + 轻量计划 + 计时动作进本批;RPE/体重/周期日程/扩容等继续冻结。
- v3 范围纪律:纯 UI 重皮,数据层/导出/计时逻辑不动;图表、Lottie、训练计划、「上次数据」增强继续不做(后续候选,用户未拍板)。
