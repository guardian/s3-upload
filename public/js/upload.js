function init() {
    Dropzone.autoDiscover = false;

    var el = document.querySelector('.file-chooser');
    el.classList.add('dropzone');

    var dropzoneEl = new Dropzone(el, {
        init: function () {
            this.on("success", function () {
                console.log('yay');
            });
        }
    });
}

document.addEventListener("DOMContentLoaded", init, false);