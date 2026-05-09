package com.voting.controller;

import com.voting.dao.VoteDao;
import com.voting.model.User;
import com.voting.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/user/receipt")
public class VoteReceiptServlet extends HttpServlet {
    private final VoteDao voteDao = new VoteDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "user")) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        String receiptId = req.getParameter("id");
        User user = SessionUtil.getLoggedInUser(req);
        try {
            Map<String, Object> receipt = voteDao.findReceipt(receiptId, user.getId());
            resp.setContentType("text/html; charset=UTF-8");
            if (receipt == null) {
                resp.getWriter().write("<!doctype html><title>Vote Receipt</title><p>Receipt not found.</p>");
                return;
            }
            resp.getWriter().write("<!doctype html><html><head><title>Vote Receipt</title><style>body{font-family:Arial,sans-serif;background:#f8fafc;color:#0f172a;padding:32px}.card{max-width:720px;margin:auto;background:#fff;border:1px solid #cbd5e1;border-radius:12px;padding:28px}.ok{color:#15803d;font-weight:800}</style></head><body><div class='card'>");
            resp.getWriter().write("<h1>Vote Receipt</h1><p class='ok'>Vote recorded. Candidate choice is private.</p>");
            row(resp, "Receipt ID", receipt.get("receiptId"));
            row(resp, "Election", receipt.get("title"));
            row(resp, "Election Date", receipt.get("electionDate"));
            row(resp, "Recorded At", receipt.get("createdAt"));
            row(resp, "Digital Signature", receipt.get("digitalSignature"));
            resp.getWriter().write("</div></body></html>");
        } catch (SQLException ex) {
            throw new ServletException("Unable to load receipt", ex);
        }
    }

    private void row(HttpServletResponse resp, String label, Object value) throws IOException {
        resp.getWriter().write("<p><strong>" + escape(label) + ":</strong> " + escape(String.valueOf(value)) + "</p>");
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
