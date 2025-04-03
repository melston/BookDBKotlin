package org.example
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

fun getConnection(): Connection? {
    // val jdbcUrl = "jdbc:mysql://192.168.1.175:3306/bookdb"
    val jdbcUrl = "jdbc:mysql://localhost:3306/bookdb"
    val username = "mark"
    val password = "ThePigeonRiverFlooded595"

    return try {
        DriverManager.getConnection(jdbcUrl, username, password)
    } catch (e: SQLException) {
        e.printStackTrace()
        null
    }
}

fun insertRecord(connection: Connection) {
    val sql = "INSERT INTO users (name, email) VALUES (?, ?)"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, "John Doe")
    preparedStatement.setString(2, "john@example.com")
    val row = preparedStatement.executeUpdate()
    println("$row row(s) inserted.")
}

fun readRecords(connection: Connection) {
    val sql = "SELECT * FROM books ORDER BY publisher_id"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(sql)

    while (resultSet.next()) {
        val title = resultSet.getString("title")
        val pubID = resultSet.getString("publisher_id")
        val msg = String.format("%8s - $title", pubID)
        println(msg)
    }
}

fun updateRecord(connection: Connection) {
    val sql = "UPDATE users SET email = ? WHERE name = ?"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, "new-email@example.com")
    preparedStatement.setString(2, "John Doe")
    val row = preparedStatement.executeUpdate()
    println("$row row(s) updated.")
}

fun deleteRecord(connection: Connection) {
    val sql = "DELETE FROM users WHERE name = ?"
    val preparedStatement = connection.prepareStatement(sql)
    preparedStatement.setString(1, "John Doe")
    val row = preparedStatement.executeUpdate()
    println("$row row(s) deleted.")
}

fun main() {
    var conn = getConnection()
    if (conn != null)
        readRecords(conn)
}