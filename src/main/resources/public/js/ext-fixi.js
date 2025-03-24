// add a 'none' swap
document.addEventListener("fx:config", (evt) => {
    if (evt.detail.cfg.swap === "none")
        evt.detail.cfg.swap = ()=>{}
});