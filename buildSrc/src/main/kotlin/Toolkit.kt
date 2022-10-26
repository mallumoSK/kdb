import org.gradle.api.plugins.ExtraPropertiesExtension

class Toolkit private constructor(private val properties: ExtraPropertiesExtension) {
    companion object {
        private lateinit var instance: Toolkit
        fun get(extensions: ExtraPropertiesExtension): Toolkit {
            if (!::instance.isInitialized) {
                instance = Toolkit(extensions)
            }
            return instance
        }
    }

    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): String =  get(property.name)

    operator fun get(key:String):String = properties[key.replace("_", ".")]?.toString()!!
}
