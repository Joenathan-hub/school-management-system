package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.SettingsDAO;
import com.school.sms.dao.WorkerDAO;
import com.school.sms.dao.WorkerPaymentDAO;
import com.school.sms.model.Role;
import com.school.sms.model.User;
import com.school.sms.model.Worker;
import com.school.sms.util.IdGenerator;

import java.time.LocalDate;

public class WorkerService {

    private final WorkerDAO workerDAO = new WorkerDAO();
    private final WorkerPaymentDAO workerPaymentDAO = new WorkerPaymentDAO();
    private final AuditLogDAO auditLog = new AuditLogDAO();
    private final SmsService smsService = new SmsService();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    /** Only the administrator can add workers. */
    public Worker addWorker(User actingUser, String fullName, String jobTitle, String contact,
                             double monthlySalary) {
        return addWorker(actingUser, fullName, jobTitle, contact, monthlySalary, LocalDate.now());
    }

    public Worker addWorker(User actingUser, String fullName, String jobTitle, String contact,
                            double monthlySalary, LocalDate dateJoined) {
        requireAdmin(actingUser, "add a worker");

        String schoolInitials = settingsDAO.get("school.initials", "SCH");
        String workerId = IdGenerator.generate(
                IdGenerator.Type.WORKER, schoolInitials, LocalDate.now(), workerDAO::workerIdExists);

        Worker worker = new Worker();
        worker.setWorkerId(workerId);
        worker.setFullName(fullName);
        worker.setJobTitle(jobTitle);
        worker.setContact(contact);
        worker.setDateJoined(dateJoined != null ? dateJoined : LocalDate.now());
        worker.setMonthlySalary(monthlySalary);
        worker.setActive(true);

        int dbId = workerDAO.insert(worker);
        worker.setId(dbId);

        auditLog.log(actingUser.getId(), "ADD_WORKER", "Added worker " + fullName + " (" + workerId + ")");
        return worker;
    }

    /** Only the administrator can remove (deactivate) workers. */
    public void removeWorker(User actingUser, int workerDbId) {
        requireAdmin(actingUser, "remove a worker");
        workerDAO.deactivate(workerDbId);
        auditLog.log(actingUser.getId(), "REMOVE_WORKER", "Deactivated worker #" + workerDbId);
    }

    public static class WorkerPaymentResult {
        public com.school.sms.model.WorkerPayment payment;
        public double balance;
    }

    /**
     * Only the bursar (or admin) can pay a worker. Deducts against their
     * monthly salary and notifies them by SMS with amount paid and balance.
     * Returns the exact date/time the payment was recorded (day/month/year, 24-hour clock).
     */
    public WorkerPaymentResult payWorker(User actingUser, Worker worker, double amount, String forMonth, String notes,
                                          java.time.LocalDate transactionDate) {
        if (actingUser.getRole() != Role.BURSAR && actingUser.getRole() != Role.ADMINISTRATOR) {
            throw new SecurityException("Only the bursar or administrator can pay workers.");
        }

        com.school.sms.model.WorkerPayment payment = workerPaymentDAO.insert(
                worker.getId(), amount, forMonth, actingUser.getId(), notes,
                com.school.sms.util.TransactionDateUtil.toTransactionDateTime(transactionDate));

        double totalPaid = workerPaymentDAO.totalPaidForMonth(worker.getId(), forMonth);
        double balance = worker.getMonthlySalary() - totalPaid;

        auditLog.log(actingUser.getId(), "PAY_WORKER",
                String.format("Paid %s UGX %,.0f for %s at %s. Balance: UGX %,.0f",
                        worker.getFullName(), amount, forMonth,
                        payment.getPaymentDateTime().format(com.school.sms.util.DateTimeUtil.DATE_TIME),
                        balance));

        if (worker.getContact() != null && !worker.getContact().isBlank()) {
            String message = balance <= 0
                    ? String.format("Dear %s, your salary for %s has been fully paid. Thank you for your work.",
                            worker.getFullName(), forMonth)
                    : String.format("Dear %s, you have been paid UGX %,.0f for %s. Remaining balance: UGX %,.0f.",
                            worker.getFullName(), amount, forMonth, balance);
            smsService.notify(worker.getContact(), message);
        }

        WorkerPaymentResult result = new WorkerPaymentResult();
        result.payment = payment;
        result.balance = balance;
        return result;
    }

    private void requireAdmin(User user, String action) {
        if (user.getRole() != Role.ADMINISTRATOR) {
            throw new SecurityException("Only the administrator can " + action + ".");
        }
    }
}
