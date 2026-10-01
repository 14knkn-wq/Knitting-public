const photoInput =
        document.getElementById("photo");

const photoPreview =
        document.getElementById("photo-preview");

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

if (workCreateForm && submitButton) {
    workCreateForm.addEventListener(
            "submit",
            () => {
                submitButton.disabled = true;
            }
    );
}