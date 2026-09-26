const registrationForm = document.querySelector("form");

if (registrationForm) {

    const submitButton =
            registrationForm.querySelector('button[type="submit"]');

    const password =
            document.getElementById("password");

    const passwordConfirmation =
            document.getElementById("passwordConfirmation");
	
	const email =
	        document.getElementById("email");

	const username =
			document.getElementById("username");

	const clearKnownDuplicate = (input) => {

			if (input.dataset.knownDuplicate !== "true") {
			    return;
			}

			input.dataset.knownDuplicate = "false";

			const errorMessage =
			       registrationForm.querySelector(
			                    `[data-field-error="${input.id}"]`);

			if (errorMessage) {
			    errorMessage.remove();
			}
	};		
	
    const updateSubmitButton = () => {

        const passwordsMatch =
                password.value === passwordConfirmation.value;

        submitButton.disabled =
                !registrationForm.checkValidity()
                || !passwordsMatch;
    };
	
	email.addEventListener("input", () => {
	    clearKnownDuplicate(email);
	});

	username.addEventListener("input", () => {
	    clearKnownDuplicate(username);
	});

    registrationForm.addEventListener(
            "input",
            updateSubmitButton);

    updateSubmitButton();
}