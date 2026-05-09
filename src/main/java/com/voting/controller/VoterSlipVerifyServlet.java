package com.voting.controller;

import com.voting.dao.UserDao;
import com.voting.dao.VoteDao;
import com.voting.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/verify-voter-slip")
public class VoterSlipVerifyServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();
    private final VoteDao voteDao = new VoteDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userIdValue = req.getParameter("userId");
        if (userIdValue == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing voter reference.");
            return;
        }

        try {
            User user = userDao.findById(Integer.parseInt(userIdValue));
            if (user == null || !"user".equalsIgnoreCase(user.getRole())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Voter not found.");
                return;
            }

            boolean voted = voteDao.hasUserVoted(user.getId());
            renderVerification(req, resp, user, voted);
        } catch (NumberFormatException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid voter reference.");
        } catch (SQLException ex) {
            throw new ServletException("Unable to verify voter slip", ex);
        }
    }

    private void renderVerification(HttpServletRequest req, HttpServletResponse resp, User user, boolean voted) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        String photoUrl = "";
        if (user.getProfilePhotoPath() != null && !user.getProfilePhotoPath().trim().isEmpty()) {
            photoUrl = req.getContextPath() + user.getProfilePhotoPath();
        }

        String status = voted ? "VOTED" : "NOT VOTED";
        String statusColor = voted ? "#059669" : "#dc2626";

        resp.getWriter().write("<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>Voter Slip Verification</title>"
                + "<style>body{font-family:Arial,sans-serif;background:#f3f4f6;margin:0;padding:24px;color:#111827}"
                + ".card{max-width:460px;margin:auto;background:#fff;border:1px solid #d1d5db;border-radius:14px;overflow:hidden;box-shadow:0 18px 45px rgba(15,23,42,.12)}"
                + ".head{background:#1e293b;color:#fff;padding:18px 22px;font-weight:800;letter-spacing:.04em}.body{padding:24px}"
                + ".photo{width:120px;height:145px;border:2px solid #e5e7eb;border-radius:10px;object-fit:cover;background:#f9fafb;display:block;margin:0 auto 20px}"
                + ".initial{width:120px;height:145px;border:2px solid #e5e7eb;border-radius:10px;background:#f9fafb;display:flex;align-items:center;justify-content:center;margin:0 auto 20px;font-size:44px;font-weight:800;color:#94a3b8}"
                + ".row{margin-bottom:12px}.label{font-size:11px;text-transform:uppercase;color:#64748b;font-weight:700}.value{font-size:16px;font-weight:700;margin-top:2px}"
                + ".status{text-align:center;margin-top:22px;font-size:26px;font-weight:900;color:" + statusColor + "}</style></head><body>"
                + "<div class=\"card\"><div class=\"head\">SEC Voter Slip Verification</div><div class=\"body\">"
                + (photoUrl.isEmpty()
                    ? "<div class=\"initial\">" + initial(user.getName()) + "</div>"
                    : "<img class=\"photo\" src=\"" + escape(photoUrl) + "\" alt=\"Voter photo\">")
                + row("Voter ID", user.getVoterIdNumber())
                + row("Name", user.getName())
                + row("Mobile No", user.getMobileNumber())
                + row("Email", user.getEmail())
                + row("City", user.getCity())
                + "<div class=\"status\">" + status + "</div>"
                + "</div></div></body></html>");
    }

    private String row(String label, String value) {
        return "<div class=\"row\"><div class=\"label\">" + escape(label) + "</div><div class=\"value\">" + escape(value) + "</div></div>";
    }

    private String initial(String value) {
        return escape(value == null || value.trim().isEmpty() ? "V" : value.trim().substring(0, 1).toUpperCase());
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
