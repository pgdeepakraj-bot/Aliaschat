package com.example.ui.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.example.BuildConfig
import com.example.R
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

class ZegoCallActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zego_call)

        val callID = intent.getStringExtra(EXTRA_CALL_ID) ?: "alias_${System.currentTimeMillis()}"
        val userID = intent.getStringExtra(EXTRA_USER_ID) ?: "alias_user_${System.currentTimeMillis()}"
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: userID
        val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customAppId = prefs.getString(KEY_CUSTOM_APP_ID, null)?.trim()?.toLongOrNull()
        val customAppSign = prefs.getString(KEY_CUSTOM_APP_SIGN, null)?.trim()?.ifBlank { null }

        val appID = customAppId ?: try {
            BuildConfig.ZEGOCLOUD_APP_ID.trim().toLong()
        } catch (_: Exception) {
            123456789L
        }

        val appSign = customAppSign ?: BuildConfig.ZEGOCLOUD_APP_SIGN.trim().ifBlank {
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        }

        val config = if (isVideo) {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
        } else {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall().apply {
                turnOnCameraWhenJoining = false
            }
        }

        config.leaveCallListener = ZegoUIKitPrebuiltCallFragment.LeaveCallListener {
            finish()
        }

        try {
            val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                appID,
                appSign,
                userID.replace("@", "").trim(),
                userName.replace("@", "").trim(),
                callID,
                config
            )

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitNow()
        } catch (e: Exception) {
            Log.e("ZegoCallActivity", "Failed to initialize ZegoCallFragment", e)
            Toast.makeText(this, "Could not start ZEGOCLOUD Call: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    companion object {
        const val PREFS_NAME = "zego_config"
        const val KEY_CUSTOM_APP_ID = "custom_app_id"
        const val KEY_CUSTOM_APP_SIGN = "custom_app_sign"

        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_IS_VIDEO = "extra_is_video"

        fun getActiveAppId(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_APP_ID, null)?.trim()?.toLongOrNull()
            return custom ?: try {
                BuildConfig.ZEGOCLOUD_APP_ID.trim().toLong()
            } catch (_: Exception) {
                123456789L
            }
        }

        fun getActiveAppSign(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_APP_SIGN, null)?.trim()?.ifBlank { null }
            return custom ?: BuildConfig.ZEGOCLOUD_APP_SIGN.trim().ifBlank {
                "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
            }
        }

        fun saveCredentials(context: Context, appId: String, appSign: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_CUSTOM_APP_ID, appId.trim())
                .putString(KEY_CUSTOM_APP_SIGN, appSign.trim())
                .apply()
        }

        fun clearCustomCredentials(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }

        fun startVoiceCall(context: Context, callId: String, userId: String, userName: String) {
            val intent = Intent(context, ZegoCallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_IS_VIDEO, false)
            }
            context.startActivity(intent)
        }

        fun startVideoCall(context: Context, callId: String, userId: String, userName: String) {
            val intent = Intent(context, ZegoCallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_IS_VIDEO, true)
            }
            context.startActivity(intent)
        }
    }
}
