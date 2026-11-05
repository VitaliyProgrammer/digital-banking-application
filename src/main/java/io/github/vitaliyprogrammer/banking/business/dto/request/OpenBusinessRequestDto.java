package io.github.vitaliyprogrammer.banking.business.dto.request;

import io.github.vitaliyprogrammer.banking.business.entity.status.TaxGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** What a client tells the bank to open a sole proprietor's (ФОП) account. */
public record OpenBusinessRequestDto(
        /* As registered, e.g. "ФОП Іваненко Олена Петрівна". */
        @NotBlank
        @Size(max = 150)
        String name,

        /* РНОКПП: 10 digits. */
        @NotBlank
        @Pattern(regexp = "\\d{10}", message = "must be 10 digits")
        String taxId,

        /* КВЕД codes such as 62.01, the main one first. */
        @NotEmpty
        @Size(max = 10)
        List<@Pattern(regexp = "\\d{2}\\.\\d{2}", message = "must look like 62.01") String> kvedCodes,

        @NotNull
        TaxGroup taxGroup,

        /* Only a group 3 proprietor may be a VAT payer. */
        boolean vatPayer,

        @NotNull
        @PastOrPresent
        LocalDate registeredOn
) {
}
