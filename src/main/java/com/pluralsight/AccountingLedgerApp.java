package com.pluralsight;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class AccountingLedgerApp {


    private static ArrayList<Transaction> transactions = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("=== Accounting Ledger Application ===");
        loadTransactions();

        System.out.println("Loaded " + transactions.size() + " transactions from file.");
        for (Transaction t : transactions) {
            System.out.println(t);
        }
    }


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
                    // tokens[0] is date and time combined: e.g. "2026-10-28 14:15:20"
                    String[] dateTimeParts = tokens[0].split(" ");
                    LocalDate date = LocalDate.parse(dateTimeParts[0]);
                    LocalTime time = LocalTime.parse(dateTimeParts[1]);

                    String description = tokens[1];
                    String vendor = tokens[2];
                    double amount = Double.parseDouble(tokens[3]);

                    // Instantiate Transaction and add to my ArrayList
                    Transaction transaction = new Transaction(date, time, description, vendor, amount);
                    transactions.add(transaction);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading transactions.csv: " + e.getMessage());
        }
    }
}