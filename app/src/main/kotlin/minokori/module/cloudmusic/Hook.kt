package minokori.module.cloudmusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import service.Converter
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object Flag {
    var hasRun = AtomicBoolean(false)
}

private const val CHANNEL_ID = "minokori_cloudmusic"

class MethodHook : XC_MethodHook() {
    override

    fun beforeHookedMethod(param: MethodHookParam) {
        XposedBridge.log("找到要hook的目标类 ${param.result}")

    }

    override fun afterHookedMethod(param: MethodHookParam) {
        super.afterHookedMethod(param)
        convert(param)
    }

    private fun toast(param: MethodHookParam, message: String) {
        val application = param.thisObject as Context
        val context: Context = application.applicationContext
        // 在主线程中显示Toast
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                // 忽略Toast显示异常
            }
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

    private fun convert(param: MethodHookParam) {
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
        for (file in linq) {
            var result = false
            try {
                result = converter.ncm2Mp3(file.absolutePath, dir)
            } catch (e: NoClassDefFoundError) {
                result = true
                XposedBridge.log("转码包直接使用了桌面包, 无法写入专辑图片")
                success += 1
            }
            if (result) {
                if (file.delete()) {
                    success += 1
                } else {
                    XposedBridge.log("删除 ${file.absolutePath}失败.")
                }

            }
        }
        if (success != count) {
            notify(param, "网易云", "转码结束, ${count - success}条转码失败")
        } else {
            notify(param, "网易云", "全部转码成功.")
        }
    }
}