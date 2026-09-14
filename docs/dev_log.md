# Development Log_23060486

## 2026-07-23
- Read through the project brief carefully and confirmed my project idea as a Community Skills Marketplace under SDG 1.
- Planned the main system features and decided to include Dashboard, Member Management, Skill Post Management, and Service Exchange & Credit Management.
- Created the project folder structure and prepared the required files and folders for development.
- Sketched an early UML draft to plan the classes and relationships before coding.

## 2026-07-28
- Set up the basic ScalaFX project and started building the main application shell.
- Planned the navigation structure for the four main tabs.
- Designed the core domain classes such as Member, SkillPost, SkillOffer, SkillRequest, ServiceExchange, and CreditTransaction.
- Decided to use inheritance for SkillPost so that SkillOffer and SkillRequest could share common fields and behaviour.
- Added a service layer to keep business logic separate from the UI code.

## 2026-08-01
- Worked on the Dashboard tab and connected it to actual system data instead of hardcoded values.
- Added summary cards to show total members, open skill posts, completed exchanges, and credits exchanged.
- Improved the dashboard layout so the information looks clearer and more balanced.
- Added recent transaction display and adjusted the dashboard sections to make the screen easier to read.

## 2026-08-05
- Implemented the Member Management tab.
- Added functions to create, update, delete, search, and view members.
- Added form validation for member details such as name, phone number, and credit balance.
- Improved the member details panel so it works better in create mode and edit mode.
- Updated the member table layout and cleaned up the interface to make it more user-friendly.

## 2026-08-06
- Implemented the Skill Post Management tab for both skill offers and skill requests.
- Added functions to create, update, delete, search, and filter skill posts.
- Updated the form and table structure to better match the final project workflow.
- Added validation and business rules to prevent invalid matches and incorrect input.
- Started work on the Service Exchange & Credit Management tab and linked it to the skill post workflow.

## 2026-08-13
- Tested the full system to make sure the tabs work properly together.
- Fixed issues related to selection handling, validation messages, and status updates.
- Checked that saved data still appears after restarting the application.
- Prepared the project documentation and reviewed the overall structure of the code.
- Prepared for the viva by revising the class design, key features, and main design decisions.
- Recorded the demo video.
- Finalised the UML diagram.

## 2026-08-14
- Viva.
- Did a final check of the submission folder and required files.
- Reviewed the project features and made sure everything was ready for submission.
- Submitted the project.
