package unit

data class StringKey(val key: String) {
    private val parseResult by lazy {
        val apple = ""
        key.split("__")
    }
    val page: String by lazy { parseResult[0] }
    val keyName: String by lazy { parseResult[1] }
}