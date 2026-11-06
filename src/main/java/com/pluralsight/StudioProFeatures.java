package com.pluralsight;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class StudioProFeatures {

    // 1. STUDIO PRESET TEMPLATES FOR 1-CLICK DEMO ENTRY
    public static final String[][] DEPOSIT_PRESETS = {
            {"-- Select Quick Studio Preset --", "", ""},
            {"Wedding Photography & Cinema Package", "VisualTizita Client", "2500.00"},
            {"Event Booking Retainer (50%)", "Event Client", "750.00"},
            {"Commercial Brand Shoot", "Corporate Partner", "1800.00"},
            {"Portrait Session & Prints", "Studio Walk-In", "350.00"},
            {"QR Live Event Gallery Upgrade", "VisualTizita Event", "500.00"}
    };

    public static final String[][] PAYMENT_PRESETS = {
            {"-- Select Quick Expense Preset --", "", ""},
            {"Studio Lighting & Gimbal Rig", "B&H Photo Video", "450.50"},
            {"Full-Frame Prime Lens Rental", "Lensrentals", "185.00"},
            {"Adobe Creative Cloud & Cloud Storage", "Adobe / AWS", "89.99"},
            {"Second Shooter / Assistant Payout", "Freelance Crew", "400.00"},
            {"Studio Backdrop & Props Restock", "Amazon Business", "145.00"}
    };

    // 2. CHRONOLOGICAL RUNNING BALANCE CALCULATOR
    public static Map<Transaction, Double> calculateRunningBalances(List<Transaction> allTransactions) {
        List<Transaction> chronological = new ArrayList<>(allTransactions);
        chronological.sort(Comparator.comparing(Transaction::getDate)
                .thenComparing(Transaction::getTime));

        Map<Transaction, Double> map = new IdentityHashMap<>();
        double running = 0.0;
        for (Transaction t : chronological) {
            running += t.getAmount();
            map.put(t, running);
        }
        return map;
    }

    // 3. ANALYTICS CONTEXT SIDEBAR (Tab 2 & Tab 3 Control Center)
    public static ScrollPane buildAnalyticsSidebar(
            Image logoImg,
            double currentTaxRate,
            double currentRevenueGoal,
            String currentScope,
            Consumer<Double> onTaxRateChanged,
            Consumer<Double> onGoalChanged,
            Consumer<String> onScopeChanged,
            Runnable onExportHtmlReport,
            Runnable onBackupCsv,
            Runnable onHideSidebar,
            Runnable onSwitchBackToLedger
    ) {
        VBox sidebar = new VBox(9);
        sidebar.setPadding(new Insets(12, 14, 14, 14));
        sidebar.setPrefWidth(245);
        sidebar.setStyle("-fx-background-color: #000000; -fx-border-color: #26221a; -fx-border-width: 0 1 0 0;");

        VBox brandBox = new VBox(3);
        brandBox.setAlignment(Pos.CENTER);
        if (logoImg != null) {
            ImageView lv = new ImageView(logoImg);
            lv.setFitWidth(120);
            lv.setPreserveRatio(true);
            brandBox.getChildren().add(lv);
        }
        Label badge = new Label("STUDIO CFO CONTROL CENTER");
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        badge.setTextFill(Color.web("#d4af37"));
        brandBox.getChildren().add(badge);

        Button btnHide = new Button("⇄  Hide Sidebar (Full Screen)");
        btnHide.setMaxWidth(Double.MAX_VALUE);
        btnHide.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 6; -fx-border-color: #d4af37; -fx-border-radius: 6; -fx-cursor: hand;");
        btnHide.setOnAction(e -> onHideSidebar.run());

        Label scopeHdr = createSmallHeader("ANALYTICS DATA SCOPE");
        ComboBox<String> scopeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "All Historical Data",
                "Current Year (YTD)",
                "Current Month (MTD)"
        ));
        scopeCombo.setValue(currentScope);
        scopeCombo.setMaxWidth(Double.MAX_VALUE);
        scopeCombo.setStyle("-fx-background-color: #111827; -fx-border-color: #374151; -fx-border-radius: 6;");
        scopeCombo.setOnAction(e -> onScopeChanged.accept(scopeCombo.getValue()));

        // Interactive Tax Reserve Slider
        Label taxHdr = createSmallHeader("ESTIMATED TAX RESERVE (%)");
        Label taxValLabel = new Label(String.format("Set-Aside Rate: %.0f%%", currentTaxRate));
        taxValLabel.setTextFill(Color.web("#34d399"));
        taxValLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));

        Slider taxSlider = new Slider(5, 40, currentTaxRate);
        taxSlider.setShowTickMarks(true);
        taxSlider.setMajorTickUnit(10);
        taxSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double rounded = Math.round(newV.doubleValue());
            taxValLabel.setText(String.format("Set-Aside Rate: %.0f%%", rounded));
            onTaxRateChanged.accept(rounded);
        });

        // Studio Revenue Target Slider
        Label goalHdr = createSmallHeader("REVENUE GOAL TARGET ($)");
        Label goalValLabel = new Label(String.format("Target: $%,.0f", currentRevenueGoal));
        goalValLabel.setTextFill(Color.web("#d4af37"));
        goalValLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));

        Slider goalSlider = new Slider(5000, 50000, currentRevenueGoal);
        goalSlider.setBlockIncrement(2500);
        goalSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double rounded = Math.round(newV.doubleValue() / 500.0) * 500.0;
            goalValLabel.setText(String.format("Target: $%,.0f", rounded));
            onGoalChanged.accept(rounded);
        });

        Label exportHdr = createSmallHeader("EXECUTIVE REPORTS & SAFETY");
        Button btnPrintReport = new Button("🖨  Printable CFO Report");
        btnPrintReport.setMaxWidth(Double.MAX_VALUE);
        btnPrintReport.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 7 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnPrintReport.setOnAction(e -> onExportHtmlReport.run());

        Button btnBackup = new Button("🛡  Backup CSV Snapshot");
        btnBackup.setMaxWidth(Double.MAX_VALUE);
        btnBackup.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 7 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnBackup.setOnAction(e -> onBackupCsv.run());

        Button btnBackLedger = new Button("←  Back to Ledger Table");
        btnBackLedger.setMaxWidth(Double.MAX_VALUE);
        btnBackLedger.setStyle("-fx-background-color: #111827; -fx-text-fill: #e5e7eb; -fx-font-size: 11px; -fx-padding: 7 10; -fx-background-radius: 6; -fx-border-color: #1f2937; -fx-cursor: hand;");
        btnBackLedger.setOnAction(e -> onSwitchBackToLedger.run());

        sidebar.getChildren().addAll(
                brandBox,
                btnHide,
                new Separator(),
                scopeHdr, scopeCombo,
                new Separator(),
                taxHdr, taxValLabel, taxSlider,
                goalHdr, goalValLabel, goalSlider,
                new Separator(),
                exportHdr, btnPrintReport, btnBackup,
                new Separator(),
                btnBackLedger
        );

        ScrollPane sp = new ScrollPane(sidebar);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background: #000000; -fx-background-color: #000000; -fx-border-color: #26221a; -fx-border-width: 0 1 0 0;");
        sp.setPrefWidth(256);
        return sp;
    }

    // 4. RICH ANALYTICS DASHBOARD (Tax Cards + Revenue Goal + Equity Curve + Charts + Vendor Bars)
    public static void populateAnalyticsDashboard(
            VBox container,
            List<Transaction> allTransactions,
            double taxRatePercent,
            double revenueGoal,
            String scope,
            boolean isSidebarHidden,
            Runnable onToggleSidebar
    ) {
        container.getChildren().clear();

        List<Transaction> scopedList = filterByScope(allTransactions, scope);

        HBox topHeader = new HBox(14);
        topHeader.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("VISUALTIZITA STUDIO — EXECUTIVE ANALYTICS & EQUITY CURVE");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#d4af37"));

        Label subtitle = new Label("Scope: " + scope + " (" + scopedList.size() + " tx)  •  Tax Rate: " + String.format("%.0f%%", taxRatePercent) + "  •  Revenue Goal: $" + String.format("%,.0f", revenueGoal));
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#9ca3af"));
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnToggle = new Button(isSidebarHidden ? "☰  Show CFO Sidebar" : "⇄  Full-Screen Mode");
        btnToggle.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 6; -fx-border-color: #d4af37; -fx-border-radius: 6; -fx-cursor: hand;");
        btnToggle.setOnAction(e -> onToggleSidebar.run());

        topHeader.getChildren().addAll(titleBox, spacer, btnToggle);

        double grossDeposits = 0;
        double totalExpenses = 0;
        int depositCount = 0;
        Transaction largestDeposit = null;
        Transaction largestExpense = null;

        for (Transaction t : scopedList) {
            if (t.getAmount() > 0) {
                grossDeposits += t.getAmount();
                depositCount++;
                if (largestDeposit == null || t.getAmount() > largestDeposit.getAmount()) largestDeposit = t;
            } else if (t.getAmount() < 0) {
                totalExpenses += Math.abs(t.getAmount());
                if (largestExpense == null || Math.abs(t.getAmount()) > Math.abs(largestExpense.getAmount())) largestExpense = t;
            }
        }

        double netProfit = grossDeposits - totalExpenses;
        double estimatedTax = netProfit > 0 ? netProfit * (taxRatePercent / 100.0) : 0.0;
        double trueSpendable = netProfit - estimatedTax;
        double avgBooking = depositCount > 0 ? grossDeposits / depositCount : 1500.0;

        // 4 Executive KPI Cards
        HBox kpiRow = new HBox(14);
        VBox c1 = createMetricCard("NET STUDIO PROFIT", formatSigned(netProfit), "#d4af37",
                "Gross Deposits: +$" + String.format("%,.2f", grossDeposits));
        VBox c2 = createMetricCard("EST. TAX RESERVE (" + String.format("%.0f%%", taxRatePercent) + ")",
                String.format("$%,.2f", estimatedTax), "#f59e0b", "Recommended Tax Set-Aside");
        VBox c3 = createMetricCard("TRUE SPENDABLE CASH", formatSigned(trueSpendable), "#34d399",
                "Net Profit After Tax Reserve");
        VBox c4 = createMetricCard("AVG BOOKING / TOP CLIENT",
                String.format("$%,.0f avg", avgBooking), "#818cf8",
                largestDeposit != null ? "Max: +$" + String.format("%,.0f", largestDeposit.getAmount()) + " (" + truncate(largestDeposit.getVendor(), 11) + ")" : "No Deposits");

        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);
        HBox.setHgrow(c3, Priority.ALWAYS);
        HBox.setHgrow(c4, Priority.ALWAYS);
        kpiRow.getChildren().addAll(c1, c2, c3, c4);

        // Studio Revenue Goal Progress Tracker
        VBox goalBox = buildRevenueGoalCard(grossDeposits, revenueGoal, avgBooking);

        // Charts Row 1: PieChart + Monthly BarChart
        HBox chartsRow = new HBox(16);
        chartsRow.setPrefHeight(265);

        VBox pieCard = new VBox(6);
        pieCard.setPadding(new Insets(12));
        pieCard.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");
        Label pieTitle = new Label("REVENUE ALLOCATION BREAKDOWN");
        pieTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        pieTitle.setTextFill(Color.web("#d4af37"));

        PieChart pieChart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Spendable ($" + String.format("%,.0f", Math.max(0, trueSpendable)) + ")", Math.max(0.01, trueSpendable)),
                new PieChart.Data("Tax Reserve ($" + String.format("%,.0f", estimatedTax) + ")", Math.max(0.01, estimatedTax)),
                new PieChart.Data("Expenses ($" + String.format("%,.0f", totalExpenses) + ")", Math.max(0.01, totalExpenses))
        ));
        pieChart.setLabelsVisible(false);
        pieCard.getChildren().addAll(pieTitle, pieChart);

        VBox barCard = new VBox(6);
        barCard.setPadding(new Insets(12));
        barCard.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");
        Label barTitle = new Label("MONTHLY DEPOSITS VS. PAYMENTS");
        barTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        barTitle.setTextFill(Color.web("#d4af37"));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setAnimated(false);

        XYChart.Series<String, Number> depSeries = new XYChart.Series<>();
        depSeries.setName("Deposits (+)");
        XYChart.Series<String, Number> paySeries = new XYChart.Series<>();
        paySeries.setName("Payments (-)");

        Map<YearMonth, double[]> monthlyMap = new TreeMap<>();
        for (Transaction t : scopedList) {
            YearMonth ym = YearMonth.from(t.getDate());
            monthlyMap.putIfAbsent(ym, new double[]{0.0, 0.0});
            if (t.getAmount() > 0) monthlyMap.get(ym)[0] += t.getAmount();
            else monthlyMap.get(ym)[1] += Math.abs(t.getAmount());
        }
        for (Map.Entry<YearMonth, double[]> entry : monthlyMap.entrySet()) {
            String mLabel = entry.getKey().toString();
            depSeries.getData().add(new XYChart.Data<>(mLabel, entry.getValue()[0]));
            paySeries.getData().add(new XYChart.Data<>(mLabel, entry.getValue()[1]));
        }
        barChart.getData().addAll(depSeries, paySeries);
        barCard.getChildren().addAll(barTitle, barChart);

        HBox.setHgrow(pieCard, Priority.ALWAYS);
        HBox.setHgrow(barCard, Priority.ALWAYS);
        chartsRow.getChildren().addAll(pieCard, barCard);

        // Chart Row 2: Cumulative Net-Worth Equity Curve LineChart
        VBox equityCard = buildEquityCurveCard(scopedList);

        // Vendor Cash Flow Bars
        VBox vendorCard = buildVendorBreakdownCard(scopedList);

        container.getChildren().addAll(topHeader, kpiRow, goalBox, chartsRow, equityCard, vendorCard);
    }

    private static VBox buildRevenueGoalCard(double grossDeposits, double goal, double avgBooking) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(10, 16, 12, 16));
        box.setStyle("-fx-background-color: #111827; -fx-background-radius: 10; -fx-border-color: #26221a; -fx-border-radius: 10;");

        double pct = goal > 0 ? Math.min(1.0, grossDeposits / goal) : 1.0;
        double remaining = Math.max(0, goal - grossDeposits);
        int bookingsNeeded = avgBooking > 0 ? (int) Math.ceil(remaining / avgBooking) : 0;

        HBox top = new HBox(10);
        Label l1 = new Label(String.format("STUDIO REVENUE GOAL TRACKER ($%,.2f / $%,.0f)", grossDeposits, goal));
        l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l1.setTextFill(Color.web("#d4af37"));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        String statusText = remaining <= 0
                ? "🎉 GOAL ACHIEVED! (+100%)"
                : String.format("%.1f%% Complete  •  $%,.0f to go (~%d avg bookings needed)", pct * 100, remaining, bookingsNeeded);
        Label l2 = new Label(statusText);
        l2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l2.setTextFill(Color.web(remaining <= 0 ? "#34d399" : "#e5e7eb"));

        top.getChildren().addAll(l1, sp, l2);

        ProgressBar pb = new ProgressBar(pct);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(9);
        pb.setStyle("-fx-accent: #d4af37;");

        box.getChildren().addAll(top, pb);
        return box;
    }

    private static VBox buildEquityCurveCard(List<Transaction> scopedList) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setPrefHeight(230);
        card.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");

        Label title = new Label("CUMULATIVE STUDIO NET-WORTH EQUITY CURVE (CHRONOLOGICAL)");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        title.setTextFill(Color.web("#d4af37"));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<Transaction> chrono = new ArrayList<>(scopedList);
        chrono.sort(Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getTime));

        double running = 0;
        int step = 1;
        for (Transaction t : chrono) {
            running += t.getAmount();
            String label = "#" + (step++) + " (" + t.getDate().toString().substring(5) + ")";
            series.getData().add(new XYChart.Data<>(label, running));
        }
        lineChart.getData().add(series);

        card.getChildren().addAll(title, lineChart);
        return card;
    }

    // 5. TAB 3: STUDIO CFO SIMULATOR, LEDGER AUDITOR & RECURRING SUBSCRIPTIONS
    public static void populateCfoToolsTab(
            VBox container,
            List<Transaction> allTransactions,
            double taxRatePercent,
            Consumer<Transaction> onAddTransaction,
            Runnable onSeedDemoData,
            Runnable onBackupCsv
    ) {
        container.getChildren().clear();

        Label header = new Label("STUDIO CFO SUITE — EVENT QUOTE SIMULATOR, LEDGER AUDIT & RECURRING OVERHEAD");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        header.setTextFill(Color.web("#d4af37"));

        HBox topSplit = new HBox(16);

        // LEFT CARD: Interactive Event Package Quote & Profit Simulator
        VBox quoteCard = new VBox(10);
        quoteCard.setPadding(new Insets(16));
        quoteCard.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");
        HBox.setHgrow(quoteCard, Priority.ALWAYS);

        Label qTitle = new Label("1. INTERACTIVE EVENT QUOTE & PROFIT MARGIN SIMULATOR");
        qTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        qTitle.setTextFill(Color.web("#d4af37"));

        TextField clientField = new TextField("VisualTizita Wedding Client");
        clientField.setStyle("-fx-background-color: #1f2937; -fx-text-fill: white;");

        Slider priceSlider = new Slider(500, 8000, 3200);
        Slider crewCostSlider = new Slider(0, 2500, 500);
        Slider gearCostSlider = new Slider(0, 1500, 250);

        Label priceLbl = new Label("Client Package Quote: $3,200");
        priceLbl.setTextFill(Color.web("#e5e7eb"));
        Label crewLbl = new Label("Second Shooter / Crew Cost: $500");
        crewLbl.setTextFill(Color.web("#e5e7eb"));
        Label gearLbl = new Label("Lens / Lighting Rental Cost: $250");
        gearLbl.setTextFill(Color.web("#e5e7eb"));

        Label simResultLbl = new Label();
        simResultLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        simResultLbl.setTextFill(Color.web("#34d399"));

        Runnable updateQuoteMath = () -> {
            double pkg = Math.round(priceSlider.getValue() / 50.0) * 50.0;
            double crew = Math.round(crewCostSlider.getValue() / 25.0) * 25.0;
            double gear = Math.round(gearCostSlider.getValue() / 25.0) * 25.0;
            priceLbl.setText(String.format("Client Package Quote: $%,.0f", pkg));
            crewLbl.setText(String.format("Second Shooter / Crew Cost: $%,.0f", crew));
            gearLbl.setText(String.format("Lens / Lighting Rental Cost: $%,.0f", gear));

            double grossProfit = pkg - crew - gear;
            double tax = grossProfit > 0 ? grossProfit * (taxRatePercent / 100.0) : 0;
            double netKeep = grossProfit - tax;
            double margin = pkg > 0 ? (grossProfit / pkg) * 100.0 : 0;

            simResultLbl.setText(String.format(
                    "Gross Profit: $%,.0f (%.0f%% Margin)  |  After %.0f%% Tax: $%,.2f Net Take-Home",
                    grossProfit, margin, taxRatePercent, netKeep
            ));
        };
        priceSlider.valueProperty().addListener((o, a, b) -> updateQuoteMath.run());
        crewCostSlider.valueProperty().addListener((o, a, b) -> updateQuoteMath.run());
        gearCostSlider.valueProperty().addListener((o, a, b) -> updateQuoteMath.run());
        updateQuoteMath.run();

        Button btnPostRetainer = new Button("+ Post 50% Client Retainer Deposit to Ledger CSV");
        btnPostRetainer.setMaxWidth(Double.MAX_VALUE);
        btnPostRetainer.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;");
        btnPostRetainer.setOnAction(e -> {
            double pkg = Math.round(priceSlider.getValue() / 50.0) * 50.0;
            double retainer = pkg * 0.50;
            String client = clientField.getText().trim().isEmpty() ? "Event Client" : clientField.getText().trim();
            Transaction tx = new Transaction(LocalDate.now(), LocalTime.now(),
                    "50% Event Retainer (" + String.format("$%,.0f Package", pkg) + ")", client, retainer);
            onAddTransaction.accept(tx);
        });

        quoteCard.getChildren().addAll(
                qTitle,
                new Label("Client / Event Name:"), clientField,
                priceLbl, priceSlider,
                crewLbl, crewCostSlider,
                gearLbl, gearCostSlider,
                new Separator(),
                simResultLbl, btnPostRetainer
        );

        // RIGHT CARD: Recurring Studio Subscriptions & Demo Data Manager
        VBox overheadCard = new VBox(10);
        overheadCard.setPadding(new Insets(16));
        overheadCard.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");
        HBox.setHgrow(overheadCard, Priority.ALWAYS);

        Label oTitle = new Label("2. RECURRING STUDIO OVERHEAD & DEMO DATA TOOLS");
        oTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        oTitle.setTextFill(Color.web("#d4af37"));

        Label subList = new Label(
                "• Adobe Creative Cloud Pro + Lightroom: -$59.99/mo\n" +
                        "• AWS S3 / Amplify Event Photo Cloud: -$42.50/mo\n" +
                        "• Studio Liability & Camera Gear Insurance: -$85.00/mo\n" +
                        "• Total Fixed Monthly Studio Burn Rate: -$187.49/mo"
        );
        subList.setTextFill(Color.web("#e5e7eb"));
        subList.setFont(Font.font("Segoe UI", 12));

        Button btnPostOverhead = new Button("⚡ Post Monthly Cloud & Software Bill (-$102.49)");
        btnPostOverhead.setMaxWidth(Double.MAX_VALUE);
        btnPostOverhead.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;");
        btnPostOverhead.setOnAction(e -> {
            Transaction tx = new Transaction(LocalDate.now(), LocalTime.now(),
                    "Monthly Adobe CC + AWS S3 Cloud Hosting", "Adobe & AWS Cloud", -102.49);
            onAddTransaction.accept(tx);
        });

        Label demoHdr = createSmallHeader("CLASS PRESENTATION & CSV SAFETY TOOLS");

        Button btnSeed = new Button("✨ Inject 6 Realistic VisualTizita Studio Transactions");
        btnSeed.setMaxWidth(Double.MAX_VALUE);
        btnSeed.setStyle("-fx-background-color: #d4af37; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSeed.setOnAction(e -> onSeedDemoData.run());

        Button btnSnap = new Button("🛡 Create Timestamped Backup of transactions.csv");
        btnSnap.setMaxWidth(Double.MAX_VALUE);
        btnSnap.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSnap.setOnAction(e -> onBackupCsv.run());

        overheadCard.getChildren().addAll(
                oTitle, subList, btnPostOverhead,
                new Separator(),
                demoHdr, btnSeed, btnSnap
        );

        topSplit.getChildren().addAll(quoteCard, overheadCard);

        // BOTTOM CARD: Automated Ledger Audit & Anomaly Detector
        VBox auditCard = buildAuditCard(allTransactions);

        container.getChildren().addAll(header, topSplit, auditCard);
    }

    private static VBox buildAuditCard(List<Transaction> allTransactions) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");

        Label title = new Label("3. AUTOMATED LEDGER AUDITOR & ANOMALY DETECTOR (LIVE DIAGNOSTICS)");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#d4af37"));
        card.getChildren().add(title);

        List<String> findings = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (Math.abs(t.getAmount()) < 0.005) {
                findings.add("⚠ ZERO-AMOUNT ENTRY: [" + t.getDate() + "] '" + t.getDescription() + "' (" + t.getVendor() + ") has $0.00 balance impact.");
            }
            if (t.getAmount() <= -400.0) {
                findings.add("🔍 HIGH-VALUE EXPENSE: [" + t.getDate() + "] -$" + String.format("%,.2f", Math.abs(t.getAmount())) + " paid to " + t.getVendor() + " — verify equipment/depreciation receipt.");
            }
            if (t.getDescription().length() <= 6 && !t.getDescription().contains(" ")) {
                findings.add("🚩 TEST / TYPO PLACEHOLDER DETECTED: [" + t.getDate() + "] Description '" + t.getDescription() + "' / Vendor '" + t.getVendor() + "' looks like test data (Right-click row in Tab 1 to Edit or Delete).");
            }
        }

        if (findings.isEmpty()) {
            Label clean = new Label("✅ All " + allTransactions.size() + " transactions passed audit checks with zero anomalies.");
            clean.setTextFill(Color.web("#34d399"));
            card.getChildren().add(clean);
        } else {
            for (String f : findings) {
                Label item = new Label(f);
                item.setTextFill(Color.web(f.startsWith("🚩") ? "#f87171" : "#fbbf24"));
                item.setFont(Font.font("Segoe UI", 12));
                card.getChildren().add(item);
            }
        }
        return card;
    }

    // 6. BACKUP & DEMO SEEDING HELPERS
    public static String createCsvBackupSnapshot() {
        try {
            File src = new File("transactions.csv");
            if (!src.exists()) return "transactions.csv not found.";
            String stamp = LocalDate.now().toString() + "_" + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            File dest = new File("transactions_backup_" + stamp + ".csv");
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "Created backup: " + dest.getName();
        } catch (Exception e) {
            return "Backup failed: " + e.getMessage();
        }
    }

    public static List<Transaction> generateRealisticStudioTransactions() {
        LocalDate today = LocalDate.now();
        return Arrays.asList(
                new Transaction(today.minusDays(28), LocalTime.of(10, 15, 0), "Full Wedding Cinema & Photo Package", "Habesha Wedding Client", 3500.00),
                new Transaction(today.minusDays(24), LocalTime.of(14, 20, 0), "Sony FX3 & G-Master Lens Rig", "B&H Photo Video", -680.00),
                new Transaction(today.minusDays(19), LocalTime.of(11, 5, 0), "Corporate Gala Live QR Photo Booth", "Seattle Tech Summit", 1850.00),
                new Transaction(today.minusDays(14), LocalTime.of(16, 45, 0), "Second Shooter & Editor Payout", "Freelance Media Crew", -450.00),
                new Transaction(today.minusDays(7), LocalTime.of(13, 30, 0), "Graduation Portrait Package", "UW Senior Client", 425.00),
                new Transaction(today.minusDays(2), LocalTime.of(9, 10, 0), "AWS S3 & Cloud Gallery Storage", "Amazon Web Services", -64.50)
        );
    }

    private static VBox buildVendorBreakdownCard(List<Transaction> list) {
        Map<String, Double> vendorTotals = new LinkedHashMap<>();
        double totalVolume = 0;
        for (Transaction t : list) {
            vendorTotals.put(t.getVendor(), vendorTotals.getOrDefault(t.getVendor(), 0.0) + t.getAmount());
            totalVolume += Math.abs(t.getAmount());
        }

        VBox breakdownCard = new VBox(10);
        breakdownCard.setPadding(new Insets(16, 20, 16, 20));
        breakdownCard.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-border-color: #26221a; -fx-border-radius: 12;");

        Label cardTitle = new Label("NET CASH FLOW BY VENDOR / CLIENT");
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        cardTitle.setTextFill(Color.web("#d4af37"));
        breakdownCard.getChildren().add(cardTitle);

        if (vendorTotals.isEmpty()) {
            Label empty = new Label("No transactions in selected scope.");
            empty.setTextFill(Color.web("#9ca3af"));
            breakdownCard.getChildren().add(empty);
        } else {
            for (Map.Entry<String, Double> entry : vendorTotals.entrySet()) {
                String vendor = entry.getKey();
                double amt = entry.getValue();

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);

                Label vLabel = new Label(vendor);
                vLabel.setPrefWidth(210);
                vLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                vLabel.setTextFill(Color.web("#e5e7eb"));

                ProgressBar bar = new ProgressBar(totalVolume > 0 ? Math.min(1.0, Math.abs(amt) / totalVolume) * 1.8 : 0);
                bar.setPrefWidth(340);
                bar.setStyle(amt >= 0 ? "-fx-accent: #34d399;" : "-fx-accent: #f87171;");

                Label aLabel = new Label(formatSigned(amt));
                aLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                aLabel.setTextFill(Color.web(amt >= 0 ? "#34d399" : "#f87171"));

                row.getChildren().addAll(vLabel, bar, aLabel);
                breakdownCard.getChildren().add(row);
            }
        }
        return breakdownCard;
    }

    public static void openEditTransactionModal(Transaction target, List<Transaction> allTransactions, Runnable onSaveCallback) {
        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle("Edit / Modify Studio Transaction");
        dialog.setHeaderText("Modify details below — transactions.csv will update automatically");

        ButtonType saveBtn = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));

        DatePicker datePicker = new DatePicker(target.getDate());
        TextField timeField = new TextField(target.getTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        TextField descField = new TextField(target.getDescription());
        descField.setPrefWidth(260);
        TextField vendorField = new TextField(target.getVendor());
        TextField amountField = new TextField(String.format("%.2f", target.getAmount()));

        grid.add(new Label("Date:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Time (HH:mm:ss):"), 0, 1);
        grid.add(timeField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descField, 1, 2);
        grid.add(new Label("Vendor:"), 0, 3);
        grid.add(vendorField, 1, 3);
        grid.add(new Label("Signed Amount ($):"), 0, 4);
        grid.add(amountField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    LocalDate newDate = datePicker.getValue();
                    LocalTime newTime = LocalTime.parse(timeField.getText().trim());
                    String newDesc = descField.getText().trim();
                    String newVendor = vendorField.getText().trim();
                    double newAmt = Double.parseDouble(amountField.getText().trim());
                    return new Transaction(newDate, newTime, newDesc, newVendor, newAmt);
                } catch (Exception ex) {
                    Alert a = new Alert(Alert.AlertType.WARNING, "Invalid input format. Check time (HH:mm:ss) and numeric amount.");
                    a.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            int idx = allTransactions.indexOf(target);
            if (idx >= 0) {
                allTransactions.set(idx, updated);
                onSaveCallback.run();
            }
        });
    }

    public static void generateHtmlInvoice(Transaction t) {
        try {
            boolean isDeposit = t.getAmount() >= 0;
            String docType = isDeposit ? "CLIENT DEPOSIT RECEIPT" : "STUDIO PAYMENT VOUCHER";
            String accent = isDeposit ? "#10b981" : "#ef4444";
            String formattedAmt = formatSigned(t.getAmount());

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <meta charset="UTF-8">
                    <title>VisualTizita Studio Invoice</title>
                    <style>
                        body { background:#0b0f19; color:#f9fafb; font-family:'Segoe UI',sans-serif; padding:40px; }
                        .card { max-width:680px; margin:0 auto; background:#111827; border:2px solid #d4af37; border-radius:14px; padding:36px; }
                        .brand { color:#d4af37; font-size:26px; font-weight:800; letter-spacing:1px; }
                        .tag { color:#9ca3af; font-size:13px; font-style:italic; margin-bottom:24px; }
                        .badge { display:inline-block; background:%s; color:#fff; padding:6px 14px; border-radius:6px; font-weight:bold; font-size:12px; }
                        table { width:100%%; margin-top:24px; border-collapse:collapse; }
                        td { padding:14px 8px; border-bottom:1px solid #1f2937; font-size:15px; }
                        .lbl { color:#9ca3af; font-weight:bold; width:38%%; }
                        .amt { font-size:26px; font-weight:800; color:%s; }
                        .btn { margin-top:28px; background:#d4af37; color:#000; border:none; padding:10px 22px; font-weight:bold; border-radius:6px; cursor:pointer; }
                        @media print { .btn { display:none; } body { background:#fff; color:#000; } .card { background:#fff; } }
                    </style>
                    </head>
                    <body>
                        <div class="card">
                            <div class="brand">VISUAL TIZITA MEDIA & EVENT STUDIO</div>
                            <div class="tag">Memories that last forever — Official Accounting Voucher</div>
                            <div class="badge">%s</div>
                            <table>
                                <tr><td class="lbl">Transaction Date</td><td>%s</td></tr>
                                <tr><td class="lbl">Timestamp</td><td>%s</td></tr>
                                <tr><td class="lbl">Client / Vendor</td><td><strong>%s</strong></td></tr>
                                <tr><td class="lbl">Description</td><td>%s</td></tr>
                                <tr><td class="lbl">Total Amount</td><td class="amt">%s</td></tr>
                            </table>
                            <button class="btn" onclick="window.print()">🖨 Print / Save as PDF</button>
                        </div>
                    </body>
                    </html>
                    """.formatted(
                    accent, accent, docType,
                    t.getDate(),
                    t.getTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    t.getVendor(),
                    t.getDescription(),
                    formattedAmt
            );

            File out = new File("visualtizita-invoice.html");
            try (FileWriter fw = new FileWriter(out)) {
                fw.write(html);
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(out.toURI());
            }
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Could not open browser invoice: " + ex.getMessage());
            a.showAndWait();
        }
    }

    public static void generateExecutiveReportHtml(List<Transaction> list, double taxRate) {
        double dep = 0, pay = 0;
        for (Transaction t : list) {
            if (t.getAmount() > 0) dep += t.getAmount();
            else pay += Math.abs(t.getAmount());
        }
        double net = dep - pay;
        double tax = net > 0 ? net * (taxRate / 100.0) : 0;
        Transaction dummy = new Transaction(LocalDate.now(), LocalTime.now(),
                String.format("Executive Summary (Deposits: +$%,.2f | Expenses: -$%,.2f | Est Tax: $%,.2f)", dep, pay, tax),
                "VisualTizita Studio CFO Report", net);
        generateHtmlInvoice(dummy);
    }

    private static List<Transaction> filterByScope(List<Transaction> all, String scope) {
        LocalDate now = LocalDate.now();
        if ("Current Year (YTD)".equals(scope)) {
            return all.stream().filter(t -> t.getDate().getYear() == now.getYear()).collect(Collectors.toList());
        } else if ("Current Month (MTD)".equals(scope)) {
            return all.stream().filter(t -> t.getDate().getYear() == now.getYear() && t.getDate().getMonth() == now.getMonth()).collect(Collectors.toList());
        }
        return all;
    }

    private static VBox createMetricCard(String title, String mainVal, String colorHex, String subText) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #111827; -fx-background-radius: 10; -fx-border-color: #26221a; -fx-border-radius: 10;");

        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        t.setTextFill(Color.web("#9ca3af"));

        Label v = new Label(mainVal);
        v.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 19));
        v.setTextFill(Color.web(colorHex));

        Label s = new Label(subText);
        s.setFont(Font.font("Segoe UI", 11));
        s.setTextFill(Color.web("#6b7280"));

        card.getChildren().addAll(t, v, s);
        return card;
    }

    private static Label createSmallHeader(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        l.setTextFill(Color.web("#d4af37"));
        l.setPadding(new Insets(4, 0, 2, 0));
        return l;
    }

    private static String formatSigned(double amt) {
        if (Math.abs(amt) < 0.005) return "+$0.00";
        return amt < 0 ? String.format("-$%,.2f", Math.abs(amt)) : String.format("+$%,.2f", amt);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}