package com.macro.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.macro.app.capture.ScreenCapture
import com.macro.app.databinding.ActivityMainBinding
import com.macro.app.manager.ActionListManager
import com.macro.app.registry.ExecutorRegistry
import com.macro.app.runner.MacroRunner
import com.macro.app.service.MacroService
import com.macro.app.ui.ActionListFragment
import com.macro.app.ui.ImageSearchFragment
import com.macro.app.ui.ConditionFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var macroRunner: MacroRunner? = null

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var screenCapture: ScreenCapture? = null

    companion object {
        private const val TAG = "MainActivity"
        const val REQUEST_SCREEN_CAPTURE = 1001
        var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        instance = this

        ActionListManager.initialize(this)
        Log.i(TAG, "ActionListManager 초기화 완료")

        setupUI()
        checkAccessibilityService()

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        if (savedInstanceState == null) {
            loadFragment(ActionListFragment())
        }
    }

    private fun setupUI() {
        binding.btnStart.setOnClickListener {
            startMacro()
        }

        binding.btnStop.setOnClickListener {
            stopMacro()
        }

        binding.btnActionList.setOnClickListener {
            loadFragment(ActionListFragment())
        }

        binding.btnImageSearch.setOnClickListener {
            loadFragment(ImageSearchFragment())
        }

        binding.btnCondition.setOnClickListener {
            loadFragment(ConditionFragment())
        }
    }

    fun requestScreenCapturePermission() {
        try {
            Log.i(TAG, "화면 캡처 권한 요청 시작")

            if (MacroService.instance == null) {
                Toast.makeText(
                    this,
                    "먼저 접근성 서비스를 켜주세요\n설정 > 접근성 > MacroApp",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, REQUEST_SCREEN_CAPTURE)
        } catch (e: Exception) {
            Log.e(TAG, "화면 캡처 권한 요청 실패", e)
            Toast.makeText(this, "권한 요청 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.i(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                initializeScreenCapture(resultCode, data)
            } else {
                Log.w(TAG, "화면 캡처 권한 거부됨")
                Toast.makeText(this, "화면 캡처 권한이 필요합니다", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeScreenCapture(resultCode: Int, data: Intent) {
        try {
            Log.i(TAG, "화면 캡처 권한 허용됨 - 초기화 시작")

            if (MacroService.instance == null) {
                Log.w(TAG, "MacroService가 아직 준비되지 않음 - 잠시 대기")

                Handler(Looper.getMainLooper()).postDelayed({
                    if (MacroService.instance != null) {
                        initializeScreenCapture(resultCode, data)
                    } else {
                        Toast.makeText(
                            this,
                            "접근성 서비스를 먼저 켜주세요",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }, 500)
                return
            }

            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            Log.i(TAG, "MediaProjection 생성 완료")

            screenCapture = ScreenCapture(this)
            Log.i(TAG, "ScreenCapture 객체 생성 완료")

            screenCapture?.initialize(mediaProjection)
            Log.i(TAG, "ScreenCapture 초기화 완료")

            MacroService.instance?.setScreenCapture(screenCapture!!)
            Log.i(TAG, "MacroService에 ScreenCapture 설정 완료")

            ExecutorRegistry.initialize(this, screenCapture!!)
            Log.i(TAG, "ExecutorRegistry 초기화 완료")

            Toast.makeText(this, "✅ 화면 캡처 권한 허용됨", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "초기화 중 오류 발생", e)
            e.printStackTrace()
            Toast.makeText(this, "초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
            screenCapture = null
        }
    }

    fun getScreenCapture(): ScreenCapture? = screenCapture

    fun hasScreenCapturePermission(): Boolean = screenCapture != null

    private fun startMacro() {
        if (MacroService.instance == null) {
            Toast.makeText(this, "접근성 서비스를 먼저 켜주세요\n설정 > 접근성 > MacroApp", Toast.LENGTH_LONG).show()
            return
        }

        if (screenCapture == null) {
            Toast.makeText(this, "화면 캡처 권한을 허용해주세요", Toast.LENGTH_LONG).show()
            requestScreenCapturePermission()
            return
        }

        if (macroRunner == null) {
            macroRunner = MacroRunner(this)
            macroRunner?.onStatusChanged = { isRunning, current, total ->
                runOnUiThread {
                    if (isRunning) {
                        val message = "실행중 $current/$total"
                        binding.btnStart.text = message
                        MacroService.instance?.showFloatingWidget(message)
                    } else {
                        binding.btnStart.text = "시작"
                        MacroService.instance?.hideFloatingWidget()
                    }
                }
            }
        }

        macroRunner?.start()
        Toast.makeText(this, "매크로 시작", Toast.LENGTH_SHORT).show()
    }

    private fun stopMacro() {
        macroRunner?.stop()
        binding.btnStart.text = "시작"
        MacroService.instance?.hideFloatingWidget()
        Toast.makeText(this, "매크로 중지", Toast.LENGTH_SHORT).show()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun checkAccessibilityService() {
        if (MacroService.instance == null) {
            Toast.makeText(
                this,
                "설정 > 접근성에서 MacroApp을 켜주세요",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        screenCapture?.release()
        instance = null
    }
}