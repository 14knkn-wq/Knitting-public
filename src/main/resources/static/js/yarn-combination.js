const yarnCombinationForm =
        document.querySelector(".yarn-combination-form");

if (yarnCombinationForm) {

    const modeInputs =
            yarnCombinationForm.querySelectorAll(
                    'input[name="mode"]');

    const doubleStrandInputs =
            document.getElementById("double-strand-inputs");

    const combinationInputs =
            document.getElementById("combination-inputs");

    const calculateButton =
            document.getElementById(
                    "yarn-combination-calculate-button");

    const result =
            document.getElementById(
                    "yarn-combination-result");

    const recalculateMessage =
            document.getElementById(
                    "yarn-combination-recalculate-message");

    const inputNamesByMode = {
        DOUBLE_STRAND: [
            "doubleStrandWeight",
            "doubleStrandLength"
        ],
        COMBINATION: [
            "yarn1Weight",
            "yarn1Length",
            "yarn2Weight",
            "yarn2Length"
        ]
    };

    const getSelectedMode = () => {

        const selected =
                yarnCombinationForm.querySelector(
                        'input[name="mode"]:checked');

        return selected ? selected.value : null;
    };

    const getInputsForMode = (mode) => {

        const inputNames = inputNamesByMode[mode];

        if (!inputNames) {
            return [];
        }

        return inputNames
                .map((name) =>
                        yarnCombinationForm.querySelector(
                                `[name="${name}"]`
                        ))
                .filter((input) => input !== null);
    };

    const isValidNumberInput = (input) => {

        const value = input.value.trim();

        if (!/^\d+(?:\.\d{1,2})?$/.test(value)) {
            return false;
        }

        const numberValue = Number(value);

        return Number.isFinite(numberValue)
                && numberValue >= 0.01
                && numberValue <= 99999.99;
    };

    const updateInputPanels = () => {

        const mode = getSelectedMode();

        doubleStrandInputs.hidden =
                mode !== "DOUBLE_STRAND";

        combinationInputs.hidden =
                mode !== "COMBINATION";
    };

    const updateCalculateButton = () => {

        const mode = getSelectedMode();
        const requiredInputs = getInputsForMode(mode);

        calculateButton.disabled =
                requiredInputs.length === 0
                || !requiredInputs.every(isValidNumberInput);
    };

    /*
     * 計算結果が表示されている場合だけ、
     * その結果を計算した時点のモードと入力値を保持する。
     */
    const calculatedMode =
            result ? getSelectedMode() : null;

    const calculatedValues =
            calculatedMode
                    ? getInputsForMode(calculatedMode)
                            .map((input) => input.value)
                    : [];

    const calculationInputsAreUnchanged = () => {

        if (!calculatedMode) {
            return true;
        }

        const currentInputs =
                getInputsForMode(calculatedMode);

        return currentInputs.every(
                (input, index) =>
                        input.value === calculatedValues[index]
        );
    };

    const updateResultVisibility = () => {

        if (!result || !calculatedMode) {
            return;
        }

        const selectedMode = getSelectedMode();

        if (selectedMode !== calculatedMode) {
            result.hidden = true;
            return;
        }

        result.hidden = false;

        if (recalculateMessage) {
            recalculateMessage.hidden =
                    calculationInputsAreUnchanged();
        }
    };

    modeInputs.forEach((modeInput) => {

        modeInput.addEventListener("change", () => {

            updateInputPanels();
            updateCalculateButton();
            updateResultVisibility();
        });
    });

    Object.values(inputNamesByMode)
            .flat()
            .forEach((inputName) => {

                const input =
                        yarnCombinationForm.querySelector(
                                `[name="${inputName}"]`);

                if (!input) {
                    return;
                }

                input.addEventListener("input", () => {

                    updateCalculateButton();

                    if (!result || !calculatedMode) {
                        return;
                    }

                    const calculatedInputNames =
                            inputNamesByMode[calculatedMode];

                    if (!calculatedInputNames.includes(inputName)) {
                        return;
                    }

                    updateResultVisibility();
                });
            });

    updateInputPanels();
    updateCalculateButton();
    updateResultVisibility();
}