document.querySelectorAll(".copy-vanity-url").forEach((button) => {
    const link = button.previousElementSibling;
    const status = button.nextElementSibling;

    button.addEventListener("click", async () => {
        status.textContent = "";
        try {
            await navigator.clipboard.writeText(link.textContent);
            status.textContent = "Copied!";
        } catch {
            status.textContent = "Could not copy the URL.";
        }
    });
});
