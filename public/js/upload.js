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
        newClassName = document.querySelector("#png-error").className.replace("hidden", "");
        document.querySelector("#png-error").className = newClassName;
      }
      selected.innerHTML += "<li>" + file.name + "<br/>" + "</li>";

    }
}
