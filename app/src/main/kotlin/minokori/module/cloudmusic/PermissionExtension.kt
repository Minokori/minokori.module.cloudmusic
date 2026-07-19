package minokori.module.cloudmusic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

private const val WRITE_MEDIA_AUDIO = "android.permission.WRITE_MEDIA_AUDIO"
fun Context.hasWriteAudioPermission(): Boolean
{
    return if (Build.VERSION.SDK_INT >= 33)
    {
        checkSelfPermission(
            WRITE_MEDIA_AUDIO
                           ) == PackageManager.PERMISSION_GRANTED
    }
    else
    {
        checkSelfPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
                           ) == PackageManager.PERMISSION_GRANTED
    }
}

fun hasCloudMusicWritePermission(context: Context): Boolean
{

    val packageManager = context.packageManager

    val packageInfo = packageManager.getPackageInfo(
        "com.netease.cloudmusic", PackageManager.GET_PERMISSIONS
                                                   )

    val requested = packageInfo.requestedPermissions ?: return false

    val granted = packageInfo.requestedPermissionsFlags

    requested.forEachIndexed { index, permission ->

        if (permission == WRITE_MEDIA_AUDIO || permission == Manifest.permission.WRITE_EXTERNAL_STORAGE)
        {
            return granted?.get(index) != 0
        }
    }

    return false
}

fun openAppPermissionSettings(context: Context)
{

    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                       )

    intent.data = Uri.parse(
        "package:com.netease.cloudmusic"
                           )

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    context.startActivity(intent)
}