package com.etmisthefox.solacecore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.function.IntFunction;

public final class PaginationUtil {

    private PaginationUtil() {}

    public static final class PageInfo {
        public final int page;
        public final int totalPages;
        public final int fromIndex;
        public final int toIndex;
        public final int totalItems;
        public final int pageSize;

        public PageInfo(int page, int totalPages, int fromIndex, int toIndex, int totalItems, int pageSize) {
            this.page = page;
            this.totalPages = totalPages;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
            this.totalItems = totalItems;
            this.pageSize = pageSize;
        }
    }

    public static PageInfo paginate(int requestedPage, int totalItems, int pageSize) {
        if (pageSize <= 0) pageSize = 1;
        int totalPages = (int) Math.ceil(totalItems / (double) pageSize);
        if (totalPages <= 0) totalPages = 1;
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        return new PageInfo(page, totalPages, fromIndex, toIndex, totalItems, pageSize);
    }

    public static <T> List<T> pageItems(List<T> all, PageInfo info) {
        if (all == null || all.isEmpty()) return all;
        if (info.fromIndex >= all.size()) return all.subList(0, Math.min(all.size(), info.pageSize));
        return all.subList(info.fromIndex, Math.min(info.toIndex, all.size()));
    }

    public static Component buildHeader(String titlePrefix, String subject, PageInfo info) {
        return Component.text(titlePrefix, NamedTextColor.GOLD)
                .append(Component.text(subject, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" (", NamedTextColor.GOLD))
                .append(Component.text("Page " + info.page + "/" + info.totalPages, NamedTextColor.AQUA))
                .append(Component.text(")", NamedTextColor.GOLD));
    }

    public static Component buildFooter(PageInfo info, IntFunction<String> commandForPage) {
        Component footer = Component.empty();
        if (info.page > 1) {
            Component prev = Component.text("<<", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Previous page", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.runCommand(commandForPage.apply(info.page - 1)));
            footer = footer.append(prev);
        }
        if (info.page > 1 && info.page < info.totalPages) {
            footer = footer.append(Component.text(" ", NamedTextColor.DARK_GRAY));
        }
        if (info.page < info.totalPages) {
            Component next = Component.text(">>", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Next page", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.runCommand(commandForPage.apply(info.page + 1)));
            footer = footer.append(next);
        }
        return footer;
    }
}

