package unit

data class InitData(val dangerChangeKeys: List<StringKey>, val normalChangeKeys: List<StringKey>, val diffData: DiffData) {
}