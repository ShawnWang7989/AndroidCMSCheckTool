package unit

data class DiffData(
    val appRootFolderPath: String,
    val defaultLanguage: LanguageStringDiffData,
    val languageList: List<LanguageStringDiffData>
) {
}