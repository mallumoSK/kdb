package tk.mallumo.kdb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val coroutineKdbDispatcher: CoroutineDispatcher
    get() = Dispatchers.IO