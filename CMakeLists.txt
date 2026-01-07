cmake_minimum_required(VERSION 3.10)
project(MPI_PingPong)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find MPI package
find_package(MPI REQUIRED)

# Include MPI directories
include_directories(${MPI_INCLUDE_PATH})

# Add executable
add_executable(ping_pong src/main.cpp)

# Link MPI libraries
target_link_libraries(ping_pong ${MPI_LIBRARIES})

# Add MPI compile flags
if(MPI_COMPILE_FLAGS)
    set_target_properties(ping_pong PROPERTIES
        COMPILE_FLAGS "${MPI_COMPILE_FLAGS}")
endif()

# Add MPI link flags
if(MPI_LINK_FLAGS)
    set_target_properties(ping_pong PROPERTIES
        LINK_FLAGS "${MPI_LINK_FLAGS}")
endif()
