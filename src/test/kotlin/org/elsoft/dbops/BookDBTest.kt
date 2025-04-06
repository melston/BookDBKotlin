package org.elsoft.dbops

import org.junit.jupiter.api.*
import java.util.*

class BookDBTest {

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

    fun setupBooks() {
        allBooks.forEach { db.addBook(it) }
    }

    @Test
    fun `can insert and retrieve book`() {
        setupBooks()

        val theBooks = db.getAllBooksOrderedByPublisher()

        Assertions.assertNotNull(theBooks)
        Assertions.assertTrue(theBooks.size == 5)

        // Caution.  This works because the publisherId of the books in the
        // 'books' list is arranged alphabetically.
        theBooks.forEachIndexed { index, book ->
            Assertions.assertTrue(book.title == allBooks[index].title)
            Assertions.assertTrue(book.author == allBooks[index].author)
            Assertions.assertTrue(book.filePath == allBooks[index].filePath)
            Assertions.assertTrue(book.publisherId == allBooks[index].publisherId)
        }
    }

    @Test
    fun `can change the title of one book`() {
        setupBooks()

        val retrieved = db.getAllBooksOrderedByPublisher()
        val newBook = retrieved.last()
        newBook.title = "Other Title"
        db.updateBookTitle(newBook)

        Assertions.assertNotNull(retrieved)
        Assertions.assertTrue(retrieved.size == 5)

        retrieved.forEachIndexed { index, b ->
            Assertions.assertEquals(retrieved[index].publisherId, b.publisherId)
            Assertions.assertEquals(retrieved[index].title, b.title)
            Assertions.assertEquals(retrieved[index].author, b.author)
            Assertions.assertEquals(retrieved[index].filePath, b.filePath, )
        }
    }
}
