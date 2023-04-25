package unit

data class LanguageStringDiffData(
    val oldLanguageFile: LanguageFile, val newLanguageFile: LanguageFile, val map: Map<StringKey, StringDiffData>
) {
}