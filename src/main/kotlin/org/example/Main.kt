package org.example
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.net.InetAddress

fun <T> ResultSet.map(transform: (ResultSet) -> T): List<T> {
    return generateSequence { if (next()) this else null }
        .map(transform)
        .toList()
}

fun getLocalIpAddress(): String {
    return InetAddress.getLocalHost().hostAddress
}

fun isRunningLocally() : Boolean {
    return getLocalIpAddress() == "192.168.1.175"
}

fun getConnection(): Connection? {

    val hostAddr = if (isRunningLocally()) "localhost" else "192.168.1.175"

    val jdbcUrl = "jdbc:mysql://${hostAddr}:3306/bookdb"
    val password = if (isRunningLocally()) "ThePigeonRiverFlooded595" else "ThePigeonRiverFlooded595!"
    val username = "mark"

    return try {
        DriverManager.getConnection(jdbcUrl, username, password)
    } catch (e: SQLException) {
        e.printStackTrace()
        null
    }
}

data class BookRecord(val id: Int, var title: String, var publisherId: String, var filePath: String,
                      var author: String, var isRead: Boolean, var isFavorite: Boolean) {
    constructor(rs: ResultSet) : this(rs.getInt("id"), rs.getString("title"), rs.getString("publisher_id"),
            rs.getString("file_path"), rs.getString("author"), rs.getBoolean("is_read"), rs.getBoolean("is_favorite"))
}

fun addBook(connection: Connection, book: BookRecord): Int {
    val sql = "INSERT INTO books (title, author, file_path, publisher_id) VALUES (?, ?, ?, ?)"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, book.title)
    preparedStatement.setString(2, book.author)
    preparedStatement.setString(3, book.filePath)
    preparedStatement.setString(4, book.publisherId)
    return preparedStatement.executeUpdate()
}

fun updateBookStatus(connection: Connection, book: BookRecord): Int {
    val sql = "UPDATE books SET is_read = ?, is_favorite = ? where id = ?"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setInt(1, if (book.isRead) 1 else 0)
    preparedStatement.setInt(2, if (book.isFavorite) 1 else 0)
    preparedStatement.setInt(3, book.id)
    return preparedStatement.executeUpdate()
}

fun updateBookTitle(connection: Connection, book: BookRecord): Int {
    val sql = "UPDATE books SET title = ? where id = ?"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, book.title)
    preparedStatement.setInt(2, book.id)
    return preparedStatement.executeUpdate()
}

fun getBooksByAuthor(connection: Connection, author: String) : List<BookRecord> {
    val sql = "SELECT * FROM books WHERE author = ? ORDER BY publisher_id"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, author)
    val resultSet = preparedStatement.executeQuery(sql)

    return resultSet.map { BookRecord(it) }
}

fun getAllBooksOrderedByPublisher(connection: Connection) : List<BookRecord> {
    val sql = "SELECT * FROM books ORDER BY publisher_id"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(sql)

    return resultSet.map { BookRecord(it) }
}

fun getAllBooksGroupedByAuthor(connection: Connection) : Map<String, List<BookRecord>> {
    val books = getAllBooksOrderedByPublisher(connection)
    return books.groupBy { it.author }
}

fun getAllAuthors(connection: Connection) : List<String> {
    val sql = "SELECT DISTINCT author FROM books ORDER BY author"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(sql)

    return resultSet.map { it.getString("author") }
}

fun printBook(book : BookRecord) {
    val pubID = String.format("%8s", book.publisherId)
    val title = String.format("%15s", book.title).trim()
    val msg = """
        |${pubID} - $title (${book.filePath})
        |           ${book.isRead}/${book.isFavorite}
        """.trimMargin()
    println(msg)
}

fun printAuthorBooks(author: String, recs: List<BookRecord>) {
    println(author)
    recs.forEach { printBook(it) }
}

fun main() {
    println("Local Address = " + getLocalIpAddress())
    val conn = getConnection()
    if (conn != null) {
//        getAllBooks(conn).forEach { printBook(it) }
//        getAllAuthors(conn).forEach { println(it) }
        val coll = getAllBooksGroupedByAuthor(conn)
        coll.keys.sorted().forEach { author ->
            val books = coll[author]
            if (books != null) {
                printAuthorBooks(author, books)
            }
        }
    }
}