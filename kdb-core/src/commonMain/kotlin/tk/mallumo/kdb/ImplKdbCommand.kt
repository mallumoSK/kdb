package tk.mallumo.kdb

sealed class ImplKdbCommand constructor(val kdb: Kdb) {
    class Insert(kdb: Kdb) : ImplKdbCommand(kdb)
    class Delete(kdb: Kdb) : ImplKdbCommand(kdb)
    class Update(kdb: Kdb) : ImplKdbCommand(kdb)
    class Query(kdb: Kdb) : ImplKdbCommand(kdb)
}