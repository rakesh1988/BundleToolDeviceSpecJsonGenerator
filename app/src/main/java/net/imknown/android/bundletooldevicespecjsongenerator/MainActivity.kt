package net.imknown.android.bundletooldevicespecjsongenerator

import android.content.pm.FeatureInfo
import android.content.res.Resources
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {
    private lateinit var navView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        fetch()
    }

    private fun fetch() = lifecycleScope.launch {
        val supportedAbis = fetchSupportedAbis()
        val supportedLocales = fetchSupportedLocales()
        val deviceFeatures = fetchDeviceFeatures()
        val glExtensions = fetchGlExtensions()
        val screenDensity = fetchScreenDensity()
        val sdkVersion = fetchSdkVersion()

        val deviceSpecJson = DeviceSpecJson(
            supportedAbis,
            supportedLocales,
            deviceFeatures,
            glExtensions,
            screenDensity,
            sdkVersion
        )

        Log.e("zzz", "deviceSpecJson = ${Json.encodeToString(deviceSpecJson)}")

        Log.e("zzz", "configuration = ${Resources.getSystem().configuration}")
    }

    // getprop ro.product.cpu.abilist
    private fun fetchSupportedAbis(): List<String> = Build.SUPPORTED_ABIS.toList()

    // dumpsys window | grep Configuration
    private fun fetchSupportedLocales(): List<String> = mutableListOf<String>().apply {
        val adjustedDefault = LocaleListCompat.getAdjustedDefault()
        for (i in 0 until adjustedDefault.size()) {
            val locale = adjustedDefault[i]
            add("${locale.language}-${locale.country}")
        }
    }

    // pm list features
    private fun fetchDeviceFeatures(): List<String> = packageManager.systemAvailableFeatures
        .sortedBy(FeatureInfo::name)
        .map {
            when {
                it.name == null -> "${it::reqGlEsVersion.name}=0x${Integer.toHexString(it.reqGlEsVersion)}"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && it.version != 0 -> "${it.name}=${it.version}"
                else -> it.name
            }
        }

    private external fun gl(): String

    // dumpsys SurfaceFlinger | grep 'SurfaceFlinger global state:' -A 4
    private suspend fun fetchGlExtensions(): List<String> = suspendCoroutine {
        GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setRenderer(object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
                    val glExtensions: List<String> =
                        gl.glGetString(GL10.GL_EXTENSIONS).trim().split(" ")
                    it.resume(glExtensions)

                    runOnUiThread { navView.removeView(this@apply) }
                }

                override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
                }

                override fun onDrawFrame(gl: GL10) {
                }
            })

            navView.addView(this)
        }
    }

    // wm density
    private fun fetchScreenDensity(): Int = Resources.getSystem().configuration.densityDpi

    // getprop ro.build.version.sdk
    private fun fetchSdkVersion(): Int = Build.VERSION.SDK_INT
}