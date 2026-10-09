package com.max.musiccloud.data.metadata

object AudioTagSanitizer {

    private val SPECIFIC_WATERMARKS = Regex(
        """[\{\[\(]?\s*(audiovk(\.com)?|mp3party(\.net)?|zaycev(\.net)?|hitmo(\.me)?|muzofond(\.fm)?|promodj(\.com)?|vkmusic)\s*[\}\]\)]?""",
        RegexOption.IGNORE_CASE
    )

    private val WEBSITE_BRACKET_REGEX = Regex(
        """[\{\[\(]\s*([a-zA-Z0-9_\-./]+\.(com|ru|net|org|me|fm|cc|to|club|xyz|biz|io|su|ua|by|kz|info|pro|top)[^}\]\)]*)\s*[\}\]\)]""",
        RegexOption.IGNORE_CASE
    )

    private val WEBSITE_PLAIN_REGEX = Regex(
        """\b[a-zA-Z0-9_\-]+\.(com|ru|net|org|me|fm|cc|to|club|xyz|biz|io|su|ua|by|kz|info|pro|top)(/[^\s]+)?\b""",
        RegexOption.IGNORE_CASE
    )

    private val CURLY_TAG_REGEX = Regex("""\{[^}]*\}""")
    private val VK_LINK_REGEX = Regex("""[\{\[\(]?\s*vk\.com/[^\s\}\]\)]+\s*[\}\]\)]?""", RegexOption.IGNORE_CASE)
    private val LEADING_NUM_REGEX = Regex("""^\d+[\s.\-_]+""")

    fun clean(text: String): String {
        val cleaned = text
            .replace(SPECIFIC_WATERMARKS, "")
            .replace(VK_LINK_REGEX, "")
            .replace(WEBSITE_BRACKET_REGEX, "")
            .replace(WEBSITE_PLAIN_REGEX, "")
            .replace(CURLY_TAG_REGEX, "")
            .replace(LEADING_NUM_REGEX, "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .removePrefix("-").removePrefix("–").removePrefix("—").removePrefix("_")
            .removeSuffix("-").removeSuffix("–").removeSuffix("—").removeSuffix("_")
            .trim()

        return if (cleaned.isNotBlank()) cleaned else text.trim()
    }
}
