package controller

import javafx.fxml.FXML
import scalafx.scene.control.{Alert, TabPane}
import scalafx.scene.control.Alert.AlertType
import service.MarketplaceManager
import scala.annotation.nowarn

/**
 * ShellController acts as the primary layout coordinator for the application.
 * Manages tab switching, FXML injection delegation, and the about information popups.
 */
//type-safe enum or navigation router (e.g. enum AppTab) to manage screen changes. 
// This would prevent the app from breaking if we rename or reorder the tabs.
@nowarn
class ShellController:

  // FXML-injected JavaFX root TabPane representing the main tab container of the application
  @FXML private var mainTabPane: javafx.scene.control.TabPane = _
  
  // ScalaFX wrapper matching the JavaFX TabPane for clean Scala DSL property access
  private var sfTabPane: TabPane = _

  // Nested child controllers injected automatically by FXMLLoader for each tab screen
  @FXML private var dashboardController: DashboardController = _
  @FXML private var memberManagementController: MemberManagementController = _
  @FXML private var skillManagementController: SkillManagementController = _
  @FXML private var serviceExchangeController: ServiceExchangeController = _

  // Local variable to store the shared database manager service instance
  private var manager: MarketplaceManager = _

  /**
   * Initializes the manager service across all child controllers and configures
   * the reactive selection listener on TabPane to reload data dynamically on tab switch.
   */
  def setMarketplaceManager(manager: MarketplaceManager): Unit =
    // Save the passed database manager reference locally.
    this.manager = manager
    // Wrap the raw JavaFX tab pane in a ScalaFX TabPane wrapper.
    sfTabPane = new TabPane(mainTabPane)
    
    // Pass the shellController reference and database manager to the Dashboard tab.
    if dashboardController != null then
      dashboardController.setShellController(this)
      dashboardController.setMarketplaceManager(manager)
    // Pass the shellController reference and database manager to the Member Management tab.
    if memberManagementController != null then
      memberManagementController.setShellController(this)
      memberManagementController.setMarketplaceManager(manager)
    // Pass the shellController reference and database manager to the Skill Post tab.
    if skillManagementController != null then
      skillManagementController.setShellController(this)
      skillManagementController.setMarketplaceManager(manager)
    // Pass the shellController reference and database manager to the Service Exchange tab.
    if serviceExchangeController != null then
      serviceExchangeController.setShellController(this)
      serviceExchangeController.setMarketplaceManager(manager)

    // Register a listener on the tab selection model to automatically refresh data when a tab is clicked.
    sfTabPane.selectionModel().selectedItemProperty().addListener: (_, _, newVal) =>
      if newVal != null then
        // Retrieve the text title of the selected tab.
        val title = newVal.getText
        // If the selected tab is Dashboard, reload the dashboard statistics and charts.
        if title.startsWith("Dashboard") then
          if dashboardController != null then dashboardController.loadDashboardData()
        // If the selected tab is Member Management, reload the members list table.
        else if title.startsWith("Member Management") then
          if memberManagementController != null then memberManagementController.loadMembers()
        // If the selected tab is Skill Post, reload the offers and requests list table.
        else if title.startsWith("Skill Post") then
          if skillManagementController != null then skillManagementController.loadPosts()
        // If the selected tab is Service Exchange, reload the exchanges list and audit log tables.
        else if title.startsWith("Service Exchange") then
          if serviceExchangeController != null then serviceExchangeController.loadData()
  end setMarketplaceManager

  /**
   * Unused placeholder method for member tab count logic.
   */
  def updateMemberTabCount(): Unit =
    ()

  /**
   * Navigates the UI automatically to the booking tab and pre-selects the requested offer or request post.
   */
  def navigateToBooking(offerId: Option[Int], requestId: Option[Int]): Unit =
    if sfTabPane != null then
      // Switch the visible tab selection to the fourth tab (index 3, which is Service Exchange).
      sfTabPane.selectionModel().select(3)
    if serviceExchangeController != null then
      // Tell the service exchange tab to automatically pre-select the chosen offer or request.
      serviceExchangeController.selectOfferAndRequest(offerId, requestId)
  end navigateToBooking

  /**
   * Closes the application context on click.
   * Triggered when the user clicks the exit options in the menu bar.
   */
  @FXML
  def handleClose(): Unit =
    // Shutdown the program process immediately.
    sys.exit(0)

  /**
   * Shows the informational About dialog.
   * Triggered when the user clicks the about option in the menu bar.
   */
  @FXML
  def handleAbout(): Unit =
    // Build a new information alert pop-up window showing app details.
    val alert = new Alert(AlertType.Information):
      title = "About Community Skills Marketplace"
      headerText = "Community Skills Marketplace System"
      contentText =
        "Version 1.2.0\n" +
        "Developer: Shirlynn Chung Kai Qing\n\n" +
        "Built with Scala 3, ScalaFX 21, and FXML.\n" +
        "Apache Derby (Embedded) via ScalikeJDBC.\n" +
        "A system for managing members, skill posts, service exchanges, and credit transactions within a community skills marketplace.\n\n"
    // Display the alert window and wait for the user to close it.
    alert.showAndWait()
  end handleAbout
end ShellController
