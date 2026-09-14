package model

import scalikejdbc._
import scala.util.Try

/**
 * Utility database manager responsible for setup, table structure creations, 
 * identity sequence updates, default data seeding, and embedded Derby shutdown connections.
 */
object DatabaseInitializer:

  /**
   * Initializes the database connection pool, drops tables to sync schema updates,
   * creates all tables, seeds them with starting data, and resets identity sequence counters.
   */
  def initialize(): Unit =
    // 1. Initialize Derby Connection Pool: Register the local embedded database driver.
    Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
    // Establish a single connection pool linking to marketplacedb database file and create it if missing.
    ConnectionPool.singleton("jdbc:derby:marketplacedb;create=true", "", "")

    // Create an implicit database session mapping to AutoSession.
    implicit val session: DBSession = AutoSession

    // 2. Outdated Schema Check & Clean: Check if the old SKILL_OFFERS table has AVAILABLE_HOURS column.
    // defines an immutable variable named oldOffersSchema and starts a safety block
    // (called Try) to execute database checks safely without crashing the application if the tables do not exist yet.
    val oldOffersSchema = Try {
      // borrows an active database connection link from the established pool to
      // talk to the SQL database.
      val conn = ConnectionPool.borrow()
      try
        // retrieves the database's metadata, which is information describing the internal tables, columns, and structural setup of the database.
        val meta = conn.getMetaData
        // null - catalog, schema pattern
        // search for "AVAILABLE_HOURS" column inside "SKILL_OFFERS" table,
        // saving the results in a result set named rs
        val rs = meta.getColumns(null, null, "SKILL_OFFERS", "AVAILABLE_HOURS")
        // checks if the metadata query returned any matches,
        // setting the exists flag to true if the column was found.
        val exists = rs.next()
        rs.close() //closes the metadata result
        exists // returns the true/false result showing if the column exists to the surrounding safety block.
      finally
        conn.close() //returns the borrowed database connection back to the connection pool so other classes can use it.
    // handles any errors (like if the table hasn't been created yet),
    // returning a default value of false so the program can continue running smoothly.
    }.getOrElse(false)

    // Check if the old SKILL_REQUESTS table has CREDIT_RATE column.
    // defines an immutable variable named oldOffersSchema and starts a safety block
    // (called Try) to execute database checks safely without crashing the application if the tables do not exist yet.
    val oldRequestsSchema = Try {
      // borrows an active database connection link from the established pool to
      // talk to the SQL database.
      val conn = ConnectionPool.borrow()
      try
        // retrieves the database's metadata, which is information describing the internal tables, columns, and structural setup of the database.
        val meta = conn.getMetaData
        // null - catalog, schema pattern
        // find the table named 'SKILL_REQUESTS' with the column 'CREDIT_RATE'
        // saving the results in a result set named rs
        val rs = meta.getColumns(null, null, "SKILL_REQUESTS", "CREDIT_RATE")
        // checks if the metadata query returned any matches,
        // setting the exists flag to true if the column was found.
        val exists = rs.next()
        rs.close() //closes the metadata result
        exists // returns the true/false result showing if the column exists to the surrounding safety block.
      finally
        conn.close() //returns the borrowed database connection back to the connection pool so other classes can use it.
    // handles any errors (like if the table hasn't been created yet),
    // returning a default value of false so the program can continue running smoothly.
    }.getOrElse(false)

    // Check if the old SERVICE_EXCHANGES table has nullable columns.
    // defines an immutable variable named oldOffersSchema and starts a safety block
    // (called Try) to execute database checks safely without crashing the application if the tables do not exist yet.
    val oldExchangesSchema = Try {
      // borrows an active database connection link from the established pool to
      // talk to the SQL database.
      val conn = ConnectionPool.borrow()
      try
        // retrieves the database's metadata, which is information describing the internal tables, columns, and structural setup of the database.
        val meta = conn.getMetaData
        // null - catalog, schema pattern
        // find the table named 'SERVICE_EXCHANGES' with the column 'OFFER_ID'
        // saving the results in a result set named rs
        val rs = meta.getColumns(null, null, "SERVICE_EXCHANGES", "OFFER_ID")
        // checks if database metadata query successfully found the column
        // that looking for and moves the reader cursor to that row.
        if rs.next() then
          // reads the column property value named "IS_NULLABLE",
          // which returns "YES" if the column is allowed to be empty (null)
          // or "NO" if the column is mandatory.
          val isNullable = rs.getString("IS_NULLABLE")
          // closes the database metadata reader to free up system memory resources.
          rs.close()
          //checks if the column property is equal to "YES",
          // returning true if the column is allowed to be empty, or false if it is mandatory.
          isNullable == "YES"
        else
          rs.close() //closes the metadata reader
          false //indicate that the column is not found (meaning it cannot be confirmed as nullable).
      finally
        conn.close() ////returns the borrowed database connection back to the connection pool
    // handles any errors (like if the table hasn't been created yet),
    // returning a default value of false so the program can continue running smoothly.
    }.getOrElse(false)

    // Force drop tables to ensure schema modifications apply cleanly if outdated.
    // This condition is always true, so refresh process runs every time the application starts.
    if true || oldOffersSchema || oldRequestsSchema || oldExchangesSchema then
      // delete the activity log table from the database,
      // wrapping the action in a Try block to ignore errors if the table does not exist.
      Try:
        sql"drop table activity_log".execute.apply()
      Try:
        sql"drop table credit_transactions".execute.apply()
      Try:
        sql"drop table service_exchanges".execute.apply()
      Try:
        sql"drop table skill_offers".execute.apply()
      Try:
        sql"drop table skill_requests".execute.apply()
      Try:
        sql"drop table members".execute.apply()
      println("Refreshing database tables to match updated PostStatus schema...")

    // 3. Initialize Tables & Seed: Call helper methods to create tables and load mock data.
    // creates the members table in the database and
    // seeds it with default community member profiles.
    initializeMembers()
    initializeOffers()
    initializeRequests()
    initializeExchanges()
    initializeTransactions()
    initializeActivityLog()

    // 4. Force Reset of Identity Counters to ensure they are synchronized with seeded data.
    Try:
      // resets the auto-increment counter for the member ID column to start at 8
      sql"alter table members alter column id restart with 8".execute.apply()
    Try:
      sql"alter table skill_offers alter column id restart with 19".execute.apply()
    Try:
      sql"alter table skill_requests alter column id restart with 19".execute.apply()
    Try:
      sql"alter table service_exchanges alter column id restart with 6".execute.apply()
    Try:
      sql"alter table credit_transactions alter column id restart with 5".execute.apply()
    Try:
      sql"alter table activity_log alter column id restart with 16".execute.apply()
    println("Identity column sequence counters verified and restarted.")
  end initialize

  /**
   * Creates the members table and seeds starting community member accounts.
   */
  // defines a private helper function named initializeMembers that
  // requires an implicit database session to run queries and returns no output value.
  private def initializeMembers()(implicit session: DBSession): Unit =
    // checks if the members table is already present in the database
    // by attempting to query its row count,
    // returning true if the query runs successfully,
    // or false if the table does not exist.
    val tableExists = Try {
      // counts the total rows in the members table and retrieves the result as an integer.
      sql"select count(*) from members".map(rs => rs.int(1)).single.apply()
    }.isSuccess

    // checks if the members table was not found in the database.
    if !tableExists then
      // Create the members table with auto-increment ID, name, contact,
      // area, balance, and date.
      sql"""
        create table members (
          id int generated by default as identity (start with 1, increment by 1) primary key,
          name varchar(255) not null,
          contact varchar(100) not null,
          area varchar(100) not null,
          credit_balance int not null,
          registration_date varchar(50) not null
        )
      """.execute.apply()

      // Define default member profiles list (seeds).
      val seeds = Seq(
        (1, "Alice Smith", "+60123456789", "Taman Perdana", 210, "15-07-2026"),
        (2, "Bob Jones", "+60198765432", "Taman Mawar", 200, "16-07-2026"),
        (3, "Charlie Brown", "+60111222333", "Taman Segah", 180, "17-07-2026"),
        (4, "Diana Prince", "+60155566677", "Taman Tyng", 120, "18-07-2026"),
        (5, "Eva Green", "+60144455566", "Taman Grandview", 250, "19-07-2026"),
        (6, "Louis Char", "+60177788899", "Taman Segah", 5, "20-07-2026"),
        (7, "Max Po", "+60188899900", "Taman Perdana", 100, "20-07-2026")
      )
      // Loop through and insert each member seed into the database.
      seeds.foreach:
        case (id, name, contact, area, balance, regDate) =>
          sql"""
            insert into members (id, name, contact, area, credit_balance, registration_date)
            values ($id, $name, $contact, $area, $balance, $regDate)
          """.update.apply()
      println("Seeded members table.")
  end initializeMembers

  /**
   * Creates the skill_offers table and seeds initial offers.
   */
  private def initializeOffers()(implicit session: DBSession): Unit =
    // Check if the skill_offers table already exists.
    val tableExists = Try {
      // counts the total rows in the offer table and retrieves the result as an integer.
      sql"select count(*) from skill_offers".map(rs => rs.int(1)).single.apply()
    }.isSuccess

    if !tableExists then
      // Create the skill_offers table with auto-increment ID, publisher ID, skill title, description, status, rate, and date.
      sql"""
        create table skill_offers (
          id int generated by default as identity (start with 1, increment by 1) primary key,
          member_id int not null,
          skill_name varchar(255) not null,
          description varchar(1000),
          status varchar(50) not null,
          credit_rate double precision not null,
          post_date varchar(50) not null
        )
      """.execute.apply()

      // Define default offer posts list (seeds).
      val seeds = Seq(
        (1, 1, "Scala Tutoring", "Teaching functional programming in Scala 3", "Open", 20.0, "20-07-2026"),
        (3, 3, "Guitar Tutoring", "Beginner guitar tutorials", "Closed", 15.0, "22-07-2026"),
        (5, 4, "Childcare", "Organic babysitting and childcare assistance", "Closed", 25.0, "24-07-2026"),
        (7, 2, "Home Repairs", "Handyman repairs and fixing household items", "Matched", 20.0, "25-07-2026"),
        (9, 7, "Scala Tutoring", "Advanced Scala programming workshops", "Open", 20.0, "26-07-2026"),
        (11, 7, "Home Repairs", "Electrical repair services", "Open", 25.0, "26-07-2026"),
        (13, 7, "Childcare", "After school childcare", "Open", 15.0, "26-07-2026"),
        (15, 1, "Piano Tutoring", "Private piano lessons", "Open", 50.0, "26-07-2026")
      )
      // Loop through and insert each offer seed.
      seeds.foreach:
        case (id, memberId, skillName, desc, status, rate, postDate) =>
          sql"""
            insert into skill_offers (id, member_id, skill_name, description, status, credit_rate, post_date)
            values ($id, $memberId, $skillName, $desc, $status, $rate, $postDate)
          """.update.apply()
      println("Seeded skill_offers table.")
  end initializeOffers

  /**
   * Creates the skill_requests table and seeds initial requests.
   */
  private def initializeRequests()(implicit session: DBSession): Unit =
    // Check if the skill_requests table already exists.
    val tableExists = Try {
      //  counts the total rows in the request table and retrieves the result as an integer.
      sql"select count(*) from skill_requests".map(rs => rs.int(1)).single.apply()
    }.isSuccess

    if !tableExists then
      // Create the skill_requests table with columns for ID, publisher ID, skill title, description, status, hours, and date.
      sql"""
        create table skill_requests (
          id int generated by default as identity (start with 1, increment by 1) primary key,
          member_id int not null,
          skill_name varchar(255) not null,
          description varchar(1000),
          status varchar(50) not null,
          needed_hours double precision not null,
          post_date varchar(50) not null
        )
      """.execute.apply()

      // Define default request posts list (seeds).
      val seeds = Seq(
        (2, 2, "Scala Tutoring", "Need help understanding implicits", "Open", 4.0, "21-07-2026"),
        (4, 2, "Guitar Tutoring", "Want to learn to play classic rock", "Closed", 3.0, "22-07-2026"),
        (6, 1, "Home Repairs", "Need one bedroom painted", "Matched", 8.0, "23-07-2026"),
        (8, 1, "Childcare", "Need help with babysitting", "Closed", 2.0, "24-07-2026"),
        (10, 5, "Guitar Tutoring", "Seeking intermediate lessons", "Open", 2.0, "25-07-2026"),
        (12, 7, "Guitar Tutoring", "Learn basic acoustic guitar chords", "Open", 4.0, "26-07-2026"),
        (14, 7, "Gardening", "Lawn mowing and weeding help", "Open", 3.0, "26-07-2026"),
        (16, 6, "Piano Tutoring", "Learn to play Fur Elise", "Open", 5.0, "26-07-2026"),
        (17, 1, "Scala Tutoring", "Practice Scala functional constructs", "Open", 2.0, "27-07-2026"),
        (18, 2, "Scala Tutoring", "Understand advanced Scala collection APIs", "Open", 2.0, "27-07-2026")
      )
      // Loop through and insert each request seed.
      seeds.foreach:
        case (id, memberId, skillName, desc, status, hours, postDate) =>
          sql"""
            insert into skill_requests (id, member_id, skill_name, description, status, needed_hours, post_date)
            values ($id, $memberId, $skillName, $desc, $status, $hours, $postDate)
          """.update.apply()
      println("Seeded skill_requests table.")
  end initializeRequests

  /**
   * Creates the service_exchanges table and seeds initial matches.
   */
  private def initializeExchanges()(implicit session: DBSession): Unit =
    // Check if the service_exchanges table already exists.
    val tableExists = Try {
      //counts the total rows in the se table and retrieves the result as an integer.
      sql"select count(*) from service_exchanges".map(rs => rs.int(1)).single.apply()
    }.isSuccess

    if !tableExists then
      // Create the service_exchanges table with columns for ID, post IDs, participant IDs, credits cost, date, and status.
      sql"""
        create table service_exchanges (
          id int generated by default as identity (start with 1, increment by 1) primary key,
          offer_id int not null,
          request_id int not null,
          provider_member_id int not null,
          receiver_member_id int not null,
          credits int not null,
          date varchar(50) not null,
          status varchar(50) not null
        )
      """.execute.apply()

      // Define default booked exchanges list (seeds).
      val seeds = Seq(
        (1, 5, 8, 4, 1, 50, "25-07-2026", "Completed"),
        (2, 7, 6, 2, 1, 160, "27-07-2026", "Accepted"),
        (3, 3, 4, 3, 2, 45, "28-07-2026", "Completed"),
        (4, 9, 2, 7, 2, 80, "29-07-2026", "Pending"),
        (5, 3, 12, 3, 7, 60, "31-07-2026", "Cancelled")
      )
      // Loop through and insert each exchange seed.
      seeds.foreach:
        case (id, offerId, reqId, provId, recId, credits, date, status) =>
          sql"""
            insert into service_exchanges (id, offer_id, request_id, provider_member_id, receiver_member_id, credits, date, status)
            values ($id, $offerId, $reqId, $provId, $recId, $credits, $date, $status)
          """.update.apply()
      println("Seeded service_exchanges table.")
  end initializeExchanges

  /**
   * Creates the credit_transactions table and seeds historical audit trails.
   */
  private def initializeTransactions()(implicit session: DBSession): Unit =
    // Check if the credit_transactions table already exists.
    val tableExists = Try {
      // counts the total rows in the ct table and retrieves the result as an integer.
      sql"select count(*) from credit_transactions".map(rs => rs.int(1)).single.apply()
    }.isSuccess

    if !tableExists then
      // Create the credit_transactions table with columns for ID, member ID, amount, type, description note, and date.
      sql"""
        create table credit_transactions (
          id int generated by default as identity (start with 1, increment by 1) primary key,
          member_id int not null,
          amount int not null,
          transaction_type varchar(50) not null,
          note varchar(255) not null,
          date varchar(50) not null
        )
      """.execute.apply()

      // Define default credit transaction audit logs list (seeds).
      val seeds = Seq(
        (1, 4, 50, "Earn", "Earned credits for exchange (ID: 1)", "25-07-2026"),
        (2, 1, 50, "Spend", "Spent credits for exchange (ID: 1)", "25-07-2026"),
        (3, 3, 45, "Earn", "Earned credits for exchange (ID: 3)", "28-07-2026"),
        (4, 2, 45, "Spend", "Spent credits for exchange (ID: 3)", "28-07-2026")
      )
      // Loop through and insert each transaction seed.
      seeds.foreach:
        case (id, memberId, amount, txType, note, date) =>
          sql"""
            insert into credit_transactions (id, member_id, amount, transaction_type, note, date)
            values ($id, $memberId, $amount, $txType, $note, $date)
          """.update.apply()
      println("Seeded credit_transactions table.")
  end initializeTransactions

  /**
   * Creates the activity_log table and seeds initial logs.
   */
  private def initializeActivityLog()(implicit session: DBSession): Unit =
    // Check if the activity_log table already exists.
    val tableExists = Try {
      // counts the total rows in the al table and retrieves the result as an integer.
      sql"select count(*) from activity_log".map(rs => rs.int(1)).single.apply()
    }.isSuccess

    if !tableExists then
      // Create the activity_log table with columns for ID, activity type, description, and date.
      sql"""
        create table activity_log (
          id int generated by default as identity (start with 1, increment by 1) primary key,
          activity_type varchar(100) not null,
          description varchar(255) not null,
          date_str varchar(50) not null
        )
      """.execute.apply()

      // Define default activity logs list (seeds).
      val seeds = Seq(
        (1, "New Member", "New Member: Alice Smith", "15-07-2026"),
        (2, "New Member", "New Member: Bob Jones", "16-07-2026"),
        (3, "New Member", "New Member: Charlie Brown", "17-07-2026"),
        (4, "New Member", "New Member: Diana Prince", "18-07-2026"),
        (5, "New Member", "New Member: Eva Green", "19-07-2026"),
        (6, "New Skill Offer", "New Skill Offer: Diana Prince - Guitar Tutoring", "20-07-2026"),
        (7, "New Skill Offer", "New Skill Offer: Alice Smith - Scala Tutoring", "20-07-2026"),
        (8, "New Skill Offer", "New Skill Offer: Charlie Brown - Childcare", "21-07-2026"),
        (9, "New Skill Request", "New Skill Request: Bob Jones - Scala Tutoring", "22-07-2026"),
        (10, "New Skill Request", "New Skill Request: Alice Smith - Childcare", "22-07-2026"),
        (11, "Pending Exchange", "Pending Exchange: Alice Smith \u2194 Diana Prince", "23-07-2026"),
        (12, "Accepted Exchange", "Accepted Exchange: Alice Smith \u2194 Diana Prince", "24-07-2026"),
        (13, "Completed Exchange", "Completed Exchange: Alice Smith \u2194 Diana Prince", "25-07-2026"),
        (14, "Pending Exchange", "Pending Exchange: Charlie Brown \u2194 Alice Smith", "25-07-2026"),
        (15, "Cancelled Exchange", "Cancelled Exchange: Charlie Brown \u2194 Alice Smith", "26-07-2026")
      )
      // Loop through and insert each activity log seed.
      seeds.foreach:
        case (id, actType, desc, date) =>
          sql"""
            insert into activity_log (id, activity_type, description, date_str)
            values ($id, $actType, $desc, $date)
          """.update.apply()
      println("Seeded activity_log table.")
  end initializeActivityLog

  /**
   * Closes the ScalikeJDBC connection pool and shutdowns the embedded Derby instance.
   */
  def shutdown(): Unit =
    Try:
      // Close all database connections in the connection pool.
      ConnectionPool.closeAll()
    Try:
      // Load standard driver manager and shutdown embedded Derby safely to unlock files.
      import java.sql.DriverManager
      DriverManager.getConnection("jdbc:derby:;shutdown=true")
  end shutdown
end DatabaseInitializer
