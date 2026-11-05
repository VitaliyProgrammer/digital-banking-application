package io.github.vitaliyprogrammer.banking.business.entity;

import io.github.vitaliyprogrammer.banking.business.entity.status.TaxGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * A client registered as a sole proprietor (ФОП): what they told the bank when they opened the business,
 * and the two accounts that came with it: the business account and the tax jar.
 */
@Entity
@Table(name = "businesses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business {

    private static final String KVED_SEPARATOR = ",";

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID ownerId;

    private String name;

    /** РНОКПП: the person's 10-digit tax number. */
    private String taxId;

    /** КВЕД codes such as 62.01, the main one first; stored as one comma-separated column. */
    private String kvedCodes;

    @Enumerated(EnumType.STRING)
    private TaxGroup taxGroup;

    private boolean vatPayer;

    private LocalDate registeredOn;

    private UUID businessAccountId;

    private UUID taxReserveAccountId;

    private Instant createdAt;

    public static Business register(UUID ownerId, String name, String taxId, List<String> kvedCodes, TaxGroup taxGroup,
                                    boolean vatPayer, LocalDate registeredOn, UUID businessAccountId,
                                    UUID taxReserveAccountId, Instant now) {
        Business business = new Business();
        business.ownerId = ownerId;
        business.name = name;
        business.taxId = taxId;
        business.kvedCodes = String.join(KVED_SEPARATOR, kvedCodes);
        business.taxGroup = taxGroup;
        business.vatPayer = vatPayer;
        business.registeredOn = registeredOn;
        business.businessAccountId = businessAccountId;
        business.taxReserveAccountId = taxReserveAccountId;
        business.createdAt = now;
        return business;
    }

    public List<String> getKvedCodeList() {

        return List.of(kvedCodes.split(KVED_SEPARATOR));
    }
}
