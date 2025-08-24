cmake_minimum_required(VERSION 3.22.1)

project("bypass")

add_library(${CMAKE_PROJECT_NAME} SHARED
        signature.cpp
        io.cpp)

add_subdirectory(lspc)

#add_library(xhook SHARED IMPORTED)
#set_target_properties(xhook PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libxhook.so)

add_library(static_dobby STATIC IMPORTED)
set_target_properties(static_dobby PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libdobby.a)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
#        xhook
        static_dobby
        dl)