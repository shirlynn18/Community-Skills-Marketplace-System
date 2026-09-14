package controller

import javafx.fxml.FXML
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.{ReadOnlyStringWrapper, ReadOnlyObjectWrapper}
import scalafx.scene.control.{Button, Label, Alert}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.VBox
import service.MarketplaceManager
import model.{ServiceExchange, ExchangeStatus, PostStatus}

// ── Data classes for table rows ──────────────────────────────────────────────

/**
 * Dropdown item selection representation for offers in the matcher.
 */
// represent an option in the "Offers" dropdown selector,
// which holds the offer post ID number and a descriptive text string
case class OfferChoice(id: Int, displayText: String):
  // shows the readable description text (e.g., "P001 - Piano Lessons")
  override def toString: String = displayText
end OfferChoice

/**
 * Dropdown item selection representation for requests in the matcher.
 */
// represent an option in the "Requests" dropdown selector,
// which holds the request post ID number and a descriptive text string.
case class RequestChoice(id: Int, displayText: String):
  // ensure that the dropdown menu displays the readable request description text
  // (e.g., "P002 - House Repairs").
  override def toString: String = displayText
end RequestChoice

/**
 * Data model representing rows in the service matches table.
 */
// holds the exact details for one row in the service match table.
// have linked to actual Member and SkillPost objects
// to avoid copy-pasting values into a temporary class.
case class ExchangeRow(
  id: Int,
  offerId: Int,
  requestId: Int,
  skillName: String,
  providerId: Int,
  providerName: String,
  receiverId: Int,
  receiverName: String,
  hours: Double,
  credits: Int,
  date: String,
  status: String
)

/**
 * Data model representing rows in the credit transaction log table.
 */
// holds the details for one row in the audit history table.
case class TxRow(
  id: Int,
  memberId: Int,
  memberIdStr: String,
  memberName: String,
  amount: Int, // credit amount
  txType: String, //transaction type (Earn/Spend)
  changeText: String, //points change indicator text (like "+50" or "-50")
  skillName: String,
  date: String,
  note: String, //description notes detailing the transaction reason.
  balanceAfter: Int // member's wallet credit balance remaining after transaction processed.
)

// ── Controller ────────────────────────────────────────────────────────────────

import scala.annotation.nowarn

/**
 * ServiceExchangeController handles booking skill matches, accepting matches,
 * executing completed credit transfers, and displaying audit trails.
 */
@nowarn
class ServiceExchangeController:

  // ── Step 1: FXML Injected Variables (JavaFX components) ────────────────────
  // Combobox containing list of available skill offers
  @FXML private var cmbOffers: javafx.scene.control.ComboBox[OfferChoice] = _
  
  // Combobox containing list of available skill requests
  @FXML private var cmbRequests: javafx.scene.control.ComboBox[RequestChoice] = _
  
  // Calendar picker for scheduling exchange match dates
  @FXML private var dpBookingDate: javafx.scene.control.DatePicker = _
  
  // Button to confirm and register a service exchange match
  @FXML private var btnMatch: javafx.scene.control.Button = _

  // Matcher Real-time Preview Labels
  @FXML private var lblPreviewSkillName: javafx.scene.control.Label = _
  @FXML private var lblPreviewCreditRate: javafx.scene.control.Label = _
  @FXML private var lblPreviewNeededHours: javafx.scene.control.Label = _
  @FXML private var lblPreviewCalculatedCredits: javafx.scene.control.Label = _

  // Statistics KPI counts header labels
  @FXML private var lblStatPending: javafx.scene.control.Label = _
  @FXML private var lblStatAccepted: javafx.scene.control.Label = _
  @FXML private var lblStatCompleted: javafx.scene.control.Label = _
  @FXML private var lblStatCancelled: javafx.scene.control.Label = _

  // Button to clear selections and reset preview values
  @FXML private var btnResetView: javafx.scene.control.Button = _

  // Service Exchanges TableView and Columns
  @FXML private var tblExchanges: javafx.scene.control.TableView[ExchangeRow] = _
  @FXML private var colExId: javafx.scene.control.TableColumn[ExchangeRow, String] = _
  @FXML private var colExOfferId: javafx.scene.control.TableColumn[ExchangeRow, String] = _
  @FXML private var colExRequestId: javafx.scene.control.TableColumn[ExchangeRow, String] = _
  @FXML private var colExCredits: javafx.scene.control.TableColumn[ExchangeRow, java.lang.Integer] = _
  @FXML private var colExStatus: javafx.scene.control.TableColumn[ExchangeRow, String] = _
  @FXML private var colExDate: javafx.scene.control.TableColumn[ExchangeRow, String] = _

  // Credit Transactions TableView and Columns (Audit Log)
  @FXML private var tblTransactions: javafx.scene.control.TableView[TxRow] = _
  @FXML private var colTxId: javafx.scene.control.TableColumn[TxRow, String] = _
  @FXML private var colTxMember: javafx.scene.control.TableColumn[TxRow, String] = _
  @FXML private var colTxMemberName: javafx.scene.control.TableColumn[TxRow, String] = _
  @FXML private var colTxChange: javafx.scene.control.TableColumn[TxRow, String] = _
  @FXML private var colTxAmount: javafx.scene.control.TableColumn[TxRow, java.lang.Integer] = _
  @FXML private var colTxDate: javafx.scene.control.TableColumn[TxRow, String] = _

  // Details Panel Layout Containers
  @FXML private var vboxExchangeDetails: javafx.scene.layout.VBox = _
  @FXML private var vboxTransactionDetails: javafx.scene.layout.VBox = _

  // Exchange Detailed Form Labels
  @FXML private var lblExDetailId: javafx.scene.control.Label = _
  @FXML private var lblExDetailSkillName: javafx.scene.control.Label = _
  @FXML private var lblExDetailOfferPostId: javafx.scene.control.Label = _
  @FXML private var lblExDetailOfferIdName: javafx.scene.control.Label = _
  @FXML private var lblExDetailRequestPostId: javafx.scene.control.Label = _
  @FXML private var lblExDetailRequestIdName: javafx.scene.control.Label = _
  @FXML private var lblExDetailHours: javafx.scene.control.Label = _
  @FXML private var lblExDetailCredits: javafx.scene.control.Label = _

  // Transaction Detailed Form Labels (Separated Member ID and Name)
  @FXML private var lblTxDetailId: javafx.scene.control.Label = _
  @FXML private var lblTxDetailMemberId: javafx.scene.control.Label = _
  @FXML private var lblTxDetailMemberName: javafx.scene.control.Label = _
  @FXML private var lblTxDetailSkillName: javafx.scene.control.Label = _
  @FXML private var lblTxDetailChange: javafx.scene.control.Label = _
  @FXML private var lblTxDetailAmount: javafx.scene.control.Label = _
  @FXML private var lblTxDetailDate: javafx.scene.control.Label = _

  // Actions Container Panel Components (Pending -> Accepted -> Completed)
  @FXML private var vboxActionsContainer: javafx.scene.layout.VBox = _
  @FXML private var lblActionStatus: javafx.scene.control.Label = _
  @FXML private var btnAccept: javafx.scene.control.Button = _
  @FXML private var btnComplete: javafx.scene.control.Button = _
  @FXML private var btnCancel: javafx.scene.control.Button = _

  // ── Step 2: Declare ScalaFX Wrappers ───────────────────────────────────────
  private var sfBtnMatch: Button = _
  private var sfBtnResetView: Button = _
  private var sfBtnAccept: Button = _
  private var sfBtnComplete: Button = _
  private var sfBtnCancel: Button = _

  private var sfLblStatPending: Label = _
  private var sfLblStatAccepted: Label = _
  private var sfLblStatCompleted: Label = _
  private var sfLblStatCancelled: Label = _

  private var sfLblPreviewSkillName: Label = _
  private var sfLblPreviewCreditRate: Label = _
  private var sfLblPreviewNeededHours: Label = _
  private var sfLblPreviewCalculatedCredits: Label = _

  private var sfLblExDetailId: Label = _
  private var sfLblExDetailSkillName: Label = _
  private var sfLblExDetailOfferPostId: Label = _
  private var sfLblExDetailOfferIdName: Label = _
  private var sfLblExDetailRequestPostId: Label = _
  private var sfLblExDetailRequestIdName: Label = _
  private var sfLblExDetailHours: Label = _
  private var sfLblExDetailCredits: Label = _

  private var sfLblTxDetailId: Label = _
  private var sfLblTxDetailMemberId: Label = _
  private var sfLblTxDetailMemberName: Label = _
  private var sfLblTxDetailSkillName: Label = _
  private var sfLblTxDetailChange: Label = _
  private var sfLblTxDetailAmount: Label = _
  private var sfLblTxDetailDate: Label = _

  private var sfLblActionStatus: Label = _

  private var sfVboxExchangeDetails: VBox = _
  private var sfVboxTransactionDetails: VBox = _
  private var sfVboxActionsContainer: VBox = _

  // Backend service and parent references
  private var manager: MarketplaceManager = _
  private var allExchanges: Seq[ExchangeRow] = Seq.empty
  private var allTxRows: Seq[TxRow] = Seq.empty
  private var shellController: ShellController = _

  // Avoid selection recursion feedback loops
  private var isSelectingFromTx = false

  /** Sets parent coordinator reference. */
  def setShellController(sc: ShellController): Unit =
    // Save reference to the parent coordinator controller.
    this.shellController = sc

  /** Pre-selects specific offers or requests when navigated programmatically from the Posts screen. */
  def selectOfferAndRequest(offerId: Option[Int], requestId: Option[Int]): Unit =
    import scala.jdk.CollectionConverters._
    offerId.foreach: id =>
      // Find and select the matching offer choice in the dropdown.
      cmbOffers.getItems.asScala.find(_.id == id).foreach(cmbOffers.value = _)
    requestId.foreach: id =>
      // Find and select the matching request choice in the dropdown.
      cmbRequests.getItems.asScala.find(_.id == id).foreach(cmbRequests.value = _)
  end selectOfferAndRequest

  /** Coordinates cell renders, TableView columns, selections sync, and DatePicker constraints. */
  @FXML
  def initialize(): Unit =
    // Step 3: Instantiate ScalaFX wrappers using raw FXML components
    sfBtnMatch = new Button(btnMatch)
    sfBtnResetView = new Button(btnResetView)
    sfBtnAccept = new Button(btnAccept)
    sfBtnComplete = new Button(btnComplete)
    sfBtnCancel = new Button(btnCancel)

    sfLblStatPending = new Label(lblStatPending)
    sfLblStatAccepted = new Label(lblStatAccepted)
    sfLblStatCompleted = new Label(lblStatCompleted)
    sfLblStatCancelled = new Label(lblStatCancelled)

    sfLblPreviewSkillName = new Label(lblPreviewSkillName)
    sfLblPreviewCreditRate = new Label(lblPreviewCreditRate)
    sfLblPreviewNeededHours = new Label(lblPreviewNeededHours)
    sfLblPreviewCalculatedCredits = new Label(lblPreviewCalculatedCredits)

    sfLblExDetailId = new Label(lblExDetailId)
    sfLblExDetailSkillName = new Label(lblExDetailSkillName)
    sfLblExDetailOfferPostId = new Label(lblExDetailOfferPostId)
    sfLblExDetailOfferIdName = new Label(lblExDetailOfferIdName)
    sfLblExDetailRequestPostId = new Label(lblExDetailRequestPostId)
    sfLblExDetailRequestIdName = new Label(lblExDetailRequestIdName)
    sfLblExDetailHours = new Label(lblExDetailHours)
    sfLblExDetailCredits = new Label(lblExDetailCredits)

    sfLblTxDetailId = new Label(lblTxDetailId)
    sfLblTxDetailMemberId = new Label(lblTxDetailMemberId)
    sfLblTxDetailMemberName = new Label(lblTxDetailMemberName)
    sfLblTxDetailSkillName = new Label(lblTxDetailSkillName)
    sfLblTxDetailChange = new Label(lblTxDetailChange)
    sfLblTxDetailAmount = new Label(lblTxDetailAmount)
    sfLblTxDetailDate = new Label(lblTxDetailDate)

    sfLblActionStatus = new Label(lblActionStatus)

    sfVboxExchangeDetails = new VBox(vboxExchangeDetails)
    sfVboxTransactionDetails = new VBox(vboxTransactionDetails)
    sfVboxActionsContainer = new VBox(vboxActionsContainer)

    // Step 4: Map table column values using string wrappers (formats IDs like SE00X or P00X)
    colExId.cellValueFactory        = { r => ReadOnlyStringWrapper(f"SE${r.value.id}%03d") }
    colExOfferId.cellValueFactory   = { r => ReadOnlyStringWrapper(f"P${r.value.offerId}%03d") }
    colExRequestId.cellValueFactory = { r => ReadOnlyStringWrapper(f"P${r.value.requestId}%03d") }
    colExCredits.cellValueFactory   = { r => ReadOnlyObjectWrapper[java.lang.Integer](r.value.credits) }
    colExStatus.cellValueFactory    = { r => ReadOnlyStringWrapper(r.value.status) }
    colExDate.cellValueFactory      = { r => ReadOnlyStringWrapper(r.value.date) }

    // Color-code Status column values (yellow=pending, green=accepted/completed, red=cancelled)
    colExStatus.cellFactory = (_: javafx.scene.control.TableColumn[ExchangeRow, String]) => new javafx.scene.control.TableCell[ExchangeRow, String]:
      override def updateItem(item: String, empty: Boolean): Unit =
        super.updateItem(item, empty)
        if empty || item == null then
          setText(null)
          setStyle("")
        else
          setText(item)
          item match
            case "Pending"   => setStyle("-fx-text-fill: -yellow;  -fx-font-weight: bold;")
            case "Accepted"  => setStyle("-fx-text-fill: -status-matched; -fx-font-weight: bold;")
            case "Completed" => setStyle("-fx-text-fill: -status-active;  -fx-font-weight: bold;")
            case "Cancelled" => setStyle("-fx-text-fill: -brick;   -fx-font-weight: bold;")
            case _           => setStyle("")

    // Step 5: Map Credit Transactions column values (formats IDs like CT00X)
    colTxId.cellValueFactory     = { r => ReadOnlyStringWrapper(f"CT${r.value.id}%03d") }
    colTxMember.cellValueFactory = { r => ReadOnlyStringWrapper(r.value.memberIdStr) }
    colTxMemberName.cellValueFactory = { r => ReadOnlyStringWrapper(r.value.memberName) }
    colTxChange.cellValueFactory = { r => ReadOnlyStringWrapper(r.value.changeText) }
    colTxAmount.cellValueFactory = { r => ReadOnlyObjectWrapper[java.lang.Integer](r.value.balanceAfter) }
    colTxDate.cellValueFactory   = { r => ReadOnlyStringWrapper(r.value.date) }

    // Color-code credit changes (Green for Earn (+), Red for Spend (-))
    colTxChange.cellFactory = (_: javafx.scene.control.TableColumn[TxRow, String]) => new javafx.scene.control.TableCell[TxRow, String]:
      override def updateItem(item: String, empty: Boolean): Unit =
        super.updateItem(item, empty)
        if empty || item == null then
          setText(null)
          setStyle("")
        else
          setText(item)
          if item.startsWith("+") then
            setStyle("-fx-text-fill: -status-active; -fx-font-weight: bold;")
          else
            setStyle("-fx-text-fill: -brick; -fx-font-weight: bold;")

    // Step 6: Setup selection model listeners
    tblExchanges.selectionModel.value.selectedItemProperty().addListener: (_, _, sel) =>
      if sel != null then
        if !isSelectingFromTx then
          // Clear selections on the transactions table to avoid loop overlaps.
          tblTransactions.selectionModel.value.clearSelection()
          // Sync: filter transaction history to display only credit entries for the selected completed exchange
          if sel.status == "Completed" then
            filterTransactionsByExchange(sel)
          else
            // Otherwise restore full log list
            val buf = new ObservableBuffer[TxRow]()
            buf ++= allTxRows
            tblTransactions.items = buf
        // Update details panel with selection.
        showExchangeDetails(sel)

    tblTransactions.selectionModel.value.selectedItemProperty().addListener: (_, _, sel) =>
      if sel != null then
        isSelectingFromTx = true
        // Find corresponding service exchange ID using note regex and select it
        val exIdOpt = """ID:\s*(\d+)""".r.findFirstMatchIn(sel.note).map(_.group(1).toInt)
        exIdOpt.flatMap(id => allExchanges.find(_.id == id)).foreach { exRow =>
          tblExchanges.selectionModel.value.select(exRow)
        }
        isSelectingFromTx = false
        // Update details panel with transaction info.
        showTransactionDetails(sel)

    // Step 7: Setup matcher real-time preview selection listeners
    cmbOffers.valueProperty().addListener((_, _, _) => updatePreview())
    cmbRequests.valueProperty().addListener((_, _, _) => updatePreview())
    dpBookingDate.valueProperty().addListener((_, _, _) => updatePreview())

    // Block/Disable past booking dates in DatePicker calendar node
    dpBookingDate.dayCellFactory = (picker: javafx.scene.control.DatePicker) => new javafx.scene.control.DateCell:
      override def updateItem(item: java.time.LocalDate, empty: Boolean): Unit =
        super.updateItem(item, empty)
        if item != null && item.isBefore(java.time.LocalDate.now()) then
          setDisable(true)
          setStyle("-fx-background-color: -surface-alt; -fx-text-fill: -ink-soft;")

    sfBtnMatch.disable = false
    // Show default empty panel view.
    showExchangeDetails(null)
  end initialize

  /** Wire manager. */
  def setMarketplaceManager(m: MarketplaceManager): Unit =
    // Save database manager instance.
    manager = m
    // Fetch and load data.
    loadData()
  end setMarketplaceManager

  /** Fetches open offers/requests, filters out already matched IDs, and refreshes tables. */
  def loadData(): Unit =
    if manager == null then return

    // ── Active Offer / Request dropdowns (exclude matched items) ──────────
    val activeEx = manager.getAllExchanges().filter(_.status != model.ExchangeStatus.Cancelled)
    val usedOfferIds = activeEx.map(_.offerId).toSet
    val usedRequestIds = activeEx.map(_.requestId).toSet

    val offerBuf = new ObservableBuffer[OfferChoice]()
    offerBuf ++= manager.getAllOffers()
      .filter(o => o.status == PostStatus.Open && !usedOfferIds.contains(o.id.getOrElse(0)))
      .map: o =>
        val mName = manager.getMemberName(o.memberId)
        OfferChoice(o.id.getOrElse(0),
          f"P${o.id.getOrElse(0)}%03d - ${o.skillName} (M${o.memberId}%03d - $mName)")
    cmbOffers.items = offerBuf

    val reqBuf = new ObservableBuffer[RequestChoice]()
    reqBuf ++= manager.getAllRequests()
      .filter(r => r.status == PostStatus.Open && !usedRequestIds.contains(r.id.getOrElse(0)))
      .map: r =>
        val mName = manager.getMemberName(r.memberId)
        RequestChoice(r.id.getOrElse(0),
          f"P${r.id.getOrElse(0)}%03d - ${r.skillName} (M${r.memberId}%03d - $mName)")
    cmbRequests.items = reqBuf

    // ── Service Exchanges mapping ──
    val allOffers   = manager.getAllOffers()
    val allRequests = manager.getAllRequests()

    allExchanges = manager.getAllExchanges().map { se =>
      val skillName = allOffers.find(_.id.contains(se.offerId)).map(_.skillName)
        .orElse(allRequests.find(_.id.contains(se.requestId)).map(_.skillName))
        .getOrElse("N/A")

      val hours = allRequests.find(_.id.contains(se.requestId)).map(_.neededHours)
        .getOrElse(0.0)

      val providerName = manager.getMemberName(se.providerMemberId)
      val receiverName = manager.getMemberName(se.receiverMemberId)

      ExchangeRow(
        id           = se.id.getOrElse(0),
        offerId      = se.offerId,
        requestId    = se.requestId,
        skillName    = skillName,
        providerId   = se.providerMemberId,
        providerName = f"M${se.providerMemberId}%03d - $providerName",
        receiverId   = se.receiverMemberId,
        receiverName = f"M${se.receiverMemberId}%03d - $receiverName",
        hours        = hours,
        credits      = se.credits,
        date         = se.date,
        status       = se.status.toString
      )
    }.sortBy(_.id)(Ordering[Int].reverse)

    val exBuf = new ObservableBuffer[ExchangeRow]()
    exBuf ++= allExchanges
    tblExchanges.items = exBuf

    // ── Stats summary counter updates ──
    val raw = manager.getAllExchanges()
    sfLblStatPending.text   = raw.count(_.status == ExchangeStatus.Pending).toString
    sfLblStatAccepted.text  = raw.count(_.status == ExchangeStatus.Accepted).toString
    sfLblStatCompleted.text = raw.count(_.status == ExchangeStatus.Completed).toString
    sfLblStatCancelled.text = raw.count(_.status == ExchangeStatus.Cancelled).toString

    // ── Credit Transactions mapping ──
    allTxRows = manager.getAllCreditTransactions().map { tx =>
      val mName      = manager.getMemberName(tx.memberId)
      val memberIdStr = f"M${tx.memberId}%03d"
      val changeText = if tx.transactionType.toString == "Earn" then s"+${tx.amount}" else s"-${tx.amount}"

      val exIdOpt  = """ID:\s*(\d+)""".r.findFirstMatchIn(tx.note).map(_.group(1).toInt)
      val skillName = exIdOpt.flatMap { exId =>
        manager.getAllExchanges().find(_.id.contains(exId)).flatMap { se =>
          allOffers.find(_.id.contains(se.offerId)).map(_.skillName)
            .orElse(allRequests.find(_.id.contains(se.requestId)).map(_.skillName))
        }
      }.getOrElse("N/A")

      val balance = manager.getMemberById(tx.memberId).map(_.creditBalance).getOrElse(0)

      TxRow(
        id         = tx.id.getOrElse(0),
        memberId   = tx.memberId,
        memberIdStr = memberIdStr,
        memberName = mName,
        amount     = tx.amount,
        txType     = tx.transactionType.toString,
        changeText = changeText,
        skillName  = skillName,
        date       = tx.date,
        note       = tx.note,
        balanceAfter = balance
      )
    }.sortBy(_.id)(Ordering[Int].reverse)

    // Restore selection after reload
    val selEx = tblExchanges.selectionModel.value.getSelectedItem
    val selTx = tblTransactions.selectionModel.value.getSelectedItem

    if selEx != null then
      if selEx.status == "Completed" then
        filterTransactionsByExchange(selEx)
      else
        val buf = new ObservableBuffer[TxRow]()
        buf ++= allTxRows
        tblTransactions.items = buf
      showExchangeDetails(selEx)
    else if selTx != null then
      val buf = new ObservableBuffer[TxRow]()
      buf ++= allTxRows
      tblTransactions.items = buf
      showTransactionDetails(selTx)
    else
      val buf = new ObservableBuffer[TxRow]()
      buf ++= allTxRows
      tblTransactions.items = buf
      showExchangeDetails(null)
    adjustTableHeights()
  end loadData

  /** Resets matching selections, clearing previews. */
  @FXML
  def handleResetView(): Unit =
    // Clear selection models.
    tblExchanges.selectionModel.value.clearSelection()
    tblTransactions.selectionModel.value.clearSelection()
    // Reset dropdown values.
    cmbOffers.value = null
    cmbRequests.value = null
    dpBookingDate.setValue(null)
    clearPreview()
    showExchangeDetails(null)

    // Restore full transactions list
    val txBuf = new ObservableBuffer[TxRow]()
    txBuf ++= allTxRows
    tblTransactions.items = txBuf
    adjustTableHeights()
  end handleResetView

  /** Real-time validation checks for matcher options. Disables match button if validation fails. */
  private def updatePreview(): Unit =
    val offerChoice   = cmbOffers.value.value
    val requestChoice = cmbRequests.value.value

    if offerChoice != null && requestChoice != null && manager != null then
      val offerOpt = manager.getAllOffers().find(_.id.contains(offerChoice.id))
      val reqOpt   = manager.getAllRequests().find(_.id.contains(requestChoice.id))

      (offerOpt, reqOpt) match
        case (Some(offer), Some(req)) =>
          // Set preview values.
          sfLblPreviewSkillName.text        = offer.skillName
          sfLblPreviewCreditRate.text       = f"${offer.creditRate}%.1f / hr"
          sfLblPreviewNeededHours.text      = f"${req.neededHours}%.1f hrs"
          val credits                     = (req.neededHours * offer.creditRate).toInt
          sfLblPreviewCalculatedCredits.text = s"$credits Credits"
          sfBtnMatch.disable = false
        case _ => clearPreview()
    else
      clearPreview()
  end updatePreview

  /** Clears matcher previews. */
  private def clearPreview(): Unit =
    sfLblPreviewSkillName.text         = "—"
    sfLblPreviewCreditRate.text        = "—"
    sfLblPreviewNeededHours.text       = "—"
    sfLblPreviewCalculatedCredits.text = "—"
    sfBtnMatch.disable = false
  end clearPreview

  /** Filters transaction list to match selected exchange ID. */
  // ai-assisted: log-#13
  // why: To structure the logic for filtering credit transactions based on the selected service exchange.
  // defines a private helper function named filterTransactionsByExchange that
  // filters the credit transaction history list on the screen to show only entries
  // related to a selected exchange.
  private def filterTransactionsByExchange(ex: ExchangeRow): Unit =
    // creates a new empty reactive list buffer named buf to hold the filtered transaction rows.
    val buf = new ObservableBuffer[TxRow]()
    // filters the full list of transactions,
    // searching for description notes containing the exchange's ID string,
    // and appends the matching records to the buffer.
    buf ++= allTxRows.filter(_.note.contains(s"ID: ${ex.id}"))
    // assigns the filtered list buffer to the transaction history table widget
    // on the screen to show only the matching entries.
    tblTransactions.items = buf
    //  calls a helper method to resize the height of the table
    //  on the screen so that it matches the number of rows displayed.
    adjustTableHeights()
  end filterTransactionsByExchange

  /** Populates details panel showing active buttons depending on exchange lifecycle state. */
  private def showExchangeDetails(ex: ExchangeRow): Unit =
    // Toggle details box visibilities.
    sfVboxTransactionDetails.visible = false
    sfVboxTransactionDetails.managed = false
    sfVboxExchangeDetails.visible    = true
    sfVboxExchangeDetails.managed    = true
    sfVboxActionsContainer.visible   = true
    sfVboxActionsContainer.managed   = true

    if ex != null then
      val offerOpt = manager.getAllOffers().find(_.id.contains(ex.offerId))
      val reqOpt   = manager.getAllRequests().find(_.id.contains(ex.requestId))

      val offerPostIdStr = f"P${ex.offerId}%03d"
      val reqPostIdStr   = f"P${ex.requestId}%03d"

      sfLblExDetailId.text            = f"SE${ex.id}%03d"
      sfLblExDetailSkillName.text     = ex.skillName
      sfLblExDetailOfferPostId.text   = offerPostIdStr
      sfLblExDetailOfferIdName.text   = ex.providerName
      sfLblExDetailRequestPostId.text = reqPostIdStr
      sfLblExDetailRequestIdName.text = ex.receiverName

      val hoursVal = reqOpt.map(_.neededHours).getOrElse(ex.hours)
      sfLblExDetailHours.text         = f"$hoursVal%.1f hrs"
      sfLblExDetailCredits.text       = s"${ex.credits} Credits"

      sfLblActionStatus.text = ex.status
      
      // Toggle button availabilities depending on status state
      ex.status match
        case "Pending" =>
          sfLblActionStatus.style = "-fx-text-fill: -yellow; -fx-font-weight: bold;"
          sfBtnAccept.visible = true;  sfBtnAccept.disable = false; sfBtnAccept.managed = true
          sfBtnCancel.visible = true;  sfBtnCancel.disable = false; sfBtnCancel.managed = true
          sfBtnComplete.visible = false; sfBtnComplete.disable = true; sfBtnComplete.managed = false
        case "Accepted" =>
          sfLblActionStatus.style = "-fx-text-fill: -status-matched; -fx-font-weight: bold;"
          sfBtnComplete.visible = true; sfBtnComplete.disable = false; sfBtnComplete.managed = true
          sfBtnCancel.visible = true;  sfBtnCancel.disable = false; sfBtnCancel.managed = true
          sfBtnAccept.visible = false;  sfBtnAccept.disable = true; sfBtnAccept.managed = false
        case "Completed" =>
          sfLblActionStatus.style = "-fx-text-fill: -status-active; -fx-font-weight: bold;"
          sfBtnAccept.visible = true;  sfBtnAccept.disable = true; sfBtnAccept.managed = true
          sfBtnComplete.visible = true; sfBtnComplete.disable = true; sfBtnComplete.managed = true
          sfBtnCancel.visible = true;  sfBtnCancel.disable = true; sfBtnCancel.managed = true
        case "Cancelled" =>
          sfLblActionStatus.style = "-fx-text-fill: -brick; -fx-font-weight: bold;"
          sfBtnAccept.visible = true;  sfBtnAccept.disable = true; sfBtnAccept.managed = true
          sfBtnComplete.visible = true; sfBtnComplete.disable = true; sfBtnComplete.managed = true
          sfBtnCancel.visible = true;  sfBtnCancel.disable = true; sfBtnCancel.managed = true
        case _ =>
          sfLblActionStatus.style = "-fx-text-fill: -ink-soft; -fx-font-weight: bold;"
          sfBtnAccept.visible = true;  sfBtnAccept.disable = true; sfBtnAccept.managed = true
          sfBtnComplete.visible = true; sfBtnComplete.disable = true; sfBtnComplete.managed = true
          sfBtnCancel.visible = true;  sfBtnCancel.disable = true; sfBtnCancel.managed = true
    else
      // Default / empty state details
      val nextId = if manager != null then manager.getNextExchangeId() else "SE001"
      sfLblExDetailId.text            = nextId
      sfLblExDetailSkillName.text     = "None"
      sfLblExDetailOfferPostId.text   = "None"
      sfLblExDetailOfferIdName.text   = "None"
      sfLblExDetailRequestPostId.text = "None"
      sfLblExDetailRequestIdName.text = "None"
      sfLblExDetailHours.text         = "None"
      sfLblExDetailCredits.text       = "None"
      
      sfLblActionStatus.text = "None"
      sfLblActionStatus.style = "-fx-text-fill: -ink-soft; -fx-font-weight: bold;"

      sfBtnAccept.visible = true;  sfBtnAccept.disable = true; sfBtnAccept.managed = true
      sfBtnComplete.visible = true; sfBtnComplete.disable = true; sfBtnComplete.managed = true
      sfBtnCancel.visible = true;  sfBtnCancel.disable = true; sfBtnCancel.managed = true
  end showExchangeDetails

  /** Populates details panel with transaction audit logs. */
  private def showTransactionDetails(tx: TxRow): Unit =
    // Toggle details box visibilities.
    sfVboxExchangeDetails.visible    = false
    sfVboxExchangeDetails.managed    = false
    sfVboxTransactionDetails.visible = true
    sfVboxTransactionDetails.managed = true
    sfVboxActionsContainer.visible   = false
    sfVboxActionsContainer.managed   = false

    sfLblTxDetailId.text         = f"CT${tx.id}%03d"
    sfLblTxDetailMemberId.text   = tx.memberIdStr
    sfLblTxDetailMemberName.text = tx.memberName
    sfLblTxDetailSkillName.text  = tx.skillName
    sfLblTxDetailChange.text     = tx.changeText
    if tx.changeText.startsWith("+") then
      sfLblTxDetailChange.style = "-fx-text-fill: -status-active; -fx-font-weight: bold;"
    else
      sfLblTxDetailChange.style = "-fx-text-fill: -brick; -fx-font-weight: bold;"
    sfLblTxDetailAmount.text     = s"${tx.balanceAfter} Credits"
    sfLblTxDetailDate.text       = tx.date
  end showTransactionDetails

  // ── FXML Handlers ────────────────────────────────────────────────────────

  /** Book match handler. Calls matchOfferRequest on service layer. */
  @FXML
  def handleMatch(): Unit =
    val offer   = cmbOffers.value.value
    val request = cmbRequests.value.value
    val rawDate = dpBookingDate.getValue

    if offer == null then
      showWarning("Match Warning", "Skill Offer Required", "Please select an active Skill Offer.")
    else if request == null then
      showWarning("Match Warning", "Skill Request Required", "Please select an active Skill Request.")
    else if rawDate == null then
      showWarning("Match Warning", "Exchange Date Required", "Please select an Exchange Date.")
    else
      val offerOpt = manager.getAllOffers().find(_.id.contains(offer.id))
      val reqOpt   = manager.getAllRequests().find(_.id.contains(request.id))

      (offerOpt, reqOpt) match
        case (Some(o), Some(r)) =>
          val credits = (r.neededHours * o.creditRate).toInt
          val namesMatch    = o.skillName.equalsIgnoreCase(r.skillName)
          val diffMembers   = o.memberId != r.memberId
          val requester     = manager.getMemberById(r.memberId)
          val enoughBalance = requester.map(_.creditBalance).getOrElse(0) >= credits
          val activeEx      = manager.getAllExchanges()
            .filter(se => se.status == ExchangeStatus.Pending || se.status == ExchangeStatus.Accepted)
          val notMatched    = !activeEx.exists(_.offerId == o.id.getOrElse(0)) &&
                              !activeEx.exists(_.requestId == r.id.getOrElse(0))

          if !namesMatch then
            showWarning("Match Warning", "Skill Name Mismatch", s"Cannot match '${o.skillName}' with '${r.skillName}'. Skill names must match (case-insensitive).")
          else if !diffMembers then
            showWarning("Match Warning", "Self Matching Prohibited", "Members cannot match with their own skill posts.")
          else if !enoughBalance then
            val balance = requester.map(_.creditBalance).getOrElse(0)
            showWarning("Match Warning", "Insufficient Credits", s"The requester has insufficient credit balance ($balance credits) for this exchange ($credits credits needed).")
          else if !notMatched then
            showWarning("Match Warning", "Double Matching Prohibited", "One or both of these skill posts are already matched or pending in another exchange.")
          else
            val date = rawDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            manager.matchOfferRequest(offer.id, request.id, date) match
              case scala.util.Success(_) =>
                loadData()
                cmbOffers.value = null; cmbRequests.value = null; dpBookingDate.setValue(null)
                showInfo("Success", "Match Created", "Service exchange registered as Pending.")
              case scala.util.Failure(e) =>
                showWarning("Match Warning", "Could not create match", e.getMessage)
        case _ =>
          showWarning("Match Warning", "Invalid Posts", "The selected offer or request is invalid.")
  end handleMatch

  /** Accept match handler. Updates status to Accepted on database. */
  @FXML
  def handleAcceptExchange(): Unit =
    withSelected: ex =>
      manager.acceptExchange(ex.id) match
        case scala.util.Success(_) =>
          loadData(); reselect(ex.id)
          showInfo("Success", "Exchange Accepted", "The provider has accepted the exchange.")
        case scala.util.Failure(e) =>
          showError("Accept Error", "Could not accept", e.getMessage)
  end handleAcceptExchange

  /** Complete exchange handler. Performs credit transfers and closes posts. */
  @FXML
  def handleCompleteExchange(): Unit =
    withSelected: ex =>
      manager.completeExchangeAtomic(ex.id) match
        case scala.util.Success(_) =>
          loadData(); reselect(ex.id)
          showInfo("Success", "Exchange Completed", "Credits transferred and transactions logged.")
        case scala.util.Failure(e) =>
          showError("Complete Error", "Could not complete", e.getMessage)
  end handleCompleteExchange

  /** Cancel exchange handler. Reverts posts back to Active. */
  @FXML
  def handleCancelExchange(): Unit =
    withSelected: ex =>
      manager.cancelExchange(ex.id) match
        case scala.util.Success(_) =>
          loadData(); reselect(ex.id)
          showInfo("Success", "Exchange Cancelled", "Exchange cancelled. Posts restored to Active.")
        case scala.util.Failure(e) =>
          showError("Cancel Error", "Could not cancel", e.getMessage)
  end handleCancelExchange

  // ── Helpers ──────────────────────────────────────────────────────────────

  private def withSelected(f: ExchangeRow => Unit): Unit =
    val sel = tblExchanges.selectionModel.value.getSelectedItem
    if sel != null then f(sel)
  end withSelected

  private def reselect(id: Int): Unit =
    allExchanges.find(_.id == id).foreach(tblExchanges.selectionModel.value.select)

  private def showError(titleVal: String, headerVal: String, contentVal: String): Unit =
    val alert = new Alert(AlertType.Error):
      title = titleVal
      headerText = headerVal
      contentText = contentVal
    alert.showAndWait()
  end showError

  private def showWarning(titleVal: String, headerVal: String, contentVal: String): Unit =
    val alert = new Alert(AlertType.Warning):
      title = titleVal
      headerText = headerVal
      contentText = contentVal
    alert.showAndWait()
  end showWarning

  private def showInfo(titleVal: String, headerVal: String, contentVal: String): Unit =
    val alert = new Alert(AlertType.Information):
      title = titleVal
      headerText = headerVal
      contentText = contentVal
    alert.showAndWait()
  end showInfo

  private def adjustTableHeights(): Unit =
    if tblExchanges != null && tblTransactions != null then
      val headerHeight = 41.0
      val rowHeight = 45.0
      val extraHeight = 10.0 // buffer to prevent any vertical scrollbars in TableView
      
      val exRows = tblExchanges.getItems.size()
      val exHeight = headerHeight + (exRows * rowHeight) + extraHeight
      tblExchanges.setPrefHeight(exHeight)
      tblExchanges.setMinHeight(exHeight)
      tblExchanges.setMaxHeight(exHeight)

      val txRows = tblTransactions.getItems.size()
      val txHeight = headerHeight + (txRows * rowHeight) + extraHeight
      tblTransactions.setPrefHeight(txHeight)
      tblTransactions.setMinHeight(txHeight)
      tblTransactions.setMaxHeight(txHeight)
  end adjustTableHeights
end ServiceExchangeController
