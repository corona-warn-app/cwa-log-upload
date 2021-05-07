const imageFileEndings = [".png", ".jpg", ".jpeg", ".gif"];

$(document).ready(() => {
    $("#step-b").hide();
    $("#step-c").hide();
});

function download(logId) {
    zip.configure({
        workerScripts: {
            deflate: ["static/js/zip-worker.js"],
            inflate: ["static/js/zip-worker.js"]
        }
    })

    const httpReader = new zip.HttpReader("api/logs/" + logId, {
        preventHeadRequest: true
    });
    const zipReader = new zip.ZipReader(httpReader);
    zipReader.getEntries()
        .then(displayEntries)
        .catch(displayError);

    $("#step-a").hide();
    $("#step-b").show();
}

function displayEntries(entries) {
    $("#step-b").hide();
    $("#step-c").show();

    entries
        .filter(entry => !entry.directory)
        .forEach(entry => {
            const element = $("<li></li>");
            element.text(entry.filename);
            element.click(() => showFile(entry, element));

            $("#file-list").append(element);
        })
}

async function showFile(file, statusElement) {
    console.log(file);

    statusElement.text("Entpacke Datei...");

    if (isImage(file.filename)) { // Show data as image
        const base64 = await file.getData(new zip.Data64URIWriter("image/" + getFileEnding(file.filename)), {
            onprogress: (val, max) => {
                statusElement.text("Entpacke Datei... (" + Math.round((val / max * 100)) + "%)");
            }
        });

        const div = $("<div></div>");
        const divNative = div.get(0);
        div.height("100%");
        div.width("100%");
        div.css("background-image", "url('" + base64 + "')");
        div.css("background-size", "contain");
        div.css("background-repeat", "no-repeat");
        div.css("background-position", "center");

        jsPanel.create({
            contentSize: "500 500",
            position: "left-top 30 20",
            content: divNative,
            headerTitle: "<span style='font-variant: initial; font-family: \"Telegrotesk Next Regular\";'>Bilddatei</span>",
            theme: "#e20074",
        });

    } else { // Show data as text
        const text = await file.getData(new zip.TextWriter(), {
            onprogress: (val, max) => {
                statusElement.text("Entpacke Datei... (" + Math.round((val / max * 100)) + "%)");
            }
        });

        const div = $("<div></div>");
        const divNative = div.get(0);
        div.height("100%");
        div.width("100%");

        jsPanel.create({
            contentSize: "800 500",
            position: "left-top 30 20",
            content: divNative,
            headerTitle: "<span style='font-variant: initial; font-family: \"Telegrotesk Next Regular\";'>Textdatei</span>",
            theme: "#e20074",
            callback: (panel) => {
                console.log(panel);
                CodeMirror(divNative, {
                    value: text,
                    lineNumbers: true,
                    readOnly: true
                });
            }
        });
    }

    statusElement.text(file.filename);
}

function isImage(filename) {
    return imageFileEndings.some(fileEnding => filename.endsWith(fileEnding));
}

function getFileEnding(filename) {
    return filename.substring(filename.lastIndexOf(".") + 1)
}

function displayError(e) {
    console.error("Error: ", e);
}