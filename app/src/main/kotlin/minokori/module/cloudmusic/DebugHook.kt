package minokori.module.cloudmusic

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.File

class DebugHook : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam?) {
        super.beforeHookedMethod(param)
        param?.let { toast(it, "debugHook 成功注入") }
    }

    override fun afterHookedMethod(param: MethodHookParam?) {
        super.afterHookedMethod(param)

        var dbs = getDatabases()
        if (dbs == null) {
            XposedBridge.log("找不到db文件")
            return
        } else {
            exportDatabases(dbs)
        }

    }


    private fun exportDatabases(dbs: List<File>) {
        for (dbFile in dbs) {
            try {
                var db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                db.execSQL("VACUUM INTO '/storage/emulated/0/Download/${dbFile.name}'")
            } catch (e: SQLiteException) {
                XposedBridge.log(e)
            }
        }

    }

    private fun getDatabases(): List<File>? {
        try {
            var dbs = File("/data/user/0/com.netease.cloudmusic/databases/")
                .walk()
                .filter { it.isFile && it.extension == "db" }
                .toList()
            return dbs

        } catch (e: Exception) {
            XposedBridge.log("打开本地下载数据库失败")
            return null
        }
    }


    private fun toast(param: MethodHookParam, message: String) {
        val application = param.thisObject as Application
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
}