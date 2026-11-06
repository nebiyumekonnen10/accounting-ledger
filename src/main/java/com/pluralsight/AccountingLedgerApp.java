package com.pluralsight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

public class AccountingLedgerApp {

    private static final ArrayList<Transaction> transactions = new ArrayList<>();
    // Scanner for user input
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        loadTransactions();
        homeScreen();
    }

    /**
     * Displays the main Home Screen menu and handles user navigation.
     */
    public static void homeScreen() {
        boolean running = true;

        while (running) {
            System.out.println("\n=======================================");
            System.out.println("   VISUALTIZITA MEDIA & EVENT STUDIO");
            System.out.println("         ACCOUNTING LEDGER");
            System.out.println("=======================================");
            System.out.println("Welcome! Please select an option:");
            System.out.println(" D) Add Deposit");
            System.out.println(" P) Make Payment (Debit)");
            System.out.println(" L) Ledger");
            System.out.println(" X) Exit");
            System.out.print("Your choice: ");

            String choice = scanner.nextLine().trim().toUpperCase();

            switch (choice) {
                case "D":
                    addDeposit();
                    break;
                case "P":
                    makePayment();
                    break;
                case "L":
                    ledgerScreen();
                    break;
                case "X":
                    System.out.println("Exiting the application. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }


    /**
     * Prompts the user for deposit details and saves the transaction.
     */
    public static void addDeposit() {
        System.out.println("\n--- Add Deposit ---");
        System.out.print("Enter description: ");
        String description = scanner.nextLine().trim();

        System.out.print("Enter vendor: ");
        String vendor = scanner.nextLine().trim();

        System.out.print("Enter deposit amount: $");
        double amount = 0;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
            // Validation: Deposits must be positive
            if (amount <= 0) {
                System.out.println("Error: Deposit amount must be greater than zero. Transaction cancelled.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format. Transaction cancelled.");
            return;
        }

        // Automatically capture current date and time
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        // Create transaction
        Transaction deposit = new Transaction(date, time, description, vendor, amount);
        transactions.add(deposit);
        saveTransaction(deposit);

        System.out.println("Successfully recorded deposit of $" + String.format("%.2f", amount) + "!");
    }



    /**
     * Prompts the user for payment details and saves the transaction as a debit (negative amount).
     */
    public static void makePayment() {
        System.out.println("\n--- Make Payment (Debit) ---");
        System.out.print("Enter description: ");
        String description = scanner.nextLine().trim();

        System.out.print("Enter vendor: ");
        String vendor = scanner.nextLine().trim();

        System.out.print("Enter payment amount: $");
        double amount = 0;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
            if (amount <= 0) {
                System.out.println("Error: Enter a positive payment amount. It will be deducted automatically. Transaction cancelled.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format. Transaction cancelled.");
            return;
        }

        // Convert the positive amount into a negative value for the ledger
        double debitAmount = -Math.abs(amount);

        // Automatically capture current date and time
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        // Create transaction, add to memory, and save to CSV
        Transaction payment = new Transaction(date, time, description, vendor, debitAmount);
        transactions.add(payment);
        saveTransaction(payment);

        System.out.println("Successfully recorded payment of $" + String.format("%.2f", amount) + "!");
    }


    /**
     * Displays the Ledger Menu for viewing and filtering transactions.
     */
    public static void ledgerScreen() {
        boolean inLedger = true;

        while (inLedger) {
            System.out.println("\n=======================================");
            System.out.println("             LEDGER MENU");
            System.out.println("=======================================");
            System.out.println(" A) All Entries");
            System.out.println(" D) Deposits");
            System.out.println(" P) Payments");
            System.out.println(" R) Reports");
            System.out.println(" H) Home (Back)");
            System.out.print("Your choice: ");

            String choice = scanner.nextLine().trim().toUpperCase();

            switch (choice) {
                case "A":
                    displayAllEntries();
                    break;
                case "D":
                    displayDeposits();
                    break;
                case "P":
                    displayPayments();
                    break;
                case "R":
                    reportsScreen();
                    break;
                case "H":
                    inLedger = false; // Exits the ledger loop, returning to Home Screen
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    /**
     * Displays all transactions sorted from newest to oldest.
     */
    public static void displayAllEntries() {
        System.out.println("\n--- All Entries (Newest First) ---");

        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        // Create a copy of the list so we can sort it without messing up the main memory
        ArrayList<Transaction> sortedList = new ArrayList<>(transactions);

        // Sort descending by Date, then by Time
        sortedList.sort((t1, t2) -> {
            int dateCompare = t2.getDate().compareTo(t1.getDate());
            if (dateCompare == 0) {
                return t2.getTime().compareTo(t1.getTime());
            }
            return dateCompare;
        });

        // Print the sorted list
        for (Transaction t : sortedList) {
            System.out.println(t);
        }
    }



    /**
     * Displays only the deposit entries (positive amounts).
     */
    public static void displayDeposits() {
        System.out.println("\n--- Deposit Entries ---");
        boolean found = false;

        for (Transaction t : transactions) {
            if (t.getAmount() > 0) {
                System.out.println(t);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No deposits found.");
        }
    }

    /**
     * Displays only the payment/debit entries (negative amounts).
     */
    public static void displayPayments() {
        System.out.println("\n--- Payment Entries ---");
        boolean found = false;

        for (Transaction t : transactions) {
            if (t.getAmount() < 0) {
                System.out.println(t);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No payments found.");
        }
    }



    /**
     * Displays the Reports Menu for date-based and vendor-based filtering.
     */
    public static void reportsScreen() {
        boolean inReports = true;

        while (inReports) {
            System.out.println("\n=======================================");
            System.out.println("             REPORTS MENU");
            System.out.println("=======================================");
            System.out.println(" 1) Month to Date");
            System.out.println(" 2) Previous Month");
            System.out.println(" 3) Year to Date");
            System.out.println(" 4) Previous Year");
            System.out.println(" 5) Search by Vendor");
            System.out.println(" 6) Custom Search");
            System.out.println(" 0) Back");
            System.out.print("Your choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    filterMonthToDate();
                    break;
                case "2":
                    filterPreviousMonth();
                    break;
                case "3":
                    filterYearToDate();
                    break;
                case "4":
                    filterPreviousYear();
                    break;
                case "5":
                    filterByVendor();
                    break;
                case "6":
                    customSearch();
                    break;
                case "0":
                    inReports = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }


    /**
     * Prompts the user for a vendor name and displays all matching transactions (case-insensitive).
     */
    public static void filterByVendor() {
        System.out.println("\n--- Search by Vendor ---");
        System.out.print("Enter vendor name to search: ");
        String vendorSearch = scanner.nextLine().trim();

        boolean found = false;
        System.out.println("\nResults for vendor: \"" + vendorSearch + "\"");

        for (Transaction t : transactions) {
            if (t.getVendor().equalsIgnoreCase(vendorSearch)) {
                System.out.println(t);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No transactions found for vendor: " + vendorSearch);
        }
    }



    /**
     * Displays transactions from the current month and year.
     */
    public static void filterMonthToDate() {
        System.out.println("\n--- Month to Date ---");
        LocalDate today = LocalDate.now();
        boolean found = false;

        for (Transaction t : transactions) {
            if (t.getDate().getYear() == today.getYear() && t.getDate().getMonth() == today.getMonth()) {
                System.out.println(t);
                found = true;
            }
        }

        if (!found) System.out.println("No transactions found for this month.");
    }

    /**
     * Displays transactions from the previous month.
     */
    public static void filterPreviousMonth() {
        System.out.println("\n--- Previous Month ---");
        LocalDate previousMonthDate = LocalDate.now().minusMonths(1);
        boolean found = false;

        for (Transaction t : transactions) {
            if (t.getDate().getYear() == previousMonthDate.getYear() && t.getDate().getMonth() == previousMonthDate.getMonth()) {
                System.out.println(t);
                found = true;
            }
        }

        if (!found) System.out.println("No transactions found for the previous month.");
    }

    /**
     * Displays transactions from the current year.
     */
    public static void filterYearToDate() {
        System.out.println("\n--- Year to Date ---");
        int currentYear = LocalDate.now().getYear();
        boolean found = false;

        for (Transaction t : transactions) {
            if (t.getDate().getYear() == currentYear) {
                System.out.println(t);
                found = true;
            }
        }

        if (!found) System.out.println("No transactions found for this year.");
    }

    /**
     * Displays transactions from the previous year.
     */
    public static void filterPreviousYear() {
        System.out.println("\n--- Previous Year ---");
        int previousYear = LocalDate.now().getYear() - 1;
        boolean found = false;

        for (Transaction t : transactions) {
            if (t.getDate().getYear() == previousYear) {
                System.out.println(t);
                found = true;
            }
        }

        if (!found) System.out.println("No transactions found for the previous year.");
    }



    /**
     * Prompts the user for multiple optional filters (Dates, Strings, Amounts).
     * If the user leaves a prompt blank, that filter is ignored.
     */
    public static void customSearch() {
        System.out.println("\n--- Custom Search ---");
        System.out.println("(Press ENTER to skip any filter)");

        try {
            System.out.print("Start Date (yyyy-MM-dd): ");
            String startDateInput = scanner.nextLine().trim();
            LocalDate startDate = startDateInput.isEmpty() ? null : LocalDate.parse(startDateInput);

            System.out.print("End Date (yyyy-MM-dd): ");
            String endDateInput = scanner.nextLine().trim();
            LocalDate endDate = endDateInput.isEmpty() ? null : LocalDate.parse(endDateInput);

            System.out.print("Description: ");
            String descInput = scanner.nextLine().trim().toLowerCase();

            System.out.print("Vendor: ");
            String vendorInput = scanner.nextLine().trim().toLowerCase();

            System.out.print("Minimum Amount: ");
            String minAmountInput = scanner.nextLine().trim();
            Double minAmount = minAmountInput.isEmpty() ? null : Double.parseDouble(minAmountInput);

            System.out.print("Maximum Amount: ");
            String maxAmountInput = scanner.nextLine().trim();
            Double maxAmount = maxAmountInput.isEmpty() ? null : Double.parseDouble(maxAmountInput);

            System.out.println("\n--- Search Results ---");
            boolean found = false;

            for (Transaction t : transactions) {
                boolean matches = true;

                // Date Filters
                if (startDate != null && t.getDate().isBefore(startDate)) matches = false;
                if (endDate != null && t.getDate().isAfter(endDate)) matches = false;

                if (!descInput.isEmpty() && !t.getDescription().toLowerCase().contains(descInput)) matches = false;
                if (!vendorInput.isEmpty() && !t.getVendor().toLowerCase().contains(vendorInput)) matches = false;

                // Amount Filters
                if (minAmount != null && t.getAmount() < minAmount) matches = false;
                if (maxAmount != null && t.getAmount() > maxAmount) matches = false;

                if (matches) {
                    System.out.println(t);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("No transactions matched your search criteria.");
            }

        } catch (Exception e) {
            System.out.println("Error: Invalid input format. Please ensure dates use yyyy-MM-dd and amounts are numbers.");
        }
    }


    /**
     * Loads transactions from transactions.csv into the global ArrayList using BufferedReader.
     */
    public static void loadTransactions() {
        transactions.clear();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("transactions.csv"));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tokens = line.split(Pattern.quote("|"));

                if (tokens.length == 4) {
                    String[] dateTimeParts = tokens[0].split(" ");
                    LocalDate date = LocalDate.parse(dateTimeParts[0]);
                    LocalTime time = LocalTime.parse(dateTimeParts[1]);
                    String description = tokens[1];
                    String vendor = tokens[2];
                    double amount = Double.parseDouble(tokens[3]);

                    Transaction transaction = new Transaction(date, time, description, vendor, amount);
                    transactions.add(transaction);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading transactions.csv: " + e.getMessage());
        }
    }

    /**
     * Appends a new transaction to the transactions.csv file.
     */
    public static void saveTransaction(Transaction transaction) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("transactions.csv", true));
            writer.write(transaction.toCsvLine());
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            System.out.println("Error saving transaction: " + e.getMessage());
        }
    }
}