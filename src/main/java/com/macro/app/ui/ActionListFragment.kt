package com.macro.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.macro.app.R
import com.macro.app.core.Action
import com.macro.app.core.ActionType
import com.macro.app.core.FlowControl
import com.macro.app.core.FlowType
import com.macro.app.data.ActionItem
import com.macro.app.manager.ActionListManager

class ActionListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ActionAdapter
    private lateinit var addButton: Button
    private lateinit var clearButton: Button
    private lateinit var fab: FloatingActionButton
    private lateinit var infiniteLoopCheckbox: CheckBox
    private var listSpinner: Spinner? = null
    private var createListButton: Button? = null
    private var deleteListButton: Button? = null
    private var renameListButton: Button? = null

    private val updateListener = object : ActionListManager.OnActionListChangeListener {
        override fun onActionListChanged(actions: List<ActionItem>) {
            adapter.updateItems(actions)
            updateListSpinner()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.action_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ActionListManager.initialize(requireContext())

        recyclerView = view.findViewById(R.id.part2_recycler_view)
        addButton = view.findViewById(R.id.part2_add_button)
        clearButton = view.findViewById(R.id.part2_clear_button)
        fab = view.findViewById(R.id.part2_fab)
        infiniteLoopCheckbox = view.findViewById(R.id.part2_infinite_loop_checkbox)

        setupListManagement(view)

        infiniteLoopCheckbox.isChecked = ActionListManager.isInfiniteLoop
        infiniteLoopCheckbox.setOnCheckedChangeListener { _, isChecked ->
            ActionListManager.isInfiniteLoop = isChecked
            Toast.makeText(
                requireContext(),
                if (isChecked) "무한반복 ON" else "무한반복 OFF",
                Toast.LENGTH_SHORT
            ).show()
        }

        adapter = ActionAdapter(
            ActionListManager.getActions().toMutableList(),
            onItemClick = { position ->
                showEditDialog(position)
            },
            onDeleteClick = { position ->
                ActionListManager.removeAction(position)
                Toast.makeText(requireContext(), "삭제됨", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        setupItemTouchHelper()

        addButton.setOnClickListener {
            onAddActionClick()
        }

        clearButton.setOnClickListener {
            showClearConfirmDialog()
        }

        fab.setOnClickListener {
            onAddActionClick()
        }

        ActionListManager.addListener(updateListener)
    }

    private fun setupListManagement(parentView: View) {
        val coordinator = parentView.findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.part2_coordinator)
        val rootLayout = coordinator?.getChildAt(0) as? LinearLayout ?: return

        val listManagementLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
                topMargin = 8
                leftMargin = 16
                rightMargin = 16
            }
        }

        listSpinner = Spinner(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        createListButton = Button(requireContext()).apply {
            text = "+"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                100,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 8
            }
            setOnClickListener { showCreateListDialog() }
        }

        renameListButton = Button(requireContext()).apply {
            text = "이름"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                120,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 4
            }
            setOnClickListener { showRenameListDialog() }
        }

        deleteListButton = Button(requireContext()).apply {
            text = "삭제"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                100,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 4
            }
            setOnClickListener { showDeleteListDialog() }
        }

        listManagementLayout.addView(listSpinner)
        listManagementLayout.addView(createListButton)
        listManagementLayout.addView(renameListButton)
        listManagementLayout.addView(deleteListButton)

        rootLayout.addView(listManagementLayout, 1)

        updateListSpinner()
    }

    private fun updateListSpinner() {
        val spinner = listSpinner ?: return

        val lists = ActionListManager.getAllLists()
        val listNames = lists.map { it.name }
        val currentListId = ActionListManager.getCurrentListId()
        val currentIndex = lists.indexOfFirst { it.id == currentListId }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        if (currentIndex >= 0) {
            spinner.setSelection(currentIndex)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedList = lists[position]
                if (selectedList.id != currentListId) {
                    ActionListManager.switchToList(selectedList.id)
                    this@ActionListFragment.adapter.updateItems(ActionListManager.getActions())
                    infiniteLoopCheckbox.isChecked = ActionListManager.isInfiniteLoop
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showCreateListDialog() {
        val input = EditText(requireContext()).apply {
            hint = "목록 이름"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("새 목록")
            .setView(input)
            .setPositiveButton("생성") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "이름 입력", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                ActionListManager.createList(name)
                updateListSpinner()
                Toast.makeText(requireContext(), "생성: $name", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showRenameListDialog() {
        val currentList = ActionListManager.getCurrentList()

        val input = EditText(requireContext()).apply {
            setText(currentList.name)
            hint = "새 이름"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("이름 변경")
            .setView(input)
            .setPositiveButton("변경") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "이름 입력", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                ActionListManager.renameList(currentList.id, newName)
                updateListSpinner()
                Toast.makeText(requireContext(), "변경: $newName", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteListDialog() {
        val currentList = ActionListManager.getCurrentList()

        if (currentList.id == "default") {
            Toast.makeText(requireContext(), "기본 목록 삭제 불가", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("목록 삭제")
            .setMessage("'${currentList.name}' 삭제?\n(모든 액션 사라짐)")
            .setPositiveButton("삭제") { _, _ ->
                ActionListManager.deleteList(currentList.id)
                updateListSpinner()
                adapter.updateItems(ActionListManager.getActions())
                Toast.makeText(requireContext(), "삭제됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        adapter.updateItems(ActionListManager.getActions())
        updateListSpinner()
        infiniteLoopCheckbox.isChecked = ActionListManager.isInfiniteLoop
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ActionListManager.removeListener(updateListener)
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                ActionListManager.moveAction(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showEditDialog(position: Int) {
        val actions = ActionListManager.getActions()
        if (position !in actions.indices) return

        val actionItem = actions[position]

        val options = mutableListOf<String>()

        when (actionItem.action.type) {
            ActionType.TOUCH -> options.add("터치 좌표 수정")
            ActionType.SWIPE -> options.add("스와이프 좌표 수정")
            ActionType.DELAY -> options.add("딜레이 값 수정")
            ActionType.IMAGE_SEARCH -> options.add("이미지 검색 설정")
            else -> {}
        }

        options.add("설명 수정")
        options.add("딜레이 수정")
        options.add("흐름 제어")
        options.add("삭제")

        AlertDialog.Builder(requireContext())
            .setTitle("액션 #${position + 1}")
            .setItems(options.toTypedArray()) { _, which ->
                var offset = 0

                when (actionItem.action.type) {
                    ActionType.TOUCH -> {
                        if (which == 0) {
                            showEditTouchDialog(position, actionItem)
                            return@setItems
                        }
                        offset = 1
                    }
                    ActionType.SWIPE -> {
                        if (which == 0) {
                            showEditSwipeDialog(position, actionItem)
                            return@setItems
                        }
                        offset = 1
                    }
                    ActionType.DELAY -> {
                        if (which == 0) {
                            showEditDelayDialog(position, actionItem)
                            return@setItems
                        }
                        offset = 1
                    }
                    ActionType.IMAGE_SEARCH -> {
                        if (which == 0) {
                            showEditImageSearchDialog(position, actionItem)
                            return@setItems
                        }
                        offset = 1
                    }
                    else -> {}
                }

                when (which - offset) {
                    0 -> showEditDescriptionDialog(position, actionItem)
                    1 -> showEditDelayMsDialog(position, actionItem)
                    2 -> showEditFlowControlDialog(position, actionItem)
                    3 -> {
                        ActionListManager.removeAction(position)
                        Toast.makeText(requireContext(), "삭제됨", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditImageSearchDialog(position: Int, actionItem: ActionItem) {
        ImageSearchEditDialog(requireContext(), actionItem) { updatedItem ->
            ActionListManager.updateAction(position, updatedItem)
            Toast.makeText(requireContext(), "수정됨", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun showEditTouchDialog(position: Int, actionItem: ActionItem) {
        val x = (actionItem.action.params["x"] as? Number)?.toInt() ?: 0
        val y = (actionItem.action.params["y"] as? Number)?.toInt() ?: 0

        val view = LayoutInflater.from(requireContext()).inflate(
            R.layout.coordinate_input_dialog,
            null
        )

        val xInput = view.findViewById<EditText>(R.id.part3_x_input)
        val yInput = view.findViewById<EditText>(R.id.part3_y_input)

        xInput.setText(x.toString())
        yInput.setText(y.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("터치 좌표")
            .setView(view)
            .setPositiveButton("확인") { _, _ ->
                val newX = xInput.text.toString().toIntOrNull() ?: x
                val newY = yInput.text.toString().toIntOrNull() ?: y

                val newParams = actionItem.action.params.toMutableMap()
                newParams["x"] = newX
                newParams["y"] = newY

                val newAction = actionItem.action.copy(params = newParams)
                val newItem = actionItem.copy(
                    action = newAction,
                    description = "터치: ($newX, $newY)"
                )

                ActionListManager.updateAction(position, newItem)
                Toast.makeText(requireContext(), "수정됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditSwipeDialog(position: Int, actionItem: ActionItem) {
        val x1 = (actionItem.action.params["x1"] as? Number)?.toInt() ?: 0
        val y1 = (actionItem.action.params["y1"] as? Number)?.toInt() ?: 0
        val x2 = (actionItem.action.params["x2"] as? Number)?.toInt() ?: 0
        val y2 = (actionItem.action.params["y2"] as? Number)?.toInt() ?: 0

        val view = LayoutInflater.from(requireContext()).inflate(
            R.layout.swipe_input_dialog,
            null
        )

        val x1Input = view.findViewById<EditText>(R.id.part3_start_x_input)
        val y1Input = view.findViewById<EditText>(R.id.part3_start_y_input)
        val x2Input = view.findViewById<EditText>(R.id.part3_end_x_input)
        val y2Input = view.findViewById<EditText>(R.id.part3_end_y_input)

        x1Input.setText(x1.toString())
        y1Input.setText(y1.toString())
        x2Input.setText(x2.toString())
        y2Input.setText(y2.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("스와이프 좌표")
            .setView(view)
            .setPositiveButton("확인") { _, _ ->
                val newX1 = x1Input.text.toString().toIntOrNull() ?: x1
                val newY1 = y1Input.text.toString().toIntOrNull() ?: y1
                val newX2 = x2Input.text.toString().toIntOrNull() ?: x2
                val newY2 = y2Input.text.toString().toIntOrNull() ?: y2

                val newParams = actionItem.action.params.toMutableMap()
                newParams["x1"] = newX1
                newParams["y1"] = newY1
                newParams["x2"] = newX2
                newParams["y2"] = newY2

                val newAction = actionItem.action.copy(params = newParams)
                val newItem = actionItem.copy(
                    action = newAction,
                    description = "스와이프: ($newX1,$newY1)→($newX2,$newY2)"
                )

                ActionListManager.updateAction(position, newItem)
                Toast.makeText(requireContext(), "수정됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditDelayDialog(position: Int, actionItem: ActionItem) {
        val delayValue = (actionItem.action.params["delayMs"] as? Number)?.toLong() ?: 1000L

        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(delayValue.toString())
            hint = "밀리초"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("딜레이 값")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val newDelay = input.text.toString().toLongOrNull() ?: delayValue

                val newParams = actionItem.action.params.toMutableMap()
                newParams["delayMs"] = newDelay

                val newAction = actionItem.action.copy(params = newParams)
                val newItem = actionItem.copy(
                    action = newAction,
                    description = "대기: ${newDelay}ms"
                )

                ActionListManager.updateAction(position, newItem)
                Toast.makeText(requireContext(), "수정됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditDescriptionDialog(position: Int, actionItem: ActionItem) {
        val input = EditText(requireContext()).apply {
            setText(actionItem.description)
            hint = "설명"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("설명")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val newDesc = input.text.toString()
                val newItem = actionItem.copy(description = newDesc)
                ActionListManager.updateAction(position, newItem)
                Toast.makeText(requireContext(), "수정됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditDelayMsDialog(position: Int, actionItem: ActionItem) {
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(actionItem.delayMs.toString())
            hint = "밀리초"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("딜레이")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val newDelayMs = input.text.toString().toLongOrNull() ?: actionItem.delayMs
                val newItem = actionItem.copy(delayMs = newDelayMs)
                ActionListManager.updateAction(position, newItem)
                Toast.makeText(requireContext(), "수정됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditFlowControlDialog(position: Int, actionItem: ActionItem) {
        val flowTypes = arrayOf("없음", "뒤로", "건너뛰기", "점프", "처음으로")

        val currentSelection = when (actionItem.flowControl.type) {
            FlowType.BACK -> 1
            FlowType.SKIP -> 2
            FlowType.GOTO -> 3
            FlowType.RESET -> 4
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("흐름 제어")
            .setSingleChoiceItems(flowTypes, currentSelection) { dialog, which ->
                val newFlow = when (which) {
                    1 -> FlowControl(FlowType.BACK, steps = 1)
                    2 -> FlowControl(FlowType.SKIP, steps = 1)
                    3 -> {
                        dialog.dismiss()
                        showGotoTargetDialog(position, actionItem)
                        return@setSingleChoiceItems
                    }
                    4 -> FlowControl(FlowType.RESET)
                    else -> FlowControl(FlowType.NONE)
                }

                val newItem = actionItem.copy(flowControl = newFlow)
                ActionListManager.updateAction(position, newItem)

                Toast.makeText(requireContext(), "설정됨", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showGotoTargetDialog(position: Int, actionItem: ActionItem) {
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "액션 번호"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("점프 대상")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val target = input.text.toString().toIntOrNull()
                if (target != null && target > 0) {
                    val newFlow = FlowControl(FlowType.GOTO, targetIndex = target - 1)
                    val newItem = actionItem.copy(flowControl = newFlow)
                    ActionListManager.updateAction(position, newItem)
                    Toast.makeText(requireContext(), "${target}번으로 점프", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "올바른 번호 입력", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun onAddActionClick() {
        val actionTypes = arrayOf("터치", "스와이프", "대기")

        AlertDialog.Builder(requireContext())
            .setTitle("액션 추가")
            .setItems(actionTypes) { _, which ->
                when (which) {
                    0 -> showAddTouchDialog()
                    1 -> showAddSwipeDialog()
                    2 -> showAddDelayDialog()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAddTouchDialog() {
        TouchInputDialog(requireContext(), { action ->
            val item = ActionItem(
                action = action,
                delayMs = 1000,
                description = "터치: (${action.params["x"]}, ${action.params["y"]})"
            )
            ActionListManager.addAction(item)
            Toast.makeText(requireContext(), "추가됨", Toast.LENGTH_SHORT).show()
        }).show()
    }

    private fun showAddSwipeDialog() {
        SwipeInputDialog(requireContext(), { action ->
            val item = ActionItem(
                action = action,
                delayMs = 1000,
                description = "스와이프"
            )
            ActionListManager.addAction(item)
            Toast.makeText(requireContext(), "추가됨", Toast.LENGTH_SHORT).show()
        }).show()
    }

    private fun showAddDelayDialog() {
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "밀리초"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("대기 추가")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val delayMs = input.text.toString().toLongOrNull() ?: 1000L

                val action = Action(
                    id = System.currentTimeMillis().toInt(),
                    type = ActionType.DELAY,
                    params = mapOf("delayMs" to delayMs)
                )

                val item = ActionItem(
                    action = action,
                    delayMs = 0,
                    description = "대기: ${delayMs}ms"
                )

                ActionListManager.addAction(item)
                Toast.makeText(requireContext(), "추가됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showClearConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("전체 삭제")
            .setMessage("모든 액션을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                ActionListManager.clearAll()
                Toast.makeText(requireContext(), "전체 삭제됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}