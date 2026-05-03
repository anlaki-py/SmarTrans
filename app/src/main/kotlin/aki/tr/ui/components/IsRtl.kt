package aki.tr.ui.components

fun isRtl(text: String): Boolean {
    if (text.isBlank()) return false
    return text.any {
        val d = Character.getDirectionality(it)
        d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
    }
}
