package com.baymc.patrol.util;

import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.lang.MessagePlaceholder;

/**
 * 上次巡查时间格式化工具
 */
public final class DurationFormatter {
    private final LanguageService languageService;

    public DurationFormatter(LanguageService languageService) {
        this.languageService = languageService;
    }

    public String formatPrevious(Long previousMillis, long nowMillis) {
        if (previousMillis == null || previousMillis <= 0L) {
            return languageService.plain("time.never");
        }
        long seconds = Math.max(0L, (nowMillis - previousMillis) / 1000L);
        if (seconds < 10L) {
            return languageService.plain("time.just-now");
        }
        if (seconds < 60L) {
            return languageService.plain("time.seconds-ago", MessagePlaceholder.unparsed("seconds", Long.toString(seconds)));
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return languageService.plain("time.minutes-ago", MessagePlaceholder.unparsed("minutes", Long.toString(minutes)));
        }
        long hours = minutes / 60L;
        long remainMinutes = minutes % 60L;
        if (hours < 24L) {
            return languageService.plain(
                    "time.hours-minutes-ago",
                    MessagePlaceholder.unparsed("hours", Long.toString(hours)),
                    MessagePlaceholder.unparsed("minutes", Long.toString(remainMinutes))
            );
        }
        long days = hours / 24L;
        long remainHours = hours % 24L;
        return languageService.plain(
                "time.days-hours-ago",
                MessagePlaceholder.unparsed("days", Long.toString(days)),
                MessagePlaceholder.unparsed("hours", Long.toString(remainHours))
        );
    }
}
