(function () {
    const body = document.body;
    const contextPath = body.dataset.contextPath || "";
    const apiUrl = contextPath + "/api/dashboard-data";
    const wsProtocol = window.location.protocol === "https:" ? "wss://" : "ws://";
    const wsUrl = wsProtocol + window.location.host + contextPath + "/voteUpdates";
    const chartCanvas = document.getElementById("adminResultsChart");
    let resultsChart;
    let activityChart;
    let demographicsChart;

    function renderChart(results) {
        if (!results || !Array.isArray(results)) {
            console.warn("No results data available for chart");
            return;
        }
        const labels = results.map(r => r.candidateName);
        const votes = results.map(item => item.totalVotes);
        if (resultsChart) {
            resultsChart.data.labels = labels;
            resultsChart.data.datasets[0].data = votes;
            resultsChart.update();
            return;
        }
        resultsChart = new Chart(chartCanvas, {
            type: "bar",
            data: {
                labels,
                datasets: [{
                    label: "Votes",
                    data: votes,
                    backgroundColor: ["#6366f1", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6"]
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        display: false
                    }
                }
            }
        });
    }

    function renderUsers(users) {
        const tbody = document.getElementById("usersTableBody");
        const escapeHtml = value => String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
        const valueOrDash = value => value === null || value === undefined || value === "" ? "-" : escapeHtml(value);
        tbody.innerHTML = users.map(user => `
            <tr>
                <td>${valueOrDash(user.name)}<br><small style="color: var(--text-muted)">${valueOrDash(user.email)}</small></td>
                <td>${valueOrDash(user.voterIdNumber)}</td>
                <td>${valueOrDash(user.electionCenter)}</td>
                <td>${user.role && user.role.toLowerCase() !== "user" ? '<span style="color: var(--accent)">SYSTEM</span>' : (user.hasVoted ? '<span style="color: var(--secondary)">VOTED</span>' : '<span style="color: var(--text-muted)">PENDING</span>')}</td>
                <td>${valueOrDash(user.role)}</td>
            </tr>
        `).join("");
    }

    function renderStats(stats) {
        document.getElementById("totalUsers").textContent = stats.totalUsers;
        document.getElementById("totalVotes").textContent = stats.totalVotes;
        document.getElementById("totalCandidates").textContent = stats.totalCandidates;
        document.getElementById("electionStatus").textContent = stats.electionStatus;

        // Calculate and display Voter Turnout Progress
        const turnoutSpan = document.getElementById("turnoutPercentage");
        const turnoutFill = document.getElementById("turnoutFill");
        
        if (turnoutSpan && turnoutFill) {
            let turnout = 0;
            if (stats.totalUsers > 0) {
                turnout = Math.round((stats.totalVotes / stats.totalUsers) * 100);
            }
            turnoutSpan.textContent = turnout + "%";
            turnoutFill.style.width = turnout + "%";
        }
    }

    function renderActivityTrend(activityTrend) {
        const activityCtx = document.getElementById("votingActivityChart");
        if (!activityCtx) return;

        const points = activityTrend || window.VOTE_TRENDS_DATA || [];
        const labels = points.map(point => point.label || point.hour + ":00");
        const votes = points.map(point => point.totalVotes || point.count);

        if (activityChart) {
            activityChart.data.labels = labels;
            activityChart.data.datasets[0].data = votes;
            activityChart.update();
            return;
        }

        activityChart = new Chart(activityCtx, {
            type: "line",
            data: {
                labels,
                datasets: [{
                    label: "Votes Cast",
                    data: votes,
                    borderColor: "#6366f1",
                    backgroundColor: "rgba(99, 102, 241, 0.1)",
                    borderWidth: 3,
                    pointBackgroundColor: "#6366f1",
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, grid: { color: "rgba(255,255,255,0.05)" } },
                    x: { grid: { display: false } }
                }
            }
        });
    }

    function renderDemographics(demographics) {
        const demographicsCtx = document.getElementById("demographicsChart");
        if (!demographicsCtx) return;

        const dataObj = demographics || window.DEMOGRAPHICS_DATA || {};
        const ageData = dataObj.ageData || [];
        const labels = ageData.map(d => d.label);
        const values = ageData.map(d => d.value);

        if (demographicsChart) {
            demographicsChart.data.labels = labels;
            demographicsChart.data.datasets[0].data = values;
            demographicsChart.update();
            return;
        }

        demographicsChart = new Chart(demographicsCtx, {
            type: "doughnut",
            data: {
                labels,
                datasets: [{
                    data: values,
                    backgroundColor: ["#6366f1", "#10b981", "#f59e0b", "#ef4444"],
                    borderWidth: 0,
                    hoverOffset: 10
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "70%",
                plugins: {
                    legend: {
                        position: "bottom",
                        labels: { color: "#94a3b8", usePointStyle: true, padding: 20 }
                    }
                }
            }
        });
    }

    const dateDisplay = document.getElementById("currentDate");
    if (dateDisplay) {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        dateDisplay.textContent = new Date().toLocaleDateString('en-US', options);
    }

    function refresh() {
        fetch(apiUrl, { headers: { "X-Requested-With": "XMLHttpRequest" } })
            .then(response => response.json())
            .then(data => {
                renderStats(data.stats);
                // Keep the server-rendered user table because role-management forms
                // differ by the current admin role and must not be overwritten.
                renderChart(data.results || []);
                renderActivityTrend(data.activityTrend || []);
                renderDemographics(data.demographics);
            })
            .catch(() => {});
    }

    // Initialize sidebar navigation first for better UX
    const navLinks = document.querySelectorAll("#sidebarNav a");
    const sections = document.querySelectorAll(".tab-section");
    const mobileMenuBtn = document.getElementById("mobileMenuBtn");
    const sidebar = document.querySelector(".sidebar");
    
    if (mobileMenuBtn && sidebar) {
        mobileMenuBtn.addEventListener("click", () => {
            sidebar.classList.toggle("open");
        });

        sidebar.addEventListener("click", (e) => {
            const link = e.target.closest('a');
            if (link && window.innerWidth <= 1080) {
                sidebar.classList.remove("open");
            }
        });

        document.addEventListener("click", (e) => {
            if (window.innerWidth <= 1080 && 
                !sidebar.contains(e.target) && 
                !mobileMenuBtn.contains(e.target) && 
                sidebar.classList.contains("open")) {
                sidebar.classList.remove("open");
            }
        });
    }

    if (navLinks.length > 0 && sections.length > 0) {
        navLinks.forEach(link => {
            link.addEventListener("click", function(e) {
                const href = this.getAttribute("href");
                if (href && href.startsWith("#")) {
                    e.preventDefault();
                    const targetId = href.substring(1);
                    
                    navLinks.forEach(nav => nav.classList.remove("active"));
                    this.classList.add("active");
                    
                    sections.forEach(sec => sec.classList.remove("active"));
                    const targetSec = document.getElementById("section-" + targetId);
                    if (targetSec) {
                        targetSec.classList.add("active");
                        if ((targetId === 'overview' || targetId === 'candidates') && typeof refresh === 'function') {
                            refresh();
                        }
                    }
                }
            });
        });
    }

    // Chart and Dynamic Data Initialization (Wrapped in safety checks)
    if (chartCanvas && typeof Chart !== 'undefined') {
        try {
            renderChart([]);
            refresh();
        } catch (err) {
            console.error("Failed to initialize charts:", err);
        }
    } else if (chartCanvas) {
        console.warn("Chart.js library not loaded. Visualization will be disabled.");
        const chartWrapper = chartCanvas.parentElement;
        if (chartWrapper) {
            chartWrapper.innerHTML = '<div class="alert muted">Charts currently unavailable due to security policy or network issue.</div>';
        }
    }

    function setupTableSearch(inputId, tableId) {
        const searchInput = document.getElementById(inputId);
        const table = document.getElementById(tableId);
        if (!searchInput || !table) return;

        searchInput.addEventListener("keyup", function() {
            const filter = searchInput.value.toLowerCase();
            const rows = table.getElementsByTagName("tr");
            
            for (let i = 1; i < rows.length; i++) {
                let cells = rows[i].getElementsByTagName("td");
                let match = false;
                for (let j = 0; j < cells.length; j++) {
                    if (cells[j]) {
                        if (cells[j].innerText.toLowerCase().indexOf(filter) > -1) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) {
                    rows[i].style.display = "";
                } else {
                    rows[i].style.display = "none";
                }
            }
        });
    }

    function setupUserFilters() {
        const table = document.getElementById("usersTable");
        const searchInput = document.getElementById("userSearch");
        const roleFilter = document.getElementById("userRoleFilter");
        const statusFilter = document.getElementById("userStatusFilter");
        const centerFilter = document.getElementById("userCenterFilter");
        const resetButton = document.getElementById("resetUserFilters");
        const countLabel = document.getElementById("userFilterCount");
        if (!table || !searchInput || !roleFilter || !statusFilter || !centerFilter) return;

        const rows = Array.from(table.querySelectorAll("tbody tr"));
        function applyFilters() {
            const query = searchInput.value.trim().toLowerCase();
            const role = roleFilter.value;
            const status = statusFilter.value;
            const center = centerFilter.value;
            let visible = 0;

            rows.forEach(row => {
                const text = row.innerText.toLowerCase();
                const rowRole = row.dataset.role || "";
                const rowStatus = row.dataset.status || "";
                const rowCenter = row.dataset.center || "";
                const matchesQuery = !query || text.includes(query);
                const matchesRole = !role || rowRole === role;
                const matchesStatus = !status || rowStatus === status;
                const matchesCenter = !center || rowCenter === center;
                const show = matchesQuery && matchesRole && matchesStatus && matchesCenter;
                row.style.display = show ? "" : "none";
                if (show) visible++;
            });

            if (countLabel) {
                countLabel.textContent = visible + " of " + rows.length + " records shown";
            }
        }

        [searchInput, roleFilter, statusFilter, centerFilter].forEach(input => {
            input.addEventListener(input.tagName === "SELECT" ? "change" : "input", applyFilters);
        });
        if (resetButton) {
            resetButton.addEventListener("click", () => {
                searchInput.value = "";
                roleFilter.value = "";
                statusFilter.value = "";
                centerFilter.value = "";
                applyFilters();
            });
        }
        applyFilters();
    }

    function setupAuditFilters() {
        const table = document.getElementById("auditTable");
        const searchInput = document.getElementById("auditSearch");
        const actorInput = document.getElementById("auditActorFilter");
        const ipInput = document.getElementById("auditIpFilter");
        const resetButton = document.getElementById("resetAuditFilters");
        const countLabel = document.getElementById("auditFilterCount");
        if (!table || !searchInput || !actorInput || !ipInput) return;

        const rows = Array.from(table.querySelectorAll("tbody tr"));
        function applyFilters() {
            const query = searchInput.value.trim().toLowerCase();
            const actor = actorInput.value.trim().toLowerCase();
            const ip = ipInput.value.trim().toLowerCase();
            let visible = 0;

            rows.forEach(row => {
                const text = row.innerText.toLowerCase();
                const matchesQuery = !query || text.includes(query);
                const matchesActor = !actor || (row.dataset.actor || "").includes(actor);
                const matchesIp = !ip || (row.dataset.ip || "").includes(ip);
                const show = matchesQuery && matchesActor && matchesIp;
                row.style.display = show ? "" : "none";
                if (show) visible++;
            });

            if (countLabel) {
                countLabel.textContent = visible + " of " + rows.length + " log entries shown";
            }
        }

        [searchInput, actorInput, ipInput].forEach(input => input.addEventListener("input", applyFilters));
        if (resetButton) {
            resetButton.addEventListener("click", () => {
                searchInput.value = "";
                actorInput.value = "";
                ipInput.value = "";
                applyFilters();
            });
        }
        applyFilters();
    }
    
    setupTableSearch('candidateSearch', 'candidateTable');
    setupUserFilters();
    setupAuditFilters();

    window.setInterval(refresh, 5000);
})();

window.exportTableToCSV = function(tableId, filename) {
    var csv = [];
    var rows = document.querySelectorAll("#" + tableId + " tr");
    
    for (var i = 0; i < rows.length; i++) {
        var row = [], cols = rows[i].querySelectorAll("td, th");
        
        // Exclude the last column if it's the "Action" column in Candidates
        var colLen = cols.length;
        if (cols[colLen-1].innerText === "Action" || cols[colLen-1].querySelector("form")) {
            colLen--;
        }

        for (var j = 0; j < colLen; j++) {
            let text = cols[j].innerText.replace(/(\r\n|\n|\r)/gm, "").replace(/"/g, '""');
            row.push('"' + text + '"');
        }
        csv.push(row.join(","));
    }

    var csvFile = new Blob([csv.join("\n")], {type: "text/csv"});
    var downloadLink = document.createElement("a");
    downloadLink.download = filename;
    downloadLink.href = window.URL.createObjectURL(csvFile);
    downloadLink.style.display = "none";
    document.body.appendChild(downloadLink);
    downloadLink.click();
    document.body.removeChild(downloadLink);
};
