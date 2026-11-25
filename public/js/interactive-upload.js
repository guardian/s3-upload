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

// Selected desks are submitted as repeated hidden `commissioningDesks` inputs
const desksSearchInput = document.getElementById("commissioningDesksSearch");
const desksResults = document.getElementById("commissioningDesksResults");
const desksSelected = document.getElementById("commissioningDesksSelected");
const selectedDesks = new Map();
let desksSearchTimeout;

function renderSelectedDesks() {
    desksSelected.replaceChildren(...[...selectedDesks].map(([id, name]) => {
        const item = document.createElement("li");
        item.textContent = name;

        const hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.name = "commissioningDesks";
        hidden.value = id;

        const remove = document.createElement("button");
        remove.type = "button";
        remove.textContent = "×";
        remove.setAttribute("aria-label", `Remove ${name}`);
        remove.addEventListener("click", () => {
            selectedDesks.delete(id);
            renderSelectedDesks();
        });

        item.append(" ", remove, hidden);
        return item;
    }));
}

function renderDeskResults(desks) {
    desksResults.replaceChildren(...desks.map(({ id, name }) => {
        const item = document.createElement("li");
        item.textContent = name;
        item.tabIndex = 0;
        const select = () => {
            selectedDesks.set(id, name);
            renderSelectedDesks();
            desksResults.replaceChildren();
            desksSearchInput.value = "";
        };
        item.addEventListener("click", select);
        item.addEventListener("keydown", (event) => {
            if (event.key === "Enter") select();
        });
        return item;
    }));
}

async function searchDesks(query) {
    if (!query) return renderDeskResults([]);
    try {
        const response = await fetch(`${desksSearchInput.dataset.searchUrl}?query=${encodeURIComponent(query)}`);
        renderDeskResults(response.ok ? await response.json() : []);
    } catch {
        renderDeskResults([]);
    }
}

desksSearchInput.addEventListener("input", () => {
    clearTimeout(desksSearchTimeout);
    desksSearchTimeout = setTimeout(() => searchDesks(desksSearchInput.value.trim()), 300);
});
// stop Enter in the search box submitting the upload form
desksSearchInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") event.preventDefault();
});
