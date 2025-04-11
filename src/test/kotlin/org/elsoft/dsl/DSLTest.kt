package org.elsoft.dsl

import org.elsoft.dbops.BookRecord
import org.elsoft.dbops.DBOps
import org.elsoft.dsl.*
import org.junit.jupiter.api.*
import java.util.*

class DSLTest {

    private lateinit var db: DBOps
    private val allBooks = listOf(
        BookRecord(0, "Test Book1", "AB-1001", "somepath1", "Author X", true, false),
        BookRecord(1, "Test Book2", "AB-1002", "somepath2", "Author X", false, false),
        BookRecord(2, "Test Book3", "AB-1003", "somepath3", "Author Y", true, true),
        BookRecord(3, "Test Book4", "AB-1004", "somepath4", "Author Y", false, false),
        BookRecord(4, "Test Book5", "AB-1005", "somepath5", "Author Y", false, false)
    )

    @BeforeEach
    fun setUp() {
        val props = Properties().apply {
            setProperty("db.url.local", "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=0")
            setProperty("db.url.remote", "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=0")
            setProperty("db.user", "sa")
            setProperty("db.password.local", "")
            setProperty("db.password.remote", "")
        }

        db = DBOps(props)
        initSchema()
    }

    @AfterEach
    fun tearDown() {
        db.close()
    }

    private fun initSchema() {
        val schema = javaClass.getResource("/schema.sql")?.readText()
            ?: throw IllegalStateException("schema.sql not found")

        val statements = schema.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (sql in statements) {
            val stmt = db.connection.prepareStatement(sql)
            stmt.execute()
            stmt.close()
        }
    }

    private fun setupBooks() {
        allBooks.forEach { db.addBook(it) }
    }

    @Test
    fun `can change book title by title`() {
        setupBooks()

        // This syntax - book(db) - is used for testing.  The default will use the
        // MySQL connection to work with the production db.
        book(db) with { title("Test Book3") } set { title("Some Title") }

        val result = db.getBookWithPublisherID("AB-1003")
        Assertions.assertNotNull(result)
        Assertions.assertEquals("Some Title", result.title)
    }

    @Test
    fun `can change book title by pubID`() {
        setupBooks()

        book(db) with { publisherID("AB-1004") } set { title("Some Title") }

        val result = db.getBookWithPublisherID("AB-1004")
        Assertions.assertNotNull(result)
        Assertions.assertEquals("Some Title", result.title)
    }

    @Test
    fun `can change book status by pubID`() {
        setupBooks()

        book(db) with { publisherID("AB-1004") } set { isRead(true) }

        val result = db.getBookWithPublisherID("AB-1004")
        Assertions.assertNotNull(result)
        Assertions.assertEquals(true, result.isRead)
        Assertions.assertEquals(false, result.isFavorite)
    }
}