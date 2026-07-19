package minokori.module.cloudmusic

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge


// 数据库相关逻辑

//region 常量
/**
 * 网易云歌词缓存目录 (以 '/'结尾)
 */
internal const val LrcCacheDir = "/storage/emulated/0/Android/data/com.netease.cloudmusic/files/LrcCache/"

/**
 * 网易云歌词下载目录 (以 '/'结尾)
 */
internal const val LrcDownloadDir = "/storage/emulated/0/Android/data/com.netease.cloudmusic/files/LrcDownload/"

/**
 * 网易云数据库位置 (默认对用户不可见)
 */
@SuppressLint("SdCardPath")
internal const val DatabasePath = "/data/user/0/com.netease.cloudmusic/databases/cloudmusic.db"

/**
 * 网易云默认音乐下载路径 (以 '/'结尾)
 */
internal const val MusicDir = "/storage/emulated/0/Download/netease/cloudmusic/Music/"
//endregion

internal fun XC_MethodHook.readDatabase(param: XC_MethodHook.MethodHookParam): SQLiteDatabase?
{
    try
    {
        val db = SQLiteDatabase.openDatabase(
            DatabasePath, null, SQLiteDatabase.OPEN_READWRITE
                                            )
        return db
    }
    catch (e: Exception)
    {
        XposedBridge.log("打开本地下载数据库失败")
        return null
    }
}

/**
 * 清除 cloudmusic.db 中一张表的内容, 并重置自增 id
 * @param table 表名称
 */
internal fun SQLiteDatabase.clear(table: String = "local_delete"): SQLiteDatabase
{
    this.execSQL("DELETE FROM $table")
    this.execSQL(
        "DELETE FROM sqlite_sequence WHERE name = ?", arrayOf(table)
                )
    return this
}

/**
 * 在 cloudmusic:local_track 中根据音乐path查找对应的id
 * @param path 音乐文件的路径
 * @return 音乐的 id, 若没有找到对应条目, 返回 null
 */
internal fun SQLiteDatabase.findMusicIdByPath(path: String): Long?
{
    val cursor = this.rawQuery(
        "SELECT match_id FROM local_track WHERE path = ?", arrayOf(path)
                              )
    cursor.use {
        if (it.moveToFirst())
        {
            val id = it.getColumnIndexOrThrow("match_id")
            if (id < 0)
            {
                return null
            }
            return it.getLong(id)
        }
    }
    return null
}

/**
 * 在 cloudmusic:local_track 中根据音乐 id 查找对应的 path
 * @param id 音乐文件的 id
 * @return 音乐的 path, 若没有找到对应条目, 返回 null
 */
internal fun SQLiteDatabase.findMusicPathById(id: Long): String?
{
    val cursor = this.rawQuery(
        "SELECT path FROM local_track WHERE match_id = ?", arrayOf(id.toString())
                              )
    cursor.use {
        if (it.moveToFirst())
        {
            val id = it.getColumnIndexOrThrow("path")
            if (id < 0)
            {
                return null
            }
            return it.getString(id)
        }
    }
    return null
}

internal fun SQLiteDatabase.listMusicIdPath(): List<Pair<Long, String>>
{
    val result = mutableListOf<Pair<Long, String>>()

    this.rawQuery(
        """
        SELECT match_id, path
        FROM local_track
        WHERE match_id IS NOT NULL
          AND path IS NOT NULL
        """.trimIndent(), null
                 ).use { cursor ->

        val matchIdIndex = cursor.getColumnIndexOrThrow("match_id")
        val pathIndex = cursor.getColumnIndexOrThrow("path")

        while (cursor.moveToNext())
        {
            val matchId = cursor.getLong(matchIdIndex)
            val path = cursor.getString(pathIndex)

            result += matchId to path
        }
    }

    return result
}

internal fun SQLiteDatabase.updateMusicPath(paths: MutableList<Pair<String, String>>): SQLiteDatabase
{

    for ((ncm, format) in paths)
    {

        val mp3 = ncm.replace(".ncm", ".${format}")
        this.execSQL("UPDATE local_track SET path = ? WHERE path = ?", arrayOf(mp3, ncm))
    }
    return this
}