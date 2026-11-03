package com.pluralsight;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Transaction represents a single financial entry (deposit or payment)
 * containing date, time, description, vendor, and amount.
 */
public class Transaction {
    private LocalDate date;
    private LocalTime time;
    private String description;
    private String vendor;
    private double amount;


    public Transaction(LocalDate date, LocalTime time, String description, String vendor, double amount) {
        this.date = date;
        this.time = time;
        this.description = description;
        this.vendor = vendor;
        this.amount = amount;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getDescription() { return description; }
    public String getVendor() { return vendor; }
    public double getAmount() { return amount; }

    // Setters
    public void setDate(LocalDate date) { this.date = date; }
    public void setTime(LocalTime time) { this.time = time; }
    public void setDescription(String description) { this.description = description; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public void setAmount(double amount) { this.amount = amount; }


    public String toCsvLine() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return String.format("%s %s|%s|%s|%.2f",
                date.format(dateFormatter),
                time.format(timeFormatter),
                description,
                vendor,
                amount);
    }

    @Override
    public String toString() {
        return String.format("Date: %s %s | Desc: %s | Vendor: %s | Amount: $%.2f",
                date, time, description, vendor, amount);
    }
}