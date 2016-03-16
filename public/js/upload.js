document.addEventListener("DOMContentLoaded", init, false);

function init() {
    document.querySelector('#fileChooser').addEventListener('change', handleFileSelect, false);
}

function handleFileSelect(event) {
    if(!event.target.files) return;
    const files = event.target.files;
    const selected = document.querySelector("#selectedFiles ul");
    selected.innerHTML = "";
    for(var i=0; i<files.length; i++) {
        const file = files[i];
        selected.innerHTML += "<li>" + file.name + "<br/>" + "</li>";
    }
}
