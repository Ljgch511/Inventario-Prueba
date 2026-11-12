package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.AssigneeTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "personnel")
@SQLRestriction("deleted_at IS NULL")
public class PersonnelEntity extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AssigneeTargetType targetType;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "national_id", length = 40)
    private String nationalId;

    @Column(name = "group_or_office_name", length = 150)
    private String groupOrOfficeName;

    @Column(length = 150)
    private String email;

    @Column(length = 100)
    private String department;

    public String displayLabel() {
        if (targetType == AssigneeTargetType.PERSON) {
            String full = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            return nationalId != null && !nationalId.isBlank() ? full + " (CI: " + nationalId + ")" : full;
        }
        return groupOrOfficeName != null ? groupOrOfficeName + " [" + targetType + "]" : targetType.name();
    }

    public AssigneeTargetType getTargetType() { return targetType; }
    public void setTargetType(AssigneeTargetType targetType) { this.targetType = targetType; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getGroupOrOfficeName() { return groupOrOfficeName; }
    public void setGroupOrOfficeName(String groupOrOfficeName) { this.groupOrOfficeName = groupOrOfficeName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
