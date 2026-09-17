package com.skdcoding

import android.text.Editable
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.util.regex.Pattern

object SyntaxHighlighter {

    private const val KEYWORD =
        0xff6a4c93.toInt()

    private const val STRING =
        0xff2f7d32.toInt()

    private const val COMMENT =
        0xff8a7b68.toInt()

    private const val NUMBER =
        0xffa35d1a.toInt()

    private const val TAG =
        0xff9b3f75.toInt()

    private const val ATTRIBUTE =
        0xff516a9b.toInt()

    private val commonKeywords =
        mapOf(

            "Python" to
                "and|as|assert|async|await|break|case|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|match|nonlocal|not|or|pass|raise|return|try|while|with|yield|True|False|None",

            "Java" to
                "abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null",

            "Kotlin" to
                "as|break|class|continue|do|else|false|for|fun|if|in|interface|is|null|object|package|private|protected|public|return|super|this|throw|true|try|typealias|typeof|val|var|when|while|by|catch|constructor|delegate|dynamic|field|file|finally|get|import|init|param|property|receiver|set|setparam|where|actual|abstract|annotation|companion|const|crossinline|data|enum|expect|external|final|infix|inline|inner|internal|lateinit|noinline|open|operator|out|override|reified|sealed|suspend|tailrec|vararg",

            "JavaScript" to
                "as|async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|false|finally|for|from|function|get|if|import|in|instanceof|let|new|null|of|return|set|static|super|switch|this|throw|true|try|typeof|var|void|while|with|yield",

            "CSS" to
                "important|inherit|initial|unset|none|auto|block|inline|flex|grid|absolute|relative|fixed|sticky",

            "HTML" to
                "DOCTYPE|html|head|body|title|meta|link|style|script|div|span|p|a|img|button|input|form|table|ul|ol|li|header|footer|main|section|article|nav",

            "HTML + CSS + JavaScript" to
                "async|await|break|case|catch|class|const|continue|default|delete|else|export|extends|false|finally|for|function|if|import|in|let|new|null|return|static|super|switch|this|throw|true|try|typeof|var|while|yield|display|position|relative|absolute|fixed|flex|grid|color|background|DOCTYPE|html|head|body|script|style|div|span|class|id"
        )

    fun apply(
        editable: Editable,
        language: String
    ) {

        val selectionStart =
            editable.getSpanStart(
                android.text.Selection.SELECTION_START
            )

        val selectionEnd =
            editable.getSpanStart(
                android.text.Selection.SELECTION_END
            )

        editable
            .getSpans(
                0,
                editable.length,
                ForegroundColorSpan::class.java
            )
            .forEach {
                editable.removeSpan(it)
            }

        editable
            .getSpans(
                0,
                editable.length,
                StyleSpan::class.java
            )
            .forEach {
                editable.removeSpan(it)
            }

        val text = editable.toString()

        val patterns =
            mutableListOf<Pair<Pattern, Int>>()

        val keywords =
            commonKeywords[language]
                ?: ""

        if (keywords.isNotBlank()) {

            patterns +=
                Pattern.compile(
                    "\\b(?:$keywords)\\b"
                ).let {
                    it to KEYWORD
                }
        }

        patterns +=
            Pattern.compile(
                "(?:\"(?:\\\\.|[^\"])*\"|'(?:\\\\.|[^'])*'|`(?:\\\\.|[^`])*`)"
            ).let {
                it to STRING
            }

        patterns +=
            when (language) {

                "Python",
                "Java",
                "Kotlin",
                "JavaScript",
                "HTML + CSS + JavaScript" ->

                    Pattern.compile(
                        "//.*$|/\\*[\\s\\S]*?\\*/|#.*$"
                    ).let {
                        it to COMMENT
                    }

                else ->

                    Pattern.compile(
                        "/\\*[\\s\\S]*?\\*/"
                    ).let {
                        it to COMMENT
                    }
            }

        patterns +=
            Pattern.compile(
                "\\b(?:0x[0-9a-fA-F]+|\\d+(?:\\.\\d+)?)\\b"
            ).let {
                it to NUMBER
            }

        for ((pattern, color) in patterns) {

            val matcher =
                pattern.matcher(text)

            while (matcher.find()) {

                editable.setSpan(
                    ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        if (
            language == "HTML" ||
            language == "HTML + CSS + JavaScript"
        ) {

            val tagMatcher =
                Pattern.compile(
                    "</?[A-Za-z][^>]*>"
                ).matcher(text)

            while (tagMatcher.find()) {

                editable.setSpan(
                    ForegroundColorSpan(TAG),
                    tagMatcher.start(),
                    tagMatcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val attrMatcher =
                Pattern.compile(
                    "\\b[A-Za-z_:][-A-Za-z0-9_:.]*(?=\\s*=)"
                ).matcher(text)

            while (attrMatcher.find()) {

                editable.setSpan(
                    ForegroundColorSpan(ATTRIBUTE),
                    attrMatcher.start(),
                    attrMatcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        if (
            selectionStart >= 0 &&
            selectionEnd >= 0
        ) {

            android.text.Selection.setSelection(
                editable,
                selectionStart.coerceAtMost(
                    editable.length
                ),
                selectionEnd.coerceAtMost(
                    editable.length
                )
            )
        }
    }
}
