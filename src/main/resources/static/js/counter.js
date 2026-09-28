const stitchCountInput = document.getElementById("stitch-count");
const stitchDecrementButton = document.getElementById("stitch-decrement-button");
const stitchIncrementButton = document.getElementById("stitch-increment-button");
const stitchResetButton = document.getElementById("stitch-reset-button");
const stitchError = document.getElementById("stitch-error");

const rowCountInput = document.getElementById("row-count");
const rowDecrementButton = document.getElementById("row-decrement-button");
const rowIncrementButton = document.getElementById("row-increment-button");
const rowResetButton = document.getElementById("row-reset-button");
const rowError = document.getElementById("row-error");

const resetAllButton = document.getElementById("reset-all-button");
const storageWarning = document.getElementById("storage-warning");

const STITCH_COUNT_STORAGE_KEY = "knitting.simpleCounter.stitchCount";
const ROW_COUNT_STORAGE_KEY = "knitting.simpleCounter.rowCount";
const STORAGE_TEST_KEY = "knitting.simpleCounter.storageTest";

let isStorageAvailable = false;

let stitchCount = 0;
let rowCount = 0;

try {
    localStorage.setItem(STORAGE_TEST_KEY, "test");

    if (localStorage.getItem(STORAGE_TEST_KEY) === "test") {
        isStorageAvailable = true;
    }

    localStorage.removeItem(STORAGE_TEST_KEY);
} catch (error) {
    isStorageAvailable = false;
}
if (!isStorageAvailable) {
    storageWarning.hidden = false;
}

stitchCount = loadCount(STITCH_COUNT_STORAGE_KEY);
rowCount = loadCount(ROW_COUNT_STORAGE_KEY);

stitchCountInput.value = String(stitchCount);
rowCountInput.value = String(rowCount);

updateButtonStates(
    stitchCount,
    stitchDecrementButton,
    stitchIncrementButton
);

updateButtonStates(
    rowCount,
    rowDecrementButton,
    rowIncrementButton
);

function isValidStoredCount(value) {
    return /^\d+$/.test(value)
            && Number(value) <= 9999;
}

function disableStorage() {
    isStorageAvailable = false;
    storageWarning.hidden = false;
}

function saveCount(storageKey, value) {
    if (!isStorageAvailable) {
        return;
    }

    try {
        localStorage.setItem(storageKey, String(value));
    } catch (error) {
        disableStorage();
    }
}

function loadCount(storageKey) {
    if (!isStorageAvailable) {
        return 0;
    }

    const storedValue = localStorage.getItem(storageKey);

    if (storedValue === null) {
        return 0;
    }

    if (isValidStoredCount(storedValue)) {
        return Number(storedValue);
    }

    saveCount(storageKey, 0);
    return 0;
}

function updateButtonStates(value, decrementButton, incrementButton) {
    decrementButton.disabled = value <= 0;
    incrementButton.disabled = value >= 9999;
}

stitchDecrementButton.addEventListener("click", () => {
    if (stitchCount <= 0) {
        return;
    }

    stitchCount--;
    stitchCountInput.value = String(stitchCount);

    saveCount(STITCH_COUNT_STORAGE_KEY, stitchCount);

    updateButtonStates(
        stitchCount,
        stitchDecrementButton,
        stitchIncrementButton
    );
});

stitchIncrementButton.addEventListener("click", () => {
    if (stitchCount >= 9999) {
        return;
    }

    stitchCount++;
    stitchCountInput.value = String(stitchCount);

    saveCount(STITCH_COUNT_STORAGE_KEY, stitchCount);

    updateButtonStates(
        stitchCount,
        stitchDecrementButton,
        stitchIncrementButton
    );
});

rowDecrementButton.addEventListener("click", () => {
    if (rowCount <= 0) {
        return;
    }

    rowCount--;
    rowCountInput.value = String(rowCount);

    saveCount(ROW_COUNT_STORAGE_KEY, rowCount);

    updateButtonStates(
        rowCount,
        rowDecrementButton,
        rowIncrementButton
    );
});

rowIncrementButton.addEventListener("click", () => {
    if (rowCount >= 9999) {
        return;
    }

    rowCount++;
    rowCountInput.value = String(rowCount);

    saveCount(ROW_COUNT_STORAGE_KEY, rowCount);

    updateButtonStates(
        rowCount,
        rowDecrementButton,
        rowIncrementButton
    );
});

function convertFullWidthDigits(value) {
    return value.replace(/[０-９]/g, (character) =>
        String.fromCharCode(character.charCodeAt(0) - 0xFEE0)
    );
}

function isValidInputCount(value) {
    return /^\d+$/.test(value)
            && Number(value) <= 9999;
}

stitchCountInput.addEventListener("input", () => {
    const convertedValue = convertFullWidthDigits(stitchCountInput.value);

    if (stitchCountInput.value !== convertedValue) {
        stitchCountInput.value = convertedValue;
    }

    if (convertedValue === "") {
        stitchError.hidden = true;
        stitchDecrementButton.disabled = true;
        stitchIncrementButton.disabled = true;
        return;
    }

    if (!isValidInputCount(convertedValue)) {
        stitchError.textContent = "0～9999の整数で入力してください。";
        stitchError.hidden = false;
        stitchDecrementButton.disabled = true;
        stitchIncrementButton.disabled = true;
        return;
    }

    stitchError.hidden = true;

    stitchCount = Number(convertedValue);
    saveCount(STITCH_COUNT_STORAGE_KEY, stitchCount);

    updateButtonStates(
        stitchCount,
        stitchDecrementButton,
        stitchIncrementButton
    );
});

stitchCountInput.addEventListener("blur", () => {
    if (
        stitchCountInput.value === ""
        || !isValidInputCount(stitchCountInput.value)
    ) {
        stitchCountInput.value = String(stitchCount);
        stitchError.hidden = true;

        updateButtonStates(
            stitchCount,
            stitchDecrementButton,
            stitchIncrementButton
        );
    }
});

rowCountInput.addEventListener("input", () => {
    const convertedValue = convertFullWidthDigits(rowCountInput.value);

    if (rowCountInput.value !== convertedValue) {
        rowCountInput.value = convertedValue;
    }

    if (convertedValue === "") {
        rowError.hidden = true;
        rowDecrementButton.disabled = true;
        rowIncrementButton.disabled = true;
        return;
    }

    if (!isValidInputCount(convertedValue)) {
        rowError.textContent = "0～9999の整数で入力してください。";
        rowError.hidden = false;
        rowDecrementButton.disabled = true;
        rowIncrementButton.disabled = true;
        return;
    }

    rowError.hidden = true;

    rowCount = Number(convertedValue);
    saveCount(ROW_COUNT_STORAGE_KEY, rowCount);

    updateButtonStates(
        rowCount,
        rowDecrementButton,
        rowIncrementButton
    );
});

rowCountInput.addEventListener("blur", () => {
    if (
        rowCountInput.value === ""
        || !isValidInputCount(rowCountInput.value)
    ) {
        rowCountInput.value = String(rowCount);
        rowError.hidden = true;

        updateButtonStates(
            rowCount,
            rowDecrementButton,
            rowIncrementButton
        );
    }
});

stitchResetButton.addEventListener("click", () => {
    stitchCount = 0;
    stitchCountInput.value = "0";
    stitchError.hidden = true;

    saveCount(STITCH_COUNT_STORAGE_KEY, stitchCount);

    updateButtonStates(
        stitchCount,
        stitchDecrementButton,
        stitchIncrementButton
    );
});

rowResetButton.addEventListener("click", () => {
    rowCount = 0;
    rowCountInput.value = "0";
    rowError.hidden = true;

    saveCount(ROW_COUNT_STORAGE_KEY, rowCount);

    updateButtonStates(
        rowCount,
        rowDecrementButton,
        rowIncrementButton
    );
});

resetAllButton.addEventListener("click", () => {
    const confirmed = window.confirm(
        "目数と段数を0に戻します。よろしいですか？"
    );

    if (!confirmed) {
        return;
    }

    stitchCount = 0;
    rowCount = 0;

    stitchCountInput.value = "0";
    rowCountInput.value = "0";

    stitchError.hidden = true;
    rowError.hidden = true;

    saveCount(STITCH_COUNT_STORAGE_KEY, stitchCount);
    saveCount(ROW_COUNT_STORAGE_KEY, rowCount);

    updateButtonStates(
        stitchCount,
        stitchDecrementButton,
        stitchIncrementButton
    );

    updateButtonStates(
        rowCount,
        rowDecrementButton,
        rowIncrementButton
    );
});

