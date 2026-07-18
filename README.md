# 一个自动将网易云 ncm 格式解码为 mp3 的 Lsposed 模块

hook 了网易云的主进程, 打开网易云时, 会扫描网易云默认下载路径下的 `ncm` 文件, 将其解码为 `mp3` 文件, 并删除源 `ncm` 文件

## 效果预览

![screeshot](./assets/screenshot.png)

## 已解决问题

### 版本1.1

+   ~~使用了 [NCM2MP3](https://github.com/charlotte-xiao/NCM2MP3), 该库为桌面库, 引用了 `javax.imageio.ImageIO` 包用于解析封面. 由于该包不能在 Android 平台上运行, 因此解析时会抛出 `java.lang.NoClassDefFoundError` 异常. 现在仅仅是忽略该异常, 导致转码后的 `mp3` 文件没有封面.~~

    >   ~~下一步开发会尝试分支原始库用 Android Runtime API 替代原始逻辑~~
    
    使用 Android 原生 API 代替了桌面端 java API.

## 已知问题

+   解码后, 本地音乐页面会展示重复歌曲, 未能及时将 ncm 信息从网易云的本地歌曲信息数据库中删除.

    >   添加更新本地文件为 mp3 后, 尝试更新网易云内部数据库信息以解决该问题. 目前仅更新了 `cloudmusic.db:local_track` 表, 未能达到预期效果.

## 第三方库和许可

+   [NCM2MP3](https://github.com/charlotte-xiao/NCM2MP3) 主要转码逻辑实现
+   [ALSModuleDemo](https://github.com/1750-shocker/ALSModuleDemo) 项目框架, [MIT](https://github.com/1750-shocker/ALSModuleDemo/blob/main/LICENSE)
