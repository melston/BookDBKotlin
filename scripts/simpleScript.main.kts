@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("mysql:mysql-connector-java:8.0.28")
@file:DependsOn("build/libs/BookDBKotlin-1.0-SNAPSHOT.jar")

import org.elsoft.dbops.BookRecord

println("Creating book record...")
val book = BookRecord(0, "Kotlin in Action", "AA-1001", "somepath", "Dmitry", true, false)
println(book)
