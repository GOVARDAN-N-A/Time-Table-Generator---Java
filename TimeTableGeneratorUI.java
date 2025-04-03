import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
// import javafx.scene.image.Image;
// import javafx.scene.image.ImageView;
import javafx.print.*; // <-- Import Printer API
import javafx.stage.Window; // <-- Import Window for print dialog owner
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger; // For serial number

// Assuming TimeTableGenerator class is available
// import TimeTableGenerator;

public class TimeTableGeneratorUI extends Application {

    // Constants (remain the same)
    private static final int PERIODS_PER_DAY = 8;
    private static final int DAYS_PER_WEEK = 5;
    private static final int TOTAL_PERIODS_PER_WEEK = PERIODS_PER_DAY * DAYS_PER_WEEK; // 40
    private static final List<String> DAYS_OF_WEEK = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");

    // Color palette (remain the same)
    private static final String PRIMARY_COLOR = "#2C3E50";
    private static final String SECONDARY_COLOR = "#3498DB";
    private static final String ACCENT_COLOR = "#1ABC9C";
    private static final String WARNING_COLOR = "#E74C3C";
    private static final String SUCCESS_COLOR = "#2ECC71";
    private static final String TEXT_COLOR = "#34495E";
    private static final String LIGHT_BG_COLOR = "#ECF0F1";
    private static final String BORDER_COLOR = "#BDC3C7";
    private static final String THEORY_COLOR = "#D5F5E3";
    private static final String LAB_COLOR = "#D4E6F1";

    // UI components (add TableView references)
    private VBox inputContainer;
    private VBox subjectsContainer;
    private final ObservableList<SubjectRow> subjectRows = FXCollections.observableArrayList();
    private Button addSubjectBtn;
    private Button generateBtn;
    private TabPane resultTabs;
    private GridPane sectionAGrid;
    private GridPane sectionBGrid;
    private Label sectionAFitness;
    private Label sectionBFitness;
    private TableView<SubjectSummary> sectionASummaryTable; // New TableView
    private TableView<SubjectSummary> sectionBSummaryTable; // New TableView
    private Tab sectionATab; // Declare sectionATab
    private Tab sectionBTab; // Declare sectionBTab
    private ProgressIndicator progressIndicator;
    private Label statusLabel;

    // Data storage (remain the same)
    private final Map<String, Boolean> isLabMap = new HashMap<>();
    private final Map<String, String> subjectStaffMap = new HashMap<>();
    private final Map<String, Integer> subjectsWithPeriods = new HashMap<>();
    private final Map<String, String> subjectShortNameMap = new HashMap<>();
    private final Map<String, String> subjectCodeMap = new HashMap<>();

    // Helper class for TableView data
    public static class SubjectSummary {
        private final SimpleIntegerProperty serialNo;
        private final SimpleStringProperty subjectCode;
        private final SimpleStringProperty subjectNameDisplay; // Full Name (Short Name)
        private final SimpleStringProperty staffName;
        private final SimpleIntegerProperty totalPeriods;

        public SubjectSummary(int serialNo, String subjectCode, String subjectNameDisplay, String staffName, int totalPeriods) {
            this.serialNo = new SimpleIntegerProperty(serialNo);
            this.subjectCode = new SimpleStringProperty(subjectCode);
            this.subjectNameDisplay = new SimpleStringProperty(subjectNameDisplay);
            this.staffName = new SimpleStringProperty(staffName);
            this.totalPeriods = new SimpleIntegerProperty(totalPeriods);
        }

        // --- Getters for PropertyValueFactory ---
        public int getSerialNo() { return serialNo.get(); }
        public String getSubjectCode() { return subjectCode.get(); }
        public String getSubjectNameDisplay() { return subjectNameDisplay.get(); }
        public String getStaffName() { return staffName.get(); }
        public int getTotalPeriods() { return totalPeriods.get(); }
    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("TimeTable Generator Pro");

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(createHeader());
        mainLayout.setBottom(createFooter());

        SplitPane splitPane = new SplitPane();
        ScrollPane inputScrollPane = new ScrollPane(createInputSection());
        inputScrollPane.setFitToWidth(true);
        inputScrollPane.getStyleClass().add("custom-scroll-pane");

        ScrollPane outputScrollPane = new ScrollPane(createOutputSection());
        outputScrollPane.setFitToWidth(true);
        outputScrollPane.setFitToHeight(false); // Allow vertical scrolling
        outputScrollPane.getStyleClass().add("custom-scroll-pane");
        outputScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outputScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        splitPane.getItems().addAll(inputScrollPane, outputScrollPane);
        splitPane.setDividerPositions(0.42);

        mainLayout.setCenter(splitPane);

        Scene scene = new Scene(mainLayout, 1450, 800);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- Header, Footer, Input Section, Add/Remove Row (No changes) ---
    private VBox createHeader() { /* ... No changes ... */
        VBox header = new VBox();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setSpacing(8);
        header.setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");

        Label titleLabel = new Label("TimeTable Generator");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        titleLabel.setTextFill(Color.WHITE);

        Label subtitleLabel = new Label("Genetic Algorithm Based Scheduler");
        subtitleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        subtitleLabel.setTextFill(Color.web(LIGHT_BG_COLOR));

        header.getChildren().addAll(titleLabel, subtitleLabel);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.4));
        header.setEffect(dropShadow);

        return header;
     }
    private HBox createFooter() { /* ... No changes ... */
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 20, 8, 20));
        footer.setStyle("-fx-background-color: " + LIGHT_BG_COLOR + "; -fx-border-width: 1 0 0 0; -fx-border-color: " + BORDER_COLOR + ";");

        Label versionLabel = new Label("Version 2.4 © 2025"); // Updated version
        versionLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        versionLabel.setTextFill(Color.web(TEXT_COLOR));

        footer.getChildren().add(versionLabel);
        return footer;
    }
    private VBox createInputSection() { /* ... No changes ... */
        inputContainer = new VBox(18);
        inputContainer.setPadding(new Insets(25));
        inputContainer.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Input Subject Details");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        Label instructionsLabel = new Label(
            "Add subjects with Full Name, Short Name (for display), Code, Periods, Type, and Staff. " +
            "Total periods MUST sum to exactly " + TOTAL_PERIODS_PER_WEEK + "."
        );
        instructionsLabel.setWrapText(true);
        instructionsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        instructionsLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");

        subjectsContainer = new VBox(10);
        subjectsContainer.setPadding(new Insets(10, 0, 10, 0));
        addSubjectRow(); // Add the first row

        addSubjectBtn = new Button("Add Another Subject");
        addSubjectBtn.setOnAction(e -> addSubjectRow());
        addSubjectBtn.getStyleClass().add("add-button");
        addSubjectBtn.setStyle("-fx-background-color: " + SECONDARY_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        addSubjectBtn.setPrefWidth(200);
        addSubjectBtn.setCursor(javafx.scene.Cursor.HAND);

        HBox controlsBox = new HBox(20);
        controlsBox.setAlignment(Pos.CENTER);

        generateBtn = new Button("Generate Timetables");
        generateBtn.setOnAction(e -> generateTimetables());
        generateBtn.getStyleClass().add("generate-button");
        generateBtn.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        generateBtn.setPrefWidth(220);
        generateBtn.setPrefHeight(45);
        generateBtn.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        generateBtn.setCursor(javafx.scene.Cursor.HAND);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(35, 35);

        controlsBox.getChildren().addAll(generateBtn, progressIndicator);

        statusLabel = new Label("");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        statusLabel.setTextFill(Color.web(TEXT_COLOR));
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setManaged(false);

        inputContainer.getChildren().addAll(
                titleLabel,
                instructionsLabel,
                new Separator(javafx.geometry.Orientation.HORIZONTAL),
                subjectsContainer,
                addSubjectBtn,
                new Separator(javafx.geometry.Orientation.HORIZONTAL),
                controlsBox,
                statusLabel
        );

        return inputContainer;
    }
    private void addSubjectRow() { /* ... No changes ... */
        int nextNumber = subjectRows.size() + 1;
        SubjectRow row = new SubjectRow(nextNumber, this::removeSubjectRow);
        subjectRows.add(row);

        Node rowNode = row.getContainer();
        subjectsContainer.getChildren().add(rowNode);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), rowNode);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
    private void removeSubjectRow(SubjectRow rowToRemove) { /* ... No changes ... */
        Node rowNode = rowToRemove.getContainer();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), rowNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            subjectsContainer.getChildren().remove(rowNode);
            subjectRows.remove(rowToRemove);
            // When a row is removed, we also need to remove its data from our maps
            String subjectNameToRemove = rowToRemove.getSubjectName();
            if (subjectNameToRemove != null && !subjectNameToRemove.isEmpty()) {
                subjectsWithPeriods.remove(subjectNameToRemove);
                subjectStaffMap.remove(subjectNameToRemove);
                isLabMap.remove(subjectNameToRemove);
                subjectShortNameMap.remove(subjectNameToRemove);
                subjectCodeMap.remove(subjectNameToRemove);
            }
        });
        fadeOut.play();
    }
    // --- End of Unchanged Sections ---

    // Modified to create and return the TableView as well
    private TabPane createOutputSection() {
        resultTabs = new TabPane();
        resultTabs.getStyleClass().add("custom-tab-pane");

        // Create content for Section A Tab, including the summary table
        sectionASummaryTable = createSubjectSummaryTable();
        Tab sectionATab = new Tab("Section A");
        // Pass the Tab itself so the content knows its owner window for printing dialog
        sectionATab.setContent(createTimetableTabContent("Section A", sectionAGrid = createEmptyTimetableGrid(), sectionAFitness = new Label("Fitness: N/A"), sectionASummaryTable));

        // Create content for Section B Tab, including the summary table
        sectionBSummaryTable = createSubjectSummaryTable();
        Tab sectionBTab = new Tab("Section B");
        sectionBTab.setContent(createTimetableTabContent("Section B", sectionBGrid = createEmptyTimetableGrid(), sectionBFitness = new Label("Fitness: N/A"), sectionBSummaryTable));

        resultTabs.getTabs().addAll(sectionATab, sectionBTab);
        resultTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        resultTabs.setStyle("-fx-tab-min-width: 100px; -fx-tab-max-height: 40px; -fx-font-size: 14px;");

        return resultTabs;
    }

    // Modified to include the summary table in the layout
    private Node createTimetableTabContent(String sectionName, GridPane grid, Label fitnessLabel, TableView<SubjectSummary> summaryTable) {
        VBox container = new VBox(15); // Main container for the tab content
        container.setPadding(new Insets(25));
        container.setStyle("-fx-background-color: white;");

        // --- Top Section: Title and Legend ---
        Label titleLabel = new Label(sectionName + " Timetable");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));
        HBox legend = createLegend();
        VBox titleAndLegendBox = new VBox(10, titleLabel, legend);

        // --- Middle Section: Grid and Controls (Fitness & Print) ---
        fitnessLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        fitnessLabel.setTextFill(Color.web(TEXT_COLOR));

        // Create Print Button
        Button printButton = new Button("Print");
        printButton.setStyle("-fx-background-color: " + SECONDARY_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold;");
        printButton.setCursor(javafx.scene.Cursor.HAND);
        printButton.setOnAction(e -> printNode(container)); // Print the whole VBox container

        // Layout for Fitness and Print Button
        Region spacer = new Region(); // Pushes fitness left, print right
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controlsBox = new HBox(15, fitnessLabel, spacer, printButton);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.setPadding(new Insets(5, 0, 5, 0)); // Add some padding

        // --- Bottom Section: Summary Table ---
        Label summaryTitleLabel = new Label("Subject & Staff Summary");
        summaryTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        summaryTitleLabel.setTextFill(Color.web(PRIMARY_COLOR));
        summaryTitleLabel.setPadding(new Insets(10, 0, 5, 0));

        // Add all components to the main VBox container
        container.getChildren().addAll(titleAndLegendBox, grid, controlsBox, summaryTitleLabel, summaryTable);
        return container;
    }


    // Creates the structure of the summary TableView
    private TableView<SubjectSummary> createSubjectSummaryTable() {
        TableView<SubjectSummary> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS); // Adjust columns to fit width
        table.setPlaceholder(new Label("Generate a timetable to see the summary."));
        table.setMinHeight(150); // Give it some minimum height
        table.setPrefHeight(250); // Preferred height

        // Define Columns
        TableColumn<SubjectSummary, Integer> serialCol = new TableColumn<>("S.No.");
        serialCol.setCellValueFactory(new PropertyValueFactory<>("serialNo"));
        serialCol.setMaxWidth(1f * Integer.MAX_VALUE * 5); // 5% width approx
        serialCol.setMinWidth(50);
        serialCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<SubjectSummary, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        codeCol.setMaxWidth(1f * Integer.MAX_VALUE * 15); // 15% width
        codeCol.setMinWidth(80);

        TableColumn<SubjectSummary, String> nameCol = new TableColumn<>("Subject Name (Short)");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("subjectNameDisplay"));
        nameCol.setMaxWidth(1f * Integer.MAX_VALUE * 40); // 40% width
        nameCol.setMinWidth(200);

        TableColumn<SubjectSummary, String> staffCol = new TableColumn<>("Staff Name");
        staffCol.setCellValueFactory(new PropertyValueFactory<>("staffName"));
        staffCol.setMaxWidth(1f * Integer.MAX_VALUE * 25); // 25% width
        staffCol.setMinWidth(150);

        TableColumn<SubjectSummary, Integer> periodsCol = new TableColumn<>("Periods");
        periodsCol.setCellValueFactory(new PropertyValueFactory<>("totalPeriods"));
        periodsCol.setMaxWidth(1f * Integer.MAX_VALUE * 15); // 15% width
        periodsCol.setMinWidth(60);
        periodsCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(serialCol, codeCol, nameCol, staffCol, periodsCol);

        return table;
    }

    private void printNode(Node nodeToPrint) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showAlert("Printing Error", "Could not create a printer job. Check system printer configuration.", Alert.AlertType.ERROR);
            return;
        }

        // Get the owner window for the print dialog
        Window owner = null;
        if (nodeToPrint != null && nodeToPrint.getScene() != null) {
            owner = nodeToPrint.getScene().getWindow();
        }

        boolean proceed = job.showPrintDialog(owner);

        if (proceed) {
            // Optional: Set Page Layout (e.g., Landscape)
             Printer printer = job.getPrinter();
             // Consider Landscape might fit the timetable better
             PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT);

             // --- Scaling Logic (Simple: Scale node to fit page width) ---
             double scaleX = 1.0;
             double scaleY = 1.0;
             if (pageLayout != null) {
                 double pgWidth = pageLayout.getPrintableWidth();
                 double pgHeight = pageLayout.getPrintableHeight();
                 double nodeWidth = nodeToPrint.getBoundsInParent().getWidth();
                 double nodeHeight = nodeToPrint.getBoundsInParent().getHeight();

                 scaleX = pgWidth / nodeWidth;
                 scaleY = pgHeight / nodeHeight;

                 // Use the smaller scale factor to ensure the entire node fits without distortion
                 double scale = Math.min(scaleX, scaleY);

                 // Apply scaling transformation (important!)
                 nodeToPrint.getTransforms().add(new javafx.scene.transform.Scale(scale, scale));
             } else {
                  System.err.println("Warning: Could not get PageLayout. Printing with default scaling.");
             }

            // Print the (potentially scaled) node using the determined page layout
            boolean printed = job.printPage(pageLayout, nodeToPrint); // Use layout

            // *** IMPORTANT: Remove the scaling transform AFTER printing ***
            // Otherwise, the node remains scaled on the screen.
             if (pageLayout != null) {
                 nodeToPrint.getTransforms().remove(nodeToPrint.getTransforms().size() - 1); // Remove the last added scale transform
             }


            if (printed) {
                boolean success = job.endJob();
                if (success) {
                    showNotification("Printing", "Sent timetable to printer.", SUCCESS_COLOR);
                } else {
                    showAlert("Printing Error", "Failed to end the print job.", Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Printing Failed", "Could not print the timetable page.", Alert.AlertType.ERROR);
                 // Also end the job even if printing the page failed
                 job.endJob();
            }
        } else {
             showNotification("Printing Cancelled", "Printing was cancelled by the user.", WARNING_COLOR);
        }
    }

    // --- Grid Creation and Cell Styling (No changes) ---
    private HBox createLegend() { /* ... No changes ... */
         HBox legend = new HBox(25);
         legend.setPadding(new Insets(5, 0, 15, 0));
         legend.setAlignment(Pos.CENTER_LEFT);

         legend.getChildren().addAll(
                 createLegendItem("Theory", THEORY_COLOR),
                 createLegendItem("Lab", LAB_COLOR)
         );

         return legend;
    }
    private HBox createLegendItem(String text, String color) { /* ... No changes ... */
         HBox item = new HBox(8);
         item.setAlignment(Pos.CENTER_LEFT);
         Pane colorRect = new Pane();
         colorRect.setPrefSize(18, 18);
         colorRect.setStyle("-fx-background-color: " + color + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1px; -fx-background-radius: 3; -fx-border-radius: 3;");
         Label label = new Label(text);
         label.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
         item.getChildren().addAll(colorRect, label);
         return item;
    }
    private GridPane createEmptyTimetableGrid() { /* ... No changes ... */
         GridPane grid = new GridPane();
         grid.setHgap(3);
         grid.setVgap(3);
         grid.setPadding(new Insets(10));
         grid.setStyle("-fx-background-color: " + BORDER_COLOR + "; -fx-border-color: " + BORDER_COLOR +"; -fx-border-width: 1;");

         ColumnConstraints dayColConstraint = new ColumnConstraints();
         dayColConstraint.setPrefWidth(110);
         dayColConstraint.setMinWidth(90);
         grid.getColumnConstraints().add(dayColConstraint);

         for (int i = 1; i <= PERIODS_PER_DAY; i++) {
             ColumnConstraints periodColConstraint = new ColumnConstraints();
             periodColConstraint.setPrefWidth(130);
             periodColConstraint.setMinWidth(100);
             periodColConstraint.setHgrow(Priority.SOMETIMES);
             grid.getColumnConstraints().add(periodColConstraint);
         }

         Label dayHeader = createHeaderCell("Day / Period");
         grid.add(dayHeader, 0, 0);
         for (int i = 1; i <= PERIODS_PER_DAY; i++) {
             Label periodLabel = createHeaderCell("Period " + i);
             grid.add(periodLabel, i, 0);
         }

         for (int i = 0; i < DAYS_OF_WEEK.size(); i++) {
             Label dayLabel = createDayCell(DAYS_OF_WEEK.get(i));
             grid.add(dayLabel, 0, i + 1);

             for (int j = 1; j <= PERIODS_PER_DAY; j++) {
                 Label emptyCell = createEmptyDataCell(); // Placeholder cell
                 grid.add(emptyCell, j, i + 1);
             }
         }

         DropShadow dropShadow = new DropShadow();
         dropShadow.setRadius(6.0);
         dropShadow.setOffsetY(4.0);
         dropShadow.setColor(Color.rgb(0, 0, 0, 0.2));
         grid.setEffect(dropShadow);

         return grid;
    }
    private Label createHeaderCell(String text) { /* ... No changes ... */
         Label label = new Label(text);
         label.setMaxWidth(Double.MAX_VALUE);
         label.setAlignment(Pos.CENTER);
         label.setFont(Font.font("Arial", FontWeight.BOLD, 13));
         label.setTextFill(Color.WHITE);
         label.setPadding(new Insets(8));
         label.setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");
         return label;
    }
    private Label createDayCell(String text) { /* ... No changes ... */
         Label label = new Label(text);
         label.setMaxWidth(Double.MAX_VALUE);
         label.setMaxHeight(Double.MAX_VALUE);
         label.setAlignment(Pos.CENTER);
         label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
         label.setTextFill(Color.WHITE);
         label.setPadding(new Insets(8));
         label.setStyle("-fx-background-color: " + SECONDARY_COLOR + ";");
         return label;
    }
    private Label createEmptyDataCell() { /* ... No changes ... */
         Label label = new Label("-");
         label.setMaxWidth(Double.MAX_VALUE);
         label.setMaxHeight(Double.MAX_VALUE);
         label.setAlignment(Pos.CENTER);
         label.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
         label.setPadding(new Insets(10));
         label.setMinHeight(60);
         label.setStyle("-fx-background-color: #f9f9f9;");
         return label;
    }
    // --- End of Unchanged Grid/Cell Code ---


    // Modified to update summary tables as well
    private void generateTimetables() {
        // --- 1. Collect and Validate Input ---
        // (Same as previous version - collects all data including short names/codes)
        subjectsWithPeriods.clear();
        isLabMap.clear();
        subjectStaffMap.clear();
        subjectShortNameMap.clear();
        subjectCodeMap.clear();
        boolean inputValid = true;
        int totalPeriodsRequired = 0;
        Set<String> subjectNames = new HashSet<>();
        Set<String> subjectCodes = new HashSet<>();
        Set<String> subjectShortNames = new HashSet<>();

        if (subjectRows.isEmpty()) { /* ... alert ... */ return; }

        for (SubjectRow row : subjectRows) {
            boolean rowIsValid = true;
             String originalStyle = "-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1px; -fx-border-radius: 5; -fx-background-radius: 5;";
             // Simplified style reset
             row.getContainer().setStyle(originalStyle);

            if (!row.isValid()) {
                rowIsValid = false;
            } else {
                String subjectName = row.getSubjectName();
                String shortName = row.getSubjectShortName();
                String code = row.getSubjectCode();
                int periods = row.getPeriods();
                boolean isLab = row.isLab();
                String staffName = row.getStaffName();

                if (!subjectNames.add(subjectName)) { /* ... alert ... */ rowIsValid = false; }
                if (!subjectShortNames.add(shortName)) { /* ... alert ... */ /* Optional: rowIsValid = false; */ }
                if (!subjectCodes.add(code)) { /* ... alert ... */ /* Optional: rowIsValid = false; */ }

                if(rowIsValid) {
                    subjectsWithPeriods.put(subjectName, periods);
                    subjectStaffMap.put(subjectName, staffName);
                    isLabMap.put(subjectName, isLab);
                    subjectShortNameMap.put(subjectName, shortName);
                    subjectCodeMap.put(subjectName, code);
                    totalPeriodsRequired += periods;
                }
            }
             if (!rowIsValid) {
                 inputValid = false;
                 row.getContainer().setStyle(row.getContainer().getStyle() + "; -fx-border-color: " + WARNING_COLOR + "; -fx-border-width: 1.5px;");
             }
        }

        if (!inputValid) { /* ... alert ... */ return; }
        if (totalPeriodsRequired != TOTAL_PERIODS_PER_WEEK) { /* ... alert ... */ return; }


        // --- 2. Prepare for Background Task ---
        generateBtn.setDisable(true);
        generateBtn.setText("Generating...");
        progressIndicator.setVisible(true);
        statusLabel.setText("Initializing Genetic Algorithm...");
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);

        // Clear previous results visually (including tables)
        updateTimetableGrid(sectionAGrid, null);
        updateTimetableGrid(sectionBGrid, null);
        updateSummaryTable(sectionASummaryTable, null); // Clear Table A
        updateSummaryTable(sectionBSummaryTable, null); // Clear Table B
        sectionAFitness.setText("Fitness: N/A");
        sectionBFitness.setText("Fitness: N/A");

        // --- 3. Run Genetic Algorithm in Background Thread ---
        // (Same GA execution logic)
        AtomicReference<TimeTableGenerator.Schedule> scheduleARef = new AtomicReference<>();
        AtomicReference<TimeTableGenerator.Schedule> scheduleBRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        Thread generationThread = new Thread(() -> {
            try {
                Map<String, Integer> periodsCopy = new HashMap<>(subjectsWithPeriods);
                Map<String, String> staffCopy = new HashMap<>(subjectStaffMap);
                Map<String, Boolean> labCopy = new HashMap<>(isLabMap);

                Platform.runLater(() -> statusLabel.setText("Generating Section A timetable..."));
                TimeTableGenerator.Schedule scheduleA = TimeTableGenerator.runGeneticAlgorithm(periodsCopy, staffCopy, labCopy, null);
                scheduleARef.set(scheduleA);
                System.out.println("Section A generation complete. Fitness: " + (scheduleA != null ? scheduleA.getFitness() : "N/A"));

                Platform.runLater(() -> statusLabel.setText("Generating Section B timetable..."));
                TimeTableGenerator.Schedule scheduleB = TimeTableGenerator.runGeneticAlgorithm(periodsCopy, staffCopy, labCopy, scheduleA);
                scheduleBRef.set(scheduleB);
                System.out.println("Section B generation complete. Fitness: " + (scheduleB != null ? scheduleB.getFitness() : "N/A"));

                // --- 4. Update UI on JavaFX Thread ---
                Platform.runLater(() -> {
                    TimeTableGenerator.Schedule finalScheduleA = scheduleARef.get();
                    TimeTableGenerator.Schedule finalScheduleB = scheduleBRef.get();

                    // Update grids
                    updateTimetableGrid(sectionAGrid, finalScheduleA);
                    updateTimetableGrid(sectionBGrid, finalScheduleB);

                    // *** Update Summary Tables ***
                    updateSummaryTable(sectionASummaryTable, subjectsWithPeriods);
                    updateSummaryTable(sectionBSummaryTable, subjectsWithPeriods); // Both sections use the same subject list

                    // Update fitness labels
                    sectionAFitness.setText(String.format("Fitness: %d", (finalScheduleA != null ? finalScheduleA.getFitness() : Integer.MIN_VALUE)));
                    sectionBFitness.setText(String.format("Fitness: %d", (finalScheduleB != null ? finalScheduleB.getFitness() : Integer.MIN_VALUE)));

                    // Reset controls
                    generateBtn.setDisable(false);
                    generateBtn.setText("Generate Timetables");
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Generation complete!");
                    statusLabel.setManaged(false);

                    showNotification("Success", "Timetables generated successfully!", SUCCESS_COLOR);
                    resultTabs.getSelectionModel().select(0);
                });

            } catch (Exception ex) { /* ... error handling ... */ }
        });
        generationThread.setDaemon(true);
        generationThread.start();
    }

    // Populates the summary table using the main data maps
    private void updateSummaryTable(TableView<SubjectSummary> table, Map<String, Integer> finalPeriodsMap) {
        ObservableList<SubjectSummary> summaryData = FXCollections.observableArrayList();
        if (table == null) return; // Safety check

        if (finalPeriodsMap != null && !finalPeriodsMap.isEmpty()) {
            AtomicInteger serialCounter = new AtomicInteger(1);
            // Sort subjects alphabetically by full name for consistent order
            List<String> sortedSubjectNames = new ArrayList<>(finalPeriodsMap.keySet());
            Collections.sort(sortedSubjectNames);

            for (String subjectFullName : sortedSubjectNames) {
                String code = subjectCodeMap.getOrDefault(subjectFullName, "N/A");
                String shortName = subjectShortNameMap.getOrDefault(subjectFullName, "");
                String staff = subjectStaffMap.getOrDefault(subjectFullName, "N/A");
                int periods = finalPeriodsMap.getOrDefault(subjectFullName, 0);

                // Format display name: Full Name (Short Name)
                String displayName = subjectFullName + (shortName.isEmpty() ? "" : " (" + shortName + ")");

                summaryData.add(new SubjectSummary(
                        serialCounter.getAndIncrement(),
                        code,
                        displayName,
                        staff,
                        periods
                ));
            }
        }
        // Set the data to the table
        table.setItems(summaryData);
    }


    // Updated to use full name from timetable grid cell data
    private void updateTimetableGrid(GridPane grid, TimeTableGenerator.Schedule schedule) {
        // Clear existing cells
        List<Node> cellsToRemove = new ArrayList<>();
        for (Node node : grid.getChildren()) {
            Integer colIndex = GridPane.getColumnIndex(node);
            Integer rowIndex = GridPane.getRowIndex(node);
            if (colIndex != null && rowIndex != null && colIndex > 0 && rowIndex > 0) {
                cellsToRemove.add(node);
            }
        }
        grid.getChildren().removeAll(cellsToRemove);

        if (schedule == null || schedule.getTimetable() == null) {
            for (int i = 0; i < DAYS_OF_WEEK.size(); i++) {
                for (int j = 1; j <= PERIODS_PER_DAY; j++) {
                    grid.add(createEmptyDataCell(), j, i + 1);
                }
            }
            return;
        }

        Map<String, List<String>> timetable = schedule.getTimetable();
        long animationDelay = 0;

        for (int i = 0; i < DAYS_OF_WEEK.size(); i++) {
            String day = DAYS_OF_WEEK.get(i);
            List<String> daySchedule = timetable.get(day);

             if (daySchedule == null) { /* ... error handling ... */ continue; }

            for (int j = 0; j < daySchedule.size() && j < PERIODS_PER_DAY; j++) {
                String subjectFullName = daySchedule.get(j); // This is the key
                if (subjectFullName == null) { subjectFullName = "ERROR"; }

                Node cellNode = createTimetableCell(subjectFullName); // Pass the full name

                FadeTransition fadeIn = new FadeTransition(Duration.millis(350), cellNode);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.setDelay(Duration.millis(animationDelay));
                animationDelay += 15;

                grid.add(cellNode, j + 1, i + 1);
                fadeIn.play();
            }
             // Fill remainder
             for (int j = daySchedule.size(); j < PERIODS_PER_DAY; j++) { grid.add(createEmptyDataCell(), j + 1, i + 1); }
        }
    }


    // Uses full name key to look up display info (Short Name) and tooltip info (Full, Code, etc.)
    private Node createTimetableCell(String subjectFullName) { // Parameter is the full name (key)
        VBox cellContainer = new VBox(3);
        cellContainer.setAlignment(Pos.CENTER);
        cellContainer.setPadding(new Insets(5));
        cellContainer.setMinHeight(60);
        cellContainer.setMaxWidth(Double.MAX_VALUE);
        cellContainer.setMaxHeight(Double.MAX_VALUE);

        // Look up Short Name for display in the cell
        String shortName = subjectShortNameMap.getOrDefault(subjectFullName, subjectFullName); // Fallback

        Label subjectLabel = new Label(shortName); // Display short name
        subjectLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        subjectLabel.setWrapText(true);
        subjectLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        cellContainer.getChildren().add(subjectLabel);

        String cellStyle = "-fx-background-radius: 3; -fx-border-radius: 3; -fx-border-width: 1px; ";
        String tooltipText = "";

        // Get other details using the full name key from the main UI maps
        boolean isLab = isLabMap.getOrDefault(subjectFullName, false);
        String staffName = subjectStaffMap.getOrDefault(subjectFullName, "N/A");
        String subjectCode = subjectCodeMap.getOrDefault(subjectFullName, "N/A");
        String subjectType = isLab ? "Lab" : "Theory";

        Label staffLabel = new Label(staffName);
        staffLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        staffLabel.setTextFill(Color.web("#555"));
        staffLabel.setWrapText(true);
        staffLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        cellContainer.getChildren().add(staffLabel);

        if (isLab) { /* ... set LAB_COLOR style ... */
            cellStyle += "-fx-background-color: " + LAB_COLOR + "; -fx-border-color: " + SECONDARY_COLOR + ";";
        } else { /* ... set THEORY_COLOR style ... */
            cellStyle += "-fx-background-color: " + THEORY_COLOR + "; -fx-border-color: " + ACCENT_COLOR + ";";
        }
        subjectLabel.setTextFill(Color.web(TEXT_COLOR));

        // Prepare enhanced tooltip
        tooltipText = String.format(
                "Subject: %s (%s)\nCode: %s\nType: %s\nStaff: %s",
                subjectFullName, shortName, subjectCode, subjectType, staffName
        );

        cellContainer.setStyle(cellStyle);

        if (!tooltipText.isEmpty()) {
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            tooltip.setShowDelay(Duration.millis(300));
            Tooltip.install(cellContainer, tooltip);
        }

        return cellContainer;
    }

    // --- Helper methods (getNodeFromGridPane, showAlert, showNotification) ---
    // --- (No changes needed) ---
    private Node getNodeFromGridPane(GridPane gridPane, int col, int row) { /* ... */ return null;}
    private void showAlert(String title, String message, Alert.AlertType type) { /* ... */ }
    private void showNotification(String title, String message, String backgroundColor) { /* ... */ }


    // --- Inner Helper Class for Subject Input Row (No changes from previous version) ---
    private class SubjectRow {
        // ... (Fields: subjectNameField, subjectShortNameField, subjectCodeField, periodsSpinner, typeGroup, staffNameField, removeButton) ...
        // ... (Constructor sets up HBox layout with labels and fields) ...
        // ... (Getters: getSubjectName, getSubjectShortName, getSubjectCode, getPeriods, isLab, getStaffName) ...
        // ... (isValid method checks all fields are non-empty and periods > 0) ...
        private final int subjectNumber;
        private final HBox container;
        private final TextField subjectNameField;
        private final TextField subjectShortNameField;
        private final TextField subjectCodeField;
        private final Spinner<Integer> periodsSpinner;
        private final ToggleGroup typeGroup;
        private final RadioButton theoryRadio;
        private final RadioButton labRadio;
        private final TextField staffNameField;
        private final Button removeButton;
        private final java.util.function.Consumer<SubjectRow> removeCallback;

        public SubjectRow(int number, java.util.function.Consumer<SubjectRow> removeCallback) {
            this.subjectNumber = number;
            this.removeCallback = removeCallback;

            container = new HBox(8); // Adjust spacing
            container.setPadding(new Insets(10));
            container.setAlignment(Pos.CENTER_LEFT);
            container.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-color: " + BORDER_COLOR + "; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 5; " +
                    "-fx-background-radius: 5;"
            );

            container.setOnMouseEntered(e -> container.setStyle(container.getStyle().replace("white", "#f9f9f9")));
            container.setOnMouseExited(e -> container.setStyle(container.getStyle().replace("#f9f9f9", "white")));

            // --- Input Fields ---
            subjectNameField = new TextField();
            subjectNameField.setPromptText("Full Name (e.g., Drone Technology)");
            subjectNameField.setPrefWidth(180); // Wider for full name
            subjectNameField.getStyleClass().add("custom-text-field");

            subjectShortNameField = new TextField(); // New Field
            subjectShortNameField.setPromptText("Short (DT)");
            subjectShortNameField.setPrefWidth(60);
            subjectShortNameField.getStyleClass().add("custom-text-field");
            subjectShortNameField.textProperty().addListener((obs, oldVal, newVal) -> {
                 subjectShortNameField.setText(newVal.toUpperCase());
             });

            subjectCodeField = new TextField(); // New Field
            subjectCodeField.setPromptText("Code (CS301)");
            subjectCodeField.setPrefWidth(80);
            subjectCodeField.getStyleClass().add("custom-text-field");
             subjectCodeField.textProperty().addListener((obs, oldVal, newVal) -> {
                 subjectCodeField.setText(newVal.toUpperCase());
             });

            periodsSpinner = new Spinner<>(1, PERIODS_PER_DAY, 4);
            periodsSpinner.setEditable(true);
            periodsSpinner.setPrefWidth(70); // Slightly narrower
            periodsSpinner.getStyleClass().add("custom-spinner");
            TextFormatter<Integer> formatter = new TextFormatter<>(
                new javafx.util.converter.IntegerStringConverter(), 4,
                change -> change.getControlNewText().matches("\\d*") ? change : null);
            periodsSpinner.getEditor().setTextFormatter(formatter);

            typeGroup = new ToggleGroup();
            theoryRadio = new RadioButton("Theory");
            theoryRadio.setToggleGroup(typeGroup); theoryRadio.setSelected(true);
            theoryRadio.setCursor(javafx.scene.Cursor.HAND);
            labRadio = new RadioButton("Lab");
            labRadio.setToggleGroup(typeGroup);
            labRadio.setCursor(javafx.scene.Cursor.HAND);
            HBox typeBox = new HBox(10, theoryRadio, labRadio);
            typeBox.setAlignment(Pos.CENTER_LEFT);

            staffNameField = new TextField();
            staffNameField.setPromptText("Staff Name");
            staffNameField.setPrefWidth(120); // Slightly narrower
            staffNameField.getStyleClass().add("custom-text-field");

            removeButton = new Button("X");
            removeButton.setStyle("-fx-background-color: " + WARNING_COLOR + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 6 3 6; -fx-background-radius: 15;");
            removeButton.setCursor(javafx.scene.Cursor.HAND);
            removeButton.setOnAction(e -> removeCallback.accept(this));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            container.getChildren().addAll(
                    new Label("Full:"), subjectNameField,
                    new Label("Short:"), subjectShortNameField,
                    new Label("Code:"), subjectCodeField,
                    new Label("Periods:"), periodsSpinner,
                    new Label("Type:"), typeBox,
                    new Label("Staff:"), staffNameField,
                    spacer,
                    removeButton
            );
        }

        public HBox getContainer() { return container; }
        public String getSubjectName() { return subjectNameField.getText().trim(); }
        public String getSubjectShortName() { return subjectShortNameField.getText().trim(); }
        public String getSubjectCode() { return subjectCodeField.getText().trim(); }
        public int getPeriods() {
             try {
                 if (periodsSpinner.isEditable()) return Integer.parseInt(periodsSpinner.getEditor().getText());
                 return periodsSpinner.getValue();
             } catch (NumberFormatException e) { return 0; }
        }
        public boolean isLab() { return labRadio.isSelected(); }
        public String getStaffName() { return staffNameField.getText().trim(); }

        public boolean isValid() {
            int periods = getPeriods();
            return !getSubjectName().isEmpty() &&
                   !getSubjectShortName().isEmpty() &&
                   !getSubjectCode().isEmpty() &&
                   periods > 0 &&
                   !getStaffName().isEmpty() &&
                   typeGroup.getSelectedToggle() != null;
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}