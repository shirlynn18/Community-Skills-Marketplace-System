# Project_23060486 <br>🤝 Community Skills Marketplace System

A premium, state-of-the-art desktop application built in Scala 3 designed to facilitate mutual skill exchanges within a local community. Utilizing a virtual credit economy, members can publish skill offers, post requests for assistance, and book matches safely.

---

## 🎨 Design System & Aesthetics
The application features a modern **Moss Green & Dark Forest** design system styled using custom CSS:
* **Backgrounds**: Deep blackened teal and dark mossy green-gray backgrounds for low eye-strain.
* **Accents**: Pale seafoam green (`-teal`), gold/mustard accents, and warm off-white text highlights.
* **Layout**: Sleek card structures, structured borders, and responsive split-pane controls.

---

## 🚀 Key Features

* **📊 Interactive Dashboard**:
  * Real-time KPI summaries (Total registered members, active posts, total credits exchanged).
  * Dynamic, responsive horizontal stacked bar chart showing categories of posts (Tutoring, Repairs, Childcare, Other) mapped with visual color indicators (Yellow for Offers, Purple for Requests).
  * Live-updating recent activity feed logging member registrations, matches, and transactions.
* **👥 Member Directory**:
  * Complete CRUD capability (Register, Edit, Delete).
  * Inputs validated using regex patterns (e.g., Malaysian phone numbers formatting matching `+601xxxxxxxx`).
  * Manual credit adjustments for administration purposes.
* **💡 Skill Post Registry**:
  * Support for **Skill Offers** (where providers specify hourly credit rates).
  * Support for **Skill Requests** (where receivers specify needed service hours).
  * Automatically switches form fields dynamically based on the selected post type.
  * Checks active match associations to lock status editing of posts currently undergoing transaction execution.
* **🔄 Service Exchange & Credit Transaction Ledger**:
  * Matches active offers and requests with overlapping skill names.
  * Prevents self-matching (members cannot trade with themselves).
  * Validates requester credit coverage before scheduling matches.
  * Toggles button states dynamically along the match lifecycle: `Pending ➔ Accepted ➔ Completed / Cancelled`.
  * **Dual selection synchronization**: Clicking a transaction audit log automatically selects and highlights the corresponding service match.

---

## 🛠️ Technology Stack

* **Language**: [Scala 3.3.5](file:///d:/Sunway/Sunway_BCS_SEM_6/PRG2104%20OOP/Final%20As/Project_23060486/build.sbt#L1) (utilizing modern indentation-based syntax and end markers)
* **GUI Core & Wrappers**: [ScalaFX 21.0.0-R32](file:///d:/Sunway/Sunway_BCS_SEM_6/PRG2104%20OOP/Final%20As/Project_23060486/build.sbt#L4)
* **JavaFX Modules**: [OpenJFX 21](file:///d:/Sunway/Sunway_BCS_SEM_6/PRG2104%20OOP/Final%20As/Project_23060486/build.sbt#L30-L32) (base, controls, fxml, graphics, media, web)
* **Database Engine**: [Apache Derby 10.16.1.1](file:///d:/Sunway/Sunway_BCS_SEM_6/PRG2104%20OOP/Final%20As/Project_23060486/build.sbt#L24-L26) (Embedded, Shared, Tools)
* **SQL Mapping**: [ScalikeJDBC 4.3.5](file:///d:/Sunway/Sunway_BCS_SEM_6/PRG2104%20OOP/Final%20As/Project_23060486/build.sbt#L23)
* **Logging Framework**: [Logback Classic 1.5.6](file:///d:/Sunway/Sunway_BCS_SEM_6/PRG2104%20OOP/Final%20As/Project_23060486/build.sbt#L22)
* **Testing Engine**: [ScalaTest 3.2.19](file:///d:/Sunway/Sunway_BCS_SEM_6/PRG2104%20OOP/Final%20As/Project_23060486/build.sbt#L27)

---

## 📂 Project Structure

```bash
Project_23060486/
├── README.md
├── build.sbt                     # SBT settings and JavaFX module dependencies
├── project/                      # SBT build settings
│   └── build.properties
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   ├── css/
│   │   │   │   └── style.css     # Custom warm cream / moss green stylesheets
│   │   │   └── view/
│   │   │       ├── DashboardView.fxml
│   │   │       ├── Main.fxml
│   │   │       ├── MemberManagementView.fxml
│   │   │       ├── ServiceExchangeView.fxml
│   │   │       └── SkillManagementView.fxml
│   │   └── scala/
│   │       ├── Main.scala        # Main entrypoint and UI views loader
│   │       ├── controller/       # UI Controllers (handling UI inputs and hooks)
│   │       │   ├── DashboardController.scala
│   │       │   ├── MemberManagementController.scala
│   │       │   ├── ServiceExchangeController.scala
│   │       │   ├── ShellController.scala
│   │       │   └── SkillManagementController.scala
│   │       ├── model/            # Model entities, Repositories, Schema initializer
│   │       │   ├── DatabaseInitializer.scala
│   │       │   ├── Models.scala
│   │       │   └── Repositories.scala
│   │       └── service/          # Business services and transactional workflows
│   │           └── MarketplaceManager.scala
│   └── test/
│       └── scala/
│           └── service/          # Unit tests verifying matching rules
│               └── MarketplaceManagerSpec.scala
├── docs/                         # Assignment documentation files
│   ├── UML.png                   # UML Class diagram with 5+ classes
│   ├── reflection.md             # Personal reflection report (~350-700 words)
│   ├── ai_reflection.md          # AI integration reflection report (300-500 words)
│   ├── dev_log.md                # Development log with 5+ dated entries spanning 5+ days
│   ├── citations.md              # Third-party code/asset citations and licenses
│   └── demo.mp4                  # Walkthrough video demonstrating all features (<= 5 min)
├── ai/                           # AI usage logs and declaration sheets
│   ├── interaction_log.md        # Template 2 - AI Interaction Log with 10+ entries
│   └── declaration.md            # Template 3 - Signed AI declaration sheet
└── submission_manifest.md        # Submission package manifest checklist

```

---

## ⚙️ Setup and Run Instructions (For Clean Machines)

### 1. Install Prerequisites

* **Java JDK 21**: Make sure Java JDK 21 is installed.
  * *Verify*: Run `java -version` in your command line or PowerShell.
* **SBT (Scala Build Tool)**: Install the latest SBT build tool.
  * *Verify*: Run `sbt --version` (verifies the launcher version).

### 2. Launch the Desktop App

Navigate to the project root directory in your command line and execute:
```bash
sbt run
```
* **First Run**: SBT automatically downloads Scala 3.3.5, JavaFX runtime packages matching your operating system, ScalikeJDBC, and Apache Derby libraries.
* The application runs locally using an embedded memory database (stored in `db/community_db`).

### 3. Run Testing Suites
To run Unit and Integration tests verifying member balances, transaction completions, and constraint checks:
```bash
sbt test
```

---

## 📖 Sample Run Walkthrough

Once the application launches:

1. **Dashboard Overview**: Review the initialized charts, KPIs, and seeded recent activity feed.
2. **Member Registration**:
   * Click **Member Directory**.
   * Fill out the form (e.g. Name: `Shirlynn`, Phone: `+60123456789`, Residential Area: `Taman Perdana`, Starting Credits: `200`).
   * Click **Register Member**.
3. **Posting Skills**:
   * Click **Skill Posts**.
   * Choose member `Shirlynn` from the dropdown, select type **Request**, enter skill `Tutoring`, hours `3.0`, and select status `Open`.
   * Click **Create Post**.
4. **Creating a Match**:
   * Click **Service Matches**.
   * Select an active Offer matching `Tutoring` (with rate e.g. `20` credits/hr) and your newly created Request.
   * Pick a booking date on the calendar and click **Create Match**. The match registers as `Pending`.
5. **Executing The Transaction**:
   * Select your match from the matches table list.
   * Click **Accept** to transition state to `Accepted`.
   * Click **Complete** to execute. Credits are automatically deducted from the requester and transferred to the provider, saving audit transaction logs in the bottom table.

---

## 🤖 AI Integration Summary

This project was developed under the Tier C AI-Integrated policy. AI tools (Gemini 3.5 and ChatGPT) were used to accelerate the development process under human direction.
* **Architecture & Scoping**: AI assisted in designing the initial layout templates, setting up the standard MVC boilerplate pattern, and advising on ScalaFX/JavaFX interoperability.
* **Refactoring & Optimization**: Used AI suggestions to refine specific implementations, such as database transaction rollbacks in ScalikeJDBC and reactive layout bindings in ScalaFX.
* **Human-in-the-Loop Supervision**: All AI suggestions were evaluated for correctness. Hallucinations or syntax mismatches (such as wrong collection bindings) were spotted during compilation, corrected, and coded manually to match the project's requirements.

---

# Third-Party and External Resources Citations

1. **ScalaFX**
   - **URL**: https://github.com/scalafx/scalafx
   - **License**: BSD 

2. **JavaFX Control**
   - **URL**: https://openjfx.io/
   - **License**: GPL 2.0

3. **JavaFX Graphics**
   - **URL**: https://openjfx.io/
   - **License**: GPL 2.0

4. **JavaFX Base**
   - **URL**: https://openjfx.io/
   - **License**: GPL 2.0

5. **JavaFX FXML**
   - **URL**: https://openjfx.io/
   - **License**: GPL 2.0
   
6. **ScalikeJDBC**
   - **URL**: https://scalikejdbc.org/
   - **License**: Apache 2.0

7. **Apache Derby Database Engine and Embedded JDBC Driver**
   - **URL**: https://db.apache.org/derby/
   - **License**: Apache 2.0

8. **Apache Derby Tools**
   - **URL**: https://db.apache.org/derby/
   - **License**: Apache 2.0
   
9. **Apache Derby Shared Code**
   - **URL**: https://db.apache.org/derby/
   - **License**: Apache 2.0

10. **Logback Classic Module**
    - **URL**: https://logback.qos.ch/
    - **License**: EPL 2.0 / LGPL 2.1

11. **ScalatestDotty**
    - **URL**: https://www.scalatest.org/
    - **License**: Apache 2.0
