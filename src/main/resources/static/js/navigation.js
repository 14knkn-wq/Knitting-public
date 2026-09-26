const menuButton = document.getElementById("global-menu-button");
const menu = document.getElementById("global-menu");

const logoutButton = document.getElementById("logout-button");
const logoutDialog = document.getElementById("logout-dialog");
const logoutUnsavedWarning =
    document.getElementById("logout-unsaved-warning");

const logoutConfirmButton =
    document.getElementById("logout-confirm-button");
const logoutForm = document.getElementById("logout-form");

const logoutCancelButton =
    document.getElementById("logout-cancel-button");
menuButton.addEventListener("click", () => {
    const isOpen = !menu.hidden;

    menu.hidden = isOpen;
    menuButton.setAttribute("aria-expanded", String(!isOpen));
});

logoutButton.addEventListener("click", () => {
    const hasUnsavedBasicInfo =
        document.body.dataset.basicInfoDirty === "true";

    logoutUnsavedWarning.hidden = !hasUnsavedBasicInfo;

    logoutDialog.showModal();
});

logoutCancelButton.addEventListener("click", () => {
    logoutDialog.close();
});

logoutConfirmButton.addEventListener("click", () => {
    logoutForm.requestSubmit();
});

document.addEventListener("click", (event) => {
    if (menu.hidden) {
        return;
    }

    if (menu.contains(event.target) || menuButton.contains(event.target)) {
        return;
    }

    menu.hidden = true;
    menuButton.setAttribute("aria-expanded", "false");
});

document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape" || menu.hidden) {
        return;
    }

    menu.hidden = true;
    menuButton.setAttribute("aria-expanded", "false");
    menuButton.focus();
});

menu.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => {
        menu.hidden = true;
        menuButton.setAttribute("aria-expanded", "false");
    });
});

logoutForm.addEventListener("submit", () => {
    document.body.dataset.allowPageLeave = "true";
});