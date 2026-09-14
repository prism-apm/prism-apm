/**
 * Fallback copy plan (create a hidden input to select and copy)
 */
export function fallbackCopyText(message: any, text: string): void {
    try {
        const textArea = document.createElement("textarea")
        textArea.value = text

        // Ensure the element is not visible but exists in the DOM
        textArea.style.position = "fixed"
        textArea.style.left = "-9999px"
        textArea.style.top = "0"
        document.body.appendChild(textArea)

        textArea.focus()
        textArea.select()

        // noinspection JSDeprecatedSymbols
        const successful: boolean = (document as any).execCommand('copy');
        if (successful) {
            message.success('Link copied to clipboard!')
        } else {
            message.error('Unable to copy automatically, please copy manually.')
        }

        document.body.removeChild(textArea)
    } catch (err) {
        console.error('Fallback copy failed', err)
        message.error('Copy failed.')
    }
}

/**
 * Compatible copy text helper function
 */
export function copyText(message: any, text: string): void {
    // Plan A: Use modern Clipboard API (requires HTTPS or localhost)
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => {
            message.success('Link copied to clipboard!')
        }).catch((err: any) => {
            console.error('Clipboard API failed, trying fallback...', err)
            fallbackCopyText(message, text)
        })
    } else {
        // Plan B: Fallback plan (compatible with old browsers or HTTP environment)
        fallbackCopyText(message, text)
    }
}
