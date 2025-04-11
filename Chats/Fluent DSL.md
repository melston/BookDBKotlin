
-----------------

I have a kotlin library with a DBOps class that allows me to retrieve 'BookRecord' objects from a MySQL database.  One of the methods in the DBOps class is getBookWithPublisherID(id: String).  I can also set things like the title and other fieds.

I would like a more fluent interface and was hoping to be able to handle a scripting-like api that would allow me to do something like this:

book with { publisherID "someID" } set { title "New Title" }

I am reading "Programming DSLs in Kotlin" as my inspiration but I am having trouble applying the concepts in that book to creating a dsl that would support this.  Can you help me come up with a structure for something that would support this?

-----------------

Absolutely! You're on the right track wanting a DSL to make your database operations more expressive and readable. Kotlin’s DSL capabilities are perfect for this, and your target syntax:

```kotlin
book with { publisherID("someID") } set { title("New Title") }
```

…is very doable with the right use of **infix functions**, **lambda receivers**, and maybe some **builder pattern** magic.

---

### 🧱 Concept Breakdown

To achieve this, you’ll need:
1. A `book` entry point.
2. A `with { ... }` function that allows filtering (e.g., by `publisherID`).
3. A `set { ... }` function that applies mutations to the book object.
4. A `BookQueryContext` and `BookUpdateContext` to collect and apply operations.

---

### 🛠 DSL Skeleton

```kotlin
// Entry point
val book = BookQuery()

class BookQuery {
    infix fun with(criteria: BookFilter.() -> Unit): BookSetter {
        val filter = BookFilter().apply(criteria)
        val book = DBOps().getBookWithPublisherID(filter.publisherID)
        return BookSetter(book)
    }
}

// Filtering context
class BookFilter {
    var publisherID: String = ""
    fun publisherID(value: String) {
        this.publisherID = value
    }
}

// Mutating context
class BookSetter(private val book: BookRecord) {
    infix fun set(updates: BookUpdater.() -> Unit) {
        val updater = BookUpdater(book)
        updater.apply(updates)
        DBOps().updateBook(book) // Assuming this persists the changes
    }
}

class BookUpdater(private val book: BookRecord) {
    fun title(value: String) {
        book.title = value
    }

    // Add similar functions for other mutable fields
}
```

---

### ✅ Usage

```kotlin
book with {
    publisherID("PUB-123")
} set {
    title("New Title")
}
```

This works because:
- `with` and `set` are **infix functions** (you could add the `infix` keyword for syntactic sugar),
- Lambdas with receiver are used to create a **builder-like DSL** structure.

---

### 🔧 Enhancements

You could:
- Support multiple filters (`author`, `isbn`, etc.)
- Support bulk operations (`getAllWith(...)`)
- Chain operations
- Add null checks and error handling (e.g., book not found)

---

Would you like help adding more filters (like `isbn`, `author`) or building a version that can chain multiple mutations?
-----------------

I would like to see about chaining multiple mutations
-----------------
Perfect — chaining multiple mutations fits nicely with the `BookUpdater` class. Since the book object is mutable, you just need your `BookUpdater` methods to return `this` so they can be chained.

Let me show you how to extend your current DSL to support chaining like this:

```kotlin
book with {
    publisherID("PUB-123")
} set {
    title("New Title")
    isFavorite(true)
    publicationDate("2025-01-01")
}
```

Or even:

```kotlin
book with {
    publisherID("PUB-123")
} set {
    title("Another Title")
        .isRead(true)
        .isbn("987654321")
}
```

---

### 🛠 Update the `BookUpdater` to support chaining

```kotlin
class BookUpdater(private val book: BookRecord) {

    fun title(value: String): BookUpdater {
        book.title = value
        return this
    }

    fun isFavorite(value: Boolean): BookUpdater {
        book.isFavorite = value
        return this
    }

    fun isRead(value: Boolean): BookUpdater {
        book.isRead = value
        return this
    }

    fun publicationDate(value: String): BookUpdater {
        book.publicationDate = value
        return this
    }

    fun isbn(value: String): BookUpdater {
        book.isbn = value
        return this
    }

    // Add other fields as needed
}
```

Now each method returns `this`, allowing for chained calls.

---

### ✅ No changes are needed in your `set {}` block

Your `set {}` block already applies the lambda to `BookUpdater`, so now the chaining just works:

```kotlin
book with {
    publisherID("PUB-456")
} set {
    title("Title A")
        .isRead(true)
        .isFavorite(true)
}
```

Kotlin DSLs are super elegant this way!

---

Would you like to add support for **querying by multiple fields** or maybe even **bulk updates** next?

-----------------

Will this work without the parentheses around the parameters?

----------------------------
Ah, good question — you're thinking of this:

```kotlin
title "New Title"
isFavorite true
```

Instead of:

```kotlin
title("New Title")
isFavorite(true)
```

👉 **Short answer:** Yes, it *can* work without parentheses — **if** you define the functions as **infix functions** and the argument is a single parameter (no commas, no named args, etc.).

---

### 🔧 How to Make It Work

Update your `BookUpdater` methods to use the `infix` modifier and ensure they only take a single argument:

```kotlin
class BookUpdater(private val book: BookRecord) {

    infix fun title(value: String): BookUpdater {
        book.title = value
        return this
    }

    infix fun isFavorite(value: Boolean): BookUpdater {
        book.isFavorite = value
        return this
    }

    infix fun isRead(value: Boolean): BookUpdater {
        book.isRead = value
        return this
    }

    infix fun publicationDate(value: String): BookUpdater {
        book.publicationDate = value
        return this
    }

    infix fun isbn(value: String): BookUpdater {
        book.isbn = value
        return this
    }
}
```

---

### ✅ Now you can write:

```kotlin
book with {
    publisherID "PUB-789"
} set {
    title "Fluent Kotlin DSLs"
    isRead true
    isFavorite true
}
```

Clean, readable, and very DSL-like!

---

Let me know if you want to add **custom validation**, make a **collection-based `book.allWith {}`**, or design something for **batch updates**. Happy to help you push this DSL even further!
-----------------
Let's see the custom validation
-----------------
Awesome! Adding **custom validation** is a great way to make your DSL robust — you can catch mistakes early, enforce business rules, and give meaningful feedback during scripting.

Let’s walk through how to:
1. Add validation rules (e.g. title must not be blank).
2. Throw a meaningful error *before* updating the DB.
3. Optionally allow soft warnings instead of exceptions.

---

### ✅ Step 1: Add a Validation Layer

We'll create a `validate()` method in `BookUpdater` that is called at the end of `set {}` — just before committing changes via `DBOps`.

Here’s how to hook it in:

```kotlin
class BookSetter(private val book: BookRecord) {
    infix fun set(updates: BookUpdater.() -> Unit) {
        val updater = BookUpdater(book)
        updater.apply(updates)
        updater.validate() // 🔍 validate before committing
        DBOps().updateBook(book)
    }
}
```

---

### ✅ Step 2: Define Validations in `BookUpdater`

```kotlin
class BookUpdater(private val book: BookRecord) {

    infix fun title(value: String): BookUpdater {
        book.title = value
        return this
    }

    infix fun isFavorite(value: Boolean): BookUpdater {
        book.isFavorite = value
        return this
    }

    infix fun isRead(value: Boolean): BookUpdater {
        book.isRead = value
        return this
    }

    infix fun publicationDate(value: String): BookUpdater {
        book.publicationDate = value
        return this
    }

    infix fun isbn(value: String): BookUpdater {
        book.isbn = value
        return this
    }

    fun validate() {
        if (book.title.isNullOrBlank()) {
            throw IllegalArgumentException("Book title must not be blank.")
        }
        if (book.isbn != null && book.isbn.length !in 10..13) {
            throw IllegalArgumentException("ISBN must be 10–13 characters.")
        }
        // Add more checks as needed
    }
}
```

---

### 🧩 Optional: Soft Validation Mode (Log Warnings Instead)

You could make validation more flexible by adding a mode flag:

```kotlin
class BookUpdater(private val book: BookRecord, val strict: Boolean = true) {

    fun validate() {
        val errors = mutableListOf<String>()

        if (book.title.isNullOrBlank()) {
            errors += "Book title must not be blank."
        }

        if (book.isbn != null && book.isbn.length !in 10..13) {
            errors += "ISBN must be 10–13 characters."
        }

        if (errors.isNotEmpty()) {
            if (strict) {
                throw IllegalArgumentException(errors.joinToString("\n"))
            } else {
                println("⚠️ Validation warnings:\n" + errors.joinToString("\n"))
            }
        }
    }
}
```

Then you can make `BookSetter` accept a `strict` flag:

```kotlin
class BookSetter(private val book: BookRecord, val strict: Boolean = true) {
    infix fun set(updates: BookUpdater.() -> Unit) {
        val updater = BookUpdater(book, strict)
        updater.apply(updates)
        updater.validate()
        DBOps().updateBook(book)
    }
}
```

Or go full custom:

```kotlin
book.with {
    publisherID "123"
}.set(strict = false) {
    title ""
    isbn "123"
}
```

---

### Would you like:
- Validation on filters too (e.g., fail if no book matches)?
- A `.validateOnly` mode to test changes before applying them?
- Hooking into Kotlin's `Result` or sealed classes for functional-style error handling?

Let me know how far you want to take this!

---------------
I notice in the BookQuery it only supports getBookWithPublisherID.  This is not what I intended.  The getBookWithPublisherID takes a string (with the id value) and looks up the book in the database.  I might also want to use something like

book with { title "Old Title" } set ...
-----
Ahh, got it now — thanks for the clarification!

So you're aiming for a DSL where the `with { ... }` block acts like a **filter builder**, allowing different criteria (like `publisherID`, `title`, etc.), and then it queries the database *after* the filters are built.

Something like this:

```kotlin
book with { 
    publisherID "PUB-001" 
    title "Old Title" 
} set {
    title "New Title"
}
```

This means:
- You don’t want a single method like `getBookWithPublisherID(id: String)`,
- Instead, you want a **dynamic query builder** that combines filters and queries accordingly.

Let’s rework the DSL to support that.

---

### ✅ Step 1: Make `BookFilter` Collect Filters

```kotlin
class BookFilter {
    var publisherID: String? = null
    var title: String? = null
    var isbn: String? = null

    infix fun publisherID(value: String) { publisherID = value }
    infix fun title(value: String) { title = value }
    infix fun isbn(value: String) { isbn = value }

    // You can keep adding more fields as needed
}
```

---

### ✅ Step 2: Query Dynamically Based on Collected Filters

Update `BookQuery` to collect filters and run the appropriate query in `DBOps`.

```kotlin
class BookQuery {
    infix fun with(criteria: BookFilter.() -> Unit): BookSetter {
        val filter = BookFilter().apply(criteria)

        val db = DBOps()
        val book: BookRecord? = db.findBookByFilter(filter)

        return if (book != null) {
            BookSetter(book)
        } else {
            throw IllegalArgumentException("No book found with the given criteria: $filter")
        }
    }
}
```

---

### ✅ Step 3: Implement `findBookByFilter()` in `DBOps`

```kotlin
class DBOps {
    fun findBookByFilter(filter: BookFilter): BookRecord? {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<String>()

        filter.publisherID?.let {
            conditions += "publisher_id = ?"
            params += it
        }
        filter.title?.let {
            conditions += "title = ?"
            params += it
        }
        filter.isbn?.let {
            conditions += "isbn = ?"
            params += it
        }

        if (conditions.isEmpty()) return null

        val sql = "SELECT * FROM books WHERE " + conditions.joinToString(" AND ")
        val stmt = conn.prepareStatement(sql)

        params.forEachIndexed { i, param ->
            stmt.setString(i + 1, param)
        }

        val rs = stmt.executeQuery()
        return if (rs.next()) BookRecord(rs) else null
    }
}
```

You’ll need to adapt that SQL part to match how you're handling DB connections and `BookRecord` construction, but the idea is sound.

---

### ✅ DSL Now Supports:

```kotlin
book with {
    title "The Hobbit"
    publisherID "PUB-001"
} set {
    title "The Hobbit: Revised Edition"
}
```

---

Would you like to:
- Support multiple matching results and handle those?
- Add fuzzy matches (like title contains)?
- Support filtering by date ranges or booleans (like `isRead true`)?

You're really close to having a slick, extensible DSL — happy to help extend it!
----
This would work but it is a bit more involved than what I was after.  I really only want to get a book by the most convenient field (single field).  I think I could actually embed that in the BookFilter functions.

----------------------
Ahh, got it now — thanks for the clarification!

So you're aiming for a DSL where the `with { ... }` block acts like a **filter builder**, allowing different criteria (like `publisherID`, `title`, etc.), and then it queries the database *after* the filters are built.

Something like this:

```kotlin
book with { 
    publisherID "PUB-001" 
    title "Old Title" 
} set {
    title "New Title"
}
```

This means:
- You don’t want a single method like `getBookWithPublisherID(id: String)`,
- Instead, you want a **dynamic query builder** that combines filters and queries accordingly.

Let’s rework the DSL to support that.

---

### ✅ Step 1: Make `BookFilter` Collect Filters

```kotlin
class BookFilter {
    var publisherID: String? = null
    var title: String? = null
    var isbn: String? = null

    infix fun publisherID(value: String) { publisherID = value }
    infix fun title(value: String) { title = value }
    infix fun isbn(value: String) { isbn = value }

    // You can keep adding more fields as needed
}
```

---

### ✅ Step 2: Query Dynamically Based on Collected Filters

Update `BookQuery` to collect filters and run the appropriate query in `DBOps`.

```kotlin
class BookQuery {
    infix fun with(criteria: BookFilter.() -> Unit): BookSetter {
        val filter = BookFilter().apply(criteria)

        val db = DBOps()
        val book: BookRecord? = db.findBookByFilter(filter)

        return if (book != null) {
            BookSetter(book)
        } else {
            throw IllegalArgumentException("No book found with the given criteria: $filter")
        }
    }
}
```

---

### ✅ Step 3: Implement `findBookByFilter()` in `DBOps`

```kotlin
class DBOps {
    fun findBookByFilter(filter: BookFilter): BookRecord? {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<String>()

        filter.publisherID?.let {
            conditions += "publisher_id = ?"
            params += it
        }
        filter.title?.let {
            conditions += "title = ?"
            params += it
        }
        filter.isbn?.let {
            conditions += "isbn = ?"
            params += it
        }

        if (conditions.isEmpty()) return null

        val sql = "SELECT * FROM books WHERE " + conditions.joinToString(" AND ")
        val stmt = conn.prepareStatement(sql)

        params.forEachIndexed { i, param ->
            stmt.setString(i + 1, param)
        }

        val rs = stmt.executeQuery()
        return if (rs.next()) BookRecord(rs) else null
    }
}
```

You’ll need to adapt that SQL part to match how you're handling DB connections and `BookRecord` construction, but the idea is sound.

---

### ✅ DSL Now Supports:

```kotlin
book with {
    title "The Hobbit"
    publisherID "PUB-001"
} set {
    title "The Hobbit: Revised Edition"
}
```

---

Would you like to:
- Support multiple matching results and handle those?
- Add fuzzy matches (like title contains)?
- Support filtering by date ranges or booleans (like `isRead true`)?

You're really close to having a slick, extensible DSL — happy to help extend it!
----------
This would work but it is a bit more involved than what I was after.  I really only want to get a book by the most convenient field (single field).  I think I could actually embed that in the BookFilter functions.
----
Ah, got it — you're going for **simplicity** and convenience, not a full-on dynamic query builder. That makes a lot of sense, especially for scripting-style usage where **you just want to grab a book by one field** — like `title`, `publisherID`, or `isbn` — without needing a whole complex system.

In that case, yes — you can totally embed the DB lookup **directly into the `BookFilter` functions themselves**. Here's how that would look.

---

### ✅ Refactor: Filter-as-Lookup

Let’s make `BookFilter` own the lookup logic, and `BookQuery` just delegate to it.

---

### 🛠 Updated `BookFilter`

```kotlin
class BookFilter {
    var result: BookRecord? = null
    private val db = DBOps()

    infix fun publisherID(value: String) {
        result = db.getBookWithPublisherID(value)
    }

    infix fun title(value: String) {
        result = db.getBookWithTitle(value)
    }

    infix fun isbn(value: String) {
        result = db.getBookWithISBN(value)
    }
}
```

This makes each function perform an immediate lookup and stash the result into `result`.

---

### 🛠 Simplified `BookQuery`

```kotlin
class BookQuery {
    infix fun with(criteria: BookFilter.() -> Unit): BookSetter {
        val filter = BookFilter().apply(criteria)
        val book = filter.result
            ?: throw IllegalArgumentException("No book found with given criteria.")

        return BookSetter(book)
    }
}
```

---

### ✅ Your DSL Now Works Like This:

```kotlin
book with {
    title "Old Title"
} set {
    title "New Title"
}
```

Or:

```kotlin
book with {
    publisherID "PUB-123"
} set {
    isFavorite true
}
```

And it only ever uses **one filter at a time**, which is exactly what you want.

---

### 📦 Minimal `DBOps` Support

You just need to define one method per filter type:

```kotlin
class DBOps {
    fun getBookWithPublisherID(id: String): BookRecord? { /*...*/ }
    fun getBookWithTitle(title: String): BookRecord? { /*...*/ }
    fun getBookWithISBN(isbn: String): BookRecord? { /*...*/ }
}
```

---

Would you like to enforce that only **one filter function is used at a time** (and throw if multiple are set)? Or maybe allow a fallback strategy like "if `title` fails, try `isbn`"?
-----------
Yes
----------

Great — we’ll add logic so your `BookFilter` enforces that **only one filter function is used per lookup**. If the user tries to use more than one (like `title` *and* `publisherID`), we throw a clear exception.

---

### ✅ Updated `BookFilter` with Single-Field Enforcement

```kotlin
class BookFilter {
    private val db = DBOps()
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

    infix fun isbn(value: String) {
        ensureOnlyOne("isbn")
        result = db.getBookWithISBN(value)
    }
}
```

---

### 🧪 Now this will work:

```kotlin
book with {
    title "The Hobbit"
} set {
    isRead true
}
```

But this will fail (as intended):

```kotlin
book with {
    title "The Hobbit"
    publisherID "PUB-001"
}
```

It throws:

```
IllegalStateException: Only one filter may be used at a time. Already filtered by 'title', cannot also use 'publisherID'.
```

Clean, safe, and very intentional.

---

Let me know if you want:
- A shorthand like `book.byTitle("The Hobbit") set { ... }`
- A fallback mechanism like "try title, then isbn"
- A warning instead of an exception (or logger integration)

You’re building a nice little fluent DSL here!

---------

When I try to compile a unit test (junit) using this approach I get a syntax error on the line:

book with { publisherID "AB-1004" } set { title "Some Title" }

It seems the compiler wants to make me use a function invocation with parentheses.  Why is that?

-----------------

Ah — **great catch**. The issue is most likely due to how Kotlin parses **non-parenthesized function calls**, particularly when you're using **infix functions inside lambdas**.

---

### 🔍 The Core Problem

Kotlin supports this:

```kotlin
publisherID "AB-1004"
```

…but only in certain contexts.

When used **inside a lambda**, Kotlin requires **the called function to be marked `infix`**, and **the receiver must be clear**. Sometimes the compiler gets confused when:
- You mix infix and regular functions.
- The lambda body is ambiguous or has multiple infix-style calls.
- The DSL structure is a bit too clever for the parser to disambiguate.

---

### ✅ Checklist for Fixing It

#### ✅ 1. Are Your Filter Methods Marked `infix`?

```kotlin
infix fun publisherID(value: String) { ... }
```

Make sure each of your `BookFilter` methods is marked with the `infix` modifier.

#### ✅ 2. Are You Calling Them Inside a Lambda with a Receiver?

This is okay:

```kotlin
book with {
    publisherID("AB-1004") // ✅ Always works
}
```

This is **allowed only if** `publisherID` is `infix` **and** there is **no ambiguity**:

```kotlin
book with {
    publisherID "AB-1004" // ✅ Maybe — see note below
}
```

---

### 🚨 Why the Syntax Error Might Appear

Even with `infix` functions, Kotlin may not allow **non-parenthesized calls** in all contexts inside a lambda — especially if:
- You're running a version of Kotlin where parser edge cases are stricter,
- There's any ambiguity (e.g., multiple infix calls in one block),
- The receiver type isn't 100% clear.

So although this looks nice:

```kotlin
book with { publisherID "AB-1004" }
```

You may need to **fall back to this more explicit form** in tests or real code:

```kotlin
book with { publisherID("AB-1004") }
```

It's still readable, and it’s 100% safe.

---

### 🧪 Suggestion for Your Unit Test

Change this:

```kotlin
book with { publisherID "AB-1004" } set { title "Some Title" }
```

To:

```kotlin
book with { publisherID("AB-1004") } set { title("Some Title") }
```

Or, for a fully fluent syntax, consider this alternative DSL flavor:

```kotlin
book.byPublisherID("AB-1004").set {
    title("Some Title")
}
```

That works cleanly in every context and avoids parser quirks.

---

Would you like to keep both forms — a strict fluent API *and* a looser DSL — or consolidate to one? I can help make the API clean and consistent either way.
---------------------------

I will keep it the way it is for now.  However, my junit tests are failing.  I have a basic API test that works fine that looks like this:


    @Test
    fun `can retrieve a book by publisher id`() {
        setupBooks()

        val retrieved = db.getBookWithPublisherID("AB-1004")
        Assertions.assertNotNull(retrieved)
        Assertions.assertEquals(retrieved.title, "Test Book4")
        Assertions.assertEquals(retrieved.publisherId, "AB-1004")
        Assertions.assertEquals(retrieved.author, "Author Y")
    }

But the DSL test is failing.  It looks like this:


    @Test
    fun `can change book title by pubID`() {
        setupBooks()

        book with { publisherID("AB-1004") } set { title("Some Title") }

        val result = db.getBookWithPublisherID("AB-1004")
        Assertions.assertNotNull(result)
        Assertions.assertEquals("Some Title", result.title)
    }

It is failing on the record retrieval, not the setting of the new title.  It is not retrieving any records.  Why might that be?

------------

Thanks for the clear details — this smells like a classic **shared state** or **database reference mismatch** issue.

Your `DSL test` fails to find the record *after* the update, even though:
- `setupBooks()` populates the DB
- `db.getBookWithPublisherID("AB-1004")` works in the regular API test
- But fails right after the DSL call in the second test

Let’s walk through some **likely causes**:

---

### ✅ 1. **Is the DSL using the same `db` instance as your test?**

This is the **most likely culprit**. If your DSL is internally creating its own instance of `DBOps` like this:

```kotlin
class BookFilter {
    private val db = DBOps() // ← a different instance
    ...
}
```

Then the DSL code is probably querying or updating a **separate database**, like:
- A different in-memory SQLite database (each instance has its own)
- A new connection with an empty state

➡️ **Fix**: Make sure both your test and the DSL use the *same* `DBOps` instance.

You could pass it in like this:

```kotlin
object book {
    infix fun with(criteria: BookFilter.() -> Unit): BookSetter {
        return BookQuery(db).with(criteria)
    }
}
```

Or inject it into the `BookFilter`:

```kotlin
class BookFilter(private val db: DBOps) {
    ...
}
```

And update your test:

```kotlin
book(db) with { publisherID("AB-1004") } set { title("Some Title") }
```

---

### ✅ 2. **Is your DB in-memory and getting wiped between accesses?**

If you're using an in-memory SQLite DB (`:memory:`), each new connection gets a **fresh DB**.

So if the DSL creates a new `DBOps`, you’re not just creating a new instance — you’re accessing a **completely different database**.

➡️ **Fix**: Share the same `Connection` object across your `DBOps` instance and your DSL's `DBOps`.

---

### ✅ 3. **Does the DSL call actually update the DB?**

It’s also possible that the DSL runs successfully but doesn’t commit the change.

If your `set` block modifies a `BookRecord` in memory but doesn't push the change back to the DB, then the final `db.getBookWithPublisherID(...)` won’t see it.

➡️ **Fix**: Make sure the `BookSetter` updates the DB. For example:

```kotlin
class BookSetter(private val book: BookRecord) {
    infix fun set(mods: BookRecord.() -> Unit) {
        book.apply(mods)
        DBOps().updateBook(book) // Ensure update persists to DB
    }
}
```

Again — be careful with multiple instances of `DBOps`.

---

### ✅ Suggested Fix Strategy

**Step 1:** Modify your DSL to accept a `DBOps` instance (injected from your test).

```kotlin
class BookQuery(private val db: DBOps) {
    infix fun with(criteria: BookFilter.() -> Unit): BookSetter {
        val filter = BookFilter(db).apply(criteria)
        val book = filter.result ?: throw IllegalArgumentException("Not found")
        return BookSetter(book, db)
    }
}
```

**Step 2:** Pass the `db` instance from the test into the DSL:

```kotlin
val db = DBOps(...) // Setup test DB

book(db) with { publisherID("AB-1004") } set { title("Some Title") }
```

**Step 3:** Ensure your setter commits the update:

```kotlin
class BookSetter(private val book: BookRecord, private val db: DBOps) {
    infix fun set(mods: BookRecord.() -> Unit) {
        book.apply(mods)
        db.updateBook(book)
    }
}
```

---

Let me know what your current `DBOps` class looks like and whether you're using an in-memory DB. I can tailor the fix more precisely!

-------------
It looks like it is the first issue.  I have created a DB instance in my test but I am using a separate one in the DSL.

My database class takes a nullable Properties object to specify the DB to open.  In my test I use an H2 DB for in-memory emulation of a MySQL database.  But the default (if the Properties object is null) is to connect to a known MySQL database.

In my API test, I create a DB instance using the H2 DB and use that for all my testing.  I tried to do the same thing in my DSL test, but forgot that I don't pass a DB into the DSL code.

I suppose I could change the line:

// Entry point
val book = BookQuery()

that you provided to take a nullable Properties and pass that to the BookFilter object to create the DB.  But would that change the `book` val to a function?

----------

Yes — you're absolutely right in how you're approaching it, and you’ve hit on a key point in Kotlin DSL design: if you want to **parameterize your DSL entry point**, like passing in a `Properties` object to configure your DB connection, then `book` will need to be a **function**, not just a `val`.

---

### 🔄 From `val book` to `fun book(props: Properties?): BookQuery`

Your current code likely has something like:

```kotlin
val book = BookQuery()
```

Which gives you this DSL:

```kotlin
book with { publisherID("AB-1004") } set { ... }
```

To allow passing in the `Properties`, you’ll change that `val` to a **function**:

```kotlin
fun book(props: Properties? = null): BookQuery {
    return BookQuery(DBOps(props))
}
```

Now you can write:

```kotlin
book(props) with { publisherID("AB-1004") } set { ... }
```

This is the cleanest way to let your **tests (or main code)** configure the DB, while your DSL stays flexible.

---

### 🧼 Optional: Make the Default Still Available

If you like the original `val book` style for convenience elsewhere, you can keep both:

```kotlin
val book: BookQuery
    get() = BookQuery(DBOps(null)) // default behavior

fun book(props: Properties?): BookQuery {
    return BookQuery(DBOps(props))
}
```

Now you get:
- `book with { ... } set { ... }` → uses default DB
- `book(props) with { ... } set { ... }` → uses test DB

---

### ✅ Summary

Yes, switching from `val book` to `fun book(...)` is the right move **if you want to inject configuration** (like test vs real DB) at runtime.

It's a small change with a big upside: clean unit testing, flexible configuration, and the same DSL design.

Let me know if you'd like a full working snippet that supports both `val book` and `fun book(props)`!
