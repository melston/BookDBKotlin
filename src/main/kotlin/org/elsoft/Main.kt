package org.elsoft

import org.elsoft.dbops.BookRecord
import org.elsoft.dbops.DBOps
import java.io.File
import java.util.*

fun printBook(book : BookRecord, indent: String = "") {
    val pubID = String.format("%8s", book.publisherId)
    val title = String.format("%15s", book.title).trim()
    val msg = """
        |${indent}${pubID} - $title (${book.filePath})
        |${indent}           ${book.isRead}/${book.isFavorite}
        """.trimMargin()
    println(msg)
}

fun printAuthorBooks(author: String, recs: List<BookRecord>) {
    println(author)
    recs.forEach { printBook(it, "    ") }
}

fun main() {
    val props = Properties()
    props.load(File("src/main/resources/properties").inputStream())
    val db = DBOps(props)
    db.getAllBooksOrderedByPublisher()
        .filter { it.title.contains( '_' ) }
        .forEach { println("${it.publisherId} - ${it.title}") }
//    DBOps.getAllAuthors().forEach { println(it) }
//    val coll = DBOps.getAllBooksGroupedByAuthor()
//    coll.keys.sorted().forEach { author ->
//        val books = coll[author]
//        if (books != null) {
//            printAuthorBooks(author, books)
//        }
//    }
}