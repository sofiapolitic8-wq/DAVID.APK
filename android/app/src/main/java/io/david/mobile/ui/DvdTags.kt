package io.david.mobile.ui

/*
 * Dvd's own tags (OM, 2026-10-07): stickers after NTL's 164 in the tag
 * table (tools/dvd-tag-append.py). Their `ntlId` is DVD_TAG_BASE + their
 * number, which is never an NTL number, and the number itself travels in the
 * skin block's corner. On the sheet each one is turned 90 degrees
 * anticlockwise so it hangs off the rope like a pendant; a picker turns it
 * back upright.
 */
const val DVD_TAG_BASE = 100000

fun isDvdTag(ntlId: Int): Boolean = ntlId >= DVD_TAG_BASE

/** Whether the tag at this sheet index is one of Dvd's own. */
fun isDvdTagIndex(index: Int): Boolean = SkinCatalog.tags.getOrNull(index)?.let { isDvdTag(it.ntlId) } == true

/** Sheet indices in picker order: Dvd's own first, then NTL's. */
val TagPickerOrder: List<Int> by lazy {
    SkinCatalog.tags.filter { isDvdTag(it.ntlId) }.map { it.id } +
        SkinCatalog.tags.filter { !isDvdTag(it.ntlId) }.map { it.id }
}
