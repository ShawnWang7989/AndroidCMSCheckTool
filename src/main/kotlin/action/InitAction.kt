package action

import extensions.addNotRepeating
import kotlinx.coroutines.*
import org.simpleframework.xml.core.Persister
import unit.*
import util.FileSearchUtils
import util.ReadUtils
import util.VarCheckUtils
import xmlUnit.XMLResource
import java.io.File

object InitAction {

    fun initStringRes(serializer: Persister, oldData: DiffData? = null): InitData {
        val rootFolder = ReadUtils.getFolder("Please enter the App root folder path", oldData?.appRootFolderPath)
        val diffData = initDiffData(serializer, rootFolder.path, oldData)

        println("\nPlease wait...\n")

        val needCheckUsageList = mutableListOf<StringKey>()
        diffData.languageList.forEach { lsd ->
            lsd.map.forEach {
                if (it.value.diffResult.isNeedCheck) {
                    needCheckUsageList.addNotRepeating(it.key)
                }
            }
        }
        needCheckUsageList.sortBy { it.key }
        val searchResult = FileSearchUtils.searchTextInFile(rootFolder, needCheckUsageList.map { it.key })
        val dangerChangeList = mutableListOf<StringKey>()
        val normalChangeList = mutableListOf<StringKey>()
        diffData.languageList.forEach { lsd ->
            needCheckUsageList.forEachIndexed { index, key ->
                lsd.map[key]?.diffResult?.run {
                    isNeedCheck = isNeedCheck && (searchResult[index] != null)
                    if (isNeedCheck) {
                        if (isDanger) {
                            dangerChangeList.addNotRepeating(key)
                        } else {
                            normalChangeList.addNotRepeating(key)
                        }
                    }
                }
            }
        }

        println("\nDanger Change Strings: (count of variable is changed or the string used in code is missing)")
        showLog(diffData, dangerChangeList)
        println()
        println("Normal Change Strings:")
        showLog(diffData, normalChangeList)


//        (dangerChangeList + normalChangeList).filter { key ->
//            diffData.defaultLanguage.map[key]?.let { it.oldString != it.newString && it.newString!=null && it.oldString!=null} ?: false
//        }.apply {
//            showLog(diffData, this)
//            println("\n\nThere are ${this.size} difference.\n\n")
//
//            this.forEach {
//                println(it.key)
//            }
//        }

        println("All Change keys:")
        (dangerChangeList + normalChangeList).forEach { println(it.key) }

        println("\nThere are ${dangerChangeList.size + normalChangeList.size} difference.")

        return InitData(dangerChangeList, normalChangeList, diffData)
    }

    private fun showLog(diffData: DiffData, candidateList: List<StringKey>) {
        candidateList.forEach { key ->
            println("------------------")
            println("key       : ${key.key}")
            val default = diffData.defaultLanguage.map[key]
            println("old default: ${default?.oldString ?: "null"}")
            if (default?.diffResult?.isNeedCheck == true) {
                println("new default: ${default.newString ?: "null"}")
            }

            diffData.languageList.filter { it.newLanguageFile.language != Language.Default }.forEach {
                val other = it.map[key]
                if (other?.diffResult?.isNeedCheck == true) {
                    println("old ${it.newLanguageFile.language.name}: ${other.oldString ?: "null"}")
                    println("new ${it.oldLanguageFile.language.name}: ${other.newString ?: "null"}")
                }
            }
        }
    }

    private fun initDiffData(serializer: Persister, appRootFolderPath: String, oldData: DiffData? = null): DiffData {
        val default = initDefaultLanguage(serializer, oldData)
        val other = addLanguage(serializer, oldData)
        val all = mutableSetOf(default).apply {
            addAll(other)
        }
        val result = mutableListOf<LanguageStringDiffData>()
        runBlocking {
            launch {
                val jobList = all.map { async { covertToLanguageStringDiffData(it) } }
                result.addAll(jobList.awaitAll())
            }
        }
        return DiffData(appRootFolderPath, result.first(), result)
    }

    private suspend fun covertToLanguageStringDiffData(resData: ResLanguageData): LanguageStringDiffData =
        withContext(Dispatchers.Default) {
            val map = mutableMapOf<StringKey, StringDiffData>()
            val allKey = mutableListOf<StringKey>()
            resData.newData.data.entriesList?.map { StringKey(it.id!!) }?.run { allKey.addAll(this) }
            resData.oldData.data.entriesList?.forEach { xmlString ->
                if (allKey.find { it.key == xmlString.id } == null) {
                    allKey.add(StringKey(xmlString.id!!))
                }
            }
            allKey.forEach loop@{ key ->
                val oldString = resData.oldData.data.entriesList?.find { it.id == key.key }?.text
                val newString = resData.newData.data.entriesList?.find { it.id == key.key }?.text
                if (oldString == newString) {
                    map[key] =
                        StringDiffData(
                            key,
                            oldString,
                            newString,
                            DiffResult(isNeedCheck = false, isDanger = false)
                        )
                    return@loop
                }
                val isDanger = (newString == null && resData.language == Language.Default)
                        || !VarCheckUtils.haveSameVar(oldString, newString)
                map[key] =
                    StringDiffData(
                        key,
                        oldString,
                        newString,
                        DiffResult(isNeedCheck = true, isDanger = isDanger)
                    )
            }
            LanguageStringDiffData(resData.oldData.file, resData.newData.file, map)
        }

    private fun initDefaultLanguage(serializer: Persister, oldData: DiffData? = null): ResLanguageData {
        val old = ReadUtils.getXMLResource(
            serializer,
            "Please enter old default strings.xml path",
            oldData?.defaultLanguage?.oldLanguageFile?.file?.path
        )
        val new = ReadUtils.getXMLResource(
            serializer,
            "Please enter new default strings.xml path",
            oldData?.defaultLanguage?.newLanguageFile?.file?.path
        )
        val oldLanguageFile = LanguageFile(Language.Default, false, File(old.first))
        val oldResWithFile = ResWithFile(oldLanguageFile, old.second)
        val newLanguageFile = LanguageFile(Language.Default, true, File(new.first))
        val newResWithFile = ResWithFile(newLanguageFile, new.second)
        return ResLanguageData(Language.Default, oldResWithFile, newResWithFile)
    }

    private fun addLanguage(serializer: Persister, oldData: DiffData? = null): List<ResLanguageData> {
        val list = mutableListOf<ResLanguageData>()
        if (oldData == null) {
            while (true) {
                println("Do you want to add a new language? (Y/N)")
                if (!ReadUtils.getYN()) {
                    break
                }
                println("Please enter the language name")
                list.add(readLanguage(serializer))
            }
        } else {
            oldData.languageList.filter { it.newLanguageFile.language != Language.Default }.forEach {
                list.add(readLanguage(serializer, it))
            }
        }
        return list
    }

    private fun readLanguage(serializer: Persister, oldDiffData: LanguageStringDiffData? = null): ResLanguageData {
        val name = oldDiffData?.oldLanguageFile?.language?.let { it as Language.Other }?.name ?: ReadUtils.getLine()
        val language = Language.Other(name)
        val old = ReadUtils.getXMLResource(
            serializer,
            "Please enter old $name strings.xml path",
            oldDiffData?.oldLanguageFile?.file?.path
        )
        val oldLanguageFile = LanguageFile(language, false, File(old.first))
        val oldResWithFile = ResWithFile(oldLanguageFile, old.second)
        val new = ReadUtils.getXMLResource(
            serializer,
            "Please enter new $name strings.xml path",
            oldDiffData?.newLanguageFile?.file?.path
        )
        val newLanguageFile = LanguageFile(language, true, File(new.first))
        val newResWithFile = ResWithFile(newLanguageFile, new.second)
        return ResLanguageData(language, oldResWithFile, newResWithFile)
    }

    private data class ResLanguageData(val language: Language, val oldData: ResWithFile, val newData: ResWithFile)
    private data class ResWithFile(val file: LanguageFile, val data: XMLResource)
}