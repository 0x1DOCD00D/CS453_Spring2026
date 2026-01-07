# CS453_Spring2026
CS 453: Introduction to Parallel and Distributed Processing; Spring, 2026

## MPI Ping-Pong Application

This is a simple MPI (Message Passing Interface) ping-pong application written in C++ for IntelliJ CLion.

### Description

The ping-pong application demonstrates basic MPI communication between two processes. Process 0 and Process 1 exchange messages back and forth (like a ping-pong game) a specified number of times.

### Prerequisites

- CMake 3.10 or higher
- MPI implementation (e.g., OpenMPI or MPICH)
- IntelliJ CLion (optional, for IDE support)
- C++ compiler with C++11 support

### Building the Project

#### Using CMake (Command Line)

```bash
mkdir build
cd build
cmake ..
make
```

#### Using CLion

1. Open the project directory in CLion
2. CLion will automatically detect the CMakeLists.txt file
3. Build the project using the build button or `Ctrl+F9`

### Running the Application

The ping-pong application requires exactly 2 MPI processes to run:

```bash
mpirun -np 2 ./ping_pong
```

Or from the build directory:

```bash
cd build
mpirun -np 2 ./ping_pong
```

### Expected Output

When run successfully, you should see output similar to:

```
Process 0 sent ping_pong_count 1 to process 1
Process 1 received ping_pong_count 1 from process 0
Process 1 sent ping_pong_count 2 to process 0
Process 0 received ping_pong_count 2 from process 1
...
Process 0 finished ping-pong with count 10
Process 1 finished ping-pong with count 10
```

### Project Structure

```
.
├── CMakeLists.txt          # CMake build configuration
├── src/
│   └── main.cpp            # Main MPI ping-pong implementation
├── .idea/                  # CLion project configuration
├── README.md               # This file
└── LICENSE                 # License file
```

### How It Works

1. The application initializes MPI and checks that exactly 2 processes are running
2. Processes alternate sending and receiving an integer counter
3. The counter increments with each send operation
4. The ping-pong continues until a predefined limit (10) is reached
5. Both processes finalize MPI and exit
