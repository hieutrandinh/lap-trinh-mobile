package org.example.be.core.util;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.springframework.util.StringUtils;

public final class MonthUtil {

    private MonthUtil() {
    }

    public static YearMonth resolve(String monthParam, String userTimezone) {
        if (StringUtils.hasText(monthParam)) {
            try {
                return YearMonth.parse(monthParam);
            } catch (DateTimeParseException e) {
                throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
            }
        }
        return YearMonth.now(safeZone(userTimezone));
    }

    public static ZoneId safeZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }

    public static LocalDateTime startOf(YearMonth month) {
        return month.atDay(1).atStartOfDay();
    }

    public static LocalDateTime startOfNext(YearMonth month) {
        return month.plusMonths(1).atDay(1).atStartOfDay();
    }

}
