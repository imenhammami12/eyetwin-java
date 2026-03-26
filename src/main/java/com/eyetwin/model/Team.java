package com.eyetwin.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Team {

    private int    id;
    private String name;
    private String description;
    private String logo;
    private LocalDateTime createdAt;
    private int    maxMembers = 10;
    private boolean isActive  = true;

    // owner — on stocke l'id ET l'objet User pour éviter les N+1
    private int    ownerId;
    private User   owner;

    // memberships chargés à la demande
    private List<TeamMembership> teamMemberships = new ArrayList<>();

    // ─── Constructeurs ───────────────────────────────────────────
    public Team() {
        this.createdAt = LocalDateTime.now();
    }

    public Team(int id, String name, String description, String logo,
                LocalDateTime createdAt, int maxMembers, boolean isActive,
                int ownerId) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.logo        = logo;
        this.createdAt   = createdAt;
        this.maxMembers  = maxMembers;
        this.isActive    = isActive;
        this.ownerId     = ownerId;
    }

    // ─── Helpers ─────────────────────────────────────────────────
    /** Nombre de membres ACTIVE */
    public long getActiveMembersCount() {
        return teamMemberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .count();
    }

    /** Pourcentage de remplissage (0–100) */
    public int getFillPercent() {
        if (maxMembers <= 0) return 0;
        return (int) Math.round((getActiveMembersCount() * 100.0) / maxMembers);
    }

    /** Vrai si l'équipe a encore de la place */
    public boolean hasRoom() {
        return getActiveMembersCount() < maxMembers;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }

    public String getLogo()                         { return logo; }
    public void setLogo(String logo)                { this.logo = logo; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    public int getMaxMembers()                      { return maxMembers; }
    public void setMaxMembers(int maxMembers)        { this.maxMembers = maxMembers; }

    public boolean isActive()                       { return isActive; }
    public void setActive(boolean active)           { isActive = active; }

    public int getOwnerId()                         { return ownerId; }
    public void setOwnerId(int ownerId)             { this.ownerId = ownerId; }

    public User getOwner()                          { return owner; }
    public void setOwner(User owner)                { this.owner = owner; }

    public List<TeamMembership> getTeamMemberships(){ return teamMemberships; }
    public void setTeamMemberships(List<TeamMembership> m){ this.teamMemberships = m; }

    @Override
    public String toString() {
        return "Team{id=" + id + ", name='" + name + "', owner=" + ownerId + "}";
    }
}