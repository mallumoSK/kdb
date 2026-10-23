# kdb

## KDB-CORE: ![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.repsy.io%2Fmvn%2Fmallumo%2Fpublic%2Ftk%2Fmallumo%2Fkdb-core%2Fmaven-metadata.xml)

## KDB-KSP: ![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.repsy.io%2Fmvn%2Fmallumo%2Fpublic%2Ftk%2Fmallumo%2Fkdb-ksp%2Fmaven-metadata.xml)

```bash
clear && tools/commit-llama.sh  "$(pwd)"
```

```bash
clear && tools/commit-llama.sh  "$(pwd)"
```

* High-performance, lightweight database wrapper and ORM for Kotlin Multiplatform (JVM / Desktop /
  Server)
* Supports **SQLite**, **MariaDB**, and **MySQL** with extensible JDBC support

### Main Goals

* Fast compilation time via **KSP** (no reflection, no `kapt`)
* Fast runtime with minimal overhead
* Direct typed operations with data objects
* **Automatic schema migrations** based on class annotations (add tables, add/remove columns, change
  column types, add/remove indexes and unique constraints)
* Complex query support and stored procedure calls via coroutines

---

## Features

* **Zero Reflection**: Compile-time code generation powered by KSP.
* **Coroutines First**: Extension functions for CRUD operations (`insert`, `query`, `update`,
  `delete`, `exec`, `call`) are all `suspend` functions.
* **Connection Pooling & Concurrency**: Configurable parallel connection pooling synchronized with
  `kotlinx.coroutines.sync.Mutex`.
* **Automatic Database Migrations**: Automatically applies table creation, column alterations, index
  definitions, and unique constraints at startup.
* **Built-in Connectors**:
    * **SQLite**: `DbEngine.createSQLite`
    * **MariaDB**: `DbEngine.createMariaDb` (TCP) and `DbEngine.createMariaDbSocket` (Unix domain
      socket)
    * **MySQL**: `DbEngine.createMySql`
    * **Generic JDBC**: `DbEngine.createFromUrl` or custom implementations of `DbEngine`, `Cursor`,
      and `DbInsertStatement`.
* **Vector Embeddings**: Native vector support (`FloatArray` mapped to `VECTOR` / `VEC_FromText`).

---

## Rules & Supported Types

### Table Class Rules

* Table objects must have an empty / default-value constructor.
* Fields of table objects must have default values.
* Fields must be **NOT** nullable.
* Classes must be marked with `@KdbTable`.

### Supported Field Types

| Kotlin Type     | SQL Type                         | Notes                                                          |
|:----------------|:---------------------------------|:---------------------------------------------------------------|
| `String`        | `TEXT` / `VARCHAR(size)`         | Set size via `@KdbColumnSize(size)`                            |
| `Int`           | `INTEGER`                        | 32-bit integer                                                 |
| `Long`          | `BIGINT`                         | 64-bit integer                                                 |
| `Double`        | `NUMERIC` / `DECIMAL`            | Decimal number                                                 |
| `Float`         | `NUMERIC` / `DECIMAL`            | Decimal number                                                 |
| `FloatArray`    | `VECTOR(size)`                   | Vector embeddings, dimension defined by `@KdbColumnSize(size)` |
| `LocalTime`     | `TIME` (or `TEXT` in SQLite)     | Supports `kotlinx.datetime` / `kotlin.datetime`                |
| `LocalDate`     | `DATE` (or `TEXT` in SQLite)     | Supports `kotlinx.datetime` / `kotlin.datetime`                |
| `LocalDateTime` | `DATETIME` (or `TEXT` in SQLite) | Supports `kotlinx.datetime` / `kotlin.datetime`                |

### Column Annotations

* `@KdbColumnIndex`: Creates an index on the column.
* `@KdbColumnUnique`: Adds a `UNIQUE` constraint to the column.
* `@KdbColumnSize(size = ...)`: Specifies column length (e.g. `VARCHAR(256)`) or vector dimension (
  e.g. `VECTOR(10)`).

---

## Examples

### 1. Table Class Definition

```kotlin
import tk.mallumo.kdb.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@KdbTable
open class TEST_TABLE(
    @KdbColumnIndex @KdbColumnUnique
    @KdbColumnSize(256)
    var item_string: String = "",

    @KdbColumnSize(10)
    @KdbColumnIndex 
    var item_double: Double = 11.0,

    @KdbColumnSize(10)
    @KdbColumnUnique 
    var item_float: Float = 0F,

    var item_int: Int = 0,
    var item_long: Long = 0,
    var a: String = "",

    @KdbColumnSize(10)
    var item_FloatArray: FloatArray = FloatArray(10),

    var item_time: LocalTime = LocalTime(1, 2, 3),
    var item_date: LocalDate = LocalDate(2025, 1, 1),
    var item_datetime: LocalDateTime = LocalDateTime(item_date, item_time),
)
```

### 2. Custom Query Result Objects

Use `@KdbQI` for custom query projections and joining:

```kotlin
import tk.mallumo.kdb.KdbQI

@KdbQI
open class BindingTEST_TABLE(var x: Double = 1.3) : TEST_TABLE()

@KdbQI
open class BindingTEST(var xyz: String = "")
```

### 3. Database Initialization

```kotlin
import tk.mallumo.kdb.*
import tk.mallumo.kdb.sqlite.DbEngine

val kdb: Kdb by lazy {
    // SQLite
    val engine = DbEngine.createSQLite(
        isDebug = true,
        path = "/tmp/test.sqlite"
    )

    // Or MariaDB:
    // val engine = DbEngine.createMariaDb(
    //     maxParallelConnections = 4,
    //     isDebug = true,
    //     name = "usr-name",
    //     pass = "usr-pass",
    //     database = "my_database",
    //     host = "localhost",
    //     port = 3306
    // )

    // Or MariaDB via Unix Socket:
    // val engine = DbEngine.createMariaDbSocket(
    //     maxParallelConnections = 4,
    //     isDebug = true,
    //     database = "my_database",
    //     properties = DbEngine.createProperties("usr-name", "usr-pass"),
    //     socket = "/var/run/mysqld/mysqld.sock"
    // )

    Kdb.get(
        engine = engine,
        reconfigureDatabaseOnStart = true, // Auto-apply schema migrations
        beforeInit = { println("Before DB init") },
        afterInit = { println("After DB init") },
        beforeDatabaseChange = { println("Before schema migration") },
        afterDatabaseChange = { println("After schema migration") }
    )
}
```

### 4. CRUD & Operations (Coroutines)

All operations are coroutine `suspend` functions:

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 1. Prepare database schema and apply migrations
    kdb.prepareDatabase()

    // 2. Insert (Single, Array, or List)
    val idList1: List<Long> = kdb.insert.test_table(
        TEST_TABLE(item_string = "a", item_float = 1F)
    )

    val idList2: List<Long> = kdb.insert.test_table(
        listOf(
            TEST_TABLE(item_string = "b", item_float = 2F),
            TEST_TABLE(item_string = "c", item_float = 3F)
        )
    )

    // 3. Query
    val allItems: List<TEST_TABLE> = kdb.query.test_table("SELECT * FROM test_table")
    val projected: List<BindingTEST_TABLE> = kdb.query.binding_test_table(
        "SELECT t.*, 123.0 as x FROM test_table t"
    )
    val singleValue: List<BindingTEST> = kdb.query.binding_test("SELECT date('now') as xyz")

    // 4. Update
    kdb.update.test_table(
        where = "item_string = 'a'",
        mapOf(
            "a" to "'updated value'",
            "item_float" to 558F,
            "item_int" to 16
        )
    )

    // 5. Delete
    kdb.delete.test_table(where = "item_string = 'c'")

    // 6. Direct SQL Execution
    kdb.exec("VACUUM")
    kdb.exec(listOf("DELETE FROM test_table WHERE item_int = 0", "VACUUM"))

    // 7. Stored Procedure / Callable Statement
    // var outResult = 0
    // kdb.call("CALL calculate_totals(?, ?)", ::inputArg, ::outResult)
}
```

---

## Setup & Gradle Configuration

Add the repository and dependencies to your project:

### 1. Repository Configuration (`settings.gradle.kts` or `build.gradle.kts`)

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.repsy.io/mvn/mallumo/public")
}
```

### 2. Module Configuration (`build.gradle.kts`)

```kotlin
plugins {
    kotlin("jvm") version "<kotlin-version>"
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    // KDB Core & KSP Generator
    implementation("tk.mallumo:kdb-core:<version>")
    ksp("tk.mallumo:kdb-ksp:<version>")

    // Coroutines & Datetime
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

    // Database Driver (choose one or more)
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    // implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    // implementation("mysql:mysql-connector-java:8.0.33")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/common/commonMain/kotlin")
    }
}

ksp.arg("commonSourcesOnly", "true")
```

### 3. Build & Code Generation

Run `kspKotlin` or build your project:

```bash
./gradlew build
```

All database helper objects, queries, insert, update, and delete functions are generated
automatically based on your `@KdbTable` and `@KdbQI` definitions.
