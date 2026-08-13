package com.school.sms.model;

/**
 * Represents a parent or guardian. Kept as its own entity (rather than
 * duplicated fields on Student) so one guardian can be linked to multiple
 * children, and so SMS notifications go to one contact per family relationship.
 */
public class Guardian {
    private int id;
    private String fullName;
    private String relationship;   // e.g. "Father", "Mother", "Guardian"
    private String occupation;
    private String contact;        // phone number, used for SMS

    public Guardian() {}

    public Guardian(int id, String fullName, String relationship, String occupation, String contact) {
        this.id = id;
        this.fullName = fullName;
        this.relationship = relationship;
        this.occupation = occupation;
        this.contact = contact;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}
