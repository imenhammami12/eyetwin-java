// Admin Notifications System - Using Polling + Mercure

class AdminNotificationManager {
    constructor() {
        console.log('🚀 AdminNotificationManager initializing...');
        
        this.userId = document.body.dataset.userId;
        this.notificationBell = document.getElementById('adminNotificationBell');
        this.notificationBadge = document.getElementById('adminNotificationBadge');
        this.notificationDropdown = document.getElementById('adminNotificationDropdown');
        this.notificationList = document.getElementById('adminNotificationList');
        this.unreadCount = 0;
        this.notificationSound = null;
        this.soundEnabled = false;
        
        if (!this.notificationBell || !this.userId) {
            console.error('❌ Missing bell or user ID');
            return;
        }
        
        console.log('✅ All elements found, initializing...');
        this.init();
    }

    init() {
        this.initSound();
        this.setupDropdown();
        this.setupMarkAllRead();
        this.checkNotifications();
        
        setInterval(() => {
            console.log('🔄 Polling notifications...');
            this.checkNotifications();
        }, 10000);
        
        this.connectToMercure();
    }

    initSound() {
        this.notificationSound = new Audio('/assets/sounds/admin-notification.mp3');
        this.notificationSound.volume = 0.5;

        // ✅ FIXED: unlock audio dès le premier clic sur la page
        const unlock = () => {
            this.notificationSound.play().then(() => {
                this.notificationSound.pause();
                this.notificationSound.currentTime = 0;
                this.soundEnabled = true;
                console.log('✅ Sound unlocked!');
            }).catch(() => {});

            ['click', 'touchstart', 'keydown'].forEach(evt => {
                document.removeEventListener(evt, unlock);
            });
        };

        ['click', 'touchstart', 'keydown'].forEach(evt => {
            document.addEventListener(evt, unlock);
        });
    }

    setupDropdown() {
        this.notificationBell.addEventListener('click', (e) => {
            e.stopPropagation();
            this.notificationDropdown.classList.toggle('show');
        });
        
        document.addEventListener('click', (e) => {
            if (!this.notificationDropdown.contains(e.target) && e.target !== this.notificationBell) {
                this.notificationDropdown.classList.remove('show');
            }
        });
    }

    // ✅ FIXED: /api/admin/ → firewall admin → session reconnue
    async checkNotifications() {
        try {
            const response = await fetch('/api/admin/notifications/check?t=' + Date.now(), {
                method: 'GET',
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                credentials: 'same-origin'
            });
            
            if (!response.ok) {
                console.error('❌ API response not ok:', response.status);
                return;
            }
            
            const data = await response.json();
            
            if (data.success) {
                const oldCount = this.unreadCount;
                const newCount = data.unreadCount ?? data.count ?? 0;
                this.updateNotificationUI(data.notifications, newCount);
                
                if (newCount > oldCount && oldCount !== 0) {
                    console.log('🔔 New notifications!');
                    this.playNotificationSound();
                    if (data.notifications.length > 0) {
                        this.showToast(data.notifications[0]);
                    }
                }
            }
        } catch (error) {
            console.error('❌ Failed to check notifications:', error);
        }
    }

    updateNotificationUI(notifications, count) {
        this.unreadCount = count;
        
        if (this.notificationBadge) {
            if (count > 0) {
                this.notificationBadge.textContent = count > 99 ? '99+' : count;
                this.notificationBadge.style.display = 'flex';
            } else {
                this.notificationBadge.style.display = 'none';
            }
        }
        
        if (this.notificationList) {
            if (notifications.length === 0) {
                this.notificationList.innerHTML = `
                    <div class="empty-notifications">
                        <i class="bi bi-bell-slash"></i>
                        <p>No new notifications</p>
                    </div>`;
            } else {
                this.notificationList.innerHTML = notifications.map(n => this.createNotificationHTML(n)).join('');
                this.attachNotificationListeners();
            }
        }
    }

    createNotificationHTML(notification) {
        return `
            <div class="notification-item ${notification.isRead ? '' : 'unread'}" data-notification-id="${notification.id}">
                <div class="notification-content">
                    <div class="notification-icon">${notification.icon || '🔔'}</div>
                    <div class="notification-text">
                        <p>${this.escapeHtml(notification.message)}</p>
                        <span class="notification-time">${notification.timeAgo}</span>
                        ${notification.link && !notification.isRead ? `
                            <div class="notification-actions">
                                <a href="${this.escapeHtml(notification.link)}" class="btn btn-sm btn-outline-light">View</a>
                                <button class="btn btn-sm btn-outline-secondary mark-read-btn" data-id="${notification.id}">Mark read</button>
                            </div>` : ''}
                    </div>
                </div>
            </div>`;
    }

    attachNotificationListeners() {
        document.querySelectorAll('.mark-read-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                await this.markAsRead(btn.dataset.id);
            });
        });
    }

    async markAsRead(notificationId) {
        try {
            const response = await fetch(`/api/admin/notifications/${notificationId}/mark-read`, {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                credentials: 'same-origin'
            });
            
            const data = await response.json();
            if (data.success) this.checkNotifications();
        } catch (error) {
            console.error('❌ Failed to mark as read:', error);
        }
    }

    setupMarkAllRead() {
        const markAllBtn = document.getElementById('markAllRead');
        if (markAllBtn) {
            markAllBtn.addEventListener('click', async () => {
                try {
                    const response = await fetch('/api/admin/notifications/mark-all-read', {
                        method: 'POST',
                        headers: { 
                            'Content-Type': 'application/json',
                            'X-Requested-With': 'XMLHttpRequest'
                        },
                        credentials: 'same-origin'
                    });
                    
                    const data = await response.json();
                    if (data.success) this.checkNotifications();
                } catch (error) {
                    console.error('❌ Failed to mark all as read:', error);
                }
            });
        }
    }

    // ✅ FIXED: crée un nouvel Audio à chaque fois pour contourner le blocage browser
    playNotificationSound() {
        const sound = new Audio('/assets/sounds/admin-notification.mp3');
        sound.volume = 0.5;
        sound.play()
            .then(() => console.log('✅ Sound played!'))
            .catch(e => console.warn('🔇 Sound blocked by browser:', e.message));
    }

    showToast(notification) {
        const toast = document.createElement('div');
        toast.className = 'admin-notification-toast';
        toast.innerHTML = `
            <div class="toast-content">
                <div class="toast-icon">${notification.icon || '🔔'}</div>
                <div class="toast-text">
                    <div class="toast-title">New Notification</div>
                    <p class="toast-message">${this.escapeHtml(notification.message)}</p>
                </div>
                <button class="toast-close" onclick="this.parentElement.parentElement.remove()">&times;</button>
            </div>`;
        
        document.body.appendChild(toast);
        setTimeout(() => toast.classList.add('show'), 100);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 400);
        }, 5000);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    connectToMercure() {
        if (!window.MERCURE_HUB_URL) return;
        try {
            const hubUrl = new URL(window.MERCURE_HUB_URL);
            hubUrl.searchParams.append('topic', 'notifications/user/' + this.userId);
            this.eventSource = new EventSource(hubUrl);
            this.eventSource.onmessage = () => this.checkNotifications();
            this.eventSource.onerror = () => this.eventSource.close();
        } catch (error) {
            console.error('❌ Failed to connect to Mercure:', error);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const userId = document.body.dataset.userId;
    if (userId) {
        window.adminNotificationManager = new AdminNotificationManager();
    }
});

console.log('📜 admin-notifications.js loaded');