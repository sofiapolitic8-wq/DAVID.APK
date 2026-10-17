#ifndef BACKGROUNDS_H
#define BACKGROUNDS_H

/**
 * The arenas' floors.
 *
 * Each entry is one seamlessly tiling image plus the world-space size of a
 * single tile. The size is not the pixel size and is not optional: the shader
 * divides a world position by it to get the sampling coordinate, so a wrong
 * number is a pattern that stretches or seams. These are the sizes the
 * reference client declares for the same images, kept exactly.
 *
 * `BACKGROUND_NONE` is the absence of one rather than an image of black: the
 * renderer drops the background pass entirely, which is cheaper than sampling a
 * texture that is the same colour as the clear.
 */
typedef struct background_def {
  const char* id;    /* stable, stored in settings and never reordered */
  const char* label; /* what a player sees */
  const char* path;  /* NULL for the ones that draw nothing */
  float tile_w;
  float tile_h;
} background_def;

enum {
  BACKGROUND_DVD = 0,
  BACKGROUND_NONE = 1,
  BACKGROUND_BLACK = 10,
};

/**
 * Order is the grid order and the stored value, so entries are only ever
 * appended: david.io's own, none, the david.io extension's eight floors
 * (tools/ext-art/bg), black, then the generated set.
 *
 * Every extension floor is declared as a 4096 x 3548 tile: at the default
 * bg_scale (599/4096) that is 599 world units, the size the extension draws
 * its 599 x 519 floor at, whatever the file's pixel size is.
 */
static const background_def BACKGROUNDS[] = {
    {"dvd", "david.io", "app/res/textures/backgrounds/bg54.jpg", 4096.0f, 3548.0f},
    {"none", "None", NULL, 512.0f, 512.0f},
    {"hexslate", "Hex slate", "app/res/textures/backgrounds/bg45.jpg", 4096.0f, 3548.0f},
    {"flowers", "Flowers", "app/res/textures/backgrounds/dd.jpg", 4096.0f, 3548.0f},
    {"greenlava", "Green lava", "app/res/textures/backgrounds/greenlava.jpg", 4096.0f, 3548.0f},
    {"grid", "Grid", "app/res/textures/backgrounds/grid.jpg", 4096.0f, 3548.0f},
    {"mars", "Mars", "app/res/textures/backgrounds/mars.jpg", 4096.0f, 3548.0f},
    {"metalgrid", "Metal grid", "app/res/textures/backgrounds/metalgrid.jpg", 4096.0f, 3548.0f},
    {"noise", "Noise", "app/res/textures/backgrounds/noise.jpg", 4096.0f, 3548.0f},
    {"redlava", "Red lava", "app/res/textures/backgrounds/redlava.jpg", 4096.0f, 3548.0f},
    {"black", "Black", NULL, 512.0f, 512.0f},
    {"dvd_midnight", "Midnight", "app/res/textures/backgrounds/dvd_midnight.png", 3790.04f, 3282.27f},
    {"dvd_carbon", "Carbon", "app/res/textures/backgrounds/dvd_carbon.png", 2954.04f, 2954.04f},
    {"dvd_abyss", "Abyss", "app/res/textures/backgrounds/dvd_abyss.png", 7002.18f, 7002.18f},
    {"dvd_nebula", "Nebula", "app/res/textures/backgrounds/dvd_nebula.png", 7002.18f, 7002.18f},
    {"dvd_dotgrid", "Dot grid", "app/res/textures/backgrounds/dvd_dotgrid.png", 2735.23f, 2735.23f},
    {"dvd_contours", "Contours", "app/res/textures/backgrounds/dvd_contours.png", 7002.18f, 7002.18f},
    {"dvd_scales", "Scales", "app/res/textures/backgrounds/dvd_scales.png", 3282.27f, 3282.27f},
};

enum { NUM_BACKGROUNDS = (int)(sizeof(BACKGROUNDS) / sizeof(BACKGROUNDS[0])) };

/** Any stored value that is no longer in the table falls back to Dvd's own. */
static inline int background_clamp(int index) {
  return (index >= 0 && index < NUM_BACKGROUNDS) ? index : BACKGROUND_DVD;
}

#endif
