package com.pluralsight;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LedgerGUI extends Application {

    private final List<Transaction> allTransactions = new ArrayList<>();
    private final ObservableList<Transaction> displayedTransactions = FXCollections.observableArrayList();
    private Map<Transaction, Double> runningBalanceMap = new IdentityHashMap<>();

    private BorderPane rootPane;
    private ScrollPane ledgerSidebarNode;
    private ScrollPane analyticsSidebarNode;
    private StackPane workspaceStack;

    private Node ledgerViewNode;
    private ScrollPane analyticsViewNode;
    private ScrollPane cfoViewNode;

    private Button segBtnLedger;
    private Button segBtnAnalytics;
    private Button segBtnCfo;
    private final List<Button> sidebarNavButtons = new ArrayList<>();

    private Label totalBalanceValue;
    private Label totalBalanceSub;
    private Label totalDepositsValue;
    private Label totalDepositsSub;
    private Label totalPaymentsValue;
    private Label totalPaymentsSub;
    private Label activeViewTitle;
    private Label recordCountBadge;
    private Label statusFooter;
    private ProgressBar cashFlowBar;
    private Label cashFlowRatioLabel;

    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private TextField descSearchField;
    private ComboBox<String> vendorComboField;
    private TextField minAmountField;
    private TextField maxAmountField;

    private VBox analyticsContainer;
    private VBox cfoToolsContainer;
    private double taxRatePercent = 20.0;
    private double revenueGoal = 20000.0;
    private String analyticsScope = "All Historical Data";
    private boolean analyticsSidebarHidden = false;
    private Stage mainStage;

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        loadFromCsv();

        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #0B0F19;");

        ledgerSidebarNode = buildEnterpriseSidebar();
        rebuildAnalyticsSidebar();

        rootPane.setLeft(ledgerSidebarNode);
        rootPane.setCenter(buildEnterpriseWorkspace());

        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        double appWidth = Math.min(1380, screenBounds.getWidth() * 0.95);
        double appHeight = Math.min(820, screenBounds.getHeight() * 0.93);

        Scene scene = new Scene(rootPane, appWidth, appHeight);
        scene.getStylesheets().add(createEnterpriseStylesheet());

        Image logoImg = loadLogoImage();
        if (logoImg != null) {
            primaryStage.getIcons().add(logoImg);
        }

        primaryStage.setTitle("VisualTizita Studio — Financial OS");
        primaryStage.setScene(scene);
        primaryStage.setX((screenBounds.getWidth() - appWidth) / 2);
        primaryStage.setY((screenBounds.getHeight() - appHeight) / 2);

        showAllEntries();

        StudioSplashScreen.show(null, primaryStage::show);
    }

    private void rebuildAnalyticsSidebar() {
        analyticsSidebarNode = StudioProFeatures.buildAnalyticsSidebar(
                loadLogoImage(),
                taxRatePercent,
                revenueGoal,
                analyticsScope,
                newRate -> {
                    taxRatePercent = newRate;
                    refreshAnalyticsTab();
                    refreshCfoToolsTab();
                },
                newGoal -> {
                    revenueGoal = newGoal;
                    refreshAnalyticsTab();
                },
                newScope -> {
                    analyticsScope = newScope;
                    refreshAnalyticsTab();
                },
                () -> StudioProFeatures.generateExecutiveReportHtml(allTransactions, taxRatePercent),
                () -> statusFooter.setText(StudioProFeatures.createCsvBackupSnapshot()),
                this::toggleAnalyticsSidebar,
                () -> switchWorkspacePage(0)
        );
    }

    private void toggleAnalyticsSidebar() {
        analyticsSidebarHidden = !analyticsSidebarHidden;
        rootPane.setLeft(analyticsSidebarHidden ? null : analyticsSidebarNode);
        refreshAnalyticsTab();
    }

    private ScrollPane buildEnterpriseSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(20, 16, 20, 16));
        sidebar.setPrefWidth(248);
        sidebar.setStyle("-fx-background-color: #070A12;");

        // Brand Identity Card with Circular Gold-Ringed Avatar
        HBox brandHeader = new HBox(12);
        brandHeader.setAlignment(Pos.CENTER_LEFT);
        brandHeader.setPadding(new Insets(6, 8, 14, 8));
        brandHeader.setStyle("-fx-background-color: #0F1626; -fx-background-radius: 12; -fx-border-color: rgba(212,175,55,0.22); -fx-border-radius: 12;");

        Image logoImg = loadLogoImage();
        if (logoImg != null) {
            ImageView logoView = new ImageView(logoImg);
            logoView.setFitWidth(46);
            logoView.setFitHeight(46);
            logoView.setSmooth(true);
            Circle clip = new Circle(23, 23, 23);
            logoView.setClip(clip);

            StackPane avatarRing = new StackPane(logoView);
            avatarRing.setPrefSize(50, 50);
            avatarRing.setStyle("-fx-background-color: #D4AF37; -fx-background-radius: 25; -fx-padding: 2;");
            brandHeader.getChildren().add(avatarRing);
        }

        VBox brandTitles = new VBox(2);
        Label brandTitle = new Label("VisualTizita");
        brandTitle.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 15));
        brandTitle.setTextFill(Color.web("#F8FAFC"));

        Label brandSub = new Label("FINTECH LEDGER OS");
        brandSub.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        brandSub.setTextFill(Color.web("#D4AF37"));
        brandTitles.getChildren().addAll(brandTitle, brandSub);
        brandHeader.getChildren().add(brandTitles);

        // Sleek Action Buttons
        HBox quickActions = new HBox(8);
        quickActions.setPadding(new Insets(8, 0, 6, 0));
        Button btnDeposit = createPrimaryCtaButton("＋ Deposit", true);
        btnDeposit.setOnAction(e -> openTransactionModal(true));

        Button btnPayment = createPrimaryCtaButton("－ Payment", false);
        btnPayment.setOnAction(e -> openTransactionModal(false));
        HBox.setHgrow(btnDeposit, Priority.ALWAYS);
        HBox.setHgrow(btnPayment, Priority.ALWAYS);
        quickActions.getChildren().addAll(btnDeposit, btnPayment);

        Label ledgerHeader = createSectionHeader("LEDGER VIEWS");
        Button btnAll = createNavButton("◈   All Transactions", true);
        btnAll.setOnAction(e -> { setActiveNav(btnAll); showAllEntries(); });

        Button btnDeposits = createNavButton("↗   Inbound Deposits", false);
        btnDeposits.setOnAction(e -> { setActiveNav(btnDeposits); showDepositsOnly(); });

        Button btnPayments = createNavButton("↘   Outbound Payments", false);
        btnPayments.setOnAction(e -> { setActiveNav(btnPayments); showPaymentsOnly(); });

        Label reportsHeader = createSectionHeader("FINANCIAL PERIODS");
        Button btnMTD = createNavButton("◷   Month to Date (MTD)", false);
        btnMTD.setOnAction(e -> { setActiveNav(btnMTD); filterMonthToDate(); });

        Button btnPrevMonth = createNavButton("◴   Previous Month", false);
        btnPrevMonth.setOnAction(e -> { setActiveNav(btnPrevMonth); filterPreviousMonth(); });

        Button btnYTD = createNavButton("◵   Year to Date (YTD)", false);
        btnYTD.setOnAction(e -> { setActiveNav(btnYTD); filterYearToDate(); });

        Button btnPrevYear = createNavButton("◶   Previous Year", false);
        btnPrevYear.setOnAction(e -> { setActiveNav(btnPrevYear); filterPreviousYear(); });

        Label toolsHeader = createSectionHeader("DATA & STORAGE");
        Button btnExport = createNavButton("⇩   Export Current CSV", false);
        btnExport.setOnAction(e -> exportCurrentViewToCsv());

        Button btnReload = createNavButton("↻   Sync from Disk", false);
        btnReload.setOnAction(e -> {
            loadFromCsv();
            setActiveNav(btnAll);
            showAllEntries();
            statusFooter.setText("Synced transactions.csv from disk.");
        });

        sidebar.getChildren().addAll(
                brandHeader,
                quickActions,
                createSubtleDivider(),
                ledgerHeader, btnAll, btnDeposits, btnPayments,
                createSubtleDivider(),
                reportsHeader, btnMTD, btnPrevMonth, btnYTD, btnPrevYear,
                createSubtleDivider(),
                toolsHeader, btnExport, btnReload
        );

        ScrollPane scrollSidebar = new ScrollPane(sidebar);
        scrollSidebar.setFitToWidth(true);
        scrollSidebar.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollSidebar.getStyleClass().add("edge-to-edge-scroll");
        scrollSidebar.setStyle("-fx-background: #070A12; -fx-background-color: #070A12; -fx-border-color: #161F30; -fx-border-width: 0 1 0 0;");
        scrollSidebar.setPrefWidth(258);
        return scrollSidebar;
    }

    private BorderPane buildEnterpriseWorkspace() {
        BorderPane workspace = new BorderPane();
        workspace.setStyle("-fx-background-color: #0B0F19;");

        // Top Segmented Executive Header Bar (Replaces ugly browser tabs)
        HBox topHeader = new HBox(16);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        topHeader.setPadding(new Insets(14, 26, 14, 26));
        topHeader.setStyle("-fx-background-color: #0B0F19; -fx-border-color: #161F30; -fx-border-width: 0 0 1 0;");

        HBox segmentedBar = new HBox(4);
        segmentedBar.setPadding(new Insets(4));
        segmentedBar.setStyle("-fx-background-color: #070A12; -fx-background-radius: 10; -fx-border-color: #1A2438; -fx-border-radius: 10;");

        segBtnLedger = createSegmentButton("Ledger & Search", true);
        segBtnAnalytics = createSegmentButton("Executive Analytics", false);
        segBtnCfo = createSegmentButton("CFO Suite & Audit", false);

        segBtnLedger.setOnAction(e -> switchWorkspacePage(0));
        segBtnAnalytics.setOnAction(e -> switchWorkspacePage(1));
        segBtnCfo.setOnAction(e -> switchWorkspacePage(2));

        segmentedBar.getChildren().addAll(segBtnLedger, segBtnAnalytics, segBtnCfo);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label liveStatusPill = new Label("●  LIVE CSV SYNC");
        liveStatusPill.setStyle("-fx-background-color: rgba(16,185,129,0.12); -fx-text-fill: #34D399; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 5 12; -fx-background-radius: 20; -fx-border-color: rgba(52,211,153,0.3); -fx-border-radius: 20;");

        topHeader.getChildren().addAll(segmentedBar, topSpacer, liveStatusPill);

        // Page 1: Ledger Workspace (No outer ScrollPane so TableView scrolls natively without double scrollbars)
        VBox mainLedgerBox = new VBox(14);
        mainLedgerBox.setPadding(new Insets(18, 26, 16, 26));

        HBox kpiRow = new HBox(16);
        totalBalanceValue = new Label("$0.00");
        totalBalanceSub = new Label("Available Studio Equity");
        totalDepositsValue = new Label("+$0.00");
        totalDepositsSub = new Label("0 Inbound Credits");
        totalPaymentsValue = new Label("-$0.00");
        totalPaymentsSub = new Label("0 Outbound Debits");

        VBox balanceCard = createExecutiveKpiCard("NET STUDIO BALANCE", totalBalanceValue, totalBalanceSub, "#D4AF37", "rgba(212,175,55,0.14)");
        VBox depositCard = createExecutiveKpiCard("GROSS INBOUND CREDITS", totalDepositsValue, totalDepositsSub, "#34D399", "rgba(52,211,153,0.14)");
        VBox paymentCard = createExecutiveKpiCard("TOTAL OUTBOUND DEBITS", totalPaymentsValue, totalPaymentsSub, "#F87171", "rgba(248,113,113,0.14)");
        HBox.setHgrow(balanceCard, Priority.ALWAYS);
        HBox.setHgrow(depositCard, Priority.ALWAYS);
        HBox.setHgrow(paymentCard, Priority.ALWAYS);
        kpiRow.getChildren().addAll(balanceCard, depositCard, paymentCard);

        VBox cashFlowBox = buildCashFlowBar();
        VBox searchCard = buildCustomSearchPanel();

        HBox tableHeaderBar = new HBox(10);
        tableHeaderBar.setAlignment(Pos.CENTER_LEFT);
        activeViewTitle = new Label("All Transactions");
        activeViewTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        activeViewTitle.setTextFill(Color.web("#F8FAFC"));

        recordCountBadge = new Label("0 records");
        recordCountBadge.setStyle("-fx-background-color: #162032; -fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label hintLabel = new Label("Right-click row: Edit / Printable Invoice / Delete   •   Double-click: Voucher");
        hintLabel.setFont(Font.font("Segoe UI", 11));
        hintLabel.setTextFill(Color.web("#64748B"));

        tableHeaderBar.getChildren().addAll(activeViewTitle, recordCountBadge, headerSpacer, hintLabel);

        TableView<Transaction> table = buildTransactionTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        statusFooter = new Label("Connected to local ledger storage (transactions.csv)");
        statusFooter.setTextFill(Color.web("#64748B"));
        statusFooter.setFont(Font.font("Segoe UI", 11));

        mainLedgerBox.getChildren().addAll(kpiRow, cashFlowBox, searchCard, tableHeaderBar, table, statusFooter);
        ledgerViewNode = mainLedgerBox;

        // Page 2: Analytics Dashboard
        analyticsContainer = new VBox(18);
        analyticsContainer.setPadding(new Insets(20, 26, 26, 26));
        analyticsViewNode = wrapInCleanScrollPane(analyticsContainer);

        // Page 3: CFO Simulator & Audit
        cfoToolsContainer = new VBox(18);
        cfoToolsContainer.setPadding(new Insets(20, 26, 26, 26));
        cfoViewNode = wrapInCleanScrollPane(cfoToolsContainer);

        workspaceStack = new StackPane(ledgerViewNode);
        workspace.setTop(topHeader);
        workspace.setCenter(workspaceStack);
        return workspace;
    }

    private void switchWorkspacePage(int index) {
        styleSegmentButton(segBtnLedger, index == 0);
        styleSegmentButton(segBtnAnalytics, index == 1);
        styleSegmentButton(segBtnCfo, index == 2);

        workspaceStack.getChildren().clear();
        if (index == 0) {
            rootPane.setLeft(ledgerSidebarNode);
            workspaceStack.getChildren().add(ledgerViewNode);
        } else if (index == 1) {
            rootPane.setLeft(analyticsSidebarHidden ? null : analyticsSidebarNode);
            refreshAnalyticsTab();
            workspaceStack.getChildren().add(analyticsViewNode);
        } else {
            rootPane.setLeft(analyticsSidebarHidden ? null : analyticsSidebarNode);
            refreshCfoToolsTab();
            workspaceStack.getChildren().add(cfoViewNode);
        }
    }

    private ScrollPane wrapInCleanScrollPane(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("edge-to-edge-scroll");
        sp.setStyle("-fx-background: #0B0F19; -fx-background-color: #0B0F19;");
        return sp;
    }

    private void refreshAnalyticsTab() {
        StudioProFeatures.populateAnalyticsDashboard(
                analyticsContainer,
                allTransactions,
                taxRatePercent,
                revenueGoal,
                analyticsScope,
                analyticsSidebarHidden,
                this::toggleAnalyticsSidebar
        );
    }

    private void refreshCfoToolsTab() {
        if (cfoToolsContainer == null) return;
        StudioProFeatures.populateCfoToolsTab(
                cfoToolsContainer,
                allTransactions,
                taxRatePercent,
                newTx -> {
                    allTransactions.add(newTx);
                    appendToCsv(newTx);
                    refreshVendorAutocomplete();
                    showAllEntries();
                    refreshCfoToolsTab();
                    statusFooter.setText("Posted from CFO Suite: " + newTx.getDescription());
                },
                () -> {
                    for (Transaction t : StudioProFeatures.generateRealisticStudioTransactions()) {
                        allTransactions.add(t);
                        appendToCsv(t);
                    }
                    refreshVendorAutocomplete();
                    showAllEntries();
                    refreshCfoToolsTab();
                    statusFooter.setText("Injected 6 realistic VisualTizita studio transactions!");
                },
                () -> {
                    String msg = StudioProFeatures.createCsvBackupSnapshot();
                    statusFooter.setText(msg);
                    showAlert("CSV Backup Complete", msg);
                }
        );
    }

    private VBox buildCashFlowBar() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(10, 18, 12, 18));
        box.setStyle("-fx-background-color: #101726; -fx-background-radius: 12; -fx-border-color: #1C283E; -fx-border-radius: 12;");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("CASH FLOW RATIO & LIQUIDITY VELOCITY");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web("#64748B"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cashFlowRatioLabel = new Label("100% Positive Flow");
        cashFlowRatioLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        cashFlowRatioLabel.setTextFill(Color.web("#D4AF37"));

        topRow.getChildren().addAll(lbl, spacer, cashFlowRatioLabel);

        cashFlowBar = new ProgressBar(1.0);
        cashFlowBar.setMaxWidth(Double.MAX_VALUE);
        cashFlowBar.setPrefHeight(7);
        cashFlowBar.getStyleClass().add("fintech-progress");

        box.getChildren().addAll(topRow, cashFlowBar);
        return box;
    }

    private VBox buildCustomSearchPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setStyle("-fx-background-color: #101726; -fx-background-radius: 12; -fx-border-color: #1C283E; -fx-border-radius: 12;");

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("INTELLIGENT MULTI-PARAMETER FILTER");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        title.setTextFill(Color.web("#D4AF37"));

        Label sub = new Label("Instant live filtering across description, vendor, date range, and transaction bounds");
        sub.setFont(Font.font("Segoe UI", 11));
        sub.setTextFill(Color.web("#64748B"));
        headerRow.getChildren().addAll(title, sub);

        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        descSearchField = createStyledTextField("⌕  Search description...");
        descSearchField.setPrefWidth(210);

        vendorComboField = new ComboBox<>();
        vendorComboField.setEditable(true);
        vendorComboField.setPromptText("Vendor / Client...");
        vendorComboField.setPrefWidth(190);
        vendorComboField.getStyleClass().add("dark-combo");
        refreshVendorAutocomplete();

        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setPrefWidth(135);
        startDatePicker.getStyleClass().add("dark-datepicker");

        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End Date");
        endDatePicker.setPrefWidth(135);
        endDatePicker.getStyleClass().add("dark-datepicker");

        minAmountField = createStyledTextField("Min $");
        minAmountField.setPrefWidth(90);

        maxAmountField = createStyledTextField("Max $");
        maxAmountField.setPrefWidth(90);

        HBox.setHgrow(descSearchField, Priority.ALWAYS);
        HBox.setHgrow(vendorComboField, Priority.ALWAYS);

        descSearchField.textProperty().addListener((obs, oldV, newV) -> runCustomSearch());
        vendorComboField.getEditor().textProperty().addListener((obs, oldV, newV) -> runCustomSearch());
        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> runCustomSearch());
        endDatePicker.valueProperty().addListener((obs, oldV, newV) -> runCustomSearch());
        minAmountField.textProperty().addListener((obs, oldV, newV) -> runCustomSearch());
        maxAmountField.textProperty().addListener((obs, oldV, newV) -> runCustomSearch());

        Button btnRunSearch = new Button("Filter");
        btnRunSearch.setMinWidth(76);
        btnRunSearch.setStyle("-fx-background-color: #D4AF37; -fx-text-fill: #070A12; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 7 14; -fx-cursor: hand;");
        btnRunSearch.setOnAction(e -> runCustomSearch());

        Button btnClear = new Button("Reset");
        btnClear.setMinWidth(70);
        btnClear.setStyle("-fx-background-color: #192338; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #263552; -fx-border-radius: 8; -fx-padding: 7 12; -fx-cursor: hand;");
        btnClear.setOnAction(e -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            descSearchField.clear();
            vendorComboField.getEditor().clear();
            vendorComboField.setValue(null);
            minAmountField.clear();
            maxAmountField.clear();
            showAllEntries();
        });

        filterBar.getChildren().addAll(
                descSearchField, vendorComboField,
                startDatePicker, endDatePicker,
                minAmountField, maxAmountField,
                btnRunSearch, btnClear
        );

        box.getChildren().addAll(headerRow, filterBar);
        return box;
    }

    private void refreshVendorAutocomplete() {
        if (vendorComboField == null) return;
        List<String> vendors = allTransactions.stream()
                .map(Transaction::getVendor)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        vendorComboField.getItems().setAll(vendors);
    }

    private TableView<Transaction> buildTransactionTable() {
        TableView<Transaction> table = new TableView<>(displayedTransactions);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("enterprise-table");

        Label emptyLabel = new Label("No matching ledger records found.");
        emptyLabel.setTextFill(Color.web("#64748B"));
        table.setPlaceholder(emptyLabel);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        TableColumn<Transaction, String> colDate = new TableColumn<>("DATE");
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDate().toString()));
        colDate.setMaxWidth(1f * Integer.MAX_VALUE * 12);
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: bold; -fx-font-size: 12px;");
                }
            }
        });

        TableColumn<Transaction, String> colTime = new TableColumn<>("TIMESTAMP");
        colTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTime().format(timeFmt)));
        colTime.setMaxWidth(1f * Integer.MAX_VALUE * 10);
        colTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #64748B; -fx-font-family: 'Consolas'; -fx-font-size: 12px;");
                }
            }
        });

        TableColumn<Transaction, String> colDesc = new TableColumn<>("TRANSACTION DESCRIPTION");
        colDesc.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
        colDesc.setMaxWidth(1f * Integer.MAX_VALUE * 32);
        colDesc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
            }
        });

        TableColumn<Transaction, String> colVendor = new TableColumn<>("COUNTERPARTY / VENDOR");
        colVendor.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getVendor()));
        colVendor.setMaxWidth(1f * Integer.MAX_VALUE * 18);
        colVendor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(item);
                    pill.setStyle("-fx-background-color: #172236; -fx-text-fill: #CBD5E1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-color: #243452; -fx-border-radius: 6;");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        // Modern Fintech Pill Badges for Transaction Amounts
        TableColumn<Transaction, String> colAmount = new TableColumn<>("NET AMOUNT");
        colAmount.setCellValueFactory(cell -> {
            double amt = cell.getValue().getAmount();
            if (Math.abs(amt) < 0.005) amt = 0.0;
            String formatted = amt < 0 ? String.format("- $%,.2f", Math.abs(amt)) : String.format("+ $%,.2f", amt);
            return new SimpleStringProperty(formatted);
        });
        colAmount.setMaxWidth(1f * Integer.MAX_VALUE * 14);
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    if (item.startsWith("-")) {
                        badge.setStyle("-fx-background-color: rgba(248,113,113,0.14); -fx-text-fill: #F87171; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-color: rgba(248,113,113,0.3); -fx-border-radius: 6;");
                    } else if (item.equals("+ $0.00")) {
                        badge.setStyle("-fx-background-color: rgba(148,163,184,0.12); -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 10; -fx-background-radius: 6;");
                    } else {
                        badge.setStyle("-fx-background-color: rgba(52,211,153,0.14); -fx-text-fill: #34D399; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-color: rgba(52,211,153,0.3); -fx-border-radius: 6;");
                    }
                    HBox wrapper = new HBox(badge);
                    wrapper.setAlignment(Pos.CENTER_RIGHT);
                    setGraphic(wrapper);
                    setText(null);
                }
            }
        });

        TableColumn<Transaction, String> colRunning = new TableColumn<>("RUNNING BALANCE");
        colRunning.setCellValueFactory(cell -> {
            Double bal = runningBalanceMap.get(cell.getValue());
            if (bal == null) return new SimpleStringProperty("$0.00");
            return new SimpleStringProperty(bal < 0 ? String.format("-$%,.2f", Math.abs(bal)) : String.format("$%,.2f", bal));
        });
        colRunning.setMaxWidth(1f * Integer.MAX_VALUE * 14);
        colRunning.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-family: 'Consolas'; -fx-font-size: 13px;");
                }
            }
        });

        table.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();

            ContextMenu menu = new ContextMenu();
            MenuItem editItem = new MenuItem("✏  Edit / Modify Transaction...");
            editItem.setOnAction(e -> {
                if (row.getItem() != null) {
                    StudioProFeatures.openEditTransactionModal(row.getItem(), allTransactions, () -> {
                        saveAllTransactionsToCsv();
                        refreshVendorAutocomplete();
                        showAllEntries();
                        statusFooter.setText("Updated transaction in transactions.csv.");
                    });
                }
            });

            MenuItem invoiceItem = new MenuItem("🖨  Generate Printable HTML Invoice");
            invoiceItem.setOnAction(e -> {
                if (row.getItem() != null) {
                    StudioProFeatures.generateHtmlInvoice(row.getItem());
                }
            });

            MenuItem deleteItem = new MenuItem("🗑  Delete Transaction");
            deleteItem.setStyle("-fx-text-fill: #F87171; -fx-font-weight: bold;");
            deleteItem.setOnAction(e -> {
                if (row.getItem() != null) deleteTransaction(row.getItem());
            });

            menu.getItems().addAll(editItem, invoiceItem, new SeparatorMenuItem(), deleteItem);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showTransactionReceipt(row.getItem());
                }
            });
            return row;
        });

        table.getColumns().addAll(colDate, colTime, colDesc, colVendor, colAmount, colRunning);
        return table;
    }

    private void openTransactionModal(boolean isDeposit) {
        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle(isDeposit ? "Add Studio Deposit" : "Make Studio Payment (Debit)");
        dialog.setHeaderText(isDeposit
                ? "Select a Quick Studio Preset or enter custom deposit details"
                : "Select a Quick Expense Preset or enter custom payment details");

        ButtonType saveButtonType = new ButtonType("Save Transaction", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));

        String[][] presets = isDeposit ? StudioProFeatures.DEPOSIT_PRESETS : StudioProFeatures.PAYMENT_PRESETS;
        ComboBox<String> presetBox = new ComboBox<>();
        for (String[] p : presets) presetBox.getItems().add(p[0]);
        presetBox.getSelectionModel().selectFirst();
        presetBox.setMaxWidth(Double.MAX_VALUE);

        TextField descField = new TextField();
        descField.setPromptText("e.g., Wedding Photography Package");
        descField.setPrefWidth(270);

        TextField vendorField = new TextField();
        vendorField.setPromptText("e.g., VisualTizita Client / B&H Photo");

        TextField amountField = new TextField();
        amountField.setPromptText("e.g., 1500.00");

        presetBox.setOnAction(e -> {
            int idx = presetBox.getSelectionModel().getSelectedIndex();
            if (idx > 0 && idx < presets.length) {
                descField.setText(presets[idx][0]);
                vendorField.setText(presets[idx][1]);
                amountField.setText(presets[idx][2]);
            }
        });

        grid.add(new Label("Quick Studio Preset:"), 0, 0);
        grid.add(presetBox, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Vendor / Client:"), 0, 2);
        grid.add(vendorField, 1, 2);
        grid.add(new Label("Amount ($):"), 0, 3);
        grid.add(amountField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                try {
                    String d = descField.getText().trim();
                    String v = vendorField.getText().trim();
                    double rawAmount = Double.parseDouble(amountField.getText().trim());
                    if (d.isEmpty() || v.isEmpty() || rawAmount <= 0) {
                        showAlert("Validation Error", "All fields are required and the amount must be greater than 0.");
                        return null;
                    }
                    double finalAmount = isDeposit ? Math.abs(rawAmount) : -Math.abs(rawAmount);
                    return new Transaction(LocalDate.now(), LocalTime.now(), d, v, finalAmount);
                } catch (NumberFormatException ex) {
                    showAlert("Invalid Amount", "Please enter a valid numeric amount (e.g., 250.00).");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newTx -> {
            allTransactions.add(newTx);
            appendToCsv(newTx);
            refreshVendorAutocomplete();
            showAllEntries();
            statusFooter.setText("Saved new transaction to transactions.csv: " + newTx.getDescription());
        });
    }

    private void updateView(String title, List<Transaction> list) {
        runningBalanceMap = StudioProFeatures.calculateRunningBalances(allTransactions);
        activeViewTitle.setText(title);
        recordCountBadge.setText(list.size() + (list.size() == 1 ? " record" : " records"));
        List<Transaction> sorted = list.stream()
                .sorted(Comparator.comparing(Transaction::getDate)
                        .thenComparing(Transaction::getTime)
                        .reversed())
                .collect(Collectors.toList());
        displayedTransactions.setAll(sorted);
        refreshKpiTotals();
    }

    private void showAllEntries() {
        updateView("All Transactions — Newest First", allTransactions);
        statusFooter.setText("Showing all " + allTransactions.size() + " transactions.");
    }

    private void showDepositsOnly() {
        updateView("Inbound Deposits Only", allTransactions.stream().filter(t -> t.getAmount() > 0).collect(Collectors.toList()));
    }

    private void showPaymentsOnly() {
        updateView("Outbound Payments Only", allTransactions.stream().filter(t -> t.getAmount() < 0).collect(Collectors.toList()));
    }

    private void filterMonthToDate() {
        LocalDate today = LocalDate.now();
        updateView("Period: Month to Date (" + today.getMonth() + " " + today.getYear() + ")",
                allTransactions.stream().filter(t -> t.getDate().getYear() == today.getYear() && t.getDate().getMonth() == today.getMonth()).collect(Collectors.toList()));
    }

    private void filterPreviousMonth() {
        LocalDate prev = LocalDate.now().minusMonths(1);
        updateView("Period: Previous Month (" + prev.getMonth() + " " + prev.getYear() + ")",
                allTransactions.stream().filter(t -> t.getDate().getYear() == prev.getYear() && t.getDate().getMonth() == prev.getMonth()).collect(Collectors.toList()));
    }

    private void filterYearToDate() {
        int year = LocalDate.now().getYear();
        updateView("Period: Year to Date (" + year + ")",
                allTransactions.stream().filter(t -> t.getDate().getYear() == year).collect(Collectors.toList()));
    }

    private void filterPreviousYear() {
        int prevYear = LocalDate.now().getYear() - 1;
        updateView("Period: Previous Year (" + prevYear + ")",
                allTransactions.stream().filter(t -> t.getDate().getYear() == prevYear).collect(Collectors.toList()));
    }

    private void runCustomSearch() {
        try {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            String desc = descSearchField.getText().trim().toLowerCase();
            String vendor = vendorComboField.getEditor().getText().trim().toLowerCase();

            String minStr = minAmountField.getText().trim();
            Double minAmt = minStr.isEmpty() ? null : Double.parseDouble(minStr);

            String maxStr = maxAmountField.getText().trim();
            Double maxAmt = maxStr.isEmpty() ? null : Double.parseDouble(maxStr);

            List<Transaction> results = new ArrayList<>();
            for (Transaction t : allTransactions) {
                boolean match = true;
                if (startDate != null && t.getDate().isBefore(startDate)) match = false;
                if (endDate != null && t.getDate().isAfter(endDate)) match = false;
                if (!desc.isEmpty() && !t.getDescription().toLowerCase().contains(desc)) match = false;
                if (!vendor.isEmpty() && !t.getVendor().toLowerCase().contains(vendor)) match = false;
                if (minAmt != null && t.getAmount() < minAmt) match = false;
                if (maxAmt != null && t.getAmount() > maxAmt) match = false;
                if (match) results.add(t);
            }

            updateView("Filtered Search View", results);
            statusFooter.setText("Filter matched " + results.size() + " transaction(s).");
        } catch (Exception ignored) {
        }
    }

    private void refreshKpiTotals() {
        double deposits = 0;
        double payments = 0;
        int depCount = 0;
        int payCount = 0;
        for (Transaction t : displayedTransactions) {
            if (t.getAmount() > 0) {
                deposits += t.getAmount();
                depCount++;
            } else if (t.getAmount() < 0) {
                payments += Math.abs(t.getAmount());
                payCount++;
            }
        }
        double net = deposits - payments;

        totalBalanceValue.setText(net < 0 ? String.format("-$%,.2f", Math.abs(net)) : String.format("$%,.2f", net));
        totalBalanceSub.setText(String.format("Across %d active ledger entries", displayedTransactions.size()));

        totalDepositsValue.setText(String.format("+$%,.2f", deposits));
        totalDepositsSub.setText(String.format("%d inbound client credit(s)", depCount));

        totalPaymentsValue.setText(String.format("-$%,.2f", payments));
        totalPaymentsSub.setText(String.format("%d outbound vendor debit(s)", payCount));

        double totalVolume = deposits + payments;
        if (totalVolume > 0) {
            double ratio = deposits / totalVolume;
            cashFlowBar.setProgress(ratio);
            cashFlowRatioLabel.setText(String.format("%.0f%% Inbound Credits   •   %.0f%% Outbound Debits", ratio * 100, (1 - ratio) * 100));
        } else {
            cashFlowBar.setProgress(0.5);
            cashFlowRatioLabel.setText("No volume in current view");
        }
    }

    private void deleteTransaction(Transaction t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete transaction with " + t.getVendor() + "?");
        confirm.setContentText("Description: " + t.getDescription() + "\nAmount: $" + t.getAmount());

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                allTransactions.remove(t);
                saveAllTransactionsToCsv();
                refreshVendorAutocomplete();
                showAllEntries();
                statusFooter.setText("Deleted transaction from transactions.csv.");
            }
        });
    }

    private void showTransactionReceipt(Transaction t) {
        Alert receipt = new Alert(Alert.AlertType.INFORMATION);
        receipt.setTitle("VisualTizita Transaction Voucher");
        receipt.setHeaderText(t.getAmount() >= 0 ? "STUDIO DEPOSIT RECEIPT" : "STUDIO PAYMENT VOUCHER");

        ButtonType htmlBtn = new ButtonType("🖨 Open Printable HTML Invoice", ButtonBar.ButtonData.LEFT);
        receipt.getButtonTypes().add(htmlBtn);

        String details = String.format(
                "Date:          %s\nTime:          %s\nVendor:        %s\nDescription:   %s\nAmount:        %s",
                t.getDate(),
                t.getTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                t.getVendor(),
                t.getDescription(),
                t.getAmount() < 0 ? String.format("-$%,.2f", Math.abs(t.getAmount())) : String.format("+$%,.2f", t.getAmount())
        );
        receipt.setContentText(details);
        receipt.showAndWait().ifPresent(res -> {
            if (res == htmlBtn) StudioProFeatures.generateHtmlInvoice(t);
        });
    }

    private void exportCurrentViewToCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Filtered Ledger View");
        chooser.setInitialFileName("visualtizita-report.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File target = chooser.showSaveDialog(mainStage);
        if (target != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(target))) {
                for (Transaction t : displayedTransactions) {
                    writer.write(t.toCsvLine());
                    writer.newLine();
                }
                statusFooter.setText("Exported " + displayedTransactions.size() + " rows to " + target.getName());
            } catch (IOException ex) {
                showAlert("Export Error", "Could not save file: " + ex.getMessage());
            }
        }
    }

    private Image loadLogoImage() {
        String[] candidatePaths = {"logo.jpg", "Logo.jpg", "Visual Tizita Logo 2.jpg", "src/main/resources/logo.jpg", "src/main/resources/Visual Tizita Logo 2.jpg"};
        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists()) return new Image(f.toURI().toString());
        }
        InputStream stream = getClass().getResourceAsStream("/logo.jpg");
        return stream != null ? new Image(stream) : null;
    }

    private void loadFromCsv() {
        allTransactions.clear();
        File file = new File("transactions.csv");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = line.split(Pattern.quote("|"));
                if (tokens.length == 4) {
                    String[] dt = tokens[0].trim().split(" ");
                    LocalDate date = LocalDate.parse(dt[0]);
                    LocalTime time = LocalTime.parse(dt[1]);
                    String desc = tokens[1].trim();
                    String vendor = tokens[2].trim();
                    double amount = Double.parseDouble(tokens[3].trim());
                    if (Math.abs(amount) < 0.005) amount = 0.0;
                    allTransactions.add(new Transaction(date, time, desc, vendor, amount));
                }
            }
        } catch (Exception e) {
            System.out.println("GUI Error loading transactions.csv: " + e.getMessage());
        }
    }

    private void appendToCsv(Transaction t) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("transactions.csv", true))) {
            writer.write(t.toCsvLine());
            writer.newLine();
        } catch (IOException e) {
            showAlert("File Save Error", "Could not write to transactions.csv: " + e.getMessage());
        }
    }

    private void saveAllTransactionsToCsv() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("transactions.csv", false))) {
            for (Transaction t : allTransactions) {
                writer.write(t.toCsvLine());
                writer.newLine();
            }
        } catch (IOException e) {
            showAlert("File Save Error", "Could not update transactions.csv: " + e.getMessage());
        }
    }

    private VBox createExecutiveKpiCard(String labelText, Label valueLabel, Label subLabel, String accentHex, String badgeBgRgba) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #121A2B, #0E1525); -fx-background-radius: 14; -fx-border-color: #1E2B45; -fx-border-radius: 14;");
        card.setEffect(new DropShadow(18, 0, 6, Color.rgb(0, 0, 0, 0.35)));

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(labelText);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        title.setTextFill(Color.web("#94A3B8"));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Region dot = new Region();
        dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color: " + accentHex + "; -fx-background-radius: 5;");
        topRow.getChildren().addAll(title, sp, dot);

        valueLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 25));
        valueLabel.setTextFill(Color.web(accentHex));

        subLabel.setFont(Font.font("Segoe UI", 11));
        subLabel.setTextFill(Color.web("#64748B"));

        card.getChildren().addAll(topRow, valueLabel, subLabel);
        return card;
    }

    private Button createPrimaryCtaButton(String text, boolean isDeposit) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        String base = isDeposit
                ? "-fx-background-color: linear-gradient(to bottom, #10B981, #059669); -fx-text-fill: #04130E; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 9 12; -fx-background-radius: 8; -fx-cursor: hand;"
                : "-fx-background-color: #162033; -fx-text-fill: #F87171; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 9 12; -fx-background-radius: 8; -fx-border-color: rgba(248,113,113,0.35); -fx-border-radius: 8; -fx-cursor: hand;";
        String hover = isDeposit
                ? "-fx-background-color: #34D399; -fx-text-fill: #04130E; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 9 12; -fx-background-radius: 8; -fx-cursor: hand;"
                : "-fx-background-color: rgba(248,113,113,0.18); -fx-text-fill: #FCA5A5; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 9 12; -fx-background-radius: 8; -fx-border-color: #F87171; -fx-border-radius: 8; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button createSegmentButton(String text, boolean active) {
        Button b = new Button(text);
        styleSegmentButton(b, active);
        return b;
    }

    private void styleSegmentButton(Button b, boolean active) {
        if (active) {
            b.setStyle("-fx-background-color: #182338; -fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 7 18; -fx-background-radius: 7; -fx-border-color: rgba(212,175,55,0.35); -fx-border-radius: 7; -fx-cursor: hand;");
        } else {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 7 18; -fx-background-radius: 7; -fx-cursor: hand;");
        }
    }

    private Button createNavButton(String text, boolean initialActive) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        sidebarNavButtons.add(btn);
        applyNavStyle(btn, initialActive);
        btn.setOnMouseEntered(e -> {
            if (!Boolean.TRUE.equals(btn.getUserData())) {
                btn.setStyle("-fx-background-color: #11192B; -fx-text-fill: #F8FAFC; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 8; -fx-cursor: hand;");
            }
        });
        btn.setOnMouseExited(e -> applyNavStyle(btn, Boolean.TRUE.equals(btn.getUserData())));
        return btn;
    }

    private void setActiveNav(Button activeBtn) {
        for (Button b : sidebarNavButtons) {
            applyNavStyle(b, b == activeBtn);
        }
    }

    private void applyNavStyle(Button btn, boolean active) {
        btn.setUserData(active);
        if (active) {
            btn.setStyle("-fx-background-color: rgba(212,175,55,0.14); -fx-text-fill: #D4AF37; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 8; -fx-border-color: rgba(212,175,55,0.35); -fx-border-radius: 8; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    private Region createSubtleDivider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color: #141D2E;");
        VBox.setMargin(r, new Insets(4, 0, 4, 0));
        return r;
    }

    private Label createSectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lbl.setTextFill(Color.web("#475569"));
        lbl.setPadding(new Insets(6, 0, 2, 6));
        return lbl;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("dark-field");
        return tf;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String createEnterpriseStylesheet() {
        String css = """
                .root {
                    -fx-base: #0B0F19;
                    -fx-background: #0B0F19;
                    -fx-control-inner-background: #131C2E;
                    -fx-control-inner-background-alt: #0E1626;
                    -fx-focus-color: #D4AF37;
                    -fx-faint-focus-color: transparent;
                }
                .edge-to-edge-scroll, .edge-to-edge-scroll > .viewport {
                    -fx-background-color: transparent;
                }
                /* Fix glaring white TextFields, ComboBoxes & DatePickers */
                .dark-field, .dark-combo, .dark-datepicker {
                    -fx-background-color: #151F33;
                    -fx-text-fill: #F8FAFC;
                    -fx-prompt-text-fill: #64748B;
                    -fx-background-radius: 8;
                    -fx-border-color: #24324D;
                    -fx-border-radius: 8;
                    -fx-padding: 6 10;
                }
                .dark-field:focused, .dark-combo:focused, .dark-datepicker:focused {
                    -fx-border-color: #D4AF37;
                    -fx-background-color: #18243B;
                }
                .combo-box-base > .text-field, .date-picker > .text-field {
                    -fx-background-color: transparent;
                    -fx-text-fill: #F8FAFC;
                    -fx-prompt-text-fill: #64748B;
                }
                .combo-box-base > .arrow-button, .date-picker > .arrow-button {
                    -fx-background-color: #1C2942;
                    -fx-background-radius: 0 7 7 0;
                }
                .combo-box-base > .arrow-button > .arrow, .date-picker > .arrow-button > .arrow {
                    -fx-background-color: #D4AF37;
                }
                .combo-box-popup .list-view {
                    -fx-background-color: #101726;
                    -fx-border-color: #24324D;
                }
                .combo-box-popup .list-cell {
                    -fx-background-color: #101726;
                    -fx-text-fill: #E2E8F0;
                    -fx-padding: 7 12;
                }
                .combo-box-popup .list-cell:hover, .combo-box-popup .list-cell:selected {
                    -fx-background-color: #1E2C46;
                    -fx-text-fill: #D4AF37;
                }
                /* Fix white ProgressBar track */
                .fintech-progress > .track {
                    -fx-background-color: #1A2438;
                    -fx-background-radius: 6;
                }
                .fintech-progress > .bar {
                    -fx-background-color: linear-gradient(to right, #10B981, #D4AF37);
                    -fx-background-radius: 6;
                    -fx-background-insets: 0;
                }
                /* Modern Borderless Enterprise Table */
                .enterprise-table {
                    -fx-background-color: #101726;
                    -fx-background-radius: 12;
                    -fx-border-color: #1C283E;
                    -fx-border-radius: 12;
                    -fx-padding: 0;
                    -fx-table-cell-border-color: transparent;
                }
                .enterprise-table .column-header-background {
                    -fx-background-color: #0D1320;
                    -fx-background-radius: 12 12 0 0;
                    -fx-border-color: #1C283E;
                    -fx-border-width: 0 0 1 0;
                }
                .enterprise-table .column-header, .enterprise-table .filler {
                    -fx-background-color: transparent;
                    -fx-size: 40px;
                    -fx-border-color: transparent;
                }
                .enterprise-table .column-header .label {
                    -fx-text-fill: #64748B;
                    -fx-font-weight: bold;
                    -fx-font-size: 10px;
                    -fx-alignment: CENTER-LEFT;
                }
                .enterprise-table .table-row-cell {
                    -fx-background-color: #101726;
                    -fx-border-color: #162033;
                    -fx-border-width: 0 0 1 0;
                    -fx-cell-size: 44px;
                }
                .enterprise-table .table-row-cell:odd {
                    -fx-background-color: #0E1523;
                }
                .enterprise-table .table-row-cell:hover {
                    -fx-background-color: #162238;
                }
                .enterprise-table .table-row-cell:selected {
                    -fx-background-color: #1D2B45;
                }
                .enterprise-table .table-cell {
                    -fx-border-color: transparent;
                    -fx-padding: 0 12 0 12;
                    -fx-alignment: CENTER-LEFT;
                }
                .enterprise-table .corner {
                    -fx-background-color: #0D1320;
                }
                /* Sleek Dark Scrollbars (Eliminates glaring white scrollbars) */
                .scroll-bar:vertical, .scroll-bar:horizontal {
                    -fx-background-color: transparent;
                }
                .scroll-bar .track {
                    -fx-background-color: #0B0F19;
                    -fx-background-radius: 4;
                }
                .scroll-bar .thumb {
                    -fx-background-color: #24324D;
                    -fx-background-radius: 4;
                }
                .scroll-bar .thumb:hover {
                    -fx-background-color: #D4AF37;
                }
                .scroll-bar .increment-button, .scroll-bar .decrement-button {
                    -fx-background-color: transparent;
                    -fx-padding: 0;
                }
                .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow {
                    -fx-shape: " ";
                    -fx-padding: 0;
                }
                .context-menu {
                    -fx-background-color: #101726;
                    -fx-border-color: #263552;
                    -fx-background-radius: 10;
                    -fx-border-radius: 10;
                }
                .menu-item .label {
                    -fx-text-fill: #F8FAFC;
                }
                .menu-item:focused {
                    -fx-background-color: #1E2C46;
                }
                .chart-pie-label { -fx-fill: #F8FAFC; }
                .chart-legend { -fx-background-color: transparent; }
                .chart-legend-item { -fx-text-fill: #F8FAFC; }
                .axis { -fx-tick-label-fill: #94A3B8; }
                .chart-plot-background { -fx-background-color: #0D1322; }
                """;
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) {
        launch(args);
    }
}