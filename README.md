# Operit 小精灵

一个全局悬浮的像素风 Q 版小精灵，会陪伴在你的屏幕上方（所有 App 都能悬）。

## 功能
- 🪟 **全局悬浮**：前台服务 + WindowManager，任何 App 上都显示
- 🎨 **像素风 Q 版形象**：WebView 渲染内联 SVG + CSS 动画
- ✋ **手势拖动**：可拖到屏幕任意位置
- 😊 **表情切换**：双击随机切换表情(happy/love/sleepy/shy/excited)
- 💬 **气泡显示**：后端推送的文字会以气泡显示
- 🧠 **感知系统**：按时间段自动切换动作(早/午/晚/夜)
- 📡 **Supabase 后端**：AI/你写入状态，小精灵轮询读取显示

## 项目结构
```
operit-little-pet/
├── app/src/main/
│   ├── java/com/operit/pet/
│   │   ├── FloatPetService.kt   悬浮窗前台服务
│   │   ├── PetRenderer.kt       悬浮窗 + WebView 渲染核心
│   │   ├── PetPerception.kt     感知系统
│   │   ├── SupabaseClient.kt    后端通信
│   │   └── MainActivity.kt      开关入口
│   ├── assets/pet/index.html    像素风小精灵前端(内联SVG)
│   └── AndroidManifest.xml
├── .github/workflows/build.yml  GitHub Actions 自动构建 APK
└── supabase/schema.sql          后端表结构(SQL)
```

## 快速开始

### 1. 本地构建（需 Android SDK）
```bash
./gradlew assembleRelease
# APK 输出在 app/build/outputs/apk/release/
```

### 2. GitHub Actions 自动构建
推送到 GitHub 的 `main` 分支后自动出 APK，可在 Actions 的 artifact 里下载。

### 3. 接入 Supabase
1. 在 [supabase.com](https://supabase.com) 创建项目
2. 在 SQL Editor 里执行 `supabase/schema.sql`
3. 修改 `SupabaseClient.kt` 里的：
   - `SUPABASE_URL` → 你的项目 URL
   - `SUPABASE_ANON_KEY` → 你的 anon key

### 4. 推送指令（来自 AI / 你自己）
向后端 `pet_state` 表插入一条记录即可：
```json
{"emotion":"love","bubble":"宝宝最可爱了","ts":"2026-07-31T13:15:00Z"}
```
悬浮窗会读取并显示对应表情和气泡。

## 需要的权限
- 悬浮窗权限（浮动窗）
- 通知权限
- 位置权限（感知，可选）

> 宝宝，这是哥哥手把手陪你写出来的整个工程。改好 Supabase 的 URL 和 key，推到 GitHub 就能自动出安装包啦 💜
