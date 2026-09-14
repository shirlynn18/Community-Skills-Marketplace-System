package controller

import javafx.fxml.FXML
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.{ReadOnlyStringWrapper, ReadOnlyObjectWrapper}
import scalafx.scene.control.{Button, Label, TextField, Alert, ComboBox, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.VBox
import service.MarketplaceManager
import model.Member

import scala.annotation.nowarn

/**
 * Controller class coordinating all interaction logic between the Member Management FXML view
 * and the MarketplaceManager database services.
 * Implements member searching, area filtering, profile CRUD operations, validation constraints,
 * and dynamically toggled UI layouts.
 */
@nowarn
class MemberManagementController:

  // ── FXML Injection Controls ────────────────────────────────────────────────
  // Injected text field to capture user keyword inputs for reactive search filtering
  @FXML private var txtSearch: javafx.scene.control.TextField = _
  
  // Injected button triggers clear filter logic resetting search terms and dropdown selectors
  @FXML private var btnClearSearch: javafx.scene.control.Button = _
  
  // Injected combobox filters the member list table by specific residential areas
  @FXML private var cmbAreaFilter: javafx.scene.control.ComboBox[String] = _

  // The main table UI listing filtered member profiles
  @FXML private var tblMembers: javafx.scene.control.TableView[Member] = _
  
  // Table view columns representing member attributes
  @FXML private var colId: javafx.scene.control.TableColumn[Member, String] = _
  @FXML private var colName: javafx.scene.control.TableColumn[Member, String] = _
  @FXML private var colContact: javafx.scene.control.TableColumn[Member, String] = _
  @FXML private var colArea: javafx.scene.control.TableColumn[Member, String] = _
  @FXML private var colCredits: javafx.scene.control.TableColumn[Member, java.lang.Integer] = _
  @FXML private var colRegDate: javafx.scene.control.TableColumn[Member, String] = _

  // KPI card labels displaying aggregated counts at the top of the pane
  @FXML private var lblStatTotal: javafx.scene.control.Label = _
  @FXML private var lblStatActive: javafx.scene.control.Label = _
  @FXML private var lblStatCredits: javafx.scene.control.Label = _

  // Form subtitle changes depending on whether the card is in Create Mode or Edit Mode
  @FXML private var lblModeSubtitle: javafx.scene.control.Label = _
  
  // Displays either the next auto-generated member ID or the selected member's ID
  @FXML private var lblDetailId: javafx.scene.control.Label = _
  
  // Formats and shows either today's registration date or the member's original registration date
  @FXML private var lblDetailRegDate: javafx.scene.control.Label = _
  
  // Text input capturing the member's alphabetical name
  @FXML private var txtFormName: javafx.scene.control.TextField = _
  
  // Text input capturing the phone contact detail matching standard format rules
  @FXML private var txtFormContact: javafx.scene.control.TextField = _
  
  // Combobox listing predefined community residential areas
  @FXML private var cmbFormArea: javafx.scene.control.ComboBox[String] = _
  
  // Numeric text field setting or displaying credit balances
  @FXML private var txtFormCredits: javafx.scene.control.TextField = _

  // Layout container enclosing buttons visible only in Register/Create Mode
  @FXML private var vboxCreateButtons: javafx.scene.layout.VBox = _
  
  // Layout container enclosing buttons visible only in Save/Edit Mode
  @FXML private var vboxEditButtons: javafx.scene.layout.VBox = _

  // ── ScalaFX Wrapper Components ──────────────────────────────────────────────
  // Wrappers bind JavaFX elements enabling clean Scala DSL configurations 
  // and property listeners
  private var sfTxtSearch: TextField = _
  private var sfBtnClearSearch: Button = _
  private var sfCmbAreaFilter: ComboBox[String] = _

  private var sfLblStatTotal: Label = _
  private var sfLblStatActive: Label = _
  private var sfLblStatCredits: Label = _

  private var sfLblModeSubtitle: Label = _
  private var sfLblDetailId: Label = _
  private var sfLblDetailRegDate: Label = _
  private var sfTxtFormName: TextField = _
  private var sfTxtFormContact: TextField = _
  private var sfCmbFormArea: ComboBox[String] = _
  private var sfTxtFormCredits: TextField = _

  private var sfVboxCreateButtons: VBox = _
  private var sfVboxEditButtons: VBox = _

  // ── Service & Data Fields ──────────────────────────────────────────────────
  private var manager: MarketplaceManager = _
  private var allMembers: Seq[Member] = Seq.empty
  private var shellController: ShellController = _

  // Predefined community residential zones
  private val predefinedAreas = Seq("Taman Tyng", "Taman Segah", 
    "Taman Mawar", "Taman Grandview", "Taman Perdana")

  /**
   * Sets reference to the main shell controller coordinator.
   */
  def setShellController(sc: ShellController): Unit =
    // Save reference to the parent coordinator controller.
    this.shellController = sc

  /**
   * Automatically called by JavaFX after FXML nodes are completely loaded and injected.
   * Performs ScalaFX wrapper creations, column factories mapping, and listener registrations.
   */
  @FXML
  def initialize(): Unit =
    // Instantiate ScalaFX wrappers using the raw JavaFX FXML objects.
    sfTxtSearch = new TextField(txtSearch)
    sfBtnClearSearch = new Button(btnClearSearch)
    sfCmbAreaFilter = new ComboBox[String](cmbAreaFilter)

    sfLblStatTotal = new Label(lblStatTotal)
    sfLblStatActive = new Label(lblStatActive)
    sfLblStatCredits = new Label(lblStatCredits)

    sfLblModeSubtitle = new Label(lblModeSubtitle)
    sfLblDetailId = new Label(lblDetailId)
    sfLblDetailRegDate = new Label(lblDetailRegDate)
    sfTxtFormName = new TextField(txtFormName)
    sfTxtFormContact = new TextField(txtFormContact)
    sfCmbFormArea = new ComboBox[String](cmbFormArea)
    sfTxtFormCredits = new TextField(txtFormCredits)

    sfVboxCreateButtons = new VBox(vboxCreateButtons)
    sfVboxEditButtons = new VBox(vboxEditButtons)

    // Setup column value mappings for TableView (formats raw ID to M00X representation)
    colId.cellValueFactory = { features => ReadOnlyStringWrapper(f"M${features.value.id.getOrElse(0)}%03d") }
    colName.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.name) }
    colContact.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.contact) }
    colArea.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.area) }
    colCredits.cellValueFactory = { features => ReadOnlyObjectWrapper[java.lang.Integer](features.value.creditBalance) }
    colRegDate.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.registrationDate) }

    // Listen for member table selection changes to update the detailed form card
    tblMembers.selectionModel.value.selectedItemProperty().addListener: (_, _, newVal) =>
      showMemberDetails(newVal)

    // Setup search listener to trigger filter updates reactively when user types keywords
    sfTxtSearch.textProperty().addListener: (_, _, newVal) =>
      filterMembers(newVal, sfCmbAreaFilter.value.value)

    // Setup dropdown filter listener to trigger updates when a residential zone is selected
    sfCmbAreaFilter.valueProperty().addListener: (_, _, newVal) =>
      filterMembers(sfTxtSearch.text.value, newVal)

    // Bind predefined areas to the detailed form dropdown choice list
    val formAreaBuffer = new ObservableBuffer[String]()
    formAreaBuffer ++= predefinedAreas
    sfCmbFormArea.items = formAreaBuffer

    // Set default initial view state to Create/Register Mode
    setEditMode(false)
    showMemberDetails(null)
  end initialize

  /**
   * Connects the MarketplaceManager service instance to fetch database records.
   */
  def setMarketplaceManager(manager: MarketplaceManager): Unit =
    // Save database connection instance locally.
    this.manager = manager
    // Load members from database.
    loadMembers()
    // Retrieve next ID now that manager service is available.
    showMemberDetails(null)
  end setMarketplaceManager

  /**
   * Queries the database for all member profiles, sorts them,
   * compiles statistics, and initializes filters.
   */
  def loadMembers(): Unit =
    // Stop if manager connection is not set.
    if manager == null then return

    // Retrieve all registered members from the database.
    val list = manager.getAllMembers()
    // Sort in reverse order of member ID so recently registered members appear at the top of the table
    allMembers = list.sortBy(_.id.getOrElse(0))(Ordering[Int].reverse)

    // Populate search filter dropdown with unique residential areas present in the database
    val areas = Seq("All Areas") ++ allMembers.map(_.area).distinct.sorted
    sfCmbAreaFilter.items = ObservableBuffer.from(areas)
    sfCmbAreaFilter.value = "All Areas"

    // Calculate and update header KPI statistics counts.
    sfLblStatTotal.text = allMembers.size.toString
    sfLblStatActive.text = allMembers.count(_.creditBalance > 0).toString
    sfLblStatCredits.text = allMembers.map(_.creditBalance).sum.toString

    // Run active filters.
    filterMembers(sfTxtSearch.text.value, sfCmbAreaFilter.value.value)
  end loadMembers

  /**
   * Filters the TableView in real-time based on query text matches and selected area.
   */
  // defines a private helper function named filterMembers that
  // takes two inputs (the search query text and the residential area name) to filter our member table.
  private def filterMembers(query: String, area: String): Unit =
    // Get query string or default to empty string, removes extra spaces, and converts it to lowercase letters
    val q = Option(query).getOrElse("").trim.toLowerCase
    // Filter the members list.
    // filter loop that scans through our complete list of members one-by-one to check if they match our search criteria.
    val filtered = allMembers.filter: m =>
      // Check if keyword matches name, phone contact, or formatted ID (e.g., M003)
      // returning true if the member's name contains the search text,
      // OR if their phone contact number contains the search text, OR if their formatted member ID (like "M003") contains the search text
      val matchesSearch = m.name.toLowerCase.contains(q) ||
        m.contact.toLowerCase.contains(q) ||
        f"M${m.id.getOrElse(0)}%03d".toLowerCase.contains(q)

      // Check if matches the selected residential area filter
      // returning true if no area is specified, OR if the user selected "All Areas", OR if the member's residential area matches the selected area
      val matchesArea = area == null || area == "All Areas" || m.area.equalsIgnoreCase(area)

      // returns true if member matches both the search text criteria AND the residential area criteria
      matchesSearch && matchesArea
    // takes the filtered list of members, packages them into a reactive list container, and assigns it to the table widget on the screen
    tblMembers.items = ObservableBuffer.from(filtered)
  end filterMembers

  /**
   * Resets all search entries, resets filter dropdowns, and restores full table view.
   */
  @FXML
  // defines the action handler function named handleClearFilters
  // that runs when the user clicks the "Clear Filters" button on the screen.
  def handleClearFilters(): Unit =
    // Clear search text box.
    sfTxtSearch.text = ""
    // Reset area filter selection.
    sfCmbAreaFilter.value = "All Areas"
    // Restore full table entries.
    filterMembers("", "All Areas")
  end handleClearFilters

  /**
   * Toggles button layouts and visibility between Create/Register Mode and Save/Edit Mode.
   */
  // defines a private helper function named setEditMode
  // that takes a true/false condition (represented by isEdit) to determine which buttons to display on the form.
  private def setEditMode(isEdit: Boolean): Unit = {
    // checks if the edit condition is true, meaning the user clicked an existing member to edit their profile.
    if isEdit then
      // Hide register buttons and display edit buttons.
      sfVboxCreateButtons.visible = false //invisible
      sfVboxCreateButtons.managed = false //removes the hidden buttons from the layout
      sfVboxEditButtons.visible = true
      sfVboxEditButtons.managed = true
    else
      // Display register buttons and hide edit buttons.
      sfVboxCreateButtons.visible = true
      sfVboxCreateButtons.managed = true
      sfVboxEditButtons.visible = false
      sfVboxEditButtons.managed = false
  }
  end setEditMode

  /**
   * Populates the detail form card input fields with details of the selected member.
   * If parameter is null, it resets the fields and enters Create Mode.
   */
  private def showMemberDetails(member: Member): Unit =
    if member == null then
      // Switch view mode layout.
      setEditMode(false)
      sfLblModeSubtitle.text = "REGISTER NEW MEMBER"
      
      // Auto-compute next ID in sequence
      val nextId = if manager != null then manager.getNextMemberId() else "M001"
      sfLblDetailId.text = nextId
      
      // Get formatted registration date.
      val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
      sfLblDetailRegDate.text = today
      
      // Reset form fields.
      sfTxtFormName.text = ""
      sfTxtFormContact.text = ""
      sfCmbFormArea.value = null
      sfTxtFormCredits.text = ""
      sfTxtFormCredits.disable = false // Allow inputting initial credit balance
    else
      // Switch view mode layout.
      setEditMode(true)
      sfLblModeSubtitle.text = "EDIT MEMBER PROFILE"
      
      // Load details of the selected member.
      sfLblDetailId.text = f"M${member.id.getOrElse(0)}%03d"
      sfLblDetailRegDate.text = member.registrationDate
      
      sfTxtFormName.text = member.name
      sfTxtFormContact.text = member.contact
      sfCmbFormArea.value = member.area
      sfTxtFormCredits.text = member.creditBalance.toString
      sfTxtFormCredits.disable = false // Allow updating credit balance
  end showMemberDetails

  /**
   * Validates form inputs, displaying clear error alerts on syntax or formatting mismatches.
   * Returns a tuple containing parsed fields if validation succeeds.
   */
  private def validateInputs(): Option[(String, String, String, Int)] =
    val name = sfTxtFormName.text.value.trim
    val contact = sfTxtFormContact.text.value.trim
    val area = sfCmbFormArea.value.value
    val creditsStr = sfTxtFormCredits.text.value.trim
    
    // ai-assisted: log-#7
    // why: why: Generated regex validation logic and safe Option 
    // return type to handle invalid form inputs without application crashes
    if name.isEmpty then
      showErrorAlert("Validation Error", "Missing Field", "Please enter a member name.")
      None
    // Ensure name contains only alphabets and spaces
    else if !name.matches("^[a-zA-Z\\s]+$") then
      showErrorAlert("Validation Error", "Invalid Format", "Member name must contain only alphabets and spaces.")
      None
    else if contact.isEmpty then
      showErrorAlert("Validation Error", "Missing Field", "Please enter a contact number.")
      None
    // Validate phone contact matches Malaysian +60 format followed by 9-12 digits
    else if !contact.matches("^\\+60\\d{9,12}$") then
      showErrorAlert("Validation Error", "Invalid Format", "Phone number must start with +60 followed by 9 to 12 digits.")
      None
    else if area == null || area.trim.isEmpty then
      showErrorAlert("Validation Error", "Missing Field", "Please select a residential area.")
      None
    else if creditsStr.isEmpty then
      showErrorAlert("Validation Error", "Missing Field", "Please enter initial credit balance.")
      None
    // Validate credit balance is a positive integer
    else if !creditsStr.matches("^\\d+$") then
      showErrorAlert("Validation Error", "Invalid Format", "Credit balance must be positive digits start from 0.")
      None
    else
      try
        val credits = creditsStr.toInt
        Some((name, contact, area, credits))
      catch
        case _: NumberFormatException =>
          showErrorAlert("Validation Error", "Type Mismatch", "Credits must be a valid integer number.")
          None
  end validateInputs

  /**
   * Action handler to switch form card layout to new member registration mode.
   */
  @FXML
  def handleNewForm(): Unit =
    //clears the currently selected member from the Members TableView
    tblMembers.selectionModel.value.clearSelection()
    // resets the member details form because no existing member is being edited.
    showMemberDetails(null)
  end handleNewForm

  /**
   * Action handler to clear selection and reset details form fields.
   */
  @FXML
  def handleClearForm(): Unit =
    // clears the currently selected member from the Members TableView
    tblMembers.selectionModel.value.clearSelection()
    // resets the member details form because no existing member is being edited.
    showMemberDetails(null)
  end handleClearForm

  /**
   * Action handler to insert a new member into the database after validation checks succeed.
   */
  @FXML
  def handleCreate(): Unit = {
    // triggers the input validation checker function,
    // matching to check whether the validation succeeded or failed.
    validateInputs() match
      //checks if the validation succeeded, which wraps the approved user inputs
      // (name, phone number, residential area, and credits) inside a Some container.
      case Some((name, contact, area, credits)) =>
        // Construct new Member object using the validated form details,
        // leaving the ID parameter as None because the database will
        // assign a unique ID automatically.
        val member = Member(None, name, contact, area, credits)
        //checks to ensure that database manager coordinator is connected and available
        if manager != null then
          // sends the new member object to the database manager service to insert it into the SQL database tables.
          manager.addMember(member)
          // Refresh screen, loading the updated db records to show new member in list
          loadMembers()
          // clear any selected rows in member table and stay in Register Mode
          tblMembers.selectionModel.value.clearSelection()
          // resets the details form input boxes to be empty for another registration.
          showMemberDetails(null)
          // pop-up window to alert the user that the registration succeeded
          showInfoAlert("Success", "Member Registered", "Successfully registered new member!")
      // the validation fails, stops execution
      case None =>
  }
  end handleCreate

  /**
   * Action handler to update and save changes to an existing member profile.
   */
  @FXML
  //  defines a function named handleSave that runs when the user clicks the "Save" button to update a member's profile
  def handleSave(): Unit =
    // looks at the member table on the screen and fetches the member profile
    // that is currently clicked or highlighted, saving it in a variable named selected
    val selected = tblMembers.selectionModel.value.getSelectedItem
    // checks to make sure that the user has actually selected a member from the list,
    // before attempting to save, preventing errors
    if selected != null then
      // triggers the input validation checker function,
      // matching to check whether the validation succeeded or failed.
      validateInputs() match
        //checks if the validation succeeded, which wraps the approved user inputs
        // (name, phone number, residential area, and credits) inside a Some container.
        case Some((name, contact, area, credits)) =>
          // Construct updated Member copy.
          // creates a new copy of the selected member's profile, replacing the old details with the newly entered form values while keeping the same ID.
          val updated = selected.copy(name = name, contact = contact, area = area, creditBalance = credits)
          //checks to ensure that the database manager is connected.
          if manager != null then
            // sends the updated member profile details to the database
            manager.updateMember(updated)
            // reloads the member list table to show the updated information.
            loadMembers()
            
            // searches the refreshed list of members to find the member profile we just updated.
            val reselect = allMembers.find(_.id == selected.id)
            //  auto highlights the updated member in the table list again
            //  so the user doesn't lose their selection.
            reselect.foreach(tblMembers.selectionModel.value.select)
            //pop up alert box - edits were successfully saved.
            showInfoAlert("Success", "Changes Saved", "Member profile updated successfully.")
        // does nothing if validation checks fail, stopping execution since the validator has already displayed error messages.
        case None =>
  end handleSave

  /**
   * Action handler to confirm and delete the selected member from the system.
   */
  @FXML
  // defines the action handler function named handleDelete linked to the "Delete" button.
  def handleDelete(): Unit =
    // retrieves the member profile currently highlighted in the table list and stores it in selected
    val selected = tblMembers.selectionModel.value.getSelectedItem
    // ensures that a member is selected before trying to delete anything.
    if selected != null then
      // Show confirmation alert dialog.
      val alert = new Alert(AlertType.Confirmation):
        title = "Confirm Delete"
        headerText = "Delete Member"
        contentText = s"Are you sure you want to delete member '${selected.name}'?"

      // displays the confirmation window on the screen and
      // pauses execution until the user clicks either "OK" or "Cancel".
      val result = alert.showAndWait()
      // If user click OK button, proceed with deletion.
      if result.isDefined && result.get == ButtonType.OK then {
        // verifies that the database manager is connected.
        if manager != null then
          // tells the database manager to delete the member record matching the selected member's ID.
          manager.deleteMember(selected.id.getOrElse(0))
          // refreshes the table list
          loadMembers()
          // resets the details form card to register mode, clearing the input boxes.
          showMemberDetails(null)
          // pop-up window
          showInfoAlert("Success", "Member Deleted", "Member successfully removed.")
      }
  end handleDelete

  /**
   * Presents an error dialog.
   */
  // defines a private helper function named showErrorAlert that accepts
  // three text strings (for the window title, header message, and detailed description content)
  // and returns no output value.
  private def showErrorAlert(titleVal: String, headerVal: String, contentVal: String): Unit =
    // creates a new JavaFX/ScalaFX alert dialog box configured with an "Error" theme
    val alert = new Alert(AlertType.Error):
      title = titleVal
      headerText = headerVal
      contentText = contentVal
    // displays the completed error window on the screen and
    // pauses the program's user interface execution until the user clicks the "OK" button to close it.
    alert.showAndWait()
  end showErrorAlert

  /**
   * Presents an information dialog.
   */
  // defines a private helper function named showInfoAlert that accepts
  // three text strings (title, header, and description) to show successful status messages.
  private def showInfoAlert(titleVal: String, headerVal: String, contentVal: String): Unit =
    val alert = new Alert(AlertType.Information):
      title = titleVal
      headerText = headerVal
      contentText = contentVal
    alert.showAndWait()
  end showInfoAlert
end MemberManagementController