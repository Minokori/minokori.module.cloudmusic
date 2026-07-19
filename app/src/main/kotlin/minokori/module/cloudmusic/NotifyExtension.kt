package minokori.module.cloudmusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

// 通知相关的逻辑
/**
 * 通知通道
 */
internal const val CHANNEL = "minokori_module_cloudmusic"

/**
 * 状态栏通知
 * @param param xposed hook 参数
 * @param title 通知标题. 传入后会用 网易云:{title} 的格式显示
 * @param message 通知内容.
 */
fun XC_MethodHook.notify(param: XC_MethodHook.MethodHookParam, title: String, message: String)
{

    val context = (param.thisObject as Context).applicationContext

    Handler(Looper.getMainLooper()).post {
        try
        {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL, "Lsposed 通知", NotificationManager.IMPORTANCE_DEFAULT
                                             )
            manager.createNotificationChannel(channel)


            val notification =
                NotificationCompat.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle("网易云:${title}").setContentText(message)
                    .setAutoCancel(true).build()

            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
        catch (e: Exception)
        {
            XposedBridge.log(e)
        }
    }
}

/**
 * toast 通知
 * @param param xposed hook 参数
 * @param message 通知内容.
 */
fun XC_MethodHook.toast(param: XC_MethodHook.MethodHookParam, message: String)
{
    val application = param.thisObject as Application
    val context: Context = application.applicationContext
    // 在主线程中显示Toast
    Handler(Looper.getMainLooper()).post {
        try
        {
            Toast.makeText(
                context, message, Toast.LENGTH_SHORT
                          ).show()
        }
        catch (e: Exception)
        {
            // 忽略Toast显示异常
        }
    }
}