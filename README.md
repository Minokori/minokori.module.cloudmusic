# 一个自动将网易云 ncm 格式解码为 mp3 的 Lsposed 模块

hook 了网易云的主进程, 打开网易云时, 会扫描网易云默认下载路径下的 `ncm` 文件, 将其解码为 `mp3` 文件, 并删除源 `ncm` 文件

## 效果预览

![screeshot](./assets/screenshot.png)

## 新增功能

### 版本1.1

+   为生成的mp3内嵌歌词

    >   扫描网易云的 `LrcCache` 和 `LrcDownload` 文件夹, 根据歌曲 id 为mp3 内嵌歌词
    >
    >   *可能存在无法写入的问题, 需要网易云具有 `WRITE_MEDIA_AUDIO` 权限, 该权限可以通过 APPOPS 赋予*

## 已解决问题

### 版本1.1

+   ~~使用了 [NCM2MP3](https://github.com/charlotte-xiao/NCM2MP3), 该库为桌面库, 引用了 `javax.imageio.ImageIO` 包用于解析封面. 由于该包不能在 Android 平台上运行, 因此解析时会抛出 `java.lang.NoClassDefFoundError` 异常. 现在仅仅是忽略该异常, 导致转码后的 `mp3` 文件没有封面.~~

    >   ~~下一步开发会尝试分支原始库用 Android Runtime API 替代原始逻辑~~
    
    使用 Android 原生 API 代替了桌面端 java API.
    
    

+   ~~解码后, 本地音乐页面会展示重复歌曲, 未能及时将 ncm 信息从网易云的本地歌曲信息数据库中删除.~~

    >   ~~添加更新本地文件为 mp3 后, 尝试更新网易云内部数据库信息以解决该问题. 目前仅更新了 `cloudmusic.db:local_track` 表, 未能达到预期效果.~~

    同时更新了 `cloudmusic.db:local_track` 和 `cloudmusic.db:local_delete` 两张表.

## 已知问题

+   权限问题. 可能出现无法为 `mp3` 内嵌歌词的问题, 是由于网易云没有写入权限导致的. 插件现在会检查网易云是否有 `WRITE_MEDIA_AUDIO` 或 `WRITE_EXTERNAL_STORAGE` 权限, 并提示用户给予该权限.

## 第三方库和许可

+   [NCM2MP3](https://github.com/charlotte-xiao/NCM2MP3) 主要转码逻辑实现
+   [ALSModuleDemo](https://github.com/1750-shocker/ALSModuleDemo) 项目框架, [MIT](https://github.com/1750-shocker/ALSModuleDemo/blob/main/LICENSE)
