package org.elsoft.dsl

import org.elsoft.dbops.BookRecord
import org.elsoft.dbops.DBOps
import java.util.*

// Entry point
val book: BookQuery
    get() = BookQuery(DBOps()) // default behavior

fun book(dbOps: DBOps): BookQuery {
    return BookQuery(dbOps)
}

class BookQuery(val dbOps: DBOps) {
    infix fun with(criteria: BookFilter.() -> Unit): BookSetter {
        val filter = BookFilter(dbOps).apply(criteria)
        val book = filter.result
            ?: throw IllegalArgumentException("No book found with given criteria.")

        return BookSetter(dbOps, book)
    }
}


// Filtering context
class BookFilter(val db: DBOps) {

    var result: BookRecord? = null
    private var filterUsed: String? = null

    private fun ensureOnlyOne(field: String) {
        if (filterUsed != null) {
            throw IllegalStateException("Only one filter may be used at a time. Already filtered by '$filterUsed', cannot also use '$field'.")
        }
        filterUsed = field
    }

    infix fun publisherID(value: String) {
        ensureOnlyOne("publisherID")
        result = db.getBookWithPublisherID(value)
    }

    infix fun title(value: String) {
        ensureOnlyOne("title")
        result = db.getBookWithTitle(value)
    }
}


// Mutating context
class BookSetter(private val db: DBOps, private val book: BookRecord) {
    infix fun set(updates: BookUpdater.() -> Unit) {
        val updater = BookUpdater(db, book)
        updater.apply(updates)
    }
}

class BookUpdater(private val db: DBOps, private val book: BookRecord) {
    infix fun title(value: String): BookUpdater {
        book.title = value
        db.updateBookTitle(book)
        return this
    }

    infix fun isRead(value: Boolean): BookUpdater {
        book.isRead = value
        db.updateBookStatus(book)
        return this
    }

    infix fun isFavorite(value: Boolean): BookUpdater {
        book.isFavorite = value
        db.updateBookStatus(book)
        return this
    }

    // Add similar functions for other mutable fields
}
