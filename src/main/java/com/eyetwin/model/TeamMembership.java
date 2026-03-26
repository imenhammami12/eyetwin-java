package com.eyetwin.model;

import java.time.LocalDateTime;

public class TeamMembership {

    private int              id;
    private int              teamId;
    private Team             team;
    private int              userId;
    private User             user;
    private MemberRole       role;
    private MembershipStatus status;
    private LocalDateTime    invitedAt;
    private LocalDateTime    joinedAt;   // null tant que pas ACTIVE

    // ─── Constructeurs ───────────────────────────────────────────
    public TeamMembership() {
        this.invitedAt = LocalDateTime.now();
    }

    public TeamMembership(int id, int teamId, int userId,
                          MemberRole role, MembershipStatus status,
                          LocalDateTime invitedAt, LocalDateTime joinedAt) {
        this.id        = id;
        this.teamId    = teamId;
        this.userId    = userId;
        this.role      = role;
        this.status    = status;
        this.invitedAt = invitedAt;
        this.joinedAt  = joinedAt;
    }

    // ─── Helpers (identiques à Symfony) ──────────────────────────
    /** Accepter une invitation ou une demande → status ACTIVE + joinedAt = now */
    public void accept() {
        this.status = MembershipStatus.ACTIVE;
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }

    /** Refuser une invitation → status INACTIVE */
    public void decline() {
        this.status = MembershipStatus.INACTIVE;
    }

    public boolean isActive()  { return status == MembershipStatus.ACTIVE;  }
    public boolean isPending() { return status == MembershipStatus.PENDING; }
    public boolean isInvited() { return status == MembershipStatus.INVITED; }

    // ─── Getters / Setters ────────────────────────────────────────
    public int getId()                               { return id; }
    public void setId(int id)                        { this.id = id; }

    public int getTeamId()                           { return teamId; }
    public void setTeamId(int teamId)                { this.teamId = teamId; }

    public Team getTeam()                            { return team; }
    public void setTeam(Team team)                   { this.team = team; if (team != null) this.teamId = team.getId(); }

    public int getUserId()                           { return userId; }
    public void setUserId(int userId)                { this.userId = userId; }

    public User getUser()                            { return user; }
    public void setUser(User user)                   { this.user = user; if (user != null) this.userId = user.getId(); }

    public MemberRole getRole()                      { return role; }
    public void setRole(MemberRole role)             { this.role = role; }

    public MembershipStatus getStatus()              { return status; }
    public void setStatus(MembershipStatus status)   { this.status = status; }

    public LocalDateTime getInvitedAt()              { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt){ this.invitedAt = invitedAt; }

    public LocalDateTime getJoinedAt()               { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt)  { this.joinedAt = joinedAt; }

    @Override
    public String toString() {
        return "TeamMembership{id=" + id + ", teamId=" + teamId
                + ", userId=" + userId + ", role=" + role
                + ", status=" + status + "}";
    }
}