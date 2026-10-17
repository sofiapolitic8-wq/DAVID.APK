package io.david.mobile.ui

/*
 * The worn skin <-> the shared-skin JSON (Trails' "Share my skin", the
 * account's shared settings document). Moved out of TrailStudio.kt unchanged
 * (2026-10-12) so Dvd Desktop compiles the same code.
 */

/** The worn skin and look as the shared-skin JSON ("Share my skin"), the shape Dvd iOS sends too. */
internal fun SkinState.toTrailSkin(look: DvdLookSpec, tagNtlId: Int = -1): io.david.mobile.data.TrailSkin {
    val letters = code.take(256)
    return io.david.mobile.data.TrailSkin(
        custom = custom && letters.isNotEmpty(),
        preset = preset.coerceIn(0, 255),
        code = letters,
        colours = coloursFor(letters.length).toList(),
        accessory = accessory.coerceIn(-1, 255),
        look = io.david.mobile.data.TrailSkinLook(look.hair, look.hairTone, look.ears, look.glasses),
        tag = tagNtlId,
    )
}

/** A shared skin as a Skin-tab draft: only code letters, presets and accessories this app has. */
internal fun io.david.mobile.data.TrailSkin.toSkinState(): SkinState {
    // Colours pair with code positions, so a letter that is dropped takes its colour with it.
    val kept = code.indices.filter { SkinCatalog.group(code[it]) != null }.take(256)
    return SkinState(
        custom = custom && kept.isNotEmpty(),
        preset = preset.takeIf { it in SkinCatalog.presets.indices } ?: 0,
        code = kept.map { code[it] }.joinToString(""),
        accessory = accessory.takeIf { it in SkinCatalog.accessories.indices } ?: -1,
        colours = IntArray(kept.size) { colours.getOrElse(kept[it]) { 0 } },
    )
}

/** A shared skin's Dvd look; the hair colour is the same slider position on both apps. */
internal fun io.david.mobile.data.TrailSkin.lookSpec(): DvdLookSpec =
    DvdLookSpec(look.hair, look.hairTone, look.ears, look.glasses).checked()
