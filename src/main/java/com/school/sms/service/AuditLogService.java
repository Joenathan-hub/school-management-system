package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.model.AuditLogEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLogService {

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public List<AuditLogEntry> getRecent(int limit) {
        return auditLogDAO.findAll().stream().limit(limit).collect(Collectors.toList());
    }

    public List<AuditLogEntry> search(String query, LocalDate fromDate, LocalDate toDate) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return auditLogDAO.findAll().stream()
                .filter(e -> q.isBlank() ||
                        (e.getAction() != null && e.getAction().toLowerCase().contains(q)) ||
                        (e.getDetails() != null && e.getDetails().toLowerCase().contains(q)) ||
                        (e.getUserFullName() != null && e.getUserFullName().toLowerCase().contains(q)))
                .filter(e -> fromDate == null || (e.getTimestamp() != null && !e.getTimestamp().toLocalDate().isBefore(fromDate)))
                .filter(e -> toDate == null || (e.getTimestamp() != null && !e.getTimestamp().toLocalDate().isAfter(toDate)))
                .collect(Collectors.toList());
    }
}