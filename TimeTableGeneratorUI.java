import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
// import javafx.scene.image.Image;
// import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

// Assuming TimeTableGenerator class is available
// import TimeTableGenerator;

public class TimeTableGeneratorUI extends Application {

    // Constants
    private static final int PERIODS_PER_DAY = 8;
    private static final int DAYS_PER_WEEK = 5;
    private static final int TOTAL_PERIODS_PER_WEEK = PERIODS_PER_DAY * DAYS_PER_WEEK; // 40
    private static final List<String> DAYS_OF_WEEK = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");

    // Color palette
    private static final String PRIMARY_COLOR = "#2C3E50";
    private static final String SECONDARY_COLOR = "#3498DB";
    private static final String ACCENT_COLOR = "#1ABC9C";
    private static final String WARNING_COLOR = "#E74C3C";
    private static final String SUCCESS_COLOR = "#2ECC71";
    private static final String TEXT_COLOR = "#34495E";
    private static final String LIGHT_BG_COLOR = "#ECF0F1";
    private static final String BORDER_COLOR = "#BDC3C7";

    // Theory and Lab colors
    private static final String THEORY_COLOR = "#D5F5E3";
    private static final String LAB_COLOR = "#D4E6F1";

    // UI components
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
    private ProgressIndicator progressIndicator;
    private Label statusLabel;

    // Data storage - Using Full Subject Name as the key
    private final Map<String, Boolean> isLabMap = new HashMap<>();
    private final Map<String, String> subjectStaffMap = new HashMap<>();
    private final Map<String, Integer> subjectsWithPeriods = new HashMap<>();
    private final Map<String, String> subjectShortNameMap = new HashMap<>(); // New map for short names
    private final Map<String, String> subjectCodeMap = new HashMap<>();      // New map for codes

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
        outputScrollPane.getStyleClass().add("custom-scroll-pane");

        splitPane.getItems().addAll(inputScrollPane, outputScrollPane);
        splitPane.setDividerPositions(0.42); // Adjust divider position slightly for more input space

        mainLayout.setCenter(splitPane);

        Scene scene = new Scene(mainLayout, 1400, 850); // Slightly wider scene

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createHeader() {
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

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 20, 8, 20));
        footer.setStyle("-fx-background-color: " + LIGHT_BG_COLOR + "; -fx-border-width: 1 0 0 0; -fx-border-color: " + BORDER_COLOR + ";");

        Label versionLabel = new Label("Version 2.3 © 2025"); // Updated version
        versionLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        versionLabel.setTextFill(Color.web(TEXT_COLOR));

        footer.getChildren().add(versionLabel);
        return footer;
    }

    private VBox createInputSection() {
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

    // --- Output Section and Grid Creation (No changes needed here) ---
    private TabPane createOutputSection() { /* ... No changes ... */
        resultTabs = new TabPane();
        resultTabs.getStyleClass().add("custom-tab-pane");

        Tab sectionATab = new Tab("Section A");
        sectionATab.setContent(createTimetableTabContent("Section A", sectionAGrid = createEmptyTimetableGrid(), sectionAFitness = new Label("Fitness: N/A")));

        Tab sectionBTab = new Tab("Section B");
        sectionBTab.setContent(createTimetableTabContent("Section B", sectionBGrid = createEmptyTimetableGrid(), sectionBFitness = new Label("Fitness: N/A")));

        resultTabs.getTabs().addAll(sectionATab, sectionBTab);
        resultTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        resultTabs.setStyle("-fx-tab-min-width: 100px; -fx-tab-max-height: 40px; -fx-font-size: 14px;");

        return resultTabs;
    }
    private Node createTimetableTabContent(String sectionName, GridPane grid, Label fitnessLabel) { /* ... No changes ... */
        VBox container = new VBox(15);
        container.setPadding(new Insets(25));
        container.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label(sectionName + " Timetable");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        HBox legend = createLegend();

        fitnessLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        fitnessLabel.setTextFill(Color.web(TEXT_COLOR));
        HBox fitnessBox = new HBox(fitnessLabel);
        fitnessBox.setAlignment(Pos.CENTER_RIGHT);
        fitnessBox.setPadding(new Insets(10, 0, 0, 0));

        container.getChildren().addAll(titleLabel, legend, grid, fitnessBox);
        return container;
    }
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
    // --- End of Unchanged Output Section ---

    private void addSubjectRow() {
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

    private void removeSubjectRow(SubjectRow rowToRemove) {
        Node rowNode = rowToRemove.getContainer();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), rowNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            subjectsContainer.getChildren().remove(rowNode);
            subjectRows.remove(rowToRemove);
            // When a row is removed, we also need to remove its data from our maps
            // Use the full subject name as the key (assuming it was valid before removal)
            String subjectNameToRemove = rowToRemove.getSubjectName(); // Get name before it's gone
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

    private void generateTimetables() {
        // --- 1. Collect and Validate Input ---
        subjectsWithPeriods.clear();
        isLabMap.clear();
        subjectStaffMap.clear();
        subjectShortNameMap.clear(); // Clear new maps
        subjectCodeMap.clear();      // Clear new maps
        boolean inputValid = true;
        int totalPeriodsRequired = 0;
        Set<String> subjectNames = new HashSet<>(); // To check for duplicate full names
        Set<String> subjectCodes = new HashSet<>(); // To check for duplicate codes (optional check)
        Set<String> subjectShortNames = new HashSet<>();// To check for duplicate short names (optional check)


        if (subjectRows.isEmpty()) {
             showAlert("No Subjects", "Please add at least one subject.", Alert.AlertType.WARNING);
             return;
        }

        for (SubjectRow row : subjectRows) {
            // Assume valid initially, mark invalid if any check fails
            boolean rowIsValid = true;
            // Reset border style
             String originalStyle = "-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1px; -fx-border-radius: 5; -fx-background-radius: 5;";
             if(row.getContainer().getStyle().contains("#f9f9f9")) { // retain hover style if applicable
                 originalStyle = originalStyle.replace("white", "#f9f9f9");
             }
            row.getContainer().setStyle(originalStyle);


            if (!row.isValid()) {
                rowIsValid = false;
            } else {
                String subjectName = row.getSubjectName(); // Full name (Key)
                String shortName = row.getSubjectShortName();
                String code = row.getSubjectCode();
                int periods = row.getPeriods();
                boolean isLab = row.isLab();
                String staffName = row.getStaffName();

                // Check for duplicate Full Name
                if (!subjectNames.add(subjectName)) {
                     showAlert("Duplicate Subject Name", "The full name '" + subjectName + "' is used more than once.", Alert.AlertType.WARNING);
                     rowIsValid = false;
                }
                // Optional: Check for duplicate Short Name
                 if (!subjectShortNames.add(shortName)) {
                     showAlert("Duplicate Short Name", "The short name '" + shortName + "' is used more than once.", Alert.AlertType.WARNING);
                     // rowIsValid = false; // Decide if this is critical enough to stop generation
                 }
                 // Optional: Check for duplicate Code
                 if (!subjectCodes.add(code)) {
                     showAlert("Duplicate Subject Code", "The code '" + code + "' is used more than once.", Alert.AlertType.WARNING);
                     // rowIsValid = false; // Decide if this is critical enough to stop generation
                 }


                if(rowIsValid) {
                    // Store data using full name as key
                    subjectsWithPeriods.put(subjectName, periods);
                    subjectStaffMap.put(subjectName, staffName);
                    isLabMap.put(subjectName, isLab);
                    subjectShortNameMap.put(subjectName, shortName); // Store short name
                    subjectCodeMap.put(subjectName, code);           // Store code
                    totalPeriodsRequired += periods;
                }
            }

             if (!rowIsValid) {
                 inputValid = false; // Mark overall input as invalid
                 // Highlight the invalid row
                 row.getContainer().setStyle(row.getContainer().getStyle() + "; -fx-border-color: " + WARNING_COLOR + "; -fx-border-width: 1.5px;");
             }
        }

        if (!inputValid) {
            showAlert("Invalid Input", "Please correct the highlighted fields or fill all required fields.\nCheck for duplicate names/codes.", Alert.AlertType.WARNING);
            return;
        }

        // Validate Total Periods
        if (totalPeriodsRequired != TOTAL_PERIODS_PER_WEEK) {
            showAlert("Incorrect Total Periods",
                      String.format("The total number of periods entered (%d) must be exactly %d. " +
                                    "Please adjust the periods per subject.",
                                    totalPeriodsRequired, TOTAL_PERIODS_PER_WEEK),
                      Alert.AlertType.ERROR);
            return;
        }

        // --- 2. Prepare for Background Task ---
        // (Same as before)
        generateBtn.setDisable(true);
        generateBtn.setText("Generating...");
        progressIndicator.setVisible(true);
        statusLabel.setText("Initializing Genetic Algorithm...");
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);

        updateTimetableGrid(sectionAGrid, null);
        updateTimetableGrid(sectionBGrid, null);
        sectionAFitness.setText("Fitness: N/A");
        sectionBFitness.setText("Fitness: N/A");

        // --- 3. Run Genetic Algorithm in Background Thread ---
        // (Pass the necessary maps - shortNameMap and codeMap are not needed by the GA itself)
        AtomicReference<TimeTableGenerator.Schedule> scheduleARef = new AtomicReference<>();
        AtomicReference<TimeTableGenerator.Schedule> scheduleBRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        Thread generationThread = new Thread(() -> {
            try {
                // Create copies of maps needed by the GA
                Map<String, Integer> periodsCopy = new HashMap<>(subjectsWithPeriods);
                Map<String, String> staffCopy = new HashMap<>(subjectStaffMap);
                Map<String, Boolean> labCopy = new HashMap<>(isLabMap);

                Platform.runLater(() -> statusLabel.setText("Generating Section A timetable..."));
                TimeTableGenerator.Schedule scheduleA =
                        TimeTableGenerator.runGeneticAlgorithm(periodsCopy, staffCopy, labCopy, null);
                scheduleARef.set(scheduleA);
                System.out.println("Section A generation complete. Fitness: " + (scheduleA != null ? scheduleA.getFitness() : "N/A"));


                Platform.runLater(() -> statusLabel.setText("Generating Section B timetable (considering Section A)..."));
                TimeTableGenerator.Schedule scheduleB =
                        TimeTableGenerator.runGeneticAlgorithm(periodsCopy, staffCopy, labCopy, scheduleA);
                scheduleBRef.set(scheduleB);
                System.out.println("Section B generation complete. Fitness: " + (scheduleB != null ? scheduleB.getFitness() : "N/A"));


                // --- 4. Update UI on JavaFX Thread ---
                Platform.runLater(() -> {
                    TimeTableGenerator.Schedule finalScheduleA = scheduleARef.get();
                    TimeTableGenerator.Schedule finalScheduleB = scheduleBRef.get();

                    // Pass the main UI maps (including shortNameMap, codeMap) for display
                    updateTimetableGrid(sectionAGrid, finalScheduleA);
                    updateTimetableGrid(sectionBGrid, finalScheduleB);

                    sectionAFitness.setText(String.format("Fitness: %d", (finalScheduleA != null ? finalScheduleA.getFitness() : Integer.MIN_VALUE)));
                    sectionBFitness.setText(String.format("Fitness: %d", (finalScheduleB != null ? finalScheduleB.getFitness() : Integer.MIN_VALUE)));

                    generateBtn.setDisable(false);
                    generateBtn.setText("Generate Timetables");
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Generation complete!");
                    statusLabel.setManaged(false);

                    showNotification("Success", "Timetables generated successfully!", SUCCESS_COLOR);
                    resultTabs.getSelectionModel().select(0);
                });

            } catch (Exception ex) {
                errorRef.set(ex);
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Generation Error", "An unexpected error occurred during timetable generation: \n" + ex.getMessage(), Alert.AlertType.ERROR);
                    generateBtn.setDisable(false);
                    generateBtn.setText("Generate Timetables");
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Generation failed.");
                    statusLabel.setManaged(false);
                });
            }
        });
        generationThread.setDaemon(true);
        generationThread.start();
    }


    // Update timetable display to use short names and enhance tooltips
    private void updateTimetableGrid(GridPane grid, TimeTableGenerator.Schedule schedule) {
        // Clear existing cells (same as before)
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
                String subjectFullName = daySchedule.get(j); // The key is the full name
                if (subjectFullName == null) { /* ... error handling ... */ subjectFullName = "ERROR"; }

                // *** Use the subjectFullName to look up display info ***
                Node cellNode = createTimetableCell(subjectFullName);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(350), cellNode);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.setDelay(Duration.millis(animationDelay));
                animationDelay += 15;

                grid.add(cellNode, j + 1, i + 1);
                fadeIn.play();
            }
             // Fill remainder (same as before)
             for (int j = daySchedule.size(); j < PERIODS_PER_DAY; j++) { /* ... */ }
        }
    }

    // Updated to display SHORT NAME in cell, FULL NAME and CODE in tooltip
    private Node createTimetableCell(String subjectFullName) { // Parameter is now the full name (key)
        VBox cellContainer = new VBox(3);
        cellContainer.setAlignment(Pos.CENTER);
        cellContainer.setPadding(new Insets(5));
        cellContainer.setMinHeight(60);
        cellContainer.setMaxWidth(Double.MAX_VALUE);
        cellContainer.setMaxHeight(Double.MAX_VALUE);

        // *** Look up Short Name for display ***
        String shortName = subjectShortNameMap.getOrDefault(subjectFullName, subjectFullName); // Fallback to full name

        Label subjectLabel = new Label(shortName); // Display short name
        subjectLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        subjectLabel.setWrapText(true);
        subjectLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        cellContainer.getChildren().add(subjectLabel);

        String cellStyle = "-fx-background-radius: 3; -fx-border-radius: 3; -fx-border-width: 1px; ";
        String tooltipText = "";

        // Get other details using the full name key
        boolean isLab = isLabMap.getOrDefault(subjectFullName, false);
        String staffName = subjectStaffMap.getOrDefault(subjectFullName, "N/A");
        String subjectCode = subjectCodeMap.getOrDefault(subjectFullName, "N/A"); // Get the code
        String subjectType = isLab ? "Lab" : "Theory";

        // Add staff label
        Label staffLabel = new Label(staffName);
        staffLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        staffLabel.setTextFill(Color.web("#555"));
        staffLabel.setWrapText(true);
        staffLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        cellContainer.getChildren().add(staffLabel);

        // Set background and border color based on type
        if (isLab) {
            cellStyle += "-fx-background-color: " + LAB_COLOR + "; -fx-border-color: " + SECONDARY_COLOR + ";";
        } else {
            cellStyle += "-fx-background-color: " + THEORY_COLOR + "; -fx-border-color: " + ACCENT_COLOR + ";";
        }
        subjectLabel.setTextFill(Color.web(TEXT_COLOR));

        // *** Prepare ENHANCED tooltip ***
        tooltipText = String.format(
                "Subject: %s (%s)\nCode: %s\nType: %s\nStaff: %s",
                subjectFullName, // Show full name in tooltip
                shortName,       // Also show short name for reference
                subjectCode,     // Show code
                subjectType,
                staffName
        );

        cellContainer.setStyle(cellStyle);

        // Add tooltip
        if (!tooltipText.isEmpty()) {
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            tooltip.setShowDelay(Duration.millis(300));
            Tooltip.install(cellContainer, tooltip);
        }

        return cellContainer;
    }


    // --- Helper methods (getNodeFromGridPane, showAlert, showNotification) ---
    // --- (No changes needed in these) ---
     private Node getNodeFromGridPane(GridPane gridPane, int col, int row) { /* ... No changes ... */
        for (Node node : gridPane.getChildren()) {
            Integer columnIndex = GridPane.getColumnIndex(node);
            Integer rowIndex = GridPane.getRowIndex(node);
            int nodeCol = (columnIndex == null) ? 0 : columnIndex;
            int nodeRow = (rowIndex == null) ? 0 : rowIndex;
            if (nodeCol == col && nodeRow == row) {
                return node;
            }
        }
        return null;
     }
     private void showAlert(String title, String message, Alert.AlertType type) { /* ... No changes ... */
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("custom-alert");
        alert.showAndWait();
     }
     private void showNotification(String title, String message, String backgroundColor) { /* ... No changes ... */
        Stage notificationStage = new Stage();
        if (inputContainer.getScene() != null && inputContainer.getScene().getWindow() != null) {
             notificationStage.initOwner(inputContainer.getScene().getWindow());
        }
        notificationStage.initStyle(StageStyle.UNDECORATED);
        notificationStage.setAlwaysOnTop(true);

        VBox notificationBox = new VBox(10);
        notificationBox.setPadding(new Insets(15));
        notificationBox.setStyle(
                "-fx-background-color: " + backgroundColor + "; " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);"
        );
        notificationBox.setMinWidth(250);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);

        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setWrapText(true);

        notificationBox.getChildren().addAll(titleLabel, messageLabel);

        Scene scene = new Scene(notificationBox);
        scene.setFill(Color.TRANSPARENT);
        notificationStage.setScene(scene);

        notificationStage.setOnShown(event -> {
             try {
                Stage ownerStage = (Stage) inputContainer.getScene().getWindow();
                double ownerX = ownerStage.getX();
                double ownerY = ownerStage.getY();
                double ownerWidth = ownerStage.getWidth();
                notificationStage.setX(ownerX + ownerWidth - notificationBox.getWidth() - 30);
                notificationStage.setY(ownerY + 30);
             } catch (Exception e) {
                 notificationStage.centerOnScreen();
             }
        });
        notificationStage.show();


        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), notificationBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(3));
        fadeOut.setOnFinished(e -> notificationStage.close());
        fadeOut.play();
     }


    // --- Inner Helper Class for Subject Input Row (Updated) ---
    private class SubjectRow {
        private final int subjectNumber;
        private final HBox container;
        private final TextField subjectNameField;      // Full Name (Key)
        private final TextField subjectShortNameField; // New Field
        private final TextField subjectCodeField;      // New Field
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
            // Listener to auto-uppercase short name
            subjectShortNameField.textProperty().addListener((obs, oldVal, newVal) -> {
                 subjectShortNameField.setText(newVal.toUpperCase());
             });


            subjectCodeField = new TextField(); // New Field
            subjectCodeField.setPromptText("Code (CS301)");
            subjectCodeField.setPrefWidth(80);
            subjectCodeField.getStyleClass().add("custom-text-field");
            // Listener to auto-uppercase code
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

            // --- Updated Layout in HBox ---
            container.getChildren().addAll(
                    new Label("Full:"), subjectNameField,
                    new Label("Short:"), subjectShortNameField, // Add Short Name field
                    new Label("Code:"), subjectCodeField,      // Add Code field
                    new Label("Periods:"), periodsSpinner,
                    new Label("Type:"), typeBox,
                    new Label("Staff:"), staffNameField,
                    spacer,
                    removeButton
            );
        }

        // --- Getters ---
        public HBox getContainer() { return container; }
        public String getSubjectName() { return subjectNameField.getText().trim(); } // Full Name (Key)
        public String getSubjectShortName() { return subjectShortNameField.getText().trim(); } // New Getter
        public String getSubjectCode() { return subjectCodeField.getText().trim(); }       // New Getter
        public int getPeriods() {
             try {
                 if (periodsSpinner.isEditable()) return Integer.parseInt(periodsSpinner.getEditor().getText());
                 return periodsSpinner.getValue();
             } catch (NumberFormatException e) { return 0; }
        }
        public boolean isLab() { return labRadio.isSelected(); }
        public String getStaffName() { return staffNameField.getText().trim(); }

        // --- Updated Validation ---
        public boolean isValid() {
            int periods = getPeriods();
            return !getSubjectName().isEmpty() &&
                   !getSubjectShortName().isEmpty() && // Validate Short Name
                   !getSubjectCode().isEmpty() &&      // Validate Code
                   periods > 0 &&
                   !getStaffName().isEmpty() &&
                   typeGroup.getSelectedToggle() != null;
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}