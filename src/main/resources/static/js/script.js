function onCopy(token) {
    navigator.clipboard.writeText(token);

    const tooltip = document.getElementById("copy-tip");
    tooltip.innerHTML = "Токен скопирован";
}

function onCopyOut() {
    const tooltip = document.getElementById("copy-tip");
    tooltip.innerHTML = "Копировать в буфер обмена";
}

const figiList = []

function addFigiField() {
    figiList.push("")
    render()
}

function onChange(e, index) {
    figiList[index] = e.value
    document.getElementById("hidden-figi").value = figiList
    render()
}

function render() {
    document.getElementById("figi-list").innerHTML = ""
    for (let v = 0; v < figiList.length; v++) {
        document.getElementById("figi-list").innerHTML += `<input type='text' value='${figiList[v]}' onchange='onChange(this, ${v})'>`
    }
}

document.addEventListener("DOMContentLoaded", (_) => addFigiField())
