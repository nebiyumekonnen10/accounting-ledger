package com.pluralsight;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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

    private Label totalBalanceValue;
    private Label totalDepositsValue;
    private Label totalPaymentsValue;
    private Label activeViewTitle;
    private Label statusFooter;
    private ProgressBar cashFlowBar;
    private Label cashFlowRatioLabel;

    private TextField startDateField;
    private TextField endDateField;
    private TextField descSearchField;
    private TextField vendorSearchField;
    private TextField minAmountField;
    private TextField maxAmountField;

    private VBox analyticsContainer;
    private Stage mainStage;

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        loadFromCsv();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #07090e;");

        root.setLeft(buildSidebar());
        root.setCenter(buildMainWorkspace());

        Scene scene = new Scene(root, 1360, 840);
        scene.getStylesheets().add(createDarkStylesheet());

        Image logoImg = loadLogoImage();
        if (logoImg != null) {
            primaryStage.getIcons().add(logoImg);
        }

        primaryStage.setTitle("VisualTizita Media & Event Studio — Accounting Ledger Pro");
        primaryStage.setScene(scene);
        primaryStage.show();

        showAllEntries();
    }

    private Image loadLogoImage() {
        String[] candidatePaths = {
                "logo.jpg",
                "Visual Tizita Logo 2.jpg",
                "src/main/resources/logo.jpg",
                "src/main/resources/Visual Tizita Logo 2.jpg"
        };
        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists()) {
                return new Image(f.toURI().toString());
            }
        }
        InputStream stream = getClass().getResourceAsStream("/logo.jpg");
        if (stream != null) {
            return new Image(stream);
        }
        return null;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(18, 16, 18, 16));
        sidebar.setPrefWidth(275);
        sidebar.setStyle("-fx-background-color: #000000; -fx-border-color: #26221a; -fx-border-width: 0 1 0 0;");

        // Brand Logo Box
        VBox brandBox = new VBox(6);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.setPadding(new Insets(2, 0, 10, 0));

        Image logoImg = loadLogoImage();
        if (logoImg != null) {
            ImageView logoView = new ImageView(logoImg);
            logoView.setFitWidth(210);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            brandBox.getChildren().add(logoView);
        } else {
            Label brandTitle = new Label("VISUAL TIZITA");
            brandTitle.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 20));
            brandTitle.setTextFill(Color.web("#d4af37"));
            Label brandTag = new Label("Memories that last forever");
            brandTag.setFont(Font.font("Segoe UI", 11));
            brandTag.setTextFill(Color.web("#9ca3af"));
            brandBox.getChildren().addAll(brandTitle, brandTag);
        }

        Label studioBadge = new Label("STUDIO ACCOUNTING LEDGER");
        studioBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        studioBadge.setTextFill(Color.web("#d4af37"));
        brandBox.getChildren().add(studioBadge);

        Button btnDeposit = createActionButton("+ Add Deposit", "#10b981", "#059669");
        btnDeposit.setOnAction(e -> openTransactionModal(true));

        Button btnPayment = createActionButton("- Make Payment", "#ef4444", "#dc2626");
        btnPayment.setOnAction(e -> openTransactionModal(false));

        Label ledgerHeader = createSectionHeader("LEDGER VIEWS");
        Button btnAll = createNavButton("All Entries (Newest)");
        btnAll.setOnAction(e -> showAllEntries());

        Button btnDeposits = createNavButton("Deposits Only");
        btnDeposits.setOnAction(e -> showDepositsOnly());

        Button btnPayments = createNavButton("Payments Only");
        btnPayments.setOnAction(e -> showPaymentsOnly());

        Label reportsHeader = createSectionHeader("TIME & VENDOR REPORTS");
        Button btnMTD = createNavButton("Month to Date (MTD)");
        btnMTD.setOnAction(e -> filterMonthToDate());

        Button btnPrevMonth = createNavButton("Previous Month");
        btnPrevMonth.setOnAction(e -> filterPreviousMonth());

        Button btnYTD = createNavButton("Year to Date (YTD)");
        btnYTD.setOnAction(e -> filterYearToDate());

        Button btnPrevYear = createNavButton("Previous Year");
        btnPrevYear.setOnAction(e -> filterPreviousYear());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnExport = createNavButton("Export Current View (.csv)");
        btnExport.setOnAction(e -> exportCurrentViewToCsv());

        Button btnReload = createNavButton("Reload CSV from Disk");
        btnReload.setOnAction(e -> {
            loadFromCsv();
            showAllEntries();
            statusFooter.setText("Reloaded transactions.csv from disk.");
        });

        sidebar.getChildren().addAll(
                brandBox,
                btnDeposit, btnPayment,
                new Separator(),
                ledgerHeader, btnAll, btnDeposits, btnPayments,
                new Separator(),
                reportsHeader, btnMTD, btnPrevMonth, btnYTD, btnPrevYear,
                spacer, btnExport, btnReload
        );

        return sidebar;
    }

    private TabPane buildMainWorkspace() {
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: #07090e;");

        // Tab 1: Ledger Table & Search Workspace
        Tab ledgerTab = new Tab("Ledger & Search");
        ledgerTab.setClosable(false);

        VBox main = new VBox(14);
        main.setPadding(new Insets(18, 24, 16, 24));

        HBox kpiRow = new HBox(16);
        totalBalanceValue = new Label("$0.00");
        totalDepositsValue = new Label("+$0.00");
        totalPaymentsValue = new Label("-$0.00");

        VBox balanceCard = createKpiCard("NET STUDIO BALANCE", totalBalanceValue, "#d4af37");
        VBox depositCard = createKpiCard("TOTAL DEPOSITS (CREDITS)", totalDepositsValue, "#34d399");
        VBox paymentCard = createKpiCard("TOTAL PAYMENTS (DEBITS)", totalPaymentsValue, "#f87171");
        HBox.setHgrow(balanceCard, Priority.ALWAYS);
        HBox.setHgrow(depositCard, Priority.ALWAYS);
        HBox.setHgrow(paymentCard, Priority.ALWAYS);
        kpiRow.getChildren().addAll(balanceCard, depositCard, paymentCard);

        VBox cashFlowBox = buildCashFlowBar();
        VBox searchCard = buildCustomSearchPanel();

        HBox tableHeaderBar = new HBox(12);
        tableHeaderBar.setAlignment(Pos.CENTER_LEFT);
        activeViewTitle = new Label("All Entries — Newest First");
        activeViewTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        activeViewTitle.setTextFill(Color.web("#f3f4f6"));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label hintLabel = new Label("Tip: Right-click any row to Delete  |  Double-click to View Receipt");
        hintLabel.setFont(Font.font("Segoe UI", 11));
        hintLabel.setTextFill(Color.web("#9ca3af"));

        tableHeaderBar.getChildren().addAll(activeViewTitle, headerSpacer, hintLabel);

        TableView<Transaction> table = buildTransactionTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        statusFooter = new Label("Ready — connected to transactions.csv");
        statusFooter.setTextFill(Color.web("#9ca3af"));
        statusFooter.setFont(Font.font("Segoe UI", 12));

        main.getChildren().addAll(kpiRow, cashFlowBox, searchCard, tableHeaderBar, table, statusFooter);
        ledgerTab.setContent(main);

        // Tab 2: Visual Studio Analytics
        Tab analyticsTab = new Tab("Studio Analytics & Vendor Breakdown");
        analyticsTab.setClosable(false);
        analyticsContainer = new VBox(16);
        analyticsContainer.setPadding(new Insets(24));
        analyticsTab.setContent(analyticsContainer);

        // Refresh analytics when switching tabs
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == analyticsTab) {
                refreshAnalyticsView();
            }
        });

        tabPane.getTabs().addAll(ledgerTab, analyticsTab);
        return tabPane;
    }

    private VBox buildCashFlowBar() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(8, 14, 10, 14));
        box.setStyle("-fx-background-color: #111827; -fx-background-radius: 10; -fx-border-color: #26221a; -fx-border-radius: 10;");

        HBox topRow = new HBox();
        Label lbl = new Label("CASH FLOW HEALTH (CURRENT VIEW)");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web("#9ca3af"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cashFlowRatioLabel = new Label("100% Positive Flow");
        cashFlowRatioLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        cashFlowRatioLabel.setTextFill(Color.web("#d4af37"));

        topRow.getChildren().addAll(lbl, spacer, cashFlowRatioLabel);

        cashFlowBar = new ProgressBar(1.0);
        cashFlowBar.setMaxWidth(Double.MAX_VALUE);
        cashFlowBar.setPrefHeight(8);
        cashFlowBar.setStyle("-fx-accent: #10b981;");

        box.getChildren().addAll(topRow, cashFlowBar);
        return box;
    }

    private VBox buildCustomSearchPanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setStyle("-fx-background-color: #111827; -fx-background-radius: 10; -fx-border-color: #26221a; -fx-border-radius: 10;");

        Label title = new Label("CUSTOM MULTI-FILTER SEARCH (Live search by typing or clicking Apply)");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        title.setTextFill(Color.web("#d4af37"));

        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER_LEFT);

        descSearchField = createStyledTextField("Description contains...");
        vendorSearchField = createStyledTextField("Vendor contains...");
        startDateField = createStyledTextField("Start (YYYY-MM-DD)");
        endDateField = createStyledTextField("End (YYYY-MM-DD)");

        HBox.setHgrow(descSearchField, Priority.ALWAYS);
        HBox.setHgrow(vendorSearchField, Priority.ALWAYS);
        startDateField.setPrefWidth(170);
        endDateField.setPrefWidth(170);

        descSearchField.textProperty().addListener((obs, oldV, newV) -> runCustomSearch());
        vendorSearchField.textProperty().addListener((obs, oldV, newV) -> runCustomSearch());

        row1.getChildren().addAll(descSearchField, vendorSearchField, startDateField, endDateField);

        HBox row2 = new HBox(10);
        row2.setAlignment(Pos.CENTER_LEFT);

        minAmountField = createStyledTextField("Min Amount ($)");
        maxAmountField = createStyledTextField("Max Amount ($)");
        minAmountField.setPrefWidth(150);
        maxAmountField.setPrefWidth(150);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRunSearch = new Button("Apply Filters");
        btnRunSearch.setMinWidth(110);
        btnRunSearch.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;");
        btnRunSearch.setOnAction(e -> runCustomSearch());

        Button btnClear = new Button("Reset All");
        btnClear.setMinWidth(95);
        btnClear.setStyle("-fx-background-color: #374151; -fx-text-fill: #e5e7eb; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        btnClear.setOnAction(e -> {
            startDateField.clear();
            endDateField.clear();
            descSearchField.clear();
            vendorSearchField.clear();
            minAmountField.clear();
            maxAmountField.clear();
            showAllEntries();
        });

        row2.getChildren().addAll(minAmountField, maxAmountField, spacer, btnRunSearch, btnClear);
        box.getChildren().addAll(title, row1, row2);
        return box;
    }

    private TableView<Transaction> buildTransactionTable() {
        TableView<Transaction> table = new TableView<>(displayedTransactions);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No transactions match the current filter."));

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        TableColumn<Transaction, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDate().toString()));
        colDate.setMaxWidth(1f * Integer.MAX_VALUE * 14);

        TableColumn<Transaction, String> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTime().format(timeFmt)));
        colTime.setMaxWidth(1f * Integer.MAX_VALUE * 12);

        TableColumn<Transaction, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
        colDesc.setMaxWidth(1f * Integer.MAX_VALUE * 36);

        TableColumn<Transaction, String> colVendor = new TableColumn<>("Vendor");
        colVendor.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getVendor()));
        colVendor.setMaxWidth(1f * Integer.MAX_VALUE * 22);

        TableColumn<Transaction, String> colAmount = new TableColumn<>("Amount");
        colAmount.setCellValueFactory(cell -> {
            double amt = cell.getValue().getAmount();
            if (Math.abs(amt) < 0.005) amt = 0.0;
            String formatted = amt < 0
                    ? String.format("-$%,.2f", Math.abs(amt))
                    : String.format("+$%,.2f", amt);
            return new SimpleStringProperty(formatted);
        });
        colAmount.setMaxWidth(1f * Integer.MAX_VALUE * 16);

        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    } else if (item.equals("+$0.00")) {
                        setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    } else {
                        setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    }
                }
            }
        });

        // Right-Click Context Menu & Double-Click Receipt
        table.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();

            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Transaction");
            deleteItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            deleteItem.setOnAction(event -> {
                Transaction t = row.getItem();
                if (t != null) {
                    deleteTransaction(t);
                }
            });
            contextMenu.getItems().add(deleteItem);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showTransactionReceipt(row.getItem());
                }
            });
            return row;
        });

        table.getColumns().addAll(colDate, colTime, colDesc, colVendor, colAmount);
        return table;
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
                showAllEntries();
                statusFooter.setText("Deleted transaction from transactions.csv.");
            }
        });
    }

    private void showTransactionReceipt(Transaction t) {
        Alert receipt = new Alert(Alert.AlertType.INFORMATION);
        receipt.setTitle("VisualTizita Transaction Receipt");
        receipt.setHeaderText(t.getAmount() >= 0 ? "STUDIO DEPOSIT RECEIPT" : "STUDIO PAYMENT VOUCHER");
        String details = String.format(
                "Date:          %s\nTime:          %s\nVendor:        %s\nDescription:   %s\nAmount:        %s",
                t.getDate(),
                t.getTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                t.getVendor(),
                t.getDescription(),
                t.getAmount() < 0 ? String.format("-$%,.2f", Math.abs(t.getAmount())) : String.format("+$%,.2f", t.getAmount())
        );
        receipt.setContentText(details);
        receipt.showAndWait();
    }

    // =========================================================================
    // ANALYTICS TAB RENDERING
    // =========================================================================

    private void refreshAnalyticsView() {
        analyticsContainer.getChildren().clear();

        Label title = new Label("VISUALTIZITA STUDIO FINANCIAL ANALYTICS");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#d4af37"));

        Label subtitle = new Label("Aggregated performance metrics across all recorded transactions.");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#9ca3af"));

        VBox headerBox = new VBox(4, title, subtitle);

        // Group by Vendor
        Map<String, Double> vendorTotals = new HashMap<>();
        double totalVolume = 0;
        for (Transaction t : allTransactions) {
            vendorTotals.put(t.getVendor(), vendorTotals.getOrDefault(t.getVendor(), 0.0) + t.getAmount());
            totalVolume += Math.abs(t.getAmount());
        }

        VBox breakdownCard = new VBox(12);
        breakdownCard.setPadding(new Insets(20));
        breakdownCard.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");

        Label cardTitle = new Label("NET CASH FLOW BY VENDOR / CLIENT");
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        cardTitle.setTextFill(Color.web("#f3f4f6"));
        breakdownCard.getChildren().add(cardTitle);

        if (vendorTotals.isEmpty()) {
            breakdownCard.getChildren().add(new Label("No transactions available for analysis."));
        } else {
            for (Map.Entry<String, Double> entry : vendorTotals.entrySet()) {
                String vendor = entry.getKey();
                double amt = entry.getValue();

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);

                Label vLabel = new Label(vendor);
                vLabel.setPrefWidth(220);
                vLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                vLabel.setTextFill(Color.web("#e5e7eb"));

                ProgressBar bar = new ProgressBar(totalVolume > 0 ? Math.min(1.0, Math.abs(amt) / totalVolume) * 2 : 0);
                bar.setPrefWidth(350);
                bar.setStyle(amt >= 0 ? "-fx-accent: #34d399;" : "-fx-accent: #f87171;");

                Label aLabel = new Label(amt < 0 ? String.format("-$%,.2f", Math.abs(amt)) : String.format("+$%,.2f", amt));
                aLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                aLabel.setTextFill(Color.web(amt >= 0 ? "#34d399" : "#f87171"));

                row.getChildren().addAll(vLabel, bar, aLabel);
                breakdownCard.getChildren().add(row);
            }
        }

        analyticsContainer.getChildren().addAll(headerBox, new Separator(), breakdownCard);
    }

    // =========================================================================
    // FILTER & SORT LOGIC
    // =========================================================================

    private List<Transaction> getSortedNewestFirst(List<Transaction> source) {
        return source.stream()
                .sorted(Comparator.comparing(Transaction::getDate)
                        .thenComparing(Transaction::getTime)
                        .reversed())
                .collect(Collectors.toList());
    }

    private void updateView(String title, List<Transaction> list) {
        activeViewTitle.setText(title + " (" + list.size() + ")");
        displayedTransactions.setAll(getSortedNewestFirst(list));
        refreshKpiTotals();
    }

    private void showAllEntries() {
        updateView("All Entries — Newest First", allTransactions);
        statusFooter.setText("Showing all " + allTransactions.size() + " transactions.");
    }

    private void showDepositsOnly() {
        List<Transaction> filtered = allTransactions.stream().filter(t -> t.getAmount() > 0).collect(Collectors.toList());
        updateView("Deposits Only", filtered);
    }

    private void showPaymentsOnly() {
        List<Transaction> filtered = allTransactions.stream().filter(t -> t.getAmount() < 0).collect(Collectors.toList());
        updateView("Payments (Debits) Only", filtered);
    }

    private void filterMonthToDate() {
        LocalDate today = LocalDate.now();
        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> t.getDate().getYear() == today.getYear() && t.getDate().getMonth() == today.getMonth())
                .collect(Collectors.toList());
        updateView("Report: Month to Date (" + today.getMonth() + " " + today.getYear() + ")", filtered);
    }

    private void filterPreviousMonth() {
        LocalDate prev = LocalDate.now().minusMonths(1);
        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> t.getDate().getYear() == prev.getYear() && t.getDate().getMonth() == prev.getMonth())
                .collect(Collectors.toList());
        updateView("Report: Previous Month (" + prev.getMonth() + " " + prev.getYear() + ")", filtered);
    }

    private void filterYearToDate() {
        int year = LocalDate.now().getYear();
        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> t.getDate().getYear() == year)
                .collect(Collectors.toList());
        updateView("Report: Year to Date (" + year + ")", filtered);
    }

    private void filterPreviousYear() {
        int prevYear = LocalDate.now().getYear() - 1;
        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> t.getDate().getYear() == prevYear)
                .collect(Collectors.toList());
        updateView("Report: Previous Year (" + prevYear + ")", filtered);
    }

    private void runCustomSearch() {
        try {
            String startStr = startDateField.getText().trim();
            LocalDate startDate = startStr.isEmpty() ? null : LocalDate.parse(startStr);

            String endStr = endDateField.getText().trim();
            LocalDate endDate = endStr.isEmpty() ? null : LocalDate.parse(endStr);

            String desc = descSearchField.getText().trim().toLowerCase();
            String vendor = vendorSearchField.getText().trim().toLowerCase();

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

            updateView("Custom Search Results", results);
            statusFooter.setText("Custom search matched " + results.size() + " transaction(s).");
        } catch (Exception ex) {
            // Ignore live typing parse errors until valid input is given
        }
    }

    private void refreshKpiTotals() {
        double deposits = 0;
        double payments = 0;
        for (Transaction t : displayedTransactions) {
            if (t.getAmount() > 0) deposits += t.getAmount();
            else if (t.getAmount() < 0) payments += Math.abs(t.getAmount());
        }
        double net = deposits - payments;

        totalBalanceValue.setText(net < 0 ? String.format("-$%,.2f", Math.abs(net)) : String.format("$%,.2f", net));
        totalDepositsValue.setText(String.format("+$%,.2f", deposits));
        totalPaymentsValue.setText(String.format("-$%,.2f", payments));

        double totalVolume = deposits + payments;
        if (totalVolume > 0) {
            double ratio = deposits / totalVolume;
            cashFlowBar.setProgress(ratio);
            cashFlowRatioLabel.setText(String.format("%.0f%% Deposits  |  %.0f%% Payments", ratio * 100, (1 - ratio) * 100));
        } else {
            cashFlowBar.setProgress(0.5);
            cashFlowRatioLabel.setText("No volume in current view");
        }
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

    // =========================================================================
    // ADD DEPOSIT / MAKE PAYMENT MODAL
    // =========================================================================

    private void openTransactionModal(boolean isDeposit) {
        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle(isDeposit ? "Add Deposit" : "Make Payment (Debit)");
        dialog.setHeaderText(isDeposit ? "Record a positive deposit into transactions.csv" : "Record a payment (automatically converted to negative)");

        ButtonType saveButtonType = new ButtonType("Save Transaction", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField descField = new TextField();
        descField.setPromptText("e.g., Wedding Photography Package");
        descField.setPrefWidth(260);

        TextField vendorField = new TextField();
        vendorField.setPromptText("e.g., VisualTizita Client / B&H Photo");

        TextField amountField = new TextField();
        amountField.setPromptText("e.g., 1500.00");

        grid.add(new Label("Description:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Vendor:"), 0, 1);
        grid.add(vendorField, 1, 1);
        grid.add(new Label("Amount ($):"), 0, 2);
        grid.add(amountField, 1, 2);

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
            showAllEntries();
            statusFooter.setText("Saved new transaction to transactions.csv: " + newTx.getDescription());
        });
    }

    // =========================================================================
    // STANDALONE CSV I/O
    // =========================================================================

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

    // =========================================================================
    // UI STYLING HELPERS
    // =========================================================================

    private VBox createKpiCard(String labelText, Label valueLabel, String accentHex) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");

        Label title = new Label(labelText);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        title.setTextFill(Color.web("#9ca3af"));

        valueLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 22));
        valueLabel.setTextFill(Color.web(accentHex));

        card.getChildren().addAll(title, valueLabel);
        return card;
    }

    private Button createActionButton(String text, String bgHex, String borderHex) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER);
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
                        "-fx-padding: 9 14; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-radius: 8; -fx-cursor: hand;",
                bgHex, borderHex));
        return btn;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: #111827; -fx-text-fill: #e5e7eb; -fx-font-size: 13px; " +
                "-fx-padding: 8 12; -fx-background-radius: 7; -fx-border-color: #1f2937; -fx-border-radius: 7; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #d4af37; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 7; -fx-border-color: #d4af37; -fx-border-radius: 7; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #111827; -fx-text-fill: #e5e7eb; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 7; -fx-border-color: #1f2937; -fx-border-radius: 7; -fx-cursor: hand;"));
        return btn;
    }

    private Label createSectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web("#d4af37"));
        lbl.setPadding(new Insets(4, 0, 2, 4));
        return lbl;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #f9fafb; -fx-prompt-text-fill: #6b7280; " +
                "-fx-background-radius: 6; -fx-border-color: #374151; -fx-border-radius: 6; -fx-padding: 5 10;");
        return tf;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String createDarkStylesheet() {
        String css = """
                .tab-pane .tab-header-area .tab-header-background {
                    -fx-background-color: #000000;
                }
                .tab-pane .tab {
                    -fx-background-color: #111827;
                    -fx-padding: 8 16;
                    -fx-background-radius: 6 6 0 0;
                    -fx-border-color: #26221a;
                }
                .tab-pane .tab:selected {
                    -fx-background-color: #1f2937;
                    -fx-border-color: #d4af37;
                }
                .tab .tab-label {
                    -fx-text-fill: #e5e7eb;
                    -fx-font-weight: bold;
                    -fx-font-size: 12px;
                }
                .tab:selected .tab-label {
                    -fx-text-fill: #d4af37;
                }
                .table-view {
                    -fx-background-color: #111827;
                    -fx-border-color: #26221a;
                    -fx-border-radius: 10;
                    -fx-background-radius: 10;
                }
                .table-view .column-header-background {
                    -fx-background-color: #161e2e;
                }
                .table-view .column-header, .table-view .filler {
                    -fx-background-color: transparent;
                    -fx-size: 36px;
                    -fx-border-color: #26221a;
                    -fx-border-width: 0 0 1 0;
                }
                .table-view .column-header .label {
                    -fx-text-fill: #d4af37;
                    -fx-font-weight: bold;
                    -fx-font-size: 12px;
                }
                .table-row-cell {
                    -fx-background-color: #111827;
                    -fx-border-color: #1f2937;
                    -fx-border-width: 0 0 1 0;
                    -fx-cell-size: 35px;
                }
                .table-row-cell:odd {
                    -fx-background-color: #0d1320;
                }
                .table-row-cell:selected {
                    -fx-background-color: #2e2618;
                }
                .table-cell {
                    -fx-text-fill: #e5e7eb;
                    -fx-font-size: 13px;
                    -fx-alignment: CENTER-LEFT;
                }
                .context-menu {
                    -fx-background-color: #111827;
                    -fx-border-color: #26221a;
                    -fx-background-radius: 8;
                    -fx-border-radius: 8;
                }
                .menu-item:hover {
                    -fx-background-color: #1f2937;
                }
                .separator *.line {
                    -fx-border-color: #26221a;
                    -fx-border-width: 1 0 0 0;
                }
                """;
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) {
        launch(args);
    }
}