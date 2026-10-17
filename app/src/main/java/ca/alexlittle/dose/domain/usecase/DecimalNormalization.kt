package ca.alexlittle.dose.domain.usecase

import java.math.BigDecimal

// stripTrailingZeros alone turns 750 into 7.5E+2, which is not equal to the BigDecimal("750") read back from the DB.
internal fun BigDecimal.normalized(): BigDecimal = stripTrailingZeros().let {
    if (it.scale() <
        0
    ) {
        it.setScale(0)
    } else {
        it
    }
}
