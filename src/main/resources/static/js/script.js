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
    document.getElementById("figis").value = figiList
    render()
}

function render() {
    document.getElementById("figi-list").innerHTML = ""
    for (let v = 0; v < figiList.length; v++) {
        document.getElementById("figi-list").innerHTML += `<input type='text' value='${figiList[v]}' onchange='onChange(this, ${v})'><br>`
    }
}

function onSelectChanged(e) {
    const strategies = document.getElementsByClassName("st-form")
    const styleStrategy = document.getElementsByClassName("strategy")
    for (const strategy of strategies) {
        strategy.disabled = true
    }
    for (const strategy of styleStrategy) {
        strategy.style.display = "none"
    }
    const curStrategies = document.getElementsByClassName(`st-form-${e.value}`)
    for (const strategy of curStrategies) {
        strategy.disabled = false
    }
    document.getElementById(e.value).style.display = "block"
}

document.addEventListener("DOMContentLoaded", (_) => addFigiField())
