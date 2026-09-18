document.querySelectorAll("[data-password-toggle]").forEach((button) => {

    button.addEventListener("click", () => {

        const targetId = button.getAttribute("aria-controls");
        const passwordInput = document.getElementById(targetId);

        if (passwordInput.type === "password") {
            passwordInput.type = "text";
            button.textContent = "非表示";
        } else {
            passwordInput.type = "password";
            button.textContent = "表示";
        }
    });
});