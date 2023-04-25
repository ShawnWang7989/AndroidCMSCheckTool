package util

import org.simpleframework.xml.Serializer
import xmlUnit.XMLResource
import java.io.File

object ReadUtils {

    fun getLine(): String {
        while (true) {
            val line = readLine()
            if (!line.isNullOrBlank()) {
                return line.trim()
            }
        }
    }

    fun getYN(): Boolean {
        while (true) {
            val b = readLine()?.lowercase()
            if (b == "y") {
                return true
            }
            if (b == "n")
                return false
        }
    }

    fun getFolder(title: String, oldPath: String? = null): File {
        var haveTryOldPath = false
        while (true) {
            val filePath: String? = if (oldPath != null && !haveTryOldPath) {
                haveTryOldPath = true
                oldPath
            } else {
                println(title)
                readLine()
            }
            if (filePath.isNullOrBlank()) {
                println("ERROR! Incorrect file path!")
                continue
            }
            val file = File(filePath)
            if (!file.exists()) {
                println("ERROR! File not existed!")
                continue
            }
            if (!file.isDirectory) {
                println("ERROR! File not a Folder!")
                continue
            }
            return file
        }
    }

    fun getXMLResource(serializer: Serializer, title: String, oldFilePath: String? = null): Pair<String, XMLResource> {
        var haveTryOldPath = false
        while (true) {
            val filePath: String? = if (oldFilePath != null && !haveTryOldPath) {
                haveTryOldPath = true
                oldFilePath
            } else {
                println(title)
                readLine()
            }
            if (filePath.isNullOrBlank()) {
                println("ERROR! Incorrect file path!")
                continue
            }
            val file = File(filePath)
            if (!file.exists()) {
                println("ERROR! File not existed!")
                continue
            }
            try {
                return Pair(filePath, serializer.read(XMLResource::class.java, file.inputStream()))
            } catch (e: Exception) {
                println("ERROR! Read file failed! $e")
            }
        }
    }

    fun getLines(): List<String> {
        val list = mutableListOf<String>()
        var line = ""
        while (readLine()?.also { line = it }?.lowercase() != "q") {
            list.add(line)
        }
        return list
    }


}