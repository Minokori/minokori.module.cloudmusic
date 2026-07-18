package minokori.module.cloudmusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import charlottexiao.ncm2mp3.service.Converter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object Flag {
    var hasRun = AtomicBoolean(false)
}

private const val CHANNEL_ID = "minokori_cloudmusic"
private const val LrcCache = "/storage/emulated/0/Android/data/com.netease.cloudmusic/files/LrcCache/"
private const val LrcDownload = "/storage/emulated/0/Android/data/com.netease.cloudmusic/files/LrcDownload/"

class MethodHook : XC_MethodHook() {
    override

    fun beforeHookedMethod(param: MethodHookParam) {
        XposedBridge.log("找到要hook的目标类 ${param.thisObject}")

    }

    override fun afterHookedMethod(param: MethodHookParam) {
        super.afterHookedMethod(param)
        var db = readDatabase(param)
        //ncm2MP3
        convert(param, db)

        //给mp3添加歌词

        if (db is SQLiteDatabase) {
            clearDatabase(db)
            add(param, db)
            db.close()
        }

    }

    private fun notify(param: MethodHookParam, title: String, message: String) {
        val context = (param.thisObject as Context).applicationContext

        Handler(Looper.getMainLooper()).post {
            try {
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Xposed Module",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    manager.createNotificationChannel(channel)
                }

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_more)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .build()

                manager.notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: Exception) {
                XposedBridge.log(e)
            }
        }
    }

    private fun convert(param: MethodHookParam, db: SQLiteDatabase?) {
        if (!Flag.hasRun.compareAndSet(false, true)) {
            return
        }

        val dir = "/storage/emulated/0/Download/netease/cloudmusic/Music"
        val root = File(dir)
        val converter = Converter()
        if (!root.exists() || !root.isDirectory) {
            return
        }
        val linq = root.walk().filter { it.isFile && it.extension == "ncm" }.toList()
        val count = linq.count().toInt()
        if (count <= 0) {
            notify(param, "网易云", "所有 ncm 文件已被转码为 mp3 格式!")
            return
        }
        notify(param, "网易云", "开始将 ${count} 条 ncm 转码为 mp3")
        var success = 0

        // 解码
        for (file in linq) {

            val r = converter.ncm2Mp3(file.absolutePath, dir)


            //结果统计
            if (r.result) {
                success += 1
                //删除源文件
                if (!file.delete()) XposedBridge.log("删除 ${file.absolutePath}失败.")
                // 更新db
                if (db is SQLiteDatabase) {
                    val mp3file = file.absolutePath.replace(".ncm", ".${r.format}")
                    db.execSQL(
                        "UPDATE local_track SET path = ? WHERE path = ?",
                        arrayOf(mp3file, file.absolutePath)
                    )
                }
            } else {
                XposedBridge.log("${file.name} 转码失败.")
            }
        }


        //输出结果
        if (success != count) {
            notify(param, "网易云", "转码结束, ${count - success}条转码失败")
        } else {
            notify(param, "网易云", "全部转码成功.")
        }
    }


    private fun readDatabase(param: MethodHookParam): SQLiteDatabase? {
        try {
            var db = SQLiteDatabase.openDatabase(
                "/data/user/0/com.netease.cloudmusic/databases/cloudmusic.db",
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            return db
        } catch (e: Exception) {
            XposedBridge.log("打开本地下载数据库失败")
            return null
        }
    }

    private fun clearDatabase(db: SQLiteDatabase) {
        db.execSQL("DELETE FROM local_delete")

        db.execSQL(
            "DELETE FROM sqlite_sequence WHERE name = ?",
            arrayOf("local_delete")
        )
    }

    private fun add(param: MethodHookParam, db: SQLiteDatabase) {
        var count = 0
        File("/storage/emulated/0/Download/netease/cloudmusic/Music")
            .walk()
            .filter { it.isFile && it.extension != "ncm" }
            .forEach {
                var id = findMusicId(db, it.absolutePath)
                if (id != null) {
                    addLrc(it.absolutePath, id)
                    count++
                }
            }
        if (count != 0) {
            notify(param, "网易云:添加MP3", "成功给${count}条mp3内嵌歌词!")
        }

    }

    private fun addLrc(mp3Path: String, musicId: Long) {
        ///storage/emulated/0/Android/data/com.netease.cloudmusic/files/
        // LrcCache, LrcDownload
        var lrcFile: File? = null
        if (File("${LrcCache}${musicId}").exists()) {
            lrcFile = File("${LrcCache}${musicId}")
        }
        if (File("${LrcDownload}${musicId}").exists())
            lrcFile = File("${LrcDownload}${musicId}")
        if (lrcFile == null) {
            return
        }
        embedLrcToMp3(mp3Path, lrcFile)

    }

    private fun findMusicId(db: SQLiteDatabase, path: String): Long? {
        val cursor = db.rawQuery(
            "SELECT match_id FROM local_track WHERE path = ?",
            arrayOf(path)
        )

        cursor.use {
            if (it.moveToFirst()) {
                return it.getLong(it.getColumnIndexOrThrow("match_id"))
            }
        }

        return null
    }


    /**
     * 将网易云歌词写入 MP3 USLT 标签
     *
     * @param mp3Path MP3文件路径
     * @param jsonPath 网易云歌词json路径
     */
    private fun embedLrcToMp3(
        mp3Path: String,
        jsonPath: File
    ) {
        val lrc = extractStandardLrc(jsonPath)

        if (lrc.isBlank()) {
            return
        }

        val mp3 = File(mp3Path)

        val audioFile = AudioFileIO.read(mp3)

        val tag = audioFile.tagOrCreateAndSetDefault


        if (tag.getFields(FieldKey.LYRICS).isNotEmpty()) {
            return
        }
        // 删除旧歌词
        tag.deleteField(FieldKey.LYRICS)

        // 写入歌词
        tag.setField(
            FieldKey.LYRICS,
            lrc
        )

        audioFile.commit()
    }


    /**
     * 从网易云json中提取标准LRC
     */
    private fun extractStandardLrc(
        jsonFile: File
    ): String {

        val json = jsonFile
            .readText(Charsets.UTF_8)

        val root = JSONObject(json)

        val rawLrc = root.getString("lrc")


        val result = StringBuilder()

        rawLrc
            .lineSequence()
            .forEach { line ->

                val trim = line.trim()

                /*
                 * 网易云元数据:
                 *
                 * {"t":0,"c":[{"tx":"作词:"}]}
                 *
                 * 直接跳过
                 */
                if (trim.startsWith("{")) {
                    return@forEach
                }


                /*
                 * 保留:
                 *
                 * [00:03.95]I'm a dreamer
                 *
                 */
                if (trim.matches(
                        Regex(
                            "^\\[\\d{2}:\\d{2}\\.\\d+].*"
                        )
                    )
                ) {
                    result.append(trim)
                    result.append('\n')
                }
            }


        return result.toString()
    }
}