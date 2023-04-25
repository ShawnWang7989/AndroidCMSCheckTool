import action.InitAction
import action.UpdateFileAction
import org.simpleframework.xml.core.Persister
import unit.InitData
import util.ReadUtils

fun main() {

    val serializer = Persister()

    var initData: InitData? = null
    while (true) {
        initData = InitAction.initStringRes(serializer,initData?.diffData)

        println("\nDo you want to retry? (Y/N)")
        if (ReadUtils.getYN()) {
            continue
        }

        println("\nDo you want to make a update file? (Y/N)")
        if(!ReadUtils.getYN()){
            return
        }
        UpdateFileAction.makeUpdateFile(serializer, initData)
        return
    }
}