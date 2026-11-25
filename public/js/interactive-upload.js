const interactiveZipInput = document.getElementById("zipfile");
const interactiveProjectNameInput = document.getElementById("projectName");
const interactiveUploadSubmit = document.getElementById("interactive-upload-submit");

function updateInteractiveUploadButton() {
    interactiveUploadSubmit.disabled = interactiveProjectNameInput.value.trim() === "";
}

interactiveProjectNameInput.addEventListener("input", updateInteractiveUploadButton);
interactiveZipInput.addEventListener("change", () => {
    const zip = interactiveZipInput.files[0];
    if (zip) {
        interactiveProjectNameInput.value = zip.name.replace(/\.zip$/i, "").slice(0, interactiveProjectNameInput.maxLength);
    }
    updateInteractiveUploadButton();
});
updateInteractiveUploadButton();

const desksSelect = document.getElementById("commissioningDesks");

fetch(desksSelect.dataset.desksUrl)
    .then(response => response.ok ? response.json() : [])
    .then(desks => desks.forEach(({ id, name }) => desksSelect.add(new Option(name, id))))
    .catch(() => {});
