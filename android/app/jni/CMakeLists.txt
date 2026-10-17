cmake_minimum_required(VERSION 3.22.1)

project(Dvd LANGUAGES C CXX)

# APP_VERSION comes from ../.cxx-version/dvd_version.h, written by build.gradle.

# 0 = no connection to NTL's tag server (default, see PRIVACY.md); 1 = NTL tags on.
set(DVD_NTL_TAG_NETWORK 0 CACHE STRING "NTL tag network: 0 off, 1 on")
get_filename_component(DVD_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../.." ABSOLUTE)

set(SDL_SHARED ON CACHE BOOL "Build SDL as a shared library" FORCE)
set(SDL_STATIC OFF CACHE BOOL "Disable the unused SDL static library" FORCE)
set(SDL_TESTS OFF CACHE BOOL "Disable SDL tests" FORCE)
add_subdirectory("${DVD_ROOT}/android-sdl" "${CMAKE_BINARY_DIR}/SDL" EXCLUDE_FROM_ALL)

file(GLOB_RECURSE DVD_C_SOURCES CONFIGURE_DEPENDS
    "${DVD_ROOT}/app/src/*.c"
    "${DVD_ROOT}/thermite/src/*.c")
file(GLOB_RECURSE DVD_CXX_SOURCES CONFIGURE_DEPENDS
    "${DVD_ROOT}/app/src/*.cpp"
    "${DVD_ROOT}/thermite/src/*.cpp")

list(FILTER DVD_CXX_SOURCES EXCLUDE REGEX "imgui_impl_glfw\\.cpp$")

add_library(main SHARED ${DVD_C_SOURCES} ${DVD_CXX_SOURCES})

target_compile_features(main PRIVATE c_std_11 cxx_std_17)
target_compile_definitions(main PRIVATE
    SETTINGS_VERSION="2.8"
    CIMGUI_NO_EXPORT
    CGLM_FORCE_DEPTH_ZERO_TO_ONE
    MG_ENABLE_IPV6=1
    DVD_SDL_JNI=1
    DVD_NTL_TAG_NETWORK=${DVD_NTL_TAG_NETWORK})
target_include_directories(main PRIVATE
    "${DVD_ROOT}/app/src"
    "${CMAKE_CURRENT_LIST_DIR}/../.cxx-version"
    "${DVD_ROOT}/thermite/include"
    "${DVD_ROOT}/thermite/src"
    "${DVD_ROOT}/android-sdl/include")
target_compile_options(main PRIVATE
    $<$<COMPILE_LANGUAGE:C>:-Wall;-Wextra;-Wno-unused-parameter>
    $<$<COMPILE_LANGUAGE:CXX>:-Wall;-Wextra;-Wno-unused-parameter>)

find_library(ANDROID_LIB android REQUIRED)
find_library(LOG_LIB log REQUIRED)
find_library(VULKAN_LIB vulkan REQUIRED)

target_link_libraries(main PRIVATE
    SDL3::SDL3
    ${ANDROID_LIB}
    ${LOG_LIB}
    ${VULKAN_LIB})
