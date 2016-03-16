document.addEventListener("DOMContentLoaded", init, false);

function init() {
    document.querySelector('#fileChooser').addEventListener('change', handleFileSelect, false);
}

function handleFileSelect(event) {
    if(!event.target.files) return;
    const files = event.target.files;
    const selectedList = document.querySelector('#selectedFiles ul');

    Array.from(files).map(function (file) {
        var li = document.createElement('li');
        li.appendChild(document.createTextNode(file.name));
        selectedList.appendChild(li);
    });
}
