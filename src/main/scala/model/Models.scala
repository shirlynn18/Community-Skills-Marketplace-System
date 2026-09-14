package model

// Represents the current status of a skill post.
enum PostStatus:
  case Open, Matched, Closed
end PostStatus

// Represents the current status of a service exchange match.
enum ExchangeStatus:
  case Pending, Accepted, Completed, Cancelled
end ExchangeStatus

// represents the direction of a credit transaction
// Credits points enter/leave member's wallet
enum TransactionType:
  case Earn, Spend
end TransactionType

// Represents a registered community member profile.
// Store profile details and credit balances
case class Member(
  // defines the unique ID number of the member, wrapping it inside an Option and
  // defaulting it to None since new members do not have a database ID
  // until they are saved to the database.
  id: Option[Int] = None,
  name: String,
  contact: String,
  area: String,
  creditBalance: Int, 
  // defines a text string parameter to store the date the member registered,
  // which auto defaults to today's date formatted as day-month-year
  // if no date is manually passed.
  registrationDate: String = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
)

// common properties shared by both skill offers and skill requests.
// only classes in the same file can extend it.
sealed trait SkillPost:
  //stored as an Option[Int] bcs a new post may not have an assigned ID yet.
  def id: Option[Int]
  // any class extending this blueprint must include the ID number of the member who published the post.
  def memberId: Int
  def skillName: String
  def description: String
  def status: PostStatus
  def postDate: String
end SkillPost

// Represents a skill offered by a provider, include hourly credit rate.
case class SkillOffer(
  // ID may or may not have a value = default None (new created object has not yet been assigned an ID)
  id: Option[Int] = None,
  memberId: Int,
  skillName: String,
  description: String,
  status: PostStatus,
  creditRate: Double, 
  // defines a text string parameter to store the creation date,
  // which automatically defaults to today's date formatted as day-month-year.
  postDate: String = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
) extends SkillPost

// Represents a skill requested by a receiver,
// include estimated number of hours.
case class SkillRequest(
  // ID may or may not have a value = default None (new created object has not yet been assigned an ID)
  id: Option[Int] = None,
  memberId: Int,
  skillName: String,
  description: String,
  status: PostStatus,
  neededHours: Double,
 // defines a text string parameter to store the creation date, which automatically defaults to today's date formatted as day-month-year.
  postDate: String = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
) extends SkillPost

/**
 * @param credits Total calculated credits for the exchange (neededHours * creditRate).
 * @param date The date scheduled for the service exchange.
 */
// Represents a booked match between a skill offer and request.
case class ServiceExchange(
  //ID may or may not have a value = default None (new created object has not yet been assigned an ID)
  id: Option[Int] = None,
  offerId: Int,
  requestId: Int,
  providerMemberId: Int,
  receiverMemberId: Int,
  credits: Int,
  date: String,
  status: ExchangeStatus
)

// Represents a record of every credit transaction
case class CreditTransaction(
  // ID may or may not have a value = default None (new created object has not yet been assigned an ID)
  id: Option[Int] = None,
  memberId: Int,
  // total number of credit points involved in the transfer
  amount: Int,
  transactionType: TransactionType, //Earn or Spend
  note: String, // defines a text description note detailing the reason for the transfer.
  date: String
)

// Represents a system activity event record displayed on the Dashboard.
// member registration / skill posts created / service exchange status
case class ActivityLog(
  // ID may or may not have a value = default None (new created object has not yet been assigned an ID)
  id: Option[Int] = None,
  // category type of the action
  activityType: String, 
  //description message of the activity.
  description: String,
  dateStr: String
)
