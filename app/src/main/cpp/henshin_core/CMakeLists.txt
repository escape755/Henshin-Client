cmake_minimum_required(VERSION 3.22)
project(henshin_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# libhenshin_core.so: la carga NativeGameBridge con System.loadLibrary("henshin_core").
add_library(henshin_core SHARED
        src/Bridge.cpp
        src/Backend.cpp
)
target_include_directories(henshin_core PRIVATE src ${CMAKE_CURRENT_SOURCE_DIR}/../Preloader/src)
target_link_libraries(henshin_core PRIVATE preloader android log)
