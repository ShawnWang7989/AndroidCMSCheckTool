package extensions

fun <T> MutableList<T>.addNotRepeating(data: T) {
    if (!contains(data)) {
        add(data)
    }
}