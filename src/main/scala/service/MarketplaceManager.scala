package service

import model._
import scala.util.Try

/**
 * Service Layer coordinator enforcing all core business logic rules, input validation controls,
 * double-matching protections, self-booking prevention, time conflict schedules, and credit balances.
 */
// begins the definition of the MarketplaceManager coordinator class,
// which manages all the business logic rules of the system.
class MarketplaceManager(
  // Repository fields are private so all data access is forced through this
  // service layer, which is the only place business rules (credit checks,
  // skill-name matching, post-status derivation, etc.) are enforced.
  // private variable to hold the repository manager that handles loading and saving member profile records.
  private val memberRepo: Repository[Member],
  private val offerRepo: Repository[SkillOffer],
  private val requestRepo: Repository[SkillRequest],
  private val exchangeRepo: Repository[ServiceExchange],
  private val transactionRepo: Repository[CreditTransaction],
  private val activityLogRepo: Repository[ActivityLog] = new ActivityLogRepository()
):

  /**
   * Logs system activities dynamically into the database.
   * Takes the type of action and its description, formats today's date, and saves it.
   */
  def addActivityLog(activityType: String, description: String): Unit =
    // Format the current date as day-month-year.
    val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
    // Insert the new activity log event into the database.
    activityLogRepo.add(ActivityLog(activityType = activityType, description = description, dateStr = todayStr))

  /**
   * Fetches all activity logs from the database.
   */
  // retrieves the complete list of system activity logs as a sequence list.
  def getAllActivityLogs(): Seq[ActivityLog] =
    activityLogRepo.getAll() // calls the activity log repository manager
  // to run a select query and pull all log rows from the database.

  /**
   * Creates and logs a new Member.
   * Saves the member profile to the database and logs a registration message.
   */
  //defines a public helper function named addMember that accepts a Member object,
  // registers it in the system, and returns the registered member details.
  def addMember(member: Member): Member =
    // Insert the member record and save the returned object with its new ID.
    val m = memberRepo.add(member)
    // Create an audit log indicating a new member registered.
    addActivityLog("New Member", s"New Member: ${m.name}")
    // Return the newly created member.
    m

  /**
   * Updates an existing member profile.
   */
  // defines a public function named updateMember that receives a
  // Member profile and returns nothing.
  def updateMember(member: Member): Unit =
    memberRepo.update(member) //calls the member repository manager to
    // update the corresponding member record inside the database table.

  /**
   * Deletes a member profile by ID.
   */
  // defines a public function named deleteMember that
  // takes a member ID number and returns nothing.
  def deleteMember(id: Int): Unit =
    memberRepo.delete(id) // calls the member repository manager to
    // delete the member record matching the ID from the database table.

  /**
   * Returns all registered members.
   */
  //defines a public function named getAllMembers that
  // retrieves the list of all members and returns them as a sequence list.
  def getAllMembers(): Seq[Member] =
    memberRepo.getAll() // calls the member repository manager to query
    // and retrieve all member records from the database table.
  
  /**
   * Creates and logs a new Skill Offer.
   * Auto-assigns the next ID and logs the creation message.
   */
  // defines a public helper function named createSkillOffer that
  // accepts a SkillOffer object, registers it in the system, and
  // returns the saved offer details.
  def createSkillOffer(offer: SkillOffer): SkillOffer =
    // Get the next available ID number for skill posts.
    val nextId = getNextSkillPostIdNumeric()
    // Insert the offer with the new ID assigned.
    // creates a copy of the offer object with the new sequential ID assigned,
    // and tells the offer repository manager to save it in the database table.
    val o = offerRepo.add(offer.copy(id = Some(nextId)))
    // logs a new system activity log event on the dashboard feed
    // showing the helper's name and the skill title offered.
    addActivityLog("New Skill Offer", s"New Skill Offer: ${getMemberName(o.memberId)} - ${o.skillName}")
    // Return the saved offer.
    o
  
  /**
   * Creates and logs a new Skill Request.
   * Auto-assigns the next ID and logs the creation message.
   */
  //defines a public helper function named createSkillRequest that
  // accepts a SkillRequest object, registers it in the system, and
  // returns the saved request details.
  def createSkillRequest(request: SkillRequest): SkillRequest =
    // Get the next available ID number for skill posts.
    val nextId = getNextSkillPostIdNumeric()
    // Insert the request with the new ID assigned.
    val r = requestRepo.add(request.copy(id = Some(nextId)))
    // Log the new skill request event.
    addActivityLog("New Skill Request", s"New Skill Request: ${getMemberName(r.memberId)} - ${r.skillName}")
    // Return the saved request.
    r

  /**
   * Helper method that derives the post status dynamically based on associated active/completed exchanges.
   * This ensures database records automatically sync their statuses based on matching activity.
   */
  // defines a private helper function named determinePostStatus that
  // determines the current status of a skill post by checking its associated service exchanges.
  private def determinePostStatus(postIdOpt: Option[Int], storedStatus: PostStatus, exchanges: Seq[ServiceExchange]): PostStatus = {
    //check if the post actually has a valid ID number assigned.
    postIdOpt match
      // proceeds if the post has a valid ID, saving the ID value in a variable named postId
      case Some(postId) =>
        // Find all exchanges and filter them to keep only those that match this post ID.
        val associated = exchanges.filter(se => se.offerId == postId || se.requestId == postId)
        if associated.isEmpty then
          // If no exchanges exist, keep the original stored status.
          storedStatus
        else
          // If any associated exchange is completed, close the post.
          if associated.exists(_.status == ExchangeStatus.Completed) then
            PostStatus.Closed
          // If any associated exchange is active (pending/accepted), mark the post as matched.
          else if associated.exists(se => se.status == ExchangeStatus.Pending || se.status == ExchangeStatus.Accepted) then
            PostStatus.Matched
          else
            // Otherwise, revert back to open unless it was manually closed.
            if storedStatus == PostStatus.Closed then PostStatus.Closed else PostStatus.Open
      // fallback for posts that do not have an ID yet, returning their original status.
      case None =>
        storedStatus
  }

  /**
   * Retrieves all Offers from the database, dynamically updating their status.
   */
  // defines a public query function named getAllOffers that retrieves
  // the list of all skill offers from the database and returns them as a sequence list.
  def getAllOffers(): Seq[SkillOffer] =
    // retrieves all raw skill offers stored in the database offers table.
    val rawOffers = offerRepo.getAll()
    //retrieves all service exchange bookings recorded in the database exchanges table.
    val exchanges = exchangeRepo.getAll()
    // loops through the raw list of offers to update each offer's status
    // based on current bookings.
    rawOffers.map: offer =>
      // calls the helper function to calculate status
      // (Open, Matched, or Closed) of the current offer.
      val derived = determinePostStatus(offer.id, offer.status, exchanges)
      // creates a copy of the offer object with the newly calculated status assigned.
      offer.copy(status = derived)

  /**
   * Retrieves all Requests from the database, dynamically updating their status.
   */
  // defines a public query function named getAllRequests that retrieves
  // the list of all skill requests from the database and returns them as a sequence list.
  def getAllRequests(): Seq[SkillRequest] =
    //retrieves all raw skill requests stored in the database requests table.
    val rawRequests = requestRepo.getAll()
    //retrieves all service exchange bookings recorded in the database exchanges table.
    val exchanges = exchangeRepo.getAll()
    // loops through the raw list of requests to update each request's status based on current bookings.
    rawRequests.map: req =>
      // calls the helper function to calculate the dynamic status (Open, Matched, or Closed) of the current request.
      val derived = determinePostStatus(req.id, req.status, exchanges)
      //creates a copy of the request object with the newly calculated status assigned.
      req.copy(status = derived)

  /**
   * Updates an existing Skill Offer.
   */
  //defines a public function named updateSkillOffer that
  // receives a SkillOffer object and returns nothing.
  def updateSkillOffer(offer: SkillOffer): Unit = {
    //calls the offer repository manager to update the offer record inside the database table.
    offerRepo.update(offer)
  }

  /**
   * Deletes a Skill Offer.
   */
  //defines a public function named deleteSkillOffer that
  // takes an offer ID number and returns nothing.
  def deleteSkillOffer(id: Int): Unit = {
    //calls the offer repository manager to delete the offer record ID from the database table.
    offerRepo.delete(id)
  }

  /**
   * Updates an existing Skill Request.
   */
  // defines a public function named updateSkillRequest
  // that receives a SkillRequest object and returns nothing.
  def updateSkillRequest(request: SkillRequest): Unit =
    requestRepo.update(request) //calls the request repository manager to update the request record inside the database table.

  /**
   * Deletes a Skill Request.
   */
  //defines a public function named deleteSkillRequest that
  // takes a request ID number and returns nothing.
  def deleteSkillRequest(id: Int): Unit =
    requestRepo.delete(id) // calls the request repository manager to delete the request record ID from the database table.

  /**
   * Finds a member profile by ID.
   */
  // defines a public query function named getMemberById that
  // takes a member ID and returns the matching Member object wrapped in an Option.
  def getMemberById(memberId: Int): Option[Member] =
    memberRepo.getAll().find(_.id.contains(memberId)) //retrieves all member records
    // and searches through them to find the member whose ID matches the requested memberId.

  /**
   * Validates and matches an offer and request post together on a scheduled date.
   * Enforces:
   *  - Active post checks (must be open).
   *  - Case-insensitive skill name equality.
   *  - Double-match protection.
   *  - Self-booking prevention.
   *  - Requester credit balance sufficiency.
   *  - Overlapping time-slot conflict validation.
   */
  //defines a public matchmaking function named matchOfferRequest that
  // accepts an offer ID, a request ID, and a scheduled date string,
  // returning a Try wrapper containing the created match.
  def matchOfferRequest(offerId: Int, requestId: Int, dateStr: String): Try[ServiceExchange] =
    Try:
      // queries the database to find the skill offer post matching the provided offerId.
      val offer = offerRepo.getAll().find(_.id.contains(offerId))
        //throws a validation error if the offer post could not be found in the database.
        .getOrElse(throw new IllegalArgumentException(s"Skill Offer with ID $offerId not found."))
      // queries the database to find the skill request post matching the provided requestId.
      val request = requestRepo.getAll().find(_.id.contains(requestId))
        //throws a validation error if the request post could not be found in the database.
        .getOrElse(throw new IllegalArgumentException(s"Skill Request with ID $requestId not found."))
        
      // checks if the selected skill offer is not currently open for bookings.
      if offer.status != PostStatus.Open then
        throw new IllegalArgumentException(s"Selected Skill Offer is not Open, it is already matched or closed (current status: ${offer.status}).")
      // checks if the selected skill request is not currently open for bookings.
      if request.status != PostStatus.Open then
        throw new IllegalArgumentException(s"Selected Skill Request is not Open, it is already matched or closed (current status: ${request.status}).")

      // Ensure the skill names match (case-insensitive check).
      if !offer.skillName.equalsIgnoreCase(request.skillName) then
        throw new IllegalArgumentException(s"Skill mismatch: Cannot match '${offer.skillName}' with '${request.skillName}'. Skill names must match (case-insensitive).")

      // Double-Matching Protection: Make sure neither post is already involved in an active exchange.
      val activeExchanges = exchangeRepo.getAll().filter(se => se.status != ExchangeStatus.Cancelled)
      if activeExchanges.exists(_.offerId == offerId) then
        throw new IllegalArgumentException(s"Skill Offer with ID $offerId is already associated with an existing service exchange.")
      if activeExchanges.exists(_.requestId == requestId) then
        throw new IllegalArgumentException(s"Skill Request with ID $requestId is already associated with an existing service exchange.")

      // Self-Booking Prevention: A user cannot match with themselves.
      if offer.memberId == request.memberId then
        throw new IllegalArgumentException("Self-booking is not allowed. A user cannot book or match their own published skills.")
      
      // Get the receiver profile to check their wallet.
      val receiver = memberRepo.getAll().find(_.id.contains(request.memberId))
        .getOrElse(throw new IllegalStateException(s"Receiver member with ID ${request.memberId} not found."))
      
      // Calculate the credit cost (rate * hours) and check if receiver has enough points.
      val credits = (request.neededHours * offer.creditRate).toInt
      if receiver.creditBalance < credits then
        throw new IllegalArgumentException(s"Receiver '${receiver.name}' has insufficient credits (${receiver.creditBalance}) for this matching match (requires $credits credits).")

      // Time-Slot Conflict Detection: Ensure neither user has another exchange scheduled on the same date.
      val hasConflict = activeExchanges.exists: se =>
        se.date == dateStr &&
        (se.providerMemberId == offer.memberId || se.providerMemberId == request.memberId ||
         se.receiverMemberId == offer.memberId || se.receiverMemberId == request.memberId)
      if hasConflict then
        throw new IllegalArgumentException(s"Time-slot conflict detected: Provider or Receiver has an overlapping appointment on $dateStr.")

      // Update both posts' status to Matched.
      val updatedOffer = offer.copy(status = PostStatus.Matched)
      val updatedRequest = request.copy(status = PostStatus.Matched)
      offerRepo.update(updatedOffer)
      requestRepo.update(updatedRequest)
      
      // constructs a new ServiceExchange match record to log the booking agreement details.
      val exchange = ServiceExchange(
        //links the match record to the specific offer and request post ID numbers.
        offerId = offerId,
        requestId = requestId,
        //links the match record to the ID numbers of the helper (provider) and the receiver.
        providerMemberId = offer.memberId,
        receiverMemberId = request.memberId,
        // sets the total credit points cost,
        // the scheduled date, and marks the initial match progress status as Pending.
        credits = credits,
        date = dateStr,
        status = ExchangeStatus.Pending
      )
      // tells the exchange repository manager to insert this new match
      // record into the database table, saving the result in se.
      val se = exchangeRepo.add(exchange)
      // logs a new system activity timeline event on the dashboard
      // showing that a match is pending between the provider and receiver.
      addActivityLog("Pending Exchange", s"Pending Exchange: ${getMemberName(offer.memberId)} \u2194 ${getMemberName(request.memberId)}")
      // Return the saved exchange.
      se
  end matchOfferRequest
  
  /**
   * Accepts a pending service exchange match.
   */
  // defines a public function named acceptExchange that
  // takes an exchange match ID, updates its status to accepted, and
  // returns a Try wrapper.
  def acceptExchange(exchangeId: Int): Try[Unit] =
    Try:
      // queries the database to find the service exchange booking record matching the provided
      val exchange = exchangeRepo.getAll().find(_.id.contains(exchangeId))
        // throws a validation error if the booking record could not be found in the database.
        .getOrElse(throw new IllegalArgumentException(s"Service exchange with ID $exchangeId not found"))
      // Verify that the exchange is actually pending before accepting.
      if exchange.status != ExchangeStatus.Pending then
        throw new IllegalStateException("Only pending exchanges can be accepted.")
      // updates the booking status to Accepted and saves the updated record in the database exchanges table.
      exchangeRepo.update(exchange.copy(status = ExchangeStatus.Accepted))
      // logs a new system activity timeline event on the dashboard showing that the provider has accepted the match.
      addActivityLog("Accepted Exchange", s"Accepted Exchange: ${getMemberName(exchange.providerMemberId)} \u2194 ${getMemberName(exchange.receiverMemberId)}")
  end acceptExchange

  /**
   * Cancels a pending/accepted service exchange, reverting both posts back to Open.
   */
  //defines a public function named cancelExchange that
  // takes an exchange match ID, cancels the booking, and returns a Try wrapper.
  def cancelExchange(exchangeId: Int): Try[Unit] =
    Try:
      //queries the database to find the service exchange booking record matching the provided exchangeId.
      val exchange = exchangeRepo.getAll().find(_.id.contains(exchangeId))
        // throws a validation error if the booking record could not be found in the database.
        .getOrElse(throw new IllegalArgumentException(s"Service exchange with ID $exchangeId not found"))
      // checks if the booking is already completed or already canceled.
      if exchange.status == ExchangeStatus.Completed || exchange.status == ExchangeStatus.Cancelled then
        //completed or already cancelled matches cannot be cancelled.
        throw new IllegalStateException("Completed or already cancelled exchanges cannot be cancelled.")
      
      // searches for the associated skill offer post in the database using the match's offer ID.
      offerRepo.getAll().find(_.id.contains(exchange.offerId)).foreach: offer =>
        //resets the status of the associated skill offer post back to Open so others can match with it.
        offerRepo.update(offer.copy(status = PostStatus.Open))
      // Revert the associated request post status to Open.
      //searches for the associated skill request post in the database using the match's request ID.
      requestRepo.getAll().find(_.id.contains(exchange.requestId)).foreach: request =>
        //resets the status of the associated skill request post back to Open so others can match with it.
        requestRepo.update(request.copy(status = PostStatus.Open))

      // Update the exchange status to Cancelled, saves the updated record in the database exchanges table.
      exchangeRepo.update(exchange.copy(status = ExchangeStatus.Cancelled))
      // logs a new system activity timeline event on the dashboard showing that the match has been cancelled.
      addActivityLog("Cancelled Exchange", s"Cancelled Exchange: ${getMemberName(exchange.providerMemberId)} \u2194 ${getMemberName(exchange.receiverMemberId)}")
  end cancelExchange
  
  /**
   * Completes a service exchange, executing an atomic transfer of credits from receiver
   * to provider, creating audit transaction entries, and closing both skill posts.
   */
  //defines a public function named completeExchangeAtomic that
  // takes an exchange match ID, completes the transaction process, and returns a Try wrapper.
  def completeExchangeAtomic(exchangeId: Int): Try[Unit] =
    Try:
      //queries the database to find the service exchange booking record matching the provided exchangeId.
      val exchange = exchangeRepo.getAll().find(_.id.contains(exchangeId))
        //throws a validation error if the booking record could not be found in the database.
        .getOrElse(throw new IllegalArgumentException(s"Service exchange with ID $exchangeId not found"))
        
      // checks if the booking is already completed.
      if exchange.status == ExchangeStatus.Completed then {
        // a match cannot be completed again.
        throw new IllegalStateException("This service exchange has already been completed.")
      }
      // check if the exchange is pending or accepted.
      if exchange.status != ExchangeStatus.Accepted && exchange.status != ExchangeStatus.Pending then
        throw new IllegalStateException("Service exchange must be in Accepted or Pending state to complete.")

      // queries the database to find the provider member profile matching the helper's ID in the booking.
      val provider = memberRepo.getAll().find(_.id.contains(exchange.providerMemberId))
        //throws an error if the provider member profile cannot be found.
        .getOrElse(throw new IllegalStateException(s"Provider member with ID ${exchange.providerMemberId} not found."))
      //queries the database to find the receiver member profile matching the receiver's ID in the booking.
      val receiver = memberRepo.getAll().find(_.id.contains(exchange.receiverMemberId))
        // throws an error if the receiver member profile cannot be found.
        .getOrElse(throw new IllegalStateException(s"Receiver member with ID ${exchange.receiverMemberId} not found."))
 
      // Verify receiver has enough credits to pay.
      // checks if the receiver's wallet credit balance is less than the total credits cost of the exchange.
      if receiver.creditBalance < exchange.credits then {
        // an error explaining that the receiver does not have enough points to complete the trade.
        throw new IllegalArgumentException(s"Receiver '${receiver.name}' has insufficient credits (${receiver.creditBalance}) for this exchange (${exchange.credits} credits).")
      }

      // Update the exchange status to Completed
      //  and saves the updated record in the database exchanges table.
      exchangeRepo.update(exchange.copy(status = ExchangeStatus.Completed))
      // logs a new system activity timeline event on the dashboard showing that the match has been successfully completed.
      addActivityLog("Completed Exchange", s"Completed Exchange: ${getMemberName(exchange.providerMemberId)} \u2194 ${getMemberName(exchange.receiverMemberId)}")
 
      // searches for the associated skill offer post in the database.
      offerRepo.getAll().find(_.id.contains(exchange.offerId)).foreach: offer =>
        // sets the status of the associated skill offer post to Closed so others cannot see or book it anymore.
        offerRepo.update(offer.copy(status = PostStatus.Closed))
      // searches for the associated skill request post in the database.
      requestRepo.getAll().find(_.id.contains(exchange.requestId)).foreach: request =>
        //sets the status of the associated skill request post to Closed so it is archived from the active requests list.
        requestRepo.update(request.copy(status = PostStatus.Closed))
 
      // Update credits (add to provider, subtract from receiver).
      memberRepo.update(provider.copy(creditBalance = provider.creditBalance + exchange.credits))
      memberRepo.update(receiver.copy(creditBalance = receiver.creditBalance - exchange.credits))

      // formats today's current date as a text string (like "14-08-2026") to record when the transaction occurred.
      val currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
      // extracts the database ID number of the exchange booking, defaulting to 0 if not found.
      val exchangeIdVal = exchange.id.getOrElse(0)
      // checks if the provider has a valid database ID number.
      provider.id.foreach: provId =>
        // creates and saves a new credit transaction audit log in the database
        // showing that the provider earned these credits from the exchange.
        transactionRepo.add(CreditTransaction(
          memberId = provId,
          amount = exchange.credits,
          transactionType = TransactionType.Earn,
          note = s"Earned credits for exchange (ID: $exchangeIdVal)",
          date = currentDate
        ))
      // Save the spending audit log for the receiver.
      // checks if the receiver member has a valid database ID number, saving it in a variable named recId
      receiver.id.foreach: recId =>
        // begins the creation of a new credit transaction audit log to record the spending event.
        transactionRepo.add(CreditTransaction(
          memberId = recId, //links the transaction log to the receiver's ID.
          amount = exchange.credits, //sets the number of points that were deducted from the receiver's wallet.
          transactionType = TransactionType.Spend, //sets the transaction type to Spend to show that points were deducted.
          //creates a description note detailing the reason for spending (referencing the completed exchange ID).
          note = s"Spent credits for exchange (ID: $exchangeIdVal)",
          date = currentDate // sets the date of the spending transaction record.
        ))
  end completeExchangeAtomic

  /** Retrieves all exchanges. */
  //  defines a public query function named getAllExchanges that
  //  retrieves the list of all service exchange bookings and returns them as a sequence list.
  def getAllExchanges(): Seq[ServiceExchange] =
    exchangeRepo.getAll() //calls the exchange repository manager to
    // query and pull all booking records from the database table.

  /** Retrieves all credit audit logs. */
  // defines a public query function named getAllCreditTransactions
  // that retrieves the list of all transaction logs and returns them as a sequence list.
  def getAllCreditTransactions(): Seq[CreditTransaction] =
    transactionRepo.getAll() //calls the transaction repository manager to
    // query and pull all credit log rows from the database table.
  
  /** Updates member credit balance directly. */
  //defines a public function named updateCredits that
  // directly changes a user's wallet credit balance and returns a Try wrapper.
  def updateCredits(memberId: Int, newBalance: Int): Try[Unit] =
    Try:
      //queries all member profiles to search for the member
      // matching the provided memberId using pattern matching.
      memberRepo.getAll().find(_.id.contains(memberId)) match
        //proceeds if the member profile is found, saving it in a variable named member.
        case Some(member) =>
          //updates the member's wallet balance to the newBalance value
          // and saves the change in the database members table.
          memberRepo.update(member.copy(creditBalance = newBalance))
        case None =>
          throw new IllegalArgumentException(s"Member with ID $memberId not found")
  end updateCredits
  
  /** Directly saves a transaction. */
  def recordTransaction(transaction: CreditTransaction): CreditTransaction =
    transactionRepo.add(transaction)
  
  // Dashboard statistics helper methods
  // retrieves all members from the database,
  // counts how many there are using .size, and returns that count number.
  def getTotalMembers(): Int = memberRepo.getAll().size

  //retrieves all skill offers from the database and
  // returns the count of those that are currently open.
  def getTotalSkillOffers(): Int = offerRepo.getAll().count(_.status == PostStatus.Open)

  //retrieves all skill requests from the database and
  // returns the count of those that are currently open.
  def getTotalSkillRequests(): Int = requestRepo.getAll().count(_.status == PostStatus.Open)

  //retrieves all matches from the database and
  // returns the count of those that are marked as completed.
  def getTotalCompletedExchanges(): Int = exchangeRepo.getAll().count(_.status == ExchangeStatus.Completed)

  // retrieves completed matches from the database,
  // extracts their credit points cost, and returns the total sum of all points traded.
  def getTotalCreditsExchanged(): Int = exchangeRepo.getAll().filter(_.status == ExchangeStatus.Completed).map(_.credits).sum

  // defines a query function to
  // retrieve the most recent transaction logs up to the specified limit count.
  def getRecentTransactions(limit: Int): Seq[CreditTransaction] = {
    // retrieves all transaction logs, sorts them by ID in reverse order (newest first),
    // and keeps only the top rows matching the limit number.
    transactionRepo.getAll().sortBy(_.id)(Ordering[Option[Int]].reverse).take(limit)
  }

  //defines a helper function that takes a member ID number and
  // returns the corresponding member's name as a string.
  def getMemberName(memberId: Int): String = {
    //earches all members to find the matching ID,
    // returning their name if found, or returning a default placeholder (like "Member #5") if not found.
    memberRepo.getAll().find(_.id.contains(memberId)).map(_.name).getOrElse(s"Member #$memberId")
  }

  //defines a helper function that calculates and
  // returns the next formatted Member ID string (like "M008") in sequence.
  def getNextMemberId(): String =
    //extracts all member IDs from the database,
    // finds the highest ID number, and defaults to 0 if the database is empty.
    val maxId = memberRepo.getAll().flatMap(_.id).maxOption.getOrElse(0)
    // increments the highest ID by 1 and formats it into a three-digit padded text string starting with "M
    f"M${maxId + 1}%03d"

  // defines a helper function that calculates and
  // returns the next available integer ID number for skill posts.
  def getNextSkillPostIdNumeric(): Int =
    //finds the highest ID number among all registered skill offers, defaulting to 0 if none exist.
    val maxOfferId = offerRepo.getAll().flatMap(_.id).maxOption.getOrElse(0)
    //finds the highest ID number among all registered skill requests, defaulting to 0 if none exist.
    val maxRequestId = requestRepo.getAll().flatMap(_.id).maxOption.getOrElse(0)
    //  compares the highest offer ID and request ID,
    //  selects the larger of the two, and adds 1 to get the next number in sequence.
    Math.max(maxOfferId, maxRequestId) + 1

  //defines a helper function that calculates and
  // returns the next formatted Skill Post ID string (like "P019").
  def getNextSkillPostId(): String = {
    //takes the next numeric ID,
    // formats it as a three-digit padded text string, and prefixes it with "P".
    f"P${getNextSkillPostIdNumeric()}%03d"
  }

  // defines a helper function that calculates and
  // returns the next formatted Service Exchange ID string (like "SE006").
  def getNextExchangeId(): String =
    //finds the highest ID number among all service exchanges, defaulting to 0.
    val maxId = exchangeRepo.getAll().flatMap(_.id).maxOption.getOrElse(0)
    // increments the highest ID by 1 and formats it as a padded three-digit text string starting with "SE".
    f"SE${maxId + 1}%03d"

  // defines a helper function that calculates and
  // returns the next formatted Credit Transaction ID string (like "C005").
  def getNextCreditTransactionId(): String =
    // finds the highest ID number among all transaction audit records, defaulting to 0.
    val maxId = transactionRepo.getAll().flatMap(_.id).maxOption.getOrElse(0)
    // increments the highest ID by 1 and formats it as a padded three-digit text string starting with "C".
    f"C${maxId + 1}%03d"

  // defines a public function named closeSkillOffer that
  // closes a skill offer post by ID and returns a Try wrapper.
  def closeSkillOffer(id: Int): Try[Unit] = Try:
    // queries the database to find the skill offer post matching the provided ID.
    val offer = offerRepo.getAll().find(_.id.contains(id))
      // throws a validation error if the offer post could not be found in the database.
      .getOrElse(throw new IllegalArgumentException(s"Skill Offer with ID $id not found."))
    //updates the offer status to Closed and saves the updated record in the database offers table.
    offerRepo.update(offer.copy(status = PostStatus.Closed))

  // defines a public function named closeSkillRequest that
  // closes a skill request post by ID, wrapping the database action in a safety Try block.
  def closeSkillRequest(id: Int): Try[Unit] = Try:
    // queries the database to find the skill request post matching the provided ID.
    val request = requestRepo.getAll().find(_.id.contains(id))
      // throws a validation error if the request post could not be found in the database.
      .getOrElse(throw new IllegalArgumentException(s"Skill Request with ID $id not found."))
    // updates the request status to Closed and saves the updated record in the database requests table.
    requestRepo.update(request.copy(status = PostStatus.Closed))
end MarketplaceManager
