package com.macro.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.macro.app.MainActivity
import com.macro.app.R
import com.macro.app.core.Action
import com.macro.app.core.ActionType
import com.macro.app.data.ActionItem
import com.macro.app.manager.ActionListManager
import com.macro.app.repository.ImageRepository
import com.macro.app.registry.ExecutorRegistry
import com.macro.app.executor.ImageSearchExecutor

class ImageSearchFragment : Fragment() {

    private lateinit var repository: ImageRepository
    private lateinit var adapter: ImageAdapter
    private lateinit var thresholdSlider: SeekBar
    private lateinit var thresholdValue: TextView
    private lateinit var searchModeSpinner: Spinner
    private lateinit var tapOnFoundCheckbox: CheckBox
    private lateinit var playSoundCheckbox: CheckBox
    private lateinit var vibrateCheckbox: CheckBox
    private lateinit var onFoundActionSpinner: Spinner
    private lateinit var onFoundJumpInput: EditText
    private lateinit var onNotFoundActionSpinner: Spinner
    private lateinit var onNotFoundJumpInput: EditText
    private lateinit var addActionButton: Button
    private lateinit var testButton: Button
    private lateinit var requestPermissionButton: Button
    private lateinit var permissionStatusText: TextView

    private var selectedTemplate: ImageRepository.TemplateInfo? = null

    private val REQUEST_IMAGE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.image_search_fragment, container, false)
        repository = ImageRepository(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView(view)
        setupAddButton(view)
        setupSearchMode()
        setupThresholdSlider()
        setupConditionSpinners()
        setupAddActionButton(view)
        setupTestButton(view)
        setupPermissionButton(view)
        loadTemplates()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        loadTemplates()
    }

    private fun setupViews(view: View) {
        thresholdSlider = view.findViewById(R.id.part4_threshold_slider)
        thresholdValue = view.findViewById(R.id.part4_threshold_value)
        searchModeSpinner = view.findViewById(R.id.part4_search_mode_spinner)
        tapOnFoundCheckbox = view.findViewById(R.id.part4_tap_on_found)
        playSoundCheckbox = view.findViewById(R.id.part4_play_sound)
        vibrateCheckbox = view.findViewById(R.id.part4_vibrate)

        onFoundActionSpinner = view.findViewById(R.id.part4_on_found_spinner)
        onFoundJumpInput = view.findViewById(R.id.part4_on_found_jump_input)
        onNotFoundActionSpinner = view.findViewById(R.id.part4_on_not_found_spinner)
        onNotFoundJumpInput = view.findViewById(R.id.part4_on_not_found_jump_input)

        requestPermissionButton = view.findViewById(R.id.part4_request_permission_button)
        permissionStatusText = view.findViewById(R.id.part4_permission_status)
    }

    private fun setupPermissionButton(view: View) {
        requestPermissionButton.setOnClickListener {
            (activity as? MainActivity)?.requestScreenCapturePermission()
        }
    }

    private fun updatePermissionStatus() {
        val activity = activity as? MainActivity
        if (activity == null) {
            permissionStatusText.text = "⚠️ 액티비티 초기화 중..."
            permissionStatusText.setTextColor(0xFFAAAA00.toInt())
            requestPermissionButton.visibility = View.GONE
            testButton.isEnabled = false
            addActionButton.isEnabled = false
            return
        }

        val hasPermission = activity.hasScreenCapturePermission()

        if (hasPermission) {
            permissionStatusText.text = "✅ 화면 캡처 권한: 허용됨"
            permissionStatusText.setTextColor(0xFF00AA00.toInt())
            requestPermissionButton.visibility = View.GONE
            testButton.isEnabled = true
            addActionButton.isEnabled = true
        } else {
            permissionStatusText.text = "❌ 화면 캡처 권한: 필요함"
            permissionStatusText.setTextColor(0xFFAA0000.toInt())
            requestPermissionButton.visibility = View.VISIBLE
            testButton.isEnabled = false
            addActionButton.isEnabled = false
        }
    }

    private fun setupRecyclerView(view: View) {
        val templateList = view.findViewById<RecyclerView>(R.id.part4_template_list)

        adapter = ImageAdapter(
            onDeleteClick = { template ->
                repository.deleteTemplate(template.id)
                if (selectedTemplate?.id == template.id) {
                    selectedTemplate = null
                }
                loadTemplates()
                Toast.makeText(requireContext(), "템플릿 삭제됨", Toast.LENGTH_SHORT).show()
            },
            onSelectClick = { template ->
                selectedTemplate = template
                adapter.setSelectedTemplate(template.id)
                Toast.makeText(requireContext(), "템플릿 선택됨: ${template.name}", Toast.LENGTH_SHORT).show()
            }
        )

        templateList.layoutManager = LinearLayoutManager(requireContext())
        templateList.adapter = adapter
    }

    private fun setupAddButton(view: View) {
        view.findViewById<FloatingActionButton>(R.id.part4_add_template)?.setOnClickListener {
            openImagePicker()
        }
    }

    private fun setupSearchMode() {
        val modes = arrayOf(
            "한 번만 체크",
            "발견될 때까지 대기",
            "사라질 때까지 대기"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        searchModeSpinner.adapter = adapter
        searchModeSpinner.setSelection(1)
    }

    private fun setupThresholdSlider() {
        thresholdSlider.progress = 85
        thresholdValue.text = "85%"

        thresholdSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                thresholdValue.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupConditionSpinners() {
        val conditionOptions = arrayOf("계속 진행", "중지", "점프")

        val foundAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conditionOptions)
        foundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        onFoundActionSpinner.adapter = foundAdapter
        onFoundActionSpinner.setSelection(0)

        onFoundActionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onFoundJumpInput.visibility = if (position == 2) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val notFoundAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conditionOptions)
        notFoundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        onNotFoundActionSpinner.adapter = notFoundAdapter
        onNotFoundActionSpinner.setSelection(1)

        onNotFoundActionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onNotFoundJumpInput.visibility = if (position == 2) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTestButton(view: View) {
        testButton = view.findViewById(R.id.part4_test_button)
        testButton.setOnClickListener {
            if (!checkPermission()) return@setOnClickListener

            if (selectedTemplate == null) {
                Toast.makeText(requireContext(), "템플릿을 선택하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            testImageSearch()
        }
    }

    private fun checkPermission(): Boolean {
        val hasPermission = (activity as? MainActivity)?.hasScreenCapturePermission() ?: false
        if (!hasPermission) {
            Toast.makeText(requireContext(), "먼저 화면 캡처 권한을 허용하세요", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun testImageSearch() {
        val templateId = selectedTemplate?.id ?: return
        val threshold = thresholdSlider.progress / 100.0f

        Toast.makeText(requireContext(), "테스트 중...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val executor = ExecutorRegistry.getExecutor(ActionType.IMAGE_SEARCH) as? ImageSearchExecutor
                if (executor == null) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Executor 초기화 실패", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val result = executor.testSearch(templateId, threshold)

                requireActivity().runOnUiThread {
                    if (result != null) {
                        showTestResult(result)
                    } else {
                        Toast.makeText(requireContext(), "테스트 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showTestResult(result: ImageSearchExecutor.TestResult) {
        val message = StringBuilder()
        message.append("📊 테스트 결과\n\n")
        message.append("화면 캡처: ${if (result.screenshotCaptured) "✅ 성공" else "❌ 실패"}\n")
        message.append("템플릿 로드: ${if (result.templateLoaded) "✅ 성공" else "❌ 실패"}\n")
        message.append("이미지 발견: ${if (result.matchFound) "✅ 발견됨" else "❌ 못찾음"}\n")
        message.append("일치도: ${(result.confidence * 100).toInt()}%\n")
        if (result.matchFound) {
            message.append("위치: ${result.position}\n")
        }
        if (result.errorMessage != null) {
            message.append("\n⚠️ ${result.errorMessage}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("테스트 결과")
            .setMessage(message.toString())
            .setPositiveButton("확인", null)
            .show()
    }

    private fun setupAddActionButton(view: View) {
        addActionButton = view.findViewById(R.id.part4_add_action_button)
        addActionButton.setOnClickListener {
            if (!checkPermission()) return@setOnClickListener

            if (selectedTemplate == null) {
                Toast.makeText(requireContext(), "템플릿을 선택하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val template = selectedTemplate!!

            ImageSearchAddDialog(requireContext(), template) { action ->
                val descBuilder = StringBuilder("이미지: ${template.name}")
                val threshold = (action.params["threshold"] as? Float)?.let { (it * 100).toInt() }
                if (threshold != null) {
                    descBuilder.append(" (${threshold}%)")
                }

                val onFoundAction = action.params["onFoundAction"] as? String
                val onFoundJumpTo = action.params["onFoundJumpTo"] as? Int
                if (onFoundAction == "JUMP" && onFoundJumpTo != null) {
                    descBuilder.append("\n찾으면: ${onFoundJumpTo + 1}번")
                }

                val onNotFoundAction = action.params["onNotFoundAction"] as? String
                val onNotFoundJumpTo = action.params["onNotFoundJumpTo"] as? Int
                if (onNotFoundAction == "JUMP" && onNotFoundJumpTo != null) {
                    descBuilder.append("\n못찾으면: ${onNotFoundJumpTo + 1}번")
                }

                val actionItem = ActionItem(
                    action = action,
                    delayMs = 0,
                    description = descBuilder.toString()
                )

                ActionListManager.addAction(actionItem)
                Toast.makeText(requireContext(), "액션 추가됨", Toast.LENGTH_SHORT).show()
            }.show()

            selectedTemplate = null
            adapter.setSelectedTemplate(null)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    showRegionSelectorDialog(bitmap)

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "이미지 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRegionSelectorDialog(originalBitmap: android.graphics.Bitmap) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("템플릿 저장")
            .setMessage("전체 이미지를 저장하시겠습니까?\n아니면 특정 영역만 선택하시겠습니까?")
            .setPositiveButton("전체 저장") { _, _ ->
                val name = "Template_${System.currentTimeMillis()}"
                repository.saveTemplate(originalBitmap, name)
                loadTemplates()
                Toast.makeText(requireContext(), "템플릿 저장됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("영역 선택") { _, _ ->
                val regionSelector = ImageRegionSelector(requireContext(), originalBitmap) { left, top, right, bottom ->
                    val croppedBitmap = android.graphics.Bitmap.createBitmap(
                        originalBitmap,
                        left,
                        top,
                        right - left,
                        bottom - top
                    )
                    val name = "Template_${System.currentTimeMillis()}"
                    repository.saveTemplate(croppedBitmap, name)
                    loadTemplates()
                    Toast.makeText(requireContext(), "선택된 영역이 저장됨", Toast.LENGTH_SHORT).show()
                }
                regionSelector.show()
            }
            .setNeutralButton("취소", null)
            .create()
        dialog.show()
    }

    private fun loadTemplates() {
        val templates = repository.listTemplates()
        adapter.submitList(templates)

        val emptyText = view?.findViewById<TextView>(R.id.part4_empty_text)
        val templateList = view?.findViewById<RecyclerView>(R.id.part4_template_list)

        if (templates.isEmpty()) {
            templateList?.visibility = View.GONE
            emptyText?.visibility = View.VISIBLE
        } else {
            templateList?.visibility = View.VISIBLE
            emptyText?.visibility = View.GONE
        }
    }
}