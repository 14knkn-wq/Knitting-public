const body = document.body;
const workId = body.dataset.workId;

const csrfToken =
        document.querySelector(
                'meta[name="_csrf"]'
        ).content;

const csrfHeader =
        document.querySelector(
                'meta[name="_csrf_header"]'
        ).content;

/* ========================================
 * 基本情報編集
 * ======================================== */

const basicInfoForm =
        document.getElementById("basic-info-form");

const basicInfoView =
        document.getElementById("basic-info-view");

const basicInfoEdit =
        document.getElementById("basic-info-edit");

const basicInfoEditButton =
        document.getElementById(
                "basic-info-edit-button"
        );

const basicInfoSaveButton =
        document.getElementById(
                "basic-info-save-button"
        );

const basicInfoCancelButton =
        document.getElementById(
                "basic-info-cancel-button"
        );

const nameInput =
        document.getElementById("name");

const categorySelect =
        document.getElementById("category");

const photoInput =
        document.getElementById("photo");

const deletePhotoInput =
        document.getElementById("delete-photo");

const photoPreview =
        document.getElementById(
                "work-photo-preview"
        );

const unsavedLeaveDialog =
        document.getElementById(
                "unsaved-leave-dialog"
        );

const unsavedLeaveCancelButton =
        document.getElementById(
                "unsaved-leave-cancel-button"
        );

const unsavedLeaveConfirmButton =
        document.getElementById(
                "unsaved-leave-confirm-button"
        );

const deleteWorkButton =
        document.getElementById(
                "delete-work-button"
        );

const deleteWorkDialog =
        document.getElementById(
                "delete-work-dialog"
        );

const deleteWorkUnsavedWarning =
        document.getElementById(
                "delete-work-unsaved-warning"
        );

const deleteWorkCancelButton =
        document.getElementById(
                "delete-work-cancel-button"
        );

const deleteWorkForm =
        document.getElementById(
                "delete-work-form"
        );

const originalName =
        basicInfoForm.dataset.originalName;

const originalCategory =
        basicInfoForm.dataset.originalCategory;

const hasOriginalPhoto =
        basicInfoForm.dataset.hasPhoto === "true";

const defaultPhotoSrc =
        new URL(
                "/images/default-work.png",
                window.location.origin
        ).href;

const originalPhotoSrc =
        hasOriginalPhoto
                ? new URL(
                        `/works/${workId}/image`,
                        window.location.origin
                ).href
                : defaultPhotoSrc;

let photoObjectUrl = null;
let pendingLeaveUrl = null;
let isDeleting = false;

function clearPhotoObjectUrl() {
    if (photoObjectUrl === null) {
        return;
    }

    URL.revokeObjectURL(photoObjectUrl);
    photoObjectUrl = null;
}

function hasSelectedPhoto() {
    return photoInput.files.length > 0;
}

function updateBasicInfoDirty() {
    const nameChanged =
            nameInput.value !== originalName;

    const categoryChanged =
            categorySelect.value
            !== originalCategory;

    const photoChanged =
            hasSelectedPhoto();

    const photoDeleted =
            deletePhotoInput !== null
            && deletePhotoInput.checked;

    body.dataset.basicInfoDirty =
            String(
                    nameChanged
                    || categoryChanged
                    || photoChanged
                    || photoDeleted
            );
}

function enterBasicInfoEditMode() {
    basicInfoView.hidden = true;
    basicInfoEdit.hidden = false;
    basicInfoEditButton.hidden = true;
}

function leaveBasicInfoEditMode() {
    basicInfoView.hidden = false;
    basicInfoEdit.hidden = true;
    basicInfoEditButton.hidden = false;
}

function restoreBasicInfo() {
    nameInput.value = originalName;
    categorySelect.value = originalCategory;
    photoInput.value = "";

    if (deletePhotoInput !== null) {
        deletePhotoInput.checked = false;
    }

    clearPhotoObjectUrl();

    photoPreview.src = originalPhotoSrc;

    body.dataset.basicInfoDirty = "false";

    leaveBasicInfoEditMode();
}

basicInfoEditButton.addEventListener(
        "click",
        () => {
            enterBasicInfoEditMode();
        }
);

basicInfoCancelButton.addEventListener(
        "click",
        () => {
            restoreBasicInfo();
        }
);

nameInput.addEventListener(
        "input",
        updateBasicInfoDirty
);

categorySelect.addEventListener(
        "change",
        updateBasicInfoDirty
);

photoInput.addEventListener(
        "change",
        () => {
            clearPhotoObjectUrl();

            const file =
                    photoInput.files[0];

            if (file) {
                if (deletePhotoInput !== null) {
                    deletePhotoInput.checked = false;
                }

                photoObjectUrl =
                        URL.createObjectURL(file);

                photoPreview.src =
                        photoObjectUrl;
            } else {
                photoPreview.src =
                        deletePhotoInput !== null
                        && deletePhotoInput.checked
                                ? defaultPhotoSrc
                                : originalPhotoSrc;
            }

            updateBasicInfoDirty();
        }
);

if (deletePhotoInput !== null) {
    deletePhotoInput.addEventListener(
            "change",
            () => {
                if (deletePhotoInput.checked) {
                    photoInput.value = "";

                    clearPhotoObjectUrl();

                    photoPreview.src =
                            defaultPhotoSrc;
                } else {
                    photoPreview.src =
                            originalPhotoSrc;
                }

                updateBasicInfoDirty();
            }
    );
}

basicInfoForm.addEventListener(
        "submit",
        () => {
            body.dataset.allowPageLeave =
                    "true";

            basicInfoSaveButton.disabled =
                    true;

            basicInfoSaveButton.textContent =
                    "保存中...";
        }
);

if (basicInfoForm.dataset.hasErrors === "true") {
    enterBasicInfoEditMode();
    updateBasicInfoDirty();
}

const nameLengthError =
        document.getElementById(
                "basic-info-name-length-error"
        );

		function getNameForValidation() {
		    // Javaのstrip()と同じ空白文字を、前後から除きます。
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

		function updateNameLengthValidation() {
			const nameForValidation =
			        getNameForValidation();

			const isBlank =
			        nameForValidation.length === 0;

			const isTooLong =
			        countCodePoints(nameForValidation) > 50;

		    if (isBlank) {
		        nameLengthError.textContent =
		                "作品名を入力してください。";
		    } else if (isTooLong) {
		        nameLengthError.textContent =
		                "作品名は50文字以内で入力してください。";
		    }

		    nameLengthError.hidden =
		            !isBlank && !isTooLong;

		    basicInfoSaveButton.disabled =
		            isBlank || isTooLong;
		}

nameInput.addEventListener(
        "input",
        updateNameLengthValidation
);

basicInfoCancelButton.addEventListener(
        "click",
        updateNameLengthValidation
);

updateNameLengthValidation();

// 写真の容量チェック（上限10MiB）
const photoSizeError =
        document.getElementById(
                "basic-info-photo-size-error"
        );

const MAX_PHOTO_SIZE_BYTES =
        10 * 1024 * 1024;

function updatePhotoSizeValidation() {
    const selectedFile =
            photoInput.files[0];

    const isTooLarge =
            selectedFile !== undefined
            && selectedFile.size > MAX_PHOTO_SIZE_BYTES;

    // 写真のエラーメッセージを切り替える
    photoSizeError.hidden = !isTooLarge;

	// 作品名と写真の両方を確認する
	const nameForValidation =
	        getNameForValidation();

	const isBlankName =
	        nameForValidation.length === 0;

	const isTooLongName =
	        countCodePoints(nameForValidation) > 50;

    basicInfoSaveButton.disabled =
            isBlankName
            || isTooLongName
            || isTooLarge;
}

// 写真を選択したとき
photoInput.addEventListener(
        "change",
        updatePhotoSizeValidation
);

// 作品名を変更したとき
nameInput.addEventListener(
        "input",
        updatePhotoSizeValidation
);

// 編集をキャンセルしたとき
basicInfoCancelButton.addEventListener(
        "click",
        updatePhotoSizeValidation
);

// 写真の削除を選択したとき
if (deletePhotoInput !== null) {
    deletePhotoInput.addEventListener(
            "change",
            updatePhotoSizeValidation
    );
}

// 初期状態も確認する
updatePhotoSizeValidation();

// 選択した写真のファイル名を表示する
const photoFileName =
        document.getElementById("photo-file-name");

function updatePhotoFileName() {
    const selectedFile =
            photoInput.files[0];

    photoFileName.textContent =
            selectedFile
                    ? selectedFile.name
                    : "選択されていません";
}

photoInput.addEventListener(
        "change",
        updatePhotoFileName
);

basicInfoCancelButton.addEventListener(
        "click",
        updatePhotoFileName
);

if (deletePhotoInput !== null) {
    deletePhotoInput.addEventListener(
            "change",
            updatePhotoFileName
    );
}

updatePhotoFileName();

/* ========================================
 * 未保存離脱
 * ======================================== */

function isNormalInternalLink(
        event,
        link
) {
    if (event.defaultPrevented
            || event.button !== 0
            || event.ctrlKey
            || event.metaKey
            || event.shiftKey
            || event.altKey) {
        return false;
    }

    if (link === null
            || link.target === "_blank"
            || link.hasAttribute("download")) {
        return false;
    }

    const url =
            new URL(
                    link.href,
                    window.location.href
            );

    return url.origin
            === window.location.origin
            && url.href
            !== window.location.href;
}

document.addEventListener(
        "click",
        (event) => {
            if (body.dataset.basicInfoDirty
                    !== "true"
                    || body.dataset.allowPageLeave
                    === "true") {
                return;
            }

            const link =
                    event.target.closest("a[href]");

            if (!isNormalInternalLink(
                    event,
                    link
            )) {
                return;
            }

            event.preventDefault();

            pendingLeaveUrl = link.href;

            unsavedLeaveDialog.showModal();
        }
);

unsavedLeaveCancelButton.addEventListener(
        "click",
        () => {
            pendingLeaveUrl = null;
            unsavedLeaveDialog.close();
        }
);

unsavedLeaveConfirmButton.addEventListener(
        "click",
        () => {
            if (pendingLeaveUrl === null) {
                unsavedLeaveDialog.close();
                return;
            }

            body.dataset.allowPageLeave =
                    "true";

            window.location.href =
                    pendingLeaveUrl;
        }
);

window.addEventListener(
        "beforeunload",
        (event) => {
            if (body.dataset.basicInfoDirty
                    === "true"
                    && body.dataset.allowPageLeave
                    !== "true") {

                event.preventDefault();
                event.returnValue = "";
            }
        }
);

/* ========================================
 * 削除
 * ======================================== */

deleteWorkButton.addEventListener(
        "click",
        () => {
            deleteWorkUnsavedWarning.hidden =
                    body.dataset.basicInfoDirty
                    !== "true";

            deleteWorkDialog.showModal();
        }
);

deleteWorkCancelButton.addEventListener(
        "click",
        () => {
            deleteWorkDialog.close();
        }
);

deleteWorkForm.addEventListener(
        "submit",
        () => {
            isDeleting = true;

            body.dataset.allowPageLeave =
                    "true";
        }
);

/* ========================================
 * 自動保存 共通
 * ======================================== */

const stitchInput =
        document.getElementById("stitch-count");

const stitchDecrementButton =
        document.getElementById(
                "stitch-decrement-button"
        );

const stitchIncrementButton =
        document.getElementById(
                "stitch-increment-button"
        );

const stitchError =
        document.getElementById(
                "stitch-error"
        );

const stitchSaveStatus =
        document.getElementById(
                "stitch-save-status"
        );

const rowInput =
        document.getElementById("row-count");

const rowDecrementButton =
        document.getElementById(
                "row-decrement-button"
        );

const rowIncrementButton =
        document.getElementById(
                "row-increment-button"
        );

const rowError =
        document.getElementById(
                "row-error"
        );

const rowSaveStatus =
        document.getElementById(
                "row-save-status"
        );

const memoInput =
        document.getElementById("work-memo");

const memoCharacterCount =
        document.getElementById(
                "memo-character-count"
        );

const memoError =
        document.getElementById(
                "memo-error"
        );

const memoSaveStatus =
        document.getElementById(
                "memo-save-status"
        );

const statusInProgress =
        document.getElementById(
                "work-status-in-progress"
        );

const statusCompleted =
        document.getElementById(
                "work-status-completed"
        );

const completionDateContainer =
        document.getElementById(
                "completion-date-container"
        );

const completionDateInput =
        document.getElementById(
                "completion-date"
        );

const workStateError =
        document.getElementById(
                "work-state-error"
        );

const workStateSaveStatus =
        document.getElementById(
                "work-state-save-status"
        );

let autoSaveStopped = false;
let autoSaveStopMessage = null;

async function postAutoSave(
        url,
        params,
        options = {}
) {
    return fetch(
            url,
            {
                method: "POST",
                headers: {
                    [csrfHeader]: csrfToken,
                    "Content-Type":
                        "application/x-www-form-urlencoded;charset=UTF-8"
                },
                body: new URLSearchParams(
                        params
                ),
                credentials: "same-origin",
                keepalive:
                    options.keepalive ?? false
            }
    );
}

function createRetryButton(
        statusElement,
        retryHandler
) {
    const button =
            document.createElement("button");

    button.type = "button";
    button.textContent = "再試行";
    button.hidden = true;

    button.addEventListener(
            "click",
            retryHandler
    );

    statusElement.insertAdjacentElement(
            "afterend",
            button
    );

    return button;
}

function clearSavedMessageTimer(
        channel
) {
    if (channel.savedMessageTimerId !== null) {
        clearTimeout(
                channel.savedMessageTimerId
        );

        channel.savedMessageTimerId = null;
    }
}

function clearDebounceTimer(
        channel
) {
    if (channel.debounceTimerId !== null) {
        clearTimeout(
                channel.debounceTimerId
        );

        channel.debounceTimerId = null;
    }
}

function showSaving(
        channel
) {
    clearSavedMessageTimer(channel);

    channel.retryButton.hidden = true;
    channel.statusElement.textContent =
            "保存中...";
}

function showSaved(
        channel
) {
    clearSavedMessageTimer(channel);

    channel.retryButton.hidden = true;
    channel.statusElement.textContent =
            "保存しました";

    channel.state = "SAVED";

    channel.savedMessageTimerId =
            setTimeout(
                    () => {
                        if (channel.state
                                !== "SAVED") {
                            return;
                        }

                        channel.statusElement
                                .textContent = "";

                        channel.state = "IDLE";
                    },
                    2000
            );
}

function showSaveError(
        channel,
        allowRetry
) {
    clearSavedMessageTimer(channel);

    channel.statusElement.textContent =
            "保存できませんでした";

    channel.retryButton.hidden =
            !allowRetry;
}

function showFieldError(
        element,
        message
) {
    element.textContent = message;
    element.hidden = false;
}

function clearFieldError(
        element
) {
    element.textContent = "";
    element.hidden = true;
}

function normalizeFullWidthDigits(
        value
) {
    return value.replace(
            /[０-９]/g,
            (character) =>
                    String.fromCharCode(
                            character.charCodeAt(0)
                            - 0xFEE0
                    )
    );
}

function parseCounterValue(
        value
) {
    if (!/^\d+$/.test(value)) {
        return null;
    }

    const number = Number(value);

    if (!Number.isInteger(number)
            || number < 0
            || number > 9999) {
        return null;
    }

    return number;
}

function countCodePoints(
        value
) {
    return Array.from(value).length;
}

function localTodayString() {
    const now = new Date();

    const year =
            String(now.getFullYear());

    const month =
            String(now.getMonth() + 1)
                    .padStart(2, "0");

    const day =
            String(now.getDate())
                    .padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function addOptionalParam(
        params,
        name,
        value
) {
    if (value !== null
            && value !== undefined
            && value !== "") {
        params[name] = value;
    }
}

/* ========================================
 * カウンターチャンネル
 * ======================================== */

function createCounterChannel(
        counterType,
        input,
        decrementButton,
        incrementButton,
        errorElement,
        statusElement
) {
    const initialValue =
            parseCounterValue(input.value);

    const channel = {
        counterType,
        input,
        decrementButton,
        incrementButton,
        errorElement,
        statusElement,
        state: "IDLE",
        savedValue: initialValue,
        pendingValue: initialValue,
        sentValue: null,
        debounceTimerId: null,
        savedMessageTimerId: null,
        validationError: false,
        keepaliveNext: false,
        retryButton: null
    };

    channel.retryButton =
            createRetryButton(
                    statusElement,
                    () => {
                        retryCounter(channel);
                    }
            );

    return channel;
}

const stitchChannel =
        createCounterChannel(
                "STITCH",
                stitchInput,
                stitchDecrementButton,
                stitchIncrementButton,
                stitchError,
                stitchSaveStatus
        );

const rowChannel =
        createCounterChannel(
                "ROW",
                rowInput,
                rowDecrementButton,
                rowIncrementButton,
                rowError,
                rowSaveStatus
        );

function updateCounterButtons(
        channel
) {
    const value =
            parseCounterValue(
                    channel.input.value
            );

    if (value === null) {
        channel.decrementButton.disabled =
                true;

        channel.incrementButton.disabled =
                true;

        return;
    }

    channel.decrementButton.disabled =
            value <= 0;

    channel.incrementButton.disabled =
            value >= 9999;
}

function scheduleCounterSave(
        channel
) {
    if (autoSaveStopped
            || isDeleting
            || channel.state === "CONFLICT") {
        return;
    }

    if (channel.state === "ERROR") {
        return;
    }

    clearDebounceTimer(channel);

    if (channel.state === "SAVING") {
        return;
    }

    if (channel.pendingValue
            === channel.savedValue) {

        channel.state = "IDLE";
        channel.statusElement.textContent = "";
        return;
    }

    channel.state = "WAITING";

    channel.debounceTimerId =
            setTimeout(
                    () => {
                        channel.debounceTimerId =
                                null;

                        saveCounter(
                                channel,
                                false
                        );
                    },
                    300
            );
}

function handleCounterInput(
        channel
) {
    const normalized =
            normalizeFullWidthDigits(
                    channel.input.value
            );

    if (normalized
            !== channel.input.value) {
        channel.input.value =
                normalized;
    }

    updateCounterButtons(channel);

	if (autoSaveStopped) {

	    if (autoSaveStopMessage !== null) {
	        showFieldError(
	                channel.errorElement,
	                autoSaveStopMessage
	        );
	    }

	    return;
	}

    if (normalized === "") {
        clearDebounceTimer(channel);

        channel.validationError = false;

        clearFieldError(
                channel.errorElement
        );

        return;
    }

    const value =
            parseCounterValue(normalized);

    if (value === null) {
        clearDebounceTimer(channel);

        channel.validationError = true;

        showFieldError(
                channel.errorElement,
                "0～9999の整数で入力してください。"
        );

        return;
    }

    channel.validationError = false;
    channel.pendingValue = value;

    clearFieldError(
            channel.errorElement
    );

    scheduleCounterSave(channel);
}

async function saveCounter(
        channel,
        keepalive
) {
    if (autoSaveStopped
            || isDeleting
            || channel.validationError
            || channel.state === "CONFLICT"
            || channel.state === "ERROR"
            || channel.state === "SAVING") {
        return;
    }

    if (channel.pendingValue
            === channel.savedValue) {
        channel.state = "IDLE";
        return;
    }

    clearDebounceTimer(channel);

    channel.sentValue =
            channel.pendingValue;

    channel.state = "SAVING";

    showSaving(channel);

    const params = {
        counterType:
            channel.counterType,
        value:
            String(channel.sentValue),
        expectedValue:
            String(channel.savedValue)
    };

    let response;

	try {
	    response =
	            await postAutoSave(
	                    `/works/${workId}/counter`,
	                    params,
	                    {
	                        keepalive
	                    }
	            );
	} catch (error) {

	    if (autoSaveStopped) {
	        return;
	    }

	    handleNetworkFailure(channel);
	    return;
	}

	if (autoSaveStopped) {
	    return;
	}

	if (response.ok) {
        const result =
                await response.json();

        channel.savedValue =
                result.value;

        channel.state = "IDLE";

        if (channel.pendingValue
                === channel.sentValue) {

            channel.pendingValue =
                    result.value;

            channel.input.value =
                    String(result.value);
        }

        updateCounterButtons(channel);

        if (channel.pendingValue
                !== channel.savedValue) {

            const nextKeepalive =
                    channel.keepaliveNext;

            channel.keepaliveNext = false;

            saveCounter(
                    channel,
                    nextKeepalive
            );

            return;
        }

        showSaved(channel);
        return;
    }

    await handleCounterError(
            channel,
            response
    );
}

async function handleCounterError(
        channel,
        response
) {
    if (response.status === 400) {
        channel.state = "IDLE";
        channel.validationError = true;

        channel.statusElement.textContent = "";

        try {
            const body =
                    await response.json();

            const message =
                    body.fieldErrors?.value
                    ?? body.fieldErrors?.expectedValue
                    ?? body.fieldErrors?.counterType
                    ?? "入力内容を確認してください。";

            showFieldError(
                    channel.errorElement,
                    message
            );
        } catch (error) {
            showFieldError(
                    channel.errorElement,
                    "入力内容を確認してください。"
            );
        }

        return;
    }

    if (response.status === 409) {
        channel.state = "CONFLICT";

        showSaveError(
                channel,
                false
        );

        showFieldError(
                channel.errorElement,
                "他の変更と競合したため保存できませんでした。ページを再読み込みしてください。"
        );

        return;
    }

    if (handleGlobalStopStatus(
            response.status,
            channel
    )) {
        return;
    }

    if (response.status >= 500) {
        channel.state = "ERROR";

        showSaveError(
                channel,
                true
        );

        return;
    }

    channel.state = "ERROR";

    showSaveError(
            channel,
            true
    );
}

function handleNetworkFailure(
        channel
) {
    channel.state = "ERROR";

    showSaveError(
            channel,
            true
    );
}

function retryCounter(
        channel
) {
    if (autoSaveStopped
            || channel.state !== "ERROR"
            || channel.validationError) {
        return;
    }

    channel.state = "IDLE";

    saveCounter(
            channel,
            false
    );
}

function changeCounter(
        channel,
        amount
) {
    const current =
            parseCounterValue(
                    channel.input.value
            );

    if (current === null) {
        return;
    }

    const next =
            current + amount;

    if (next < 0
            || next > 9999) {
        return;
    }

    channel.input.value =
            String(next);

    handleCounterInput(channel);
}

stitchInput.addEventListener(
        "input",
        () => handleCounterInput(
                stitchChannel
        )
);

rowInput.addEventListener(
        "input",
        () => handleCounterInput(
                rowChannel
        )
);

stitchInput.addEventListener(
        "blur",
        () => {
            if (parseCounterValue(
                    stitchInput.value
            ) === null) {
                showFieldError(
                        stitchError,
                        "0～9999の整数で入力してください。"
                );
            }
        }
);

rowInput.addEventListener(
        "blur",
        () => {
            if (parseCounterValue(
                    rowInput.value
            ) === null) {
                showFieldError(
                        rowError,
                        "0～9999の整数で入力してください。"
                );
            }
        }
);

stitchDecrementButton.addEventListener(
        "click",
        () => changeCounter(
                stitchChannel,
                -1
        )
);

stitchIncrementButton.addEventListener(
        "click",
        () => changeCounter(
                stitchChannel,
                1
        )
);

rowDecrementButton.addEventListener(
        "click",
        () => changeCounter(
                rowChannel,
                -1
        )
);

rowIncrementButton.addEventListener(
        "click",
        () => changeCounter(
                rowChannel,
                1
        )
);

/* ========================================
 * メモチャンネル
 * ======================================== */

const memoChannel = {
    state: "IDLE",
    savedValue: memoInput.value,
    pendingValue: memoInput.value,
    sentValue: null,
    debounceTimerId: null,
    savedMessageTimerId: null,
    validationError: false,
    keepaliveNext: false,
    statusElement: memoSaveStatus,
    errorElement: memoError,
    retryButton: null
};

memoChannel.retryButton =
        createRetryButton(
                memoSaveStatus,
                () => {
                    retryMemo();
                }
        );

function updateMemoCharacterCount() {
    memoCharacterCount.textContent =
            String(
                    countCodePoints(
                            memoInput.value
                    )
            );
}

function scheduleMemoSave() {
    if (autoSaveStopped
            || isDeleting
            || memoChannel.state === "CONFLICT") {
        return;
    }

    if (memoChannel.state === "ERROR") {
        return;
    }

    clearDebounceTimer(memoChannel);

    if (memoChannel.state === "SAVING") {
        return;
    }

    if (memoChannel.pendingValue
            === memoChannel.savedValue) {

        memoChannel.state = "IDLE";
        memoChannel.statusElement
                .textContent = "";

        return;
    }

    memoChannel.state = "WAITING";

    memoChannel.debounceTimerId =
            setTimeout(
                    () => {
                        memoChannel.debounceTimerId =
                                null;

                        saveMemo(false);
                    },
                    1000
            );
}

memoInput.addEventListener(
        "input",
        () => {
            memoChannel.pendingValue =
                    memoInput.value;

            updateMemoCharacterCount();

			if (autoSaveStopped) {

			    if (autoSaveStopMessage !== null) {
			        showFieldError(
			                memoError,
			                autoSaveStopMessage
			        );
			    }

			    return;
			}

            const length =
                    countCodePoints(
                            memoInput.value
                    );

            if (length > 1000) {
                clearDebounceTimer(
                        memoChannel
                );

                memoChannel.validationError =
                        true;

                showFieldError(
                        memoError,
                        "制作メモは1000文字以内で入力してください。"
                );

                return;
            }

            memoChannel.validationError =
                    false;

            clearFieldError(memoError);

            scheduleMemoSave();
        }
);

async function saveMemo(
        keepalive
) {
    if (autoSaveStopped
            || isDeleting
            || memoChannel.validationError
            || memoChannel.state === "CONFLICT"
            || memoChannel.state === "ERROR"
            || memoChannel.state === "SAVING") {
        return;
    }

    if (memoChannel.pendingValue
            === memoChannel.savedValue) {
        memoChannel.state = "IDLE";
        return;
    }

    clearDebounceTimer(memoChannel);

    memoChannel.sentValue =
            memoChannel.pendingValue;

    memoChannel.state = "SAVING";

    showSaving(memoChannel);

    let response;

	try {
	    response =
	            await postAutoSave(
	                    `/works/${workId}/memo`,
	                    {
	                        memo:
	                            memoChannel.sentValue,
	                        expectedMemo:
	                            memoChannel.savedValue
	                    },
	                    {
	                        keepalive
	                    }
	            );
	} catch (error) {

	    if (autoSaveStopped) {
	        return;
	    }

	    memoChannel.state = "ERROR";

	    showSaveError(
	            memoChannel,
	            true
	    );

	    return;
	}

	if (autoSaveStopped) {
	    return;
	}

	if (response.ok) {
        memoChannel.savedValue =
                memoChannel.sentValue;

        memoChannel.state = "IDLE";

        if (memoChannel.pendingValue
                !== memoChannel.savedValue) {

            const nextKeepalive =
                    memoChannel.keepaliveNext;

            memoChannel.keepaliveNext = false;

            saveMemo(
                    nextKeepalive
            );

            return;
        }

        showSaved(memoChannel);
        return;
    }

    if (response.status === 400) {
        memoChannel.state = "IDLE";
        memoChannel.validationError = true;

        memoChannel.statusElement
                .textContent = "";

        try {
            const body =
                    await response.json();

            showFieldError(
                    memoError,
                    body.fieldErrors?.memo
                    ?? body.fieldErrors?.expectedMemo
                    ?? "入力内容を確認してください。"
            );
        } catch (error) {
            showFieldError(
                    memoError,
                    "入力内容を確認してください。"
            );
        }

        return;
    }

    if (response.status === 409) {
        memoChannel.state = "CONFLICT";

        showSaveError(
                memoChannel,
                false
        );

		showFieldError(
		        memoError,
		        "他の変更と競合したため保存できませんでした。ページを再読み込みしてください。"
		);

        return;
    }

    if (handleGlobalStopStatus(
            response.status,
            memoChannel
    )) {
        return;
    }

    memoChannel.state = "ERROR";

    showSaveError(
            memoChannel,
            true
    );
}

function retryMemo() {
    if (autoSaveStopped
            || memoChannel.state !== "ERROR"
            || memoChannel.validationError) {
        return;
    }

    memoChannel.state = "IDLE";

    saveMemo(false);
}

/* ========================================
 * ステータス・完成日チャンネル
 * ======================================== */

function getSelectedStatus() {
    return statusCompleted.checked
            ? "COMPLETED"
            : "IN_PROGRESS";
}

function normalizeCompletionDate(
        value
) {
    return value === ""
            ? null
            : value;
}

const initialStatus =
        getSelectedStatus();

const initialCompletionDate =
        normalizeCompletionDate(
                completionDateInput.value
        );

const workStateChannel = {
    state: "IDLE",
    savedStatus: initialStatus,
    savedCompletionDate:
        initialCompletionDate,
    pendingStatus: initialStatus,
    pendingCompletionDate:
        initialCompletionDate,
    sentStatus: null,
    sentCompletionDate: null,
    sentKind: null,
    debounceTimerId: null,
    savedMessageTimerId: null,
    validationError: false,
    keepaliveNext: false,
    statusElement: workStateSaveStatus,
    errorElement: workStateError,
    retryButton: null
};

workStateChannel.retryButton =
        createRetryButton(
                workStateSaveStatus,
                () => {
                    retryWorkState();
                }
        );

function updateCompletionDateAvailability() {
    const completed =
            workStateChannel.pendingStatus
            === "COMPLETED";

    completionDateContainer.hidden =
            !completed;

    if (!completed) {
        completionDateInput.disabled = true;
        completionDateInput.value = "";
        return;
    }

    completionDateInput.disabled =
            workStateChannel.state === "SAVING"
            || workStateChannel.savedStatus
                    !== "COMPLETED"
            || autoSaveStopped
            || workStateChannel.state
                    === "CONFLICT";
}

function validateCompletionDate(
        value
) {
    if (value === null) {
        return "完成日を入力してください。";
    }

    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        return "正しい日付を入力してください。";
    }

    if (value > localTodayString()) {
        return "未来の日付は指定できません。";
    }

    return null;
}

function changePendingStatus(
        status
) {
    if (autoSaveStopped
            || isDeleting
            || workStateChannel.state
                    === "CONFLICT") {
        return;
    }

    workStateChannel.pendingStatus =
            status;

    workStateChannel.validationError =
            false;

    clearFieldError(workStateError);

    if (status === "IN_PROGRESS") {
        workStateChannel.pendingCompletionDate =
                null;

        completionDateInput.value = "";
    }

    updateCompletionDateAvailability();

    if (workStateChannel.state === "ERROR") {
        return;
    }

    saveWorkState(false);
}

statusInProgress.addEventListener(
        "change",
        () => {
            if (statusInProgress.checked) {
                changePendingStatus(
                        "IN_PROGRESS"
                );
            }
        }
);

statusCompleted.addEventListener(
        "change",
        () => {
            if (statusCompleted.checked) {
                changePendingStatus(
                        "COMPLETED"
                );
            }
        }
);

completionDateInput.addEventListener(
        "change",
        () => {
            if (autoSaveStopped
                    || isDeleting
                    || workStateChannel.state
                            === "CONFLICT") {
                return;
            }

            const value =
                    normalizeCompletionDate(
                            completionDateInput.value
                    );

            workStateChannel
                    .pendingCompletionDate =
                    value;

            const validationMessage =
                    validateCompletionDate(
                            value
                    );

            if (validationMessage !== null) {
                workStateChannel
                        .validationError = true;

                showFieldError(
                        workStateError,
                        validationMessage
                );

                return;
            }

            if (workStateChannel.savedStatus
                    !== "COMPLETED") {
                workStateChannel
                        .validationError = true;

                showFieldError(
                        workStateError,
                        "制作中の作品には完成日を設定できません。"
                );

                return;
            }

            workStateChannel.validationError =
                    false;

            clearFieldError(workStateError);

            if (workStateChannel.state
                    === "ERROR") {
                return;
            }

            saveWorkState(false);
        }
);

async function saveWorkState(
        keepalive
) {
    if (autoSaveStopped
            || isDeleting
            || workStateChannel.validationError
            || workStateChannel.state
                    === "CONFLICT"
            || workStateChannel.state
                    === "ERROR"
            || workStateChannel.state
                    === "SAVING") {
        return;
    }

    let endpoint;
    let params;

    if (workStateChannel.pendingStatus
            !== workStateChannel.savedStatus) {

        workStateChannel.sentKind =
                "status";

        workStateChannel.sentStatus =
                workStateChannel.pendingStatus;

        workStateChannel.sentCompletionDate =
                workStateChannel
                        .pendingCompletionDate;

        endpoint =
                `/works/${workId}/status`;

        params = {
            status:
                workStateChannel.sentStatus,
            expectedStatus:
                workStateChannel.savedStatus
        };

        addOptionalParam(
                params,
                "expectedCompletionDate",
                workStateChannel
                        .savedCompletionDate
        );

    } else if (
            workStateChannel.savedStatus
                    === "COMPLETED"
            && workStateChannel
                    .pendingCompletionDate
                    !== workStateChannel
                        .savedCompletionDate) {

        const validationMessage =
                validateCompletionDate(
                        workStateChannel
                                .pendingCompletionDate
                );

        if (validationMessage !== null) {
            workStateChannel.validationError =
                    true;

            showFieldError(
                    workStateError,
                    validationMessage
            );

            return;
        }

        workStateChannel.sentKind =
                "completionDate";

        workStateChannel.sentStatus =
                workStateChannel.pendingStatus;

        workStateChannel.sentCompletionDate =
                workStateChannel
                        .pendingCompletionDate;

        endpoint =
                `/works/${workId}/completion-date`;

        params = {
            completionDate:
                workStateChannel
                        .sentCompletionDate,
            expectedStatus:
                workStateChannel.savedStatus
        };

        addOptionalParam(
                params,
                "expectedCompletionDate",
                workStateChannel
                        .savedCompletionDate
        );

    } else {
        workStateChannel.state = "IDLE";

        updateCompletionDateAvailability();

        return;
    }

    workStateChannel.state = "SAVING";

    updateCompletionDateAvailability();

    showSaving(workStateChannel);

    let response;

	try {
	    response =
	            await postAutoSave(
	                    endpoint,
	                    params,
	                    {
	                        keepalive
	                    }
	            );
	} catch (error) {

	    if (autoSaveStopped) {
	        return;
	    }

	    workStateChannel.state = "ERROR";

	    updateCompletionDateAvailability();

	    showSaveError(
	            workStateChannel,
	            true
	    );

	    return;
	}

	if (autoSaveStopped) {
	    return;
	}

	if (response.ok) {
        const result =
                await response.json();

        if (workStateChannel.sentKind
                === "status") {

            workStateChannel.savedStatus =
                    result.status;

            workStateChannel
                    .savedCompletionDate =
                    normalizeCompletionDate(
                            result.completionDate
                    );

            if (workStateChannel.pendingStatus
                    === workStateChannel
                        .sentStatus) {

                workStateChannel
                        .pendingCompletionDate =
                        workStateChannel
                                .savedCompletionDate;

                completionDateInput.value =
                        workStateChannel
                                .savedCompletionDate
                        ?? "";
            }

        } else {
            workStateChannel
                    .savedCompletionDate =
                    normalizeCompletionDate(
                            result.completionDate
                    );

            if (workStateChannel
                    .pendingCompletionDate
                    === workStateChannel
                        .sentCompletionDate) {

                workStateChannel
                        .pendingCompletionDate =
                        workStateChannel
                                .savedCompletionDate;

                completionDateInput.value =
                        workStateChannel
                                .savedCompletionDate
                        ?? "";
            }
        }

        workStateChannel.state = "IDLE";
        workStateChannel.sentKind = null;

        updateCompletionDateAvailability();

        if (workStateChannel.pendingStatus
                    !== workStateChannel.savedStatus
                || (
                    workStateChannel.savedStatus
                            === "COMPLETED"
                    && workStateChannel
                        .pendingCompletionDate
                        !== workStateChannel
                            .savedCompletionDate
                )) {

            const nextKeepalive =
                    workStateChannel.keepaliveNext;

            workStateChannel.keepaliveNext =
                    false;

            saveWorkState(
                    nextKeepalive
            );

            return;
        }

        showSaved(workStateChannel);
        return;
    }

    if (response.status === 400) {
        workStateChannel.state = "IDLE";
        workStateChannel.validationError =
                true;

        workStateChannel.statusElement
                .textContent = "";

        try {
            const body =
                    await response.json();

            const message =
                    body.fieldErrors
                            ?.completionDate
                    ?? body.fieldErrors?.status
                    ?? body.fieldErrors
                            ?.expectedStatus
                    ?? body.fieldErrors
                            ?.expectedCompletionDate
                    ?? "入力内容を確認してください。";

            showFieldError(
                    workStateError,
                    message
            );
        } catch (error) {
            showFieldError(
                    workStateError,
                    "入力内容を確認してください。"
            );
        }

        updateCompletionDateAvailability();

        return;
    }

    if (response.status === 409) {
        workStateChannel.state =
                "CONFLICT";

        updateCompletionDateAvailability();

        showSaveError(
                workStateChannel,
                false
        );

		showFieldError(
		        workStateError,
		        "他の変更と競合したため保存できませんでした。ページを再読み込みしてください。"
		);

        return;
    }

    if (handleGlobalStopStatus(
            response.status,
            workStateChannel
    )) {
        updateCompletionDateAvailability();
        return;
    }

    workStateChannel.state = "ERROR";

    updateCompletionDateAvailability();

    showSaveError(
            workStateChannel,
            true
    );
}

function retryWorkState() {
    if (autoSaveStopped
            || workStateChannel.state
                    !== "ERROR"
            || workStateChannel.validationError) {
        return;
    }

    workStateChannel.state = "IDLE";

    saveWorkState(false);
}

/* ========================================
 * 全体停止
 * ======================================== */

function stopAllAutoSave(
        message
) {
    autoSaveStopped = true;
    autoSaveStopMessage = message;

    const channels = [
        stitchChannel,
        rowChannel,
        memoChannel,
        workStateChannel
    ];

    channels.forEach(
            (channel) => {
                clearDebounceTimer(channel);
                clearSavedMessageTimer(channel);

                channel.retryButton.hidden = true;

                channel.statusElement
                        .textContent =
                        "保存できませんでした";
            }
    );

    showFieldError(
            stitchError,
            message
    );

    showFieldError(
            rowError,
            message
    );

    showFieldError(
            memoError,
            message
    );

    showFieldError(
            workStateError,
            message
    );

    updateCompletionDateAvailability();
}

function handleGlobalStopStatus(
        status,
        channel
) {
    if (status === 401) {
        stopAllAutoSave(
                "ログイン状態が切れたため保存できませんでした。再度ログインしてください。"
        );

        return true;
    }

    if (status === 403) {
        stopAllAutoSave(
                "保存処理を続けられません。ページを再読み込みしてください。"
        );

        return true;
    }

    if (status === 404) {
        stopAllAutoSave(
                "作品を確認できません。ページを再読み込みするか一覧へ戻ってください。"
        );

        return true;
    }

    return false;
}

/* ========================================
 * 離脱時 best-effort flush
 * ======================================== */

function flushCounterChannel(
        channel
) {
    clearDebounceTimer(channel);

    if (channel.validationError
            || channel.state === "CONFLICT"
            || channel.state === "ERROR"
            || channel.pendingValue
                    === channel.savedValue) {
        return;
    }

    if (channel.state === "SAVING") {
        channel.keepaliveNext = true;
        return;
    }

    channel.state = "IDLE";

    saveCounter(
            channel,
            true
    );
}

function flushMemoChannel() {
    clearDebounceTimer(memoChannel);

    if (memoChannel.validationError
            || memoChannel.state === "CONFLICT"
            || memoChannel.state === "ERROR"
            || memoChannel.pendingValue
                    === memoChannel.savedValue) {
        return;
    }

    if (memoChannel.state === "SAVING") {
        memoChannel.keepaliveNext = true;
        return;
    }

    memoChannel.state = "IDLE";

    saveMemo(true);
}

function flushWorkStateChannel() {
    if (workStateChannel.validationError
            || workStateChannel.state
                    === "CONFLICT"
            || workStateChannel.state
                    === "ERROR") {
        return;
    }

    const hasPending =
            workStateChannel.pendingStatus
                    !== workStateChannel.savedStatus
            || (
                workStateChannel.savedStatus
                        === "COMPLETED"
                && workStateChannel
                    .pendingCompletionDate
                    !== workStateChannel
                        .savedCompletionDate
            );

    if (!hasPending) {
        return;
    }

    if (workStateChannel.state
            === "SAVING") {

        workStateChannel.keepaliveNext =
                true;

        return;
    }

    workStateChannel.state = "IDLE";

    saveWorkState(true);
}

function flushPendingAutoSaves() {
    if (autoSaveStopped
            || isDeleting) {
        return;
    }

    flushCounterChannel(
            stitchChannel
    );

    flushCounterChannel(
            rowChannel
    );

    flushMemoChannel();

    flushWorkStateChannel();
}

document.addEventListener(
        "visibilitychange",
        () => {
            if (document.visibilityState
                    === "hidden") {

                flushPendingAutoSaves();
            }
        }
);

window.addEventListener(
        "pagehide",
        () => {
            flushPendingAutoSaves();
            clearPhotoObjectUrl();
        }
);

/* ========================================
 * 初期表示
 * ======================================== */

updateCounterButtons(
        stitchChannel
);

updateCounterButtons(
        rowChannel
);

updateMemoCharacterCount();

updateCompletionDateAvailability();