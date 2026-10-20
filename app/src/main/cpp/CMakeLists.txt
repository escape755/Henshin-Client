cmake_minimum_required(VERSION 3.22.1)
project(HenshinNative LANGUAGES C CXX)

# Requiere el submódulo Preloader (git submodule update --init --recursive)
if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/Preloader/CMakeLists.txt")
    message(FATAL_ERROR "Falta el submódulo Preloader. Ejecuta: git submodule update --init --recursive")
endif()

add_subdirectory(Preloader)
add_subdirectory(henshin_init)
add_subdirectory(unpaircore)
add_subdirectory(henshin_motion_blur)
add_subdirectory(HenshinMods)
add_subdirectory(henshin_core)
