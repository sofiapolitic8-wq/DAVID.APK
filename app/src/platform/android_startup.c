#include "android_startup.h"

/*
 * Startup reporting, with nowhere to report to.
 *
 * Dvd has no diagnostics screen: the app goes straight to Home. The renderer
 * and the texture uploader still narrate what they are doing, because that
 * narration is genuinely useful when a device fails to start, so it is kept as
 * logging and no longer crosses into Java.
 */

#ifdef DVD_SDL_JNI
#include <SDL3/SDL.h>

void android_startup_stage(int stage, const char* title, const char* detail) {
  SDL_Log("Dvd startup [%d]: %s — %s", stage, title ? title : "",
          detail ? detail : "");
}

void android_startup_failure(int stage, const char* title, const char* detail) {
  SDL_LogError(SDL_LOG_CATEGORY_APPLICATION, "Dvd startup failed [%d]: %s — %s",
               stage, title ? title : "", detail ? detail : "");
}

void android_startup_gpu(const char* name, const char* vulkan_version,
                         int max_texture_dimension) {
  SDL_Log("Dvd startup: GPU %s, Vulkan %s, max texture %d",
          name ? name : "", vulkan_version ? vulkan_version : "",
          max_texture_dimension);
}

void android_startup_texture(const char* label, int width, int height,
                             int completed, int total) {
  SDL_Log("Dvd startup: texture %s %dx%d (%d/%d)", label ? label : "", width,
          height, completed, total);
}

void android_startup_ready(void) { SDL_Log("Dvd startup: ready"); }

#else

void android_startup_stage(int stage, const char* title, const char* detail) {
  (void)stage;
  (void)title;
  (void)detail;
}

void android_startup_failure(int stage, const char* title, const char* detail) {
  (void)stage;
  (void)title;
  (void)detail;
}

void android_startup_gpu(const char* name, const char* vulkan_version,
                         int max_texture_dimension) {
  (void)name;
  (void)vulkan_version;
  (void)max_texture_dimension;
}

void android_startup_texture(const char* label, int width, int height,
                             int completed, int total) {
  (void)label;
  (void)width;
  (void)height;
  (void)completed;
  (void)total;
}

void android_startup_ready(void) {}

#endif
