package com.voting.controller;

import com.voting.dao.CandidateDao;
import com.voting.model.Candidate;
import com.voting.service.VotingService;
import com.voting.util.InputValidator;
import com.voting.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/* Mapping and Multipart are managed via web.xml for maximum Tomcat 7 reliability */
public class CandidateServlet extends HttpServlet {
    private final CandidateDao candidateDao = new CandidateDao();
    private final VotingService votingService = new VotingService();

    private static class MultipartRequest {
        Map<String, String> params = new HashMap<>();
        Map<String, Part> files = new HashMap<>();

        MultipartRequest(HttpServletRequest req) throws ServletException, IOException {
            try {
                for (Part part : req.getParts()) {
                    String name = part.getName();
                    String fileName = getFileName(part);
                    if (fileName == null) {
                        params.put(name, readString(part.getInputStream()));
                    } else {
                        files.put(name, part);
                    }
                }
            } catch (Exception e) {
                // If it's not a multipart request, this will throw an exception
                // which we should catch if we want to support fallback
                throw new ServletException(e);
            }
        }

        private String getFileName(Part part) {
            String contentDisposition = part.getHeader("content-disposition");
            if (contentDisposition == null) return null;
            for (String cd : contentDisposition.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return fileName.isEmpty() ? null : fileName;
                }
            }
            return null;
        }

        private String readString(InputStream in) throws IOException {
            if (in == null) return "";
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8").trim();
        }
    }

    private String savePhoto(Part photoPart) throws IOException {
        if (photoPart == null || photoPart.getSize() == 0) return null;
        
        String originalName = null;
        String contentDisposition = photoPart.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String cd : contentDisposition.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    originalName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    break;
                }
            }
        }
        
        if (originalName == null || originalName.isEmpty()) return null;

        String contentType = photoPart.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) return null;

        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".jpg";
        String fileName = "candidate_" + UUID.randomUUID().toString().replace("-", "") + ext;

        String webappPath = getServletContext().getRealPath("/");
        if (webappPath == null) {
            webappPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "webapp";
        }

        String uploadDir = webappPath + File.separator + "assets" + File.separator + "img" + File.separator + "candidates";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        File dest = new File(dir, fileName);
        try (InputStream in = photoPart.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try {
            String projectPath = System.getProperty("user.dir");
            if (projectPath != null) {
                File srcDir = new File(projectPath + File.separator + "src" + File.separator + "main" + File.separator + "webapp" + File.separator + "assets" + File.separator + "img" + File.separator + "candidates");
                if (srcDir.exists() && !srcDir.getAbsolutePath().equals(dir.getAbsolutePath())) {
                    Files.copy(dest.toPath(), new File(srcDir, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception ignored) {}

        return "/assets/img/candidates/" + fileName;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard#candidates");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.canManageElections(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Candidate management requires Super Admin, Admin, or Election Officer access.");
            return;
        }

        try {
            String action = null;
            String candidateIdStr = null;
            String electionIdStr = null;
            String ballotOrderStr = null;
            String name = null;
            String manifesto = null;
            Part photoPart = null;

            String contentType = req.getContentType();
            boolean isMultipart = contentType != null && contentType.toLowerCase().contains("multipart/form-data");

            if (isMultipart) {
                try {
                    MultipartRequest multi = new MultipartRequest(req);
                    action = multi.params.get("action");
                    candidateIdStr = multi.params.get("candidateId");
                    electionIdStr = multi.params.get("electionId");
                    ballotOrderStr = multi.params.get("ballotOrder");
                    name = multi.params.get("name");
                    manifesto = multi.params.get("manifesto");
                    photoPart = multi.files.get("photo");
                } catch (Exception ex) {
                    // Fallback to regular parameters if getParts fails
                    action = req.getParameter("action");
                    candidateIdStr = req.getParameter("candidateId");
                    electionIdStr = req.getParameter("electionId");
                    ballotOrderStr = req.getParameter("ballotOrder");
                    name = req.getParameter("name");
                    manifesto = req.getParameter("manifesto");
                }
            } else {
                action = req.getParameter("action");
                candidateIdStr = req.getParameter("candidateId");
                electionIdStr = req.getParameter("electionId");
                ballotOrderStr = req.getParameter("ballotOrder");
                name = req.getParameter("name");
                manifesto = req.getParameter("manifesto");
            }

            if ("add".equalsIgnoreCase(action)) {
                if (name == null || name.isEmpty()) throw new IllegalArgumentException("Candidate name is required.");
                Candidate candidate = new Candidate();
                candidate.setName(InputValidator.required(name, "Candidate name", 120));
                candidate.setManifesto(InputValidator.required(manifesto, "Manifesto", 2000));
                candidate.setElectionId(Integer.parseInt(electionIdStr));
                candidate.setBallotOrder(parseBallotOrder(ballotOrderStr));
                candidate.setPhotoPath(savePhoto(photoPart));
                candidateDao.createCandidate(candidate);
                
                try {
                    new com.voting.dao.MongoLogDao().logSystemEvent("Candidate Added", "Added candidate: " + candidate.getName(), SessionUtil.getClientIp(req));
                    votingService.broadcastElectionUpdate("candidate_add", candidate.getElectionId(), candidate.getName());
                } catch (Throwable ignored) {}

            } else if ("edit".equalsIgnoreCase(action)) {
                if (candidateIdStr == null) throw new IllegalArgumentException("Candidate ID missing.");
                Candidate candidate = new Candidate();
                candidate.setId(Integer.parseInt(candidateIdStr));
                candidate.setName(InputValidator.required(name, "Candidate name", 120));
                candidate.setManifesto(InputValidator.required(manifesto, "Manifesto", 2000));
                candidate.setElectionId(Integer.parseInt(electionIdStr));
                candidate.setBallotOrder(parseBallotOrder(ballotOrderStr));
                
                if (photoPart != null && photoPart.getSize() > 0) {
                    candidate.setPhotoPath(savePhoto(photoPart));
                } else {
                    Candidate existing = candidateDao.findById(candidate.getId());
                    if (existing != null) candidate.setPhotoPath(existing.getPhotoPath());
                }
                candidateDao.updateCandidate(candidate);
                
                try {
                    new com.voting.dao.MongoLogDao().logSystemEvent("Candidate Edited", "Edited candidate: " + candidate.getName(), SessionUtil.getClientIp(req));
                    votingService.broadcastElectionUpdate("candidate_edit", candidate.getElectionId(), candidate.getName());
                } catch (Throwable ignored) {}

            } else if ("delete".equalsIgnoreCase(action)) {
                if (candidateIdStr != null) {
                    int candidateId = Integer.parseInt(candidateIdStr);
                    candidateDao.deleteCandidate(candidateId);
                    try {
                        int eId = electionIdStr != null ? Integer.parseInt(electionIdStr) : 0;
                        new com.voting.dao.MongoLogDao().logSystemEvent("Candidate Removed", "Removed candidate ID: " + candidateId, SessionUtil.getClientIp(req));
                        votingService.broadcastElectionUpdate("candidate_delete", eId, String.valueOf(candidateId));
                    } catch (Throwable ignored) {}
                }
            }
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard#candidates");
        } catch (Throwable ex) {
            ex.printStackTrace();
            req.getSession().setAttribute("errorMessage", "Management Error: " + ex.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard#candidates");
        }
    }

    private int parseBallotOrder(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
