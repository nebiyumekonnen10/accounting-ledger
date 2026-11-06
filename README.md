# Accounting Ledger App - Java Capstone 1

Welcome to my Java Capstone 1 project! This is a command line financial ledger application built for VisualTizita Media & Event Studio to track all incoming client deposits and outgoing expenses.

## Project Overview

This project was built to meet all the requirements for the Pluralsight / Year Up Java Core curriculum. It reads and writes from a shared transaction log (`transactions.csv`) using standard Java file I/O and object-oriented principles.

## Features

* **Add Deposits (D):** Log incoming client payments with automated timestamps.
* **Make Payments (P):** Log outgoing expenses (automatically formatted as negative numbers).
* **Ledger Views (L):** View all transactions, deposits only, or payments only (sorted newest first).
* **Financial Reports (R):** Filter by Month-to-Date, Previous Month, Year-to-Date, Previous Year, or run a custom multi-parameter search.

## How It Works (Code Architecture)

* **`Transaction.java`:** Our core data model using encapsulation (private fields for `LocalDate`, `LocalTime`, description, vendor, and amount).
* **`AccountingLedgerApp.java`:** The main command line application containing our `main` method, menu loops (`Scanner` and `switch` statements), and file reading/writing logic.

## How to Run

1. Open the project in IntelliJ IDEA.
2. Open `AccountingLedgerApp.java`.
3. Click the green **Run** button next to the `main` method to launch the console menu.