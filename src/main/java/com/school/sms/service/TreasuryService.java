package com.school.sms.service;

import com.school.sms.dao.ExpenditureDAO;
import com.school.sms.dao.PaymentDAO;

public class TreasuryService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ExpenditureDAO expenditureDAO = new ExpenditureDAO();

    public static class TreasurySummary {
        public double totalCollected;
        public double totalExpenditure;
        public double remainingBalance;
    }

    /** Live snapshot for the treasury dashboard: collected, spent, and what's left. */
    public TreasurySummary getSummary() {
        TreasurySummary summary = new TreasurySummary();
        summary.totalCollected = paymentDAO.totalCollected();
        summary.totalExpenditure = expenditureDAO.totalExpenditure();
        summary.remainingBalance = summary.totalCollected - summary.totalExpenditure;
        return summary;
    }
}
