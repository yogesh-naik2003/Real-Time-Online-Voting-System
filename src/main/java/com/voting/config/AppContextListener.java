package com.voting.config;

import com.voting.dao.MongoLogDao;
import com.voting.dao.UserDao;
import com.voting.model.User;
import com.voting.util.PasswordUtil;
import com.voting.util.SchemaMigrator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    private java.util.concurrent.ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            SchemaMigrator.run();
            UserDao userDao = new UserDao();
            if (userDao.findByEmail(AppConfig.get("app.defaultAdminEmail")) == null) {
                User admin = new User();
                admin.setName("System Admin");
                admin.setEmail(AppConfig.get("app.defaultAdminEmail"));
                admin.setPassword(PasswordUtil.hashPassword(AppConfig.get("app.defaultAdminPassword")));
                admin.setRole("admin");
                admin.setHasVoted(false);
                userDao.createUser(admin);
            }
            seedRoleAccount(userDao, "Super Admin", "superadmin@voting.local", "super_admin", "ADMIN-SUPER-001");
            seedRoleAccount(userDao, "Election Officer", "officer@voting.local", "election_officer", "ADMIN-OFFICER-001");
            seedRoleAccount(userDao, "Auditor", "auditor@voting.local", "auditor", "ADMIN-AUDITOR-001");
            new MongoLogDao().logSystemEvent("application_started", "Application bootstrapped successfully");
            
        } catch (Exception ex) {
            sce.getServletContext().log("Startup initialization failed", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void seedRoleAccount(UserDao userDao, String name, String email, String role, String voterId) throws Exception {
        User existing = userDao.findByEmail(email);
        String passwordHash = PasswordUtil.hashPassword("Admin@123");
        if (existing == null) {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setMobileNumber("9000000000");
            user.setVoterIdNumber(voterId);
            user.setDateOfBirth(java.sql.Date.valueOf("1990-01-01"));
            user.setAge(36);
            user.setElectionCenter("SEC HQ");
            user.setCity("Bengaluru");
            user.setState("Karnataka");
            user.setPassword(passwordHash);
            user.setRole(role);
            user.setHasVoted(false);
            userDao.createUser(user);
        } else {
            userDao.updatePassword(existing.getId(), passwordHash);
            userDao.updateRole(existing.getId(), role);
            userDao.resetFailedAttempts(existing.getId());
        }
    }

}
