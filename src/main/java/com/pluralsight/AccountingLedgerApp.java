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
                    System.out.println("Add Deposit selected");
                    // addDeposit();
                    break;
                case "P":
                    System.out.println("Make Payment selected");
                    // makePayment();
                    break;
                case "L":
                    System.out.println("Ledger selected");
                    // ledgerScreen();
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