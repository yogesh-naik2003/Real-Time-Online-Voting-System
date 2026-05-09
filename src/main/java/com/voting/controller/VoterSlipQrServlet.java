package com.voting.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.voting.model.User;
import com.voting.util.SessionUtil;
import com.voting.util.WebUrlUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@WebServlet("/user/voter-slip-qr")
public class VoterSlipQrServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "user")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        User user = SessionUtil.getLoggedInUser(req);
        String verifyUrl = WebUrlUtil.absoluteUrl(req, "/verify-voter-slip?userId=" + user.getId());

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(verifyUrl, BarcodeFormat.QR_CODE, 220, 220, hints);
            resp.setContentType("image/png");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            MatrixToImageWriter.writeToStream(matrix, "PNG", resp.getOutputStream());
        } catch (Exception ex) {
            throw new ServletException("Unable to generate voter slip QR", ex);
        }
    }

}
