document.addEventListener('DOMContentLoaded', function() {
    console.log('Registration validation script loaded');
    
    // Get all input elements by their generated names from Symfony
    const usernameInput = document.querySelector('input[name="registration_form[username]"]');
    const emailInput = document.querySelector('input[name="registration_form[email]"]');
    const fullnameInput = document.querySelector('input[name="registration_form[fullName]"]');
    const passwordInput = document.querySelector('input[name="registration_form[plainPassword][first]"]');
    const passwordConfirm = document.querySelector('input[name="registration_form[plainPassword][second]"]');
    const agreeTerms = document.querySelector('input[name="registration_form[agreeTerms]"]');
    
    // Get feedback elements
    const usernameFeedback = document.getElementById('username-feedback');
    const usernameMessage = document.getElementById('username-message');
    const usernameIcon = document.getElementById('username-icon');
    const usernameSpinner = document.getElementById('username-spinner');
    
    const emailFeedback = document.getElementById('email-feedback');
    const emailMessage = document.getElementById('email-message');
    const emailIcon = document.getElementById('email-icon');
    const emailSpinner = document.getElementById('email-spinner');
    
    const fullnameFeedback = document.getElementById('fullname-feedback');
    const fullnameMessage = document.getElementById('fullname-message');
    const fullnameIcon = document.getElementById('fullname-icon');
    
    const passwordIcon = document.getElementById('password-icon');
    
    const confirmFeedback = document.getElementById('confirm-feedback');
    const confirmMessage = document.getElementById('confirm-message');
    const confirmIcon = document.getElementById('confirm-icon');
    
    const termsFeedback = document.getElementById('terms-feedback');
    const termsMessage = document.getElementById('terms-message');
    
    // Password requirements
    const reqLength = document.getElementById('req-length');
    const reqUppercase = document.getElementById('req-uppercase');
    const reqLowercase = document.getElementById('req-lowercase');
    const reqNumber = document.getElementById('req-number');
    const strengthFill = document.getElementById('strength-fill');
    const strengthText = document.getElementById('strength-text');
    
    // Validation states
    let validationState = {
        username: false,
        email: false,
        fullname: false,
        password: false,
        confirm: false,
        terms: false
    };
    
    // Debounce timers
    let usernameTimer;
    let emailTimer;
    
    // ============================================
    // USERNAME VALIDATION (with server check)
    // ============================================
    if (usernameInput) {
        usernameInput.addEventListener('input', function() {
            const username = this.value.trim();
            
            // Clear previous timer
            clearTimeout(usernameTimer);
            
            if (username.length === 0) {
                showFeedback(usernameFeedback, usernameMessage, usernameIcon, usernameInput, '', '', false);
                hideSpinner(usernameSpinner);
                validationState.username = false;
                updateSubmitButton();
                return;
            }
            
            // Client-side validation first
            if (username.length < 3) {
                showFeedback(usernameFeedback, usernameMessage, usernameIcon, usernameInput, 
                    'Username must be at least 3 characters long', 'invalid', true);
                hideSpinner(usernameSpinner);
                validationState.username = false;
                updateSubmitButton();
                return;
            }
            
            if (!/^[a-zA-Z0-9_]+$/.test(username)) {
                showFeedback(usernameFeedback, usernameMessage, usernameIcon, usernameInput, 
                    'Username can only contain letters, numbers, and underscores', 'invalid', true);
                hideSpinner(usernameSpinner);
                validationState.username = false;
                updateSubmitButton();
                return;
            }
            
            // Show checking state
            showSpinner(usernameSpinner);
            hideIcon(usernameIcon);
            showFeedback(usernameFeedback, usernameMessage, null, usernameInput, 
                'Checking availability...', 'checking', true);
            validationState.username = false;
            updateSubmitButton();
            
            // Debounce server check (wait 500ms after user stops typing)
            usernameTimer = setTimeout(() => {
                checkUsernameAvailability(username);
            }, 500);
        });
    }
    
    function checkUsernameAvailability(username) {
        fetch('/api/check-username', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ username: username })
        })
        .then(response => response.json())
        .then(data => {
            hideSpinner(usernameSpinner);
            
            if (data.available) {
                showFeedback(usernameFeedback, usernameMessage, usernameIcon, usernameInput, 
                    'Username is available!', 'valid', true);
                validationState.username = true;
            } else {
                showFeedback(usernameFeedback, usernameMessage, usernameIcon, usernameInput, 
                    'This username is already taken', 'invalid', true);
                validationState.username = false;
            }
            updateSubmitButton();
        })
        .catch(error => {
            console.error('Error checking username:', error);
            hideSpinner(usernameSpinner);
            showFeedback(usernameFeedback, usernameMessage, usernameIcon, usernameInput, 
                'Unable to verify username. Please try again.', 'invalid', true);
            validationState.username = false;
            updateSubmitButton();
        });
    }
    
    // ============================================
    // EMAIL VALIDATION (with server check)
    // ============================================
    if (emailInput) {
        emailInput.addEventListener('input', function() {
            const email = this.value.trim();
            
            // Clear previous timer
            clearTimeout(emailTimer);
            
            if (email.length === 0) {
                showFeedback(emailFeedback, emailMessage, emailIcon, emailInput, '', '', false);
                hideSpinner(emailSpinner);
                validationState.email = false;
                updateSubmitButton();
                return;
            }
            
            // Client-side validation first
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                showFeedback(emailFeedback, emailMessage, emailIcon, emailInput, 
                    'Please provide a valid email address', 'invalid', true);
                hideSpinner(emailSpinner);
                validationState.email = false;
                updateSubmitButton();
                return;
            }
            
            // Show checking state
            showSpinner(emailSpinner);
            hideIcon(emailIcon);
            showFeedback(emailFeedback, emailMessage, null, emailInput, 
                'Checking availability...', 'checking', true);
            validationState.email = false;
            updateSubmitButton();
            
            // Debounce server check (wait 500ms after user stops typing)
            emailTimer = setTimeout(() => {
                checkEmailAvailability(email);
            }, 500);
        });
    }
    
    function checkEmailAvailability(email) {
        fetch('/api/check-email', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email: email })
        })
        .then(response => response.json())
        .then(data => {
            hideSpinner(emailSpinner);
            
            if (data.available) {
                showFeedback(emailFeedback, emailMessage, emailIcon, emailInput, 
                    'Email is available!', 'valid', true);
                validationState.email = true;
            } else {
                showFeedback(emailFeedback, emailMessage, emailIcon, emailInput, 
                    'This email is already registered', 'invalid', true);
                validationState.email = false;
            }
            updateSubmitButton();
        })
        .catch(error => {
            console.error('Error checking email:', error);
            hideSpinner(emailSpinner);
            showFeedback(emailFeedback, emailMessage, emailIcon, emailInput, 
                'Unable to verify email. Please try again.', 'invalid', true);
            validationState.email = false;
            updateSubmitButton();
        });
    }
    
    // ============================================
    // FULL NAME VALIDATION
    // ============================================
    if (fullnameInput) {
        fullnameInput.addEventListener('input', function() {
            const fullname = this.value.trim();
            
            if (fullname.length === 0) {
                showFeedback(fullnameFeedback, fullnameMessage, fullnameIcon, fullnameInput, '', '', false);
                validationState.fullname = false;
            } else if (fullname.length < 2) {
                showFeedback(fullnameFeedback, fullnameMessage, fullnameIcon, fullnameInput, 
                    'Full name must be at least 2 characters long', 'invalid', true);
                validationState.fullname = false;
            } else {
                showFeedback(fullnameFeedback, fullnameMessage, fullnameIcon, fullnameInput, 
                    'Full name is valid', 'valid', true);
                validationState.fullname = true;
            }
            updateSubmitButton();
        });
    }
    
    // ============================================
    // PASSWORD VALIDATION
    // ============================================
    if (passwordInput) {
        passwordInput.addEventListener('input', function() {
            const password = this.value;
            let validCount = 0;
            
            // Check length
            if (password.length >= 6) {
                reqLength.classList.add('valid');
                reqLength.classList.remove('invalid');
                reqLength.querySelector('i').className = 'fas fa-check-circle';
                validCount++;
            } else {
                reqLength.classList.remove('valid');
                reqLength.classList.add('invalid');
                reqLength.querySelector('i').className = 'fas fa-circle';
            }
            
            // Check uppercase
            if (/[A-Z]/.test(password)) {
                reqUppercase.classList.add('valid');
                reqUppercase.classList.remove('invalid');
                reqUppercase.querySelector('i').className = 'fas fa-check-circle';
                validCount++;
            } else {
                reqUppercase.classList.remove('valid');
                reqUppercase.classList.add('invalid');
                reqUppercase.querySelector('i').className = 'fas fa-circle';
            }
            
            // Check lowercase
            if (/[a-z]/.test(password)) {
                reqLowercase.classList.add('valid');
                reqLowercase.classList.remove('invalid');
                reqLowercase.querySelector('i').className = 'fas fa-check-circle';
                validCount++;
            } else {
                reqLowercase.classList.remove('valid');
                reqLowercase.classList.add('invalid');
                reqLowercase.querySelector('i').className = 'fas fa-circle';
            }
            
            // Check number
            if (/\d/.test(password)) {
                reqNumber.classList.add('valid');
                reqNumber.classList.remove('invalid');
                reqNumber.querySelector('i').className = 'fas fa-check-circle';
                validCount++;
            } else {
                reqNumber.classList.remove('valid');
                reqNumber.classList.add('invalid');
                reqNumber.querySelector('i').className = 'fas fa-circle';
            }
            
            // Update strength bar
            strengthFill.className = 'password-strength-fill';
            if (password.length === 0) {
                strengthText.textContent = 'Enter password to see strength';
                strengthText.style.color = '';
                strengthFill.style.width = '0';
                passwordIcon.classList.remove('show');
                passwordInput.classList.remove('is-valid-custom', 'is-invalid-custom');
                validationState.password = false;
            } else if (validCount <= 2) {
                strengthFill.classList.add('strength-weak');
                strengthText.textContent = 'Weak password';
                strengthText.style.color = '#dc3545';
                passwordIcon.className = 'live-validation-icon fas fa-times-circle invalid show';
                passwordInput.classList.add('is-invalid-custom');
                passwordInput.classList.remove('is-valid-custom');
                validationState.password = false;
            } else if (validCount === 3) {
                strengthFill.classList.add('strength-medium');
                strengthText.textContent = 'Medium password - add one more requirement';
                strengthText.style.color = '#ffc107';
                passwordIcon.className = 'live-validation-icon fas fa-exclamation-circle invalid show';
                passwordInput.classList.add('is-invalid-custom');
                passwordInput.classList.remove('is-valid-custom');
                validationState.password = false;
            } else if (validCount === 4) {
                strengthFill.classList.add('strength-strong');
                strengthText.textContent = 'Strong password!';
                strengthText.style.color = '#28a745';
                passwordIcon.className = 'live-validation-icon fas fa-check-circle valid show';
                passwordInput.classList.add('is-valid-custom');
                passwordInput.classList.remove('is-invalid-custom');
                validationState.password = true;
            }
            
            checkPasswordMatch();
            updateSubmitButton();
        });
    }
    
    // ============================================
    // PASSWORD CONFIRMATION
    // ============================================
    if (passwordConfirm) {
        passwordConfirm.addEventListener('input', checkPasswordMatch);
    }
    
    function checkPasswordMatch() {
        if (!passwordInput || !passwordConfirm) return;
        
        const password = passwordInput.value;
        const confirm = passwordConfirm.value;
        
        if (confirm.length === 0) {
            showFeedback(confirmFeedback, confirmMessage, confirmIcon, passwordConfirm, '', '', false);
            validationState.confirm = false;
        } else if (password !== confirm) {
            showFeedback(confirmFeedback, confirmMessage, confirmIcon, passwordConfirm, 
                'Passwords do not match', 'invalid', true);
            validationState.confirm = false;
        } else {
            showFeedback(confirmFeedback, confirmMessage, confirmIcon, passwordConfirm, 
                'Passwords match!', 'valid', true);
            validationState.confirm = true;
        }
        updateSubmitButton();
    }
    
    // ============================================
    // TERMS VALIDATION
    // ============================================
    if (agreeTerms) {
        agreeTerms.addEventListener('change', function() {
            if (this.checked) {
                showFeedback(termsFeedback, termsMessage, null, null, 
                    'Terms accepted', 'valid', true);
                validationState.terms = true;
            } else {
                showFeedback(termsFeedback, termsMessage, null, null, 
                    'You must accept the terms and conditions', 'invalid', true);
                validationState.terms = false;
            }
            updateSubmitButton();
        });
    }
    
    // ============================================
    // HELPER FUNCTIONS
    // ============================================
    function showFeedback(feedbackEl, messageEl, iconEl, inputEl, message, type, show) {
        if (!feedbackEl || !messageEl) return;
        
        if (show) {
            feedbackEl.classList.add('show');
            feedbackEl.classList.remove('valid', 'invalid', 'checking');
            feedbackEl.classList.add(type);
            messageEl.textContent = message;
            
            const feedbackIcon = feedbackEl.querySelector('i');
            if (feedbackIcon) {
                if (type === 'valid') {
                    feedbackIcon.className = 'fas fa-check-circle';
                } else if (type === 'checking') {
                    feedbackIcon.className = 'fas fa-spinner fa-spin';
                } else {
                    feedbackIcon.className = 'fas fa-exclamation-circle';
                }
            }
            
            if (iconEl) {
                iconEl.classList.add('show');
                iconEl.classList.remove('valid', 'invalid');
                iconEl.classList.add(type);
                if (type === 'valid') {
                    iconEl.className = 'live-validation-icon fas fa-check-circle valid show';
                } else {
                    iconEl.className = 'live-validation-icon fas fa-times-circle invalid show';
                }
            }
            
            if (inputEl) {
                inputEl.classList.remove('is-valid-custom', 'is-invalid-custom', 'is-checking-custom');
                if (type === 'valid') {
                    inputEl.classList.add('is-valid-custom');
                } else if (type === 'checking') {
                    inputEl.classList.add('is-checking-custom');
                } else {
                    inputEl.classList.add('is-invalid-custom');
                }
            }
        } else {
            feedbackEl.classList.remove('show');
            if (iconEl) {
                iconEl.classList.remove('show');
            }
            if (inputEl) {
                inputEl.classList.remove('is-valid-custom', 'is-invalid-custom', 'is-checking-custom');
            }
        }
    }
    
    function showSpinner(spinnerEl) {
        if (spinnerEl) {
            spinnerEl.classList.add('show');
        }
    }
    
    function hideSpinner(spinnerEl) {
        if (spinnerEl) {
            spinnerEl.classList.remove('show');
        }
    }
    
    function hideIcon(iconEl) {
        if (iconEl) {
            iconEl.classList.remove('show');
        }
    }
    
    function updateSubmitButton() {
        const submitBtn = document.getElementById('submit-btn');
        if (!submitBtn) return;
        
        const allValid = Object.values(validationState).every(state => state === true);
        
        if (allValid) {
            submitBtn.disabled = false;
            submitBtn.style.opacity = '1';
            submitBtn.style.cursor = 'pointer';
        } else {
            submitBtn.disabled = true;
            submitBtn.style.opacity = '0.6';
            submitBtn.style.cursor = 'not-allowed';
        }
    }
    
    // Initial button state
    updateSubmitButton();
});