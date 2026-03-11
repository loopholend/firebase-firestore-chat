(function () {
    var storageKey = "theme";
    var body = document.body;
    var toggle = document.getElementById("themeToggle");

    function applyTheme(theme) {
        body.classList.toggle("dark", theme === "dark");
        if (toggle) {
            toggle.textContent = theme === "dark" ? "☀" : "🌙";
        }
    }

    var savedTheme = localStorage.getItem(storageKey);
    var initialTheme = savedTheme === "dark" ? "dark" : "light";
    applyTheme(initialTheme);

    if (toggle) {
        toggle.addEventListener("click", function () {
            var nextTheme = body.classList.contains("dark") ? "light" : "dark";
            localStorage.setItem(storageKey, nextTheme);
            applyTheme(nextTheme);
        });
    }
})();
