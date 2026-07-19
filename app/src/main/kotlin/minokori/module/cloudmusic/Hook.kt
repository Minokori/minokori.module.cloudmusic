package minokori.module.cloudmusic

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import charlottexiao.ncm2mp3.service.Converter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 标志位, 用于保证方法仅执行一次
 */
object MethodHookLock
{
    var before = AtomicBoolean(false)
    var after = AtomicBoolean(false)
}


class MethodHook : XC_MethodHook()
{
    override fun beforeHookedMethod(param: MethodHookParam)
    {
        // 确保逻辑仅运行一次
        if (!MethodHookLock.before.compareAndSet(false, true))
        {
            return
        }
        XposedBridge.log("找到要hook的目标类 ${param.thisObject}")

    }

    override fun afterHookedMethod(param: MethodHookParam)
    {
        // 确保逻辑仅运行一次
        if (!MethodHookLock.after.compareAndSet(false, true))
        {
            return
        }
        val application = param.thisObject as Application
        if (!hasCloudMusicWritePermission(application))
        {
            notify(param, "权限", "请开启网易云音乐写入媒体数据的权限, 否则无法为mp3文件嵌入歌词")
            openAppPermissionSettings(application)
        }

        super.afterHookedMethod(param)

        //转码, 必执行的逻辑
        val converted = convert(param)

        //处理数据库, 在读取失败时不执行
        val db = readDatabase(param)
        //给mp3添加歌词
        db?.updateMusicPath(converted)//更新 cloudmusic.db:local_track[path] 中储存的音乐文件信息
            ?.clear()// 清除 cloudmusic.db:local_delete 表
            ?.mapWithLyrics(param) //根据 网易云缓存/下载为 mp3 嵌入歌词元数据
            ?.close()// 关闭数据库


    }


    private fun convert(param: MethodHookParam): MutableList<Pair<String, String>>
    {
        val converted = mutableListOf<Pair<String, String>>()


        val ncmFiles = File(MusicDir).select("ncm").toList()
        val count = ncmFiles.count()

        // 若没有ncm文件存在
        if (count <= 0)
        {
            notify(param, "转码", "所有 ncm 文件已被转码为 mp3 格式!")
            return converted
        }

        //开始转码
        notify(param, "转码", "开始将 $count 条 ncm 转码为 mp3")


        val converter = Converter()
        // 解码
        for (file in ncmFiles)
        {

            val resultTuple = converter.ncm2Mp3(file.absolutePath, MusicDir)


            //结果统计
            if (resultTuple.result)
            {

                converted += file.absolutePath to resultTuple.format
                //删除源文件
                if (!file.delete()) XposedBridge.log("删除 ${file.absolutePath}失败.")

            }
            else
            {
                XposedBridge.log("${file.name} 转码失败.")
            }
        }


        //输出结果
        if (converted.count() != count)
        {
            notify(param, "转码", "转码结束, ${count - converted.count()}条转码失败")
        }
        else
        {
            notify(param, "转码", "全部转码成功.")
        }

        return converted
    }

    private fun SQLiteDatabase.mapWithLyrics(param: MethodHookParam): SQLiteDatabase
    {
        var count = 0
        val dirs = listOf(LrcCacheDir, LrcDownloadDir)
        for (dir in dirs)
        {
            File(dir).walk().filter { it.isFile }.forEach {
                val mp3Path = this.findMusicPathById(it.nameWithoutExtension.toLong())

                if (mp3Path != null)
                {

                    val result = File(mp3Path).attachLrc(it)
                    if (result) count++
                }
            }
            if (count != 0)
            {
                notify(param, "内嵌歌词", "成功给 $count 条 mp3 内嵌歌词!")
            }

        }
        return this

    }
}