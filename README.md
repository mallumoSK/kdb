# kdb

## KDB: ![https://mallumo.jfrog.io/artifactory/gradle-dev-local/tk/mallumo/kdb/](https://img.shields.io/maven-metadata/v?color=%234caf50&metadataUrl=https%3A%2F%2Fmallumo.jfrog.io%2Fartifactory%2Fgradle-dev-local%2Ftk%2Fmallumo%2Fkdb%2Fmaven-metadata.xml&style=for-the-badge "Version")

## KDB-KSP: ![https://mallumo.jfrog.io/artifactory/gradle-dev-local/tk/mallumo/kdb-ksp/](https://img.shields.io/maven-metadata/v?color=%234caf50&metadataUrl=https%3A%2F%2Fmallumo.jfrog.io%2Fartifactory%2Fgradle-dev-local%2Ftk%2Fmallumo%2Fkdb-ksp%2Fmaven-metadata.xml&style=for-the-badge "Version")

```

//Current version
kotlin_version = '1.7.0'
ksp = 1.7.0-1.0.6

//Previous
kotlin_version = '1.6.10'
ksp = 1.6.10-1.0.2

//Previous
kotlin_version = '1.5.10'
ksp = 1.5.10-1.0.0-beta01

//Previous
kotlin_version = '1.4.32'
kdb = 1.1.0
kdb-ksp = 1.1.0
ksp = 1.4.32-1.0.0-alpha07

//Previous
kotlin_version = '1.4.31'
kdb = 1.0.1
kdb-ksp = 1.0.1
```

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
* automatic generating suspend extension function of (query, insert, update, delete)
* all functions work on ``Dispatchers.IO`` and synchronized by ``kotlinx.coroutines.sync.Mutex``
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
  id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

//...

android{
  //...
  sourceSets.main.java.srcDirs += ['build/generated/ksp/debug/kotlin']
}

//...

repositories {
  maven {
    url = uri("https://mallumo.jfrog.io/artifactory/gradle-dev-local")
  }
}
//...

dependencies {
  implementation "tk.mallumo:kdb:x.y.z"
  ksp "tk.mallumo:kdb-ksp:x.y.z"
    
  //    in case of desktop add:
  implementation("org.xerial:sqlite-jdbc:x.x.x")

}
```

2. add pluginManagement **On top** of file **settings.gradle** :
```groovy
pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}
```

3. JOB DONE :)
