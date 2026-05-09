package com.voting.controller;

import com.voting.dao.CandidateDao;
import com.voting.dao.ElectionDao;
import com.voting.dao.UserDao;
import com.voting.model.Election;
import com.voting.model.User;
import com.voting.service.VotingService;
import com.voting.util.JsonUtil;
import com.voting.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/dashboard-data")
public class ResultsApiServlet extends HttpServlet {
    private final VotingService votingService = new VotingService();
    private final ElectionDao electionDao = new ElectionDao();
    private final CandidateDao candidateDao = new CandidateDao();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = SessionUtil.getLoggedInUser(req);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            if (SessionUtil.isAdminRole(user.getRole())) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("stats", votingService.getDashboardStats());
                payload.put("results", votingService.getResultsForActiveElection());
                List<User> users = userDao.findAll();
                payload.put("users", sanitizeUsers(users));
                payload.put("activityTrend", votingService.getVoteActivityTrend());
                payload.put("demographics", buildDemographics(users));
                payload.put("elections", electionDao.findAll());
                resp.getWriter().write(JsonUtil.toJson(payload));
            } else {
                Map<String, Object> payload = votingService.getUserDashboardPayload(user);
                Election activeElection = (Election) payload.get("activeElection");
                payload.remove("user");
                payload.remove("results");
                if (activeElection != null) {
                    payload.put("candidates", candidateDao.findByElectionId(activeElection.getId()));
                }
                resp.getWriter().write(JsonUtil.toJson(payload));
            }
        } catch (SQLException ex) {
            throw new ServletException("Unable to load live data", ex);
        }
    }

    private List<Map<String, Object>> sanitizeUsers(List<User> users) {
        return users.stream().map(item -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", item.getId());
            view.put("name", item.getName());
            view.put("email", item.getEmail());
            view.put("mobileNumber", item.getMobileNumber());
            view.put("voterIdNumber", item.getVoterIdNumber());
            view.put("dateOfBirth", item.getDateOfBirth() == null ? null : item.getDateOfBirth().toString());
            view.put("age", item.getAge());
            view.put("electionCenter", item.getElectionCenter());
            view.put("city", item.getCity());
            view.put("state", item.getState());
            view.put("role", item.getRole());
            view.put("hasVoted", item.isHasVoted());
            return view;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> buildDemographics(List<User> users) {
        int age18To25 = 0;
        int age26To40 = 0;
        int age41To60 = 0;
        int age60Plus = 0;

        for (User item : users) {
            if (!"user".equalsIgnoreCase(item.getRole()) || item.getAge() == null) {
                continue;
            }
            int age = item.getAge();
            if (age <= 25) {
                age18To25++;
            } else if (age <= 40) {
                age26To40++;
            } else if (age <= 60) {
                age41To60++;
            } else {
                age60Plus++;
            }
        }

        List<Map<String, Object>> ageData = new ArrayList<>();
        ageData.add(createDataPoint("18-25", age18To25));
        ageData.add(createDataPoint("26-40", age26To40));
        ageData.add(createDataPoint("41-60", age41To60));
        ageData.add(createDataPoint("60+", age60Plus));

        Map<String, Object> demographics = new LinkedHashMap<>();
        demographics.put("ageData", ageData);
        return demographics;
    }

    private Map<String, Object> createDataPoint(String label, int value) {
        Map<String, Object> point = new HashMap<>();
        point.put("label", label);
        point.put("value", value);
        return point;
    }
}
