document.addEventListener('DOMContentLoaded', () => {
    // Flash Sale Countdown Logic
    const countdownElements = document.querySelectorAll('.countdown-box');
    if (countdownElements.length > 0) {
        // Set a deadline (e.g., 2 hours from now)
        let timeInSeconds = 2 * 60 * 60;

        const updateCountdown = () => {
            const hours = Math.floor(timeInSeconds / 3600);
            const minutes = Math.floor((timeInSeconds % 3600) / 60);
            const seconds = timeInSeconds % 60;

            if (countdownElements[0]) countdownElements[0].textContent = String(hours).padStart(2, '0');
            if (countdownElements[1]) countdownElements[1].textContent = String(minutes).padStart(2, '0');
            if (countdownElements[2]) countdownElements[2].textContent = String(seconds).padStart(2, '0');

            if (timeInSeconds > 0) {
                timeInSeconds--;
            }
        };

        setInterval(updateCountdown, 1000);
        updateCountdown();
    }

    // Slider Logic Removed (Images are now static split layout)
});
