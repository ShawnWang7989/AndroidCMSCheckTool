package unit

import java.io.File

data class LanguageFile(val language: Language, val isNew: Boolean, val file: File) {
}