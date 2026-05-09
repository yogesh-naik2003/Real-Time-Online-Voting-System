(function () {
    const body = document.body;
    const contextPath = body.dataset.contextPath || "";
    const apiUrl = contextPath + "/api/dashboard-data";
    const wsProtocol = window.location.protocol === "https:" ? "wss://" : "ws://";
    const wsUrl = wsProtocol + window.location.host + contextPath + "/voteUpdates";
    let countdownTimer;

    const navLinks = document.querySelectorAll("#sidebarNav a");
    const sections = document.querySelectorAll(".tab-section");

    if (navLinks.length > 0 && sections.length > 0) {
        navLinks.forEach(link => {
            link.addEventListener("click", function (event) {
                event.preventDefault();
                const targetId = this.getAttribute("href").substring(1);

                navLinks.forEach(nav => nav.classList.remove("active"));
                this.classList.add("active");

                sections.forEach(section => section.classList.remove("active"));
                const targetSection = document.getElementById("section-" + targetId);
                if (targetSection) {
                    targetSection.classList.add("active");
                }
            });
        });
    }



    function renderCandidates(candidates, hasVoted, activeElection) {
        const list = document.getElementById("candidateList");
        const formButton = document.querySelector("#voteForm button[type='submit']");
        if (!list || !formButton) return;
        if (!candidates || candidates.length === 0) {
            list.innerHTML = "<p class='muted'>Candidates will appear here once an election is active.</p>";
            formButton.disabled = true;
            formButton.textContent = activeElection ? "No candidates available" : "Waiting for active election";
            return;
        }

        let compareBtnHtml = candidates.length >= 2 ? '<button type="button" class="btn btn-sm btn-outline" id="compareCandidatesBtn" style="margin-bottom: 12px; width: 100%;">Compare Selected Candidates</button>' : '';

        list.innerHTML = compareBtnHtml + candidates.map(candidate => {
            const initial = (candidate.name || 'C')[0].toUpperCase();
            const photoHtml = candidate.photoPath
                ? `<img src="${escapeHtml(candidate.photoPath)}" alt="${escapeHtml(candidate.name)}" style="width:56px;height:56px;border-radius:50%;object-fit:cover;border:2px solid var(--line);flex-shrink:0;">`
                : `<div style="width:56px;height:56px;border-radius:50%;background:linear-gradient(135deg,rgba(99,102,241,0.3),rgba(16,185,129,0.2));display:flex;align-items:center;justify-content:center;font-size:1.5rem;font-weight:bold;color:#fff;flex-shrink:0;">${initial}</div>`;
            return `
                <div style="display: flex; align-items: center; gap: 12px;">
                    <input type="checkbox" class="compare-check" title="Select to compare" style="width: 18px; height: 18px; accent-color: var(--primary);">
                    <label class="candidate-option" style="flex: 1;" data-name="${escapeHtml(candidate.name)}" data-manifesto="${escapeHtml(candidate.manifesto)}" data-election-id="${candidate.electionId}" data-photo="${escapeHtml(candidate.photoPath || '')}">
                        <input type="radio" name="candidateId" value="${candidate.id}" ${hasVoted ? 'disabled' : ''} required>
                        ${photoHtml}
                        <span>
                            <strong>${escapeHtml(candidate.name)}</strong>
                            <small>${escapeHtml(candidate.manifesto)}</small>
                        </span>
                        <button type="button" class="btn btn-sm btn-outline candidate-details-btn">Details</button>
                    </label>
                </div>
            `;
        }).join("");
        formButton.disabled = !!hasVoted || !activeElection;
        formButton.textContent = hasVoted ? "Vote Already Recorded" : "Submit Vote";
        applyCandidateSearch();
        bindCandidateDetails();
    }

    function renderTurnout(turnout) {
        const percentage = turnout && turnout.percentage ? turnout.percentage : 0;
        const votesCast = turnout && turnout.votesCast ? turnout.votesCast : 0;
        const registeredVoters = turnout && turnout.registeredVoters ? turnout.registeredVoters : 0;
        const percentageText = document.getElementById("userTurnoutPercentage");
        const fill = document.getElementById("userTurnoutFill");
        const votes = document.getElementById("userTurnoutVotes");
        const registered = document.getElementById("userTurnoutRegistered");
        if (percentageText) percentageText.textContent = percentage + "%";
        if (fill) fill.style.width = percentage + "%";
        if (votes) votes.textContent = votesCast;
        if (registered) registered.textContent = registeredVoters;
    }

    function renderNotifications(notifications) {
        const list = document.getElementById("notifList");
        const badge = document.getElementById("notifBadge");
        const items = Array.isArray(notifications) ? notifications : [];
        if (badge) {
            badge.classList.toggle("hidden", items.length === 0);
        }
        if (!list) return;
        if (items.length === 0) {
            list.innerHTML = '<p class="muted notification-empty">No new alerts.</p>';
            return;
        }
        list.innerHTML = items.map(item => (
            `<div class="notification-item">${escapeHtml(item)}</div>`
        )).join("");
    }

    function startCountdown(endsAt) {
        if (countdownTimer) {
            window.clearInterval(countdownTimer);
        }
        
        const output = document.getElementById("electionCountdown");
        if (!output || !endsAt) {
            if (output) output.textContent = "--:--:--";
            return;
        }

        // Handle ISO date format more robustly for older browsers/cross-platform
        const targetDate = new Date(endsAt.replace(/-/g, "/").replace("T", " "));
        const target = targetDate.getTime();
        
        if (isNaN(target)) {
            console.warn("Invalid countdown date:", endsAt);
            output.textContent = "TBD";
            return;
        }

        const update = () => {
            const now = Date.now();
            const remaining = target - now;
            const statusPill = document.getElementById("electionStatusPill");

            if (remaining <= 0) {
                output.textContent = "Polls Closed";
                output.style.color = "var(--danger)";
                if (statusPill) {
                    statusPill.textContent = "INACTIVE";
                    statusPill.classList.remove("active");
                    statusPill.classList.add("inactive");
                }
                if (countdownTimer) window.clearInterval(countdownTimer);
                return;
            }

            if (statusPill) {
                statusPill.textContent = "ACTIVE";
                statusPill.classList.remove("inactive");
                statusPill.classList.add("active");
            }

            const h = Math.floor(remaining / 3600000);
            const m = Math.floor((remaining % 3600000) / 60000);
            const s = Math.floor((remaining % 60000) / 1000);

            output.textContent = `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
            
            // Visual warning if less than 1 hour left
            if (remaining < 3600000) {
                output.style.color = "#f59e0b"; // Warning orange
            } else {
                output.style.color = "var(--primary)";
            }
        };

        update();
        countdownTimer = window.setInterval(update, 1000);
    }

    function applyCandidateSearch() {
        const search = document.getElementById("candidateSearch");
        const options = document.querySelectorAll(".candidate-option");
        if (!search) return;
        const query = search.value.trim().toLowerCase();
        options.forEach(option => {
            const text = (option.dataset.name + " " + option.dataset.manifesto).toLowerCase();
            option.style.display = text.includes(query) ? "flex" : "none";
        });
    }

    function bindCandidateDetails() {
        document.querySelectorAll(".candidate-details-btn").forEach(button => {
            button.addEventListener("click", event => {
                event.preventDefault();
                event.stopPropagation();
                const option = button.closest(".candidate-option");
                openCandidateModal(
                    option.dataset.name,
                    option.dataset.manifesto,
                    option.dataset.electionId,
                    option.dataset.photo
                );
            });
        });
        
        // Comparison logic
        const compareBtn = document.getElementById("compareCandidatesBtn");
        if (compareBtn) {
            compareBtn.addEventListener("click", () => {
                const selected = Array.from(document.querySelectorAll(".compare-check:checked")).map(cb => {
                    const opt = cb.closest(".candidate-option");
                    return {
                        name: opt.dataset.name,
                        manifesto: opt.dataset.manifesto,
                        photo: opt.dataset.photo
                    };
                });
                if (selected.length < 2) {
                    alert("Please select at least 2 candidates to compare.");
                    return;
                }
                openComparisonModal(selected);
            });
        }
    }

    function openComparisonModal(candidates) {
        const modal = document.createElement("div");
        modal.className = "modal-backdrop active";
        modal.innerHTML = `
            <div class="modal-panel" style="max-width: 900px;">
                <div class="panel-header-row" style="margin-bottom: 24px;">
                    <h2>Candidate Comparison</h2>
                    <button class="btn btn-sm btn-outline close-comp">Close</button>
                </div>
                <div style="display: grid; grid-template-columns: repeat(${candidates.length}, 1fr); gap: 20px;">
                    ${candidates.map(c => `
                        <div style="text-align: center; background: rgba(255,255,255,0.03); padding: 20px; border-radius: 16px; border: 1px solid var(--line);">
                            ${c.photo ? `<img src="${c.photo}" style="width: 80px; height: 80px; border-radius: 50%; object-fit: cover; border: 2px solid var(--primary); margin-bottom: 16px;">` : `<div style="width: 80px; height: 80px; border-radius: 50%; background: var(--primary); display: flex; align-items: center; justify-content: center; font-size: 2rem; font-weight: 800; color: #fff; margin: 0 auto 16px;">${c.name[0].toUpperCase()}</div>`}
                            <h3 style="margin-bottom: 12px;">${escapeHtml(c.name)}</h3>
                            <p class="muted" style="font-size: 0.9rem; text-align: left;">${escapeHtml(c.manifesto)}</p>
                        </div>
                    `).join("")}
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        modal.querySelector(".close-comp").onclick = () => modal.remove();
    }

    function openCandidateModal(name, manifesto, electionId, photoUrl) {
        const modal = document.getElementById("candidateModal");
        if (!modal) return;
        document.getElementById("candidateModalName").textContent = name || "Candidate";
        document.getElementById("candidateModalManifesto").textContent = manifesto || "No manifesto provided.";
        document.getElementById("candidateModalElection").textContent = electionId || "-";

        // Photo or initial
        const photoEl = document.getElementById("candidateModalPhoto");
        const initialEl = document.getElementById("candidateModalInitial");
        if (photoUrl && photoEl && initialEl) {
            photoEl.src = photoUrl;
            photoEl.style.display = "block";
            initialEl.style.display = "none";
        } else if (initialEl) {
            initialEl.textContent = (name || 'C')[0].toUpperCase();
            initialEl.style.display = "flex";
            if (photoEl) photoEl.style.display = "none";
        }

        modal.classList.add("active");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeCandidateModal() {
        const modal = document.getElementById("candidateModal");
        if (!modal) return;
        modal.classList.remove("active");
        modal.setAttribute("aria-hidden", "true");
    }

    function downloadVoterSlip() {
        const data = document.getElementById("voterSlipData");
        if (!data) return;
        const slipWindow = window.open("", "_blank", "width=800,height=600");
        
        const profileImg = document.querySelector(".voter-card-visual .voter-profile-photo");
        const photoUrl = data.dataset.photo || (profileImg && profileImg.src ? profileImg.src : "");
        const qrUrl = data.dataset.qrUrl || "";
        const verifyUrl = data.dataset.verifyUrl || "";
        const voteStatus = data.dataset.voteStatus || "NOT VOTED";
        const voted = voteStatus.toUpperCase() === "VOTED";
        
        const initial = (data.dataset.name || "V")[0].toUpperCase();
        
        slipWindow.document.write(`
            <html>
            <head>
                <title>SEC Voter Slip - ${data.dataset.name}</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px; background: #f3f4f6; display: flex; justify-content: center; }
                    .card { 
                        width: 500px; background: #fff; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); 
                        border: 1px solid #e5e7eb; overflow: hidden; position: relative;
                    }
                    .header { background: #1e293b; color: #fff; padding: 20px; display: flex; align-items: center; gap: 12px; }
                    .header img { width: 32px; height: 32px; filter: brightness(100); }
                    .header h1 { font-size: 16px; margin: 0; text-transform: uppercase; letter-spacing: 1px; }
                    
                    .content { padding: 30px; display: flex; gap: 24px; }
                    .photo-box { 
                        width: 120px; height: 150px; background: #f9fafb; border: 2px solid #e5e7eb; 
                        border-radius: 8px; overflow: hidden; display: flex; align-items: center; justify-content: center;
                    }
                    .photo-box img { width: 100%; height: 100%; object-fit: cover; }
                    .photo-box .initial { font-size: 48px; font-weight: 800; color: #9ca3af; }
                    
                    .info { flex: 1; }
                    .info-row { margin-bottom: 12px; }
                    .label { font-size: 10px; text-transform: uppercase; color: #6b7280; font-weight: 700; margin-bottom: 2px; }
                    .value { font-size: 14px; color: #111827; font-weight: 600; }
                    
                    .status-line { margin: 20px 30px 22px; text-align: center; font-size: 24px; font-weight: 900; color: ${voted ? "#059669" : "#dc2626"}; }
                    .footer { background: #f9fafb; padding: 18px 30px; border-top: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; gap: 18px; }
                    .qr-code { width: 110px; height: 110px; border: 1px solid #e5e7eb; padding: 6px; border-radius: 8px; background: #fff; object-fit: contain; }
                    .verify-url { font-size: 10px; color: #64748b; word-break: break-all; margin-top: 6px; max-width: 280px; }
                    
                    @media print {
                        body { background: #fff; padding: 0; }
                        .card { box-shadow: none; border: 2px solid #1e293b; }
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <img src="${contextPath}/assets/img/sec-logo.svg" alt="SEC">
                        <h1>State Election Commission</h1>
                    </div>
                    <div class="content">
                        <div class="photo-box">
                            ${photoUrl ? `<img src="${photoUrl}">` : `<div class="initial">${initial}</div>`}
                        </div>
                        <div class="info">
                            <div class="info-row">
                                <div class="label">Full Name</div>
                                <div class="value">${escapeHtml(data.dataset.name)}</div>
                            </div>
                            <div class="info-row">
                                <div class="label">Voter ID Number</div>
                                <div class="value" style="font-family: monospace; letter-spacing: 1px;">${escapeHtml(data.dataset.voterId)}</div>
                            </div>
                            <div class="info-row">
                                <div class="label">Mobile No</div>
                                <div class="value">${escapeHtml(data.dataset.mobile)}</div>
                            </div>
                            <div class="info-row">
                                <div class="label">Email</div>
                                <div class="value">${escapeHtml(data.dataset.email)}</div>
                            </div>
                            <div class="info-row">
                                <div class="label">Current Election</div>
                                <div class="value">${escapeHtml(data.dataset.election)}</div>
                            </div>
                            <div class="info-row">
                                <div class="label">Voting Center</div>
                                <div class="value">${escapeHtml(data.dataset.center)}</div>
                            </div>
                            <div class="info-row">
                                <div class="label">Location</div>
                                <div class="value">${escapeHtml(data.dataset.city)}, ${escapeHtml(data.dataset.state)}</div>
                            </div>
                        </div>
                    </div>
                    <div class="status-line">${escapeHtml(voteStatus)}</div>
                    <div class="footer">
                        <div>
                            <div class="label" style="margin-bottom: 4px;">Digital Verification</div>
                            <div class="verify-url">${escapeHtml(verifyUrl)}</div>
                            <div class="value" style="font-size: 10px; color: #10b981;">● Verified Secure Asset</div>
                        </div>
                        ${qrUrl ? `<img class="qr-code" src="${qrUrl}" alt="Verification QR code">` : ""}
                        <div class="qr-code" style="display:none;">
                            ${Array(16).fill(0).map(() => `<div class="qr-block" style="opacity: ${Math.random() > 0.5 ? 1 : 0.1}"></div>`).join('')}
                        </div>
                    </div>
                </div>
                <script>
                    function triggerPrint() {
                        window.print();
                        setTimeout(() => window.close(), 500);
                    }
                    window.onload = () => {
                        const images = Array.from(document.images);
                        const pending = images.filter(img => !img.complete);
                        if (pending.length > 0) {
                            let remaining = pending.length;
                            const done = () => {
                                remaining -= 1;
                                if (remaining <= 0) setTimeout(triggerPrint, 300);
                            };
                            pending.forEach(img => {
                                img.onload = done;
                                img.onerror = done;
                            });
                        } else {
                            setTimeout(triggerPrint, 500);
                        }
                    };
                <\/script>
            </body>
            </html>
        `);
        slipWindow.document.close();
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function renderElection(activeElection, endsAt, resultsAnnounced, latestElection) {
        const electionCard = document.getElementById("activeElectionCard");
        if (!electionCard) return;
        
        if (!activeElection) {
            if (resultsAnnounced && latestElection) {
                electionCard.innerHTML = `
                    <h2>${escapeHtml(latestElection.title)}</h2>
                    <p>${escapeHtml(latestElection.description)}</p>
                    <span class="status-pill active">COMPLETED</span>
                    <div class="countdown-card">
                        <span class="muted">Status</span>
                        <strong style="color: var(--success)">Results Published</strong>
                    </div>
                `;
            } else {
                electionCard.innerHTML = "<p>No active election is available right now. This card updates live when the admin starts one.</p>";
            }
            startCountdown(null);
            return;
        }
        
        electionCard.innerHTML = `
            <h2>${escapeHtml(activeElection.title)}</h2>
            <p>${escapeHtml(activeElection.description)}</p>
            <span class="status-pill active" id="electionStatusPill">${escapeHtml(activeElection.status)} ${activeElection.resultsAnnounced ? "- Results Live" : ""}</span>
            <div class="countdown-card">
                <span class="muted">Voting closes in</span>
                <strong id="electionCountdown" data-ends-at="${escapeHtml(endsAt)}">--:--:--</strong>
            </div>
        `;
        startCountdown(endsAt);
    }

    function renderResults(announced, results, election) {
        let panel = document.querySelector(".results-panel");
        if (!announced || (!results && !election)) {
            if (panel) panel.style.display = "none";
            return;
        }

        if (!panel) {
            // Create panel if it doesn't exist (though it should be in JSP)
            return;
        }
        
        panel.style.display = "block";
        const title = panel.querySelector("h2");
        const list = panel.querySelector(".results-list");
        if (title) title.textContent = election ? election.title : "Election Results";
        
        if (list && results) {
            const totalVotes = results.reduce((sum, r) => sum + r.totalVotes, 0);
            list.innerHTML = results.map((stat, i) => {
                const pct = totalVotes === 0 ? 0 : (stat.totalVotes * 100 / totalVotes);
                return `
                    <div class="result-item">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <strong>${escapeHtml(stat.candidateName)}</strong>
                            <span>${stat.totalVotes} votes (${pct.toFixed(1)}%)</span>
                        </div>
                        <div class="progress-bar-bg" style="height: 8px;">
                            <div class="progress-bar-fill" style="width: ${pct}%; background: ${i === 0 ? "var(--success)" : "var(--primary)"}"></div>
                        </div>
                        ${i === 0 && stat.totalVotes > 0 ? '<div style="margin-top: 4px;"><span class="status-pill active" style="font-size: 0.7rem; padding: 2px 8px;">Winner</span></div>' : ""}
                    </div>
                `;
            }).join("");
        }
    }

    function refresh() {
        fetch(apiUrl, { headers: { "X-Requested-With": "XMLHttpRequest" } })
            .then(response => response.json())
            .then(data => {
                renderElection(data.activeElection, data.activeElectionEndsAt, data.resultsAnnounced, data.latestElection);
                renderCandidates(data.candidates || [], data.hasVoted, data.activeElection);
                renderTurnout(data.turnout);
                renderNotifications(data.notifications || []);
                renderResults(data.resultsAnnounced, data.finalResults, data.latestElection || data.activeElection);
            })
            .catch(() => {});
    }

    const search = document.getElementById("candidateSearch");
    if (search) {
        search.addEventListener("input", applyCandidateSearch);
    }

    const closeModal = document.getElementById("candidateModalClose");
    if (closeModal) {
        closeModal.addEventListener("click", closeCandidateModal);
    }

    const candidateModal = document.getElementById("candidateModal");
    if (candidateModal) {
        candidateModal.addEventListener("click", event => {
            if (event.target === candidateModal) {
                closeCandidateModal();
            }
        });
    }

    const slipButton = document.getElementById("downloadSlipBtn");
    if (slipButton) {
        slipButton.addEventListener("click", downloadVoterSlip);
    }

    const dobInput = document.querySelector("input[name='dateOfBirth']");
    const ageInput = document.querySelector("input[name='age']");
    if (dobInput && ageInput) {
        const updateAgeFromDob = () => {
            if (!dobInput.value) return;
            const dob = new Date(dobInput.value + "T00:00:00");
            if (Number.isNaN(dob.getTime())) return;
            const today = new Date();
            let age = today.getFullYear() - dob.getFullYear();
            const monthDiff = today.getMonth() - dob.getMonth();
            if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
                age -= 1;
            }
            ageInput.value = age > 0 ? age : "";
        };
        dobInput.addEventListener("change", updateAgeFromDob);
        updateAgeFromDob();
    }

    const requestOtpBtn = document.getElementById("requestOtpBtn");
    const otpInput = document.getElementById("otpInput");
    if (requestOtpBtn) {
        requestOtpBtn.addEventListener("click", function() {
            requestOtpBtn.disabled = true;
            requestOtpBtn.textContent = "Sending...";
            const formData = new FormData();
            formData.append("action", "requestVoteOtp");
            const csrf = document.querySelector("#voteForm input[name='csrfToken']");
            if (csrf) {
                formData.append("csrfToken", csrf.value);
            }
            
            fetch(contextPath + "/user/actions", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "X-Requested-With": "XMLHttpRequest",
                    "X-CSRF-Token": csrf ? csrf.value : ""
                },
                body: new URLSearchParams(formData)
            })
            .then(response => {
                if (!response.ok) {
                    return response.json()
                        .catch(() => ({ message: "OTP request failed with status " + response.status }))
                        .then(data => {
                            throw new Error(data.message || "OTP request failed with status " + response.status);
                        });
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    const destination = data.destination || "your registered email";
                    alert("OTP sent to " + destination + ". Please check your inbox.");
                    requestOtpBtn.textContent = "Resend OTP";
                    if (otpInput) otpInput.focus();
                } else {
                    alert(data.message || "Failed to generate security code. Please try again.");
                    requestOtpBtn.textContent = "Get OTP";
                }
                requestOtpBtn.disabled = false;
            })
            .catch(error => {
                console.error(error);
                alert(error.message || "Unable to request OTP. Please refresh the page and try again.");
                requestOtpBtn.textContent = "Get OTP";
                requestOtpBtn.disabled = false;
            });
        });
    }

    const initialCountdown = document.getElementById("electionCountdown");
    startCountdown(initialCountdown ? initialCountdown.dataset.endsAt : null);
    bindCandidateDetails();
    refresh();

    // --- Face Verification Logic ---
    const startCameraBtn = document.getElementById("startCameraBtn");
    const verifyFaceBtn = document.getElementById("verifyFaceBtn");
    const webcam = document.getElementById("webcam");
    const faceStatus = document.getElementById("faceStatus");
    const scannerOverlay = document.getElementById("faceScannerOverlay");
    const voteSubmitBtn = document.querySelector("#voteForm button[type='submit']");
    let stream = null;
    let faceVerified = false;

    if (startCameraBtn) {
        startCameraBtn.addEventListener("click", async () => {
            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                alert("Camera access is not available in this browser. Open the portal on localhost/HTTPS and allow camera permission.");
                return;
            }

            try {
                stream = await navigator.mediaDevices.getUserMedia({ video: { width: 640, height: 480 } });
                webcam.srcObject = stream;
                startCameraBtn.style.display = "none";
                verifyFaceBtn.style.display = "inline-flex";
                faceStatus.textContent = "Camera Active";
                faceStatus.className = "status-pill active";
            } catch (err) {
                let message = "Could not access camera. Please allow camera permission in the browser.";
                if (err && err.name === "NotAllowedError") {
                    message = "Camera permission was denied. Click the camera icon in the address bar and allow access.";
                } else if (err && err.name === "NotFoundError") {
                    message = "No camera was found on this device.";
                } else if (err && err.name === "NotReadableError") {
                    message = "The camera is already in use by another app. Close it and try again.";
                } else if (!window.isSecureContext) {
                    message = "Camera access requires localhost or HTTPS. Open http://localhost:8082/ during local testing.";
                }
                alert(message);
                console.error(err);
            }
        });
    }

    if (verifyFaceBtn) {
        verifyFaceBtn.addEventListener("click", () => {
            verifyFaceBtn.disabled = true;
            verifyFaceBtn.textContent = "Scanning...";
            scannerOverlay.style.display = "block";
            faceStatus.textContent = "Authenticating...";

            // Simulate AI Face Comparison
            setTimeout(() => {
                scannerOverlay.style.display = "none";
                faceVerified = true;
                faceStatus.textContent = "Identity Verified";
                faceStatus.style.background = "rgba(16, 185, 129, 0.15)";
                faceStatus.style.color = "var(--secondary)";
                verifyFaceBtn.textContent = "Success ✓";
                
                // Enable vote button if other conditions met
                if (voteSubmitBtn && !voteSubmitBtn.dataset.alreadyVoted) {
                    voteSubmitBtn.disabled = false;
                }
                
                // Stop camera to save power
                if (stream) {
                    stream.getTracks().forEach(track => track.stop());
                }
            }, 3000);
        });
    }

    const notifBtn = document.getElementById("notifBtn");
    const notifDropdown = document.getElementById("notifDropdown");
    if (notifBtn && notifDropdown) {
        notifBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            const isOpen = notifDropdown.parentElement.classList.toggle("open");
            notifBtn.setAttribute("aria-expanded", String(isOpen));
        });
        document.addEventListener("click", () => {
            if (notifDropdown) {
                notifDropdown.parentElement.classList.remove("open");
                notifBtn.setAttribute("aria-expanded", "false");
            }
        });
        notifDropdown.addEventListener("click", (e) => e.stopPropagation());
    }

    window.setInterval(refresh, 5000);
})();
