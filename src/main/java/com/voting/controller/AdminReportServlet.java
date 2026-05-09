package com.voting.controller;

import com.voting.dao.ElectionDao;
import com.voting.dao.MongoLogDao;
import com.voting.dao.UserDao;
import com.voting.dao.VoteDao;
import com.voting.dao.OfficerOpsDao;
import com.voting.model.Election;
import com.voting.model.User;
import com.voting.model.VoteStat;
import com.voting.service.VotingService;
import com.voting.util.SessionUtil;
import org.bson.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/reports")
public class AdminReportServlet extends HttpServlet {
    private final VotingService votingService = new VotingService();
    private final ElectionDao electionDao = new ElectionDao();
    private final UserDao userDao = new UserDao();
    private final MongoLogDao mongoLogDao = new MongoLogDao();
    private final VoteDao voteDao = new VoteDao();
    private final OfficerOpsDao officerOpsDao = new OfficerOpsDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "admin")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String type = req.getParameter("type");
        try {
            if ("finalResults".equalsIgnoreCase(type)) {
                downloadFinalResultsPdf(resp);
            } else if ("resultsCsv".equalsIgnoreCase(type)) {
                downloadFinalResultsCsv(resp);
            } else if ("voterRoll".equalsIgnoreCase(type)) {
                downloadVoterRollCsv(resp);
            } else if ("securityAudit".equalsIgnoreCase(type)) {
                downloadSecurityAuditXml(resp);
            } else if ("complianceReport".equalsIgnoreCase(type)) {
                downloadComplianceReportPdf(resp);
            } else if ("evidenceCsv".equalsIgnoreCase(type)) {
                downloadEvidenceCsv(resp);
            } else if ("centerTurnout".equalsIgnoreCase(type)) {
                downloadCenterTurnoutCsv(resp);
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown report type.");
            }
        } catch (SQLException ex) {
            throw new ServletException("Unable to generate report", ex);
        }
    }

    private void downloadFinalResultsCsv(HttpServletResponse resp) throws IOException, SQLException {
        Election election = electionDao.findActiveElection();
        if (election == null) {
            election = electionDao.findLatestElection();
        }
        List<VoteStat> results = votingService.getResultsForActiveElection();
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"final-results.csv\"");
        StringBuilder csv = new StringBuilder();
        csv.append("Election ID,Title,Date,Status,Candidate,Vote Count,Winner\n");
        int topVotes = results.isEmpty() ? 0 : results.get(0).getTotalVotes();
        for (VoteStat result : results) {
            csv.append(csv(election == null ? "" : election.getId())).append(',')
                    .append(csv(election == null ? "" : election.getTitle())).append(',')
                    .append(csv(election == null ? "" : election.getElectionDate())).append(',')
                    .append(csv(election == null ? "" : election.getStatus())).append(',')
                    .append(csv(result.getCandidateName())).append(',')
                    .append(csv(result.getTotalVotes())).append(',')
                    .append(csv(result.getTotalVotes() > 0 && result.getTotalVotes() == topVotes ? "YES" : "NO"))
                    .append('\n');
        }
        resp.getWriter().write(csv.toString());
    }

    private void downloadFinalResultsPdf(HttpServletResponse resp) throws IOException, SQLException {
        Election election = electionDao.findActiveElection();
        if (election == null) {
            election = electionDao.findLatestElection();
        }
        List<VoteStat> results = votingService.getResultsForActiveElection();

        List<String> lines = new ArrayList<>();
        lines.add("State Election Commission");
        lines.add("Final Results Report");
        lines.add("Generated: " + Instant.now());
        lines.add("");
        if (election == null) {
            lines.add("No election data is available.");
        } else {
            lines.add("Election ID: " + election.getId());
            lines.add("Title: " + election.getTitle());
            lines.add("Date: " + election.getElectionDate());
            lines.add("Time: " + election.getStartTime() + " to " + election.getEndTime());
            lines.add("Status: " + election.getStatus());
            lines.add("");
            lines.add("Candidate Results");
            if (results.isEmpty()) {
                lines.add("No votes or candidates found for this election.");
            } else {
                int rank = 1;
                for (VoteStat result : results) {
                    lines.add(rank + ". " + result.getCandidateName() + " - " + result.getTotalVotes() + " vote(s)");
                    rank++;
                }
            }
        }

        byte[] pdf = SimplePdf.write(lines);
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"final-results.pdf\"");
        resp.setContentLength(pdf.length);
        resp.getOutputStream().write(pdf);
    }

    private void downloadVoterRollCsv(HttpServletResponse resp) throws IOException, SQLException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"voter-roll.csv\"");
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Email,Mobile,Voter ID,Date of Birth,Age,Election Center,City,State,Role,Voting Status\n");
        for (User user : userDao.findAll()) {
            csv.append(csv(user.getId())).append(',')
                    .append(csv(user.getName())).append(',')
                    .append(csv(user.getEmail())).append(',')
                    .append(csv(user.getMobileNumber())).append(',')
                    .append(csv(user.getVoterIdNumber())).append(',')
                    .append(csv(user.getDateOfBirth())).append(',')
                    .append(csv(user.getAge())).append(',')
                    .append(csv(user.getElectionCenter())).append(',')
                    .append(csv(user.getCity())).append(',')
                    .append(csv(user.getState())).append(',')
                    .append(csv(user.getRole())).append(',')
                    .append(csv("admin".equalsIgnoreCase(user.getRole()) ? "N/A" : (user.isHasVoted() ? "Voted" : "Pending")))
                    .append('\n');
        }
        resp.getWriter().write(csv.toString());
    }

    private void downloadSecurityAuditXml(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/xml; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"security-audit.xml\"");
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<securityAudit generatedAt=\"").append(xml(Instant.now())).append("\">\n");
        for (Document log : mongoLogDao.getRecentLogs()) {
            xml.append("  <event>\n");
            xml.append("    <timestamp>").append(xml(String.valueOf(log.get("timestamp")))).append("</timestamp>\n");
            xml.append("    <action>").append(xml(String.valueOf(log.get("action")))).append("</action>\n");
            xml.append("    <actor>").append(xml(log.get("actor") != null ? String.valueOf(log.get("actor")) : "System")).append("</actor>\n");
            xml.append("    <ipAddress>").append(xml(log.get("ip_address") != null ? String.valueOf(log.get("ip_address")) : "-")).append("</ipAddress>\n");
            xml.append("    <details>").append(xml(String.valueOf(log.get("details")))).append("</details>\n");
            xml.append("  </event>\n");
        }
        xml.append("</securityAudit>\n");
        resp.getWriter().write(xml.toString());
    }

    private void downloadComplianceReportPdf(HttpServletResponse resp) throws IOException, SQLException {
        Map<String, Object> integrity = voteDao.getIntegritySummary();
        Map<String, Object> suspicious = mongoLogDao.getSuspiciousActivitySummary();
        List<String> lines = new ArrayList<>();
        lines.add("State Election Commission");
        lines.add("Auditor Compliance Report");
        lines.add("Generated: " + Instant.now());
        lines.add("");
        lines.add("Integrity Verification");
        lines.add("Total votes: " + integrity.get("totalVotes"));
        lines.add("Missing receipts: " + integrity.get("missingReceipts"));
        lines.add("Missing signatures: " + integrity.get("missingSignatures"));
        lines.add("Duplicate receipts: " + integrity.get("duplicateReceipts"));
        lines.add("Status: " + (Boolean.TRUE.equals(integrity.get("verified")) ? "VERIFIED" : "REVIEW REQUIRED"));
        lines.add("");
        lines.add("Suspicious Activity Window");
        lines.add("Recent logs scanned: " + suspicious.get("logWindow"));
        lines.add("Failed logins: " + suspicious.get("failedLogins"));
        lines.add("Invalid OTP attempts: " + suspicious.get("invalidOtps"));
        lines.add("Account lockouts: " + suspicious.get("lockedAccounts"));
        lines.add("Denied/CSRF events: " + suspicious.get("deniedAccess"));
        lines.add("Anomaly score: " + suspicious.get("anomalyScore") + "/100 (" + suspicious.get("riskLevel") + ")");
        lines.add("Busiest IP: " + suspicious.get("busiestIp") + " (" + suspicious.get("busiestIpEvents") + " events)");
        lines.add("");
        lines.add("Election Timeline");
        for (Election election : electionDao.findAll()) {
            lines.add("#" + election.getId() + " " + election.getTitle() + " | " + election.getStatus() + " | " +
                    election.getElectionDate() + " " + election.getStartTime() + "-" + election.getEndTime());
        }
        lines.add("");
        lines.add("Recent Auditor Notes");
        for (Document note : mongoLogDao.getRecentAuditorNotes()) {
            String timestamp = String.valueOf(note.get("timestamp"));
            String author = String.valueOf(note.get("auditor_name"));
            String text = String.valueOf(note.get("note"));
            lines.add(timestamp + " | " + author + " | " + text);
        }

        byte[] pdf = SimplePdf.write(lines);
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"auditor-compliance-report.pdf\"");
        resp.setContentLength(pdf.length);
        resp.getOutputStream().write(pdf);
    }

    private void downloadEvidenceCsv(HttpServletResponse resp) throws IOException, SQLException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"auditor-evidence.csv\"");
        StringBuilder csv = new StringBuilder();
        csv.append("Vote ID,Election ID,Election Title,User ID,Voter ID,Candidate ID,Candidate,Receipt ID,IP Address,Digital Signature,Created At\n");
        for (Map<String, Object> row : voteDao.getEvidenceRows()) {
            csv.append(csv(row.get("voteId"))).append(',')
                    .append(csv(row.get("electionId"))).append(',')
                    .append(csv(row.get("electionTitle"))).append(',')
                    .append(csv(row.get("userId"))).append(',')
                    .append(csv(row.get("voterId"))).append(',')
                    .append(csv(row.get("candidateId"))).append(',')
                    .append(csv(row.get("candidateName"))).append(',')
                    .append(csv(row.get("receiptId"))).append(',')
                    .append(csv(row.get("ipAddress"))).append(',')
                    .append(csv(row.get("digitalSignature"))).append(',')
                    .append(csv(row.get("createdAt")))
                    .append('\n');
        }
        resp.getWriter().write(csv.toString());
    }

    private void downloadCenterTurnoutCsv(HttpServletResponse resp) throws IOException, SQLException {
        Election election = electionDao.findActiveElection();
        if (election == null) {
            election = electionDao.findLatestElection();
        }
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"center-turnout.csv\"");
        StringBuilder csv = new StringBuilder();
        csv.append("Election ID,Election Title,Center,Eligible Voters,Votes Cast,Turnout Percentage\n");
        if (election != null) {
            for (Map<String, Object> row : officerOpsDao.getCenterTurnout(election.getId())) {
                csv.append(csv(election.getId())).append(',')
                        .append(csv(election.getTitle())).append(',')
                        .append(csv(row.get("centerName"))).append(',')
                        .append(csv(row.get("eligibleCount"))).append(',')
                        .append(csv(row.get("votesCast"))).append(',')
                        .append(csv(row.get("turnoutPercentage")))
                        .append('\n');
            }
        }
        resp.getWriter().write(csv.toString());
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String xml(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static final class SimplePdf {
        private static byte[] write(List<String> lines) throws IOException {
            List<List<String>> pages = paginate(lines, 42);
            List<String> objects = new ArrayList<>();
            objects.add("<< /Type /Catalog /Pages 2 0 R >>");

            int firstPageObject = 4;
            StringBuilder kids = new StringBuilder();
            for (int i = 0; i < pages.size(); i++) {
                kids.append(firstPageObject + (i * 2)).append(" 0 R ");
            }
            objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
            objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

            for (int i = 0; i < pages.size(); i++) {
                int pageObject = firstPageObject + (i * 2);
                int contentObject = pageObject + 1;
                objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObject + " 0 R >>");
                String stream = contentStream(pages.get(i));
                objects.add("<< /Length " + stream.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + stream + "\nendstream");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(out.size());
                out.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
                out.write(objects.get(i).getBytes(StandardCharsets.ISO_8859_1));
                out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }

            int xrefOffset = out.size();
            out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (int i = 1; i < offsets.size(); i++) {
                out.write(String.format("%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.ISO_8859_1));
            }
            out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
            return out.toByteArray();
        }

        private static List<List<String>> paginate(List<String> lines, int maxLines) {
            List<List<String>> pages = new ArrayList<>();
            for (int i = 0; i < lines.size(); i += maxLines) {
                pages.add(lines.subList(i, Math.min(i + maxLines, lines.size())));
            }
            if (pages.isEmpty()) {
                pages.add(new ArrayList<String>());
            }
            return pages;
        }

        private static String contentStream(List<String> lines) {
            StringBuilder content = new StringBuilder();
            content.append("BT\n/F1 11 Tf\n50 760 Td\n14 TL\n");
            for (String line : lines) {
                content.append("(").append(pdf(line)).append(") Tj\nT*\n");
            }
            content.append("ET");
            return content.toString();
        }

        private static String pdf(String value) {
            String text = value == null ? "" : value;
            return text.replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replaceAll("[^\\x20-\\x7E]", "?");
        }
    }
}
