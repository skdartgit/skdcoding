package com.skdcoding

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val bg =
        Color.rgb(255, 248, 231)

    private val surface =
        Color.rgb(255, 253, 245)

    private val text =
        Color.rgb(62, 58, 50)

    private val muted =
        Color.rgb(118, 111, 99)

    private val accent =
        Color.rgb(91, 75, 138)

    private lateinit var root: CodeNode

    private var currentFolderId =
        "root"

    private lateinit var content: FrameLayout

    private lateinit var footer: LinearLayout

    private lateinit var homeTab: TextView

    private lateinit var exportTab: TextView

    private var adapter:
        CodingAdapter? = null

    private var itemTouchHelper:
        ItemTouchHelper? = null

    private val exportLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->

            if (uri == null) return@registerForActivityResult

            runCatching {

                contentResolver
                    .openOutputStream(uri)
                    ?.use { out ->

                        out.write(
                            TreeJson
                                .encode(root)
                                .toByteArray(
                                    Charsets.UTF_8
                                )
                        )
                    }
                    ?: error(
                        "Unable to open destination"
                    )

                toast(
                    "All data exported"
                )

            }.onFailure {

                toast(
                    "Export failed: " +
                        (
                            it.message
                                ?: "unknown error"
                            )
                )
            }
        }

    private val restoreLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) return@registerForActivityResult

            runCatching {

                val raw =
                    contentResolver
                        .openInputStream(uri)
                        ?.use {
                            it.readBytes()
                                .toString(
                                    Charsets.UTF_8
                                )
                        }
                        ?: error(
                            "Unable to open backup"
                        )

                val restored =
                    TreeJson.decode(raw)

                if (!restored.isFolder) {
                    error(
                        "Invalid skdcoding backup"
                    )
                }

                root =
                    restored.copy(
                        id = "root"
                    )

                currentFolderId =
                    "root"

                Storage.save(
                    this,
                    root
                )

                showHome()

                toast(
                    "All data restored"
                )

            }.onFailure {

                toast(
                    "Restore failed: " +
                        (
                            it.message
                                ?: "invalid backup"
                            )
                )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        window.statusBarColor =
            bg

        window.navigationBarColor =
            bg

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        root =
            Storage.load(this)

        buildShell()

        showHome()
    }

    private fun buildShell() {

        val outer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    bg
                )
            }

        content =
            FrameLayout(this)

        footer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
                )

                setBackgroundColor(
                    surface
                )
            }

        homeTab =
            tab("Home")

        exportTab =
            tab("Export & Restore")

        homeTab.setOnClickListener {
            showHome()
        }

        exportTab.setOnClickListener {
            showExportRestore()
        }

        footer.addView(
            homeTab,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        footer.addView(
            exportTab,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        outer.addView(
            content,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        outer.addView(
            footer,
            LinearLayout.LayoutParams(
                -1,
                dp(56)
            )
        )

        setContentView(outer)
    }

    private fun tab(
        label: String
    ) =
        TextView(this).apply {

            text = label

            textSize = 13f

            gravity =
                Gravity.CENTER

            setTextColor(muted)

            typeface =
                Typeface.DEFAULT

            isAllCaps = false
        }

    private fun showHome() {

        homeTab.setTextColor(
            accent
        )

        exportTab.setTextColor(
            muted
        )

        val folder =
            findNode(
                root,
                currentFolderId
            ) ?: root.also {
                currentFolderId =
                    it.id
            }

        content.removeAllViews()

        val page =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(14),
                    dp(10),
                    dp(14),
                    dp(4)
                )

                setBackgroundColor(
                    bg
                )
            }

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        if (folder.id != "root") {

            val back =
                TextView(this).apply {

                    text = "‹"

                    textSize = 34f

                    gravity =
                        Gravity.CENTER

                    setTextColor(
                        accent
                    )

                    setPadding(
                        0,
                        0,
                        dp(8),
                        0
                    )

                    setOnClickListener {

                        val parent =
                            findParent(
                                root,
                                folder.id
                            ) ?: root

                        currentFolderId =
                            parent.id

                        showHome()
                    }
                }

            header.addView(
                back,
                LinearLayout.LayoutParams(
                    dp(42),
                    dp(48)
                )
            )
        }

        val title =
            TextView(this).apply {

                text =
                    if (
                        folder.id == "root"
                    ) {
                        "Home"
                    } else {
                        folder.name
                    }

                textSize = 22f

                setTextColor(
                    this@MainActivity.text
                )

                typeface =
                    Typeface.DEFAULT_BOLD
            }

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        page.addView(header)

        val actionRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        actionRow.addView(
            actionButton("+ Folder") {
                showFolderDialog(folder)
            },
            LinearLayout.LayoutParams(
                0,
                dp(42),
                1f
            ).apply {
                marginEnd = dp(6)
            }
        )

        actionRow.addView(
            actionButton("+ Code") {
                showCodeDialog(
                    folder,
                    null
                )
            },
            LinearLayout.LayoutParams(
                0,
                dp(42),
                1f
            ).apply {
                marginStart = dp(6)
            }
        )

        page.addView(
            actionRow
        )

        val recycler =
            RecyclerView(this).apply {

                layoutManager =
                    LinearLayoutManager(
                        this@MainActivity
                    )

                setBackgroundColor(
                    bg
                )

                overScrollMode =
                    View.OVER_SCROLL_IF_CONTENT_SCROLLS
            }

        adapter =
            CodingAdapter(
                items =
                    folder.children,

                onOpen = { node ->

                    if (node.isFolder) {

                        currentFolderId =
                            node.id

                        showHome()

                    } else {

                        showCodeViewer(
                            node
                        )
                    }
                },

                onMenu = { node, anchor ->

                    showNodeMenu(
                        node,
                        anchor,
                        folder
                    )
                },

                onMoved = {
                    Storage.save(
                        this,
                        root
                    )
                },

                startDrag = { holder ->

                    itemTouchHelper
                        ?.startDrag(
                            holder
                        )
                }
            )

        recycler.adapter =
            adapter

        val callback =
            object :
                ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or
                        ItemTouchHelper.DOWN,
                    0
                ) {

                override fun onMove(
                    rv: RecyclerView,
                    vh: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {

                    val from =
                        vh.bindingAdapterPosition

                    val to =
                        target.bindingAdapterPosition

                    if (
                        from ==
                            RecyclerView.NO_POSITION ||
                        to ==
                            RecyclerView.NO_POSITION
                    ) {
                        return false
                    }

                    adapter?.move(
                        from,
                        to
                    )

                    return true
                }

                override fun onSwiped(
                    viewHolder:
                        RecyclerView.ViewHolder,
                    direction: Int
                ) = Unit
            }

        itemTouchHelper =
            ItemTouchHelper(
                callback
            ).also {
                it.attachToRecyclerView(
                    recycler
                )
            }

        page.addView(
            recycler,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            ).apply {
                topMargin = dp(8)
            }
        )

        content.addView(page)
    }

    private fun showExportRestore() {

        homeTab.setTextColor(
            muted
        )

        exportTab.setTextColor(
            accent
        )

        content.removeAllViews()

        val page =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(24),
                    dp(20),
                    dp(20)
                )

                setBackgroundColor(
                    bg
                )
            }

        val title =
            TextView(this).apply {

                text =
                    "Export & Restore"

                textSize = 24f

                setTextColor(
                    this@MainActivity.text
                )

                typeface =
                    Typeface.DEFAULT_BOLD
            }

        page.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            )
        )

        val note =
            TextView(this).apply {

                text =
                    "Use Android's official file picker to save or restore everything in skdcoding, including folders, nested folders, code, descriptions, formatting and languages."

                textSize = 15f

                setTextColor(
                    muted
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(20)
                )
            }

        page.addView(note)

        page.addView(
            actionButton(
                "Export All Data"
            ) {
                exportLauncher.launch(
                    "skdcoding_backup.json"
                )
            },
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                bottomMargin = dp(12)
            }
        )

        page.addView(
            actionButton(
                "Restore All Data"
            ) {
                restoreLauncher.launch(
                    arrayOf(
                        "application/json",
                        "text/json",
                        "text/plain"
                    )
                )
            },
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        content.addView(page)
    }

    private fun actionButton(
        label: String,
        click: () -> Unit
    ) =
        Button(this).apply {

            text = label

            textSize = 14f

            setTextColor(
                Color.WHITE
            )

            setBackground(
                roundDrawable(
                    accent,
                    14
                )
            )

            setOnClickListener {
                click()
            }
        }

    private fun showFolderDialog(
        parent: CodeNode
    ) {

        val input =
            EditText(this).apply {

                hint =
                    "Folder name"

                setSingleLine(true)

                setTextColor(
                    this@MainActivity.text
                )
            }

        val box =
            LinearLayout(this).apply {

                setPadding(
                    dp(22),
                    dp(4),
                    dp(22),
                    0
                )

                addView(
                    input,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                    )
                )
            }

        AlertDialog.Builder(this)
            .setTitle("Create Folder")
            .setView(box)
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Create"
            ) { _, _ ->

                val name =
                    input.text
                        .toString()
                        .trim()

                if (name.isEmpty()) {

                    toast(
                        "Folder name cannot be empty"
                    )

                    return@setPositiveButton
                }

                parent.children +=
                    CodeNode(
                        name = name,
                        isFolder = true
                    )

                Storage.save(
                    this,
                    root
                )

                showHome()
            }
            .show()
    }

    private fun showNodeMenu(
        node: CodeNode,
        anchor: View,
        parent: CodeNode
    ) {

        val popup =
            PopupMenu(
                this,
                anchor
            )

        if (node.isFolder) {
            popup.menu.add("Open")
        }

        popup.menu.add("Rename")

        if (!node.isFolder) {
            popup.menu.add("Edit Code")
        }

        popup.menu.add("Delete")

        popup.setOnMenuItemClickListener {

            when (
                it.title.toString()
            ) {

                "Open" -> {

                    currentFolderId =
                        node.id

                    showHome()
                }

                "Rename" -> {

                    showRenameDialog(
                        node,
                        parent
                    )
                }

                "Edit Code" -> {

                    showCodeDialog(
                        parent,
                        node
                    )
                }

                "Delete" -> {

                    confirmDelete(
                        node,
                        parent
                    )
                }
            }

            true
        }

        popup.show()
    }

    private fun showRenameDialog(
        node: CodeNode,
        parent: CodeNode
    ) {

        val input =
            EditText(this).apply {

                setSingleLine(true)

                setText(
                    node.name
                )

                setSelectAllOnFocus(
                    true
                )

                setTextColor(
                    this@MainActivity.text
                )
            }

        val box =
            LinearLayout(this).apply {

                setPadding(
                    dp(22),
                    0,
                    dp(22),
                    0
                )

                addView(
                    input,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                    )
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                if (node.isFolder)
                    "Rename Folder"
                else
                    "Rename Code"
            )
            .setView(box)
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Save"
            ) { _, _ ->

                val name =
                    input.text
                        .toString()
                        .trim()

                if (name.isNotEmpty()) {

                    node.name =
                        name

                    Storage.save(
                        this,
                        root
                    )

                    showHome()
                }
            }
            .show()
    }

    private fun confirmDelete(
        node: CodeNode,
        parent: CodeNode
    ) {

        val what =
            if (node.isFolder)
                "folder and everything inside it"
            else
                "code"

        AlertDialog.Builder(this)
            .setTitle(
                "Delete " +
                    if (node.isFolder)
                        "Folder?"
                    else
                        "Code?"
            )
            .setMessage(
                "This will permanently delete this $what."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                parent.children.removeAll {
                    it.id == node.id
                }

                Storage.save(
                    this,
                    root
                )

                showHome()
            }
            .show()
    }

    private fun showCodeDialog(
        parent: CodeNode,
        existing: CodeNode?
    ) {

        val isEdit =
            existing != null

        val titleEdit =
            EditText(this).apply {

                hint =
                    "Code Title"

                setSingleLine(true)

                setText(
                    existing?.name ?: ""
                )

                setTextColor(
                    this@MainActivity.text
                )
            }

        val descriptionEdit =
            EditText(this).apply {

                hint =
                    "Code Description"

                minLines = 4

                gravity =
                    Gravity.TOP or
                        Gravity.START

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                setTextColor(
                    this@MainActivity.text
                )

                setTextSize(15f)

                setText(
                    existing?.let {
                        Html.fromHtml(
                            it.descriptionHtml,
                            Html.FROM_HTML_MODE_LEGACY
                        )
                    } ?: ""
                )
            }

        val bold =
            Button(this).apply {

                text = "B"

                setOnClickListener {
                    toggleStyle(
                        descriptionEdit,
                        true
                    )
                }
            }

        val underline =
            Button(this).apply {

                text = "U"

                setOnClickListener {
                    toggleStyle(
                        descriptionEdit,
                        false
                    )
                }
            }

        val formatRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        formatRow.addView(
            TextView(this).apply {

                text =
                    "Description formatting"

                setTextColor(
                    muted
                )

                textSize = 12f

            },
            LinearLayout.LayoutParams(
                0,
                dp(40),
                1f
            )
        )

        formatRow.addView(
            bold,
            LinearLayout.LayoutParams(
                dp(48),
                dp(40)
            )
        )

        formatRow.addView(
            underline,
            LinearLayout.LayoutParams(
                dp(48),
                dp(40)
            ).apply {
                marginStart = dp(4)
            }
        )

        val spinner =
            Spinner(this)

        val languages =
            listOf(
                "Python",
                "Java",
                "Kotlin",
                "JavaScript",
                "HTML",
                "CSS",
                "HTML + CSS + JavaScript"
            )

        spinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                languages
            )

        spinner.setSelection(
            languages
                .indexOf(
                    existing?.language
                )
                .takeIf {
                    it >= 0
                } ?: 0
        )

        val codeEdit =
            EditText(this).apply {

                hint =
                    "Original Code"

                typeface =
                    Typeface.MONOSPACE

                textSize = 14f

                gravity =
                    Gravity.TOP or
                        Gravity.START

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

                setHorizontallyScrolling(
                    true
                )

                isSingleLine = false

                minLines = 12

                setTextColor(
                    this@MainActivity.text
                )

                setBackgroundColor(
                    Color.rgb(
                        255,
                        251,
                        239
                    )
                )

                setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(12)
                )

                setText(
                    existing?.code ?: ""
                )
            }

        val horizontalCode =
            HorizontalScrollView(this).apply {

                isFillViewport =
                    true

                addView(
                    codeEdit,
                    ViewGroup.LayoutParams(
                        -1,
                        -2
                    )
                )
            }

        val codeLabel =
            TextView(this).apply {

                text =
                    "Original Code  •  select language"

                setTextColor(
                    muted
                )

                textSize = 12f

                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(5)
                )
            }

        val watcher =
            object : TextWatcher {

                private var busy =
                    false

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) = Unit

                override fun afterTextChanged(
                    s: Editable?
                ) {

                    if (
                        busy ||
                        s == null
                    ) {
                        return
                    }

                    busy = true

                    SyntaxHighlighter.apply(
                        s,
                        spinner.selectedItem
                            .toString()
                    )

                    busy = false
                }
            }

        codeEdit.addTextChangedListener(
            watcher
        )

        spinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    SyntaxHighlighter.apply(
                        codeEdit.text,
                        languages[position]
                    )
                }
            }

        SyntaxHighlighter.apply(
            codeEdit.text,
            existing?.language
                ?: "Python"
        )

        val form =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(4),
                    dp(18),
                    dp(8)
                )

                addView(
                    titleEdit,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                    )
                )

                addView(
                    formatRow
                )

                addView(
                    descriptionEdit,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(118)
                    )
                )

                addView(
                    codeLabel
                )

                addView(
                    spinner,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(50)
                    )
                )

                addView(
                    horizontalCode,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(270)
                    )
                )
            }

        val scroll =
            ScrollView(this).apply {
                addView(form)
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    if (isEdit)
                        "Edit Code"
                    else
                        "Create Code"
                )
                .setView(scroll)
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    if (isEdit)
                        "Save"
                    else
                        "Create",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog
                .getButton(
                    AlertDialog.BUTTON_POSITIVE
                )
                .setOnClickListener {

                    val title =
                        titleEdit.text
                            .toString()
                            .trim()

                    if (title.isEmpty()) {

                        toast(
                            "Code title cannot be empty"
                        )

                        return@setOnClickListener
                    }

                    val lang =
                        spinner.selectedItem
                            .toString()

                    val descHtml =
                        Html.toHtml(
                            descriptionEdit.text,
                            Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
                        )

                    if (isEdit) {

                        existing!!.name =
                            title

                        existing.descriptionHtml =
                            descHtml

                        existing.code =
                            codeEdit.text.toString()

                        existing.language =
                            lang

                    } else {

                        parent.children +=
                            CodeNode(
                                name = title,
                                descriptionHtml =
                                    descHtml,
                                code =
                                    codeEdit.text.toString(),
                                language =
                                    lang,
                                isFolder =
                                    false
                            )
                    }

                    Storage.save(
                        this,
                        root
                    )

                    dialog.dismiss()

                    showHome()
                }
        }

        dialog.show()
    }

    private fun toggleStyle(
        edit: EditText,
        bold: Boolean
    ) {

        val start =
            edit.selectionStart

        val end =
            edit.selectionEnd

        if (
            start < 0 ||
            end <= start
        ) {

            toast(
                "Select text first"
            )

            return
        }

        if (bold) {

            edit.text.setSpan(
                StyleSpan(
                    Typeface.BOLD
                ),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

        } else {

            edit.text.setSpan(
                UnderlineSpan(),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun showCodeViewer(
        node: CodeNode
    ) {

        content.removeAllViews()

        val page =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(14),
                    dp(10),
                    dp(14),
                    dp(8)
                )

                setBackgroundColor(
                    bg
                )
            }

        val top =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val back =
            TextView(this).apply {

                text = "‹"

                textSize = 34f

                setTextColor(
                    accent
                )

                gravity =
                    Gravity.CENTER

                setOnClickListener {
                    showHome()
                }
            }

        top.addView(
            back,
            LinearLayout.LayoutParams(
                dp(44),
                dp(48)
            )
        )

        val title =
            TextView(this).apply {

                text =
                    node.name

                textSize = 21f

                setTextColor(
                    this@MainActivity.text
                )

                typeface =
                    Typeface.DEFAULT_BOLD
            }

        top.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        val edit =
            actionButton("Edit") {

                val parent =
                    findParent(
                        root,
                        node.id
                    ) ?: root

                showCodeDialog(
                    parent,
                    node
                )
            }

        top.addView(
            edit,
            LinearLayout.LayoutParams(
                dp(74),
                dp(42)
            )
        )

        page.addView(top)

        page.addView(
            label("Code Title")
        )

        page.addView(
            TextView(this).apply {

                text =
                    node.name

                textSize = 18f

                setTextColor(
                    this@MainActivity.text
                )

                setTextIsSelectable(
                    true
                )

                setPadding(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(8)
                )
            }
        )

        page.addView(
            label("Code Description")
        )

        page.addView(
            TextView(this).apply {

                text =
                    Html.fromHtml(
                        node.descriptionHtml,
                        Html.FROM_HTML_MODE_LEGACY
                    )

                textSize = 15f

                setTextColor(
                    this@MainActivity.text
                )

                setTextIsSelectable(
                    true
                )

                setPadding(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(8)
                )
            }
        )

        val codeTitleRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        codeTitleRow.addView(
            label(
                "Original Code  •  ${node.language}"
            ),
            LinearLayout.LayoutParams(
                0,
                dp(38),
                1f
            )
        )

        codeTitleRow.addView(
            actionButton(
                "Copy All"
            ) {
                copyCode(
                    node.code
                )
            },
            LinearLayout.LayoutParams(
                dp(92),
                dp(38)
            )
        )

        page.addView(
            codeTitleRow
        )

        val codeText =
            TextView(this).apply {

                typeface =
                    Typeface.MONOSPACE

                textSize = 14f

                setTextColor(
                    this@MainActivity.text
                )

                setTextIsSelectable(
                    true
                )

                setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(12)
                )

                setBackgroundColor(
                    Color.rgb(
                        255,
                        251,
                        239
                    )
                )

                setHorizontallyScrolling(
                    true
                )

                text =
                    SpannableStringBuilder(
                        node.code
                    ).also {

                        SyntaxHighlighter.apply(
                            it,
                            node.language
                        )
                    }
            }

        val hs =
            HorizontalScrollView(
                this
            ).apply {

                isFillViewport =
                    true

                addView(
                    codeText,
                    ViewGroup.LayoutParams(
                        -1,
                        -2
                    )
                )
            }

        val vs =
            ScrollView(this).apply {

                addView(
                    hs,
                    ViewGroup.LayoutParams(
                        -1,
                        -1
                    )
                )
            }

        page.addView(
            vs,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        content.addView(page)
    }

    private fun label(
        s: String
    ) =
        TextView(this).apply {

            text = s

            textSize = 12f

            setTextColor(
                muted
            )

            setPadding(
                dp(4),
                dp(7),
                0,
                dp(3)
            )
        }

    private fun copyCode(
        code: String
    ) {

        val clipboard =
            getSystemService(
                CLIPBOARD_SERVICE
            ) as android.content.ClipboardManager

        clipboard.setPrimaryClip(
            android.content.ClipData
                .newPlainText(
                    "Original Code",
                    code
                )
        )

        toast(
            "Original code copied"
        )
    }

    private fun findNode(
        node: CodeNode,
        id: String
    ): CodeNode? {

        if (node.id == id) {
            return node
        }

        node.children.forEach {

            findNode(
                it,
                id
            )?.let { found ->
                return found
            }
        }

        return null
    }

    private fun findParent(
        node: CodeNode,
        childId: String
    ): CodeNode? {

        node.children.forEach {

            if (it.id == childId) {
                return node
            }

            findParent(
                it,
                childId
            )?.let { found ->
                return found
            }
        }

        return null
    }

    private fun roundDrawable(
        color: Int,
        radiusDp: Int
    ) =
        android.graphics.drawable
            .GradientDrawable()
            .apply {

                setColor(color)

                cornerRadius =
                    dp(radiusDp)
                        .toFloat()
            }

    private fun toast(
        message: String
    ) =
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()

    private fun dp(
        v: Int
    ): Int =
        (
            v *
                resources
                    .displayMetrics
                    .density
        ).toInt()

    private inner class CodingAdapter(
        private val items:
            MutableList<CodeNode>,

        private val onOpen:
            (CodeNode) -> Unit,

        private val onMenu:
            (CodeNode, View) -> Unit,

        private val onMoved:
            () -> Unit,

        private val startDrag:
            (RecyclerView.ViewHolder) -> Unit

    ) :
        RecyclerView.Adapter<
            CodingAdapter.Holder
        >() {

        inner class Holder(
            val row: LinearLayout
        ) :
            RecyclerView.ViewHolder(row) {

            val title =
                row.findViewWithTag<TextView>(
                    "title"
                )

            val subtitle =
                row.findViewWithTag<TextView>(
                    "subtitle"
                )

            val menu =
                row.findViewWithTag<ImageButton>(
                    "menu"
                )
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): Holder {

            val row =
                LinearLayout(
                    this@MainActivity
                ).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(12),
                        dp(6),
                        dp(6),
                        dp(6)
                    )

                    background =
                        roundDrawable(
                            surface,
                            12
                        )

                    elevation =
                        dp(1).toFloat()
                }

            val icon =
                TextView(
                    this@MainActivity
                ).apply {

                    textSize = 23f

                    setTextColor(
                        accent
                    )

                    gravity =
                        Gravity.CENTER

                    tag = "icon"
                }

            val textBox =
                LinearLayout(
                    this@MainActivity
                ).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            val title =
                TextView(
                    this@MainActivity
                ).apply {

                    tag = "title"

                    textSize = 16f

                    setTextColor(
                        this@MainActivity.text
                    )

                    typeface =
                        Typeface.DEFAULT_BOLD

                    maxLines = 1

                    ellipsize =
                        android.text.TextUtils
                            .TruncateAt.END
                }

            val subtitle =
                TextView(
                    this@MainActivity
                ).apply {

                    tag = "subtitle"

                    textSize = 12f

                    setTextColor(
                        muted
                    )

                    maxLines = 1

                    ellipsize =
                        android.text.TextUtils
                            .TruncateAt.END
                }

            textBox.addView(
                title,
                LinearLayout.LayoutParams(
                    -1,
                    dp(27)
                )
            )

            textBox.addView(
                subtitle,
                LinearLayout.LayoutParams(
                    -1,
                    dp(21)
                )
            )

            val menu =
                ImageButton(
                    this@MainActivity
                ).apply {

                    tag = "menu"

                    setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainActivity,
                            android.R.drawable.ic_menu_more
                        )
                    )

                    setBackgroundColor(
                        Color.TRANSPARENT
                    )

                    contentDescription =
                        "More actions"
                }

            row.addView(
                icon,
                LinearLayout.LayoutParams(
                    dp(40),
                    dp(58)
                )
            )

            row.addView(
                textBox,
                LinearLayout.LayoutParams(
                    0,
                    dp(58),
                    1f
                )
            )

            row.addView(
                menu,
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(58)
                )
            )

            val lp =
                RecyclerView.LayoutParams(
                    -1,
                    dp(70)
                )

            lp.setMargins(
                0,
                dp(4),
                0,
                dp(4)
            )

            row.layoutParams = lp

            return Holder(row)
        }

        override fun onBindViewHolder(
            holder: Holder,
            position: Int
        ) {

            val node =
                items[position]

            holder.row
                .findViewWithTag<TextView>(
                    "icon"
                )
                .text =
                if (node.isFolder)
                    "▣"
                else
                    "</>"

            holder.title.text =
                node.name

            holder.subtitle.text =
                if (node.isFolder) {

                    "${node.children.size} item(s)  •  long-press to drag"

                } else {

                    "${node.language}  •  long-press to drag"
                }

            holder.row.setOnClickListener {
                onOpen(node)
            }

            holder.menu.setOnClickListener {
                v ->
                onMenu(
                    node,
                    v
                )
            }

            holder.row.setOnLongClickListener {

                startDrag(
                    holder
                )

                true
            }
        }

        override fun getItemCount():
            Int =
            items.size

        fun move(
            from: Int,
            to: Int
        ) {

            if (from == to) {
                return
            }

            val item =
                items.removeAt(from)

            items.add(
                to,
                item
            )

            notifyItemMoved(
                from,
                to
            )

            onMoved()
        }
    }
}
