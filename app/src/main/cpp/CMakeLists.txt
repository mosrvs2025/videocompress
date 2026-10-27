cmake_minimum_required(VERSION 3.22)
project(pocketpilot_needle CXX)

add_library(needle STATIC IMPORTED)
set_target_properties(needle PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/needle/libneedle.a)

add_library(needle_jni SHARED needle_jni.cpp)
target_include_directories(needle_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/needle)
target_link_libraries(needle_jni needle log android)
