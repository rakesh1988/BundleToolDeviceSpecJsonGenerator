package net.imknown.android.bundletooldevicespecjsongenerator.ui.home

import android.app.ActivityManager
import android.content.Context
import android.content.pm.FeatureInfo
import android.content.res.Resources
import android.opengl.GLSurfaceView
import android.os.Build
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.imknown.android.bundletooldevicespecjsongenerator.DeviceSpecJson
import net.imknown.android.bundletooldevicespecjsongenerator.Event
import net.imknown.android.bundletooldevicespecjsongenerator.MyApplication
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HomeViewModel : ViewModel() {

    companion object {
        private const val GL_ES_VERSION_2 = 2
    }

    private val _addGlSurfaceViewEvent = MutableLiveData<Event<GLSurfaceView>>()
    val addGlSurfaceViewEvent: LiveData<Event<GLSurfaceView>> = _addGlSurfaceViewEvent

    private val _removeGlSurfaceViewEvent = MutableLiveData<Event<GLSurfaceView>>()
    val removeGlSurfaceViewEvent: LiveData<Event<GLSurfaceView>> = _removeGlSurfaceViewEvent

    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result

    private var openGlEsVersion = GL_ES_VERSION_2

    fun fetch() = viewModelScope.launch {
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

        _result.value =
            """
            |deviceSpecJson = ${Json { prettyPrint = true }.encodeToString(deviceSpecJson)}
            |
            |configuration = ${Resources.getSystem().configuration}
            """.trimMargin()
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
    private fun fetchDeviceFeatures(): List<String> = MyApplication.instance.packageManager
        .systemAvailableFeatures
        .sortedBy(FeatureInfo::name)
        .map {
            when {
                it.name == null -> {
                    openGlEsVersion = it.glEsVersion.substringBefore(".").toInt()
                    "${it::reqGlEsVersion.name}=0x${Integer.toHexString(it.reqGlEsVersion)}"
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && it.version != 0 -> {
                    "${it.name}=${it.version}"
                }
                else -> it.name
            }
        }

    // dumpsys SurfaceFlinger | grep 'SurfaceFlinger global state:' -A 4
    private suspend fun fetchGlExtensions(): List<String> = suspendCoroutine {
        // FIXME: Memory leak!
        GLSurfaceView(MyApplication.instance).apply {
            setEGLContextClientVersion(openGlEsVersion)
            setRenderer(object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
                    val glExtensions: List<String> =
                        gl.glGetString(GL10.GL_EXTENSIONS).trim().split(" ")
                    it.resume(glExtensions)

                    _removeGlSurfaceViewEvent.postValue(Event(this@apply))
                }

                override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
                }

                override fun onDrawFrame(gl: GL10) {
                }
            })

            _addGlSurfaceViewEvent.value = Event(this@apply)
        }
    }

    private fun fetchOpenGlEsVersionFromActivityManager(): Float {
        val activityManager =
            MyApplication.instance.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configInfo = activityManager.deviceConfigurationInfo
        return configInfo.glEsVersion.toFloat()
    }

    // wm density
    private fun fetchScreenDensity(): Int = Resources.getSystem().configuration.densityDpi

    // getprop ro.build.version.sdk
    private fun fetchSdkVersion(): Int = Build.VERSION.SDK_INT
}