package org.elsoft.dbops

import java.io.File
import java.net.InetAddress
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.*

data class BookRecord(val id: Int, var title: String, var publisherId: String, var filePath: String,
                      var author: String, var isRead: Boolean, var isFavorite: Boolean) {
    constructor(rs: ResultSet) : this(rs.getInt("id"), rs.getString("title"), rs.getString("publisher_id"),
        rs.getString("file_path"), rs.getString("author"), rs.getBoolean("is_read"), rs.getBoolean("is_favorite"))
}

class DBOps(config: Properties? = null) {

    private val props: Properties = config ?: loadDefaultConfig()

    val connection: Connection = DriverManager.getConnection(
        if (isRunningLocally()) props.getProperty("db.url.local") else props.getProperty("db.url.remote"),
        props.getProperty("db.user"),
        if (isRunningLocally()) props.getProperty("db.password.local") else props.getProperty("db.password.remote")
    )

    private fun loadDefaultConfig(): Properties {
        val props = Properties()
        props.load(File("src/main/resources/properties").inputStream())
        return props
    }

    fun <T> ResultSet.map(transform: (ResultSet) -> T): List<T> {
        return generateSequence { if (next()) this else null }
            .map(transform)
            .toList()
    }

    private fun getLocalIpAddress(): String {
        return InetAddress.getLocalHost().hostAddress
    }

    private fun isRunningLocally() : Boolean {
        return getLocalIpAddress() == "192.168.1.175"
    }

    fun close() : Unit {
        connection.close()
    }

    fun addBook(book: BookRecord): Int {
        val conn = connection
        val sql = """
            |INSERT INTO books (title, author, file_path, publisher_id, is_read, is_favorite) 
            |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin()
        val preparedStatement = conn.prepareStatement(sql)
        preparedStatement.setString(1, book.title)
        preparedStatement.setString(2, book.author)
        preparedStatement.setString(3, book.filePath)
        preparedStatement.setString(4, book.publisherId)
        preparedStatement.setBoolean(5, book.isRead)
        preparedStatement.setBoolean(6, book.isFavorite)
        return preparedStatement.executeUpdate()
    }

    fun updateBookStatus(book: BookRecord): Int {
        val conn = connection
        val sql = "UPDATE books SET is_read = ?, is_favorite = ? where id = ?"
        val preparedStatement = conn.prepareStatement(sql)
        preparedStatement.setInt(1, if (book.isRead) 1 else 0)
        preparedStatement.setInt(2, if (book.isFavorite) 1 else 0)
        preparedStatement.setInt(3, book.id)
        return preparedStatement.executeUpdate()
    }

    fun updateBookTitle(book: BookRecord): Int {
        val conn = connection
        val sql = "UPDATE books SET title = ? where id = ?"
        val preparedStatement = conn.prepareStatement(sql)
        preparedStatement.setString(1, book.title)
        preparedStatement.setInt(2, book.id)
        return preparedStatement.executeUpdate()
    }

    fun getBooksByAuthor(author: String) : List<BookRecord> {
        val conn = connection
        val sql = "SELECT * FROM books WHERE author = ? ORDER BY publisher_id"
        val preparedStatement = conn.prepareStatement(sql)
        preparedStatement.setString(1, author)
        val resultSet = preparedStatement.executeQuery(sql)

        return resultSet.map { BookRecord(it) }
    }

    fun getAllBooksOrderedByPublisher() : List<BookRecord> {
        val conn = connection
        val sql = "SELECT * FROM books ORDER BY publisher_id"
        val statement = conn.createStatement()
        val resultSet = statement.executeQuery(sql)

        return resultSet.map { BookRecord(it) }
    }

    fun getAllAuthors() : List<String> {
        val conn = connection
        val sql = "SELECT DISTINCT author FROM books ORDER BY author"
        val statement = conn.createStatement()
        val resultSet = statement.executeQuery(sql)

        return resultSet.map { it.getString("author") }
    }

    fun getAllBooksGroupedByAuthor() : Map<String, List<BookRecord>> {
        val books = getAllBooksOrderedByPublisher()
        return books.groupBy { it.author }
    }
}