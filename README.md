# kdb

![https://mallumo.jfrog.io/artifactory/gradle-dev-local/tk/mallumo/kdb/](https://img.shields.io/maven-metadata/v?color=%234caf50&metadataUrl=https%3A%2F%2Fmallumo.jfrog.io%2Fartifactory%2Fgradle-dev-local%2Ftk%2Fmallumo%2Fkdb%2Fmaven-metadata.xml&style=for-the-badge "Version")


* sqlite wrapper for desktop and mobile
* Similiar to [ROOM](https://developer.android.com/jetpack/androidx/releases/room?hl=en) **BUT**
  * faster compilation time
  * faster runtime
  * simply to use
  * automatic database changes except rename column -> NEVER DO THAT

## About
* no reflection
* no kapt
* fast
* can be used for android and jvm-desktop in future ios too
* direct work with objects
* automatic generating extension function of (query, insert, update, delete)
* **automatic database changes without programmer interaction:**
  * add table
  * add/remove column
  * change column type
  * apply/remove colmum index
  * apply/remove colmum unique identifier

## Rules
* table objects must have empty constructor
* fields of table objects must have default values
* fields of table must be **NOT** nullable
* enabled field types: String(TEXT), Int(INTEGER), Long(INTEGER), Double(REAL), Float(REAL)

## Example

### TABLE CLASS
```kotlin
import tk.mallumo.kdb.KdbTable
import tk.mallumo.kdb.KdbColumnUnique
import tk.mallumo.kdb.KdbColumnIndex

@KdbTable
open class TEST_TABLE(
    @KdbColumnIndex @KdbColumnUnique var item_string: String = "",
    @KdbColumnIndex var item_double: Double = 11.0,
    @KdbColumnUnique var item_float: Float = 0F,
    var item_int: Int = 0,
    var item_long: Long = 0,
)
```

### Objects just for custom query
```kotlin
import tk.mallumo.kdb.KdbQI

@KdbQI
open class BindingTEST_TABLE(var x: Double = 1.3):TEST_TABLE()

@KdbQI
open class BindingTEST(var xyz: Int = 0)
```

### Instance of database
```kotlin
import tk.mallumo.kdb.createKDB

val kdb by lazy { createKDB(MainApplication.instance) }
```

### Insert, Query, Delete, Update
```kotlin
// insert
kdb.insert.test_table(TEST_TABLE(item_string = "a", item_float = 1F))

kdb.insert.test_table(arrayOf(BindingTEST_TABLE().apply {item_string = "b"; item_float = 2F },
                             TEST_TABLE(item_string = "c", item_float = 3F)))

kdb.insert.test_table(listOf(TEST_TABLE(item_string = "d", item_float = 4F),
                             TEST_TABLE(item_string = "e", item_float = 5F)))
//query
val items = kdb.query.test_table("SELECT * FROM test_table")
val items2 = kdb.query.binding_test_table("SELECT t.*, 123.0 as x  FROM test_table t ")
val items3 = kdb.query.binding_test("SELECT date('now') as xyz ")

//delete
kdb.delete.test_table(where = "item_string = 'c'")

//update
kdb.update.test_table(where = "item_string = 'a'",
     mapOf("item_string" to "item_float",
           "item_float" to 558F,
            "item_int" to 16))

kdb.update.test_table(where = "item_string = 'b'",
     mapOf("item_string" to "'abcd ...'"))
```

## How to implement

1. add plugin (**build.gradle**)
```groovy
plugins {
    id("kotlin-ksp") version "1.4.0-dev-experimental-20200914"
}
//...
android{
    //...
}
//...

apply from: 'https://raw.githubusercontent.com/mallumoSK/kdb/main/ksp-config.gradle'

//ANDROID:
apply from: 'https://raw.githubusercontent.com/mallumoSK/kdb/main/ksp-kdb-android.gradle'
//JVM DESKTOP:
apply from: 'https://raw.githubusercontent.com/mallumoSK/kdb/main/ksp-kdb-jvm_desktop.gradle'

dependencies {
    implementation "tk.mallumo:kdb:0.0.1"
    ksp "tk.mallumo:kdb:x.y.z"
}
```

2. add pluginResolutionStrategy On top of file **settings.gradle** add this:
```groovy
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if ("kotlin-ksp".equals(requested.id.id)) {
                useModule("org.jetbrains.kotlin:kotlin-ksp:${requested.version}")
            }
            if ("org.jetbrains.kotlin.kotlin-ksp".equals(requested.id.id)) {
                useModule("org.jetbrains.kotlin:kotlin-ksp:${requested.version}")
            }
            if ("org.jetbrains.kotlin.ksp".equals(requested.id.id)) {
                useModule("org.jetbrains.kotlin:kotlin-ksp:${requested.version}")
            }
        }
    }
    repositories {
        gradlePluginPortal()
        maven {
            url = "https://dl.bintray.com/kotlin/kotlin-eap"
        }
        google()
    }
}
```

3. JOB DONE :)