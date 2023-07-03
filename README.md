# kdb

## KDB: ![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.repsy.io%2Fmvn%2Fmallumo%2Fpublic%2Ftk%2Fmallumo%2Fkdb%2Fmaven-metadata.xml)


## KDB-KSP: ![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.repsy.io%2Fmvn%2Fmallumo%2Fpublic%2Ftk%2Fmallumo%2Fkdb-ksp%2Fmaven-metadata.xml)



* sqlite wrapper for desktop and android
* **Main GOALS**
  * faster compilation time
  * faster runtime
  * simply to use
  * **automatic database changes** based on class annotations, except rename column -> **NEVER DO THAT**
  * use/write complex query (in runtime or remote delivery)

## About
* no reflection
* no kapt
* can be used for android and jvm-desktop
* direct work with objects
* automatic generating suspend extension function of (query, insert, update, delete)
* all tasks are synchronized by ``kotlinx.coroutines.sync.Mutex``
* you can use yours own jdbc implementations for other database engines, by custom implementation of ``tk.mallumo.kdb.sqlite.SqliteDB``, ``tk.mallumo.kdb.sqlite.Cursor``, ``tk.mallumo.kdb.sqlite.DbInsertStatement`` 
  * mysql
  * oracle
  * ...
  
* **automatic database changes without programmer interaction:**
    * add table
    * add/remove column
    * change column type
    * apply/remove colmum index
    * apply/remove colmum unique identifier
    * generating common code with gradle params:
        * see test-desktop/build.gradle.kts
            * kotlin.srcDir("build/generated/ksp/common/commonMain/kotlin")
            * ksp.arg("commonSourcesOnly" , "true")

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
open class BindingTEST(var xyz: String = "")
```

### Instance of database
```kotlin
import tk.mallumo.kdb


// ANDROID:
val kdb by lazy {
    Kdb.get(SqliteDB(isDebug = true, dbPath = applicationContext.defaultSqLitePath()))
}

//DESKTOP:
val kdb: Kdb by lazy {
    Kdb.get(SqliteDB(isDebug = true, isSqLite = true) {
        DriverManager.getConnection("jdbc:sqlite:/tmp/test.sqlite").apply {
            autoCommit = false
        }
    })

}
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
    id("com.google.devtools.ksp") version "1.8.20-1.0.11"
}
```
```groovy
// ANDROID
android {
    sourceSets.apply {
        getByName("debug") {
            java.srcDirs("build/generated/ksp/debug/kotlin")
        }
        getByName("release") {
            java.srcDirs("build/generated/ksp/release/kotlin")
        }
    }
}

// DESKTOP
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
```

```groovy
dependencies {
    ksp("tk.mallumo:kdb-ksp:<version>")
    implementation("tk.mallumo:kdb:<version>")

    //    in case of desktop add:
    implementation("org.xerial:sqlite-jdbc:<version>")
}

repositories {
    maven("https://repo.repsy.io/mvn/mallumo/public")
}
```

2. add pluginManagement **On top** of file **settings.gradle** :
```groovy
pluginManagement {
  repositories {
    gradlePluginPortal()
  }
}
```

3. add first table-class qith annotation of ``@KdbTable``
```kotlin
@KdbTable
class TEST_TABLE(
    var xyz: String = ""
)
```

4. build application or run gradle task whitch starts with **ksp** etc. ``kspKotlin``, ``kspDebugKotlin``, ...  
4.1 all functions (instance of database, insert, update, delete, query ) will be generated automatically

 
5. JOB DONE :)
