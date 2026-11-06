/* global JSZip */
"use strict";

const zipInput = document.getElementById("zipfile");
const projectInput = document.getElementById("projectName");
const uploadStatus = document.getElementById("interactive-status");
const submit = document.getElementById("interactive-submit");
const fileList = document.getElementById("interactive-files");
let selection = 0;
let ready = false;

async function inspectZip() {
    const current = ++selection;
    const file = zipInput.files[0];
    ready = false;
    fileList.replaceChildren();
    zipInput.setCustomValidity("");
    uploadStatus.textContent = "";
    submit.disabled = true;
    if (!file) return;
    if (!/\.zip$/i.test(file.name) || file.size > 50 * 1024 * 1024) {
        zipInput.setCustomValidity("Choose a ZIP file smaller than 50 MB.");
        zipInput.reportValidity();
        return;
    }
    if (!projectInput.value) projectInput.value = file.name.replace(/\.zip$/i, "").slice(0, 80);
    uploadStatus.textContent = "Checking ZIP contents…";
    try {
        const zip = await JSZip.loadAsync(file);
        if (current !== selection) return;
        const files = Object.values(zip.files).filter(entry => !entry.dir &&
            !entry.name.split("/").some(part => part === "__MACOSX" || part === ".DS_Store" || part.startsWith("._")));
        if (Object.keys(zip.files).length > 2000) throw new Error("ZIP must contain at most 2,000 entries.");
        if (!files.some(entry => entry.name === "index.html")) {
            throw new Error("ZIP must contain index.html at its root. Zip the folder's contents, rather than the folder itself.");
        }
        files.forEach(entry => {
            const item = document.createElement("li");
            item.textContent = entry.name;
            fileList.appendChild(item);
        });
        uploadStatus.textContent = `${files.length} files ready to upload.`;
        ready = true;
        submit.disabled = false;
    } catch (error) {
        if (current !== selection) return;
        uploadStatus.textContent = error.message || "Unable to read ZIP.";
        zipInput.setCustomValidity("Choose a valid ZIP with index.html at its root.");
    }
}

zipInput.addEventListener("change", inspectZip);
const dropZone = document.getElementById("interactive-drop");
dropZone.addEventListener("dragover", event => event.preventDefault());
dropZone.addEventListener("drop", event => {
    event.preventDefault();
    zipInput.files = event.dataTransfer.files;
    if (zipInput.files.length !== 1) {
        ready = false;
        submit.disabled = true;
        uploadStatus.textContent = "Choose one ZIP file.";
        return;
    }
    inspectZip();
});
document.getElementById("interactive-form").addEventListener("submit", event => {
    if (!ready) {
        event.preventDefault();
        return;
    }
    submit.disabled = true;
    uploadStatus.textContent = "Uploading and publishing your interactive… Please keep this page open.";
});
// Browsers can restore the previous form when navigating back from the result.
window.addEventListener("pageshow", () => {
    if (zipInput.files.length) inspectZip();
});
