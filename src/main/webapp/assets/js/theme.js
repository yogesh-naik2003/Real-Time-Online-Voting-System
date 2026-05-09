document.addEventListener("DOMContentLoaded", function() {
    const savedTheme = localStorage.getItem("sec_theme") || "dark";
    const themeToggle = document.getElementById("themeToggle");

    function applyTheme(theme) {
        const isLight = theme === "light";
        document.documentElement.classList.toggle("light-theme", isLight);
        document.documentElement.classList.toggle("dark-theme", !isLight);
        document.body.classList.toggle("light-theme", isLight);
        document.body.classList.toggle("dark-theme", !isLight);
        localStorage.setItem("sec_theme", isLight ? "light" : "dark");

        if (themeToggle) {
            const compact = themeToggle.dataset.compact === "true" || themeToggle.offsetWidth <= 52;
            themeToggle.textContent = compact ? (isLight ? "D" : "L") : (isLight ? "Dark Mode" : "Light Mode");
            themeToggle.setAttribute("aria-label", isLight ? "Switch to dark mode" : "Switch to light mode");
            themeToggle.setAttribute("title", isLight ? "Switch to dark mode" : "Switch to light mode");
        }
    }

    applyTheme(savedTheme);

    if (themeToggle) {
        themeToggle.addEventListener("click", function() {
            const nextTheme = document.body.classList.contains("light-theme") ? "dark" : "light";
            applyTheme(nextTheme);
        });
    }
});
