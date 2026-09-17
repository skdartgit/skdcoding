package com.skdcoding

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CodeNode(
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var descriptionHtml: String = "",
    var code: String = "",
    var language: String = "Python",
    var isFolder: Boolean = false,
    val children: MutableList<CodeNode> = mutableListOf()
)

object TreeJson {

    fun encode(root: CodeNode): String {
        return root.toJson().toString(2)
    }

    fun decode(raw: String): CodeNode {
        return fromJson(JSONObject(raw))
    }

    private fun CodeNode.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("descriptionHtml", descriptionHtml)
            put("code", code)
            put("language", language)
            put("isFolder", isFolder)

            put(
                "children",
                JSONArray().apply {
                    children.forEach {
                        put(it.toJson())
                    }
                }
            )
        }
    }

    private fun fromJson(o: JSONObject): CodeNode {

        val node = CodeNode(
            id = o.optString("id")
                .ifBlank { UUID.randomUUID().toString() },

            name = o.optString(
                "name",
                "Untitled"
            ),

            descriptionHtml = o.optString(
                "descriptionHtml",
                ""
            ),

            code = o.optString(
                "code",
                ""
            ),

            language = o.optString(
                "language",
                "Python"
            ),

            isFolder = o.optBoolean(
                "isFolder",
                false
            )
        )

        val arr = o.optJSONArray("children")
            ?: JSONArray()

        for (i in 0 until arr.length()) {
            node.children += fromJson(
                arr.getJSONObject(i)
            )
        }

        return node
    }
}
