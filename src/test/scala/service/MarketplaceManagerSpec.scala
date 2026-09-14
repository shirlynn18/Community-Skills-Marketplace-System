package service

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import model._
import scala.util.{Success, Failure}

/**
 * Mock repository helper to run tests completely in-memory without accessing a live Derby database instance.
 */
class MockRepository[T](var items: Seq[T], idGetter: T => Option[Int], idSetter: (T, Int) => T) extends Repository[T]:
  private var nextId = 1
  override def add(item: T): T =
    val id = idGetter(item).getOrElse {
      val assigned = nextId
      nextId += 1
      assigned
    }
    val newItem = idSetter(item, id)
    items = items.filterNot(idGetter(_) == Some(id)) :+ newItem
    newItem

  override def update(item: T): Unit =
    idGetter(item).foreach: id =>
      items = items.map(x => if idGetter(x) == Some(id) then item else x)

  override def delete(id: Int): Unit =
    items = items.filterNot(idGetter(_) == Some(id))

  override def getAll(): Seq[T] = items
end MockRepository

/**
 * Unit test suite verifying skill matching rules and service exchange lifecycles.
 */
class MarketplaceManagerSpec extends AnyFunSpec with Matchers:

  /**
   * Helper fixture that instantiates in-memory repositories and initializes MarketplaceManager.
   */
  def createFixture() =
    val members = Seq(
      Member(Some(1), "Alice", "+6012", "Taman Tyng", 100),
      Member(Some(2), "Bob", "+6019", "Taman Segah", 50)
    )
    val memberRepo = new MockRepository[Member](members, _.id, (m, id) => m.copy(id = Some(id)))
    val offerRepo = new MockRepository[SkillOffer](Nil, _.id, (o, id) => o.copy(id = Some(id)))
    val requestRepo = new MockRepository[SkillRequest](Nil, _.id, (r, id) => r.copy(id = Some(id)))
    val exchangeRepo = new MockRepository[ServiceExchange](Nil, _.id, (e, id) => e.copy(id = Some(id)))
    val transactionRepo = new MockRepository[CreditTransaction](Nil, _.id, (t, id) => t.copy(id = Some(id)))
    val activityLogRepo = new MockRepository[ActivityLog](Nil, _.id, (a, id) => a.copy(id = Some(id)))

    val manager = new MarketplaceManager(memberRepo, offerRepo, requestRepo, exchangeRepo, transactionRepo, activityLogRepo)
    (manager, offerRepo, requestRepo, exchangeRepo, memberRepo)
  end createFixture

  describe("Skill Matching Rules"):

    it("should allow matching if skill names match case-insensitively"):
      val (manager, offerRepo, requestRepo, exchangeRepo, memberRepo) = createFixture()
      val bob = memberRepo.getAll().find(_.id.contains(2)).get
      memberRepo.update(bob.copy(creditBalance = 100))

      val offer = offerRepo.add(SkillOffer(None, 1, "scala programming", "Desc", PostStatus.Open, 20.0))
      val request = requestRepo.add(SkillRequest(None, 2, "SCALA PROGRAMMING", "Desc", PostStatus.Open, 3.0))

      val result = manager.matchOfferRequest(offer.id.get, request.id.get, "20-07-2026")
      result shouldBe a[Success[_]]
      result.get.credits shouldBe 60

    it("should reject matching if skill names do not match"):
      val (manager, offerRepo, requestRepo, _, _) = createFixture()
      val offer = offerRepo.add(SkillOffer(None, 1, "Scala", "Desc", PostStatus.Open, 20.0))
      val request = requestRepo.add(SkillRequest(None, 2, "Guitar", "Desc", PostStatus.Open, 3.0))

      val result = manager.matchOfferRequest(offer.id.get, request.id.get, "20-07-2026")
      result shouldBe a[Failure[_]]
      result.failed.get.getMessage should include("Skill mismatch")

    it("should reject matching if provider and receiver are the same member"):
      val (manager, offerRepo, requestRepo, _, _) = createFixture()
      val offer = offerRepo.add(SkillOffer(None, 1, "Scala", "Desc", PostStatus.Open, 20.0))
      val request = requestRepo.add(SkillRequest(None, 1, "Scala", "Desc", PostStatus.Open, 3.0))

      val result = manager.matchOfferRequest(offer.id.get, request.id.get, "20-07-2026")
      result shouldBe a[Failure[_]]
      result.failed.get.getMessage should include("Self-booking is not allowed")

    it("should reject matching if receiver has insufficient credits"):
      val (manager, offerRepo, requestRepo, _, _) = createFixture()
      val offer = offerRepo.add(SkillOffer(None, 1, "Scala", "Desc", PostStatus.Open, 50.0))
      val request = requestRepo.add(SkillRequest(None, 2, "Scala", "Desc", PostStatus.Open, 3.0)) // requires 150, Bob has 50

      val result = manager.matchOfferRequest(offer.id.get, request.id.get, "20-07-2026")
      result shouldBe a[Failure[_]]
      result.failed.get.getMessage should include("insufficient credits")

    it("should reject matching if a post is already matched in an active exchange"):
      val (manager, offerRepo, requestRepo, _, _) = createFixture()
      val offer = offerRepo.add(SkillOffer(None, 1, "Scala", "Desc", PostStatus.Open, 10.0))
      val request = requestRepo.add(SkillRequest(None, 2, "Scala", "Desc", PostStatus.Open, 3.0))

      // First match
      manager.matchOfferRequest(offer.id.get, request.id.get, "20-07-2026") shouldBe a[Success[_]]

      // Second request attempt to match same offer
      val request2 = requestRepo.add(SkillRequest(None, 2, "Scala", "Desc", PostStatus.Open, 2.0))
      val result = manager.matchOfferRequest(offer.id.get, request2.id.get, "21-07-2026")
      result shouldBe a[Failure[_]]
      result.failed.get.getMessage should (include("already associated") or include("not Open"))

  describe("Service Exchange Lifecycle"):

    it("should set status to Pending and change posts to Matched on match creation"):
      val (manager, offerRepo, requestRepo, _, _) = createFixture()
      val offer = offerRepo.add(SkillOffer(None, 1, "Scala", "Desc", PostStatus.Open, 10.0))
      val request = requestRepo.add(SkillRequest(None, 2, "Scala", "Desc", PostStatus.Open, 3.0))

      val ex = manager.matchOfferRequest(offer.id.get, request.id.get, "20-07-2026").get
      ex.status shouldBe ExchangeStatus.Pending
      
      offerRepo.getAll().find(_.id == offer.id).get.status shouldBe PostStatus.Matched
      requestRepo.getAll().find(_.id == request.id).get.status shouldBe PostStatus.Matched

    it("should revert posts to Open when an exchange is cancelled"):
      val (manager, offerRepo, requestRepo, exchangeRepo, _) = createFixture()
      val offer = offerRepo.add(SkillOffer(None, 1, "Scala", "Desc", PostStatus.Open, 10.0))
      val request = requestRepo.add(SkillRequest(None, 2, "Scala", "Desc", PostStatus.Open, 3.0))

      val ex = manager.matchOfferRequest(offer.id.get, request.id.get, "20-07-2026").get
      manager.cancelExchange(ex.id.get) shouldBe a[Success[_]]

      offerRepo.getAll().find(_.id == offer.id).get.status shouldBe PostStatus.Open
      requestRepo.getAll().find(_.id == request.id).get.status shouldBe PostStatus.Open
      exchangeRepo.getAll().find(_.id == ex.id).get.status shouldBe ExchangeStatus.Cancelled

    it("should close posts and transfer credits when an exchange is completed"):
      val (manager, offerRepo, requestRepo, _, memberRepo) = createFixture()
      val offer = offerRepo.add(SkillOffer(None, 1, "Scala", "Desc", PostStatus.Open, 10.0)) // Provider: Alice (starts at 100)
      val request = requestRepo.add(SkillRequest(None, 2, "Scala", "Desc", PostStatus.Open, 3.0)) // Receiver: Bob (starts at 50)

      val ex = manager.matchOfferRequest(offer.id.get, request.id.get, "20-07-2026").get
      manager.completeExchangeAtomic(ex.id.get) shouldBe a[Success[_]]

      offerRepo.getAll().find(_.id == offer.id).get.status shouldBe PostStatus.Closed
      requestRepo.getAll().find(_.id == request.id).get.status shouldBe PostStatus.Closed

      memberRepo.getAll().find(_.id.contains(1)).get.creditBalance shouldBe 130 // Alice earned 30
      memberRepo.getAll().find(_.id.contains(2)).get.creditBalance shouldBe 20  // Bob spent 30
end MarketplaceManagerSpec
