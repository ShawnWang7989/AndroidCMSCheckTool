package action

import kotlinx.coroutines.*
import org.simpleframework.xml.core.Persister
import unit.InitData
import unit.Language
import unit.LanguageStringDiffData
import unit.StringKey
import util.ReadUtils
import xmlUnit.XMLString
import java.io.*

object UpdateFileAction {
    private const val FILE_TAIL = ".update"
    private const val EOF_KEYWORD = "</resources>"

    fun makeUpdateFile(
        serializer: Persister,
        initData: InitData
    ) {
        println("Please enter the string key you do \"NOT\" want to update (enter q to stop)")
        initData.dangerChangeKeys.forEach { println(it.key) }
        val keepOldList = initData.dangerChangeKeys.map { it.key } + ReadUtils.getLines()
        val results = mutableListOf<Throwable>()
        runBlocking {
            launch {
                initData.diffData.languageList.map {
                    async {
                        writeFile(serializer, it, keepOldList)
                    }
                }.awaitAll().filterNotNull().apply { results.addAll(this) }
            }
        }
        if (results.isEmpty()) {
            println("\nWrite Success!!\n")
        } else {
            results.forEach {
                if (it is FileError) {
                    println(it.msg)
                } else {
                    println(it.message)
                }
            }
        }
    }

    private suspend fun writeFile(
        serializer: Persister,
        lDiffData: LanguageStringDiffData,
        keepOldList: List<String>
    ): Throwable? = withContext(Dispatchers.IO) {
        val thisKeepOldList =
            keepOldList.map { StringKey(it) }.filter { lDiffData.map[it]?.oldString != null }.toMutableList()
        val updateFile = File("${lDiffData.newLanguageFile.file.path}${FILE_TAIL}")
        if (updateFile.exists()) {
            return@withContext FileError.UpdateFileAlreadyExist()
        }
        if (!lDiffData.newLanguageFile.file.exists()) {
            return@withContext FileError.NewLanguageFileIsMissing(lDiffData.newLanguageFile.language)
        }

        try {
            BufferedReader(FileReader(lDiffData.newLanguageFile.file.path)).use { bufferReader ->
                updateFile.bufferedWriter().use { bufferWriter ->
                    val myWriter = MyWriter(bufferWriter)
                    var line: String? = null
                    var xmlString: XMLString? = null
                    while (bufferReader.readLine()?.also { line = it } != null) {
                        try {
                            xmlString = serializer.read(XMLString::class.java, line)
                        } catch (e: Exception) {
                            if (line!!.trim().lowercase() == EOF_KEYWORD && xmlString != null) {
                                thisKeepOldList.forEach {
                                    val stringWriter = StringWriter()
                                    serializer.write(XMLString(it.key, lDiffData.map[it]!!.oldString), stringWriter)
                                    myWriter.writeLn("    ${stringWriter.buffer}")
                                }
                            }
                            myWriter.writeLn(line)
                            continue
                        }
                        val key = StringKey(xmlString.id!!)
                        if (thisKeepOldList.contains(key)) {
                            val oldString = lDiffData.map[key]?.oldString
                            if (oldString != null) {
                                thisKeepOldList.remove(key)
                                val stringWriter = StringWriter()
                                serializer.write(XMLString(xmlString.id, oldString), stringWriter)
                                myWriter.writeLn("    ${stringWriter.buffer}")
                                continue
                            }
                        }
                        myWriter.writeLn(line)
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext FileError.UnknownFileError(e)
        }
        return@withContext null
    }

    private class MyWriter(private val bufferedWriter: BufferedWriter) {
        private var haveWritten: Boolean = false
        fun writeLn(s: String?) {
            if (s == null)
                return
            if (haveWritten) {
                bufferedWriter.newLine()
            } else {
                haveWritten = true
            }
            bufferedWriter.write(s)
        }
    }

    private sealed class FileError(val msg: String) : IOException() {
        class UpdateFileAlreadyExist : FileError("Error!! File already exist!")
        class NewLanguageFileIsMissing(language: Language) :
            FileError("Error!! new ${language.name} file is missing!!")

        class UnknownFileError(e: Throwable) : FileError("Error!! ${e.message}")
    }
}