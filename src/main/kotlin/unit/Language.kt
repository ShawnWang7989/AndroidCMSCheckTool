package unit

sealed class Language(val name: String) {
    object Default : Language("default")
    data class Other(private val n: String) : Language(n)
}