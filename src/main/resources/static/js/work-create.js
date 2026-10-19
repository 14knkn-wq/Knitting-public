const photoInput =
        document.getElementById("photo");

const photoPreview =
        document.getElementById("photo-preview");

const MAX_PHOTO_SIZE_BYTES =
		10 * 1024 * 1024;

if (photoInput && photoPreview) {

    const defaultPreviewSrc =
            photoPreview.src;

    let currentObjectUrl = null;

    const clearObjectUrl = () => {
        if (currentObjectUrl !== null) {
            URL.revokeObjectURL(
                    currentObjectUrl
            );

            currentObjectUrl = null;
        }
    };

    photoInput.addEventListener(
            "change",
            () => {

                clearObjectUrl();

                const file =
                        photoInput.files[0];

				if (!file) {
					photoPreview.src =
						    defaultPreviewSrc;

					return;
				}

				if (file.size > MAX_PHOTO_SIZE_BYTES) {
					photoPreview.src =
						    defaultPreviewSrc;

					return;
				}

				currentObjectUrl =
						URL.createObjectURL(file);

                photoPreview.src =
                        currentObjectUrl;
            }
    );

    window.addEventListener(
            "pagehide",
            clearObjectUrl
    );
}

const workCreateForm =
        document.getElementById("work-create-form");

const submitButton =
        document.getElementById("submit-button");

const photoSizeError =
		document.getElementById("photo-size-error");

		if (workCreateForm && submitButton) {
		    const nameInput =
		            document.getElementById("name");

		    let isSubmitting = false;

		    function getNameForValidation() {
		        const whitespace =
		                "[\\u0009-\\u000D\\u001C-\\u0020\\u1680"
		                + "\\u2000-\\u2006\\u2008-\\u200A"
		                + "\\u2028\\u2029\\u205F\\u3000]";

		        const pattern = new RegExp(
		                "^" + whitespace + "+|" + whitespace + "+$",
		                "g"
		        );

		        return nameInput.value.replace(pattern, "");
		    }

		    function updateSubmitButton() {
		        const nameForValidation =
		                getNameForValidation();

				const isInvalidName =
						nameForValidation.length === 0
						|| Array.from(nameForValidation).length > 50;

				const selectedPhoto =
						photoInput.files[0];

				const isTooLargePhoto =
						selectedPhoto !== undefined
						&& selectedPhoto.size > MAX_PHOTO_SIZE_BYTES;

				photoSizeError.hidden = !isTooLargePhoto;

				submitButton.disabled =
						isSubmitting
						|| isInvalidName
						|| isTooLargePhoto
						|| !workCreateForm.checkValidity();
		    }

		    workCreateForm.addEventListener(
		            "input",
		            updateSubmitButton
		    );

		    workCreateForm.addEventListener(
		            "change",
		            updateSubmitButton
		    );

		    workCreateForm.addEventListener(
		            "submit",
		            () => {
		                isSubmitting = true;
		                submitButton.disabled = true;
		            }
		    );

		    window.addEventListener(
		            "pageshow",
		            () => {
		                isSubmitting = false;
		                updateSubmitButton();
		            }
		    );

		    updateSubmitButton();
		}