package com.mgmt.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * 富文本白名单过滤：只保留基础排版标签，去掉脚本、事件属性与非法协议，
 * 前端用 v-html 渲染公告内容前必须经过这里清洗。
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.basicWithImages()
            .addTags("h1", "h2", "h3", "h4", "hr", "pre", "code")
            .addAttributes("a", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer");

    private HtmlSanitizer() {
    }

    public static String clean(String html) {
        return html == null ? null : Jsoup.clean(html, SAFELIST);
    }

    /** 富文本转纯文本，用于站内消息等只需要摘要的场景 */
    public static String plainText(String html) {
        return html == null ? null : Jsoup.parse(html).text();
    }
}
