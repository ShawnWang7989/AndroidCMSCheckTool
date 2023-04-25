package util

object VarCheckUtils {

    fun haveSameVar(s1: String?, s2: String?): Boolean {
        val s1VarList = findVarList(s1)
        val s2VarList = findVarList(s2)
        return s1VarList.containsAll(s2VarList) && s2VarList.containsAll(s1VarList)
    }

    private fun findVarList(s: String?): List<Int> {
        val list = ArrayList<Int>()
        if (s == null)
            return list
        var index = 0
        while (index >= 0) {
            index = s.indexOf("%", index)
            if (index < 0 || index + 1 >= s.length) {
                return list
            }
            // %% just means %. skip it
            if (s[index + 1] == '%') {
                index += 2
                continue
            }
            val result = getVar(s, index + 1)
            result.first?.apply {
                if (!list.contains(this)) {
                    list.add(this)
                }
            }
            index = result.second + 1
        }
        return list
    }

    private fun getVar(s: String, index: Int): Pair<Int?, Int> { // return var number and end index
        for (i in index until s.length) {
            val c = s[i]
            if (c.isDigit()) {
                continue
            }
            //end of num must be '$' and the sub string length must be more than 0
            if (c != '$' || index == i) {
                return Pair(null, i)
            }
            return Pair(s.substring(index, i).toInt(), i)
        }
        return Pair(null, s.length - 1)
    }
}