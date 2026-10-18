cmake_minimum_required(VERSION 3.22)
project(HenshinMods LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

file(GLOB_RECURSE HENSHINMODS_SRC CONFIGURE_DEPENDS "src/*.cpp")

add_library(HenshinMods SHARED ${HENSHINMODS_SRC})

target_include_directories(HenshinMods PRIVATE
        src
        ${CMAKE_CURRENT_SOURCE_DIR}/../Preloader/src)

find_library(log-lib log)
find_library(android-lib android)
find_library(egl-lib EGL)
find_library(glesv3-lib GLESv3)

target_link_libraries(HenshinMods PRIVATE
        preloader
        ${log-lib}
        ${android-lib}
        ${egl-lib}
        ${glesv3-lib})
