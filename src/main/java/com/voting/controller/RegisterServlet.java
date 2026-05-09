package com.voting.controller;

import com.voting.dao.MongoLogDao;
import com.voting.dao.UserDao;
import com.voting.model.User;
import com.voting.util.InputValidator;
import com.voting.util.PasswordUtil;
import com.voting.util.SecurityAuditUtil;
import com.voting.util.SystemSettings;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.security.SecureRandom;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();
    private final MongoLogDao mongoLogDao = new MongoLogDao();
    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        forwardWithCaptcha(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String mobileNumber = req.getParameter("mobileNumber");
        String voterIdNumber = req.getParameter("voterIdNumber");
        String dateOfBirthValue = req.getParameter("dateOfBirth");
        String ageValue = req.getParameter("age");
        String electionCenter = req.getParameter("electionCenter");
        String city = req.getParameter("city");
        String state = req.getParameter("state");
        String password = req.getParameter("password");
        String captcha = req.getParameter("captcha");
        preserveRegistrationInput(req, name, email, mobileNumber, voterIdNumber, dateOfBirthValue, ageValue,
                electionCenter, city, state);

        if (!SystemSettings.allowRegistrations) {
            req.setAttribute("error", "New registrations are currently disabled by the State Election Commission.");
            forwardWithCaptcha(req, resp);
            return;
        }

        try {
            name = InputValidator.required(name, "Full name", 100);
            email = InputValidator.email(email);
            mobileNumber = InputValidator.mobile(mobileNumber);
            voterIdNumber = InputValidator.voterId(voterIdNumber);
            electionCenter = InputValidator.required(electionCenter, "Election center", 150);
            city = InputValidator.required(city, "City", 100);
            state = InputValidator.required(state, "State", 100);
            InputValidator.strongPassword(password);
        } catch (IllegalArgumentException ex) {
            req.setAttribute("error", ex.getMessage());
            forwardWithCaptcha(req, resp);
            return;
        }

        if (!isCaptchaValid(req, captcha)) {
            req.setAttribute("error", "Captcha answer is incorrect. Please try the new challenge.");
            forwardWithCaptcha(req, resp);
            return;
        }

        LocalDate dateOfBirth;
        int age;
        try {
            dateOfBirth = LocalDate.parse(dateOfBirthValue.trim());
            age = Integer.parseInt(ageValue.trim());
        } catch (DateTimeParseException | NumberFormatException ex) {
            req.setAttribute("error", "Please provide a valid date of birth and age.");
            forwardWithCaptcha(req, resp);
            return;
        }

        int calculatedAge = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (dateOfBirth.isAfter(LocalDate.now()) || age < 18 || calculatedAge < 18 || Math.abs(calculatedAge - age) > 1) {
            req.setAttribute("error", "Voter must be at least 18 years old and age must match the date of birth.");
            forwardWithCaptcha(req, resp);
            return;
        }

        try {
            String normalizedEmail = email;
            String normalizedVoterId = voterIdNumber;
            if (userDao.findByEmail(normalizedEmail) != null) {
                req.setAttribute("error", "This email is already registered.");
                forwardWithCaptcha(req, resp);
                return;
            }
            if (mongoLogDao.isUserPending(normalizedEmail)) {
                req.setAttribute("error", "This email already has a registration pending approval.");
                forwardWithCaptcha(req, resp);
                return;
            }
            if (userDao.findByVoterIdNumber(normalizedVoterId) != null || mongoLogDao.isVoterIdPending(normalizedVoterId)) {
                req.setAttribute("error", "This voter ID number is already registered or pending approval.");
                forwardWithCaptcha(req, resp);
                return;
            }

            User user = new User();
            user.setName(name);
            user.setEmail(normalizedEmail);
            user.setMobileNumber(mobileNumber);
            user.setVoterIdNumber(normalizedVoterId);
            user.setDateOfBirth(java.sql.Date.valueOf(dateOfBirth));
            user.setAge(age);
            user.setElectionCenter(electionCenter);
            user.setCity(city);
            user.setState(state);
            user.setPassword(PasswordUtil.hashPassword(password));
            user.setRole("user");
            user.setHasVoted(false);

            if (SystemSettings.requireIdVerification) {
                mongoLogDao.savePendingUser(user);
                mongoLogDao.logSecurityEvent("registration_pending", normalizedEmail, getClientIp(req), "Registration submitted for admin approval.");
                resp.sendRedirect(req.getContextPath() + "/login?pending=true");
            } else {
                userDao.createUser(user);
                mongoLogDao.logSecurityEvent("registration_created", normalizedEmail, getClientIp(req), "Registration created.");
                SecurityAuditUtil.log("USER_REGISTERED", email, getClientIp(req), "New voter registered successfully");
                resp.sendRedirect(req.getContextPath() + "/login?registered=true");
            }
        } catch (SQLException ex) {
            throw new ServletException("Unable to register user", ex);
        }
    }

    private boolean isCaptchaValid(HttpServletRequest req, String captcha) {
        Object expected = req.getSession().getAttribute("registrationCaptchaAnswer");
        if (expected == null || captcha == null) {
            return false;
        }
        return String.valueOf(expected).equals(captcha.trim());
    }

    private void forwardWithCaptcha(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int left = random.nextInt(8) + 2;
        int right = random.nextInt(8) + 2;
        req.getSession().setAttribute("registrationCaptchaAnswer", left + right);
        req.setAttribute("captchaQuestion", left + " + " + right);
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
    }

    private void preserveRegistrationInput(HttpServletRequest req, String name, String email, String mobileNumber,
                                           String voterIdNumber, String dateOfBirth, String age,
                                           String electionCenter, String city, String state) {
        req.setAttribute("nameValue", safeValue(name));
        req.setAttribute("emailValue", safeValue(email));
        req.setAttribute("mobileNumberValue", safeValue(mobileNumber));
        req.setAttribute("voterIdNumberValue", safeValue(voterIdNumber));
        req.setAttribute("dateOfBirthValue", safeValue(dateOfBirth));
        req.setAttribute("ageValue", safeValue(age));
        req.setAttribute("electionCenterValue", safeValue(electionCenter));
        req.setAttribute("cityValue", safeValue(city));
        req.setAttribute("stateValue", safeValue(state));
    }

    private String getClientIp(HttpServletRequest req) {
        return com.voting.util.ClientIpUtil.fromRequest(req);
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }
}
