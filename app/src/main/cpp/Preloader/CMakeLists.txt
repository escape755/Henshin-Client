cmake_minimum_required(VERSION 3.22)
project(preloader LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# libpreloader.so: la carga MinecraftActivity con System.loadLibrary("preloader").
add_library(preloader SHARED
        src/impl/Module.cpp
        src/impl/Signature.cpp
        src/impl/HookEngine.cpp
        src/impl/Gloss.cpp
)

# Los demás targets (HenshinMods, pairipcore, MotionBlur) usan ../Preloader/src
target_include_directories(preloader PUBLIC src)
target_compile_options(preloader PRIVATE -fvisibility=default -fexceptions)
target_link_libraries(preloader PRIVATE log dl)
