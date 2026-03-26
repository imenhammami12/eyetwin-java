// public/assets/js/notifications.js
// Real-time notification system for front-office

(function() {
    'use strict';
    
    const POLL_INTERVAL = 30000; // 30 seconds
    let lastCheckTime = Date.now();
    
    // Play notification sound
    function playNotificationSound() {
        const audio = new Audio('/assets/sounds/notification.mp3');
        audio.volume = 0.5;
        audio.play().catch(e => console.log('Audio blocked:', e));
    }
    
    // Update notification badge
    function updateBadge(count) {
        const badge = document.querySelector('#notifDropdown .badge');
        if (badge) {
            if (count > 0) {
                badge.textContent = count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    }
    
    // Check for new notifications
    async function checkNotifications() {
        try {
            const response = await fetch('/api/notifications/check?t=' + Date.now(), {
                method: 'GET',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                },
                credentials: 'same-origin'
            });
            
            if (!response.ok) return;
            
            const data = await response.json();
            
            if (data.success) {
                // Update badge
                updateBadge(data.unreadCount);
                
                // Check for new notifications
                if (data.newNotifications && data.newNotifications.length > 0) {
                    playNotificationSound();
                    
                    // Refresh the dropdown content
                    refreshDropdownContent(data.allNotifications);
                }
            }
        } catch (error) {
            console.error('Error checking notifications:', error);
        }
    }
    
    // Refresh dropdown content
    function refreshDropdownContent(notifications) {
        const dropdown = document.querySelector('#notifDropdown + .dropdown-menu > div:last-child');
        if (!dropdown) return;
        
        if (notifications.length === 0) {
            dropdown.innerHTML = '<div class="p-3 text-muted">No notifications ✅</div>';
            return;
        }
        
        dropdown.innerHTML = notifications.map(notif => `
            <div class="px-3 py-2" style="border-bottom: 1px solid rgba(255,255,255,0.08); color:#fff; ${notif.isRead ? '' : 'background: rgba(255,255,255,0.06);'}">
                <div style="font-size: 14px;">
                    <span class="me-2">${notif.icon}</span>
                    <span class="${notif.isRead ? 'text-muted' : ''}">${escapeHtml(notif.message)}</span>
                </div>
                <div class="small text-muted mt-1">${notif.timeAgo}</div>
                <div class="mt-2 d-flex gap-2">
                    ${notif.link ? `<a href="${escapeHtml(notif.link)}" class="btn btn-sm btn-outline-light">View</a>` : ''}
                    ${!notif.isRead ? `
                        <button onclick="markAsRead(${notif.id})" class="btn btn-sm btn-outline-secondary">
                            Mark read
                        </button>
                    ` : ''}
                </div>
            </div>
        `).join('');
    }
    
    // Mark notification as read
    window.markAsRead = async function(notificationId) {
        try {
            const response = await fetch(`/api/notifications/${notificationId}/mark-read`, {
                method: 'POST',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                },
                credentials: 'same-origin'
            });
            
            if (response.ok) {
                checkNotifications();
            }
        } catch (error) {
            console.error('Error marking notification as read:', error);
        }
    };
    
    // Escape HTML
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    // Initialize
    document.addEventListener('DOMContentLoaded', function() {
        // Check immediately
        checkNotifications();
        
        // Then check every 30 seconds
        setInterval(checkNotifications, POLL_INTERVAL);
        
        console.log('✓ Notification system initialized');
    });
})();
