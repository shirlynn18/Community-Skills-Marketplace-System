import controller.ShellController
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import model.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import service.MarketplaceManager

/**
 * Main entry point of the Community Skills Marketplace System.
 * Inherits from JFXApp3 to bootstrap the ScalaFX application lifecycle.
 */
object Main extends JFXApp3:
  override def start(): Unit =
    // 1. Initialize Derby DB Tables & Connection Pool (Embedded Database Setup)
    DatabaseInitializer.initialize()
    
    // Register JVM shutdown hook to cleanly close the Derby database connection pool on exit
    sys.addShutdownHook:
      DatabaseInitializer.shutdown()
    
    // 2. Instantiate Repositories (Data Access Layer)
    val memberRepo = new MemberRepository()
    val offerRepo = new SkillOfferRepository()
    val requestRepo = new SkillRequestRepository()
    val exchangeRepo = new ServiceExchangeRepository()
    val transactionRepo = new CreditTransactionRepository()
    
    // 3. Create MarketplaceManager (Service Layer coordinating database transacting and business logic)
    val manager = new MarketplaceManager(
      memberRepo,
      offerRepo,
      requestRepo,
      exchangeRepo,
      transactionRepo
    )
    
    // 4. Load FXML Shell View (Main Layout Container)
    val loader = new FXMLLoader(getClass.getResource("/view/ShellView.fxml"))
    val root: Parent = loader.load()
    
    // 5. Connect Service layer to ShellController (which passes it to the respective child view controllers)
    val shellController = loader.getController[ShellController]()
    shellController.setMarketplaceManager(manager)
    
    // 6. Set up the Primary Stage & ScalaFX Scene Layout
    stage = new JFXApp3.PrimaryStage:
      title = "Community Skills Marketplace System"
      scene = new Scene(new javafx.scene.Scene(root))
      width = 1100
      height = 750
  end start
end Main
