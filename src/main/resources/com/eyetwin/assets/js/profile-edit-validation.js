// Validation temps réel pour édition de profil utilisateur
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ Profile edit validation script loaded');
    
    // Essayer différents sélecteurs pour trouver l'input email
    const emailInput = document.querySelector('input[type="email"]') || 
                      document.querySelector('input[name*="email"]') ||
                      document.querySelector('input[name*="[email]"]');
    
    console.log('📧 Email input found:', emailInput);
    
    // Récupérer l'ID utilisateur depuis window
    const currentUserId = window.currentUserId;
    console.log('👤 Current user ID:', currentUserId);
    
    if (emailInput && currentUserId) {
        const originalEmail = emailInput.value.trim();
        console.log('📨 Original email:', originalEmail);
        
        let timeout;
        emailInput.addEventListener('input', function() {
            clearTimeout(timeout);
            
            const value = this.value.trim();
            
            // Si c'est la valeur originale, pas besoin de vérifier
            if (value === originalEmail) {
                showFieldFeedback(this, 'neutral', 'Your current email');
                return;
            }
            
            if (value.length === 0) {
                clearFieldFeedback(this);
                return;
            }
            
            if (!isValidEmail(value)) {
                showFieldFeedback(this, 'error', 'Invalid email format');
                return;
            }
            
            showFieldFeedback(this, 'loading', 'Checking availability...');
            
            timeout = setTimeout(() => {
                checkEmail(value, currentUserId);
            }, 500);
        });
    } else {
        if (!emailInput) console.error('❌ Email input not found');
        if (!currentUserId) console.error('❌ Current user ID not found');
    }
});

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function checkEmail(email, currentUserId) {
    console.log('🔍 Checking email:', email, 'for user:', currentUserId);
    
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
        console.log('✅ API Response:', data);
        
        const emailInput = document.querySelector('input[type="email"]') || 
                          document.querySelector('input[name*="email"]') ||
                          document.querySelector('input[name*="[email]"]');
        
        if (data.available) {
            if (data.is_current) {
                showFieldFeedback(emailInput, 'neutral', data.message);
            } else {
                showFieldFeedback(emailInput, 'success', data.message);
            }
        } else {
            showFieldFeedback(emailInput, 'error', data.message);
        }
    })
    .catch(err => {
        console.error('❌ Error checking email:', err);
    });
}

function clearFieldFeedback(input) {
    input.classList.remove('is-valid', 'is-invalid', 'border-warning');
    
    let container = input.closest('.form_group') || input.parentElement;
    let feedback = container.querySelector('.validation-feedback');
    if (feedback) {
        feedback.remove();
    }
}

function showFieldFeedback(input, type, message) {
    // Trouver le conteneur approprié
    let container = input.closest('.form_group') || input.parentElement;
    let feedback = container.querySelector('.validation-feedback');
    
    if (!feedback) {
        feedback = document.createElement('small');
        feedback.className = 'validation-feedback d-block mt-2';
        
        // Chercher le wrapper d'input
        const wrapper = input.closest('.input_wrapper');
        if (wrapper && wrapper.parentElement) {
            wrapper.insertAdjacentElement('afterend', feedback);
        } else {
            input.insertAdjacentElement('afterend', feedback);
        }
    }
    
    // Supprimer les anciennes classes de validation
    input.classList.remove('is-valid', 'is-invalid', 'border-warning');
    
    switch(type) {
        case 'success':
            input.classList.add('is-valid');
            feedback.className = 'validation-feedback d-block mt-2 text-success';
            feedback.innerHTML = `<i class="fas fa-check-circle me-1"></i>${message}`;
            break;
        case 'error':
            input.classList.add('is-invalid');
            feedback.className = 'validation-feedback d-block mt-2 text-danger';
            feedback.innerHTML = `<i class="fas fa-times-circle me-1"></i>${message}`;
            break;
        case 'loading':
            input.classList.add('border-warning');
            feedback.className = 'validation-feedback d-block mt-2 text-warning';
            feedback.innerHTML = `<i class="fas fa-hourglass-half me-1"></i>${message}`;
            break;
        case 'neutral':
            feedback.className = 'validation-feedback d-block mt-2 text-muted';
            feedback.innerHTML = `<i class="fas fa-info-circle me-1"></i>${message}`;
            break;
    }
}