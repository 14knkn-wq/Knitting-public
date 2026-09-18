const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");
const loginButton = document.querySelector("[data-login-submit]");

function updateLoginButtonState() {
    loginButton.disabled =
        usernameInput.value.length === 0 ||
        passwordInput.value.length === 0;
}

usernameInput.addEventListener("input", updateLoginButtonState);
passwordInput.addEventListener("input", updateLoginButtonState);

updateLoginButtonState();