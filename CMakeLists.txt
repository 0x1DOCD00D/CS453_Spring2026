cmake_minimum_required(VERSION 3.10)
project(MPI_PingPong)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find MPI package
find_package(MPI REQUIRED)

# Add executable
add_executable(ping_pong src/main.cpp)

# Link MPI libraries using modern CMake target
target_link_libraries(ping_pong MPI::MPI_CXX)
