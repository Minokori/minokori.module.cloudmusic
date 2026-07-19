package minokori.module.cloudmusic

// 文件相关逻辑

//import org.jaudiotagger.audio.AudioFileIO
//import org.jaudiotagger.tag.FieldKey
import com.shabinder.jaudiotagger.audio.AudioFileIO
import com.shabinder.jaudiotagger.tag.FieldKey
import org.json.JSONObject
import java.io.File


fun File.select(extension: String): Sequence<File>
{
    if (this.exists() && this.isDirectory) return this.walk().filter { it.isFile && it.extension.equals(extension, ignoreCase = true) }
    throw RuntimeException("路径不合法 ${this.absolutePath}")
}

/**
 * 从网易云歌词缓存文件(无后缀, json格式)中读取 lrc
 */
fun File.getLrc(): String
{
    if (!this.exists() || !this.isFile)
    {
        throw RuntimeException("路径不合法 ${this.absolutePath}")
    }

    val rawLrc = JSONObject(this.readText(Charsets.UTF_8)).getString("lrc")


    val result = StringBuilder()

    rawLrc.lineSequence().forEach { line ->

        val trim = line.trim()

        // 跳过网易云元数据
        if (trim.startsWith("{"))
        {
            return@forEach
        }

        // 读取 lrc 内容
        if (trim.matches(
                Regex(
                    "^\\[\\d{2}:\\d{2}\\.\\d+].*"
                     )
                        )
        )
        {
            result.append(trim)
            result.append('\n')
        }
    }
    return result.toString()
}


/**
 * 为 MP3 附加歌词内容
 * @param lrc lrc格式歌词内容
 * @param update 若设为 true, 即使文件有有效歌词, 也强制更新歌词内容
 * @return 是否写入数据
 */
fun File.attachLrc(lrc: String, update: Boolean = false): Boolean
{
    if (!this.exists() || !this.isFile)
    {
        throw RuntimeException("路径不合法 ${this.absolutePath}")
    }
    val audioFile = if (this.extension.equals("old", ignoreCase = true))
    {
        AudioFileIO.readAs(this, this.name.substringAfterLast(".").let { "mp3" })
    }
    else
    {
        AudioFileIO.read(this)
    }
    val tag = audioFile.tagOrCreateDefault

    if (tag.getFirstField(FieldKey.LYRICS) != null && !tag.getFirstField(FieldKey.LYRICS).isEmpty && !update) return false

    // 删除旧歌词
    tag.deleteField(FieldKey.LYRICS)
    // 写入歌词
    tag.setField(FieldKey.LYRICS, lrc)
    audioFile.commit()
    return true
}

fun File.attachLrc(lrcFile: File, update: Boolean = false): Boolean
{
    return this.attachLrc(lrcFile.getLrc(), update)
}


