package minokori.module.cloudmusic


import android.app.Application
import android.os.Build.MODEL
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Main : IXposedHookLoadPackage
{

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam)
    {
        // 寻找作用域
        if (!isCloudMusic(lpparam)) return
        val hook = MethodHook()


        XposedHelpers.findAndHookMethod(Application::class.java, "onCreate", hook)

        // TODO 使用 isDebug 实现, 不要硬编码机器类型.
        // 在mumu模拟器上测试时注入 debughook
        if (MODEL != "BVL-AN00") return
        val debughook = DebugHook()
        XposedHelpers.findAndHookMethod(Application::class.java, "onCreate", debughook)


    }


    private fun isCloudMusic(lpparam: XC_LoadPackage.LoadPackageParam): Boolean
    {

        if (lpparam.packageName != "com.netease.cloudmusic") return false
        if (lpparam.processName != lpparam.packageName)
        {
            return false
        }
        return true
    }


}