package util

import kotlinx.coroutines.*
import java.io.File

object FileSearchUtils {
    private const val IS_SEARCH_ENABLE = true
    private const val REX_RULE = "[a-zA-Z0-9_]+"

    private val skipFolders = listOf("build")
    private val skipFiles = listOf("strings_cms.xml")

    fun searchTextInFile(file: File, list: List<String>): List<String?> {
        val stack = mutableListOf<File>()
        val threadList = mutableListOf<Deferred<List<String?>>>()
        val resultPathList = mutableListOf<String?>()
        if(!IS_SEARCH_ENABLE){
            return list.map { file.path }
        }
        stack.add(file)
        runBlocking {
            launch {
                while (stack.isNotEmpty()) {
                    val f = stack.last()
                    stack.removeLast()
                    if (f.name.startsWith("."))
                        continue
                    if (f.isDirectory) {
                        if (!skipFolders.contains(f.name))
                            f.listFiles().takeIf { !it.isNullOrEmpty() }?.run { stack.addAll(this) }
                        continue
                    }
                    if (skipFiles.contains(f.name)) {
                        continue
                    }
                    threadList.add(async {
                        checkIsContains(f, list)
                    })
                }
                val result = threadList.awaitAll()
                list.forEachIndexed { index, _ ->
                    resultPathList.add(result.find { it[index] != null }?.get(index))
                }
            }
        }
        return resultPathList
    }

    private suspend fun checkIsContains(file: File, targetList: List<String>): List<String?> =
        withContext(Dispatchers.Default) {
            val s = file.readText()
            val list = mutableListOf<String?>()
            targetList.forEach {
                list.add(if (isContains(s, it)) file.path else null)
            }
            list
        }

    private fun isContains(fileString: String, key: String): Boolean {
        var mainString = fileString

        while (true) {
            val start = mainString.indexOf(key)
            if (start < 0) {
                return false
            }
            if (start + key.length + 1 < mainString.length) {
                val tail = mainString.substring(start + key.length, start + key.length + 1)
                if (tail.matches(REX_RULE.toRegex())) {
                    mainString = mainString.substring(start + key.length + 1, mainString.length)
                    continue
                }
            }
            if (start - 1 >= 0) {
                val head = mainString.substring(start - 1, start)
                if (head.matches(REX_RULE.toRegex())) {
                    mainString = mainString.substring(start + key.length, mainString.length)
                    continue
                }
            }
            return true
        }
    }
}