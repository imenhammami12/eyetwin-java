// Validation temps réel pour création d'utilisateur (Admin)
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ Admin user validation script loaded');
    
    const usernameInput = document.querySelector('input[name="username"]');
    const emailInput = document.querySelector('input[name="email"]');
    
    if (usernameInput) {
        let usernameTimeout;
        usernameInput.addEventListener('input', function() {
            clearTimeout(usernameTimeout);
            
            const value = this.value.trim();
            if (value.length === 0) {
                clearFieldFeedback(this);
                return;
            }
            
            if (value.length < 3) {
                showFieldFeedback(this, 'neutral', 'Minimum 3 characters');
                return;
            }
            
            if (value.length > 50) {
                showFieldFeedback(this, 'error', 'Maximum 50 characters');
                return;
            }
            
            if (!/^[a-zA-Z0-9_-]+$/.test(value)) {
                showFieldFeedback(this, 'error', 'Only letters, numbers, - and _ allowed');
                return;
            }
            
            showFieldFeedback(this, 'loading', 'Checking availability...');
            
            usernameTimeout = setTimeout(() => {
                checkUsername(value, null);
            }, 500);
        });
    }
    
    if (emailInput) {
        let emailTimeout;
        emailInput.addEventListener('input', function() {
            clearTimeout(emailTimeout);
            
            const value = this.value.trim();
            if (value.length === 0) {
                clearFieldFeedback(this);
                return;
            }
            
            if (!isValidEmail(value)) {
                showFieldFeedback(this, 'error', 'Invalid email format');
                return;
            }
            
            showFieldFeedback(this, 'loading', 'Checking availability...');
            
            emailTimeout = setTimeout(() => {
                checkEmail(value, null);
            }, 500);
        });
    }
});

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function checkUsername(username, currentUserId) {
    fetch('/api/check-username', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
            username: username,
            currentUserId: currentUserId 
        })
    })
    .then(res => res.json())
    .then(data => {
        const input = document.querySelector('input[name="username"]');
        if (data.available) {
            showFieldFeedback(input, 'success', data.message);
        } else {
            showFieldFeedback(input, 'error', data.message);
        }
    })
    .catch(err => {
        console.error('❌ Error checking username:', err);
    });
}

function checkEmail(email, currentUserId) {
    fetch('/api/check-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
            email: email,
            currentUserId: currentUserId 
        })
    })
    .then(res => res.json())
    .then(data => {
        const input = document.querySelector('input[name="email"]');
        if (data.available) {
            showFieldFeedback(input, 'success', data.message);
        } else {
            showFieldFeedback(input, 'error', data.message);
        }
    })
    .catch(err => {
        console.error('❌ Error checking email:', err);
    });
}

function clearFieldFeedback(input) {
    input.classList.remove('is-valid', 'is-invalid', 'border-warning');
    
    let container = input.closest('.form-group-enhanced') || input.parentElement;
    let feedback = container.querySelector('.validation-feedback');
    if (feedback) {
        feedback.remove();
    }
}

function showFieldFeedback(input, type, message) {
    // Supprimer l'ancien feedback
    let container = input.closest('.form-group-enhanced') || input.parentElement;
    let feedback = container.querySelector('.validation-feedback');
    if (!feedback) {
        feedback = document.createElement('small');
        feedback.className = 'validation-feedback d-block mt-1';
        
        // Si l'input est dans un input-group, ajouter après le groupe
        if (input.parentElement.classList.contains('input-group')) {
            input.parentElement.insertAdjacentElement('afterend', feedback);
        } else {
            input.insertAdjacentElement('afterend', feedback);
        }
    }
    
    // Appliquer le style selon le type
    input.classList.remove('is-valid', 'is-invalid', 'border-warning');
    
    switch(type) {
        case 'success':
            input.classList.add('is-valid');
            feedback.className = 'validation-feedback d-block mt-1 text-success';
            feedback.innerHTML = `<i class="bi bi-check-circle me-1"></i>${message}`;
            break;
        case 'error':
            input.classList.add('is-invalid');
            feedback.className = 'validation-feedback d-block mt-1 text-danger';
            feedback.innerHTML = `<i class="bi bi-x-circle me-1"></i>${message}`;
            break;
        case 'loading':
            input.classList.add('border-warning');
            feedback.className = 'validation-feedback d-block mt-1 text-warning';
            feedback.innerHTML = `<i class="bi bi-hourglass-split me-1"></i>${message}`;
            break;
        case 'neutral':
            feedback.className = 'validation-feedback d-block mt-1 text-muted';
            feedback.textContent = message;
            break;
    }
}