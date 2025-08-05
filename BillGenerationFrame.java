package com.billingapp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class BillGenerationFrame extends JFrame {

    private JTextField searchField;
    private JButton searchButton, printButton, exportPdfButton, saveButton;
    private JPanel billPanel;
    private JLabel companyLabel, addressLabel, buyerLabel, subtotalLabel, totalLabel, transDetailLabel;
    private JTable productTable;
    private DefaultTableModel productTableModel;

    public BillGenerationFrame() {
        setTitle("Generate Bill");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(750, 650);
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Header
        companyLabel = new JLabel("My Company Name", SwingConstants.CENTER);
        companyLabel.setFont(new Font("Arial", Font.BOLD, 22));
        addressLabel = new JLabel("123 Business Rd, Bengaluru | 080-1234567", SwingConstants.CENTER);

        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.add(companyLabel);
        headerPanel.add(addressLabel);

        // Search
        JPanel searchPanel = new JPanel();
        searchField = new JTextField(25);
        searchButton = new JButton("Search Transaction");
        searchPanel.add(new JLabel("Transaction ID:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Bill Panel (the preview area)
        billPanel = new JPanel();
        billPanel.setLayout(new BoxLayout(billPanel, BoxLayout.Y_AXIS));

        buyerLabel = new JLabel();
        productTableModel = new DefaultTableModel(new String[]{"Product Name", "Quantity", "Unit Price", "Total Price"}, 0);
        productTable = new JTable(productTableModel);
        subtotalLabel = new JLabel();
        totalLabel = new JLabel();
        transDetailLabel = new JLabel();

        billPanel.add(Box.createVerticalStrut(8));
        billPanel.add(buyerLabel);
        billPanel.add(Box.createVerticalStrut(8));
        billPanel.add(new JScrollPane(productTable));
        billPanel.add(Box.createVerticalStrut(8));
        billPanel.add(subtotalLabel);
        billPanel.add(totalLabel);
        billPanel.add(Box.createVerticalStrut(8));
        billPanel.add(transDetailLabel);
        billPanel.add(Box.createVerticalStrut(12));

        JScrollPane billScroll = new JScrollPane(billPanel);

        // Buttons
        JPanel buttonPanel = new JPanel();
        printButton = new JButton("Print");
        exportPdfButton = new JButton("Export as PDF");
        saveButton = new JButton("Save");

        buttonPanel.add(printButton);
        buttonPanel.add(exportPdfButton);
        buttonPanel.add(saveButton);

        // Add all to frame
        add(headerPanel, BorderLayout.NORTH);
        add(searchPanel, BorderLayout.BEFORE_FIRST_LINE);
        add(billScroll, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Logic
        searchButton.addActionListener(e -> searchAndPreview());

        printButton.addActionListener(e -> {
            try { billPanel.print(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage()); }
        });

        exportPdfButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Export to PDF feature placeholder. Use PDFBox/iText for real jobs."));
        saveButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Save feature placeholder. Add DB persistence as needed."));
    }

    private void searchAndPreview() {
        String transId = searchField.getText().trim();
        if (transId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a Transaction ID");
            return;
        }
        productTableModel.setRowCount(0);

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Get buyer and transaction details
            String sql = "SELECT t.id, t.purchase_date, t.payment_method, b.name, b.address, b.phone, b.email "
                       + "FROM transactions t JOIN buyers b ON t.buyer_id = b.id WHERE t.id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, transId);
            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                buyerLabel.setText("Transaction not found.");
                subtotalLabel.setText("");
                totalLabel.setText("");
                transDetailLabel.setText("");
                return;
            }

            String bName = rs.getString("name");
            String bAddr = rs.getString("address");
            String bPhone = rs.getString("phone");
            String bEmail = rs.getString("email");
            String pDate = rs.getString("purchase_date");
            String payMeth = rs.getString("payment_method");

            buyerLabel.setText("<html><b>Buyer Name:</b> " + bName +
                               " &nbsp; <b>Phone:</b> " + bPhone +
                               "<br><b>Address:</b> " + bAddr +
                               "<br><b>Email:</b> " + bEmail + "</html>");

            // 2. Product table
            String psql = "SELECT p.name, tp.quantity, tp.unit_price "
                        + "FROM transaction_products tp JOIN products p ON tp.product_id = p.id "
                        + "WHERE tp.transaction_id = ?";
            PreparedStatement pps = conn.prepareStatement(psql);
            pps.setString(1, transId);
            ResultSet prs = pps.executeQuery();

            double subtotal = 0;
            while (prs.next()) {
                String prod = prs.getString(1);
                int qty = prs.getInt(2);
                double price = prs.getDouble(3);
                double total = qty * price;
                productTableModel.addRow(new Object[]{prod, qty, String.format("%.2f", price), String.format("%.2f", total)});
                subtotal += total;
            }
            subtotalLabel.setText("Subtotal: ₹" + String.format("%.2f", subtotal));
            totalLabel.setText("Total Amount Due: ₹" + String.format("%.2f", subtotal));

            transDetailLabel.setText("<html><b>Transaction ID:</b> " + transId +
                                  " &nbsp; <b>Date:</b> " + pDate +
                                  " &nbsp; <b>Payment:</b> " + payMeth + "</html>");

        } catch (Exception ex) {
            buyerLabel.setText("Database error: " + ex.getMessage());
        }
        billPanel.revalidate();
        billPanel.repaint();
    }

    // For testing: main()
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BillGenerationFrame::new);
    }
}
