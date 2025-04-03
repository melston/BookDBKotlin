package org.example
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.net.InetAddress

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
                      var isRead: Boolean, var isFavorite: Boolean) {
    constructor(rs: ResultSet) : this(rs.getInt("id"), rs.getString("title"), rs.getString("publisher_id"),
            rs.getString("file_path"), rs.getBoolean("is_read"), rs.getBoolean("is_favorite"))
}

//fun insertRecord(connection: Connection) {
//    val sql = "INSERT INTO users (name, email) VALUES (?, ?)"
//    val preparedStatement = connection.prepareStatement(sql)
//    preparedStatement.setString(1, "John Doe")
//    preparedStatement.setString(2, "john@example.com")
//    val row = preparedStatement.executeUpdate()
//    println("$row row(s) inserted.")
//}

fun readRecords(connection: Connection) {
    val sql = "SELECT * FROM books ORDER BY publisher_id"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(sql)

    while (resultSet.next()) {
        val book = BookRecord(resultSet)
        val pubID = String.format("%8s", book.publisherId)
        val title = String.format("%15s", book.title)
        val msg = """
            |${pubID} - $title (${book.filePath})
            |           ${book.isRead}/${book.isFavorite}
            """.trimIndent()
        println(msg)
    }
}

//fun updateRecord(connection: Connection) {
//    val sql = "UPDATE users SET email = ? WHERE name = ?"
//    val preparedStatement = connection.prepareStatement(sql)
//    preparedStatement.setString(1, "new-email@example.com")
//    preparedStatement.setString(2, "John Doe")
//    val row = preparedStatement.executeUpdate()
//    println("$row row(s) updated.")
//}

//fun deleteRecord(connection: Connection) {
//    val sql = "DELETE FROM users WHERE name = ?"
//    val preparedStatement = connection.prepareStatement(sql)
//    preparedStatement.setString(1, "John Doe")
//    val row = preparedStatement.executeUpdate()
//    println("$row row(s) deleted.")
//}

fun main() {
    println("Local Address = " + getLocalIpAddress())
    val conn = getConnection()
    if (conn != null)
        readRecords(conn)
}