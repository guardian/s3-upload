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
      var file = files[i];
      var ext = file.name.match(/\.(.+)$/)[1];
      if(ext == "png"){
        document.querySelector("#png-error").classList.remove("upload-error--hidden");
      }

      var li = document.createElement('li')
      li.appendChild(document.createTextNode(file.name));
      selected.appendChild(li);

    }
}
