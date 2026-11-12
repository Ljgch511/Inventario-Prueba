package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.AcquisitionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "financial_records")
@SQLRestriction("deleted_at IS NULL")
public class FinancialRecordEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_type", nullable = false, length = 40)
    private AcquisitionType acquisitionType;

    @Column(name = "local_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal localCost = BigDecimal.ZERO;

    @Column(name = "local_currency", nullable = false, length = 10)
    private String localCurrency;

    @Column(name = "usd_reference_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal usdReferenceCost = BigDecimal.ZERO;

    @Column(name = "depreciation_rate_annual", precision = 5, scale = 2)
    private BigDecimal depreciationRateAnnual = new BigDecimal("20.00");

    @Column(name = "residual_value_usd", precision = 15, scale = 2)
    private BigDecimal residualValueUsd = BigDecimal.ZERO;

    @Column(name = "provider_name", length = 150)
    private String providerName;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(name = "warranty_expires_at")
    private LocalDate warrantyExpiresAt;

    @Column(name = "lease_counterparty", length = 150)
    private String leaseCounterparty;

    @Column(name = "lease_start_date")
    private LocalDate leaseStartDate;

    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;

    @Column(name = "lease_monthly_price", precision = 15, scale = 2)
    private BigDecimal leaseMonthlyPrice;

    @Column(name = "consignment_installments")
    private Integer consignmentInstallments;

    @Column(name = "consignment_paid_installments")
    private Integer consignmentPaidInstallments = 0;

    @Column(name = "consignment_term_months")
    private Integer consignmentTermMonths;

    @Column(name = "consignment_due_date")
    private LocalDate consignmentDueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public AcquisitionType getAcquisitionType() { return acquisitionType; }
    public void setAcquisitionType(AcquisitionType acquisitionType) { this.acquisitionType = acquisitionType; }
    public BigDecimal getLocalCost() { return localCost; }
    public void setLocalCost(BigDecimal localCost) { this.localCost = localCost; }
    public String getLocalCurrency() { return localCurrency; }
    public void setLocalCurrency(String localCurrency) { this.localCurrency = localCurrency; }
    public BigDecimal getUsdReferenceCost() { return usdReferenceCost; }
    public void setUsdReferenceCost(BigDecimal usdReferenceCost) { this.usdReferenceCost = usdReferenceCost; }
    public BigDecimal getDepreciationRateAnnual() { return depreciationRateAnnual; }
    public void setDepreciationRateAnnual(BigDecimal depreciationRateAnnual) { this.depreciationRateAnnual = depreciationRateAnnual; }
    public BigDecimal getResidualValueUsd() { return residualValueUsd; }
    public void setResidualValueUsd(BigDecimal residualValueUsd) { this.residualValueUsd = residualValueUsd; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public LocalDate getWarrantyExpiresAt() { return warrantyExpiresAt; }
    public void setWarrantyExpiresAt(LocalDate warrantyExpiresAt) { this.warrantyExpiresAt = warrantyExpiresAt; }
    public String getLeaseCounterparty() { return leaseCounterparty; }
    public void setLeaseCounterparty(String leaseCounterparty) { this.leaseCounterparty = leaseCounterparty; }
    public LocalDate getLeaseStartDate() { return leaseStartDate; }
    public void setLeaseStartDate(LocalDate leaseStartDate) { this.leaseStartDate = leaseStartDate; }
    public LocalDate getLeaseEndDate() { return leaseEndDate; }
    public void setLeaseEndDate(LocalDate leaseEndDate) { this.leaseEndDate = leaseEndDate; }
    public BigDecimal getLeaseMonthlyPrice() { return leaseMonthlyPrice; }
    public void setLeaseMonthlyPrice(BigDecimal leaseMonthlyPrice) { this.leaseMonthlyPrice = leaseMonthlyPrice; }
    public Integer getConsignmentInstallments() { return consignmentInstallments; }
    public void setConsignmentInstallments(Integer consignmentInstallments) { this.consignmentInstallments = consignmentInstallments; }
    public Integer getConsignmentPaidInstallments() { return consignmentPaidInstallments; }
    public void setConsignmentPaidInstallments(Integer consignmentPaidInstallments) { this.consignmentPaidInstallments = consignmentPaidInstallments; }
    public Integer getConsignmentTermMonths() { return consignmentTermMonths; }
    public void setConsignmentTermMonths(Integer consignmentTermMonths) { this.consignmentTermMonths = consignmentTermMonths; }
    public LocalDate getConsignmentDueDate() { return consignmentDueDate; }
    public void setConsignmentDueDate(LocalDate consignmentDueDate) { this.consignmentDueDate = consignmentDueDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
