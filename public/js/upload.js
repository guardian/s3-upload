function init() {
    Dropzone.autoDiscover = false;

    var el = document.querySelector('.file-chooser');
    el.classList.add('dropzone');

    var previewTmpl = document.querySelector('.dz-template').innerHTML;

    var dropzoneEl = new Dropzone(el, {
        previewTemplate: previewTmpl,
        init: function () {
            this.on("success", function () {
                console.log('yay');
            });
        }
    });
}

document.addEventListener("DOMContentLoaded", init, false);
