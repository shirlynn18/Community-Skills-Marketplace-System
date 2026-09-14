package controller

import javafx.fxml.FXML
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.{ReadOnlyStringWrapper, ReadOnlyObjectWrapper}
import scalafx.scene.control.{Button, Label, TextField, ComboBox, Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.VBox
import service.MarketplaceManager
import model.{SkillOffer, SkillRequest, PostStatus}

/**
 * Data representation for rows displayed in the Skill Post TableView.
 * Maps both Offers (with credit rates) and Requests (with needed hours)
 * into a single flat model.
 */
// holds the combined details of both offers and requests to
// display them together in a single table.
case class SkillPostRow(
  id: Int,
  memberId: Int,
  postType: String, // "Offer" or "Request"
  skillName: String,
  description: String,
  status: String,   // Active, Matched, Closed
  creditRate: Option[Double],
  neededHours: Option[Double],
  postDate: String
)

/**
 * Choice element mapping member ID keys to user-friendly text
 * descriptions in selection dropdowns.
 */
// represent an option in the "Member" dropdown selector on the form,
// holding their database ID and a name label.
case class MemberChoice(id: Int, displayText: String):
  // replaces the default system conversion function to ensure that when this option is listed in the dropdown menu,
  // it displays the member's readable name details (e.g., "M001 - Alice Smith")
  override def toString: String = displayText
end MemberChoice

import scala.annotation.nowarn

/**
 * SkillManagementController handles creation, status tracking, filters, edits, 
 * and deletions for both Skill Offers and Skill Requests.
 */
@nowarn
class SkillManagementController:

  // ── Step 1: Declare FXML Injected Variables (JavaFX components) ────────────
  // Text field capturing keywords for filtering skill posts
  @FXML private var txtSearch: javafx.scene.control.TextField = _
  
  // Combobox to filter table entries by type (All Types, Offer, Request)
  @FXML private var cmbTypeFilter: javafx.scene.control.ComboBox[String] = _
  
  // Combobox to filter table entries by status (All Statuses, Open, Matched, Closed)
  @FXML private var cmbStatusFilter: javafx.scene.control.ComboBox[String] = _
  
  // Button to reset all search inputs and combobox selections
  @FXML private var btnClearFilters: javafx.scene.control.Button = _

  // Statistics header labels showing totals of active, matched, and closed posts
  @FXML private var lblStatTotal: javafx.scene.control.Label = _
  @FXML private var lblStatActive: javafx.scene.control.Label = _
  @FXML private var lblStatMatched: javafx.scene.control.Label = _
  @FXML private var lblStatClosed: javafx.scene.control.Label = _

  // The main table listing skill offers and requests
  @FXML private var tblPosts: javafx.scene.control.TableView[SkillPostRow] = _
  
  // Individual columns representing skill post attributes
  @FXML private var colId: javafx.scene.control.TableColumn[SkillPostRow, String] = _
  @FXML private var colMemberId: javafx.scene.control.TableColumn[SkillPostRow, String] = _
  @FXML private var colType: javafx.scene.control.TableColumn[SkillPostRow, String] = _
  @FXML private var colSkillName: javafx.scene.control.TableColumn[SkillPostRow, String] = _
  @FXML private var colStatus: javafx.scene.control.TableColumn[SkillPostRow, String] = _
  @FXML private var colCreditRate: javafx.scene.control.TableColumn[SkillPostRow, java.lang.Double] = _
  @FXML private var colNeededHours: javafx.scene.control.TableColumn[SkillPostRow, java.lang.Double] = _
  @FXML private var colPostDate: javafx.scene.control.TableColumn[SkillPostRow, String] = _

  // Right-side detailed form card labels and inputs
  @FXML private var lblModeSubtitle: javafx.scene.control.Label = _
  @FXML private var lblDetailId: javafx.scene.control.Label = _
  @FXML private var lblDetailPostDate: javafx.scene.control.Label = _
  @FXML private var cmbFormType: javafx.scene.control.ComboBox[String] = _
  @FXML private var cmbFormMemberId: javafx.scene.control.ComboBox[MemberChoice] = _
  @FXML private var txtFormSkillName: javafx.scene.control.TextField = _
  @FXML private var lblFormDescription: javafx.scene.control.Label = _
  @FXML private var txtFormDescription: javafx.scene.control.TextField = _
  @FXML private var cmbFormStatus: javafx.scene.control.ComboBox[String] = _
  
  // Layout views conditionally rendered based on post type (Offer vs Request)
  @FXML private var lblFormHours: javafx.scene.control.Label = _
  @FXML private var txtFormHours: javafx.scene.control.TextField = _
  @FXML private var lblFormCreditRate: javafx.scene.control.Label = _
  @FXML private var txtFormCreditRate: javafx.scene.control.TextField = _

  // Action button container layout boxes
  @FXML private var vboxCreateButtons: javafx.scene.layout.VBox = _
  @FXML private var vboxEditButtons: javafx.scene.layout.VBox = _
  @FXML private var btnDelete: javafx.scene.control.Button = _

  // ── Step 2: Declare ScalaFX Wrappers ───────────────────────────────────────
  // ScalaFX wrappers wrap JavaFX controls to enable clean DSL properties
  private var sfTxtSearch: TextField = _
  private var sfCmbTypeFilter: ComboBox[String] = _
  private var sfCmbStatusFilter: ComboBox[String] = _
  private var sfBtnClearFilters: Button = _

  private var sfLblStatTotal: Label = _
  private var sfLblStatActive: Label = _
  private var sfLblStatMatched: Label = _
  private var sfLblStatClosed: Label = _

  private var sfLblModeSubtitle: Label = _
  private var sfLblDetailId: Label = _
  private var sfLblDetailPostDate: Label = _
  private var sfCmbFormType: ComboBox[String] = _
  private var sfCmbFormMemberId: ComboBox[MemberChoice] = _
  private var sfTxtFormSkillName: TextField = _
  private var sfLblFormDescription: Label = _
  private var sfTxtFormDescription: TextField = _
  private var sfCmbFormStatus: ComboBox[String] = _

  private var sfLblFormHours: Label = _
  private var sfTxtFormHours: TextField = _
  private var sfLblFormCreditRate: Label = _
  private var sfTxtFormCreditRate: TextField = _

  private var sfVboxCreateButtons: VBox = _
  private var sfVboxEditButtons: VBox = _
  private var sfBtnDelete: Button = _

  // Backend service and parent references
  private var manager: MarketplaceManager = _
  private var allPosts: Seq[SkillPostRow] = Seq.empty
  private var shellController: ShellController = _

  /** Sets parent controller reference. */
  def setShellController(sc: ShellController): Unit =
    // Save reference to the parent coordinator controller.
    this.shellController = sc

  /**
   * Initializes ScalaFX wrappers, binds custom cell-color render factories, 
   * maps table columns, and hooks reactive search filter triggers.
   */
  @FXML
  def initialize(): Unit =
    // Step 3: Instantiate ScalaFX wrappers using raw FXML components
    sfTxtSearch = new TextField(txtSearch)
    sfCmbTypeFilter = new ComboBox[String](cmbTypeFilter)
    sfCmbStatusFilter = new ComboBox[String](cmbStatusFilter)
    sfBtnClearFilters = new Button(btnClearFilters)

    sfLblStatTotal = new Label(lblStatTotal)
    sfLblStatActive = new Label(lblStatActive)
    sfLblStatMatched = new Label(lblStatMatched)
    sfLblStatClosed = new Label(lblStatClosed)

    sfLblModeSubtitle = new Label(lblModeSubtitle)
    sfLblDetailId = new Label(lblDetailId)
    sfLblDetailPostDate = new Label(lblDetailPostDate)
    sfCmbFormType = new ComboBox[String](cmbFormType)
    sfCmbFormMemberId = new ComboBox[MemberChoice](cmbFormMemberId)
    sfTxtFormSkillName = new TextField(txtFormSkillName)
    sfLblFormDescription = new Label(lblFormDescription)
    sfTxtFormDescription = new TextField(txtFormDescription)
    sfCmbFormStatus = new ComboBox[String](cmbFormStatus)

    sfLblFormHours = new Label(lblFormHours)
    sfTxtFormHours = new TextField(txtFormHours)
    sfLblFormCreditRate = new Label(lblFormCreditRate)
    sfTxtFormCreditRate = new TextField(txtFormCreditRate)

    sfVboxCreateButtons = new VBox(vboxCreateButtons)
    sfVboxEditButtons = new VBox(vboxEditButtons)
    sfBtnDelete = new Button(btnDelete)

    // Step 4: Map table column values using string wrappers (formats IDs like P00X or M00X)
    colId.cellValueFactory = { features => ReadOnlyStringWrapper(f"P${features.value.id}%03d") }
    colMemberId.cellValueFactory = { features => ReadOnlyStringWrapper(f"M${features.value.memberId}%03d") }
    colType.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.postType) }
    
    // Custom cell render coloring: Yellow for Offers, Purple for Requests (matches dashboard bar colors)
    colType.cellFactory = (_: javafx.scene.control.TableColumn[SkillPostRow, String]) => new javafx.scene.control.TableCell[SkillPostRow, String]:
      override def updateItem(item: String, empty: Boolean): Unit =
        super.updateItem(item, empty)
        if empty || item == null then
          setText(null)
          setStyle("")
        else
          setText(item)
          item match
            case "Offer"   => setStyle("-fx-text-fill: -yellow; -fx-font-weight: bold;")
            case "Request" => setStyle("-fx-text-fill: -purple; -fx-font-weight: bold;")
            case _         => setStyle("")
    
    colSkillName.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.skillName) }
    colStatus.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.status) }
    
    // Map optional numeric values.
    colCreditRate.cellValueFactory = { features => 
      val rate = features.value.creditRate.map(r => java.lang.Double.valueOf(r)).orNull
      ReadOnlyObjectWrapper[java.lang.Double](rate) 
    }
    colNeededHours.cellValueFactory = { features => 
      val hours = features.value.neededHours.map(h => java.lang.Double.valueOf(h)).orNull
      ReadOnlyObjectWrapper[java.lang.Double](hours) 
    }
    colPostDate.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.postDate) }

    // Renders '-' symbol for empty optional fields (e.g. Offers do not have neededHours)
    colCreditRate.cellFactory = (_: javafx.scene.control.TableColumn[SkillPostRow, java.lang.Double]) =>
      new javafx.scene.control.TableCell[SkillPostRow, java.lang.Double]:
        override def updateItem(item: java.lang.Double, empty: Boolean): Unit =
          super.updateItem(item, empty)
          setText(if empty || item == null then "-" else item.toString)

    //sets up a custom text-renderer instruction (called a cell factory) for the "Needed Hours" table column,
    // defining how it should format the decimal number cell before printing it on the screen.
    colNeededHours.cellFactory = (_: javafx.scene.control.TableColumn[SkillPostRow, java.lang.Double]) =>
      //creates a new visual cell block object for the table row.
      new javafx.scene.control.TableCell[SkillPostRow, java.lang.Double]:
        // overrides the default system rendering function that is called automatically every time
        // the table draws or updates a cell, passing the raw decimal number value (the item)
        // and a true/false condition (called empty) showing if the cell has no data.
        override def updateItem(item: java.lang.Double, empty: Boolean): Unit =
          // runs the standard parent JavaFX cell drawing setup first,
          // preserving default properties like background colors and text alignments.
          super.updateItem(item, empty)
          // determines what text is displayed in the cell: if the cell has no data or is empty
          // (for example, if the row is an Offer post instead of a Request post), it displays a - symbol; otherwise,
          // it converts the decimal hours number into a text string and prints it in the cell.
          setText(if empty || item == null then "-" else item.toString)
    
    // Renders color styles for the status column (green = open, blue = matched, grey = closed)
    //  sets up a custom text-renderer instruction (called a cell factory) for the "Status" column,
    //  telling the table to create a custom cell block to apply specialized colors and styles to the text.
    colStatus.cellFactory = (_: javafx.scene.control.TableColumn[SkillPostRow, String]) => new javafx.scene.control.TableCell[SkillPostRow, String]:
      // overrides the default system cell drawing function that runs automatically whenever the table refreshes,
      // accepting the status text string (the item) and a true/false condition (called empty) showing if the cell is blank.
      override def updateItem(item: String, empty: Boolean): Unit =
        // runs the standard parent JavaFX cell drawing setup first, preserving default table cell behaviors.
        super.updateItem(item, empty)
        // checks if the row has no data or if the status value is empty.
        if empty || item == null then
          // clears out any old text inside the cell
          setText(null)
          // clears out any previously applied text colors and font styles from the cell.
          setStyle("")
        else
          //prints the status text string directly into the table cell on the screen.
          setText(item)
          // pattern matching block to check the exact text value of the status string
          item match
            case "Open"    => setStyle("-fx-text-fill: -status-active; -fx-font-weight: bold;")
            case "Matched"   => setStyle("-fx-text-fill: -status-matched; -fx-font-weight: bold;")
            case "Closed"    => setStyle("-fx-text-fill: -status-closed; -fx-font-weight: bold;")
            // fallback case, resetting the cell styling to default if the status string matches none of the choices.
            case _           => setStyle("")

    // Listen for member table selection changes to update the detailed form card
    tblPosts.selectionModel.value.selectedItemProperty().addListener: (_, _, selectedRow) =>
      showPostDetails(selectedRow)

    // Bind reactive listeners to trigger query updates when filters are modified
    sfTxtSearch.textProperty().addListener((_, _, _) => applyFilters())
    sfCmbTypeFilter.valueProperty().addListener((_, _, _) => applyFilters())
    sfCmbStatusFilter.valueProperty().addListener((_, _, _) => applyFilters())

    // Form layout listener (toggles visibility of creditRate vs neededHours based on selected postType)
    sfCmbFormType.valueProperty().addListener: (_, _, newType) =>
      adjustFormFields(newType)

    // Populate dropdown options with standard choices
    sfCmbTypeFilter.items = ObservableBuffer("All Types", "Offer", "Request")
    sfCmbTypeFilter.value = "All Types"

    sfCmbStatusFilter.items = ObservableBuffer("All Statuses", "Open", "Matched", "Closed")
    sfCmbStatusFilter.value = "All Statuses"

    sfCmbFormType.items = ObservableBuffer("Offer", "Request")
    sfCmbFormStatus.items = ObservableBuffer("Open", "Matched", "Closed")

    // Default card view to Create/Register Mode
    showPostDetails(null)
  end initialize

  /** Injects service layer and loads details. */
  //defines a helper function named setMarketplaceManager that
  // receives a connection reference to the main database manager coordinator (the m)
  // and returns no output value.
  def setMarketplaceManager(m: MarketplaceManager): Unit =
    // Save database connection instance locally inside the controller
    this.manager = m
    // Load members list dropdown options.
    populateMemberChoiceDropdown()
    // Load skill posts table from database.
    loadPosts()
    //  resets the details form card to register mode and auto-generates the next post ID now that the database is connected.
    showPostDetails(null)
  end setMarketplaceManager

  /** Populates member selections dropdown choices. */
  // defines a private helper function named populateMemberChoiceDropdown that
  // fetches member records from the database and loads them as choices in the dropdown selector.
  private def populateMemberChoiceDropdown(): Unit =
    // stops execution immediately if the database manager connection is not set yet
    if manager == null then return
    // asks the database manager to fetch the complete list of all registered members.
    val members = manager.getAllMembers()
    // loops through the fetched members list, mapping each member object to a dropdown selection choice object.
    val choices = members.map: m =>
      // packages the member's ID and formatted name string (like "M001 - Alice Smith") into a choice object.
      MemberChoice(m.id.getOrElse(0), f"M${m.id.getOrElse(0)}%03d - ${m.name}")
    //takes the list of choices, wraps them in a reactive list container, and
    // assigns them to the member selection dropdown list on the screen.
    sfCmbFormMemberId.items = ObservableBuffer.from(choices)
  end populateMemberChoiceDropdown

  /** Loads all skill posts. */
  // responsible for pulling all skill offers and requests from the database and
  // loading them into the table.
  def loadPosts(): Unit =
    // stops execution immediately if the database manager connection is not set yet
    if manager == null then return
    // calls the helper function to refresh the member selection dropdown list
    // so it matches the current list of members in the database.
    populateMemberChoiceDropdown()

    // 1. Fetch the complete list of all offers and requests from database manager
    val offers = manager.getAllOffers()
    val requests = manager.getAllRequests()

    // 2. Map different data structures into a single unified SkillPostRow list
    val rows = (offers.map { o =>
      // Gets the offer ID, or uses 0 if the ID is missing., None - Needed Hour
      SkillPostRow(o.id.getOrElse(0), o.memberId, "Offer", o.skillName, o.description, o.status.toString, Some(o.creditRate), None, o.postDate)
    // combines all offer and request rows into one list,
    // processes each skill request one at a time.
    } ++ requests.map { r =>
      // None - Credit Rate/hour
      SkillPostRow(r.id.getOrElse(0), r.memberId, "Request", r.skillName, r.description, r.status.toString, None, Some(r.neededHours), r.postDate)
    }).sortBy(_.id)(Ordering[Int].reverse) // Sort all skill post rows by ID in des order (new first)

    // saves the sorted list of all posts inside a local variable named allPosts
    allPosts = rows
    // Run active filters (search box/ selected in filters).
    applyFilters()
    // Updates header statistics. (count active,matched, closed posts)
    updateStatistics(rows)
  end loadPosts

  /** Applies type, status, and query keywords to filter table rows. */
  private def applyFilters(): Unit =
    val query = Option(sfTxtSearch.text.value).getOrElse("").trim.toLowerCase
    val selectedType = sfCmbTypeFilter.value.value
    val selectedStatus = sfCmbStatusFilter.value.value

    val filtered = allPosts.filter: post =>
      // Match query search terms against skill titles, descriptions, post IDs, or member IDs
      val matchesQuery = post.skillName.toLowerCase.contains(query) ||
        post.description.toLowerCase.contains(query) ||
        f"P${post.id}%03d".toLowerCase.contains(query) ||
        f"M${post.memberId}%03d".toLowerCase.contains(query)

      // Match post type filter selection
      val matchesType = selectedType match
        case "Offer" => post.postType == "Offer"
        case "Request" => post.postType == "Request"
        case _ => true

      // Match status filter selection
      val matchesStatus = selectedStatus match
        case "Open"    => post.status == "Open"
        case "Matched"   => post.status == "Matched"
        case "Closed"    => post.status == "Closed"
        case _ => true

      matchesQuery && matchesType && matchesStatus
    tblPosts.items = ObservableBuffer.from(filtered)
  end applyFilters

  /** Updates header KPI statistics labels. */
  private def updateStatistics(posts: Seq[SkillPostRow]): Unit =
    sfLblStatTotal.text = posts.size.toString
    sfLblStatActive.text = posts.count(_.status == "Open").toString
    sfLblStatMatched.text = posts.count(_.status == "Matched").toString
    sfLblStatClosed.text = posts.count(_.status == "Closed").toString
  end updateStatistics

  /** Resets filters and updates view. */
  @FXML
  def handleClearFilters(): Unit =
    // Clear search text box.
    sfTxtSearch.text = ""
    // Reset type filter dropdown.
    sfCmbTypeFilter.value = "All Types"
    // Reset status filter dropdown.
    sfCmbStatusFilter.value = "All Statuses"
    // Reapply filter changes.
    applyFilters()
  end handleClearFilters

  /** Toggles layout visibility of credit rate and needed hours fields based on post type. */
  private def adjustFormFields(postType: String): Unit =
    if postType == "Offer" then
      // Hide hours input box and show rate input box.
      sfLblFormHours.visible = false
      sfLblFormHours.managed = false
      sfTxtFormHours.visible = false
      sfTxtFormHours.managed = false

      sfLblFormCreditRate.visible = true
      sfLblFormCreditRate.managed = true
      sfTxtFormCreditRate.visible = true
      sfTxtFormCreditRate.managed = true
    else
      // Show hours input box and hide rate input box.
      sfLblFormHours.visible = true
      sfLblFormHours.managed = true
      sfTxtFormHours.visible = true
      sfTxtFormHours.managed = true

      sfLblFormCreditRate.visible = false
      sfLblFormCreditRate.managed = false
      sfTxtFormCreditRate.visible = false
      sfTxtFormCreditRate.managed = false
  end adjustFormFields

  /** Populates details panel inputs with selected post details. Resets fields if null. */
  private def showPostDetails(selected: SkillPostRow): Unit =
    if selected == null then
      // Setup detailed card panel in Create Mode
      sfLblModeSubtitle.text = "CREATE NEW POST"
      if sfLblFormDescription != null then sfLblFormDescription.text = "Description *:"
      if sfBtnDelete != null then sfBtnDelete.disable = false
      
      // Auto-compute next ID
      val nextPostId = if manager != null then manager.getNextSkillPostId() else "P001"
      sfLblDetailId.text = nextPostId
      
      // Get today's date formatted.
      val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
      sfLblDetailPostDate.text = today
      
      // Reset form variables.
      sfCmbFormType.value = "Offer"
      sfCmbFormMemberId.value = null
      sfTxtFormSkillName.text = ""
      sfTxtFormDescription.text = ""
      sfCmbFormStatus.items = ObservableBuffer("Open")
      sfCmbFormStatus.value = "Open"
      sfTxtFormHours.text = ""
      sfTxtFormCreditRate.text = ""
      
      sfCmbFormType.disable = false
      sfCmbFormMemberId.disable = false
      sfCmbFormStatus.disable = true // default status for new posts is always Open

      if lblModeSubtitle != null && vboxCreateButtons != null && vboxEditButtons != null then
        // Display register buttons and hide edit buttons.
        sfVboxCreateButtons.visible = true
        sfVboxCreateButtons.managed = true
        sfVboxEditButtons.visible = false
        sfVboxEditButtons.managed = false
      adjustFormFields("Offer")
    else
      // Setup detailed card panel in Edit Mode
      sfLblModeSubtitle.text = "EDIT SKILL POST"
      if sfLblFormDescription != null then sfLblFormDescription.text = "Description:"
      sfLblDetailId.text = f"P${selected.id}%03d"
      sfLblDetailPostDate.text = selected.postDate
      
      sfCmbFormType.value = selected.postType
      
      // Bind Member dropdown selection
      val memberItem = sfCmbFormMemberId.items.value.find(_.id == selected.memberId).orNull
      sfCmbFormMemberId.value = memberItem

      sfTxtFormSkillName.text = selected.skillName
      sfTxtFormDescription.text = selected.description
      
      // Matched posts temporarily show "Matched" status in list (read-only)
      if selected.status == "Matched" then
        sfCmbFormStatus.items = ObservableBuffer("Open", "Closed", "Matched")
      else
        sfCmbFormStatus.items = ObservableBuffer("Open", "Closed")
      sfCmbFormStatus.value = selected.status
      
      sfTxtFormHours.text = selected.neededHours.map(_.toString).getOrElse("")
      sfTxtFormCreditRate.text = selected.creditRate.map(_.toString).getOrElse("")

      sfCmbFormType.disable = true
      sfCmbFormMemberId.disable = true
      
      // Lock status selection if the post is associated with a pending, accepted, or completed exchange
      // ai-assisted: log-#10
      // why: To structure the logic for preventing users from manually modifying 
      // the status of posts involved in Pending, Accepted, or Completed exchanges.
      // Check if the database manager connection is active to search for any bookings
      // related to the selected post.
      val isMatchedOrCompletedExchange = if manager != null then {
        // scans all service exchanges in the database to see if
        // at least one match matches our criteria.
        manager.getAllExchanges().exists: se =>
          // checks if the booking is currently active or finished (Pending, Accepted, or Completed).
          (se.status == model.ExchangeStatus.Pending ||
           se.status == model.ExchangeStatus.Accepted ||
           se.status == model.ExchangeStatus.Completed) &&
          (if selected.postType == "Offer" then se.offerId == selected.id else se.requestId == selected.id)
          // links the check to this specific post ID, matching it as either an offer ID
          // or a request ID based on the post type.
      } else false // returns false if the database manager is not connected.

      // disables the status dropdown menu on the form
      // if the post is locked inside an active or completed exchange
      sfCmbFormStatus.disable = isMatchedOrCompletedExchange
      // disables the "Delete" button if the post is currently matched in an exchange
      if sfBtnDelete != null then sfBtnDelete.disable = (selected.status == "Matched")

      // checks if the screen layout containers are fully initialized.
      if lblModeSubtitle != null && vboxCreateButtons != null && vboxEditButtons != null then
        // Hide register buttons and display edit/delete buttons.
        sfVboxCreateButtons.visible = false //register buttons container invisible.
        sfVboxCreateButtons.managed = false // removes the screen layout space occupied by the register buttons.
        sfVboxEditButtons.visible = true //makes the edit/delete buttons container visible
        sfVboxEditButtons.managed = true //allocates layout space for the edit/delete buttons on the screen.
      //calls a helper method to show or hide credit rate/needed hours fields
      // based on the selected post type
      adjustFormFields(selected.postType)
  end showPostDetails

  /** Validates skill post form inputs, returning a tuple if successful. */
  private def validateInput(): Option[(Int, String, String, String, String, Option[Double], Option[Double])] =
    val memberChoice = sfCmbFormMemberId.value.value
    val postType = sfCmbFormType.value.value
    val skillName = sfTxtFormSkillName.text.value.trim
    val description = sfTxtFormDescription.text.value.trim
    val statusVal = sfCmbFormStatus.value.value

    if memberChoice == null then
      showErrorAlert("Validation Error", "Missing Field", "Please select a Member.")
      None
    else if skillName.isEmpty then
      showErrorAlert("Validation Error", "Missing Field", "Please enter a Skill Name.")
      None
    // Match only alphabetic letters and spaces
    else if !skillName.matches("^[a-zA-Z\\s]+$") then
      showErrorAlert("Validation Error", "Invalid Format", "Skill name must contain only alphabets and spaces.")
      None
    else if description.isEmpty then
      showErrorAlert("Validation Error", "Missing Field", "Please enter a Description.")
      None
    // Match only alphabetic letters and spaces
    else if !description.matches("^[a-zA-Z\\s]+$") then
      showErrorAlert("Validation Error", "Invalid Format", "Description must contain only alphabets and spaces.")
      None
    else if statusVal == null then
      showErrorAlert("Validation Error", "Missing Field", "Please select a Status.")
      None
    else
      val memberId = memberChoice.id
      val selectedRow = tblPosts.selectionModel.value.getSelectedItem
      val targetStatus = PostStatus.valueOf(statusVal)
      val isTargetActive = targetStatus == PostStatus.Open || targetStatus == PostStatus.Matched

      if isTargetActive then
        // Enforce the 3 active offers limit per member.
        if postType == "Offer" then
          val activeOffers = if manager != null then manager.getAllOffers().filter(o => o.memberId == memberId && (o.status == PostStatus.Open || o.status == PostStatus.Matched)) else Seq.empty
          val isEditingThisPost = selectedRow != null && selectedRow.postType == "Offer"
          val count = if isEditingThisPost then activeOffers.filterNot(_.id.contains(selectedRow.id)).size else activeOffers.size
          if count >= 3 then
            showErrorAlert("Limit Reached", "Maximum Active Offers Reached", "This member already has the maximum number of active or matched skill offers (3). Please close an existing post before adding/opening a new one.")
            return None
        // Enforce the 2 active requests limit per member.
        else
          val activeRequests = if manager != null then manager.getAllRequests().filter(r => r.memberId == memberId && (r.status == PostStatus.Open || r.status == PostStatus.Matched)) else Seq.empty
          val isEditingThisPost = selectedRow != null && selectedRow.postType == "Request"
          val count = if isEditingThisPost then activeRequests.filterNot(_.id.contains(selectedRow.id)).size else activeRequests.size
          if count >= 2 then
            showErrorAlert("Limit Reached", "Maximum Active Requests Reached", "This member already has the maximum number of active or matched skill requests (2). Please close an existing post before adding/opening a new one.")
            return None

      if postType == "Offer" then
        val rateStr = sfTxtFormCreditRate.text.value.trim
        try
          val rate = rateStr.toDouble
          if rate <= 0 then
            showErrorAlert("Validation Error", "Invalid Credit Rate", "Credit rate must be greater than zero.")
            None
          else
            Some((memberId, postType, skillName, description, statusVal, None, Some(rate)))
        catch
          case _: NumberFormatException =>
            showErrorAlert("Validation Error", "Invalid Credit Rate", "Credit rate must be a valid decimal number.")
            None
      else
        val hoursStr = sfTxtFormHours.text.value.trim
        try
          val hours = hoursStr.toDouble
          if hours <= 0 then
            showErrorAlert("Validation Error", "Invalid Needed Hours", "Needed hours must be greater than zero.")
            None
          else
            Some((memberId, postType, skillName, description, statusVal, Some(hours), None))
        catch
          case _: NumberFormatException =>
            showErrorAlert("Validation Error", "Invalid Needed Hours", "Needed hours must be a valid decimal number.")
            None
  end validateInput

  /** Validates and inserts a new skill offer or request post into database. */
  @FXML
  def handleCreate(): Unit =
    // Run validation first.
    validateInput() match
      case Some((memberId, postType, skillName, description, _, hoursOpt, creditRateOpt)) =>
        // Get formatted date.
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        if postType == "Offer" then
          // Construct and save the new offer.
          val offer = SkillOffer(None, memberId, skillName, description, PostStatus.Open, creditRateOpt.getOrElse(20.0), today)
          manager.createSkillOffer(offer)
        else
          // Construct and save the new request.
          val request = SkillRequest(None, memberId, skillName, description, PostStatus.Open, hoursOpt.getOrElse(4.0), today)
          manager.createSkillRequest(request)
        // Refresh tables.
        loadPosts()
        // Reset form.
        showPostDetails(null)
        showInfoAlert("Success", "Post Created", "New skill post added to marketplace.")
      case None =>
  end handleCreate

  /** Validates and saves edits on the selected post. */
  @FXML
  def handleSave(): Unit =
    val selected = tblPosts.selectionModel.value.getSelectedItem
    if selected != null then
      // Run validation first.
      validateInput() match
        case Some((memberId, postType, skillName, description, statusVal, hoursOpt, creditRateOpt)) =>
          val statusEnum = PostStatus.valueOf(statusVal)
          
          if postType == "Offer" then
            // Construct and update the offer.
            val offer = SkillOffer(Some(selected.id), memberId, skillName, description, statusEnum, creditRateOpt.getOrElse(20.0))
            manager.updateSkillOffer(offer)
          else
            // Construct and update the request.
            val request = SkillRequest(Some(selected.id), memberId, skillName, description, statusEnum, hoursOpt.getOrElse(4.0))
            manager.updateSkillRequest(request)
          // Refresh list.
          loadPosts()
          
          // Re-select updated row item
          val reselect = allPosts.find(_.id == selected.id)
          reselect.foreach(tblPosts.selectionModel.value.select)
          
          showInfoAlert("Success", "Changes Saved", "Skill post updated successfully.")
        case None =>
  end handleSave

  /** Confirms and deletes the selected post. */
  @FXML
  def handleDelete(): Unit =
    val selected = tblPosts.selectionModel.value.getSelectedItem
    if selected != null then
      // Confirm deletion using dialog box.
      val alert = new Alert(AlertType.Confirmation):
        title = "Confirm Delete"
        headerText = "Delete Skill Post"
        contentText = s"Are you sure you want to delete post '${selected.skillName}'?"
      
      val result = alert.showAndWait()
      // If confirmed, proceed.
      if result.isDefined && result.get == ButtonType.OK then
        if selected.postType == "Offer" then
          manager.deleteSkillOffer(selected.id)
        else
          manager.deleteSkillRequest(selected.id)
        // Refresh list.
        loadPosts()
        // Reset form card.
        showPostDetails(null)
        showInfoAlert("Success", "Post Deleted", "Skill post successfully removed.")
  end handleDelete

  /** Swaps form panel layout to Create Mode. */
  @FXML
  def handleNewForm(): Unit =
    // Clear selection.
    tblPosts.selectionModel.value.clearSelection()
    // Reset detailed form view.
    showPostDetails(null)
  end handleNewForm

  /** Clears selection. */
  @FXML
  def handleClearForm(): Unit =
    // Clear selection.
    tblPosts.selectionModel.value.clearSelection()
    // Reset detailed form view.
    showPostDetails(null)
  end handleClearForm

  /** Shows error dialog. */
  private def showErrorAlert(titleVal: String, headerVal: String, contentVal: String): Unit =
    val alert = new Alert(AlertType.Error):
      title = titleVal
      headerText = headerVal
      contentText = contentVal
    alert.showAndWait()
  end showErrorAlert

  /** Shows info dialog. */
  private def showInfoAlert(titleVal: String, headerVal: String, contentVal: String): Unit =
    val alert = new Alert(AlertType.Information):
      title = titleVal
      headerText = headerVal
      contentText = contentVal
    alert.showAndWait()
  end showInfoAlert
end SkillManagementController
