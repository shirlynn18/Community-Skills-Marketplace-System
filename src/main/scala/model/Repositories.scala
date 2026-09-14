package model

import scalikejdbc._

/**
 * Type parameter [T] represents the entity type (e.g., Member, SkillOffer, etc.) handled by the repository.
 */
// represents a generic blueprint for all repositories.
// It defines the common CRUD operations (add, update, delete, and retrieve.)
trait Repository[T]:
  // Insert a new record into the database and return the saved item (usually containing its newly assigned database ID).
  def add(item: T): T
  // Update an existing record in the database matching the item's ID.
  def update(item: T): Unit
  // Delete a record from the database matching the provided unique integer ID.
  def delete(id: Int): Unit
  // Retrieve all records for this entity type from the database table as a Sequence list.
  def getAll(): Seq[T]
end Repository


// represents the repository responsible for managing Member data in the database.
// It handles creating, updating, deleting, and retrieving member records.
class MemberRepository extends Repository[Member]:
  // This class uses DerbyDB and ScalikeJDBC SQL statements to persist member profiles.
  /**
   * ADD/CREATE: Inserts a new Member record.
   */
  override def add(item: Member): Member =
    // DB.localTx creates a database transaction block. If an error occurs, it rolls back the changes.
    DB.localTx: session =>
      // Set the session implicitly so ScalikeJDBC statements can use it.
      implicit val s: DBSession = session
      // sql"""...""" is SQL interpolation where variables (like item.name) are safely parameterized to prevent SQL injection.
      val generatedId = sql"""
        insert into members (name, contact, area, credit_balance, registration_date)
        values (${item.name}, ${item.contact}, ${item.area}, ${item.creditBalance}, ${item.registrationDate})
      """.updateAndReturnGeneratedKey.apply() // Runs the insert query and returns the auto-generated primary key ID.
      
      // Return a copy of the member object including the newly generated ID.
      item.copy(id = Some(generatedId.toInt))

  /**
   * UPDATE: Updates columns of an existing Member record matching its ID.
   */
  override def update(item: Member): Unit =
    // Retrieve the integer ID. If not found, default to 0.
    val idVal = item.id.getOrElse(0)
    // Run inside a local transaction.
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"""
        update members set
          name = ${item.name},
          contact = ${item.contact},
          area = ${item.area},
          credit_balance = ${item.creditBalance},
          registration_date = ${item.registrationDate}
        where id = $idVal
      """.update.apply() // Executes the SQL update statement.

  /**
   * DELETE: Permanently deletes a member profile record matching the ID.
   */
  override def delete(id: Int): Unit =
    DB.localTx: session =>
      implicit val s: DBSession = session
      // Executes a delete statement matching the primary key ID.
      sql"delete from members where id = $id".update.apply()

  /**
   * GET ALL / READ: Retrieves all member records from the database table.
   */
  override def getAll(): Seq[Member] =
    // DB.readOnly runs a read-only transaction, optimizing performance for select queries.
    DB.readOnly: session =>
      implicit val s: DBSession = session
      sql"select id, name, contact, area, credit_balance, registration_date from members"
        .map: rs =>
          // For each row in the result set (rs), map the columns back into a Member case class instance.
          Member(
            id = Some(rs.int("id")),
            name = rs.string("name"),
            contact = rs.string("contact"),
            area = rs.string("area"),
            creditBalance = rs.int("credit_balance"),
            registrationDate = rs.string("registration_date")
          )
        .list.apply() // Converts the mapped results into a Scala List Sequence.
end MemberRepository


//represents the repository responsible for managing skill offer records in the database.
class SkillOfferRepository extends Repository[SkillOffer]:
  // This class uses DerbyDB and ScalikeJDBC SQL statements to persist skill offers.
  /**
   * ADD/CREATE: Inserts a new Skill Offer. Supports both pre-defined IDs (for seeding) and auto-incremented IDs.
   */
  override def add(item: SkillOffer): SkillOffer =
    DB.localTx: session =>
      implicit val s: DBSession = session
      // Check if an ID has already been assigned (used primarily during database seeding)
      item.id.filter(_ > 0) match
        case Some(preId) =>
          // Insert with the explicit, pre-defined ID.
          sql"""
            insert into skill_offers (id, member_id, skill_name, description, status, credit_rate, post_date)
            values ($preId, ${item.memberId}, ${item.skillName}, ${item.description}, ${item.status.toString}, ${item.creditRate}, ${item.postDate})
          """.update.apply()
          item
        case None =>
          // Insert letting the database auto-generate the ID.
          val generatedId = sql"""
            insert into skill_offers (member_id, skill_name, description, status, credit_rate, post_date)
            values (${item.memberId}, ${item.skillName}, ${item.description}, ${item.status.toString}, ${item.creditRate}, ${item.postDate})
          """.updateAndReturnGeneratedKey.apply()
          item.copy(id = Some(generatedId.toInt))

  /**
   * UPDATE: Updates an existing skill offer post.
   */
  override def update(item: SkillOffer): Unit =
    val idVal = item.id.getOrElse(0)
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"""
        update skill_offers set
          member_id = ${item.memberId},
          skill_name = ${item.skillName},
          description = ${item.description},
          status = ${item.status.toString},
          credit_rate = ${item.creditRate},
          post_date = ${item.postDate}
        where id = $idVal
      """.update.apply()

  /**
   * DELETE: Deletes a skill offer record matching the ID.
   */
  override def delete(id: Int): Unit =
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"delete from skill_offers where id = $id".update.apply()

  /**
   * GET ALL / READ: Retrieves all skill offers from the table.
   */
  override def getAll(): Seq[SkillOffer] =
    DB.readOnly: session =>
      implicit val s: DBSession = session
      sql"select id, member_id, skill_name, description, status, credit_rate, post_date from skill_offers"
        .map: rs =>
          // Map database rows to SkillOffer case class instances.
          SkillOffer(
            id = Some(rs.int("id")),
            memberId = rs.int("member_id"),
            skillName = rs.string("skill_name"),
            description = rs.string("description"),
            // Convert status string from DB to PostStatus Enum.
            status = PostStatus.valueOf(rs.string("status")),
            creditRate = rs.double("credit_rate"),
            postDate = rs.string("post_date")
          )
        .list.apply()
end SkillOfferRepository

//represents the repository responsible for managing skill request records in the database.
class SkillRequestRepository extends Repository[SkillRequest]:
  // This class uses DerbyDB and ScalikeJDBC SQL statements to persist skill requests.

  /**
   * ADD/CREATE: Inserts a new Skill Request. Supports both pre-defined IDs (for seeding) and auto-incremented IDs.
   */
  override def add(item: SkillRequest): SkillRequest =
    DB.localTx: session =>
      implicit val s: DBSession = session
      // Check if an ID has already been assigned (used primarily during database seeding)
      item.id.filter(_ > 0) match
        case Some(preId) =>
          // Insert with the explicit, pre-defined ID.
          sql"""
            insert into skill_requests (id, member_id, skill_name, description, status, needed_hours, post_date)
            values ($preId, ${item.memberId}, ${item.skillName}, ${item.description}, ${item.status.toString}, ${item.neededHours}, ${item.postDate})
          """.update.apply()
          item
        case None =>
          // Insert letting the database auto-generate the ID.
          val generatedId = sql"""
            insert into skill_requests (member_id, skill_name, description, status, needed_hours, post_date)
            values (${item.memberId}, ${item.skillName}, ${item.description}, ${item.status.toString}, ${item.neededHours}, ${item.postDate})
          """.updateAndReturnGeneratedKey.apply()
          item.copy(id = Some(generatedId.toInt))

  /**
   * UPDATE: Updates columns of an existing skill request post.
   */
  override def update(item: SkillRequest): Unit =
    val idVal = item.id.getOrElse(0)
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"""
        update skill_requests set
          member_id = ${item.memberId},
          skill_name = ${item.skillName},
          description = ${item.description},
          status = ${item.status.toString},
          needed_hours = ${item.neededHours},
          post_date = ${item.postDate}
        where id = $idVal
      """.update.apply()

  /**
   * DELETE: Deletes a skill request record matching the ID.
   */
  override def delete(id: Int): Unit =
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"delete from skill_requests where id = $id".update.apply()

  /**
   * GET ALL / READ: Retrieves all skill requests from the table.
   */
  override def getAll(): Seq[SkillRequest] =
    DB.readOnly: session =>
      implicit val s: DBSession = session
      sql"select id, member_id, skill_name, description, status, needed_hours, post_date from skill_requests"
        .map: rs =>
          // Map database rows to SkillRequest case class instances.
          SkillRequest(
            id = Some(rs.int("id")),
            memberId = rs.int("member_id"),
            skillName = rs.string("skill_name"),
            description = rs.string("description"),
            // Convert status string from DB to PostStatus Enum.
            status = PostStatus.valueOf(rs.string("status")),
            neededHours = rs.double("needed_hours"),
            postDate = rs.string("post_date")
          )
        .list.apply()
end SkillRequestRepository

// represents the repository responsible for managing service exchange records in the database.
class ServiceExchangeRepository extends Repository[ServiceExchange]:
  // This class uses DerbyDB and ScalikeJDBC SQL statements to persist service exchanges.

  /**
   * ADD/CREATE: Inserts a new Service Exchange match and returns the saved item with its generated ID.
   */
  override def add(item: ServiceExchange): ServiceExchange =
    DB.localTx: session =>
      implicit val s: DBSession = session
      val generatedId = sql"""
        insert into service_exchanges (offer_id, request_id, provider_member_id, receiver_member_id, credits, date, status)
        values (${item.offerId}, ${item.requestId}, ${item.providerMemberId}, ${item.receiverMemberId}, ${item.credits}, ${item.date}, ${item.status.toString})
      """.updateAndReturnGeneratedKey.apply()
      item.copy(id = Some(generatedId.toInt))

  /**
   * UPDATE: Updates the details/status of an active service exchange.
   */
  override def update(item: ServiceExchange): Unit =
    val idVal = item.id.getOrElse(0)
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"""
        update service_exchanges set
          offer_id = ${item.offerId},
          request_id = ${item.requestId},
          provider_member_id = ${item.providerMemberId},
          receiver_member_id = ${item.receiverMemberId},
          credits = ${item.credits},
          date = ${item.date},
          status = ${item.status.toString}
        where id = $idVal
      """.update.apply()

  /**
   * DELETE: Deletes a service exchange record.
   */
  override def delete(id: Int): Unit =
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"delete from service_exchanges where id = $id".update.apply()

  /**
   * GET ALL / READ: Retrieves all logged service exchanges.
   */
  override def getAll(): Seq[ServiceExchange] =
    DB.readOnly: session =>
      implicit val s: DBSession = session
      sql"select id, offer_id, request_id, provider_member_id, receiver_member_id, credits, date, status from service_exchanges"
        .map: rs =>
          // Map database rows to ServiceExchange case class instances.
          ServiceExchange(
            id = Some(rs.int("id")),
            offerId = rs.int("offer_id"),
            requestId = rs.int("request_id"),
            providerMemberId = rs.int("provider_member_id"),
            receiverMemberId = rs.int("receiver_member_id"),
            credits = rs.int("credits"),
            date = rs.string("date"),
            // Convert status string from DB to ExchangeStatus Enum.
            status = ExchangeStatus.valueOf(rs.string("status"))
          )
        .list.apply()
end ServiceExchangeRepository

// represents the repository responsible for recording and retrieving all credit transaction history.
class CreditTransactionRepository extends Repository[CreditTransaction]:
  /**
   * ADD/CREATE: Inserts a new credit transaction audit record (Earn / Spend events).
   */
  override def add(item: CreditTransaction): CreditTransaction =
    DB.localTx: session =>
      implicit val s: DBSession = session
      val generatedId = sql"""
        insert into credit_transactions (member_id, amount, transaction_type, note, date)
        values (${item.memberId}, ${item.amount}, ${item.transactionType.toString}, ${item.note}, ${item.date})
      """.updateAndReturnGeneratedKey.apply()
      item.copy(id = Some(generatedId.toInt))

  /**
   * UPDATE: Updates an audit record in the database.
   */
  override def update(item: CreditTransaction): Unit =
    val idVal = item.id.getOrElse(0)
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"""
        update credit_transactions set
          member_id = ${item.memberId},
          amount = ${item.amount},
          transaction_type = ${item.transactionType.toString},
          note = ${item.note},
          date = ${item.date}
        where id = $idVal
      """.update.apply()

  /**
   * DELETE: Deletes an audit record.
   */
  override def delete(id: Int): Unit =
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"delete from credit_transactions where id = $id".update.apply()

  /**
   * GET ALL / READ: Retrieves all logged credit transactions.
   */
  override def getAll(): Seq[CreditTransaction] =
    DB.readOnly: session =>
      implicit val s: DBSession = session
      sql"select id, member_id, amount, transaction_type, note, date from credit_transactions"
        .map: rs =>
          // Map database rows to CreditTransaction case class instances.
          CreditTransaction(
            id = Some(rs.int("id")),
            memberId = rs.int("member_id"),
            amount = rs.int("amount"),
            // Convert type string from DB back to TransactionType Enum.
            transactionType = TransactionType.valueOf(rs.string("transaction_type")),
            note = rs.string("note"),
            date = rs.string("date")
          )
        .list.apply()
end CreditTransactionRepository

// represents the repository responsible for storing and retrieving activity logs displayed on the dashboard.
class ActivityLogRepository extends Repository[ActivityLog]:

  /**
   * ADD/CREATE: Inserts a new system log event (e.g. "New Member: Alice Smith").
   */
  override def add(item: ActivityLog): ActivityLog =
    DB.localTx: session =>
      implicit val s: DBSession = session
      val generatedId = sql"""
        insert into activity_log (activity_type, description, date_str)
        values (${item.activityType}, ${item.description}, ${item.dateStr})
      """.updateAndReturnGeneratedKey.apply()
      item.copy(id = Some(generatedId.toInt))

  /**
   * UPDATE: Updates a log record.
   */
  override def update(item: ActivityLog): Unit =
    val idVal = item.id.getOrElse(0)
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"""
        update activity_log set
          activity_type = ${item.activityType},
          description = ${item.description},
          date_str = ${item.dateStr}
        where id = $idVal
      """.update.apply()

  /**
   * DELETE: Deletes a log record.
   */
  override def delete(id: Int): Unit =
    DB.localTx: session =>
      implicit val s: DBSession = session
      sql"delete from activity_log where id = $id".update.apply()

  /**
   * GET ALL / READ: Retrieves all recorded system log activities.
   */
  override def getAll(): Seq[ActivityLog] =
    DB.readOnly: session =>
      implicit val s: DBSession = session
      sql"select id, activity_type, description, date_str from activity_log"
        .map: rs =>
          // Map database rows to ActivityLog case class instances.
          ActivityLog(
            id = Some(rs.int("id")),
            activityType = rs.string("activity_type"),
            description = rs.string("description"),
            dateStr = rs.string("date_str")
          )
        .list.apply()
end ActivityLogRepository
