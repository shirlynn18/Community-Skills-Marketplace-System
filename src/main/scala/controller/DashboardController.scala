package controller

import javafx.fxml.FXML
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.{ReadOnlyStringWrapper, ReadOnlyObjectWrapper}
import scalafx.scene.control.{Label, Button}
import scalafx.scene.layout.{VBox, HBox, Region, Priority}
import scalafx.geometry.Pos
import service.MarketplaceManager

/**
 * Data representation for rows displayed in the Completed Transactions table on the Dashboard.
 */
// holds the details for one row in the completed exchanges table shown on the dashboard.
case class CompletedTransactionRow(
  idStr: String,
  date: String,
  providerName: String,
  receiverName: String,
  skillName: String,
  credits: Int
)

import scala.annotation.nowarn

/**
 * DashboardController binds to DashboardView.fxml and manages the display of
 * system statistics, custom horizontal stacked bar charts, and recent activity logs.
 */
@nowarn
class DashboardController:

  // FXML-injected JavaFX controls representing UI nodes defined in FXML layout
  @FXML private var lblHeaderDate: javafx.scene.control.Label = _
  
  // Row 1 KPI Card Labels
  @FXML private var lblTotalMembers: javafx.scene.control.Label = _
  @FXML private var lblOpenPosts: javafx.scene.control.Label = _
  @FXML private var lblOpenPostsSub: javafx.scene.control.Label = _
  @FXML private var lblTotalExchanged: javafx.scene.control.Label = _
  @FXML private var lblExchangedSub: javafx.scene.control.Label = _
  @FXML private var lblCompletedExchanges: javafx.scene.control.Label = _
  @FXML private var lblOverviewSub: javafx.scene.control.Label = _

  // Custom layout components for chart and activity feed
  @FXML private var vboxSkillsChart: javafx.scene.layout.VBox = _
  @FXML private var vboxActivityFeed: javafx.scene.layout.VBox = _
  @FXML private var btnViewAllTransactions: javafx.scene.control.Button = _

  // Row 4 Completed Transactions Table
  @FXML private var tblCompletedTransactions: javafx.scene.control.TableView[CompletedTransactionRow] = _
  @FXML private var colCtId: javafx.scene.control.TableColumn[CompletedTransactionRow, String] = _
  @FXML private var colCtDate: javafx.scene.control.TableColumn[CompletedTransactionRow, String] = _
  @FXML private var colCtProvider: javafx.scene.control.TableColumn[CompletedTransactionRow, String] = _
  @FXML private var colCtReceiver: javafx.scene.control.TableColumn[CompletedTransactionRow, String] = _
  @FXML private var colCtSkill: javafx.scene.control.TableColumn[CompletedTransactionRow, String] = _
  @FXML private var colCtCredits: javafx.scene.control.TableColumn[CompletedTransactionRow, java.lang.Integer] = _

  // ScalaFX wrappers matching JavaFX FXML injected controls for clean DSL usage
  private var sfLblHeaderDate: Label = _
  private var sfLblTotalMembers: Label = _
  private var sfLblOpenPosts: Label = _
  private var sfLblOpenPostsSub: Label = _
  private var sfLblTotalExchanged: Label = _
  private var sfLblExchangedSub: Label = _
  private var sfLblCompletedExchanges: Label = _
  private var sfLblOverviewSub: Label = _
  private var sfVboxSkillsChart: VBox = _
  private var sfVboxActivityFeed: VBox = _
  private var sfBtnViewAllTransactions: Button = _

  // Core service layer and parent controller references
  private var manager: MarketplaceManager = _
  private var shellController: ShellController = _

  /**
   * Sets reference to the parent ShellController to support cross-tab navigation.
   */
  def setShellController(sc: ShellController): Unit =
    this.shellController = sc

  /**
   * Automatically called by JavaFX FXMLLoader after FXML components are injected.
   * Performs wrapper initialization, date formatting, table column mappings, and event registration.
   */
  @FXML
  def initialize(): Unit =
    // Initialize ScalaFX wrappers using raw JavaFX elements
    sfLblHeaderDate = new Label(lblHeaderDate)
    sfLblTotalMembers = new Label(lblTotalMembers)
    sfLblOpenPosts = new Label(lblOpenPosts)
    sfLblOpenPostsSub = new Label(lblOpenPostsSub)
    sfLblTotalExchanged = new Label(lblTotalExchanged)
    sfLblExchangedSub = new Label(lblExchangedSub)
    sfLblCompletedExchanges = new Label(lblCompletedExchanges)
    sfLblOverviewSub = new Label(lblOverviewSub)
    sfVboxSkillsChart = new VBox(vboxSkillsChart)
    sfVboxActivityFeed = new VBox(vboxActivityFeed)
    sfBtnViewAllTransactions = new Button(btnViewAllTransactions)

    // Set current date and time in header format: 9/8/2026 (Sun), 11:44 AM
    val now = java.time.LocalDateTime.now() //assign current system date time
    // template for time date
    val formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy (E), h:mm a")
    sfLblHeaderDate.text = now.format(formatter) //reshape current time into readable text using template

    // Set cell value factories for Completed Transactions mapping model columns to TableView cells
    // Credit Transaction ID column in table view. determine what to show in every cell of this column
    // {takes one row at a time in tableView => wraps raw id text (access object and retrieve transaction ID from object)
    colCtId.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.idStr) }
    colCtDate.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.date) }
    colCtProvider.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.providerName) }
    colCtReceiver.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.receiverName) }
    colCtSkill.cellValueFactory = { features => ReadOnlyStringWrapper(features.value.skillName) }
    colCtCredits.cellValueFactory = { features => ReadOnlyObjectWrapper[java.lang.Integer](features.value.credits) }

    // Register click event handler to navigate to Service Exchange tab on click
    sfBtnViewAllTransactions.onAction = _ =>
      // if main navigation manager is not empty then switch to service exchange tab
      // without preselecting any offer, request
      if shellController != null then shellController.navigateToBooking(None, None)
  end initialize

  /**
   * Injects the shared MarketplaceManager service instance and loads dashboard data.
   */
  // defines a method called setMarketplaceManager that receives a MarketplaceManager object
  // and performs some actions without returning any value.
  def setMarketplaceManager(manager: MarketplaceManager): Unit =
    this.manager = manager // stores the received MarketplaceManager object inside the controller
    loadDashboardData() // triggers screen update (nums, stats, logs)
  end setMarketplaceManager

  /**
   * Queries the service manager to retrieve live stats, computes success rates, builds
   * custom horizontal stacked bar charts, and populates recent activity logs and transactions.
   */
  def loadDashboardData(): Unit =
    if manager != null then
      // == 1. Update KPI Card content (4): total members, total open posts, completed exchanges, credit exchanged==
      //  1. Fetch count and credit stats from the database
      val totalOffers       = manager.getTotalSkillOffers() //ask manager to count all active skill offers, store in totalOffers
      val totalRequests     = manager.getTotalSkillRequests()
      val completedExchanges = manager.getTotalCompletedExchanges()
      val totalExchanges    = manager.getAllExchanges().size //get list all matches (P,A,C,C) from db, counts how many in total

      // 2. Card 1: Update Total Registered Members label
      sfLblTotalMembers.text    = manager.getTotalMembers().toString //convert num to text string

      // 3. Card 2: Update Active Posts label (sum of active offers and requests)
      sfLblOpenPosts.text    = (totalOffers + totalRequests).toString //convert the sum to text
      sfLblOpenPostsSub.text = s"$totalOffers offers \u00b7 $totalRequests requests" // eg 8 offers · 5 requests

      // 4. Card 3: Update Total Credits Exchanged & Calculate Average credits per exchange
      val totalCredits = manager.getTotalCreditsExchanged() //get sum of all credits points traded across completed exchanges
      // prevent division-by-zero, divide total credit points by the count of completed exchanged, if no complete exchange, avg sets zero
      val avgCredits   = if completedExchanges > 0 then totalCredits / completedExchanges else 0
      sfLblTotalExchanged.text = totalCredits.toString // convert total points traded into text
      sfLblExchangedSub.text   = s"Avg $avgCredits per Exchange"

      // 5. Card 4: Update Completed Match Count & Calculate Success Rate percentage
      val rate = if totalExchanges > 0 then (completedExchanges.toDouble / totalExchanges * 100).toInt else 0
      sfLblCompletedExchanges.text = completedExchanges.toString
      sfLblOverviewSub.text        = s"$rate% Success Rate"

      // Helper function to classify skill names into distinct categories
      def getCategory(skillName: String): String =
        val name = skillName.toLowerCase
        if name.contains("tutor") || name.contains("scala") || name.contains("programming") || name.contains("lesson") || name.contains("guitar") || name.contains("teach") then
          "Tutoring"
        else if name.contains("repair") || name.contains("paint") || name.contains("house") || name.contains("fix") || name.contains("plumb") then
          "Repairs"
        else if name.contains("child") || name.contains("babysit") || name.contains("care") then
          "Childcare"
        else
          "Other"
      end getCategory

      // == 2. Skill distribution - horizontal stacked bar chart ==
      // 1. Fetch all offers and requests
      val offers   = manager.getAllOffers()
      val requests = manager.getAllRequests()

      // create a fixed list categories, use them to group and count skill posts
      val categories = Seq("Tutoring", "Repairs", "Childcare", "Other")

      // store the aggregated post count details for a category,
      // tracking the category name, the count of offers, the count of requests,
      // and the combined total offer and request.
      case class CategoryStat(name: String, 
                              offersCount: Int, 
                              requestsCount: Int, 
                              total: Int)
      
      // 2. Count occurrences of offers/requests for each category
      val stats = categories.map { cat => // process each category one at time to calculate its stats
        // list offers.count offer posts satisfy condition (one offer => finds the offer category
        // == checks whether the offer belongs to the current category)
        // counts how many skill offer posts belong to the current category.
        val oCount = offers.count(o => getCategory(o.skillName) == cat)
        // request ''
        val rCount = requests.count(r => getCategory(r.skillName) == cat)
        // cat name, offer count, request count , total count
        CategoryStat(cat, oCount, rCount, oCount + rCount)
        //remove any categories with zero posts.sorts remaining categories by total count in desc order
      }.filter(_.total > 0).sortBy(_.total)(Ordering[Int].reverse)

      // 3. Clear container and draw chart legend (yellow dot = Offers, purple dot = Requests)
      sfVboxSkillsChart.getChildren.clear()

      // Build and add the chart legend row (dots and text labels)
      val legendBox = new HBox: //instantiates new horizontal layout box called legend box align chart legend side-by-side
        spacing = 15 // spacing gap of 15 pixels
        // vertical alignment inside the legend box to center the items vertically
        // and snap them to the left side horizontally
        alignment = Pos.CenterLeft
        style = "-fx-padding: 0 0 5 0;" //padding margin of 5 pixels at legend box bottom

      val offerDot = new Region:
        style = "-fx-background-color: -yellow; -fx-background-radius: 3px; -fx-min-width: 12px; -fx-min-height: 12px; -fx-max-width: 12px; -fx-max-height: 12px;"
      val offerLbl = new Label("Offers"):
        style = "-fx-text-fill: -ink-soft; -fx-font-size: 11px; -fx-font-weight: bold;"

      val requestDot = new Region:
        style = "-fx-background-color: -purple; -fx-background-radius: 3px; -fx-min-width: 12px; -fx-min-height: 12px; -fx-max-width: 12px; -fx-max-height: 12px;"
      val requestLbl = new Label("Requests"):
        style = "-fx-text-fill: -ink-soft; -fx-font-size: 11px; -fx-font-weight: bold;"

      // add yellow dot, offer label, purple dot, request label to horizontal legend box
      // getChildren: gets the list of UI components currently inside the VBox.
      legendBox.getChildren.addAll(offerDot, offerLbl, requestDot, requestLbl)
      //adds the completed legendBox into the Skills Chart VBox, so the legend is displayed together with the chart.
      sfVboxSkillsChart.getChildren.add(legendBox)

      // 4. Find category with the largest total posts num to use as 100% width baseline
      // default to 1 if no posts exist
      val maxTotal = stats.map(_.total).maxOption.getOrElse(1)

      // 5. Build proportional bars dynamically for each category
      stats.foreach { stat => //loop runs for each category in stats list to draw its bar on the chart
        // creates a vertical box container to stack the category's main bar row and its breakdown description
        val categoryVBox = new VBox:
          spacing = 4 // 4 pixels gap between name/bar row and details row below it
        //category layout expand vertically to fill the available height in the chart area.
        VBox.setVgrow(categoryVBox, Priority.Always)

        // Row components: Category Label, Bar, and Total Count
        val line1 = new HBox: // creates a horizontal row container for the category name and its bar wrapper.
          spacing = 10 //  10 pixels gap between items inside this row
          alignment = Pos.CenterLeft //align all items vertically in the center and pushes them to the left.
        HBox.setHgrow(line1, Priority.Always) //permits the row to expand horizontally as the window gets wider.

        //creates a text label displaying the category name (like "Tutoring")
        val nameLabel = new Label(stat.name):
          style = "-fx-text-fill: -ink; -fx-font-size: 12px; -fx-font-weight: bold; -fx-min-width: 90px; -fx-max-width: 90px; -fx-pref-width: 90px;"

        val barWrapper = new HBox: //creates a horizontal container to align the colored bar and the total number label side-by-side
          alignment = Pos.CenterLeft //aligns the bar and the number vertically in the center.
        HBox.setHgrow(barWrapper, Priority.Always) //allows the bar wrapper to expand horizontally.

        // Outer container for the category bar
        val barContainer = new HBox: //creates a horizontal container that will hold our colored bar segments.
          alignment = Pos.CenterLeft //aligns the bar segments to start on the left.
          style = "-fx-background-radius: 4px; -fx-background-color: transparent;"

        // Bind the bar's width proportionally to the max category size
        // ai-assisted: log-#3
        // why: Used AI to improve proportional bar chart rendering logic using ScalaFX property binding.
        val barWidthRatio = stat.total.toDouble / maxTotal //calculates how wide current category's bar should be relative to the largest bar in the chart (cat has most post)
        //ratio to set the bar's preferred width, bar can use up to 70% of the available width, and bind makes the bar resize automatically when the wrapper's width changes.
        barContainer.prefWidthProperty().bind(barWrapper.widthProperty().multiply(0.7 * barWidthRatio))

        // Append yellow segment for Offer if any exists
        // checks if the category contains any offer post
        if stat.offersCount > 0 then
          //creates a graphical block representing the offer portion of the bar.
          val offerSegment = new Region:
            style = "-fx-background-color: -yellow; -fx-min-height: 16px; -fx-max-height: 16px; -fx-pref-height: 16px;"
          //calculates the percentage of offers inside this category.
          val ratio = stat.offersCount.toDouble / stat.total
          //binds the yellow block's width to its percentage share of the bar.
          offerSegment.prefWidthProperty().bind(barContainer.prefWidthProperty().multiply(ratio))
          //appends the yellow segment into our bar container.
          barContainer.getChildren.add(offerSegment)

        // Append purple segment for Request if any exists
        // checks if the category contains any request posts.
        if stat.requestsCount > 0 then
          //creates a graphical block representing the request portion of the bar.
          val reqSegment = new Region:
            style = "-fx-background-color: -purple; -fx-min-height: 16px; -fx-max-height: 16px; -fx-pref-height: 16px;"
          // calculates the percentage of requests inside this category.
          val ratio = stat.requestsCount.toDouble / stat.total
          // binds the purple block's width to its percentage share of the bar.
          reqSegment.prefWidthProperty().bind(barContainer.prefWidthProperty().multiply(ratio))
          //appends the purple segment into our bar container.
          barContainer.getChildren.add(reqSegment)

        //Finalize category row
        // creates a text label showing the combined total count of posts.
        val totalLabel = new Label(s"   ${stat.total}"):
          style = "-fx-text-fill: -ink; -fx-font-size: 12px; -fx-font-weight: bold;"

        //places the completed bar and the total number label into the wrapper.
        barWrapper.getChildren.addAll(barContainer, totalLabel)
        // places the category name label and the bar wrapper into the main row layout.
        line1.getChildren.addAll(nameLabel, barWrapper)

        // Detail label: Displays specific O (Offer) vs R (Request) counts
        // Breakdown label (e.g., "O: 3 | R: 1")
        //creates a second horizontal row container to hold the detailed text breakdown.
        val line2 = new HBox:
          spacing = 10
          alignment = Pos.CenterLeft

        // creates an invisible block exactly 90 pixels wide to push the details text to line up with the bar, bypassing the category name label.
        val spacer = new Region:
          style = "-fx-min-width: 90px; -fx-max-width: 90px; -fx-pref-width: 90px;"

        //creates a text label showing the breakdown details (like "O: 3 | R: 1")
        val breakdownLabel = new Label(s"O: ${stat.offersCount} | R: ${stat.requestsCount}"):
          style = "-fx-text-fill: -ink-soft; -fx-font-size: 11px;"

        //places the invisible spacer and the details label into the second row
        line2.getChildren.addAll(spacer, breakdownLabel)

        // stacks both horizontal rows (the bar row and the breakdown row) into the category box.
        categoryVBox.getChildren.addAll(line1, line2)
        // appends the finished category box onto the screen chart container.
        sfVboxSkillsChart.getChildren.add(categoryVBox)
      }

      // 3. Clear and construct recent Activity Feed entries
      // clears out any old activity log entries from the scrolling list on the dashboard.
      sfVboxActivityFeed.getChildren.clear()

      // formats today's current date as a text string (like "13-08-2026") to use for date comparisons
      val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))

      // defines a helper function named parseDate that takes a date text string and converts it into a calendar date object.
      def parseDate(dStr: String): java.time.LocalDate =
        // attempts to parse the date text using the "dd-MM-yyyy" pattern.
        scala.util.Try(java.time.LocalDate.parse(dStr, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")))
          .getOrElse(java.time.LocalDate.now())
          // uses today's date as a fallback if the date text is formatted incorrectly
      end parseDate

      //  defines a simple data class structure to hold details for a single activity log row,
      //  tracking its calendar sort date, ID number, indicator dot color,
      //  description message, and date string.
      case class ActivityEvent(
        sortDate: java.time.LocalDate,
        idForTieBreak: Int, // decide the order when two activities have the same date.
        dotColor: String,
        description: String,
        dateStr: String
      )

      // == 3. Recent Highlight ===
      // 1. Fetch all logs from the database
      // queries the database manager to fetch all system activity logs
      val allLogs = manager.getAllActivityLogs()

      // Assign custom color codes depending on action type
      // defines a function to return a specific color code based on the log category.
      def getDotColor(logType: String): String = logType match
        case "New Member" => "-mustard" // yellow color code for new member registration logs.
        case "New Skill Offer" | "New Skill Request" => "-status-matched" // assigns a blue-green color code for new post logs.
        case _ => "-brick" // a brick-red color code for all other logs
      end getDotColor

      // Sort and capture the latest 5 entries
      // 2. Map and parse date strings into Date objects for sorting
      // loops through the fetched logs, converting each raw log database row into
      // formatted event data structure.
      val allEvents = allLogs.map { log =>
        ActivityEvent(
          sortDate = parseDate(log.dateStr), //converts the log's date string into a calendar date object.
          idForTieBreak = log.id.getOrElse(0), // extracts the log's database ID to use for sorting get the ID if it exists; otherwise use 0.
          dotColor = getDotColor(log.activityType), // get dot color based on the type of activity
          description = log.description, //extracts the activity description text message
          dateStr = log.dateStr //extracts the original formatted date string
        )
      }.sortBy(_.idForTieBreak)(Ordering[Int].reverse)  // 3. sorts the list of events by their database ID numbers in descending order so the newest logs appear first.
       .take(5)                                         // 4. Top 5 events

      // 5. Add each activity feed event row to the UI
      // loops through each of 5 latest events to draw them in the feed list.
      allEvents.foreach { event =>
        val row = new HBox:
          spacing = 10
          alignment = Pos.CenterLeft
          style = "-fx-padding: 5 8; -fx-background-color: -surface-alt; -fx-background-radius: 4px;"

        // Renders the colored bullet dot
        val dot = new Region:
          style = s"-fx-background-color: ${event.dotColor}; -fx-background-radius: 4px; -fx-min-width: 8px; -fx-min-height: 8px; -fx-max-width: 8px; -fx-max-height: 8px;"

        // Renders the text description
        val descLbl = new Label(event.description):
          style = "-fx-text-fill: -ink; -fx-font-size: 12px;"
        
        val spacer = new Region() //creates an invisible space region.
        //tells the space region to expand as much as possible, pushing the date label to the far right
        HBox.setHgrow(spacer, Priority.Always)

        // if the log happened today, displaying "Today" if it did,
        // or displaying the date string if it happened on a previous day.
        val displayTime = if event.dateStr == todayStr then "Today" else event.dateStr
        //creates a text label to show the formatted time/date details
        val timeLbl = new Label(displayTime):
          style = "-fx-text-fill: -ink-soft; -fx-font-size: 11px;"

        //adds the dot, the description, the expanding spacer, and the date label into the row container.
        row.getChildren.addAll(dot, descLbl, spacer, timeLbl)
        //appends the completed activity row onto the screen feed container.
        sfVboxActivityFeed.getChildren.add(row)
      }
      //checks if there are no activity events logged in the system.
      if allEvents.isEmpty then
        //creates a text label displaying a message - no history to show.
        val noActs = new Label("No recent activities recorded."):
          style = "-fx-text-fill: -ink-soft; -fx-font-style: italic;"
        //appends the empty message to the feed container on the screen.
        sfVboxActivityFeed.getChildren.add(noActs)

      // == 4. Load the Recent Completed Transactions list ==
      // 1. Fetch completed service exchanges from the database
      //queries the database manager for all service matches and filters them to keep only those marked as completed.
      val completedExs = manager.getAllExchanges().filter(_.status == model.ExchangeStatus.Completed)
      // 2. Sort by ID descending (newest first) and keep the top 5
      // sorts the completed matches by their database ID numbers in descending order and keeps only the top 5 newest rows.
      val recentCompleted = completedExs.sortBy(_.id)(Ordering[Option[Int]].reverse).take(5)

      // 3. Convert raw database exchanges into formatted completed row rows
      // loops through our 5 recent completed matches to convert them into rows suited for our dashboard table.
      val rows = recentCompleted.map { se =>
        //looks up the name of the provider/receiver member from their ID.
        val providerName = manager.getMemberName(se.providerMemberId)
        val receiverName = manager.getMemberName(se.receiverMemberId)

        // searches the active offers and requests list to find the skill post name
        // associated with this match, defaulting to "Skill Exchange" if not found.
        val skillName = manager.getAllOffers().find(_.id.contains(se.offerId)).map(_.skillName)
          .getOrElse(manager.getAllRequests().find(_.id.contains(se.requestId)).map(_.skillName).getOrElse("Skill Exchange"))

        // Format the credit transaction ID (CT00X)
        // finds the Credit Transaction related to the current Service Exchange
        // by searching for its ID in the transaction note.
        // If a matching transaction is found, it gets the transaction ID, formats as CT00X.
        // If no matching transaction/ID is found, it uses the Service Exchange ID as a fallback.
        val creditTxId = manager.getAllCreditTransactions()
          .find(_.note.contains(s"ID: ${se.id.getOrElse(0)}"))
          .flatMap(_.id)
          .map(txId => f"CT$txId%03d")
          .getOrElse(f"CT${se.id.getOrElse(0)}%03d")

        // packages the formatted transaction ID, date, provider name, receiver name,
        // skill name, and credits value into a new table row data object.
        CompletedTransactionRow(
          idStr = creditTxId,
          date = se.date,
          providerName = providerName,
          receiverName = receiverName,
          skillName = skillName,
          credits = se.credits
        )
      }

      // 4. Update the TableView items
      // a variable called buffer to store the completed transaction rows.
      // creates a special reactive list buffer named buffer that allows the table
      // to detect changes automatically.
      val buffer = new ObservableBuffer[CompletedTransactionRow]()
      //loads all our mapped table row data objects into the list buffer.
      buffer ++= rows
      // assigns the buffer to the Completed Transactions table widget on the screen,
      // so completed transaction data is displayed in the table.
      tblCompletedTransactions.items = buffer
  end loadDashboardData
end DashboardController
