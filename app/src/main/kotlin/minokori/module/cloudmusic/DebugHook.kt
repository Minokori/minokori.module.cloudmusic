package minokori.module.cloudmusic

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 标志位, 用于保证方法仅执行一次
 */
object DebugHookLock
{
    var before = AtomicBoolean(false)
    var after = AtomicBoolean(false)
}


class DebugHook : XC_MethodHook()
{
    override fun beforeHookedMethod(param: MethodHookParam?)
    {
        // 确保逻辑仅运行一次
        if (!DebugHookLock.before.compareAndSet(false, true))
        {
            return
        }
        param?.let { XposedBridge.log("找到要hook的目标类 ${it.thisObject}") }
        super.beforeHookedMethod(param)
        param?.let { toast(it, "debugHook 成功注入") }
    }

    override fun afterHookedMethod(param: MethodHookParam?)
    {
        // 确保逻辑仅运行一次
        if (!DebugHookLock.after.compareAndSet(false, true))
        {
            return
        }
        super.afterHookedMethod(param)


    }


}