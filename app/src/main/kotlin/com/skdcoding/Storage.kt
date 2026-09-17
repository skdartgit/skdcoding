package com.skdcoding

import android.content.Context
import java.io.File

object Storage {

    private const val FILE_NAME = "skdcoding_data.json"

    fun load(context: Context): CodeNode {

        val file = File(
            context.filesDir,
            FILE_NAME
        )

        if (!file.exists()) {
            return CodeNode(
                id = "root",
                name = "Home",
                isFolder = true
            )
        }

        return runCatching {
            TreeJson.decode(
                file.readText(
                    Charsets.UTF_8
                )
            )
        }.getOrElse {
            CodeNode(
                id = "root",
                name = "Home",
                isFolder = true
            )
        }.also {
            it.id = "root"
            it.isFolder = true
        }
    }

    fun save(
        context: Context,
        root: CodeNode
    ) {

        val target = File(
            context.filesDir,
            FILE_NAME
        )

        val temp = File(
            context.filesDir,
            "$FILE_NAME.tmp"
        )

        temp.writeText(
            TreeJson.encode(root),
            Charsets.UTF_8
        )

        if (!temp.renameTo(target)) {
            temp.copyTo(
                target,
                overwrite = true
            )

            temp.delete()
        }
    }
}
